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
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.Start;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class StartJob extends BundleJob implements Start {

	/**
	 * Default constructor wit a default job name
	 */
	public StartJob() {
		super(Msg.START_JOB);
	}
	
	/**
	 * Constructs a start job with a given name
	 * 
	 * @param name job name
	 * @see Msg#START_JOB
	 */
	public StartJob(String name) {
		super(name);
	}

	/**
	 * Constructs a start job with a given name and pending bundle projects to start
	 * 
	 * @param name job name
	 * @param projects pending projects to start
	 * @see Msg#START_JOB
	 */
	public StartJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs a start job with a given job name and a pending bundle project to start
	 * 
	 * @param name job name
	 * @param project pending project to start
	 * @see Msg#START_JOB
	 */
	public StartJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle(s) start operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.START_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			start(monitor);
		} catch(InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
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
	 * Starts bundles. BundleExecutor in state INSTALLED are resolved before stated. Calculates dependency closures
	 * according to dependency options and always adds providing bundles.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of starting with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @throws OperationCanceledException after resolve
	 * @throws InterruptedException typically thrown in user start method of the bundle to start
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus start(IProgressMonitor monitor) throws 
			OperationCanceledException, InterruptedException, CircularReferenceException {

		Collection<Bundle> bundlesToResolve = null;
		Collection<Bundle> bundlesToStart = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		// Add bundles according to dependency options
		BundleClosures bundleClosure = new BundleClosures();
		bundlesToStart = bundleClosure.bundleActivation(bundlesToStart, activatedBundles);
		Collection<Bundle> notResolvedBundles = new LinkedHashSet<Bundle>();
		for (Bundle bundle : bundlesToStart) {
			int state = bundle.getState();
			if ((state & (Bundle.UNINSTALLED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE)) != 0) {
				notResolvedBundles.add(bundle);
				// These comes from install and there should be no need to refresh
			} else if ((state & (Bundle.INSTALLED)) != 0) {
//				Collection<Bundle> errorBundles = removeTransitionErrorClosures(bundlesToStart);
//				if (null != errorBundles) {
//					String msg = ErrorMessage.getInstance().formatString("bundle_errors_start", bundleRegion.formatBundleList(errorBundles, false));
//					addError(null, msg);
//					notResolvedBundles.addAll(errorBundles);
//				} else {
					// Resolve and start in two steps, to control the resolve process before starting
					if (null == bundlesToResolve) {
						bundlesToResolve = new LinkedHashSet<Bundle>();
					}
					bundlesToResolve.add(bundle);
//				}
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
		start(bundlesToStart, null, new SubProgressMonitor(monitor, 1));
		// Warn about missing providing closures in state RESOLVED
		try {
			DependencyOptions dependencyOptions = Activator.getDependencyOptionsService();
			if (dependencyOptions.get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING)
					|| dependencyOptions.get(Operation.ACTIVATE_BUNDLE, Closure.SINGLE)) {
				BundleSorter bs = new BundleSorter();
				for (Bundle bundle : bundlesToStart) {
					Collection<Bundle> providingBundles = bs.sortProvidingBundles(Collections.<Bundle>singletonList(bundle),
							bundleRegion.getBundles(Bundle.RESOLVED | Bundle.STOPPING));
					providingBundles.remove(bundle);
					if (providingBundles.size() > 0) {
						Collection<Bundle> requiringBundles = bs.sortRequiringBundles(Collections.<Bundle>singletonList(bundle),
								bundleRegion.getBundles(Bundle.ACTIVE | Bundle.STARTING));
						String msg = WarnMessage.getInstance().formatString("has_started_providing_bundles",
								bundleRegion.formatBundleList(providingBundles, true), bundleRegion.formatBundleList(requiringBundles, true));
						addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg));
					}
				}
			}
		} catch (ExtenderException e) {
			addStatus(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
		}
		return getLastErrorStatus();
	}

	/**
	 * Remove all bundles from the specified set of bundles tagged with a transition error and their providing bundles
	 *  
	 * @param initialBundleSet set of bundles to remove transition errors from
	 * @return reduced set of the specified initial set without bundles with transition errors and their providing bundles 
	 */
	@SuppressWarnings("unused")
	private Collection<Bundle> removeTransitionErrorClosures(Collection<Bundle> initialBundleSet) {

		BundleSorter bs = new BundleSorter();
		Collection<Bundle> workspaceBundles = bundleRegion.getActivatedBundles();
		Collection<Bundle> bErrorDepClosures = bs.sortDeclaredProvidingBundles(initialBundleSet,
				workspaceBundles);
		Collection<Bundle> bundles = null;
		for (Bundle errorBundle : bErrorDepClosures) {
			IProject errorProject = bundleRegion.getProject(errorBundle);
			TransitionError transitionError = bundleTransition.getBuildError(errorBundle);
			if (null != errorProject
					&& (transitionError == TransitionError.WORKSPACE_DUPLICATE || transitionError == TransitionError.CYCLE)) {
				if (null == bundles) {
					bundles = new LinkedHashSet<Bundle>();
				}
				bundles.addAll(bs.sortDeclaredRequiringBundles(
						Collections.<Bundle> singletonList(errorBundle), workspaceBundles));
			}
		}
		if (null != bundles) {
			initialBundleSet.removeAll(bundles);
		}
		return bundles;
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
