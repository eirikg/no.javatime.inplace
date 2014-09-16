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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.manager.BundleActivatorException;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleStateChangeException;
import no.javatime.inplace.region.manager.BundleThread;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.DuplicateBundleException;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Support processing of bundle operations on bundles added as pending projects to bundle jobs.
 * <p>
 * Pending bundle projects are added to a job before it is scheduled and the bundle projects are executed according to
 * the job type.
 */
public abstract class BundleJob extends JobStatus {

	/** Standard activate bundle job name */
	final public static String activateJobName = Message.getInstance().formatString("activate_bundle_job_name");

	/** Standard update bundle job name */
	final public static String updateJobName = Message.getInstance().formatString("update_job_name");

	/** Name of the uninstall operation */
	final protected static String uninstallSubtaskName = Message.getInstance().formatString(
			"uninstall_subtask_name");
	/** Name of the install operation */
	final protected static String installSubtaskName = Message.getInstance().formatString(
			"install_subtask_name");
	final protected static String reInstallSubtaskName = Message.getInstance().formatString(
			"reinstall_subtask_name");
	/** Name of the resolve operation */
	final protected static String resolveTaskName = Message.getInstance().formatString("resolve_task_name");
	/** Common family job name for all bundle jobs */
	public static final String FAMILY_BUNDLE_LIFECYCLE = "BundleFamily";
	/** Use the same rules as the build system */
	public static final ISchedulingRule buildRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

	/**
	 * Unsorted list of unique pending projects to process in a job
	 */
	private Collection<IProject> pendingProjects = new LinkedHashSet<IProject>();
	
	private DependencyOptions dependencyOptions; 
	/**
	 * Construct a bundle job with a bundle name. Sets job priority and scheduling rule.
	 * 
	 * @param name of the job to process
	 */
	public BundleJob(String name) {
		super(name);
		setPriority(Job.BUILD);
		setRule(buildRule);
	}

	/**
	 * Construct a bundle job with a name and pending bundle projects to perform bundle operations on. Sets job priority
	 * and scheduling rule.
	 * 
	 * @param name of the job to run
	 * @param projects pending bundle projects to perform bundle operations on
	 */
	public BundleJob(String name, Collection<IProject> projects) {
		super(name);
		addPendingProjects(projects);
		setPriority(Job.BUILD);
		setRule(buildRule);
	}

	/**
	 * Constructs a bundle job with a name and a bundle project to perform bundle operations on. Sets job priority and
	 * scheduling rule.
	 * 
	 * @param name of the job to run
	 * @param project pending bundle project to perform bundle operations on
	 */
	public BundleJob(String name, IProject project) {
		super(name);
		addPendingProject(project);
		setPriority(Job.BUILD);
		setRule(buildRule);
	}

	/**
	 * Runs the bundle(s) operation.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		return super.runInWorkspace(monitor);
	}

	/**
	 * Determine if this job belongs to the bundle family
	 * 
	 * @return true if the specified object == {@code Bundle.FAMILY_BUNDLE_LIFECYCLE} or is of type {@code BundleJob},
	 *         otherwise false
	 */
	@Override
	public boolean belongsTo(Object family) {
		if (family == FAMILY_BUNDLE_LIFECYCLE || family instanceof BundleJob) {
			return true;
		}
		return false;
	}

	/**
	 * Add a pending bundle project to process in a job
	 * 
	 * @param project to process
	 */
	public void addPendingProject(IProject project) {
		pendingProjects.add(project);
	}

	/**
	 * Adds a collection of pending bundle projects to process in a job. To maintain the topological
	 * sort order use {@link #resetPendingProjects(Collection)}
	 * 
	 * @param projects bundle projects to process
	 * @see BundleJob#resetPendingProjects(Collection)
	 */
	public void addPendingProjects(Collection<IProject> projects) {
		pendingProjects.addAll(projects);
	}

	/**
	 * Replace existing pending projects with the specified collection The specified collection is copied and the 
	 * topological sort order of the projects is maintained
	 * 
	 * @param projects bundle projects to replace
	 */
	public void resetPendingProjects(Collection<IProject> projects) {
		pendingProjects = new LinkedHashSet<IProject>(projects);
	}

	/**
	 * Removes a pending bundle project from the project job list
	 * 
	 * @param project bundle project to remove
	 */
	public void removePendingProject(IProject project) {
		pendingProjects.remove(project);
	}

	/**
	 * Removes pending bundle projects from the project job list.
	 * 
	 * @param projects bundle projects to remove
	 */
	public void removePendingProjects(Collection<IProject> projects) {
		pendingProjects.removeAll(projects);
	}

	/**
	 * Clears all pending bundle projects from the project job list.
	 */
	public void clearPendingProjects() {
		pendingProjects.clear();
	}

	/**
	 * Determine if the specified bundle project is a pending bundle project
	 * 
	 * @param project bundle project to check for pending status
	 * @return true if the specified bundle project is a pending bundle project, otherwise false.
	 */
	public Boolean isPendingProject(IProject project) {
		return this.pendingProjects.contains(project);
	}

	/**
	 * Checks if there are any pending bundle projects to process.
	 * 
	 * @return true if there are any bundle projects to process, otherwise false
	 */
	public Boolean hasPendingProjects() {
		return !pendingProjects.isEmpty();
	}

	/**
	 * Returns all pending bundle projects.
	 * 
	 * @return the set of pending bundle projects or an empty set
	 */
	public Collection<IProject> getPendingProjects() {
		return pendingProjects;
	}

	/**
	 * Number of pending bundle projects
	 * 
	 * @return number of pending bundle projects
	 */
	public int pendingProjects() {
		return pendingProjects.size();
	}
	
	public DependencyOptions getDepOpt() {
		if (null == dependencyOptions) {
			dependencyOptions = InPlace.get().getDependencyOptionsService();
		}
		return dependencyOptions;
	}

	/**
	 * Reinstalls the specified bundle projects. Failures to reinstall are added to the job status list. Only specified
	 * bundle projects in state INSTALLED are re-installed.
	 * 
	 * @param projectsToInstall a collection of bundle projects to re install
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return installed and activated bundle projects. An empty set means that zero or more deactivated bundles have been
	 *         installed
	 */
	protected IBundleStatus reInstall(Collection<IProject> projectsToReinstall, IProgressMonitor monitor) {

		IBundleStatus status = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
		SubMonitor progress = SubMonitor.convert(monitor, projectsToReinstall.size());

		for (IProject project : projectsToReinstall) {
			Bundle bundle = bundleRegion.get(project);
			if ((null != bundle) && (bundle.getState() & (Bundle.INSTALLED)) != 0) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					progress.subTask(reInstallSubtaskName + project.getName());
					IBundleStatus result = uninstall(Collections.<Bundle>singletonList(bundle), new SubProgressMonitor(monitor, 1),
							false);
					if (result.hasStatus(StatusCode.OK)) {
						install(Collections.<IProject>singletonList(project), new SubProgressMonitor(monitor, 1));
					}
				} catch (DuplicateBundleException e) {
					addError(e, e.getLocalizedMessage(), project);
				} catch (InPlaceException e) {
					addError(e, e.getLocalizedMessage(), project);
				} finally {
					progress.worked(1);
				}
			}
		}
		return status;
	}

	/**
	 * Installs pending bundles and set activation status on the bundles. All failures to install are added to the job
	 * status list
	 * 
	 * @param projectsToInstall a collection of bundle projects to install
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return installed and activated bundle projects. The returned list is never {@code null}
	 */
	protected Collection<Bundle> install(Collection<IProject> projectsToInstall, IProgressMonitor monitor) {

		SubMonitor progress = SubMonitor.convert(monitor, projectsToInstall.size());
		Collection<Bundle> activatedBundles = new LinkedHashSet<Bundle>();

		for (IProject project : projectsToInstall) {
			Bundle bundle = null; // Assume not installed
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				progress.subTask(installSubtaskName + project.getName());
				// Get the activation status of the corresponding project
				Boolean activated = BundleProjectState.isNatureEnabled(project);
				bundle = bundleCommand.install(project, activated);
				// Project must be activated and bundle must be successfully installed to be activated
				if (null != bundle && activated) {
					activatedBundles.add(bundle);
				}
			} catch (DuplicateBundleException e) {
				String msg = null;
				try {
					handleDuplicateException(project, e, null);
				} catch (InPlaceException e1) {
					msg = e1.getLocalizedMessage();
					addError(e, msg, project);
				}
			} catch (ProjectLocationException e) {
				IBundleStatus status = addError(e, e.getLocalizedMessage());
				String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());
				status.add(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, msg, null));
				msg  = NLS.bind(Msg.REFRESH_HINT_INFO, project.getName()); 
				status.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, project, msg, null));
			} catch (InPlaceException e) {
				String msg = ErrorMessage.getInstance().formatString("install_error_project", project.getName());
				addError(e, msg, project);
			} finally {
				progress.worked(1);
			}
		}
		return activatedBundles;
	}

	/**
	 * Uninstall and refresh the specified bundles. Errors are added to this job for bundles that fail to uninstall.
	 * 
	 * @param bundles to uninstall
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @param unregister If true the bundle is removed from the internal workspace region. Will be registered again
	 * automatically when installed
	 * @return status object describing the result of uninstalling with {@code StatusCode.OK} if no failure, otherwise one
	 *         of the failure codes are returned. If more than one bundle fails, status of the last failed bundle is
	 *         returned. All failures are added to the job status list
	 */
	protected IBundleStatus uninstall(Collection<Bundle> bundles, IProgressMonitor monitor,
			boolean unregister) {

		IBundleStatus result = createStatus();

		if (null != bundles && bundles.size() > 0) {
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			for (Bundle bundle : bundles) {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				localMonitor.subTask(UninstallJob.uninstallSubtaskName + bundle.getSymbolicName());
				try {
					// Unregister after refresh
					bundleCommand.uninstall(bundle, false);					
				} catch (InPlaceException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
				} finally {
					localMonitor.worked(1);
				}
			}
			refresh(bundles, new SubProgressMonitor(monitor, 1));
			if (unregister) {	
				for (Bundle bundle : bundles) {
					bundleCommand.unregisterBundleProject(bundleRegion.getRegisteredBundleProject(bundle));
				}
			}	else {
				for (Bundle bundle : bundles) {
					bundleCommand.unregisterBundle(bundle);
				}
			}
		}
		return result;
	}
		
	/**
	 * Start the specified bundles. If the activation policy for a bundle is lazy the bundle is activated according to the
	 * declared activation policy. If the activation policy is eager, the bundle is started transient. Only bundles in
	 * state RESOLVED and STOPPING are started.
	 * <p>
	 * If the the specified integrity rule is not set to providing, the specified bundles should be sorted in providing
	 * order. Each bundle in the specified collection of bundles to start should be part of a providing dependency closure.
	 * <p>
	 * Requiring bundles to bundles that fail to start are not started.
	 * <p>
	 * If the task running the start operation is terminated the bundle and the requiring bundles are deactivated if the
	 * deactivate on terminate option is switched on.
	 * 
	 * @param bundles the set of bundles to start
	 * @param closure if rule is {@code Closure.PROVIDING} include providing bundles to start
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of starting with {@code StatusCode.OK} if no failure, otherwise one of
	 *         the failure codes are returned. If more than one bundle fails, status of the last failed bundle is
	 *         returned. All failures are added to the job status list
	 * @throws CircularReferenceException if the integrity parameter contains providing and cycles are detected in the
	 *           project graph
	 * @throws InterruptedException Checks for and interrupts right before call to start bundle. Start is also interrupted
	 *           if the task running the stop method is terminated abnormally (timeout or manually)
	 * @throws InPlaceException illegal closure for activate bundle operation
	 */
	public IBundleStatus start(Collection<Bundle> bundles, Closure closure,
			IProgressMonitor monitor) throws InterruptedException, InPlaceException {

		IBundleStatus result = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
		Collection<Bundle> exceptionBundles = null;

		if (null != bundles && bundles.size() > 0) {
			if (null != closure) {
				BundleClosures bc = new BundleClosures();
				bundles = bc.bundleActivation(closure, bundles, bundleRegion.getActivatedBundles());
			}
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			boolean timeout = true;
			int timeoutVal = 5000;
			try {
				timeout = getOptionsService().isTimeOut();
				if (timeout) {
					timeoutVal = getTimeout(timeout);
				}
			} catch (InPlaceException e) {
				addStatus(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e));
			}
			for (Bundle bundle : bundles) {
				try {
					if (localMonitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					localMonitor.subTask(StartJob.startSubTaskName + bundle.getSymbolicName());
					if (null != exceptionBundles) {
						try {
							// Do not start this bundle if it has requirements on bundles that are not started
							BundleSorter bs = new BundleSorter();
							Collection<Bundle> provBundles = bs.sortProvidingBundles(Collections.<Bundle>singleton(bundle),
									exceptionBundles);
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
							&& (!ManifestOptions.isFragment(bundle))
							&& ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING)) != 0)) {
						int startOption = Bundle.START_TRANSIENT;
						if (ManifestOptions.getlazyActivationPolicy(bundle)) {
							startOption = Bundle.START_ACTIVATION_POLICY;
						}
						if (timeout) {
							bundleCommand.start(bundle, startOption, timeoutVal);
						} else {
							bundleCommand.start(bundle, startOption);
						}
					}
				} catch (BundleActivatorException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
					// Only check for output folder in class path if class path is set to be updated on activation
					// If missing instruct the bundle and its requiring bundles to resolve, but not start.
					IBundleStatus classPathStatus = checkClassPath(Collections.<Bundle>singletonList(bundle));
					// Add class path messages into the activation exception
					if (!classPathStatus.hasStatus(StatusCode.OK)) {
						result.add(classPathStatus);
					}
					if (null == exceptionBundles) {
						exceptionBundles = new LinkedHashSet<Bundle>();
					}
					exceptionBundles.add(bundle);
				} catch (IllegalStateException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
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
								Integer.toString(timeoutVal), bundle);
						IBundleStatus errStat = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e);
						msg = WarnMessage.getInstance().formatString("timeout_termination", bundle);
						createMultiStatus(errStat, addWarning(null, msg, BundleJobManager.getRegion().getRegisteredBundleProject(bundle)));
						stopCurrentBundleOperation(monitor);						
					} else if (null != cause && cause instanceof BundleException) {
						stopCurrentBundleOperation(monitor);						
					}
				} catch (InPlaceException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
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
	 * Stop the specified bundles. Try to stop as many bundles as possible. May cause additional bundles to stop, due to
	 * dependencies between bundles. Only bundles in state ACTIVE and STARTING are stopped.
	 * <p>
	 * If the the specified integrity rule is not set to requiring, the specified bundles should be sorted in requiring
	 * order. Each bundle in the specified collection of bundles to start should be part of a requiring dependency closure.
	 * <p>
	 * If the task running the start operation is terminated the bundle and the requiring bundles are deactivated if the
	 * deactivate on terminate option is switched on.
	 * 
	 * @param bundles the set of bundles to stop
	 * @param closure if rule is restrict, do not include requiring bundles
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of stopping with {@code StatusCode.OK} if no failure, otherwise one of
	 *         the failure codes are returned. If more than one bundle fails, status of the last failed bundle is
	 *         returned. All failures are added to the job status list
	 * @throws CircularReferenceException if the integrity parameter contains requiring and cycles are detected in the
	 *           project graph
	 * @throws InterruptedException Checks for and interrupts right before call to stop bundle. Stop is also interrupted
	 *           if the task running the stop method is terminated abnormally (timeout or manually)
	 */
	public IBundleStatus stop(Collection<Bundle> bundles, Closure closure,
			SubProgressMonitor monitor) throws CircularReferenceException, InterruptedException {

		IBundleStatus result = createStatus();

		if (null != bundles && bundles.size() > 0) {
			if (null != closure) {
				BundleClosures bc = new BundleClosures();
				bundles = bc.bundleDeactivation(closure, bundles, bundleRegion.getActivatedBundles());
			}
			SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
			boolean timeout = true;
			int timeoutVal = 5000;
			try {
				timeout = getOptionsService().isTimeOut();
				if (timeout) {
					timeoutVal = getTimeout(timeout);
				}
			} catch (InPlaceException e) {
				addStatus(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e));
			}
			for (Bundle bundle : bundles) {
				try {
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					localMonitor.subTask(StopJob.stopSubTaskName + bundle.getSymbolicName());
					if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
						if (timeout) {
							bundleCommand.stop(bundle, false, timeoutVal);
						} else {
							bundleCommand.stop(bundle, false);
						}
					}
				} catch (IllegalStateException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
				} catch (BundleStateChangeException e) {
					addError(e, e.getMessage());
					Throwable cause = e.getCause(); 
					if (null != cause && cause instanceof TimeoutException) {
						String msg = ExceptionMessage.getInstance().formatString("bundle_stop_timeout_error",
								Integer.toString(timeoutVal), bundle);
						IBundleStatus errStat = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e);
						msg = WarnMessage.getInstance().formatString("timeout_termination", bundle);
						createMultiStatus(errStat, addWarning(null, msg, BundleJobManager.getRegion().getRegisteredBundleProject(bundle)));
						stopCurrentBundleOperation(monitor);						
					} else if (null != cause && cause instanceof BundleException) {
						stopCurrentBundleOperation(monitor);						
					}
				} catch (InPlaceException e) {
					result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
				}
				finally {
					localMonitor.worked(1);
				}
			}
		}
		return result;
	}

	public static boolean isStateChanging() {
		Bundle bundle = BundleJobManager.getCommand().getCurrentBundle();
		if (null != bundle) {
			return true;
		}
		return false;
	}
	
	/**
	 * Stop the current start or stop bundle operation 
	 * 
	 * @throws InPlaceException if the options service not could be acquired
	 */
	public void stopCurrentBundleOperation(IProgressMonitor monitor) throws InPlaceException {

		DeactivateJob deactivateTask = null;
		
		Bundle bundle = bundleCommand.getCurrentBundle();
		String threadName = null;
		if (null != bundle) {
			if (getOptionsService().isDeactivateOnTerminate() && null == deactivateTask) {
				deactivateTask = new DeactivateJob(DeactivateJob.deactivateJobName);
				deactivateTask.addPendingProject(bundleRegion.getRegisteredBundleProject(bundle));
			}
			Thread thread = BundleThread.getThread(bundle);
			if (null != thread) {
				threadName = thread.getName();
				State state = thread.getState();
				BundleThread.stopThread(thread);
				if (state == State.BLOCKED) {
					String msg = NLS.bind(Msg.THREAD_BLOCKED_INFO, threadName); 
					IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null);
					if (getOptionsService().isTimeOut()) {
						addStatus(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);						
					}
				} else if (state == State.WAITING || state == State.TIMED_WAITING) {
					String msg = NLS.bind(Msg.THREAD_WAITING_INFO, threadName); 
					IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null);
					if (getOptionsService().isTimeOut()) {
						addStatus(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);						
					}
				}
			} else {
				String transitioNname = bundleTransition.getTransitionName(bundleRegion.getRegisteredBundleProject(bundle));
				String msg = NLS.bind(Msg.FAILED_TO_GET_THREAD_INFO, bundle, transitioNname); 
				IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null);
				if (getOptionsService().isTimeOut()) {
					addStatus(status);						
				} else {
					StatusManager.getManager().handle(status, StatusManager.LOG);												
				}					
			}
		} else {
			IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, Msg.NO_TASK_TO_STOP_INFO, null);
			if (getOptionsService().isTimeOut()) {
				addStatus(status);
			} else {
				StatusManager.getManager().handle(status, StatusManager.LOG);						
			}
		}
		
		if (null != threadName) {
			if (null != deactivateTask) {
				try {
					if (getOptionsService().isDeactivateOnTerminate()) {
						deactivateTask.deactivate(monitor);
						if (getOptionsService().isTimeOut()) {
							String msg = NLS.bind(Msg.DEACTIVATING_AFTER_TIMEOUT_STOP_TASK_INFO, bundle, threadName); 
							addStatus(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null));
						} else {
							String msg = NLS.bind(Msg.DEACTIVATING_MANUAL_STOP_TASK_INFO, bundle, threadName); 
							StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null),
									StatusManager.LOG);						
						}
					}
				} catch (InPlaceException e) {
					IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, bundle, e.getMessage(), e);
					if (getOptionsService().isTimeOut()) {
						addStatus(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);						
					}
				} catch (InterruptedException e) {
					IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, bundle, e.getMessage(), e);
					if (getOptionsService().isTimeOut()) {
						addStatus(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);						
					}
				} catch (OperationCanceledException e) {
					IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, bundle, e.getMessage(), e);
					if (getOptionsService().isTimeOut()) {
						addStatus(status);
					} else {
						StatusManager.getManager().handle(status, StatusManager.LOG);						
					}
				}
			} else {
				String transitioNname = bundleTransition.getTransitionName(bundleRegion.getRegisteredBundleProject(bundle));
				if (getOptionsService().isTimeOut()) {
					String msg = NLS.bind(Msg.AFTER_TIMEOUT_STOP_TASK_INFO, new Object[] {bundle, threadName, transitioNname}); 
					addStatus(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null));
				} else {
					String msg = NLS.bind(Msg.MANUAL_STOP_TASK_INFO, new Object[] {bundle, threadName, transitioNname}); 
					StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle, msg, null),
							StatusManager.LOG);						
				}				
			}
		}
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
			seconds = getOptionsService().getTimeout();
			if (seconds < 1 || seconds > 60) {
				int defaultTimeout = getOptionsService().getDeafultTimeout();
				if (isTimeout) {
					String msg = WarnMessage.getInstance()
							.formatString("illegal_timout_value", seconds, defaultTimeout);
					addWarning(null, msg, null);
				}
				return defaultTimeout * 1000;
			}
		} catch (InPlaceException e) {
			addStatus(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e));
		}
		return seconds * 1000;
	}

	/**
	 * Refresh the specified collection of bundles. If the collection of specified bundles is empty refresh is not
	 * invoked. Refresh runs in a separate thread causing this job, when calling the framework refresh method, to wait
	 * until the framework fires an event indicating that refresh has finished. The event handler then notifies this job
	 * to proceed.
	 * 
	 * @param bundlesToRefresh the set of bundles to refresh
	 * @param subMonitor monitor the progress monitor to use for reporting progress to the user.
	 * @throws InPlaceException if this thread is interrupted, security violation, illegal argument (not same framework)
	 *           or illegal monitor (current thread not owner of monitor)
	 * @see BundleCommandImpl#refresh(Collection)
	 */
	protected void refresh(final Collection<Bundle> bundlesToRefresh, IProgressMonitor subMonitor)
			throws InPlaceException {

		SubMonitor localMonitor = SubMonitor.convert(subMonitor, bundlesToRefresh.size());
		if (Category.getState(Category.progressBar))
			sleep(sleepTime);
		localMonitor.subTask(RefreshJob.refreshTaskName);
		if (Category.DEBUG && Category.getState(Category.listeners)) {
			// Report on any additional bundles refreshed than those specified
			Collection<Bundle> dependencyClosure = bundleCommand.getDependencyClosure(bundlesToRefresh);
			dependencyClosure.removeAll(bundlesToRefresh);
			if (dependencyClosure.size() > 0) {
				String msg = WarnMessage.getInstance().formatString("dependency_closure",
						bundleRegion.formatBundleList(bundlesToRefresh, true));
				addInfoMessage(msg);
				msg = WarnMessage.getInstance().formatString("dependency_closure_additional",
						bundleRegion.formatBundleList(dependencyClosure, true));
				addInfoMessage(msg);
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
	 * Finds and returns the dependency closure to resolve and/or refresh giving an initial set of bundles.
	 * 
	 * @param bundles the initial set of bundles to calculate the dependency closure from
	 * @return set of bundles to resolve and/or refresh including the specified initial set of bundles
	 */
	public Collection<Bundle> getBundlesToResolve(Collection<Bundle> bundles) {
		// return bundleCommand.getDependencyClosure(bundles);
		BundleSorter bs = new BundleSorter();
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		Collection<Bundle> bundlesToResolve = bs.sortDeclaredRequiringBundles(bundles, activatedBundles);
		// The resolver always include bundles with the same symbolic name in the resolve process
		// TODO Add these when duplicates are detected. See who uses this method. NB -> Now excluded for update job
		Map<IProject, Bundle> duplicates = bundleRegion.getSymbolicNameDuplicates(
				bundleRegion.getBundleProjects(bundlesToResolve), activatedBundles, true);
		if (duplicates.size() > 0) {
			bundlesToResolve.addAll(bs.sortDeclaredRequiringBundles(duplicates.values(), activatedBundles));
		}
		return bundlesToResolve;
	}

	/**
	 * Resolves the specified bundles. Bundle closures with errors are not excluded before resolved. Resolve errors are
	 * added to the job status list.
	 * 
	 * @param bundlesToResolve may resolve additional bundles due to dependencies on this collection of bundles
	 * @param monitor monitor the progress monitor to use for reporting progress and cancellation to the user.
	 * @return the bundles not resolved among the specified bundles to resolve or an empty collection if all bundles where
	 *         resolved
	 */
	protected Collection<Bundle> resolve(Collection<Bundle> bundlesToResolve, SubProgressMonitor monitor) {
		SubMonitor localMonitor = SubMonitor.convert(monitor, 1);
		try {
			if (Category.getState(Category.progressBar))
				sleep(sleepTime);
			localMonitor.subTask(resolveTaskName);
			if (!bundleCommand.resolve(bundlesToResolve)) {
				Collection<IProject> projectsToResolve = bundleRegion.getBundleProjects(bundlesToResolve);
				BuildErrorClosure be = new BuildErrorClosure(projectsToResolve, 
						Transition.RESOLVE, Closure.REQUIRING);
				if (be.hasBuildErrors()) {
					Collection<IProject> buildErrClosure = be.getBuildErrorClosures();
					projectsToResolve.removeAll(buildErrClosure);
					if (InPlace.get().getMsgOpt().isBundleOperations()) {
						IBundleStatus bundleStatus = be.getErrorClosureStatus();
						if (null != bundleStatus) {
							addTrace(bundleStatus);			
						}
					}
				}
				Collection<Bundle> notResolvedBundles = new ArrayList<Bundle>();
				Collection<IProject> notResolvedProjects = new ArrayList<IProject>();
				IBundleStatus startStatus = null;
				for (IProject project : projectsToResolve) {
					Bundle bundle = bundleRegion.get(project);
					// Bundle may be rejected by the resolver hook due to dependencies on deactivated bundles
					if (BundleProjectState.isNatureEnabled(project)) {
						int state = bundleCommand.getState(bundle);
						if ((state & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
							notResolvedBundles.add(bundle);
							notResolvedProjects.add(project);
							String resolveMsg1 = WarnMessage.getInstance().formatString("not_resolved_bundle", bundle,
									project.getName());
							if (null == startStatus) {
								// startStatus = addWarning(null, resolveMsg1, null);
								startStatus = addError(null, resolveMsg1, project);
							} else {
								// addWarning(null, resolveMsg1, null);
								addError(null, resolveMsg1, project);
							}
							ProjectSorter ps = new ProjectSorter();
							Collection<IProject> providingProjects = ps.sortProvidingProjects(Collections
									.<IProject>singletonList(project));
							Collection<IProject> providingAndNotRsolvedProjects = new ArrayList<IProject>();
							for (IProject providingProject : providingProjects) {
								Bundle providingBundle = bundleRegion.get(providingProject);
								int providingState = bundleCommand.getState(providingBundle);
								if ((providingState & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
									providingAndNotRsolvedProjects.add(providingProject);
								}
							}
							providingAndNotRsolvedProjects.remove(project);
							if (providingAndNotRsolvedProjects.size() > 0) {
								String resolveMsg2 = WarnMessage.getInstance().formatString(
										"providing_to_not_resolved_bundles", project.getName(),
										BundleProjectState.formatProjectList(providingAndNotRsolvedProjects));
								if (null == startStatus) {
									startStatus = addInfoMessage(resolveMsg2);
								} else {
									addInfoMessage(resolveMsg2);
								}
								providingAndNotRsolvedProjects.clear();
							}
						}
					}
				}
				if (notResolvedBundles.size() > 0) {
					String rootMsg = WarnMessage.getInstance().formatString("not_resolved_root");
					createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, rootMsg), startStatus);
					return notResolvedBundles;
				}
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("resolve_job", getName(),
					bundleRegion.formatBundleList(bundlesToResolve, true));
			addError(e, msg);
		} finally {
			localMonitor.worked(1);
		}
		return Collections.emptyList();
	}

	/**
	 * Verify that the specified bundles have a valid standard binary entry in class path. Bundles that are missing a bin
	 * entry are tagged with {@code Transition.RESOLVE}.
	 * 
	 * @param bundles to check for a valid bin entry in class path
	 * @return status object describing the result of checking the class path with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the last failed
	 *         bundle is returned. All failures are added to the job status list
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	private IBundleStatus checkClassPath(Collection<Bundle> bundles) throws CircularReferenceException {

		IBundleStatus result = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
		BundleSorter bs = null;
		// Bundles missing binary path and not to be started
		Collection<Bundle> errorBundles = null;
		for (Bundle bundle : bundles) {
			try {
				if ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING | Bundle.STARTING)) != 0) {
					// Check for the output folder path in the cached version of the bundle
					IPath path = BundleProjectSettings.getDefaultOutputLocation(bundleRegion.getRegisteredBundleProject(bundle));
					if (!ManifestOptions.verifyPathInClassPath(bundle, path)) {
						if (null == errorBundles) {
							bs = new BundleSorter();
							errorBundles = new LinkedHashSet<Bundle>();
						}
						errorBundles.add(bundle);
						errorBundles = bs.sortRequiringBundles(errorBundles);
						String msg = null;
						// Check if the not cached output folder path exist in the manifest file
						Boolean binExist = BundleProjectSettings.isOutputFolderInBundleClassPath(bundleRegion.getRegisteredBundleProject(bundle));
						if (binExist) {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle_loaded", bundle);
						} else {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle", bundle);
						}
						result.setStatusCode(StatusCode.ERROR);
						result.setMessage(msg);
						if (errorBundles.size() > 1) {
							msg = ErrorMessage.getInstance().formatString("missing_classpath_bundle_affected",
									bundleRegion.formatBundleList(errorBundles, true));
							result.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle.getBundleId(),msg, null));
						}
						if (binExist) {
							msg = NLS.bind(Msg.MISSING_CLASSPATH_BUNDLE_LOADED_INFO, bundle); 
						} else {
							msg = Msg.MISSING_CLASSPATH_BUNDLE_INFO; 
						}
						result.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, bundle.getBundleId(), msg, null));
					}
				}
			} catch (InPlaceException e) {
				addError(e, bundleRegion.getRegisteredBundleProject(bundle));
			} finally {
				if (null != errorBundles) {
					errorBundles.clear();
				}
			}
		}
		if (!result.hasStatus(StatusCode.OK)) {
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				result.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, Msg.MISSING_DEV_CLASSPATH_BUNDLE_INFO));
			}
		}
		return result;
	}

	/**
	 * Sets the dev class path and/or updates the Bundle-ClassPath with the default output folder for the specified
	 * project.
	 * 
	 * @param project the project to add the dev class path and/or the output folder to
	 * @return an {@code BundleStatus.ERROR} code if the loading of the specified dev class path fails, otherwise the
	 *         bundles status code returned is {@code BundleStatus.OK_BUNDLE_STATUS}
	 */
	protected IBundleStatus resolveBundleClasspath(IProject project) {

		IBundleStatus result = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");

		try {
			String symbolicName = BundleProjectSettings.getSymbolicNameFromManifest(project);
			if (null == symbolicName) {
				String msg = NLS.bind(Msg.SYMBOLIC_NAME_ERROR, project.getName());
				result = addError(null, msg);
			}
			BundleProjectSettings.setDevClasspath(symbolicName, BundleProjectSettings
					.getDefaultOutputLocation(project).toString());
			if (getOptionsService().isUpdateDefaultOutPutFolder()) {
				BundleProjectSettings.addOutputLocationToBundleClassPath(project);
			}
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("error_resolve_class_path", project);
			result = addError(e, msg);
		}
		return result;
	}

	public Collection<Bundle> removeTransitionErrorClosures(Collection<Bundle> initialBundleSet,
			Collection<Bundle> bDepClosures, Collection<IProject> pDepClosures) {

		BundleSorter bs = new BundleSorter();
		Collection<Bundle> workspaceBundles = bundleRegion.getActivatedBundles();
		Collection<Bundle> bErrorDepClosures = bs
				.sortDeclaredProvidingBundles(initialBundleSet, workspaceBundles);
		Collection<Bundle> bundles = null;
		for (Bundle errorBundle : bErrorDepClosures) {
			IProject errorProject = bundleRegion.getRegisteredBundleProject(errorBundle);
			TransitionError transitionError = bundleTransition.getError(errorBundle);
			if (null != errorProject
					&& (transitionError == TransitionError.DUPLICATE || transitionError == TransitionError.CYCLE)) {
				if (null == bundles) {
					bundles = new LinkedHashSet<Bundle>();
				}
				bundles.addAll(bs.sortDeclaredRequiringBundles(Collections.<Bundle>singletonList(errorBundle),
						workspaceBundles));
			}
		}
		if (null != bundles) {
			if (null != pDepClosures) {
				pDepClosures.removeAll(bundleRegion.getBundleProjects(bundles));
			}
			if (null != bDepClosures) {
				bDepClosures.removeAll(bundles);
			}
			initialBundleSet.removeAll(bundles);
		}
		return bundles;
	}

	/**
	 * Removes pending workspace projects and their requiring projects from the specified projects and dependency closures
	 * of duplicate projects to workspace bundles.
	 * 
	 * @param projects duplicate candidates to workspace bundles
	 * @param bDepClosures existing dependency closure of bundles to the specified candidate projects. May be null.
	 * @param pDepClosures TODO
	 * @param scope TODO
	 * @param message information message added to the end of the error sent to the log view if duplicates are detected.
	 *          Null is allowed.
	 * @return all duplicates and the requiring dependency closure for each duplicate or null if no duplicates found.
	 */
	protected Collection<IProject> removeWorkspaceDuplicates(Collection<IProject> projects,
			Collection<Bundle> bDepClosures, Collection<IProject> pDepClosures, Collection<IProject> scope,
			String message) {

		Map<IProject, IProject> wsDuplicates = bundleRegion.getWorkspaceDuplicates(projects, scope);
		Collection<IProject> duplicateClosures = null;
		if (wsDuplicates.size() > 0) {
			duplicateClosures = new ArrayList<IProject>();
			ProjectSorter ps = new ProjectSorter();
			ps.setAllowCycles(true);
			for (Map.Entry<IProject, IProject> key : wsDuplicates.entrySet()) {
				IProject duplicateProject = key.getKey();
				IProject duplicateProject1 = key.getValue();
				try {
					// If checked on an uninstalled bundle that is not registered yet
					if (!bundleCommand.isBundleProjectRegistered(duplicateProject)) {
						bundleCommand.registerBundleProject(duplicateProject, null, false);
					}
					bundleTransition.setTransitionError(duplicateProject, TransitionError.DUPLICATE);
					DuplicateBundleException duplicateBundleException = new DuplicateBundleException(
							"duplicate_of_ws_bundle", duplicateProject.getName(),
							BundleProjectSettings.getSymbolicNameFromManifest(duplicateProject1), duplicateProject1.getLocation());
					handleDuplicateException(duplicateProject, duplicateBundleException, message);
					Collection<IProject> requiringProjects = ps.sortRequiringProjects(Collections
							.<IProject>singletonList(duplicateProject));
					if (requiringProjects.size() > 0) {
						for (IProject reqProject : requiringProjects) {
							bundleTransition.removePending(reqProject, Transition.UPDATE);
							bundleTransition.removePending(reqProject, Transition.UPDATE_ON_ACTIVATE);
						}
						projects.removeAll(requiringProjects);
						duplicateClosures.addAll(requiringProjects);
						if (null != bDepClosures) {
							bDepClosures.removeAll(bundleRegion.getBundles(requiringProjects));
						}
						if (null != pDepClosures) {
							pDepClosures.removeAll(requiringProjects);
						}
					} else {
						projects.remove(duplicateProject);
						duplicateClosures.add(duplicateProject);
					}
				} catch (ProjectLocationException e) {
					addError(e, e.getLocalizedMessage(), duplicateProject);
				}
			}
		}
		return duplicateClosures;
	}

	/**
	 * Removes pending workspace projects and their requiring projects from the specified projects and dependency closures
	 * of duplicate projects to external bundles. -- Detect bundles which are duplicates of external bundles -- Can not
	 * let update detect the duplicate -- OSGi will refresh all dependent bundles of the jar bundle -- and suspend the
	 * refreshPackages and not return -- See private void suspendBundle(AbstractBundle bundle) { -- attempt to suspend the
	 * bundle or obtain the state change lock -- Note that this may fail but we cannot quit the -- refreshPackages
	 * operation because of it. (bug 84169)
	 * 
	 * @param projects duplicate candidates to external bundles
	 * @param bDepClosures existing dependency closure of bundles to the specified candidate projects. May be null.
	 * @param message information message added to the end of the error sent to the log view if duplicates are detected.
	 *          Null is allowed.
	 * @return all duplicates and the requiring dependency closure for each duplicate or null if no duplicates found.
	 */
	protected Collection<IProject> removeExternalDuplicates(Collection<IProject> projects,
			Collection<Bundle> bDepClosures, String message) {

		if (!InPlace.get().isRefreshDuplicateBSNAllowed()) {
			return null;
		}
		Map<IProject, Bundle> externalDuplicates = bundleRegion.getSymbolicNameDuplicates(projects,
				bundleRegion.getJarBundles(), true);
		Collection<IProject> duplicateClosures = null;
		if (externalDuplicates.size() > 0) {
			duplicateClosures = new ArrayList<IProject>();
			ProjectSorter ps = new ProjectSorter();
			ps.setAllowCycles(true);
			for (Map.Entry<IProject, Bundle> key : externalDuplicates.entrySet()) {
				IBundleStatus startStatus = null;
				IProject duplicate = key.getKey();
				try {
					// If checked on an uninstalled bundle that is not registered yet
					if (!bundleCommand.isBundleProjectRegistered(duplicate)) {
						bundleCommand.registerBundleProject(duplicate, null, false);
					}
					bundleTransition.setTransitionError(duplicate, TransitionError.DUPLICATE);
					String msg = ErrorMessage.getInstance().formatString("duplicate_of_jar_bundle",
							duplicate.getName(), key.getValue().getSymbolicName(), key.getValue().getLocation());
					startStatus = addError(null, msg);
					Collection<IProject> requiringProjects = ps.sortRequiringProjects(Collections
							.<IProject>singletonList(duplicate));
					if (requiringProjects.size() > 0) {
						String affectedBundlesMsg = ErrorMessage.getInstance().formatString("duplicate_affected_bundles",
								duplicate.getName(), BundleProjectState.formatProjectList(requiringProjects));
						addInfoMessage(affectedBundlesMsg);
						for (IProject reqProject : requiringProjects) {
							bundleTransition.removePending(reqProject, Transition.UPDATE);
							bundleTransition.removePending(reqProject, Transition.UPDATE_ON_ACTIVATE);
						}
						projects.removeAll(requiringProjects);
						duplicateClosures.addAll(requiringProjects);
						if (null != bDepClosures) {
							bDepClosures.removeAll(bundleRegion.getBundles(requiringProjects));
						}
					} else {
						projects.remove(duplicate);
						duplicateClosures.add(duplicate);
					}
					if (null != message) {
						addInfoMessage(message);
					}
					String rootMsg = ErrorMessage.getInstance().formatString("detected_duplicate_of_jar_bundle");
					createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, rootMsg), startStatus);
				} catch (ProjectLocationException e) {
					addError(e, e.getLocalizedMessage(), duplicate);
				}
			}
		}
		return duplicateClosures;
	}

	/**
	 * Compares bundle projects for the same symbolic name and version. Each specified project is compared to all other
	 * valid workspace projects as specified by {@link ProjectProperties#getInstallableProjects()}.
	 * <p>
	 * When duplicates are detected the providing project - if any - in a set of duplicates is treated as the one to be
	 * installed or updated while the rest of the duplicates in the set are those left uninstalled or not updated. This
	 * becomes indirectly evident from the formulation of the error messages sent to the log view.
	 * <p>
	 * Any errors that occurs while retrieving the symbolic name and version of bundles are added to the job status list
	 * 
	 * @param duplicateProject duplicate project
	 * @param duplicateException the duplicate exception object associated with the specified duplicate project
	 * @param message TODO
	 * @return a list of duplicate tuples. Returns an empty list if no duplicates are found.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @see #getStatusList()
	 */
	Collection<IProject> handleDuplicateException(IProject duplicateProject,
			DuplicateBundleException duplicateException, String message) throws CircularReferenceException {

		// List of detected duplicate tuples
		Collection<IProject> duplicates = new LinkedHashSet<IProject>();
		TransitionError transitionError = TransitionError.DUPLICATE;

		try {
			transitionError = bundleTransition.getError(duplicateProject);
		} catch (ProjectLocationException e) {
			addError(e, e.getLocalizedMessage(), duplicateProject);
		}

		String duplicateCandidateKey = bundleRegion.getSymbolicKey(null, duplicateProject);
		if (null == duplicateCandidateKey || duplicateCandidateKey.length() == 0) {
			String msg = ErrorMessage.getInstance().formatString("project_symbolic_identifier",
					duplicateProject.getName());
			addError(null, msg, duplicateProject);
			return null;
		}
		ProjectSorter ps = new ProjectSorter();
		ps.setAllowCycles(true);
		Collection<IProject> installableProjects = ProjectProperties.getInstallableProjects();
		installableProjects.remove(duplicateProject);
		for (IProject duplicateProjectCandidate : installableProjects) {
			IBundleStatus startStatus = null;
			try {
				String symbolicKey = bundleRegion.getSymbolicKey(null, duplicateProjectCandidate);
				if (null == symbolicKey || symbolicKey.length() == 0) {
					String msg = ErrorMessage.getInstance().formatString("project_symbolic_identifier",
							duplicateProjectCandidate.getName());
					addError(null, msg, duplicateProjectCandidate);
					continue;
				}
				if (symbolicKey.equals(duplicateCandidateKey)) {
					throw new DuplicateBundleException("duplicate_bundle_project", symbolicKey,
							duplicateProject.getName(), duplicateProjectCandidate.getName());
				}
			} catch (DuplicateBundleException e) {
				// Build the multi status error log message
				String msg = null;
				try {
					msg = ErrorMessage.getInstance().formatString("duplicate_error",
							BundleProjectSettings.getSymbolicNameFromManifest(duplicateProject),
							BundleProjectSettings.getBundleVersionFromManifest(duplicateProject));
					startStatus = addError(e, msg, duplicateProject);
					addError(null, e.getLocalizedMessage());
					addError(duplicateException, duplicateException.getLocalizedMessage(), duplicateProject);
					// Inform about the requiring projects of the duplicate project
					Collection<IProject> duplicateClosureSet = ps.sortRequiringProjects(Collections
							.<IProject>singleton(duplicateProject));
					duplicateClosureSet.remove(duplicateProject);
					if (duplicateClosureSet.size() > 0) {
						String affectedBundlesMsg = ErrorMessage.getInstance().formatString("duplicate_affected_bundles",
								duplicateProject.getName(), BundleProjectState.formatProjectList(duplicateClosureSet));
						addInfoMessage(affectedBundlesMsg);
					}
					if (null != message) {
						addInfoMessage(message);
					}
					String rootMsg = ExceptionMessage.getInstance().formatString("root_duplicate_exception");
					createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, rootMsg), startStatus);
				} catch (InPlaceException e1) {
					addError(e1, e1.getLocalizedMessage());
				} finally {
					duplicates.add(duplicateProjectCandidate);
				}
			} catch (ProjectLocationException e) {
				addError(e, e.getLocalizedMessage(), duplicateProject);
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), duplicateProject);
			}
		}
		try {
			bundleTransition.setTransitionError(duplicateProject, transitionError);
		} catch (ProjectLocationException e) {
			addError(e, e.getLocalizedMessage(), duplicateProject);
		}
		return duplicates;
	}

	protected CommandOptions getOptionsService() throws InPlaceException {
		return InPlace.get().getCommandOptionsService();
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
