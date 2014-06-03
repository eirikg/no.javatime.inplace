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

import java.util.Collection;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundle.log.status.BundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus.StatusCode;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.BundleClosures;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.msg.Msg;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

/**
 * Activates a project or a set of projects by adding the JavaTime nature and builder to the .project file of
 * the project.
 * <p>
 * Project dependency closures are calculated and added as pending projects to this job according to the
 * current dependency option.
 * <P>
 * If the option for adding bin to class path on update is switched on the the Bundle-ClassPath header is
 * inserted or updated if the bin entry is missing. If the activation policy option is different from the
 * current setting the Bundle-ActivationPolicy header is updated when the option is set to "lazy" and removed
 * if set to "eager".
 * <p>
 * This job does not alter the state of bundles. When a project is nature enabled the project is per
 * definition activated even if the bundle is not yet.
 * <p>
 * After activation of a project one of the following jobs should be triggered by the
 * {@link no.javatime.inplace.builder.PostBuildListener PostBuildListener} (also when auto build is off):
 * <li>If the activated bundle project is in state UNINSTALLED a bundle activate job is triggered.
 * <li>If the activated bundle project is in state INSTALLED a bundle update job is triggered.
 */
public class ActivateProjectJob extends NatureJob {

	/** Standard name of an activate job */
	final public static String activateProjectsJobName = Msg.ACTIVATE_PROJECTS_JOB; 
	final public static String activateWorkspaceJobName = Msg.ACTIVATE_WORKSPACE_JOB; 

	/** Used to name the set of operations needed to activate a project */
	final private static String activateProjectTaskName = Message.getInstance().formatString(
			"activate_project_task_name");

	/**
	 * Construct an activate job with a given name
	 * 
	 * @param name job name
	 */
	public ActivateProjectJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and pending projects to activate
	 * 
	 * @param name job name
	 * @param projects to activate
	 */
	public ActivateProjectJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct an activate job with a given name and a pending project to activate
	 * 
	 * @param name job name
	 * @param project to activate
	 */
	public ActivateProjectJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the project(s) activation operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			BundleManager.addBundleTransitionListener(this);
			monitor.beginTask(ActivateProjectJob.activateProjectTaskName, getTicks());
			activate(monitor);
		} catch(InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (OperationCanceledException e) {
			String msg = UserMessage.getInstance().formatString("cancel_job", getName());
			addCancelMessage(e, msg);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors", getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
//		} catch (CoreException e) {
//			String msg = ExceptionMessage.getInstance().formatString("core_exception_job", getName());
//			addError(e, msg);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			monitor.done();
			BundleManager.removeBundleTransitionListener(this);
		}
	} 

	/**
	 * Activates pending projects to this job by adding the JavaTime nature to the projects
	 * <p>
	 * Calculate closure of projects and add them as pending projects to this job before the projects are
	 * activated according to the current dependency option.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of activating with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException when failing to enable nature
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @see #getStatusList()
	 */
	private IBundleStatus activate(IProgressMonitor monitor) 
			throws InPlaceException, InterruptedException, CircularReferenceException {

		if (pendingProjects() > 0) {
			IBundleStatus result = initWorkspace(monitor);
			if (!result.hasStatus(StatusCode.OK) && !result.hasStatus(StatusCode.INFO)) {
				return result;
			}
			// Bundle order is of no importance when nature enabling projects
			ProjectSorter projectSorter = new ProjectSorter();
			result = removeBuildErrorClosures(projectSorter);
			if (!result.hasStatus(StatusCode.OK) && pendingProjects() == 0) {
				String msg = WarnMessage.getInstance().formatString("terminate_with_errors", getName());
				return createMultiStatus(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg));
			}
			BundleClosures pd = new BundleClosures();
			Collection<IProject> projects = pd.projectActivation(getPendingProjects(), false);
			resetPendingProjects(projects);
			activateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
			// An activate project job triggers an update job when the workspace is activated and
			// an activate bundle job when the workspace is deactivated
			// If Update on Build is switched off and workspace is activated, mark that these projects
			// should be updated as part of the activation process
			if (!getOptionsService().isUpdateOnBuild() && bundleRegion.isBundleWorkspaceActivated()) {
				for (IProject project : getPendingProjects()) {
					if (ProjectProperties.isProjectActivated(project)) {
						bundleTransition.addPending(project, Transition.UPDATE_ON_ACTIVATE);
					}
				}
			}
			InPlace.get().savePluginSettings(true, true);
		} else {
			if (Category.getState(Category.infoMessages)) {
				UserMessage.getInstance().getString("no_projects_to_activate");
			}
			return getLastStatus();
		}
		if (Category.getState(Category.infoMessages) && !ProjectProperties.isAutoBuilding()) {
			UserMessage.getInstance().getString("builder_off");
			UserMessage.getInstance().getString("builder_off_list",
					ProjectProperties.formatProjectList(getPendingProjects()));
		}
		return getLastStatus();
	}

	/**
	 * Prepares the workspace for activation by stopping and uninstalling all workspace bundles and then adding
	 * the uninstalled bundles to the projects to activate.
	 * <p>
	 * Note that it is not allowed (possible) to uninstall a bundle from an external source in an activated
	 * workspace, so clearing the workspace will only happen when the workspace is being activated and bundles
	 * have been installed from an external source.
	 * <p>
	 * This is strictly not necessary but leaves a clean workspace as a starting point.
	 * 
	 * @param monitor report progress on stop and uninstall bundle operations
	 * @return status object with {@code StatusCode.OK}. Otherwise the last failure code is returned. All
	 *         statuses are added to the job status list
	 * @throws OperationCanceledException If user cancel job after uninstalling the bundles
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus initWorkspace(IProgressMonitor monitor) 
			throws OperationCanceledException, InterruptedException, CircularReferenceException {

		// The bundles are registered in the workspace region when installed from an external source		
		final Collection<Bundle> bundlesToActivate = bundleRegion.getBundles();

		if (!ProjectProperties.isProjectWorkspaceActivated() && bundlesToActivate.size() > 0) {
			uninstallBundles(bundlesToActivate, monitor);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			Collection<IProject> projectsToActivate = bundleRegion.getProjects(bundlesToActivate);
			// Subtract installed projects already pending
			projectsToActivate.removeAll(getPendingProjects());
			if (projectsToActivate.size() > 0) {
				addPendingProjects(projectsToActivate);
				if (Category.getState(Category.infoMessages))
					UserMessage.getInstance().getString("added_bundles_to_activate",
							ProjectProperties.formatProjectList(projectsToActivate));
			}
		}
		return getLastStatus();
	}

	/**
	 * Uninstalls the specified bundles.
	 * 
	 * @param bundlesToUninstall he bundles is first stopped and then uninstalled
	 * @param monitor report progress on stop and uninstall bundle operations
	 * @return status object with {@code StatusCode.OK}. Otherwise the last failure code is returned. All
	 *         statuses are added to the job status list
	 * @throws OperationCanceledException If user cancel job after stopping and before uninstalling bundles
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InterruptedException Checks for and interrupts right before call to stop bundle. Stop is also interrupted
	 *           if the task running the stop method is terminated abnormally (timeout or manually)
	 */
	private IBundleStatus uninstallBundles(Collection<Bundle> bundlesToUninstall, IProgressMonitor monitor)
			throws OperationCanceledException, InterruptedException, CircularReferenceException {
		int entrySize = statusList();
		IBundleStatus status = stop(bundlesToUninstall, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		status = uninstall(bundlesToUninstall, new SubProgressMonitor(monitor, 1), false);
		if (!status.hasStatus(StatusCode.OK)) {
			String msg = ErrorMessage.getInstance().formatString("failed_uninstall_before_activate");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
			status = createMultiStatus(multiStatus);
		} else {
			if (Category.getState(Category.infoMessages))
				UserMessage.getInstance().getString("uninstall_before_activate",
						bundleRegion.formatBundleList(bundlesToUninstall, true));
		}
		return statusList() > entrySize ? getLastStatus() : status;
	}

	/**
	 * Detect and report build errors and remove build error closure bundles from projects to nature enable.
	 * 
	 * @param ps topological sort of error closure
	 * @return status object describing the result of removing build errors with {@code StatusCode.OK} if no
	 *         failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 *         of the last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException if projects to nature enable becomes empty after bundle error closure
	 *           is removed
	 */
	private IBundleStatus removeBuildErrorClosures(ProjectSorter bs) throws OperationCanceledException {

		IBundleStatus result = createStatus();

		Collection<IProject> projectErrorClosures = bs.getRequiringBuildErrorClosure(getPendingProjects(), false);
		if (projectErrorClosures.size() > 0) {
			String msg = ProjectProperties.formatBuildErrorsFromClosure(projectErrorClosures, getName());
			if (null != msg) {
				result = addBuildError(msg, null);
			}
			removePendingProjects(projectErrorClosures);
			// Remove delayed activated projects to update waiting for this project with errors to be activated
			Collection<IProject> delayedProjects = bundleTransition.getPendingProjects(
					bundleRegion.getProjects(true), Transition.UPDATE);
			if (delayedProjects.size() > 0) {
				String delayedMsg = WarnMessage.getInstance().formatString("build_errors_delayed",
						UpdateJob.updateJobName, ProjectProperties.formatProjectList(delayedProjects));
				for (IProject delayedProject : delayedProjects) {
					bundleTransition.removePending(delayedProject, Transition.UPDATE);
				}
				result = addBuildError(delayedMsg, null);
			}
		}
		return result;
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		if (!bundleRegion.isBundleWorkspaceActivated() && bundleTransition.containsPending(Transition.EXTERNAL)) {
			return 2;
		} else {
			return 1;
		}
	}
}
