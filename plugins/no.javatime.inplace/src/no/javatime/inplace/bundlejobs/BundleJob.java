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

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleActivatorException;
import no.javatime.inplace.region.intface.BundleStateChangeException;
import no.javatime.inplace.region.intface.BundleThread;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.progress.IProgressConstants2;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Support processing of bundle operations on bundles added as pending projects to bundle jobs.
 * <p>
 * Pending bundle projects are added to a job before it is scheduled and the bundle projects are
 * executed according to type of bundle job.
 */
public class BundleJob extends JobStatus implements BundleExecutor {

	/**
	 * Unsorted list of unique pending projects to process in a job
	 */
	private Collection<IProject> pendingProjects;

	/**
	 * Initialized according to preference setting. 
	 * 
	 * If true a workspace snapshot should be saved before running this job
	 */
	private boolean isSaveWorkspaceSnaphot;

	/**
	 * Construct a bundle job with a bundle name. Sets job priority and scheduling rule.
	 * 
	 * @param name of the job to process
	 */
	public BundleJob(String name) {
		super(name);
		init();
	}

	/**
	 * Construct a bundle job with a name and pending bundle projects to perform bundle operations on.
	 * Sets job priority and scheduling rule.
	 * 
	 * @param name of the job to run
	 * @param projects pending bundle projects to perform bundle operations on
	 */
	public BundleJob(String name, Collection<IProject> projects) {
		super(name);
		init();
		addPendingProjects(projects);
	}

	/**
	 * Constructs a bundle job with a name and a bundle project to perform bundle operations on. Sets
	 * job priority and scheduling rule.
	 * 
	 * @param name of the job to run
	 * @param project pending bundle project to perform bundle operations on
	 */
	public BundleJob(String name, IProject project) {
		super(name);
		init();
		addPendingProject(project);
	}
	
	private void init() {
		setPriority(Job.BUILD);
		if (!bundleRule().equals(getRule())) {
			setRule(bundleRule());
		}
		pendingProjects = new LinkedHashSet<>();
		setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);	
		try {
			if (null == commandOptions) {
				commandOptions = Activator.getCommandOptionsService();
			}
			isSaveWorkspaceSnaphot = commandOptions.isSaveSnapshotBeforeBundleOperation();
		} catch (ExtenderException e) {
			isSaveWorkspaceSnaphot = false;
		}
	}

	/**
	 * Use the same rule as the build job locking the whole workspace while the job is running
	 * 
	 * @return The workspace root scheduling rule
	 */
	public ISchedulingRule bundleRule() {

		return  ResourcesPlugin.getWorkspace().getRuleFactory()
				.buildRule();
	}

	/**
	 * Runs the bundle(s) operation.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException, ExtenderException {

		return super.runInWorkspace(monitor);
	}
	
	/**
	 * Determine if this job belongs to the bundle family
	 * 
	 * @return true if the specified object == {@code Bundle.FAMILY_BUNDLE_LIFECYCLE} or is of type
	 * {@code BundleJob}, otherwise false
	 */
	@Override
	public boolean belongsTo(Object family) {
		if (family == BundleExecutor.FAMILY_BUNDLE_LIFECYCLE || family instanceof BundleJob) {
			return true;
		}
		return false;
	}
	
	public boolean isSaveWorkspaceSnaphot() {
		return isSaveWorkspaceSnaphot;
	}

	public void setSaveWorkspaceSnaphot(boolean saveWorkspaceSnaphot) {
		this.isSaveWorkspaceSnaphot = saveWorkspaceSnaphot;
	}

	@Override
	public void addPendingProject(IProject project) {
		pendingProjects.add(project);
	}

	@Override
	public void addPendingProjects(Collection<IProject> projects) {
		pendingProjects.addAll(projects);
	}

	@Override
	public void resetPendingProjects(Collection<IProject> projects) {
		pendingProjects = new LinkedHashSet<IProject>(projects);
	}

	@Override
	public void removePendingProject(IProject project) {
		pendingProjects.remove(project);
	}

	@Override
	public void removePendingProjects(Collection<IProject> projects) {
		pendingProjects.removeAll(projects);
	}

	@Override
	public void clearPendingProjects() {
		pendingProjects.clear();
	}

	@Override
	public Boolean isPendingProject(IProject project) {
		return this.pendingProjects.contains(project);
	}

	@Override
	public Boolean hasPendingProjects() {
		return !pendingProjects.isEmpty();
	}

	@Override
	public Collection<IProject> getPendingProjects() {
		return pendingProjects;
	}

	@Override
	public int pendingProjects() {
		return pendingProjects.size();
	}
	
	/**
	 * Start the specified bundles. If the activation policy for a bundle is lazy the bundle is
	 * activated according to the declared activation policy. If the activation policy is eager, the
	 * bundle is started transient. Only bundles in state RESOLVED and STOPPING are started.
	 * <p>
	 * If the the specified integrity rule is not set to providing, the specified bundles should be
	 * sorted in providing order. Each bundle in the specified collection of bundles to start should
	 * be part of a providing dependency closure.
	 * <p>
	 * Requiring bundles to bundles that fail to start are not started.
	 * <p>
	 * If the task running the start operation is terminated the bundle and the requiring bundles are
	 * deactivated if the deactivate on terminate option is switched on.
	 * 
	 * @param bundles the set of bundles to start
	 * @param closure if rule is {@code Closure.PROVIDING} include providing bundles to start
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of starting with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws CircularReferenceException if the integrity parameter contains providing and cycles are
	 * detected in the project graph
	 * @throws InterruptedException Checks for and interrupts right before call to start bundle. Start
	 * is also interrupted if the task running the stop method is terminated abnormally (timeout or
	 * manually)
	 * @throws InPlaceException illegal closure for activate bundle operation
	 */
	protected IBundleStatus start(Collection<Bundle> bundles, Closure closure, IProgressMonitor monitor)
			throws InterruptedException, InPlaceException {

		IBundleStatus result = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, "");
		Collection<Bundle> exceptionBundles = null;

		if (null != bundles && bundles.size() > 0) {
			if (null != closure) {
				BundleClosures bc = new BundleClosures();
				bundles = bc.bundleActivation(closure, bundles, bundleRegion.getActivatedBundles());
			}
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			boolean timeout = true;
			long timeoutVal = 5000;
			try {
				timeout = commandOptions.isTimeOut();
				if (timeout) {
					timeoutVal = getTimeout(timeout);
				}
			} catch (InPlaceException e) {
				addError(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
			}
			for (Bundle bundle : bundles) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					localMonitor.subTask(NLS.bind(Msg.START_SUB_TASK_JOB, bundle.getSymbolicName()));
					if (null != exceptionBundles) {
						try {
							// Do not start this bundle if it has requirements on bundles that are not started
							BundleSorter bs = new BundleSorter();
							Collection<Bundle> provBundles = bs.sortProvidingBundles(
									Collections.<Bundle> singleton(bundle), exceptionBundles);
							if (provBundles.size() > 1) {
								exceptionBundles.add(bundle);
								continue;
							}
						} catch (CircularReferenceException e) {
							exceptionBundles.add(bundle);
							continue;
						}
					}
					if (!bundleTransition.containsPending(bundle, Transition.RESOLVE, true)
							&& (!bundleProjectMeta.isCachedFragment(bundle))
							&& ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING)) != 0)) {
						int startOption = Bundle.START_TRANSIENT;
						if (bundleProjectMeta.getCachedActivationPolicy(bundle)) {
							startOption = Bundle.START_ACTIVATION_POLICY;
						}
						if (timeout) {
							bundleCommand.start(bundle, startOption, timeoutVal);
						} else {
							bundleCommand.start(bundle, startOption);
						}
					}
				} catch (BundleActivatorException e) {
					result = addError(e, e.getLocalizedMessage(), bundle);
					// Only check for output folder in class path if class path is set to be updated on
					// activation. If missing instruct the bundle and its requiring bundles to resolve, but not start.
					IBundleStatus classPathStatus = checkClassPath(Collections.<Bundle> singletonList(bundle));
					// Add class path messages into the activation exception
					if (!classPathStatus.hasStatus(StatusCode.OK)) {
						result.add(classPathStatus);
					}
					if (null == exceptionBundles) {
						exceptionBundles = new LinkedHashSet<Bundle>();
					}
					exceptionBundles.add(bundle);
				} catch (IllegalStateException e) {
					result = addError(e, e.getMessage(), bundle);
					Throwable firstCause = e.getCause();
					if (null != firstCause.getCause() && 
							(firstCause.getCause() instanceof ThreadDeath)) {
						throw new OperationCanceledException();						
					}
					if (null == exceptionBundles) {
						exceptionBundles = new LinkedHashSet<Bundle>();
					}
					exceptionBundles.add(bundle);
				} catch (BundleStateChangeException e) {
					if (null == exceptionBundles) {
						exceptionBundles = new LinkedHashSet<Bundle>();
					}
					exceptionBundles.add(bundle);
					addError(e, e.getMessage());
					Throwable cause = e.getCause();
					if (null != cause && cause instanceof TimeoutException) {
						String msg = ExceptionMessage.getInstance().formatString("bundle_start_timeout_error",
								Long.toString(timeoutVal), bundle);
						IBundleStatus errStat = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,
								e);
						msg = WarnMessage.getInstance().formatString("timeout_termination", bundle);
						createMultiStatus(errStat, addWarning(null, msg, bundleRegion.getProject(bundle)));
						stopBundleOperation(monitor);
					} else if (null != cause && cause instanceof BundleException) {
						stopBundleOperation(monitor);
					}
				} catch (InPlaceException e) {
					result = addError(e, e.getLocalizedMessage(), bundle);
					if (null == exceptionBundles) {
						exceptionBundles = new LinkedHashSet<Bundle>();
					}
					exceptionBundles.add(bundle);
				} finally {
					localMonitor.worked(1);
				}
			}
		}
		return result;
	}

	/**
	 * Stop the specified bundles. Try to stop as many bundles as possible. May cause additional
	 * bundles to stop, due to dependencies between bundles. Only bundles in state ACTIVE and STARTING
	 * are stopped.
	 * <p>
	 * If the the specified integrity rule is not set to requiring, the specified bundles should be
	 * sorted in requiring order. Each bundle in the specified collection of bundles to start should
	 * be part of a requiring dependency closure.
	 * <p>
	 * If the task running the start operation is terminated the bundle and the requiring bundles are
	 * deactivated if the deactivate on terminate option is switched on.
	 * 
	 * @param bundles the set of bundles to stop
	 * @param closure if rule is restrict, do not include requiring bundles
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of stopping with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws CircularReferenceException if the integrity parameter contains requiring and cycles are
	 * detected in the project graph
	 * @throws InterruptedException Checks for and interrupts right before call to stop bundle. Stop
	 * is also interrupted if the task running the stop method is terminated abnormally (timeout or
	 * manually)
	 */
	protected IBundleStatus stop(Collection<Bundle> bundles, Closure closure, SubProgressMonitor monitor)
			throws CircularReferenceException, InterruptedException {

		IBundleStatus result = createStatus();

		if (null != bundles && bundles.size() > 0) {
			if (null != closure) {
				BundleClosures bc = new BundleClosures();
				bundles = bc.bundleDeactivation(closure, bundles, bundleRegion.getActivatedBundles());
			}
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			boolean timeout = true;
			long timeoutVal = 5000;
			try {
				timeout = commandOptions.isTimeOut();
				if (timeout) {
					timeoutVal = getTimeout(timeout);
				}
			} catch (InPlaceException e) {
				addError(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
			}
			for (Bundle bundle : bundles) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					localMonitor.subTask(NLS.bind(Msg.STOP_SUB_TASK_JOB, bundle.getSymbolicName()));
					if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
						if (timeout) {
							bundleCommand.stop(bundle, false, timeoutVal);
						} else {
							bundleCommand.stop(bundle, false);
						}
					}
				} catch (IllegalStateException e) {
					result = addError(e, e.getMessage(), bundle);
					Throwable firstCause = e.getCause();
					if (null != firstCause.getCause() && 
							(firstCause.getCause() instanceof ThreadDeath)) {
						throw new OperationCanceledException();						
					}
				} catch (BundleStateChangeException e) {
					addError(e, e.getMessage());
					Throwable cause = e.getCause();
					if (null != cause && cause instanceof TimeoutException) {
						String msg = ExceptionMessage.getInstance().formatString("bundle_stop_timeout_error",
								Long.toString(timeoutVal), bundle);
						IBundleStatus errStat = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,
								e);
						msg = WarnMessage.getInstance().formatString("timeout_termination", bundle);
						createMultiStatus(errStat, addWarning(null, msg, bundleRegion.getProject(bundle)));
						stopBundleOperation(monitor);
					} else if (null != cause && cause instanceof BundleException) {
						stopBundleOperation(monitor);
					}
				} catch (InPlaceException e) {
					result = addError(e, e.getLocalizedMessage(), bundle);
				} finally {
					localMonitor.worked(1);
				}
			}
		}
		return result;
	}

	@Override
	public Bundle isStateChanging() throws ExtenderException {
		
		return Activator.getBundleRegionService().isRegionStateChanging();
	}
	
	@Override
	public boolean stopCurrentOperation() throws ExtenderException {
		initServices();
		return stopBundleOperation(new NullProgressMonitor());
	}

	/**
	 * Stop the current start or stop bundle operation
	 * 
	 * @throws ExtenderException if failing to get the bundle command, transition, region and/or the options service
	 */
	protected boolean stopBundleOperation(IProgressMonitor monitor) throws ExtenderException {

		boolean stopped = false;
		if (getState() != Job.RUNNING) {
			return stopped;
		}		
		Bundle bundle = bundleRegion.isRegionStateChanging();
		String threadName = null;
		boolean isTimeOut = commandOptions.isTimeOut();
		if (null != bundle) {
			Thread thread = BundleThread.getThread(bundle);
			if (null != thread) {
				threadName = thread.getName();
				State state = thread.getState();
				stopped = BundleThread.stopThread(thread);
				if (state == State.BLOCKED) {
					String msg = NLS.bind(Msg.THREAD_BLOCKED_INFO, threadName);
					IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg,
							null);
					if (isTimeOut) {
						addError(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);
					}
				} else if (state == State.WAITING || state == State.TIMED_WAITING) {
					String msg = NLS.bind(Msg.THREAD_WAITING_INFO, threadName);
					IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg,
							null);
					if (isTimeOut) {
						addError(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);
					}
				}
			} else {
				String transitioNname = bundleTransition.getTransitionName(bundleRegion.getProject(bundle));
				String msg = NLS.bind(Msg.FAILED_TO_GET_THREAD_INFO, bundle, transitioNname);
				IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg,
						null);
				if (isTimeOut) {
					addError(status);
				} else {
					StatusManager.getManager().handle(status, StatusManager.LOG);
				}
			}			
			// Deactivate bundle after stopping the bundle operation
			if (commandOptions.isDeactivateOnTerminate()) {
				DeactivateJob deactivateTask = 
						new DeactivateJob(Msg.DEACTIVATE_AFTER_STOP_OP_JOB, bundleRegion.getProject(bundle));
				Activator.getBundleExecutorEventService().add(deactivateTask);
				if (isTimeOut) {
					String msg = NLS.bind(Msg.DEACTIVATING_AFTER_TIMEOUT_STOP_TASK_INFO, bundle,
							threadName);
					addError(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null));
				} else {
					String msg = NLS.bind(Msg.DEACTIVATING_MANUAL_STOP_TASK_INFO, bundle, threadName);
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null),
							StatusManager.LOG);
				}
			} else {
				String transitionName = bundleTransition.getTransitionName(bundleRegion.getProject(bundle));
				if (isTimeOut) {
					String msg = NLS.bind(Msg.AFTER_TIMEOUT_STOP_TASK_INFO, new Object[] { bundle,
							threadName, transitionName });
					addError(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null));
				} else {
					String msg = NLS.bind(Msg.MANUAL_STOP_TASK_INFO, new Object[] { bundle, threadName,
							transitionName });
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null),
							StatusManager.LOG);
				}
			}
		} else {
			IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle,
					Msg.NO_TASK_TO_STOP_INFO, null);
			if (isTimeOut) {
				addError(status);
			} else {
				StatusManager.getManager().handle(status, StatusManager.LOG);
			}
		}
		return stopped;
	}

	/**
	 * Gets and validate the specified timeout value and use default if invalid
	 * 
	 * @param seconds timeout value
	 * @return timeout value in ms
	 */
	private int getTimeout(boolean isTimeout) {
		int seconds = 5;
		try {
			seconds = commandOptions.getTimeout();
			if (seconds < 1 || seconds > 60) {
				int defaultTimeout = commandOptions.getDeafultTimeout();
				if (isTimeout) {
					String msg = WarnMessage.getInstance().formatString("illegal_timout_value", seconds,
							defaultTimeout);
					addWarning(null, msg, null);
				}
				return defaultTimeout * 1000;
			}
		} catch (InPlaceException e) {
			addError(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
		}
		return seconds * 1000;
	}

	/**
	 * Refresh the specified collection of bundles. If the collection of specified bundles is empty
	 * refresh is not invoked. Refresh runs in a separate thread causing this job, when calling the
	 * framework refresh method, to wait until the framework fires an event indicating that refresh
	 * has finished. The event handler then notifies this job to proceed.
	 * 
	 * @param bundlesToRefresh the set of bundles to refresh
	 * @param subMonitor monitor the progress monitor to use for reporting progress to the user.
	 * @throws InPlaceException if this thread is interrupted, security violation, illegal argument
	 * (not same framework) or illegal monitor (current thread not owner of monitor)
	 */
	protected void refresh(final Collection<Bundle> bundlesToRefresh, IProgressMonitor subMonitor)
			throws InPlaceException {

		SubMonitor localMonitor = SubMonitor.convert(subMonitor, bundlesToRefresh.size());
		if (Category.getState(Category.progressBar))
			sleep(sleepTime);
		localMonitor.subTask(Msg.REFRESH_TASK_JOB);
		if (Category.DEBUG && Category.getState(Category.listeners)) {
			// Report on any additional bundles to refreshed by the framework than calculated
			Collection<Bundle> dependencyClosure = bundleCommand.getDependencyClosure(bundlesToRefresh);
			dependencyClosure.removeAll(bundlesToRefresh);
			if (dependencyClosure.size() > 0) {
				String msg = WarnMessage.getInstance().formatString("dependency_closure",
						bundleRegion.formatBundleList(bundlesToRefresh, true));
				addInfo(msg);
				msg = WarnMessage.getInstance().formatString("dependency_closure_additional",
						bundleRegion.formatBundleList(dependencyClosure, true));
				addInfo(msg);
			}
		}
		if (bundlesToRefresh.size() == 0) {
			return;
		}
		try {
			bundleCommand.refresh(bundlesToRefresh);
		} finally {
			localMonitor.worked(bundlesToRefresh.size());
		}
	}
	
	/**
	 * Calculates the requiring dependency closure giving an initial set of bundles and a
	 * domain that limits the set of requiring bundles
	 * <p>
	 * If bundles in the specified domain of bundles have the same symbolic name as any from the
	 * specified initial set of bundles they are added to the requiring closure
	 * <p>
	 * If any activated bundle are tagged with a pending {@code Transition.REFRESH} the bundles and
	 * their requiring bundles are added to the requiring closure 
	 * 
	 * @param bundles the initial set of bundles to calculate the dependency closure from
	 * @param domain set of possible requiring bundles
	 * @return set of bundles to refresh including the specified initial set of bundles or an empty set
	 * @throws CircularReferenceException If cycles are detected in the bundle graph
	 * @throws InPlaceException If failing to get the dependency options service
	 */
	public Collection<Bundle> getRefreshClosure(Collection<Bundle> bundles,
			Collection<Bundle> domain) throws InPlaceException, CircularReferenceException {

		BundleClosures bc = new BundleClosures();
		Collection<Bundle> bundleClosure = bc.bundleDeactivation(Closure.REQUIRING, bundles,
				domain);

		// Add any bundles tagged with refresh and their requiring closure to the requiring closure
		Collection<Bundle> refreshBundles = bundleTransition.getPendingBundles(domain, Transition.REFRESH);
		if (refreshBundles.size() > 0) {
			for (Bundle bundle : refreshBundles) {
				bundleTransition.removePending(bundle, Transition.REFRESH);
			}
			Collection<Bundle> refreshClosure = bc.bundleDeactivation(Closure.REQUIRING, refreshBundles, domain);			
			bundleClosure.addAll(refreshClosure);
		}
		// The resolver always include bundles with the same symbolic name in the refresh process
		Map<IProject, Bundle> duplicates = bundleRegion.getSymbolicNameDuplicates(
				bundleRegion.getProjects(bundleClosure), domain);
		if (duplicates.size() > 0) {
			bundleClosure.addAll(bc.bundleDeactivation(Closure.REQUIRING, duplicates.values(), domain)); 
		}
		return bundleClosure;
	}

	/**
	 * Resolves the specified bundles. Bundle closures with errors are not excluded before resolved.
	 * Resolve errors are added to the job status list.
	 * 
	 * @param bundlesToResolve may resolve additional bundles due to dependencies on this collection
	 * of bundles
	 * @param monitor monitor the progress monitor to use for reporting progress and cancellation to
	 * the user.
	 * @return the bundles not resolved among the specified bundles to resolve or an empty collection
	 * if all bundles where resolved
	 */
	protected Collection<Bundle> resolve(Collection<Bundle> bundlesToResolve,
			SubProgressMonitor monitor) {
		SubMonitor localMonitor = SubMonitor.convert(monitor, 1);
		if (Category.getState(Category.progressBar))
			sleep(sleepTime);
		localMonitor.subTask(Msg.RESOLVE_TASK_JOB);
		boolean isResolved = true;
		try {
			isResolved = bundleCommand.resolve(bundlesToResolve);
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("resolve_job", getName(),
					bundleRegion.formatBundleList(bundlesToResolve, true));
			addError(e, msg);
			isResolved = false;
		}
		try {			
			if (!isResolved) {
				Collection<IProject> projectsToResolve = bundleRegion.getProjects(bundlesToResolve);
				BundleBuildErrorClosure be = new BundleBuildErrorClosure(projectsToResolve, Transition.RESOLVE,
						Closure.REQUIRING);
				if (be.hasBuildErrors(false)) {
					Collection<IProject> buildErrClosure = be.getBuildErrorClosures(false);
					projectsToResolve.removeAll(buildErrClosure);
					if (messageOptions.isBundleOperations()) {
						IBundleStatus bundleStatus = be.getErrorClosureStatus();
						if (null != bundleStatus) {
							addLogStatus(bundleStatus);
						}
					}
				}
				Collection<Bundle> notResolvedBundles = new ArrayList<Bundle>();
				Collection<IProject> notResolvedProjects = new ArrayList<IProject>();
				IBundleStatus startStatus = null;
				for (IProject project : projectsToResolve) {
					Bundle bundle = bundleRegion.getBundle(project);
					// Bundle may be rejected by the resolver hook due to dependencies on deactivated bundles
					if (bundleRegion.isBundleActivated(project)) {
						int state = bundleCommand.getState(bundle);
						if ((state & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
							notResolvedBundles.add(bundle);
							// Must deactivate to not get an activated bundle in state installed
							bundleTransition.addPending(bundle, Transition.DEACTIVATE);
							notResolvedProjects.add(project);
							String msgNotResolvedBundle = NLS.bind(Msg.NOT_RESOLVED_BUNDLE_WARN, bundle,
									project.getName());
							if (null == startStatus) {
								startStatus = addWarning(null, msgNotResolvedBundle, project);
							} else {
								addWarning(null, msgNotResolvedBundle, project);
							}
							ProjectSorter ps = new ProjectSorter();
							Collection<IProject> providingProjects = ps.sortProvidingProjects(Collections
									.<IProject> singletonList(project));
							Collection<IProject> providingAndNotRsolvedProjects = new ArrayList<IProject>();
							for (IProject providingProject : providingProjects) {
								Bundle providingBundle = bundleRegion.getBundle(providingProject);
								int providingState = bundleCommand.getState(providingBundle);
								if ((providingState & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
									providingAndNotRsolvedProjects.add(providingProject);
								}
							}
							providingAndNotRsolvedProjects.remove(project);
							if (providingAndNotRsolvedProjects.size() > 0) {
								String msgNotResolvedBundles = NLS.bind(Msg.PROVIDING_TO_NOT_RESOLVED_BUNDLES_WARN,
										project.getName(),
										bundleProjectCandidates.formatProjectList(providingAndNotRsolvedProjects));
								if (null == startStatus) {
									startStatus = addInfo(msgNotResolvedBundles);
								} else {
									addInfo(msgNotResolvedBundles);
								}
								providingAndNotRsolvedProjects.clear();
							}
						}
					}
				}
				if (null != notResolvedBundles && notResolvedBundles.size() > 0) {
					createMultiStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
							Msg.NOT_RESOLVED_ROOT_WARN), startStatus);
					return notResolvedBundles;
				}
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		} finally {
			localMonitor.worked(1);
		}
		return Collections.emptyList();
	}

	/**
	 * Verify that the specified bundles have a valid standard binary entry in class path.
	 * BundleExecutor that are missing a bin entry are tagged with {@code Transition.RESOLVE}.
	 * 
	 * @param bundles to check for a valid bin entry in class path
	 * @return status object describing the result of checking the class path with
	 * {@code StatusCode.OK} if no failure, otherwise one of the failure codes are returned. If more
	 * than one bundle fails, status of the last failed bundle is returned. All failures are added to
	 * the job status list
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus checkClassPath(Collection<Bundle> bundles)
			throws CircularReferenceException {

		IBundleStatus result = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, null);
		BundleSorter bs = null;
		// BundleExecutor missing binary path and not to be started
		Collection<Bundle> errorBundles = null;
		for (Bundle bundle : bundles) {
			try {
				if ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING | Bundle.STARTING)) != 0) {
					// Check for the output folder path in the cached version of the bundle
					IPath path = bundleProjectMeta.getDefaultOutputFolder(bundleRegion.getProject(bundle));
					if (!bundleProjectMeta.verifyPathInCachedClassPath(bundle, path)) {
						if (null == errorBundles) {
							bs = new BundleSorter();
							errorBundles = new LinkedHashSet<Bundle>();
						}
						errorBundles.add(bundle);
						errorBundles = bs.sortRequiringBundles(errorBundles);
						String msg = null;
						// Check if the not cached output folder path exist in the manifest file
						Boolean binExist = bundleProjectMeta.isDefaultOutputFolder(bundleRegion
								.getProject(bundle));
						if (binExist) {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle_loaded",
									bundle);
						} else {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle", bundle);
						}
						result.setStatusCode(StatusCode.ERROR);
						result.setMessage(msg);
						if (errorBundles.size() > 1) {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle_affected",
									bundleRegion.formatBundleList(errorBundles, true));
							result.add(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, bundle.getBundleId(),
									msg, null));
						}
						if (binExist) {
							msg = NLS.bind(Msg.MISSING_CLASSPATH_BUNDLE_LOADED_INFO, bundle);
						} else {
							msg = NLS.bind(Msg.MISSING_CLASSPATH_BUNDLE_INFO, bundle);
						}
						result.add(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, bundle.getBundleId(),
								msg, null));
					}
				}
			} catch (InPlaceException e) {
				addError(e, bundleRegion.getProject(bundle));
			} finally {
				if (null != errorBundles) {
					errorBundles.clear();
				}
			}
		}
		if (!result.hasStatus(StatusCode.OK)) {
			if (messageOptions.isBundleOperations()) {
				result.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
						Msg.MISSING_DEV_CLASSPATH_BUNDLE_INFO));
			}
		}
		return result;
	}

	/**
	 * Sets the dev class path and/or updates the Bundle-ClassPath with the default output folder for
	 * the specified project.
	 * <p>
	 * The Bundle-ClassPath is updated if the set update default output folder is on.
	 * 
	 * @param project the project to add the dev class path and/or the output folder to
	 * @return an {@code BundleStatus.ERROR} code if the loading of the specified dev class path
	 * fails, otherwise the bundles status code returned is {@code BundleStatus.OK_BUNDLE_STATUS}
	 */
	protected IBundleStatus resolveBundleClasspath(IProject project) {

		IBundleStatus result = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, "");

		try {
			String symbolicName = bundleProjectMeta.getSymbolicName(project);
			if (null == symbolicName) {
				String msg = NLS.bind(Msg.SYMBOLIC_NAME_ERROR, project.getName());
				result = addError(null, msg);
			}
			try {
				if (null != bundleProjectMeta.inDevelopmentMode()) {
					bundleProjectMeta.setDevClasspath(project);
				}
			} catch (InPlaceException e) {
				String msg = ExceptionMessage.getInstance().formatString("error_resolve_class_path",
						project.getName());
				result = addError(e, msg);
			}
			if (commandOptions.isUpdateDefaultOutPutFolder()) {
				bundleProjectMeta.addDefaultOutputFolder(project);
			}
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("error_resolve_class_path",
					project.getName());
			result = addError(e, msg);
		}
		return result;
	}


	@Override
	public void run(long delay) {
		super.schedule(delay);
	}

	@Override
	public void run() {
		super.schedule(0L);
	}

	@Override
	public void joinBundleExecutor() throws InterruptedException {
		super.join();
	}

	@Override
	public WorkspaceJob getJob() {
		return this;
	}

	/**
	 * Debug for synchronizing the progress monitor. Default number of mills to sleep
	 */
	final public static int sleepTime = 300;

	/**
	 * Debug for synchronizing the progress monitor
	 * 
	 * @param mills to sleep
	 */
	static public void sleep(long mills) {
		try {
			Thread.sleep(mills);
		} catch (InterruptedException e) {
			// ignore
		}
	}
}
