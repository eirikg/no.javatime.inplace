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
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.Refresh;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
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

public class RefreshJob extends BundleJob implements Refresh {

	/**
	 * Default constructor wit a default job name
	 */
	public RefreshJob() {
		super(Msg.REFRESH_JOB);
	}
	/**
	 * Construct a refresh job with a given name
	 * 
	 * @param name job name
	 */
	public RefreshJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and pending bundle projects to refresh
	 * 
	 * @param name job name
	 * @param projects pending projects to refresh
	 * @see Msg#REFRESH_JOB
	 */
	public RefreshJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a refresh job with a given job name and a pending bundle project to refresh
	 * 
	 * @param name job name
	 * @param project pending project to refresh
	 * @see Msg#REFRESH_JOB
	 */
	public RefreshJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle(s) refresh operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(Msg.REFRESH_TASK_JOB, getTicks());
			refreshBundles(monitor);
		} catch(InterruptedException e) {
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
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors", getName());
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
	 * Refresh bundles. BundleExecutor in state ACYIVE and STARTING are stopped, refreshed and started. Calculates
	 * dependency closures according to dependency options and always adds requiring bundles.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of refreshing with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after stop and refresh
	 * @throws InterruptedException if job is being interrupted
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus refreshBundles(IProgressMonitor monitor) throws InPlaceException, InterruptedException, CircularReferenceException {

		// Stopped bundles are started after refresh
		Collection<Bundle> initialBundleSet = bundleRegion.getBundles(getPendingProjects());
		// Get the requiring closure of bundles to refresh and add bundles to the requiring closure
		// with same symbolic name as in the refresh closure.
		Collection<Bundle> bundlesToRefresh = getRefreshClosure(initialBundleSet, bundleRegion.getActivatedBundles());
		Collection<Bundle> bundlesToRestart = new LinkedHashSet<Bundle>();		
		for (Bundle bundle : bundlesToRefresh) {
			if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
				bundlesToRestart.add(bundle);
			}
		}
		// Ok to stop/refresh/start with build errors
		stop(bundlesToRestart, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		refresh(bundlesToRefresh, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		start(bundlesToRestart, Closure.PROVIDING,
				new SubProgressMonitor(monitor, 1));
		return getLastErrorStatus();
	}
	
	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		return 3;
	}

}
