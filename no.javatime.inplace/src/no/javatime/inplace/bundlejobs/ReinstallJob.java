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
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Reinstalls pending projects. Projects are first uninstalled and then installed. Only pending bundle
 * projects in state INSTALLED are reinstalled and no dependency closures are calculated
 * 
 */
public class ReinstallJob extends NatureJob {

	/** Standard name of a start job */
	final public static String reinstallJobName = Message.getInstance().formatString("reinstall_job_name");
	/** Used to name the set of operations needed to reinstall a bundle */
	final public static String reinstallTaskName = Message.getInstance().formatString("reinstall_task_name");

	/**
	 * Construct a reinstall job with a given name
	 * 
	 * @param name job name
	 */
	public ReinstallJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and bundle projects to reinstall
	 * 
	 * @param name job name
	 * @param projects bundle projects to reinstall
	 */
	public ReinstallJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a job with a given name and bundle a project to reinstall
	 * 
	 * @param name job name
	 * @param project bundle project to reinstall
	 */
	public ReinstallJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) reinstall operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(reinstallTaskName, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			reinstall(monitor);
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
	 * Reinstalls bundle projects in state INSTALLED.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of reinstalling with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 */
	private IBundleStatus reinstall(IProgressMonitor monitor) {
		return reInstall(getPendingProjects(), new SubProgressMonitor(monitor, 1));
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		return UninstallJob.getTicks() + 1;
	}
}
