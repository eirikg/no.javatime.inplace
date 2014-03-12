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
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
			if (ProjectProperties.isProjectWorkspaceActivated()) {
				if (!bundleRegion.isBundleWorkspaceActivated()) {
					// First nature activated projects. Activate the workspace
					addPendingProjects(ProjectProperties.getPlugInProjects());
				}
			}
			ProjectSorter projectSorter = new ProjectSorter();
			try {
				replacePendingProjects(projectSorter.sortProvidingProjects(getPendingProjects()));
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
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			monitor.done();
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
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
	 */
	private IBundleStatus removeErrorClosures(ProjectSorter projectSorter, Collection<Bundle> installedBundles) {

		IBundleStatus status = createStatus();
		Collection<IProject> projectErrorClosures = null;
		try {
			projectErrorClosures = projectSorter.getRequiringBuildErrorClosure(getPendingProjects());
			if (projectErrorClosures.size() > 0) {
				String msg = ProjectProperties.formatBuildErrorsFromClosure(projectErrorClosures, getName());
				if (null != msg) {
					status = addBuildError(msg, null);
				}
				removePendingProjects(projectErrorClosures);
			}
			bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
			removeExternalDuplicates(getPendingProjects(), null, null);
			Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null, 
					ProjectProperties.getInstallableProjects(), duplicateMessage);
			if (null != duplicates) {
				Collection<IProject> installedRequirers = projectSorter.sortRequiringProjects(duplicates, true);
				if (installedRequirers.size() > 0) {
					removePendingProjects(installedRequirers);
				}
			}

		} catch (CircularReferenceException e) {
			projectErrorClosures = ProjectProperties.getBuildErrors(getPendingProjects());
			projectErrorClosures.addAll(ProjectProperties.hasBuildState(getPendingProjects()));
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
