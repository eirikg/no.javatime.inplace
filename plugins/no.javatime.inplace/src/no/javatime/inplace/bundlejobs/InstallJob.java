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
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
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
import org.osgi.framework.Bundle;

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
			boolean isWorkspaceActivated = isProjectWorkspaceActivated();

			if (isWorkspaceActivated) {
				if (!bundleRegion.isRegionActivated()) {
					// First nature activated projects. Activate the workspace
					addPendingProjects(bundleProjectCandidates.getBundleProjects());
				}
				Collection<IProject> errorClosure = buildErrorClosure(getPendingProjects());
				if (errorClosure.size() > 0) {
					removePendingProjects(errorClosure);
				}
			}	else {
				// Must be able to install all projects or none in a deactivated workspace
				Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
				Collection<IProject> errorClosure = buildErrorClosure(projects);
				if (errorClosure.size() > 0) {
					return getLastErrorStatus();
				}
			}
			try {
				install(getPendingProjects(), monitor);
			} catch (InPlaceException | WorkspaceDuplicateException | ProjectLocationException e) {
				bundleTransition.addPendingCommand(getActivatedProjects(), Transition.DEACTIVATE);
				return addError(new BundleStatus(StatusCode.JOB_ERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));
			}

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
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
	 * Find and return build error closures among the specified deactivated projects to install.
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
		if (be.hasBuildErrors(false)) {
			Collection<IProject> errorClosure = be.getBuildErrorClosures(false);
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
}
