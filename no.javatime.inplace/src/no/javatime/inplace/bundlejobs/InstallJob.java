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

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;

public class InstallJob extends NatureJob implements Install {

	/**
	 * Default constructor wit a default job name
	 */
	public InstallJob() {
		super(Msg.INSTALL_JOB);
	}

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
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.INSTALL_TASK_JOB, 1);
			BundleTransitionListener.addBundleTransitionListener(this);
			if (isProjectWorkspaceActivated()) {
				if (!bundleRegion.isRegionActivated()) {
					// First nature activated projects. Activate the workspace
					addPendingProjects(bundleProjectCandidates.getBundleProjects());
				}
			}
			removeErrorClosures(new ProjectSorter());
			install(getPendingProjects(), monitor);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
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
	 * Remove build and duplicate error closures from the set of pending projects. Error closures
	 * status objects are also added to the log status list if logging is enabled.
	 * 
	 * @param projectSorter Topological sort of projects
	 * @return Status object with a {@code StatusCode.OK} if no duplicates and build errors and a
	 * status object of {@code StatusCode.BUILDERROR} if duplicates and/or build errors.
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	private IBundleStatus removeErrorClosures(ProjectSorter projectSorter) throws InPlaceException {

		IBundleStatus status = createStatus();

		Collection<IProject> projectErrorClosures = null;
		try {
			BuildErrorClosure be = new BuildErrorClosure(getPendingProjects(), Transition.INSTALL,
					Closure.REQUIRING);
			if (be.hasBuildErrors()) {
				projectErrorClosures = be.getBuildErrorClosures();
				status.setStatusCode(StatusCode.BUILDERROR);
				removePendingProjects(projectErrorClosures);
				if (messageOptions.isBundleOperations()) {
					IBundleStatus bundleStatus = be.getErrorClosureStatus();
					if (null != bundleStatus) {
						addLogStatus(bundleStatus);
					}
				}
			}
			bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
			Collection<IProject> externalDuplicates = getExternalDuplicateClosures(getPendingProjects(), null);
			if (null != externalDuplicates) {
				removePendingProjects(externalDuplicates);
			}
			Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null,
					bundleProjectCandidates.getInstallable(), Msg.DUPLICATE_WS_BUNDLE_INSTALL_ERROR);
			if (null != duplicates) {
				status.setStatusCode(StatusCode.BUILDERROR);
				Collection<IProject> requiringBundles = projectSorter.sortRequiringProjects(duplicates,
						true);
				if (requiringBundles.size() > 0) {
					removePendingProjects(requiringBundles);
				}
			}

		} catch (CircularReferenceException e) {
			projectErrorClosures = BuildErrorClosure.getBuildErrors(getPendingProjects());
			projectErrorClosures.addAll(BuildErrorClosure.hasBuildState(getPendingProjects()));
			if (projectErrorClosures.size() > 0) {
				status.setStatusCode(StatusCode.BUILDERROR);
				removePendingProjects(projectErrorClosures);
			}
		}
		return status;
	}
}
