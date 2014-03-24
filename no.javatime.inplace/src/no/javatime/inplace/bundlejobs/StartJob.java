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
import java.util.EnumSet;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;

/**
 * Starts pending bundle projects with an initial state of INSTALLED, RESOLVED and STOPPING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this job before the bundles are
 * started according to the current dependency option. Providing bundles to pending bundle projects are always
 * added as pending bundle projects before bundles are started.
 */
public class StartJob extends BundleJob {

	/** Standard name of a start job */
	final public static String startJobName = Message.getInstance().formatString("start_job_name");
	/** Used to name the set of operations needed to start a bundle */
	final private static String startTaskName = Message.getInstance().formatString("start_task_name");
	/** The name of the operation to start a bundle */
	final protected static String startSubTaskName = Message.getInstance().formatString("start_subtask_name");

	/**
	 * Constructs a start job with a given name
	 * 
	 * @param name job name
	 * @see #startJobName
	 */
	public StartJob(String name) {
		super(name);
	}

	/**
	 * Constructs a start job with a given name and pending bundle projects to start
	 * 
	 * @param name job name
	 * @param projects pending projects to start
	 * @see #startJobName
	 */
	public StartJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs a start job with a given job name and a pending bundle project to start
	 * 
	 * @param name job name
	 * @param project pending project to start
	 * @see #startJobName
	 */
	public StartJob(String name, IProject project) {
		super(name, project);
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
			monitor.beginTask(startTaskName, getTicks());
			start(monitor);
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
	 * Starts bundles. Bundles in state INSTALLED are resolved before stated. Calculates dependency closures
	 * according to dependency options and always adds providing bundles.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of starting with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after resolve
	 * @throws InterruptedException typically thrown in user start method of the bundle to start
	 */
	private IBundleStatus start(IProgressMonitor monitor) throws OperationCanceledException, InterruptedException {

		Collection<Bundle> bundlesToStart = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> bundlesToResolve = null;
		BundleSorter bs = new BundleSorter();
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();

		// Add bundles according to dependency options
		// If all activated bundles are selected, no dependency option is needed
		if (bundlesToStart.size() == activatedBundles.size()) {
			bundlesToStart = bs.sortProvidingBundles(bundlesToStart, bundlesToStart);
		} else if (Category.getState(Category.partialGraphOnStart)) {
			int count = 0;
			do {
				count = bundlesToStart.size();
				bundlesToStart = bs.sortRequiringBundles(bundlesToStart, activatedBundles);
				bundlesToStart = bs.sortProvidingBundles(bundlesToStart, activatedBundles);
			} while (bundlesToStart.size() > count);
		} else {
			// When both requiring and providing on start is enabled this is the same as
			// the requiring and providing option
			if (Category.getState(Category.requiringOnStart)) {
				bundlesToStart = bs.sortRequiringBundles(bundlesToStart, activatedBundles);
			}
			if (Category.getState(Category.providingOnStart)) {
				bundlesToStart = bs.sortProvidingBundles(bundlesToStart, activatedBundles);
			} else {
				// No options set (or same as single). Just sort the bundles
				bundlesToStart = bs.sortProvidingBundles(bundlesToStart, bundlesToStart);
			}
		}
		Collection<Bundle> notResolvedBundles = new LinkedHashSet<Bundle>();
		for (Bundle bundle : bundlesToStart) {
			int state = bundle.getState();
			if ((state & (Bundle.UNINSTALLED | Bundle.STARTING | Bundle.ACTIVE)) != 0) {
				notResolvedBundles.add(bundle);
				// These comes from install and there should be no need to refresh
			} else if ((state & (Bundle.INSTALLED)) != 0) {
				Collection<Bundle> errorBundles = removeTransitionErrorClosures(bundlesToStart, null, null);
				if (null != errorBundles) {
					String msg = ErrorMessage.getInstance().formatString("bundle_errors_start", bundleRegion.formatBundleList(errorBundles, false));
					addError(null, msg);
					notResolvedBundles.addAll(errorBundles);
				} else {
					// Resolve and start in two steps, to control the resolve process before starting
					if (null == bundlesToResolve) {
						bundlesToResolve = new LinkedHashSet<Bundle>();
					}
					bundlesToResolve.add(bundle);
				}
			}
		}
		if (null != bundlesToResolve) {
			notResolvedBundles.addAll(resolve(bundlesToResolve, new SubProgressMonitor(monitor, 1)));
			if (notResolvedBundles.size() > 0) {
				// This should include dependency closures, so no dependent bundles should be started
				bundlesToStart.removeAll(notResolvedBundles);
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		start(bundlesToStart, EnumSet.of(Integrity.RESTRICT), new SubProgressMonitor(
				monitor, 1));
		return getLastStatus();
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		// Check if to resolve before start
		Collection<Bundle> bundles = bundleRegion.getBundles(getPendingProjects());
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
				return 2;
			}
		}
		return 1;
	}
}