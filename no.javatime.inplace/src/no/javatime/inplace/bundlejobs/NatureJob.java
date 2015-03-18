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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.builder.JavaTimeNature;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.DuplicateBundleException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Installing, uninstalling and enabling/disabling the JavaTime nature of projects. Checks and
 * reports on duplicate bundles.
 */
public abstract class NatureJob extends BundleJob {

	/** Used to name the set of operations needed to enable the nature on a project */
	private static String enableNatureSubTaskName = Message.getInstance().formatString(
			"enable_nature_subtask_name");
	/** Used to name the set of operations needed to disable the nature of a project */
	private static String disableNatureSubTaskName = Message.getInstance().formatString(
			"disable_nature_subtask_name");

	/** Task name for the build operation */
	final protected static String buildTaskName = Message.getInstance().formatString(
			"build_task_name");

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

	public Boolean isProjectActivated(IProject project) throws InPlaceException, ExtenderException {
		BundleProjectCandidates bundleProjectCandidates = InPlace.getBundleProjectCandidatesService();
		if (bundleProjectCandidates.isNatureEnabled(project, JavaTimeNature.JAVATIME_NATURE_ID)) {
			return true;
		}
		return false;
	}

	public Boolean isProjectWorkspaceActivated() throws InPlaceException, ExtenderException {
		BundleProjectCandidates bundleProjectCandidates = InPlace.getBundleProjectCandidatesService();
		for (IProject project : bundleProjectCandidates.getBundleProjects()) {
			if (isProjectActivated(project)) {
				return true;
			}
		}
		return false;
	}

	public Collection<IProject> getActivatedProjects() throws InPlaceException, ExtenderException {

		BundleProjectCandidates bundleProjectCandidates = InPlace.getBundleProjectCandidatesService();
		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : bundleProjectCandidates.getBundleProjects()) {
			if (isProjectActivated(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	/**
	 * Does nothing
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		return super.runInWorkspace(monitor);
	}

	/**
	 * Installs pending bundles and set activation status on the bundles. All failures to install are
	 * added to the job status list
	 * 
	 * @param projectsToInstall a collection of bundle projects to install
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return activated installed bundle projects. The returned list is never {@code null}
	 */
	protected Collection<Bundle> install(Collection<IProject> projectsToInstall,
			IProgressMonitor monitor) {

		SubMonitor progress = SubMonitor.convert(monitor, projectsToInstall.size());
		Collection<Bundle> activatedBundles = new LinkedHashSet<Bundle>();

		for (IProject project : projectsToInstall) {
			Bundle bundle = null; // Assume not installed
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				progress.subTask(installSubtaskName + project.getName());
				// Get the activation status of the corresponding project
				boolean activated = isProjectActivated(project);
				bundle = bundleCommand.install(project, activated);
				// Project must be activated and bundle must be successfully installed to be activated
				if (null != bundle && activated) {
					activatedBundles.add(bundle);
				}
			} catch (DuplicateBundleException e) {
				String msg = null;
				try {
					handleDuplicateException(project, e, null);
				} catch (InPlaceException e1) {
					msg = e1.getLocalizedMessage();
					addError(e, msg, project);
				}
			} catch (ProjectLocationException e) {
				IBundleStatus status = addError(e, e.getLocalizedMessage());
				String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());
				status.add(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, msg, null));
				msg = NLS.bind(Msg.REFRESH_HINT_INFO, project.getName());
				status.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, project, msg, null));
			} catch (InPlaceException e) {
				String msg = ErrorMessage.getInstance().formatString("install_error_project",
						project.getName());
				addError(e, msg, project);
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
				localMonitor.subTask(UninstallJob.uninstallSubtaskName + bundle.getSymbolicName());
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
				refresh(bundles, new SubProgressMonitor(monitor, 1));
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
	 * @param projectsToInstall a collection of bundle projects to re install
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return installed and activated bundle projects. An empty set means that zero or more
	 * deactivated bundles have been installed
	 */
	protected IBundleStatus reInstall(Collection<IProject> projectsToReinstall,
			IProgressMonitor monitor) {

		IBundleStatus status = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
		SubMonitor progress = SubMonitor.convert(monitor, projectsToReinstall.size());

		for (IProject project : projectsToReinstall) {
			Bundle bundle = bundleRegion.getBundle(project);
			if ((null != bundle) && (bundle.getState() & (Bundle.INSTALLED)) != 0) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					progress.subTask(reInstallSubtaskName + project.getName());
					IBundleStatus result = uninstall(Collections.<Bundle> singletonList(bundle),
							new SubProgressMonitor(monitor, 1), true, false);
					if (result.hasStatus(StatusCode.OK)) {
						install(Collections.<IProject> singletonList(project), new SubProgressMonitor(monitor,
								1));
					}
				} catch (DuplicateBundleException e) {
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
	 * 
	 * @param projectsToDeactivate are the projects to deactivate by removing the JavaTime nature from
	 * the projects
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return status object describing the result of deactivating nature with {@code StatusCode.OK}
	 * if no failure, otherwise one of the failure codes are returned. If more than one bundle fails,
	 * status of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException failed to remove nature or the default output folder
	 */
	protected IBundleStatus deactivateNature(Collection<IProject> projectsToDeactivate,
			SubProgressMonitor monitor) throws InPlaceException {

		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToDeactivate.size());

		for (IProject project : projectsToDeactivate) {
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				localMonitor.subTask(NatureJob.disableNatureSubTaskName + project.getName());
				if (isProjectActivated(project)) {
					toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
				}
				if (getOptionsService().isUpdateDefaultOutPutFolder()) {
					bundleProjectMeta.removeDefaultOutputFolder(project);
				}
				bundleTransition.clearTransitionError(project);
				bundleRegion.setActivation(project, false);
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), project);
				throw e;
			} finally {
				localMonitor.worked(1);
			}
		}
		return getLastErrorStatus();
	}

	/**
	 * Activates the specified projects by adding the JavaTime nature to the projects. Updates output
	 * folder and reinstalls bundles with lazy activation policy when the Set Activation Policy to
	 * Eager on activate option is set in an activated workspace.
	 * 
	 * 
	 * @param projectsToActivate are the projects to activate by assigning the JavaTime nature to them
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return status object describing the result of activating nature with {@code StatusCode.OK} if
	 * no failure, otherwise one of the failure codes are returned. If more than one bundle fails,
	 * status of the last failed bundle is returned. All failures are added to the job status list
	 */
	protected IBundleStatus activateNature(Collection<IProject> projectsToActivate,
			SubProgressMonitor monitor) {

		IBundleStatus result = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToActivate.size());

		for (IProject project : projectsToActivate) {
			try {
				localMonitor.subTask(NatureJob.enableNatureSubTaskName + project.getName());
				if (bundleProjectCandidates.isCandidate(project)) {
					// Set the JavaTime nature
					if (!isProjectActivated(project)) {
						toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
					}
					result = resolveBundleClasspath(project);
					Bundle bundle = bundleRegion.getBundle(project);
					try {
						if (getOptionsService().isEagerOnActivate()) {
							Boolean isLazy = bundleProjectMeta.getActivationPolicy(project.getProject());
							if (isLazy) {
								bundleProjectMeta.toggleActivationPolicy(project);
								// Uninstall and install bundles when toggling from lazy to eager activation policy
								if (null != bundle) {
									reInstall(Collections.<IProject> singletonList(project), new SubProgressMonitor(
											monitor, 1));
									bundle = bundleRegion.getBundle(project);
								}
							}
						}
					} catch (InPlaceException e) {
						result = addError(e, e.getLocalizedMessage(), project);
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
			} catch (InPlaceException e) {
				result = addError(e, e.getLocalizedMessage(), project);
			} finally {
				localMonitor.worked(1);
			}
		}
		return result;
	}

	/**
	 * Toggles JavaTime nature on a project. If the project has the JavaTime nature, the nature is
	 * removed and if the the project is not nature enabled, the JavaTime nature is added.
	 * 
	 * @param project the project to add or remove JavaTime nature on
	 * @param monitor the progress monitor to use for reporting progress.
	 * @throws InPlaceException Fails to remove or add the nature from/to the project.
	 */
	public void toggleNatureActivation(IProject project, IProgressMonitor monitor)
			throws InPlaceException {

		if (project.exists() && project.isOpen()) {
			try {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				SubMonitor localMonitor = SubMonitor.convert(monitor, 1);
				for (int i = 0; i < natures.length; ++i) {
					if (JavaTimeNature.JAVATIME_NATURE_ID.equals(natures[i])) {
						// Remove the nature
						localMonitor.subTask(NatureJob.disableNatureSubTaskName + project.getName());
						String[] newNatures = new String[natures.length - 1];
						System.arraycopy(natures, 0, newNatures, 0, i);
						System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
						description.setNatureIds(newNatures);
						project.setDescription(description, null);
						if (InPlace.get().getMsgOpt().isBundleOperations()) {
							Bundle bundle = bundleRegion.getBundle(project);
							if (null == bundle) {
								addLogStatus(Msg.DISABLE_NATURE_TRACE, new Object[] { project.getName() }, project);
							} else {
								addLogStatus(Msg.DISABLE_NATURE_TRACE, new Object[] { project.getName() }, bundle);
							}
						}
						localMonitor.worked(1);
						return;
					}
				}
				// Add the nature
				localMonitor.subTask(NatureJob.enableNatureSubTaskName + project.getName());
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = JavaTimeNature.JAVATIME_NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					Bundle bundle = bundleRegion.getBundle(project);
					if (null == bundle) {
						addLogStatus(Msg.ENABLE_NATURE_TRACE, new Object[] { project.getName() }, project);
					} else {
						addLogStatus(Msg.ENABLE_NATURE_TRACE, new Object[] { project.getName() }, bundle);
					}
				}
				localMonitor.worked(1);
			} catch (CoreException e) {
				throw new InPlaceException(e, "error_changing_nature", project.getName());
			}
		} else {
			String msg = WarnMessage.getInstance().formatString("add_nature_project_invalid",
					project.getName());
			addWarning(null, msg, project);
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
				localMonitor.subTask(buildTaskName + project.getName());
				project.build(buildType, localMonitor.newChild(1));
			} finally {
				localMonitor.worked(1);
			}
		}
	}
}
