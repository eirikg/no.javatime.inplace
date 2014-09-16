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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
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
 * Refreshes pending projects. Active (state ACTIVE and STARTING) bundles are first stopped, refreshed and
 * then started. Requiring dependency closure is calculated and requiring bundles to pending bundles to
 * refresh are added as pending bundle projects.
 * 
 */
public class RefreshJob extends BundleJob {

	/** Standard name of a refresh job */
	final public static String refreshJobName = Message.getInstance().formatString("refresh_job_name");
	/** Used to name the set of operations needed to refresh a bundle */
	final protected static String refreshTaskName = Message.getInstance().formatString("refresh_task_name");

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
	 * @see #refreshJobName
	 */
	public RefreshJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a refresh job with a given job name and a pending bundle project to refresh
	 * 
	 * @param name job name
	 * @param project pending project to refresh
	 * @see #refreshJobName
	 */
	public RefreshJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle(s) refresh operation.
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
			monitor.beginTask(refreshTaskName, getTicks());
			refreshBundles(monitor);
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
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		} finally {
			monitor.done();
			BundleManager.removeBundleTransitionListener(this);
		}
	}

	/**
	 * Refresh bundles. Bundles in state ACYIVE and STARTING are stopped, refreshed and started. Calculates
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
		// Include requiring bundles in refresh
		Collection<Bundle> bundlesToRefresh = getBundlesToResolve(initialBundleSet);
		Collection<Bundle> errorBundles = removeTransitionErrorClosures(bundlesToRefresh, null, null);
		if (null != errorBundles) {
			String msg = ErrorMessage.getInstance().formatString("bundle_errors_refresh", bundleRegion.formatBundleList(errorBundles, false));
			addError(null, msg);
		}

		if (containsBuildErrorClosures(bundlesToRefresh)) {
			throw new OperationCanceledException();
		}
		Collection<Bundle> bundlesToRestart = new LinkedHashSet<Bundle>();		
		for (Bundle bundle : bundlesToRefresh) {
			if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
				bundlesToRestart.add(bundle);
			}
		}
		stop(bundlesToRestart, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		refresh(bundlesToRefresh, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		start(bundlesToRestart, Closure.PROVIDING,
				new SubProgressMonitor(monitor, 1));
		return getLastStatus();
	}
	
	/**
	 * If there are any activated bundles with build errors, terminate to avoid
	 * the resolver to try to resolve bundles with build errors
	 * 
	 * @param bundlesToRefresh bundles to check for error closures. Error closures are removed
	 * from this collection of bundles to refresh
	 * @return true if any build error closures are detected. Otherwise false
	 */
	private boolean containsBuildErrorClosures(Collection<Bundle> bundlesToRefresh) {
		boolean containsErrorClosures = false;

		Collection<IProject> projectsToRefresh = bundleRegion.getBundleProjects(bundlesToRefresh);
		BuildErrorClosure be = new BuildErrorClosure(projectsToRefresh, 
				Transition.REFRESH, Closure.REQUIRING, Bundle.RESOLVED, ActivationScope.ACTIVATED);
		if (be.hasBuildErrors()) {
			Collection<IProject> buildErrClosures = be.getBuildErrorClosures();
			bundlesToRefresh.removeAll(bundleRegion.getBundles(buildErrClosures));
			IBundleStatus bundleStatus = be.getErrorClosureStatus();
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				addStatus(bundleStatus);			
			}
			containsErrorClosures = true;
		}
		if (!containsErrorClosures) {
			be = new BuildErrorClosure(projectsToRefresh, 
					Transition.REFRESH, Closure.PROVIDING, Bundle.RESOLVED, ActivationScope.ACTIVATED);
			if (be.hasBuildErrors()) {
				Collection<IProject> buildErrClosures = be.getBuildErrorClosures();
				bundlesToRefresh.removeAll(bundleRegion.getBundles(buildErrClosures));
				IBundleStatus bundleStatus = be.getErrorClosureStatus();
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					addStatus(bundleStatus);			
				}
				containsErrorClosures = true;
			}
		}
		
		return containsErrorClosures;
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
