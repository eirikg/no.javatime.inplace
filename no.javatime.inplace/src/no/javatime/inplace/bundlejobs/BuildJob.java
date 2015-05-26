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
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Runs incremental or full build for a set of projects. A clean build is only applied to the the whole
 * workspace.
 */
public class BuildJob extends NatureJob {

	/** Either full or incremental. Default is full. */
	private int buildType = IncrementalProjectBuilder.FULL_BUILD;

	/**
	 * Construct a build job with a given job name
	 * 
	 * @param name job name
	 * @see Msg#FULL_BUILD_JOB
	 * @see Msg#INCREMENTAL_BUILD_JOB
	 * @see Msg#FULL_WORKSPACE_BUILD_JOB
	 */
	public BuildJob(String name) {
		super(name);
	}

	/**
	 * Construct a build job with a given job name and build type. A clean build is only applied to the whole
	 * workspace
	 * 
	 * @param name job name
	 * @param buildType
	 * @see Msg#FULL_BUILD_JOB
	 * @see Msg#INCREMENTAL_BUILD_JOB
	 * @see Msg#FULL_WORKSPACE_BUILD_JOB
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#FULL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#INCREMENTAL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#CLEAN_BUILD
	 */
	public BuildJob(String name, int buildType) {
		super(name);
		this.buildType = buildType;
	}

	/**
	 * Construct a build job with a given job name and build type for a set of specified projects A clean build
	 * is only applied to the whole workspace
	 * 
	 * @param name job name
	 * @param buildType
	 * @param projects to build according to the specified build type
	 * @see Msg#FULL_BUILD_JOB
	 * @see Msg#INCREMENTAL_BUILD_JOB
	 * @see Msg#FULL_WORKSPACE_BUILD_JOB
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#FULL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#INCREMENTAL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#CLEAN_BUILD
	 */
	public BuildJob(String name, int buildType, Collection<IProject> projects) {
		super(name, projects);
		this.buildType = buildType;
	}

	/**
	 * Runs the project(s) build operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.BUILD_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			if (buildType == IncrementalProjectBuilder.INCREMENTAL_BUILD) {
				incrementalBuild(monitor);
			} else {
				fullBuild(monitor);
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
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
	 * Runs a full build on pending project(s) or a full or a clean build on all projects in workspace if number
	 * of pending projects equals number of possible projects to install in workspace. Providing projects to
	 * projects to build are added to the build list.
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @throws CoreException if build fails
	 */
	private void fullBuild(IProgressMonitor monitor) throws CoreException {

		if (pendingProjects() == bundleProjectCandidates.getInstallable().size()
				|| IncrementalProjectBuilder.CLEAN_BUILD == buildType) {
			ResourcesPlugin.getWorkspace().build(buildType, new SubProgressMonitor(monitor, 1));
		} else {
			buildProjects(getPendingProjects(), buildType, Msg.BUILD_TASK_JOB, new SubProgressMonitor(monitor, 1));
		}
	}

	/**
	 * Runs an incremental build on pending projects.
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @throws CoreException if build fails
	 */
	private void incrementalBuild(IProgressMonitor monitor) throws CoreException {

		buildProjects(getPendingProjects(), buildType, Msg.BUILD_TASK_JOB, new SubProgressMonitor(monitor, 3));
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public static int getTicks() {
		return 1; // build
	}

}
