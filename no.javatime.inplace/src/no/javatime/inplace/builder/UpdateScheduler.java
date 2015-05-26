package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dialogs.ResourceStateHandler;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Helper class for scheduling and adding projects to update jobs.
 * <p>
 * Duplicate projects that have become unique are scheduled for activation (and update). Projects
 * that are identified as members in build errors closures are neither scheduled for update or added
 * to any update job.
 */
public class UpdateScheduler {

	/**
	 * Schedules an update job for the specified projects. Projects that are members in build error
	 * closures are not added to the update job. Uninstalled duplicate projects that now are unique -
	 * by changing their symbolic name and/or version - are scheduled for activation.
	 * <p>
	 * Only activated projects with a pending update transition are scheduled for update
	 * 
	 * @param projects projects to schedule for update. Must not be null
	 * @param bundleRegion The bundle region service used to check if region is activated
	 * @param bundleProjectCandidates The candidate service used to get installable projects
	 * @param delay number of milliseconds before starting the update job
	 * @throws ExtenderException if failing to add update or activate to the job executor service
	 */

	static public void scheduleUpdateJob(Collection<IProject> projects,
			final BundleRegion bundleRegion, final BundleTransition bundleTransition,
			final BundleProjectCandidates bundleProjectCandidates, long delay) throws ExtenderException {

		Update updateJob = new UpdateJob();
		for (IProject project : projects) {
			if (bundleRegion.isBundleActivated(project)
					&& bundleTransition.containsPending(project, Transition.UPDATE, false)) {
				addProjectToUpdateJob(project, updateJob);
			}
		}
		ActivateBundle postActivateBundleJob = null;
		if (updateJob.pendingProjects() > 0) {
			postActivateBundleJob = resolveduplicates(null, updateJob, bundleRegion, bundleTransition,
					bundleProjectCandidates);
			jobHandler(updateJob, delay);
		}
		if (null != postActivateBundleJob) {
			jobHandler(postActivateBundleJob, delay);
		}
	}

	/**
	 * Adds the specified bundle project to the specified update job if the bundle project has no
	 * build error closures.
	 * <p>
	 * It is only activated bundle projects that are being updated. Bundle projects updated the first
	 * time after being activated are in state installed. The bundle project may than be in any state
	 * except uninstalled (not considering the temporary starting/stopping states) when being updated.
	 * <p>
	 * Build error closures that allows and prevents an update of an activated bundle project are:
	 * <ol>
	 * <li><b>Requiring update closures</b>
	 * <p>
	 * <br>
	 * <b>Activated requiring closure.</b> An update is rejected when there exists activated bundles
	 * with build errors requiring capabilities from the project to update. An update would imply a
	 * resolve (and a stop/start if in state active) of the activated requiring bundles with build
	 * errors. The preferred solution is to suspend the resolve and possibly a start to avoid the
	 * framework to throw exceptions when an activated requiring bundle with build errors is started
	 * as part of the update process (stop/resolve/start) or when started later on. From this follows
	 * that there is nothing to gain by constructing a new revision (resolving or refreshing) of a
	 * requiring bundle with build errors.
	 * <p>
	 * <br>
	 * <b>Deactivated requiring closure.</b> Deactivated bundle projects with build errors requiring
	 * capabilities from the project to update will not trigger an activation of the deactivated
	 * bundles and as a consequence the bundles will not be resolved and are therefore allowed.
	 * <p>
	 * <br>
	 * <li><b>Providing update closures</b>
	 * <p>
	 * <br>
	 * <b>Deactivated providing closure.</b> Update is rejected when deactivated bundles with build
	 * errors provides capabilities to a project to update. This would trigger an activation in the
	 * resolver hook (update and resolve) of the providing bundle project(s) and result in errors of
	 * the same type as in the <b>Activated requiring closure</b>.
	 * <p>
	 * <br>
	 * <b>Activated providing closure.</b> It is legal to update the project when there are activated
	 * bundles with build errors that provides capabilities to the project to update. The providing
	 * bundles will not be affected (updated and resolved) when the project is updated. The project to
	 * update will get wired to the current revision (that is from the last successful resolve) of the
	 * activated bundles with build errors when resolved.
	 * </ol>
	 * 
	 * @param project to add to the specified update job or to be ignored
	 * @param update the job to add the specified project to. Specify null to not add the project
	 * @return true if the specified project is added to the specified update job and false if not
	 * @throws ExtenderException If failing to get the candidate service
	 */
	static public boolean addProjectToUpdateJob(IProject project, Update update)
			throws ExtenderException {

		boolean isUpdate = true;
		// Do not update when there are activated requiring projects or deactivated proving projects
		// with build errors

		// Activated requiring closure. Activated bundles with build errors requiring
		// capabilities from the project to update
		try {
			BuildErrorClosure be = new BuildErrorClosure(Collections.<IProject> singletonList(project),
					Transition.UPDATE, Closure.REQUIRING);
			BundleProjectCandidates bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
			MessageOptions messageOptions = Activator.getMessageOptionsService(); 
			if (be.hasBuildErrors()) {
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.UPDATE_BUILD_ERROR_INFO, new Object[] { project.getName(),
							bundleProjectCandidates.formatProjectList(be.getBuildErrors()) });
					be.setBuildErrorHeaderMessage(msg);
					IBundleStatus bundleStatus = be.getErrorClosureStatus();
					if (null != bundleStatus) {
						Activator.log(bundleStatus);
						// StatusManager.getManager().handle(bundleStatus, StatusManager.LOG);
					}
				}
				isUpdate = false;
			}
			// Deactivated providing closure. Deactivated projects with build errors providing
			// capabilities to project to update
			if (isUpdate) {
				be = new BuildErrorClosure(Collections.<IProject> singletonList(project),
						Transition.UPDATE, Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.DEACTIVATED);
				if (be.hasBuildErrors()) {
					if (messageOptions.isBundleOperations()) {
						String msg = NLS.bind(Msg.UPDATE_BUILD_ERROR_INFO, new Object[] { project.getName(),
								bundleProjectCandidates.formatProjectList(be.getBuildErrors()) });
						be.setBuildErrorHeaderMessage(msg);
						IBundleStatus bundleStatus = be.getErrorClosureStatus();
						if (null != bundleStatus) {
							Activator.log(bundleStatus);
							// StatusManager.getManager().handle(bundleStatus, StatusManager.LOG);
						}
					}
					isUpdate = false;
				}
			}
			if (isUpdate && null != update) {
				update.addPendingProject(project);
			}
		} catch (BundleLogException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
			isUpdate = false;
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
					project, msg, null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
			isUpdate = false;
		}
		return isUpdate;
	}

	/**
	 * Get the providing dependency closure for deactivated projects of the specified project. The
	 * specified project is not part of the result set.
	 * 
	 * @param project initial project for the traversal
	 * @return ordered providing dependency closure or an empty set if there are no deactivated
	 * providers
	 */
	@SuppressWarnings("unused")
	private Collection<IProject> getDeactivatedProviders(IProject project) {
		Collection<IProject> providingProjects = null;
		try {
			ProjectSorter projectSorter = new ProjectSorter();
			providingProjects = projectSorter.sortProvidingProjects(
					Collections.<IProject> singletonList(project), false);
			providingProjects.remove(project);
		} catch (CircularReferenceException e) {
			return Collections.<IProject> emptySet();
		}
		return providingProjects;
	}

	@SuppressWarnings("unused")
	private ActivateBundle getInstalledRequiringProjects(Update updateJob) throws ExtenderException {

		ActivateBundle postActivateBundleJob = null;
		BundleRegion bundleRegion = Activator.getBundleRegionService();
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> installedRequirers = ps.sortRequiringProjects(
				updateJob.getPendingProjects(), true);
		if (installedRequirers.size() > 0) {
			for (IProject project : installedRequirers) {
				Bundle bundle = bundleRegion.getBundle(project);
				if (null != bundle
						&& (Activator.getBundleCommandService().getState(bundle) & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0) {
					if (null == postActivateBundleJob) {
						postActivateBundleJob = new ActivateBundleJob();
					}
					postActivateBundleJob.addPendingProject(project);
				}
			}
		}
		return postActivateBundleJob;
	}

	/**
	 * The purpose is to automatically install and update bundles that are no longer duplicates due to
	 * changes in projects to update.
	 * <p>
	 * Bundles are added to the specified update job or added to the returned bundle activation job
	 * when the following conditions are satisfied:
	 * <ol>
	 * <li>There exist activated duplicate bundles in the workspace
	 * <li>There are bundles to update that have changed their symbolic key (symbolic name and/or
	 * version)
	 * <li>There are unmodified projects with the same symbolic key as in the bundles to update
	 * </ol>
	 * 
	 * @param activateBundle job activating bundles
	 * @param update job updating bundles
	 * @param bundleRegion The bundle region service used to obtain project and bundle attribute
	 * information
	 * @param bundleTransition The bundle transition service used to obtain project and bundle state
	 * information
	 * @param bundleProjectCandidates The candidate service used to get installable projects
	 * @return An activate bundle job with uninstalled duplicate bundles added or null if no
	 * uninstalled duplicates exist with the same symbolic key as any of the bundles with changed
	 * symbolic keys to update
	 */
	public static ActivateBundle resolveduplicates(final ActivateBundle activateBundle,
			final Update update, final BundleRegion bundleRegion,
			final BundleTransition bundleTransition, final BundleProjectCandidates bundleProjectCandidates) {

		ActivateBundle postActivateBundleJob = null;

		if (!bundleTransition.hasTransitionError(TransitionError.DUPLICATE)) {
			return postActivateBundleJob;
		}

		// Get projects that have changed their symbolic key (symbolic name and/or the version)
		Map<IProject, String> symbolicKeymap = getModifiedSymbolicKey(update.getPendingProjects(),
				bundleRegion);
		if (symbolicKeymap.size() > 0) {
			// Install/update and update all projects that are duplicates to the current symbolic key
			// (before this update) of changed bundles
			Collection<IProject> projects = bundleProjectCandidates.getInstallable();
			projects.removeAll(symbolicKeymap.keySet());
			for (IProject project : projects) {
				String duplicateProjectKey = bundleRegion.getSymbolicKey(null, project);
				if (duplicateProjectKey.length() == 0) {
					continue;
				}
				// Does this bundle have the same symbolic key as one ore more of the bundles to update
				if (symbolicKeymap.containsValue(duplicateProjectKey)) {
					Bundle bundle = bundleRegion.getBundle(project);
					// Activate uninstalled bundles and update installed bundles
					if (null == bundle) {
						if (bundleTransition.getError(project) == TransitionError.DUPLICATE) {
							if (null == postActivateBundleJob) {
								postActivateBundleJob = new ActivateBundleJob();
							}
							postActivateBundleJob.addPendingProject(project);
							if (null != activateBundle) {
								activateBundle.removePendingProject(project);
							}
						}
					} else {
						bundleTransition.addPending(project, Transition.UPDATE);
						UpdateScheduler.addProjectToUpdateJob(project, update);
					}
				}
			}
		}
		return postActivateBundleJob;
	}

	/**
	 * Find and return project symbolic key (symbolic name and version) pairs among the specified
	 * projects where the cached symbolic key of the associated bundle is different from the symbolic
	 * key in manifest.
	 * <p>
	 * 
	 * @param projects to search for different symbolic keys in
	 * @param bundleRegion The bundle region service used to get bundles from specified projects
	 * @return A map of project and cached symbolic key pairs for all specified projects that have
	 * different symbolic keys
	 */
	private static Map<IProject, String> getModifiedSymbolicKey(Collection<IProject> projects,
			BundleRegion bundleRegion) {

		// Record projects that have changed their symbolic key (symbolic name and/or the version)
		Map<IProject, String> symbolicKeymap = new HashMap<IProject, String>();

		for (IProject project : projects) {
			String newProjectKey = bundleRegion.getSymbolicKey(null, project);
			Bundle bundle = bundleRegion.getBundle(project);
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
	 * Default way to schedule jobs, with no delay, saving files before schedule, waiting on builder
	 * to finish, no progress dialog and run the job via the bundle view if visible showing a half
	 * busy cursor and also displaying the job name in the content bar of the bundle view
	 * 
	 * @param job to schedule
	 * @param delay number of msecs to wait before starting the job
	 * @throws ExtenderException If failing to add he specified job to job queue
	 */
	static private void jobHandler(BundleExecutor job, long delay) throws ExtenderException {

		ResourceStateHandler so = new ResourceStateHandler();
		if (so.saveModifiedResources()) {
			so.waitOnBuilder(true);
			Activator.getBundleExecutorEventService().add(job, delay);

		}
	}

}
