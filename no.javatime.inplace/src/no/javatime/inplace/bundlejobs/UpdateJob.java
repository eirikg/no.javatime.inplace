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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.DuplicateBundleException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class UpdateJob extends BundleJob implements Update {

	final private static String updateTaskName = Message.getInstance().formatString(
			"update_task_name");
	final private static String updateSubTaskName = Message.getInstance().formatString(
			"update_subtask_name");

	/**
	 * Default constructor wit a default job name
	 */
	public UpdateJob() {
		super(updateJobName);
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
	 * Runs the bundle(s) update operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list and
	 * {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if any
	 * status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(updateTaskName, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			update(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
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
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
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

		// (1) Collect any additional bundles to update and the requiring closure to stop,
		// resolve/refresh and start

		Collection<Bundle> bundlesToUpdate = bundleRegion.getBundles(getPendingProjects());
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();

		// If auto build has been switched off and than switched on,
		// include all bundles that have been built
		if (bundleRegion.isAutoBuildActivated(true)) {
			Collection<Bundle> pendingBundles = bundleTransition.getPendingBundles(activatedBundles,
					Transition.UPDATE);
			pendingBundles.removeAll(bundlesToUpdate);
			if (pendingBundles.size() > 0) {
				bundlesToUpdate.addAll(pendingBundles);
				addPendingProjects(bundleRegion.getProjects(pendingBundles));
			}
		}

		Collection<Bundle> bundleClosure = null;
		// (2) Get any requiring closure of bundles to update and refresh
		if (getOptionsService().isRefreshOnUpdate()) {
			// Get the requiring closure of bundles to update and add bundles to the requiring closure
			// with same symbolic name as in the update closure.
			// The bundles in this closure are stopped before update, bound to the current revision of the
			// updated bundles during refresh and then started again if in state ACTIVE
			bundleClosure = getBundlesToRefresh(bundlesToUpdate, activatedBundles);

			// Necessary to get the latest updated version of a coherent set (closure) of running bundles
			Collection<Bundle> pendingBundles = bundleTransition.getPendingBundles(bundleClosure,
					Transition.UPDATE);
			pendingBundles.removeAll(bundlesToUpdate);
			if (pendingBundles.size() > 0) {
				bundlesToUpdate.addAll(pendingBundles);
				addPendingProjects(bundleRegion.getProjects(pendingBundles));
			}
		} else {
			// The requiring closure will be bound to the previous revision of the bundles to update and
			// resolve
			bundleClosure = bundlesToUpdate;
		}

		// (3) Adjust the set of bundles to update and refresh due to duplicates
		// Remove workspace bundles that are duplicates of external bundles
		Collection<IProject> duplicateProjects = removeExternalDuplicates(getPendingProjects(),
				bundleClosure, currentExternalInstance);
		if (null != duplicateProjects) {
			bundlesToUpdate.removeAll(bundleRegion.getBundles(duplicateProjects));
		}

		if (!bundleTransition.containsPending(bundlesToUpdate, Transition.UPDATE, false)) {
			return getLastErrorStatus();
		}

		// (4) Collect all bundles to restart after update and refresh
		Collection<Bundle> bundlesToRestart = new LinkedHashSet<Bundle>();
		// ACTIVE (and STARTING) bundles are restored to their current state or as directed by pending
		// operations assigned to the bundle. Activated bundles in state INSTALLED (this indicates a
		// corrected bundle error or an activation of the bundle) are started after update
		for (Bundle bundle : bundleClosure) {
			if (bundleTransition.containsPending(bundle, Transition.START, Boolean.TRUE)
					|| (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.INSTALLED)) != 0) {
				bundlesToRestart.add(bundle);
			}
		}
		// (5) Stop bundles collected in (4)
		stop(bundlesToRestart, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (6) Update bundles
		Collection<IBundleStatus> errorStatusList = updateByReference(bundlesToUpdate,
				new SubProgressMonitor(monitor, 1));
		// (7) Report any update errors
		handleUpdateExceptions(errorStatusList, bundleClosure);
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (8) Refresh updated bundles and their closures or resolve updated bundles
		if (bundleClosure.size() > 0) {
			// Also resolve/refresh and start all activated bundles in state installed and their requiring
			// bundles
			Collection<Bundle> installedBundles = bundleRegion.getBundles(Bundle.INSTALLED);
			installedBundles.removeAll(bundleClosure);
			installedBundles.removeAll(bundleRegion.getDeactivatedBundles());
			if (installedBundles.size() > 0) {
				BundleSorter bs = new BundleSorter();
				Collection<Bundle> installedBundlesToRefresh = bs.sortDeclaredRequiringBundles(
						installedBundles, activatedBundles);
				bundleClosure.addAll(installedBundlesToRefresh);
				bundlesToRestart.addAll(installedBundlesToRefresh);
			}

			if (getOptionsService().isRefreshOnUpdate()) {
				refresh(bundleClosure, new SubProgressMonitor(monitor, 1));
			} else {
				Collection<Bundle> notResolvedBundles = resolve(bundleClosure, new SubProgressMonitor(
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
		// (9) Start all bundles stopped in (4)
		start(bundlesToRestart, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));

		// (10) Restore any transition errors from before update
		// Successful start operations removes the error transition added when updated.
		// Add the error transition to bundles that failed to be updated.
		if (errorStatusList.size() > 0) {
			IBundleStatus status = null;
			for (IBundleStatus bundleError : errorStatusList) {
				Bundle bundle = bundleError.getBundle();
				if (null != bundle) {
					try {
						IProject project = bundleRegion.getProject(bundle);
						Throwable updExp = bundleError.getException();
						if (null != updExp && updExp instanceof DuplicateBundleException) {
							bundleTransition.setTransitionError(project, TransitionError.DUPLICATE);
						} else {
							bundleTransition.setTransitionError(project);
						}
					} catch (ProjectLocationException e) {
						if (null == status) {
							String msg = ExceptionMessage.getInstance()
									.formatString("error_setting_bundle_error");
							status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, null);
						}
						IProject project = bundleRegion.getProject(bundle);
						if (null != project) {
							status.add(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project, project
									.getName(), e));
						}
					}
				}
			}
			if (null != status) {
				addStatus(status);
			}
		}
		return getLastErrorStatus();
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
					localMonitor.subTask(updateSubTaskName + bundle.getSymbolicName());
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					bundleCommand.update(bundle);
				} catch (DuplicateBundleException e) {
					handleDuplicateException(bundleRegion.getProject(bundle), e, null);
					String msg = ErrorMessage.getInstance().formatString("duplicate_error",
							bundle.getSymbolicName(), bundle.getVersion().toString());
					IBundleStatus result = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID,
							bundle.getBundleId(), msg, e);
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
					statusList.add(result);
				} catch (InPlaceException e) {
					IBundleStatus result = addError(e, e.getLocalizedMessage(), bundle);
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

	private static String currentExternalInstance = ErrorMessage.getInstance().formatString(
			"current_revision_of_jar_duplicate");
	private static String currentWorkspaceInstance = ErrorMessage.getInstance().formatString(
			"current_revision_of_ws_duplicate");

	private void removeDuplicates(Collection<IProject> projectsToUpdate,
			Collection<Bundle> bundlesToUpdate, Collection<Bundle> bDepClosures)
			throws OperationCanceledException {

		Collection<IProject> duplicateProjects = removeExternalDuplicates(projectsToUpdate,
				bDepClosures, currentExternalInstance);
		if (null != duplicateProjects) {
			bundlesToUpdate.removeAll(bundleRegion.getBundles(duplicateProjects));
		}
		// TODO detected by the update operation. do some more testing here
		// duplicateProjects = removeWorkspaceDuplicates(projectsToUpdate, bDepClosures, null,
		// BundleProjectCandidatesImpl.INSTANCE.getInstallableProjects(),
		// currentWorkspaceInstance);
		// if (null != duplicateProjects) {
		// bundlesToUpdate.removeAll(bundleRegion.getBundles(duplicateProjects));
		// }
	}

	/**
	 * Formats and log exceptions specified in the status list. Removes all bundles and their
	 * requiring bundles with an update exception from being refreshed by removing them from the
	 * specified bundles to refresh
	 * 
	 * @param errorStatusList bundles with an status object to remove from bundles to refresh
	 * @param bundlesTorRefresh is the bundles to refresh
	 */
	private void handleUpdateExceptions(Collection<IBundleStatus> errorStatusList,
			Collection<Bundle> bundlesTorRefresh) {

		Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
		if (errorStatusList.size() > 0) {
			for (IBundleStatus bundleStatus : errorStatusList) {
				Bundle bundle = bundleStatus.getBundle();
				if (null != bundle) {
					BundleSorter bs = new BundleSorter();
					Collection<Bundle> affectedBundles = bs.sortDeclaredRequiringBundles(
							Collections.singleton(bundle), bundlesTorRefresh);
					bundles.addAll(affectedBundles);
				}
			}
			bundlesTorRefresh.removeAll(bundles);
		}
	}

	public Collection<IProject> getUpdateOrder() {
		Collection<Bundle> pendingBundles = bundleTransition.getPendingBundles(bundleRegion.getActivatedBundles(),
				Transition.UPDATE);
		Collection<Bundle> orderedBundles = getUpdateOrder(pendingBundles, false);
		Collection<IProject> orderedProjects = bundleRegion.getProjects(orderedBundles);
		return orderedProjects;
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
	 * @return ordered collection of bundles to update
	 */
	public Collection<Bundle> getUpdateOrder(Collection<Bundle> bundlesToUpdate,
			boolean reportCollisions) {

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
			createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, rootMsg), status);
		}
		return sortedBundlesToUpdate;
	}

	public void addUpdateTransition(Collection<IProject> projects) {

		for (IProject project : projects) {
			bundleTransition.addPending(project, Transition.UPDATE);
		}
	}

	public boolean isPendingForUpdate(IProject project) {

		return bundleTransition.containsPending(project, Transition.UPDATE, false);
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
