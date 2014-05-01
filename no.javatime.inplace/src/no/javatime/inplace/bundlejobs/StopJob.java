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
import java.util.EnumSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.PartialDependencies;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

/**
 * Stops pending bundle projects with an initial state of ACTIVE and STARTING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this job before the bundles are
 * stopped according to the current dependency option. Requiring bundles to pending bundle projects are always
 * added as pending bundle projects before bundles are stopped.
 */
public class StopJob extends BundleJob {

	/** Standard name of a stop job */
	final public static String stopJobName = Message.getInstance().formatString("stop_job_name");
	/** Used to name the set of operations needed to stop a bundle */
	final private static String stopTaskName = Message.getInstance().formatString("stop_task_name");
	/** The name of the operation to stop a bundle */
	final protected static String stopSubTaskName = Message.getInstance().formatString("stop_subtask_name");

	/**
	 * Construct a stop job with a given name
	 * 
	 * @param name job name
	 * @see #stopJobName
	 */
	public StopJob(String name) {
		super(name);
	}

	/**
	 * Construct a stop job with a given name and bundle projects to stop
	 * 
	 * @param name job name
	 * @param projects pending bundle projects to stop
	 * @see #stopJobName
	 */
	public StopJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a stop job with a given job name and a pending bundle project to stop
	 * 
	 * @param name job name
	 * @param project pending bundle project to stop
	 * @see #stopJobName
	 */
	public StopJob(String name, IProject project) {
		super(name, project);
	}
	
	public StopJob(String name, Bundle bundle) {
		super(name, BundleProject.getProject(bundle));
	}

	/**
	 * Runs the bundle(s) start operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(stopTaskName, getTicks());
			stop(monitor);
		} catch(InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
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
		} catch (CoreException e) {
			String msg = ExceptionMessage.getInstance().formatString("core_exception_job", getName());
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
	 * Stops bundles. Bundles in state ACTIVE and STARTING are stopped. Calculates dependency closures according
	 * to dependency options and always adds requiring bundles.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of stopping with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after resolve
	 */
	private IBundleStatus stop(IProgressMonitor monitor) throws InPlaceException, CoreException, InterruptedException {

		Collection<Bundle> bundlesToStop = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		// Add bundles according to dependency options
		PartialDependencies pd = new PartialDependencies();
		bundlesToStop = pd.bundleDeactivationDependencies(bundlesToStop, activatedBundles);
		stop(bundlesToStop, EnumSet.of(Closure.SINGLE), new SubProgressMonitor(monitor, 1));
		// Warn about requiring bundles in state ACTIVE
		if (!Category.getState(Category.partialGraphOnStop) && !Category.getState(Category.requiringOnStop)) {
			BundleSorter bs = new BundleSorter();
			for (Bundle bundle : bundlesToStop) {
				Collection<Bundle> requiringBundles = bs.sortRequiringBundles(Collections.singletonList(bundle),
						bundleRegion.getBundles(Bundle.ACTIVE | Bundle.STARTING));
				requiringBundles.remove(bundle);
				if (requiringBundles.size() > 0) {
					Collection<Bundle> providingBundles = bs.sortProvidingBundles(Collections.singletonList(bundle),
							bundleRegion.getBundles(Bundle.RESOLVED | Bundle.STOPPING));
					WarnMessage.getInstance().getString("has_stopped_requiring_bundles",
							bundleRegion.formatBundleList(requiringBundles, true), bundleRegion.formatBundleList(providingBundles, true)); 
				}
			}
		}
		return getLastStatus();
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
