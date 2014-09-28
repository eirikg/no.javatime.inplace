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
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.InPlaceException;
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
import org.osgi.framework.Bundle;

/**
 * Uninstalls pending bundle projects and all requiring bundles of the pending bundle projects from any state
 * except UNINSTALLED.
 * 
 * @see no.javatime.inplace.bundlejobs.InstallJob
 */
public class UninstallJob extends NatureJob {

	/** Standard name of an uninstall job */
	final public static String uninstallJobName = Message.getInstance().formatString("uninstall_job_name");
	/** Can be used at IDE shut down */
	final public static String shutDownJobName = Message.getInstance().formatString("shutDown_job_name");
	/** Used to name the set of operations needed to uninstall a bundle */
	final private static String uninstallTaskName = Message.getInstance().formatString("uninstall_task_name");

	// Remove the bundle project from the workspace region 
	private boolean unregisterBundleProject = false;
	private boolean includeRequiring = true;

	public boolean isIncludeRequiring() {
		return includeRequiring;
	}

	public void setIncludeRequiring(boolean includeRequiring) {
		this.includeRequiring = includeRequiring;
	}

	/**
	 * Construct an uninstall job with a given name
	 * 
	 * @param name job name
	 */
	public UninstallJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and bundle projects to uninstall
	 * 
	 * @param name job name
	 * @param projects bundle projects to uninstall
	 */
	public UninstallJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs an uninstall job with a given name and a bundle project to uninstall
	 * 
	 * @param name job name
	 * @param project bundle project to uninstall
	 */
	public UninstallJob(String name, IProject project) {
		super(name, project);
	}

	public boolean isBundleProjectUnregistered() {
		return unregisterBundleProject;
	}

	public void unregisterBundleProject(boolean unregister) {
		this.unregisterBundleProject = unregister;
	}

	/**
	 * Runs the bundle(s) uninstall operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(uninstallTaskName, getTicks());
			BundleManager.addBundleTransitionListener(this);
			uninstall(monitor);
		} catch(InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
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
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			monitor.done();
			BundleManager.removeBundleTransitionListener(this);
		}
	}

	/**
	 * Stops, uninstalls and refreshes a set of pending bundle projects and all bundle projects requiring
	 * capabilities from the pending bundle projects.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of uninstalling with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after stop and uninstall
	 */
	private IBundleStatus uninstall(IProgressMonitor monitor) throws InterruptedException{

		Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
		if (pendingBundles.size() == 0) {
			return getLastStatus();
		}
		Collection<Bundle> bundlesToUninstall = null;
		if (includeRequiring){
			BundleSorter bs = new BundleSorter();
			bs.setAllowCycles(Boolean.TRUE);
			bundlesToUninstall = bs.sortDeclaredRequiringBundles(pendingBundles, bundleRegion.getBundles());
		} else {
			bundlesToUninstall = pendingBundles;
		}
		stop(bundlesToUninstall, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		uninstall(bundlesToUninstall, new SubProgressMonitor(monitor, 1), unregisterBundleProject);
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		return getLastStatus();
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public static int getTicks() {
		return 3; // Stop, Uninstall and Refresh
	}

}
