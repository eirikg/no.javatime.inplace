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
import java.util.Collections;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class ActivateProjectJob extends NatureJob implements ActivateProject {

	/**
	 * Default constructor with a default job name
	 */
	public ActivateProjectJob() {
		super(Msg.ACTIVATE_PROJECT_JOB);
	}

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
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(Msg.ACTIVATE_PROJECT_TASK_JOB, getTicks());
			activate(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (OperationCanceledException e) {
			addCancel(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addError(multiStatus);
		} catch (ExtenderException e) {
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, e);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();
	}

	/**
	 * Activates pending projects to this job by adding the JavaTime nature to the projects
	 * <p>
	 * Calculate closure of projects and add them as pending projects to this job before the projects
	 * are activated according to the current dependency option.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of activating with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException when failing to enable nature or if one of the specified projects does
	 * not exist or is closed
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @see #getErrorStatusList()
	 */
	private IBundleStatus activate(IProgressMonitor monitor) throws InPlaceException,
			InterruptedException, CircularReferenceException {

		if (pendingProjects() == 0) {
			if (messageOptions.isBundleOperations()) {
				addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
						Msg.NO_PROJECTS_TO_ACTIVATE_INFO));
			}
			return getLastErrorStatus();
		}
		boolean isWorkspaceActivated = isProjectWorkspaceActivated();
		saveDirtyMetaFiles(true);
		// Include dependent projects to activate according to dependency options
		BundleClosures closures = new BundleClosures();
		// As a minimum (mandatory) these closures include providing deactivated projects
		Collection<IProject> pendingProjects = closures.projectActivation(getPendingProjects(), false);
		resetPendingProjects(pendingProjects);
		if (!isWorkspaceActivated) {
			// Must be able to install all projects or none in a deactivated workspace
			Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
			Collection<IProject> errorClosure = buildErrorClosure(projects);
			if (errorClosure.size() > 0) {
				return getLastErrorStatus();
			}
		} else {
			Collection<IProject> errorClosure = buildErrorClosure(getPendingProjects());
			if (errorClosure.size() > 0) {
				removePendingProjects(errorClosure);
			}
		}
		// Return if there are no closures left to activate after error closures have been removed
		if (pendingProjects() == 0) {
			return getLastErrorStatus();
		}
		// Uninstall any installed bundles before activating workspace
		IBundleStatus result = initWorkspace(monitor);
		if (!result.hasStatus(StatusCode.OK) && !result.hasStatus(StatusCode.INFO)) {
			return result;
		}
		activateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
		// An activate project job triggers an update job when the workspace is activated and
		// an activate bundle job when the workspace is deactivated
		// If Update on Build is switched off and workspace is activated, mark that these projects
		// should be updated as part of the activation process
		if (!commandOptions.isUpdateOnBuild() && isWorkspaceActivated) {
			for (IProject project : getPendingProjects()) {
				if (isProjectActivated(project)) {
					bundleTransition.addPending(project, Transition.UPDATE_ON_ACTIVATE);
				}
			}
		}
		return getLastErrorStatus();
	}

	/**
	 * Find and return build error closures among the specified deactivated projects to activate.
	 * 
	 * @param projects projects to activate with possible build error closures
	 * @return set of providing build error closures or an empty set
	 */
	private Collection<IProject> buildErrorClosure(Collection<IProject> projects) {

		// Deactivated providing closure. In this case the activation scope is deactivated as long as we
		// are not checking activated providing or requiring bundles with build errors
		// Activated requiring closure is not checked (see method comments)
		// Note that the bundles to activate are not activated yet.
		BundleBuildErrorClosure be = new BundleBuildErrorClosure(projects, Transition.ACTIVATE_PROJECT,
				Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.DEACTIVATED);
		if (be.hasBuildErrors(true)) {
			Collection<IProject> errorClosure = be.getBuildErrorClosures(true);
			try {
				if (messageOptions.isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			} catch (ExtenderException e) {
				addLogStatus(be.getErrorClosureStatus());
			}
			return errorClosure;
		}
		return Collections.<IProject> emptySet();
	}

	/**
	 * Initialize the workspace by stopping and uninstalling all workspace bundles and then adding the
	 * uninstalled bundles as pending projects. This only happens if the workspace is deactivated
	 * 
	 * @param monitor report progress on stop and uninstall bundle operations
	 * @return status object with {@code StatusCode.OK}. Otherwise the last failure code is returned.
	 * All statuses are added to the job status list
	 * @throws OperationCanceledException If user cancel job after uninstalling the bundles
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public IBundleStatus initWorkspace(IProgressMonitor monitor) throws OperationCanceledException,
			InterruptedException, CircularReferenceException {

		// Installed bundles are always registered in the workspace region (e.g. when installed from an
		// external source)
		final Collection<Bundle> installedBundles = bundleRegion.getBundles();

		// If the workspace is deactivated and there are registered bundles they are uninstalled but not
		// unregistered
		if (!isProjectWorkspaceActivated() && installedBundles.size() > 0) {
			Collection<IProject> projectsToActivate = bundleRegion.getProjects(installedBundles);
			uninstallBundles(installedBundles, monitor);
			// Subtract installed projects already pending
			projectsToActivate.removeAll(getPendingProjects());
			if (projectsToActivate.size() > 0) {
				addPendingProjects(projectsToActivate);
				if (messageOptions.isBundleOperations()) {
					addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
							Msg.ADD_BUNDLES_TO_ACTIVATE_INFO,
							bundleProjectCandidates.formatProjectList(projectsToActivate))));
				}
			}
		}
		return getLastErrorStatus();
	}

	/**
	 * Uninstalls the specified bundles.
	 * 
	 * @param bundlesToUninstall he bundles is first stopped and then uninstalled
	 * @param monitor report progress on stop and uninstall bundle operations
	 * @return status object with {@code StatusCode.OK}. Otherwise the last failure code is returned.
	 * All statuses are added to the job status list
	 * @throws OperationCanceledException If user cancel job after stopping and before uninstalling
	 * bundles
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InterruptedException Checks for and interrupts right before call to stop bundle. Stop
	 * is also interrupted if the task running the stop method is terminated abnormally (timeout or
	 * manually)
	 */
	private IBundleStatus uninstallBundles(Collection<Bundle> bundlesToUninstall,
			IProgressMonitor monitor) throws OperationCanceledException, InterruptedException,
			CircularReferenceException {
		int entrySize = errorStatusList();
		IBundleStatus status = stop(bundlesToUninstall, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		status = uninstall(bundlesToUninstall, new SubProgressMonitor(monitor, 1), true, false);
		if (!status.hasStatus(StatusCode.OK)) {
			String msg = ErrorMessage.getInstance().formatString("failed_uninstall_before_activate");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg);
			status = createMultiStatus(multiStatus);
		} else {
			if (messageOptions.isBundleOperations()) {
				addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
						Msg.UNINSTALL_BEFORE_ACTIVATE_INFO,
						bundleRegion.formatBundleList(bundlesToUninstall, true))));
			}
		}
		return errorStatusList() > entrySize ? getLastErrorStatus() : status;
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		if (!bundleRegion.isRegionActivated() && bundleTransition.containsPending(Transition.EXTERNAL)) {
			return 2;
		} else {
			return 1;
		}
	}
}
