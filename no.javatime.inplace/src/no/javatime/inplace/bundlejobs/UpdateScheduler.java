package no.javatime.inplace.bundlejobs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleRegion;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.UserMessage;

public class UpdateScheduler {

	static public void scheduleUpdateJob(Collection<IProject> projects, long delay) {
		if (projects.size() > 0) {
			UpdateJob updateJob = new UpdateJob(UpdateJob.updateJobName);;
			ActivateProjectJob activateProjectJob = new ActivateProjectJob(ActivateProjectJob.activateNatureJobName);
			ProjectSorter ps = new ProjectSorter();

			// Include pending projects to update in partial graph of projects to update  
			Collection<IProject> projectsToUpdate = new LinkedHashSet<IProject>(projects);
			int count = 0;
			do {
				count = projectsToUpdate.size();
				projectsToUpdate.addAll(ps.sortRequiringProjects(projectsToUpdate, Boolean.TRUE));
				projectsToUpdate.addAll(ps.sortProvidingProjects(projectsToUpdate, Boolean.TRUE));
			} while (projectsToUpdate.size() > count);
			projectsToUpdate = BundleManager.getTransition().getPendingProjects(projectsToUpdate, Transition.UPDATE);			
			for (IProject project : projectsToUpdate) {
				if (ProjectProperties.isProjectActivated(project)) {
					addChangedProject(project, updateJob, activateProjectJob);
				}
			}
			ActivateBundleJob postActivateBundleJob = null;
			if (updateJob.pendingProjects() > 0) {
				postActivateBundleJob = resolveduplicates(activateProjectJob, null, updateJob);
			}
			
			if (activateProjectJob.pendingProjects() > 0) {
				// Update all projects together with the deactivated providers to activate
				if (!InPlace.getDefault().getPrefService().isUpdateOnBuild()) {
					for (IProject project : projects) {
						BundleManager.getTransition().addPending(project, Transition.UPDATE_ON_ACTIVATE);					
					}
				}
				jobHandler(activateProjectJob, delay);			
			}	 else {
				// No deactivated providers to activate
				jobHandler(updateJob, delay);
			}
			if (null != postActivateBundleJob) {
				jobHandler(postActivateBundleJob, delay);
			}
		}
	}

	/**
	 * Adds the specified project to the specified update job if the project has no deactivated providing
	 * projects. If there are deactivated providers to the specified project they are added to the specified
	 * activate project job and the project is ignored. The ignored project will then be updated together with
	 * the deactivated providers when they are activated and updated (delayed update).
	 * 
	 * @param project to add to the specified update job or to be ignored
	 * @param updateJob to add the specified project to
	 * @param activateProjectJob if there are deactivated projects to this project they are added to the
	 *          activate project job
	 * @return true if the specified project is added to the specified update job and false if not (meaning that there
	 * are deactivated projects to the specified project added to the activate project job and the project is ignored)           
	 */
	static public boolean addChangedProject(IProject project, UpdateJob updateJob, ActivateProjectJob activateProjectJob) {

		boolean updated = true;
		// If this project has providing projects that are deactivated they are scheduled for activation
		// and update. This project is delayed for update until the providing projects are updated as
		// part of the scheduled activation process
		Collection<IProject> projects = getDeactivatedProviders(project);
		if (projects.size() == 0) {
			updateJob.addPendingProject(project);
		} else {
			updated = false;
			activateProjectJob.addPendingProjects(projects);
			if (Category.getState(Category.infoMessages)) {
				UserMessage.getInstance().getString("implicit_activation", project.getName(),
						ProjectProperties.formatProjectList(projects));
			}
		}
		return updated;
	}

	/**
	 * Get the providing dependency closure for deactivated projects of the specified project. The specified
	 * project is not part of the result set.
	 * 
	 * @param project initial project for the traversal
	 * @return ordered providing dependency closure. An empty graph is valid.
	 */
	public static Collection<IProject> getDeactivatedProviders(IProject project) {
		Collection<IProject> providingProjects = null;
		try {
			ProjectSorter projectSorter = new ProjectSorter();
			providingProjects = projectSorter.sortProvidingProjects(Collections.singletonList(project), false);
			providingProjects.remove(project);
		} catch (CircularReferenceException e) {
			return Collections.emptySet();
		}
		return providingProjects;
	}
	
	public static ActivateBundleJob getInstalledRequirers(UpdateJob updateJob) {
		ActivateBundleJob postActivateBundleJob = null;
		BundleRegion bundleRegion = BundleManager.getRegion();
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> installedRequirers = ps.sortRequiringProjects(updateJob.getPendingProjects(), true);
		if (installedRequirers.size() > 0) {
			for (IProject project : installedRequirers) {
				Bundle bundle = bundleRegion.get(project);
				if (null != bundle && (BundleManager.getCommand().getState(bundle) & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0) {
					if (null == postActivateBundleJob) {
						postActivateBundleJob = new ActivateBundleJob(ActivateBundleJob.activateJobName);
					}
					postActivateBundleJob.addPendingProject(project);					
				}
			}
		}
		return postActivateBundleJob;
	}
	
	/**
	 * The purpose is to automatically install and update bundles that are no longer duplicates due to changes
	 * in projects to update.
	 * <p>
	 * Bundles are added to the specified update job or added to the returned bundle activation job when the
	 * following conditions are satisfied:
	 * <ol>
	 * <li>There exist activated duplicate bundles in the workspace
	 * <li>There are bundles to update that have changed their symbolic key (symbolic name and/or version)
	 * <li>There are unmodified projects with the same symbolic key as in the bundles to update
	 * </ol>
	 * 
	 * @param activateProjectJob job activating projects
	 * @param activateBundleJob job activating bundles
	 * @param updateJob job updating bundles
	 * @return An activate bundle job with uninstalled duplicate bundles added or null if no uninstalled
	 *         duplicates exist with the same symbolic key as any of the bundles with changed symbolic keys to
	 *         update
	 */
	public static ActivateBundleJob resolveduplicates(ActivateProjectJob activateProjectJob,
			ActivateBundleJob activateBundleJob, UpdateJob updateJob) {

		ActivateBundleJob postActivateBundleJob = null;
		BundleRegion bundleRegion = BundleManager.getRegion();
		BundleTransition bundleTransition = BundleManager.getTransition();

		if (!bundleTransition.hasTransitionError(TransitionError.DUPLICATE)) {
			return postActivateBundleJob;
		}

		// Get projects that have changed their symbolic key (symbolic name and/or the version)
		Map<IProject, String> symbolicKeymap = getModifiedSymbolicKey(updateJob.getPendingProjects());
		if (symbolicKeymap.size() > 0) {
			// Install/update and update all projects that are duplicates to the current symbolic key (before this
			// update) of changed bundles
			Collection<IProject> projects = ProjectProperties.getInstallableProjects();
			projects.removeAll(symbolicKeymap.keySet());
			for (IProject project : projects) {
				String duplicateProjectKey = bundleRegion.getSymbolicKey(null, project);
				if (duplicateProjectKey.length() == 0) {
					continue;
				}
				if (symbolicKeymap.containsValue(duplicateProjectKey)) {
					Bundle bundle = bundleRegion.get(project);
					if (null == bundle) {
						if (bundleTransition.getError(project) == TransitionError.DUPLICATE) {
							if (null == postActivateBundleJob) {
								postActivateBundleJob = new ActivateBundleJob(ActivateBundleJob.activateJobName);
							}
							postActivateBundleJob.addPendingProject(project);
							if (null != activateBundleJob) {
								activateBundleJob.removePendingProject(project);
							}
						}
					} else {
						bundleTransition.addPending(project, Transition.UPDATE);
						UpdateScheduler.addChangedProject(project, updateJob, activateProjectJob);
					}
				}
			}
		}
		return postActivateBundleJob;
	}
	/**
	 * Find and return project symbolic key (symbolic name and version) pairs among the specified projects where
	 * the cached symbolic key of the associated bundle is different from the symbolic key in manifest.
	 * <p>
	 * 
	 * @param projects to search for different symbolic keys in
	 * @return A map of project and cached symbolic key pairs for all specified projects that have different
	 *         symbolic keys
	 */
	static public Map<IProject, String> getModifiedSymbolicKey(Collection<IProject> projects) {

		// Record projects that have changed their symbolic key (symbolic name and/or the version)
		Map<IProject, String> symbolicKeymap = new HashMap<IProject, String>();
		BundleRegion bundleRegion = BundleManager.getRegion();

		for (IProject project : projects) {
			String newProjectKey = bundleRegion.getSymbolicKey(null, project);
			Bundle bundle = bundleRegion.get(project);
			// Do not include activated projects that are not installed. They are in an erroneous state
			if (newProjectKey.length() > 0 && null != bundle) {
				String oldBundleKey = bundleRegion.getSymbolicKey(bundle, null);
				if (oldBundleKey.length() > 0 && !oldBundleKey.equals(newProjectKey)) {
					// The bundle has changed it symbolic key (symbolic name and/or version)
					symbolicKeymap.put(project, oldBundleKey);
				}
			}
		}
		return symbolicKeymap;
	}


	/**
	 * Default way to schedule jobs, with no delay, saving files before schedule, 
	 * waiting on builder to finish, no progress dialog, run the job via the bundle view 
	 * if visible showing a half busy cursor and also displaying the job name in the 
	 * content bar of the bundle view
	 * 
	 * @param job to schedule
	 * @param delay TODO
	 */
	static public void jobHandler(WorkspaceJob job, long delay) {
	
			OpenProjectHandler so = new OpenProjectHandler();
			if (so.saveModifiedFiles()) {
				OpenProjectHandler.waitOnBuilder();
				BundleManager.addBundleJob(job, delay);
			}
	}

}
