/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.bundlejobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.JavaTimeNature;
import no.javatime.inplace.builder.SaveOptionsJob;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.ExternalDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;

/**
 * Installing, uninstalling and enabling/disabling the JavaTime nature of projects. Checks and
 * reports on duplicate bundles.
 */
public abstract class NatureJob extends BundleJob {

	/**
	 * Construct a nature based job with a given job name
	 * 
	 * @param name the name of the job to run
	 */
	public NatureJob(String name) {
		super(name);
	}

	/**
	 * Constructs a nature based job with a name and projects to perform bundle operations on
	 * 
	 * @param name the name of the job to run
	 * @param projects pending projects to perform bundle operations on
	 */
	public NatureJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs a job with a name and a project to perform bundle operations on
	 * 
	 * @param name the name of the job to run
	 * @param project pending project to perform bundle operations on
	 */
	public NatureJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Does nothing
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException,
			ExtenderException {
		return super.runInWorkspace(monitor);
	}

	@Override
	public void end() {

		super.end();
	}

	public Boolean isProjectActivated(IProject project) throws InPlaceException, ExtenderException {

		if (null == bundleProjectCandidates) {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		}
		if (bundleProjectCandidates.isNatureEnabled(project, JavaTimeNature.JAVATIME_NATURE_ID)) {
			return true;
		}
		return false;
	}

	public Boolean isProjectWorkspaceActivated() throws InPlaceException, ExtenderException {

		if (null == bundleProjectCandidates) {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		}
		for (IProject project : bundleProjectCandidates.getBundleProjects()) {
			if (isProjectActivated(project)) {
				return true;
			}
		}
		return false;
	}

	public Collection<IProject> getActivatedProjects() throws InPlaceException, ExtenderException {

		if (null == bundleProjectCandidates) {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		}
		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : bundleProjectCandidates.getBundleProjects()) {
			if (isProjectActivated(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	public Collection<IProject> getDeactivatedProjects() throws InPlaceException, ExtenderException {

		if (null == bundleProjectCandidates) {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		}
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : bundleProjectCandidates.getBundleProjects()) {
			if (!isProjectActivated(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	/**
	 * Installs pending bundles and set activation status on the bundles. All failures to install are
	 * added to the job error status list
	 * 
	 * @param projectsToInstall a collection of bundle projects to install
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return activated installed bundle projects. The returned list is never {@code null}
	 */
	protected Collection<Bundle> install(Collection<IProject> projectsToInstall,
			IProgressMonitor monitor) {

		SubMonitor progress = SubMonitor.convert(monitor, projectsToInstall.size());

		Collection<Bundle> activatedBundles = new LinkedHashSet<>();
		for (IProject project : projectsToInstall) {
			Bundle bundle = null; // Assume not installed
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				progress.subTask(NLS.bind(Msg.INSTALL_SUB_TASK_JOB, project.getName()));
				// Get the activation status of the corresponding project
				boolean isActivated = isProjectActivated(project);
				bundle = bundleCommand.install(project, isActivated);
				// Project must be activated and bundle must be successfully installed to be activated
				if (null != bundle && isActivated) {
					activatedBundles.add(bundle);
				}
			} catch (WorkspaceDuplicateException e) {
				String msg = null;
				addError(e, e.getMessage(), bundle);
				throw e;
//				try {	
//					handleDuplicateException(project, e, null);
//				} catch (InPlaceException e1) {
//					msg = e1.getLocalizedMessage();
//					addError(e, msg, project);
//				}
//				throw e;
			} catch (ExternalDuplicateException e) {
				addError(e, e.getMessage(), bundle);
				throw e;
			} catch (ProjectLocationException e) {
				IBundleStatus status = addError(e, e.getLocalizedMessage());
				String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());
				status.add(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, project, msg, null));
				msg = NLS.bind(Msg.REFRESH_HINT_INFO, project.getName());
				status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, project, msg, null));
				throw e;
			} catch (InPlaceException e) {
				String msg = ErrorMessage.getInstance().formatString("install_error_project",
						project.getName());
				addError(e, msg, project);
				throw e;
			} finally {
				progress.worked(1);
			}
		}
		return activatedBundles;
	}

	/**
	 * Uninstall and refresh the specified bundles. Errors are added to this job for bundles that fail
	 * to uninstall.
	 * 
	 * @param bundles to uninstall
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @param refresh true to refresh the uninstalled bundles and false to not refresh.
	 * @param unregister If true the bundle is removed from the internal workspace region. Will be
	 * registered again automatically when installed
	 * @return status object describing the result of uninstalling with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException if this thread is interrupted, security violation, illegal argument
	 * (not same framework) or illegal monitor (current thread not owner of monitor)
	 */
	protected IBundleStatus uninstall(Collection<Bundle> bundles, IProgressMonitor monitor,
			boolean refresh, boolean unregister) throws InPlaceException {

		IBundleStatus result = createStatus();
		Collection<Bundle> errorBundles = null;

		if (null != bundles && bundles.size() > 0) {
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			for (Bundle bundle : bundles) {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				localMonitor.subTask(NLS.bind(Msg.UNINSTALL_SUB_TASK_JOB, bundle.getSymbolicName()));
				try {
					// Unregister after refresh
					bundleCommand.uninstall(bundle, false);
				} catch (InPlaceException | ProjectLocationException e) {
					if (null == errorBundles) {
						errorBundles = new ArrayList<>(bundles.size());
					}
					errorBundles.add(bundle);
					result = addError(e, e.getLocalizedMessage(), bundle);
				} finally {
					localMonitor.worked(1);
				}
			}
			if (null == errorBundles && refresh) {
				try {
					refresh(bundles, new SubProgressMonitor(monitor, 1));
				} catch (InPlaceException e) {
					addError(e, NLS.bind(Msg.REFRESH_EXP, getName()));
				}
			}
			if (unregister) {
				if (null != errorBundles) {
					bundles.removeAll(errorBundles);
				}
				for (Bundle bundle : bundles) {
					bundleRegion.unregisterBundleProject(bundleRegion.getProject(bundle));
				}
			}
		}
		return result;
	}

	/**
	 * Reinstalls the specified bundle projects. Failures to reinstall are added to the job status
	 * list. Only specified bundle projects in state INSTALLED are re-installed.
	 * 
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @param refresh true to refresh after uninstall and false if not
	 * @param states Bundle states that must be satisfied for the specified bundles to be reinstalled
	 * @param projectsToInstall a collection of bundle projects to re install
	 * @return installed and activated bundle projects. An empty set means that zero or more
	 * deactivated bundles have been installed
	 */
	protected IBundleStatus reInstall(Collection<IProject> projectsToReinstall,
			IProgressMonitor monitor, boolean refresh, int states) {

		IBundleStatus status = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, "");
		SubMonitor progress = SubMonitor.convert(monitor, projectsToReinstall.size());

		for (IProject project : projectsToReinstall) {
			Bundle bundle = bundleRegion.getBundle(project);
			if ((null != bundle) && (bundle.getState() & (states)) != 0) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					progress.subTask(NLS.bind(Msg.REINSTALL_SUB_TASK_JOB, project.getName()));
					IBundleStatus result = uninstall(Collections.<Bundle> singletonList(bundle),
							new SubProgressMonitor(monitor, 1), refresh, false);
					if (result.hasStatus(StatusCode.OK)) {
						try {
							install(Collections.<IProject> singletonList(project), new SubProgressMonitor(monitor, 1));
						} catch (InPlaceException | WorkspaceDuplicateException | ProjectLocationException e) {
							bundleTransition.addPendingCommand(getActivatedProjects(), Transition.DEACTIVATE);
							return addStatus(new BundleStatus(StatusCode.JOBERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));
						}
					}
				} catch (WorkspaceDuplicateException e) {
					addError(e, e.getLocalizedMessage(), project);
				} catch (InPlaceException e) {
					addError(e, e.getLocalizedMessage(), project);
				} finally {
					progress.worked(1);
				}
			}
		}
		return status;
	}

	/**
	 * Deactivates the specified projects by removing the JavaTime nature from the projects. If update
	 * Bundle-ClassPat on Activate/Deactivate is switched on, remove the default output folder from
	 * the header.
	 * <p>
	 * Failure to deactivate nature for projects and non existing and closed projects are logged
	 * 
	 * @param projectsToDeactivate are the projects to deactivate by removing the JavaTime nature from
	 * the projects
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return Projects where nature deactivation failed. Non existent and closed projects are ignored 
	 */
	protected Collection<IProject> deactivateNature(Collection<IProject> projectsToDeactivate,
			SubProgressMonitor monitor) {

		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToDeactivate.size());
		Collection<IProject> projects = null;
		for (IProject project : projectsToDeactivate) {
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				localMonitor.subTask(NLS.bind(Msg.DISABLE_NATURE_SUB_TASK_JOB, project.getName()));

				if (isProjectActivated(project)) {
					bundleRegion.setActivation(project, false);
					toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
					if (commandOptions.isUpdateDefaultOutPutFolder()) {
						bundleProjectMeta.removeDefaultOutputFolder(project);
					}
				}
			} catch (CoreException e) {
				if (null == projects) {
					projects = new LinkedHashSet<>();
				}
				projects.add(project);
				addError(e, e.getLocalizedMessage(), project);
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), project);
			} finally {
				localMonitor.worked(1);
			}
		}
		return projects;
	}

	/**
	 * Activates the specified projects by adding the JavaTime nature to the projects. Updates output
	 * folder and reinstalls bundles with lazy activation policy when the Set Activation Policy to
	 * Eager on activate option is set in an activated workspace.
	 * <p>
	 * Failure to activate nature for projects and non existing and closed projects are logged
	 * 
	 * @param projectsToActivate are the projects to activate by assigning the JavaTime nature to them
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return Projects where nature activation failed. Non existent and closed projects are ignored 
	 */
	protected Collection<IProject> activateNature(Collection<IProject> projectsToActivate,
			SubProgressMonitor monitor) {
		
		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToActivate.size());
		Collection<IProject> projects = null;
		for (IProject project : projectsToActivate) {
			try {
				localMonitor.subTask(NLS.bind(Msg.ENABLE_NATURE_SUB_TASK_JOB, project.getName()));
				if (bundleProjectCandidates.isCandidate(project)) {
					// Set the JavaTime nature
					if (!isProjectActivated(project)) {
						toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
					}
					resolveBundleClasspath(project);
					Bundle bundle = bundleRegion.getBundle(project);
					try {
						if (commandOptions.isEagerOnActivate()) {
							Boolean isLazy = bundleProjectMeta.getActivationPolicy(project.getProject());
							if (isLazy) {
								bundleProjectMeta.toggleActivationPolicy(project);
								// Uninstall and install bundles when toggling from lazy to eager activation policy
								if (null != bundle) {
									reInstall(Collections.<IProject> singletonList(project), new SubProgressMonitor(
											monitor, 1), true, Bundle.INSTALLED);
									bundle = bundleRegion.getBundle(project);
								}
							}
						}
					} catch (InPlaceException e) {
						addError(e, e.getLocalizedMessage(), project);
					}
					boolean isInstalled = null != bundle ? true : false;
					// Wait to set the bundle as activated to after it is installed
					bundleRegion.registerBundleProject(project, bundle, isInstalled);
					// Adopt any external operations on bundle in an active workspace
					bundleTransition.removePending(project, Transition.EXTERNAL);
					if (isInstalled) {
						// Always tag with update when installed.The post build listener
						// does not always receive all projects after they have been marked by
						// the JavaTimeBuilder after a project has been nature enabled
						bundleTransition.addPending(project, Transition.UPDATE);
					} else {
						// Assure that the bundle is activated when installed
						bundleTransition.addPending(project, Transition.ACTIVATE_BUNDLE);
					}
				}
			} catch (CoreException e) {
				if (null == projects) {
					projects = new LinkedHashSet<>();
				}
				projects.add(project);				
				addError(e, e.getLocalizedMessage(), project);
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), project);
			} finally {
				localMonitor.worked(1);
			}
		}
		return projects;
	}

	/**
	 * Toggles JavaTime nature on a project. If the project has the JavaTime nature, the nature is
	 * removed and if the the project is not nature enabled, the JavaTime nature is added.
	 * 
	 * @param project the project to add or remove JavaTime nature on
	 * @param monitor the progress monitor to use for reporting progress.
	 * @throws InPlaceException Project does not exist or is not open
	 * @throws CoreException Workspace out of sync with the file in the file system, file modification
	 * not allowed or resource is not allowed
	 */
	private void toggleNatureActivation(IProject project, IProgressMonitor monitor)
			throws InPlaceException, CoreException {

		SubMonitor localMonitor = SubMonitor.convert(monitor, 1);
		try {
			if (project.exists() && project.isOpen()) {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				for (int i = 0; i < natures.length; ++i) {
					if (JavaTimeNature.JAVATIME_NATURE_ID.equals(natures[i])) {
						// Remove the nature
						localMonitor.subTask(NLS.bind(Msg.DISABLE_NATURE_SUB_TASK_JOB, project.getName()));
						String[] newNatures = new String[natures.length - 1];
						System.arraycopy(natures, 0, newNatures, 0, i);
						System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
						description.setNatureIds(newNatures);
						project.setDescription(description, null);
						if (messageOptions.isBundleOperations()) {
							Bundle bundle = bundleRegion.getBundle(project);
							if (null == bundle) {
								addLogStatus(Msg.DISABLE_NATURE_TRACE, new Object[] { project.getName() }, project);
							} else {
								addLogStatus(Msg.DISABLE_NATURE_TRACE, new Object[] { project.getName() }, bundle);
							}
						}
						return;
					}
				}
				// Add the nature
				localMonitor.subTask(NLS.bind(Msg.ENABLE_NATURE_SUB_TASK_JOB, project.getName()));
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = JavaTimeNature.JAVATIME_NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				if (messageOptions.isBundleOperations()) {
					Bundle bundle = bundleRegion.getBundle(project);
					if (null == bundle) {
						addLogStatus(Msg.ENABLE_NATURE_TRACE, new Object[] { project.getName() }, project);
					} else {
						addLogStatus(Msg.ENABLE_NATURE_TRACE, new Object[] { project.getName() }, bundle);
					}
				}
			} else {
				addWarning(null, NLS.bind(Msg.ADD_NATURE_PROJECT_ERROR, project), project);
				throw new InPlaceException(NLS.bind(Msg.TOGGLE_NATURE_ERROR, project.getName()));
			}
		} finally {
			localMonitor.worked(1);
		}
	}

	/**
	 * Prompt to save dirty manifest and project meta files
	 * 
	 * @param includeProjectMetaFiles true if to be prompted for saving project meta files
	 * @return
	 * @throws OperationCanceledException If not all files are saved
	 */
	protected void saveDirtyMetaFiles(final boolean includeProjectMetaFiles)
			throws OperationCanceledException {

		IWorkbench workbench = PlatformUI.getWorkbench();
		if (null == workbench || workbench.isClosing()) {
			return;
		}
		final Collection<IResource> dirtyResources = SaveOptionsJob.getScopedDirtyMetaFiles(
				getPendingProjects(), includeProjectMetaFiles);
		if (dirtyResources.size() > 0) {
			Activator.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					final IResource[] dirtyResourcesArray = dirtyResources
							.toArray(new IResource[dirtyResources.size()]);
					boolean allSaved = IDE.saveAllEditors(dirtyResourcesArray, true);
					Collection<IResource> pendingDirtyResources = SaveOptionsJob.getScopedDirtyMetaFiles(
							getPendingProjects(), includeProjectMetaFiles);
					if (!allSaved || pendingDirtyResources.size() > 0) {
						addCancelMessage(null, NLS.bind(Msg.SAVE_FILES_CANCELLED_INFO, getName()));
					}
					if (messageOptions.isBundleOperations()) {
						for (IResource dirtyResource : dirtyResources) {
							IProject dirtyProject = dirtyResource.getProject();
							if (null != dirtyProject) {
								if (pendingDirtyResources.contains(dirtyResource)) {
									addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
											Msg.NOT_SAVE_RESOURCE_IN_PROJECT_INFO, dirtyResource.getName(),
											dirtyProject.getName())));
								} else {
									addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
											Msg.SAVE_RESOURCE_IN_PROJECT_INFO, dirtyResource.getName(),
											dirtyProject.getName())));
								}
							}
						}
					}
				}
			});
		}
		if (getLastErrorStatus().getStatusCode() == StatusCode.CANCEL) {
			throw new OperationCanceledException(getLastErrorStatus().getMessage());
		}
	}

	/**
	 * Builds the collection of projects
	 * 
	 * @param projects to build
	 * @param buildType incremental or full
	 * @param buildName used in progress monitor
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @throws CoreException if build fails
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#FULL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#INCREMENTAL_BUILD
	 */
	protected void buildProjects(Collection<IProject> projects, int buildType, String buildName,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor localMonitor = SubMonitor.convert(monitor, buildName, projects.size());
		for (IProject project : projects) {
			try {
				if (localMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				localMonitor.subTask(Msg.BUILD_TASK_JOB + " " + project.getName());
				project.build(buildType, localMonitor.newChild(1));
			} finally {
				localMonitor.worked(1);
			}
		}
	}
}
