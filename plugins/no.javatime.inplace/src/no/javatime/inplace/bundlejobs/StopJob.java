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
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class StopJob extends BundleJob implements Stop {

	/**
	 * Default constructor wit a default job name
	 */
	public StopJob() {
		super(Msg.STOP_JOB);
	}
	/**
	 * Construct a stop job with a given name
	 * 
	 * @param name job name
	 * @see Msg#STOP_JOB
	 */
	public StopJob(String name) {
		super(name);
	}

	/**
	 * Construct a stop job with a given name and bundle projects to stop
	 * 
	 * @param name job name
	 * @param projects pending bundle projects to stop
	 * @see Msg#STOP_JOB
	 */
	public StopJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a stop job with a given job name and a pending bundle project to stop
	 * 
	 * @param name job name
	 * @param project pending bundle project to stop
	 * @see Msg#STOP_JOB
	 */
	public StopJob(String name, IProject project) {
		super(name, project);
	}
	
	/**
	 * Runs the bundle(s) stop operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.STOP_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			stop(monitor);
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
			String msg = ExceptionMessage.getInstance().formatString("core_exception_job", getName());
			addError(e, msg);
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
	 * Stops bundles. BundleExecutor in state ACTIVE and STARTING are stopped. Calculates dependency closures according
	 * to dependency options and always adds requiring bundles.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of stopping with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after resolve
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus stop(IProgressMonitor monitor) throws
	InPlaceException, CoreException, InterruptedException, CircularReferenceException {

		Collection<Bundle> bundlesToStop = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		// Add bundles according to dependency options
		BundleClosures pd = new BundleClosures();
		bundlesToStop = pd.bundleDeactivation(bundlesToStop, activatedBundles);
		stop(bundlesToStop, null, new SubProgressMonitor(monitor, 1));
		// Warn about requiring bundles in state ACTIVE
		try {
			DependencyOptions dependencyOptions = Activator.getDependencyOptionsService();
			if (dependencyOptions.get(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING) 
					|| dependencyOptions.get(Operation.DEACTIVATE_BUNDLE, Closure.SINGLE)) {
				BundleSorter bs = new BundleSorter();
				for (Bundle bundle : bundlesToStop) {
					Collection<Bundle> requiringBundles = bs.sortRequiringBundles(Collections.<Bundle>singletonList(bundle),
							bundleRegion.getBundles(Bundle.ACTIVE | Bundle.STARTING));
					requiringBundles.remove(bundle);
					if (requiringBundles.size() > 0) {
						Collection<Bundle> providingBundles = bs.sortProvidingBundles(Collections.<Bundle>singletonList(bundle),
								bundleRegion.getBundles(Bundle.RESOLVED | Bundle.STOPPING));
						String msg =	WarnMessage.getInstance().formatString("has_stopped_requiring_bundles",
								bundleRegion.formatBundleList(requiringBundles, true), bundleRegion.formatBundleList(providingBundles, true)); 
						addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg));
					}
				}
			}
		} catch (ExtenderException e) {
			addError(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
		}
		return getLastErrorStatus();
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		return 1;
	}

}
