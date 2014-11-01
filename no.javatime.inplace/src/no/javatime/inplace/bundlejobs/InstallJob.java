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
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Installs pending projects in state UNINSTALLED. Installed bundle projects are moved to state INSTALLED.
 * <p>
 * If this is the first set of pending projects to install, all candidate bundle projects in the workspace are
 * installed. Otherwise only the pending projects are installed.
 * 
 * @see no.javatime.inplace.bundlejobs.UninstallJob
 */
public class InstallJob extends BundleJob {

	/** Standard name of an install job */
	final public static String installJobName = Message.getInstance().formatString("install_job_name");
	/** Used to name the set of operations needed to install a bundle */
	final private static String installTaskName = Message.getInstance().formatString("install_task_name");
	final private static String duplicateMessage = ErrorMessage.getInstance().formatString("duplicate_ws_bundle_install");

	/**
	 * Construct an install job with a given name
	 * 
	 * @param name job name
	 */
	public InstallJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to install
	 * 
	 * @param name job name
	 * @param projects projects to install
	 */
	public InstallJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct an install job with a given name and a bundle project to install
	 * 
	 * @param name job name
	 * @param project bundle project to install
	 */
	public InstallJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) install operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(installTaskName, 1);
			BundleTransitionListener.addBundleTransitionListener(this);
		if (bundleProject.isWorkspaceNatureEnabled()) {
				if (!bundleRegion.isBundleWorkspaceActivated()) {
					// First nature activated projects. Activate the workspace
					addPendingProjects(bundleProject.getPlugIns());
				}
			}
			ProjectSorter projectSorter = new ProjectSorter();
			try {
//				resetPendingProjects(projectSorter.sortProvidingProjects(getPendingProjects()));
			} catch (CircularReferenceException e) {
				String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
				BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
				multiStatus.add(e.getStatusList());
				addStatus(multiStatus);
				// Remove all pending projects that participate in the cycle(s)
				if (null != e.getProjects()) {
					removePendingProjects(e.getProjects());
				}
			}
			removeErrorClosures(projectSorter, null);
			install(getPendingProjects(), monitor);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
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
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
	}
	/**
	 * Remove build error closure bundles from the set of activated projects. If there are no activated projects
	 * left the error closures are added to the status list. Otherwise the error closures are only removed from
	 * the set of pending projects.
	 * 
	 * @param projectSorter topological sort of error closure
	 * @return {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. Return
	 *         {@code IBundleStatus} status with a {@code StatusCode.BUILDERROR} if there are projects with
	 *         build errors and there are no pending projects left. If there are build errors they are added to
	 *         the job status list.
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	private IBundleStatus removeErrorClosures(ProjectSorter projectSorter, Collection<Bundle> installedBundles) throws InPlaceException {

		IBundleStatus status = createStatus();
		Collection<IProject> projectErrorClosures = null;
		try {
			BuildErrorClosure be = new BuildErrorClosure(getPendingProjects(), 
					Transition.INSTALL, Closure.REQUIRING);
			if (be.hasBuildErrors()) {
				projectErrorClosures = be.getBuildErrorClosures();
				removePendingProjects(projectErrorClosures);
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					IBundleStatus bundleStatus = be.getErrorClosureStatus();
					if (null != bundleStatus) {
						addLogStatus(bundleStatus);			
					}
				}
			}
			bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
			removeExternalDuplicates(getPendingProjects(), null, null);
			Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null, 
					bundleProject.getInstallable(), duplicateMessage);
			if (null != duplicates) {
				Collection<IProject> installedRequirers = projectSorter.sortRequiringProjects(duplicates, true);
				if (installedRequirers.size() > 0) {
					removePendingProjects(installedRequirers);
				}
			}

		} catch (CircularReferenceException e) {
			projectErrorClosures = BuildErrorClosure.getBuildErrors(getPendingProjects());
			projectErrorClosures.addAll(BuildErrorClosure.hasBuildState(getPendingProjects()));
			if (projectErrorClosures.size() > 0) {
				removePendingProjects(projectErrorClosures);
				if (null != installedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(projectErrorClosures);
					installedBundles.removeAll(bundleErrorClosure);
				}
			}
		}
		return status;
	}
}
