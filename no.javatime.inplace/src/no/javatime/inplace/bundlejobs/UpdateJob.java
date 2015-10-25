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

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.UpdateScheduler;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.ExternalDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class UpdateJob extends BundleJob implements Update {

	/**
	 * Default constructor wit a default job name
	 */
	public UpdateJob() {
		super(Msg.UPDATE_JOB);
	}

	/**
	 * Construct an update job with a given name
	 * 
	 * @param name job name
	 */
	public UpdateJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to update
	 * 
	 * @param name job name
	 * @param projects to update
	 * @see #addPendingProjects(Collection)
	 */
	public UpdateJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs an update job with a given name and a bundle project to update
	 * 
	 * @param name job name
	 * @param project bundle project to update
	 * @see #addPendingProject(IProject)
	 */
	public UpdateJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the update bundle(s) operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.UPDATE_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			update(monitor);
		} catch (InterruptedException e) {
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
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
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
	 * Updates pending bundles with the JavaTime nature that have been built, due to changes in
	 * source.
	 * <p>
	 * <ol>
	 * <li>All bundles scheduled for update have been activated and are in least in state INSTALLED
	 * <li>Active (ACTIVE/STARTING) bundles are stopped in dependency order and moved to state
	 * RESOLVED
	 * <li>Any active requiring bundles of pending bundles to update are stopped before update and
	 * started after update
	 * <li>Duplicate candidates are registered in the resolver hook visited by the framework while
	 * updating
	 * <li>All bundles are updated and refreshed or resolved if auto refresh is off
	 * <li>Pending bundles in state ACTIVE/STARTING before update are started in dependency order.
	 * <li>Pending bundles in state INSTALLED before update are started.
	 * </ol>
	 * <p>
	 * Both update and refresh starts bundles if they are active on beforehand. Stop bundles before,
	 * calling update and optionally refresh and start them afterwards.
	 * 
	 * @param monitor It is the caller's responsibility to call done() on the given monitor.
	 * @return status object describing the result of updating with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InterruptedIOException
	 */
	private IBundleStatus update(IProgressMonitor monitor) throws InPlaceException,
			InterruptedException, CoreException {

		// (1) Collect bundle projects to update
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		Collection<Bundle> requiringClosure = getUpdateClosure(activatedBundles);
		Collection<Bundle> bundlesToUpdate = bundleRegion.getBundles(getPendingProjects());
		if (!bundleTransition.containsPending(bundlesToUpdate, Transition.UPDATE, false)) {
			return getLastErrorStatus();
		}
		// (2) Collect all bundles to restart after update and resolve/refresh
		// ACTIVE (and STARTING) bundles are restored to their current state or as directed by pending
		// operations assigned to the bundle. Activated bundles in state INSTALLED (this indicates a
		// corrected bundle error or an activation of the bundle) are started after update
		Collection<Bundle> bundlesToRestart = new LinkedHashSet<>();
		for (Bundle bundle : requiringClosure) {
			// Providing bundles of bundles tagged with start are calculated by the start operation
			if (bundleTransition.containsPending(bundle, Transition.START, Boolean.TRUE)
					|| (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.INSTALLED)) != 0) {
				bundlesToRestart.add(bundle);
			}
		}
		// (3) Stop bundles collected in (2). Bundles in state installed are ignored
		stop(bundlesToRestart, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (4) Update bundles
		Collection<IBundleStatus> errorStatusList = updateByReference(bundlesToUpdate,
				new SubProgressMonitor(monitor, 1));
		// (5) Report any update errors
		// handleUpdateExceptions(errorStatusList, requiringClosure);
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (6) Refresh updated bundles and their closures or resolve updated bundles
		if (requiringClosure.size() > 0) {
			if (commandOptions.isRefreshOnUpdate()) {
				try {
					refresh(requiringClosure, new SubProgressMonitor(monitor, 1));
				} catch (InPlaceException e) {
					bundlesToRestart.removeAll(requiringClosure);
					handleRefreshException(e, requiringClosure);
				}
			} else {
				Collection<Bundle> notResolvedBundles = resolve(requiringClosure, new SubProgressMonitor(
						monitor, 1));
				if (notResolvedBundles.size() > 0) {
					// This should include dependency closures, so no dependent bundles should be started
					bundlesToRestart.removeAll(notResolvedBundles);
				}
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (7) Start bundles stopped in (3)
		start(bundlesToRestart, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
		// (8) Restore any transition errors detected by update (4)
		restoreStatus(errorStatusList);
		return getLastErrorStatus();
	}

	/**
	 * Identify bundle projects to update based on the current set of pending bundle projects added to
	 * this job and calculate and return the requiring (update) closure of those bundle projects.
	 * 
	 * The new set of bundle projects to update may be equal, a sub set or super set within the
	 * specified domain of the current set of bundle projects to update. Identified bundle projects to
	 * update are synchronized with the current set of pending bundle projects already added to this
	 * job. The resulting set of pending bundle projects to update is a subset of the requiring
	 * (update) closure within the specified domain of bundle projects.
	 * <p>
	 * Only bundles tagged with {@code Transition.UPDATE} are added. The current set of pending
	 * projects may be empty and it is assumed that any pending projects in the set are tagged with
	 * {@code Transition.UPDATE}.
	 * <p>
	 * The following steps are executed to collect bundle projects to update:
	 * <ol>
	 * <li>Get all pending projects already added to this job.
	 * <li>If the "Refresh on Update" option is on
	 * <ol>
	 * <li>all requiring bundles tagged for update ({@code Transition.UPDATE}) to the bundles to
	 * update are added to the list of bundles to update
	 * <li>all bundles tagged with {@code Trasnition.REFRESH} and their requiring bundles are added to
	 * requiring closure
	 * </ol>
	 * <li>Duplicate workspace bundles and their requiring bundles to bundles to update are added to
	 * requiring closure (see below) of the set of bundles to update (they are in one way dependent of
	 * each other and have to be resolved)
	 * <li>Any bundles to update and their requiring bundles that are duplicates of external bundles
	 * are removed for the list of bundles to update
	 * </ol>
	 * After the set of bundle projects to update are collected, the requiring closure of those bundle
	 * projects are calculated and returned. The requiring closures is the set of activated bundles
	 * that require capabilities from the bundle projects to update.
	 * 
	 * @param domain The domain or scope of the the computed requiring closure. The domain of
	 * activated bundle projects in the workspace region to consider when calculating the requiring
	 * closure of the bundle projects to update
	 * @return The requiring (update) closure of the set of identified bundle projects to update
	 */
	private Collection<Bundle> getUpdateClosure(final Collection<Bundle> domain) {

		// The calculated requiring closure based on identified bundle projects to update
		Collection<Bundle> requiringClosure = null;
		// The initial set of bundle projects to update
		Collection<Bundle> bundlesToUpdate = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> installedBundles = bundleRegion.getBundles(domain, Bundle.INSTALLED);
		if (commandOptions.isRefreshOnUpdate()) {
			// Get the requiring closure of bundles to update. Any bundles in the domain
			// with the same symbolic name as in the requiring closure are added to the
			// requiring closure (required by the resolver). ExternalDuplicates are reported by update
			requiringClosure = getRefreshClosure(bundlesToUpdate, domain);

			// Get all activated bundles in state installed and their requiring bundles
			installedBundles.removeAll(requiringClosure);
			if (installedBundles.size() > 0) {
				BundleSorter bs = new BundleSorter();
				Collection<Bundle> installedClosure = bs.sortDeclaredRequiringBundles(installedBundles,
						domain);
				requiringClosure.addAll(installedClosure);
			}
		} else {
			// The closure will be bound to the previous revision of the bundles to update and resolve
			requiringClosure = new LinkedHashSet<>(bundlesToUpdate);
			installedBundles.removeAll(requiringClosure);
			// Add all activated bundles in state installed to the closure
			requiringClosure.addAll(installedBundles);
		}

		// Get the latest updated version of a coherent set (closure) of running bundles to update
		Collection<Bundle> pendingBundles = bundleTransition.getPendingBundles(requiringClosure,
				Transition.UPDATE);
		pendingBundles.removeAll(bundlesToUpdate);
		if (pendingBundles.size() > 0) {
			addPendingProjects(bundleRegion.getProjects(pendingBundles));
		}

		// Remove workspace bundles and their requiring bundle projects that are duplicates of external
		// bundles
//		Collection<IProject> duplicateProjects = getExternalDuplicateClosures(getPendingProjects(),
//				Msg.USE_CURR_REV_DUPLICATES_ERROR);
//		if (null != duplicateProjects) {
//			// There are no requiring closures on workspace bundles from external bundles
//			removePendingProjects(duplicateProjects);
//			requiringClosure.removeAll(bundleRegion.getBundles(duplicateProjects));
//		}
		return requiringClosure;
	}

	/**
	 * Updates the specified bundles using an input stream
	 * 
	 * @param bundles to update
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of updating with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 */
	private Collection<IBundleStatus> updateByReference(Collection<Bundle> bundles,
			SubProgressMonitor monitor) {

		Collection<IBundleStatus> statusList = null;
		SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
		for (Bundle bundle : getUpdateOrder(bundles, true)) {
			if (bundleTransition.containsPending(bundle, Transition.UPDATE, true)) {
				try {
					localMonitor.subTask(Msg.UPDATE_SUB_TASK_JOB + bundle.getSymbolicName());
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					bundleCommand.update(bundle);
				} catch (WorkspaceDuplicateException e) {
					IBundleStatus result = addError(e, e.getMessage(), bundle);
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
//					handleDuplicateException(bundleRegion.getProject(bundle), e, null);
//					String msg = ErrorMessage.getInstance().formatString("duplicate_error",
//							bundle.getSymbolicName(), bundle.getVersion().toString());
//					IBundleStatus result = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
//							bundle.getBundleId(), msg, e);
//					if (null == statusList) {
//						statusList = new LinkedHashSet<IBundleStatus>();
//					}
					statusList.add(result);
				} catch (ExternalDuplicateException e) {
					IBundleStatus result = addError(e, e.getMessage(), bundle);
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
					statusList.add(result);					
				} catch (InPlaceException e) {
					IBundleStatus result = addError(e, e.getMessage(), bundle);
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
					statusList.add(result);
				} finally {
					localMonitor.worked(1);
				}
			}
		}
		if (null == statusList) {
			return Collections.<IBundleStatus> emptySet();
		} else {
			return statusList;
		}
	}

	/**
	 * Detect circular symbolic name collisions and order the specified collection of bundles based on
	 * existing and new symbolic keys (symbolic name and version) before they are updated.
	 * <p>
	 * Bundles must be ordered when a bundle changes its symbolic key to the same symbolic key as an
	 * other bundle to update, and this other bundle at the same time changes its symbolic key to a
	 * new value. The other bundle must then be updated first to avoid that the first bundle becomes a
	 * duplicate of the other bundle. A special case, called a circular name collision, occurs if the
	 * other bundle in addition changes it symbolic key to the same as the current (or existing)
	 * symbolic key of the first bundle.
	 * 
	 * 
	 * @param bundlesToUpdate collection of bundles to update
	 * @param reportCollisions if true an error status is added to the update executor if the are any
	 * name conflicts. if false collisions are detected but no error status object is added.
	 * @return An ordered collection of bundles to update
	 */
	private Collection<Bundle> getUpdateOrder(Collection<Bundle> bundlesToUpdate,
			boolean reportCollisions) throws ExtenderException {

		ArrayList<Bundle> sortedBundlesToUpdate = new ArrayList<Bundle>();
		Map<String, IProject> projectKeysMap = new HashMap<String, IProject>();
		IBundleStatus status = null;
		if (bundlesToUpdate.size() <= 1) {
			return bundlesToUpdate;
		}
		for (Bundle bundle : bundlesToUpdate) {
			IProject project = bundleRegion.getProject(bundle);
			String newProjectKey = bundleRegion.getSymbolicKey(null, project);
			String oldBundleKey = bundleRegion.getSymbolicKey(bundle, null);
			if (newProjectKey.length() == 0 || oldBundleKey.length() == 0) {
				continue;
			}
			// Existing symbolic key of this bundle is the same as the new symbolic key of an other bundle
			if (projectKeysMap.containsKey(oldBundleKey)) {
				sortedBundlesToUpdate.add(0, bundle);
				IProject collisionProject = projectKeysMap.get(oldBundleKey);
				if (null == collisionProject) {
					continue;
				}
				Bundle collisionBundle = bundleRegion.getBundle(collisionProject);
				if (null == collisionBundle) {
					continue;
				}
				String collisionBundleKey = bundleRegion.getSymbolicKey(collisionBundle, null);
				// Existing symbolic key of the other bundle is the same as the new symbolic key of this
				// bundle
				if (collisionBundleKey.equals(newProjectKey)) {
					String msg = ErrorMessage.getInstance().formatString("circular_names", bundle,
							newProjectKey, collisionBundle, bundleRegion.getSymbolicKey(null, collisionProject));
					if (null == status) {
						status = addError(null, msg);
					} else {
						addError(null, msg);
					}
				}
			} else {
				sortedBundlesToUpdate.add(bundle);
			}
			projectKeysMap.put(newProjectKey, project);
		}
		if (null != status && reportCollisions) {
			String rootMsg = ErrorMessage.getInstance().formatString("circular_name_conflict");
			createMultiStatus(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, rootMsg), status);
		}
		return sortedBundlesToUpdate;
	}

	private Collection<Bundle> getActivatedInstalledClosure(Collection<Bundle> requiringClosure,
			Collection<Bundle> activatedBundles) {

		Collection<Bundle> installedClosure;
		Collection<Bundle> installedBundles = bundleRegion.getBundles(activatedBundles,
				Bundle.INSTALLED);
		installedBundles.removeAll(requiringClosure);
		if (installedBundles.size() > 0) {
			BundleSorter bs = new BundleSorter();
			installedClosure = bs.sortDeclaredRequiringBundles(installedBundles, activatedBundles);
			return installedClosure;
		}
		return Collections.<Bundle> emptySet();
	}

	/**
	 * Restore any transition errors on bundles detected by update.
	 * <P>
	 * Successful start operations removes the error transition added when bundles were updated.
	 * 
	 * @param errorStatusList List of error status objects of bundles that failed to be updated
	 */
	private void restoreStatus(Collection<IBundleStatus> errorStatusList) {

		if (errorStatusList.size() > 0) {
			IBundleStatus status = null;
			for (IBundleStatus bundleError : errorStatusList) {
				Bundle bundle = bundleError.getBundle();
				if (null != bundle) {
					try {
						IProject project = bundleRegion.getProject(bundle);
						Throwable updExp = bundleError.getException();
//						if (null != updExp && updExp instanceof WorkspaceDuplicateException) {
//							bundleTransition.setTransitionError(project, TransitionError.WORKSPACE_DUPLICATE);
//						} else {
//							bundleTransition.setTransitionError(project);
//						}
					} catch (ProjectLocationException e) {
						if (null == status) {
							String msg = ExceptionMessage.getInstance()
									.formatString("error_setting_bundle_error");
							status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, null);
						}
						IProject project = bundleRegion.getProject(bundle);
						if (null != project) {
							status.add(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project,
									project.getName(), e));
						}
					}
				}
			}
			if (null != status) {
				addStatus(status);
			}
		}
	}

	@Override
	public void addUpdateTransition(Collection<IProject> projects) throws ExtenderException {

		bundleTransition = Activator.getBundleTransitionService();
		for (IProject project : projects) {
			bundleTransition.addPending(project, Transition.UPDATE);
		}
	}

	@Override
	public void addUpdateTransition(IProject project) throws ExtenderException {

		bundleTransition = Activator.getBundleTransitionService();
		bundleTransition.addPending(project, Transition.UPDATE);
	}

	@Override
	public boolean isPendingForUpdate(IProject project) throws ExtenderException {

		bundleTransition = Activator.getBundleTransitionService();
		return bundleTransition.containsPending(project, Transition.UPDATE, false);
	}

	@Override
	public Collection<IProject> getRequiringUpdateClosure() {

		initServices();
		Collection<IProject> pendingProjectsCopy = new LinkedHashSet<>(getPendingProjects());
		Collection<Bundle> actiavtedBundles = bundleRegion.getActivatedBundles();
		Collection<Bundle> refreshBundles = bundleTransition.getPendingBundles(actiavtedBundles,
				Transition.REFRESH);
		Collection<Bundle> bundles = getUpdateClosure(actiavtedBundles);
		// Bundles tagged with refresh are removed by get update closure
		if (refreshBundles.size() > 0) {
			for (Bundle bundle : refreshBundles) {
				bundleTransition.addPending(bundle, Transition.REFRESH);
			}
		}
		resetPendingProjects(pendingProjectsCopy);
		return bundleRegion.getProjects(bundles);
	}

	@Override
	public Collection<IProject> getBundlesToUpdate() {

		initServices();
		Collection<IProject> pendingProjectsCopy = new LinkedHashSet<>(getPendingProjects());
		getUpdateClosure(bundleRegion.getActivatedBundles());
		Collection<IProject> resultSet = new LinkedHashSet<>(getPendingProjects());
		resetPendingProjects(pendingProjectsCopy);
		return resultSet;
	}

	public boolean canUpdate(IProject project) throws ExtenderException {

		return UpdateScheduler.addProjectToUpdateJob(project, null);
	}

	@Override
	public Collection<IProject> getUpdateOrder() throws ExtenderException {

		initServices();
		Collection<Bundle> pendingBundles = bundleTransition.getPendingBundles(
				bundleRegion.getActivatedBundles(), Transition.UPDATE);
		Collection<Bundle> orderedBundles = getUpdateOrder(pendingBundles, false);
		Collection<IProject> orderedProjects = bundleRegion.getProjects(orderedBundles);
		return orderedProjects;
	}

	/**
	 * Removes all bundles in the specified error status list and their requiring bundles from the
	 * specified bundles to refresh out parameter
	 * 
	 * @param errorStatusList Status objects for error bundles to remove from bundles to refresh
	 * @param bundlesToRefresh A non-null out parameter where any error bundles and their requiring
	 * bundles found in the error status list parameter are removed
	 */
	private void handleUpdateExceptions(Collection<IBundleStatus> errorStatusList,
			Collection<Bundle> bundlesToRefresh) {

		if (errorStatusList.size() > 0) {
			Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
			for (IBundleStatus bundleStatus : errorStatusList) {
				Bundle bundle = bundleStatus.getBundle();
				if (null != bundle) {
					BundleSorter bs = new BundleSorter();
					Collection<Bundle> affectedBundles = bs.sortDeclaredRequiringBundles(
							Collections.singleton(bundle), bundlesToRefresh);
					bundles.addAll(affectedBundles);
				}
			}
			bundlesToRefresh.removeAll(bundles);
		}
	}

	/**
	 * Adds the specified exception to the log and adds deactivate as a pending transition to bundles
	 * in state installed and uninstalled
	 * 
	 * @param throwable The refresh exception
	 * @param bundlesToResolve bundles to refresh/resolve
	 */
	private void handleRefreshException(Throwable throwable, Collection<Bundle> bundlesToResolve) {

		for (Bundle bundle : bundlesToResolve) {
			// Bundle may be rejected by the resolver hook due to dependencies on deactivated bundles
			if (bundleRegion.isBundleActivated(bundle)) {
				int state = bundleCommand.getState(bundle);
				if ((state & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
					// Must deactivate to not get an activated bundle in state installed
					bundleTransition.addPending(bundle, Transition.DEACTIVATE);
				}
			}
		}
		addError(throwable, NLS.bind(Msg.REFRESH_EXP, getName()));
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		return 4;
	}
}
