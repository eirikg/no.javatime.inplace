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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundlemanager.DuplicateBundleException;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundlemanager.ProjectLocationException;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.UserMessage;

/**
 * Job to update modified bundles after a build of activated projects. The job is automatically scheduled
 * after a build of activated bundle projects. The scheduled bundles are updated and together with their
 * requiring bundles, unresolved, resolved, optionally refreshed and started as part of the update process.
 * <p>
 * Bundles are stopped and started again after update and resolve if ACTIVE/STARTING before update or refresh
 * and resolved if in state RESOLVE when the job is scheduled.
 * <p>
 * Plug-ins are usually singletons, and it is a requirement, if the plug-in contributes to the UI. When
 * resolving bundles, a collision may occur with earlier resolved bundles with the same symbolic name. In
 * these cases the duplicate (the earlier resolved bundle) is removed in the resolving process.
 * <p>
 * In case a bundle to be updated is dependent on other not activated bundles, it is handled in the resolver
 * hook. The resolver hook is visited by the framework during resolve.
 */
public class UpdateJob extends BundleJob {

	final private static String updateSubTaskName = Message.getInstance().formatString("update_subtask_name");

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
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(Message.getInstance().formatString("update_task_name"), getTicks());
			update(monitor);
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
			return (IBundleStatus) super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		}
	}

	/**
	 * Updates pending bundles with the JavaTime nature that have been built, due to changes in source.
	 * <p>
	 * <ol>
	 * <li>All bundles scheduled for update have been activated and are in least in state INSTALLED
	 * <li>Active (ACTIVE/STARTING) bundles are stopped in dependency order and moved to state RESOLVED
	 * <li>Any active requiring bundles of pending bundles to update are stopped before update and started after
	 * update
	 * <li>Duplicate candidates are registered in the resolver hook visited by the framework while updating
	 * <li>All bundles are updated and refreshed or resolved if auto refresh is off
	 * <li>Pending bundles in state ACTIVE/STARTING before update are started in dependency order.
	 * <li>Pending bundles in state INSTALLED before update are started.
	 * </ol>
	 * <p>
	 * Both update and refresh starts bundles if they are active on beforehand. Stop bundles before, calling
	 * update and optionally refresh and start them afterwards.
	 * 
	 * @param monitor It is the caller's responsibility to call done() on the given monitor.
	 * @return status object describing the result of updating with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 */
	private IBundleStatus update(IProgressMonitor monitor) throws InPlaceException, InterruptedException, CoreException {

		// (1) Collect any additional bundles to update
		// Update all bundles that are part of an activation process when the update on build option is switched
		// off
		Collection<IProject> activateProjects = bundleTransition.getPendingProjects(
				bundleRegion.getProjects(true), Transition.UPDATE_ON_ACTIVATE);
		if (activateProjects.size() > 0) {
			for (IProject project : activateProjects) {
				bundleTransition.removePending(project, Transition.UPDATE_ON_ACTIVATE);
				addPendingProject(project);
			}
		} 
		// This is an optimization to reduce the number of update jobs, by including pending projects
		// waiting for the next update job. See post build listener for delayed projects on update
		if (Category.getState(Category.autoUpdate)) {
			addPendingProjects(bundleTransition.getPendingProjects(bundleRegion.getProjects(true),
					Transition.UPDATE));
		}
		Collection<Bundle> bundlesToUpdate = bundleRegion.getBundles(getPendingProjects());

		// (2) Include requiring bundles to refresh (and resolve) due to changes in projects to update
		Collection<Bundle> bundlesToRefresh = getBundlesToResolve(bundlesToUpdate);

		// (3) Reduce the set of bundles to update and refresh due to bundles with errors
		removeErrorBundles(getPendingProjects(), bundlesToUpdate, bundlesToRefresh);
		if (bundlesToUpdate.size() == 0 || 
				bundleTransition.getPendingProjects(getPendingProjects(), Transition.UPDATE).size() == 0) {
			if (Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("not_updated");
			return getLastStatus();
		}
		// (4): Collect all bundles to restart after update and refresh
		Collection<Bundle> bundlesToRestart = new LinkedHashSet<Bundle>();
		// ACTIVE (and STARTING) bundles are restored to their current state or as directed by operations assigned
		// to the bundle. Activated bundles in state INSTALLED (this indicates a corrected bundle error) are
		// started
		for (Bundle bundle : bundlesToRefresh) {
			if (bundleTransition.containsPending(bundle, Transition.START, Boolean.TRUE)
					|| (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.INSTALLED)) != 0) {
				bundlesToRestart.add(bundle);
			}
		}
		// (5) Stop bundles collected in (4)
		stop(bundlesToRestart, EnumSet.of(Integrity.RESTRICT), new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (6) Update remaining bundles
		Collection<IBundleStatus> errorStatusList = updateByReference(bundlesToUpdate, new SubProgressMonitor(
				monitor, 1));
		// (7) Report any update errors
		handleUpdateExceptions(errorStatusList, bundlesToRefresh);
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// (8) Refresh or resolve updated bundles and their requiring bundles
		if (bundlesToRefresh.size() > 0) {
			// Also resolve/refresh and start all activated bundles in state installed and their requiring bundles
			Collection<Bundle> installedBundles = bundleRegion.getBundles(Bundle.INSTALLED);
			installedBundles.removeAll(bundlesToRefresh);
			installedBundles.removeAll(bundleRegion.getDeactivatedBundles());
			if (installedBundles.size() > 0) {
				BundleSorter bs = new BundleSorter();
				Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
				Collection<Bundle> installedBundlesToRefresh = bs.sortDeclaredRequiringBundles(installedBundles,
						activatedBundles);
				bundlesToRefresh.addAll(installedBundlesToRefresh);
				bundlesToRestart.addAll(installedBundlesToRefresh);
			}

			if (Category.getState(Category.autoRefresh)) {
				refresh(bundlesToRefresh, new SubProgressMonitor(monitor, 1));
			} else {
				Collection<Bundle> notResolvedBundles = resolve(bundlesToRefresh, new SubProgressMonitor(monitor, 1));
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
		start(bundlesToRestart, EnumSet.of(Integrity.PROVIDING), new SubProgressMonitor(monitor, 1));

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
							String msg = ExceptionMessage.getInstance().formatString("error_setting_bundle_error");
							status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, null);
						}
						IProject project = bundleRegion.getProject(bundle);
						if (null != project) {
							status.add(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project,
									project.getName(), e));
						}
					}
				}
			}
			if (null != status) {
				addStatus(status);
			}
		}
		return getLastStatus();
	}

	/**
	 * Updates the specified bundles using an input stream
	 * 
	 * @param bundles to update
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of updating with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 * @see BundleCommandImpl#update(Bundle)
	 */
	private Collection<IBundleStatus> updateByReference(Collection<Bundle> bundles, SubProgressMonitor monitor) {

		// Contains duplicate candidate bundles to be removed in the resolver hook in case of singleton collisions
		Map<Bundle, Set<Bundle>> duplicateInstanceGroups = new HashMap<Bundle, Set<Bundle>>();
		Set<Bundle> duplicateInstanceCandidates = new LinkedHashSet<Bundle>();
		Collection<IBundleStatus> statusList = null;
		SubMonitor localMonitor = SubMonitor.convert(monitor, bundles.size());
		for (Bundle bundle : getUpdateOrder(bundles)) {
			if (bundleTransition.containsPending(bundle, Transition.UPDATE, true)) {
				try {
					localMonitor.subTask(updateSubTaskName + bundle.getSymbolicName());
					if (Category.getState(Category.progressBar))
						sleep(sleepTime);
					// Set conditions in the resolver hook for removal of duplicates to avoid singleton collisions
					duplicateInstanceCandidates.add(bundle);
					duplicateInstanceGroups.put(bundle, duplicateInstanceCandidates);
					bundleCommand.getResolverHookFactory().setGroups(duplicateInstanceGroups);
					bundleCommand.update(bundle);
				} catch (DuplicateBundleException e) {
					handleDuplicateException(bundleRegion.getProject(bundle), e, null);
					String msg = ErrorMessage.getInstance().formatString("duplicate_error", bundle.getSymbolicName(),
							bundle.getVersion().toString());
					IBundleStatus result = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, bundle.getBundleId(), msg, e);
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
					statusList.add(result);
				} catch (InPlaceException e) {
					IBundleStatus result = addError(e, e.getLocalizedMessage(), bundle.getBundleId());
					if (null == statusList) {
						statusList = new LinkedHashSet<IBundleStatus>();
					}
					statusList.add(result);
				} finally {
					// Resolver hook has been visited during update.
					duplicateInstanceGroups.clear();
					duplicateInstanceCandidates.clear();
					localMonitor.worked(1);
				}
			}
		}
		if (null == statusList) {
			return Collections.emptySet();
		} else {
			return statusList;
		}
	}
	private static String currentExternalInstance = ErrorMessage.getInstance().formatString("current_revision_of_jar_duplicate");
	private static String currentWorkspaceInstance= ErrorMessage.getInstance().formatString("current_revision_of_ws_duplicate");

	private void removeErrorBundles(Collection<IProject> projectsToUpdate, Collection<Bundle> bundlesToUpdate,
			Collection<Bundle> bDepClosures) throws OperationCanceledException {

		Collection<IProject> pDepClosures = bundleRegion.getProjects(bDepClosures);
		removeBuildErrorClosures(bundlesToUpdate, bDepClosures, pDepClosures);
		Collection<IProject> duplicateProjects = removeExternalDuplicates(projectsToUpdate, bDepClosures, currentExternalInstance);
		if (null != duplicateProjects) {
			bundlesToUpdate.removeAll(bundleRegion.getBundles(duplicateProjects));
		}
		duplicateProjects = removeWorkspaceDuplicates(projectsToUpdate, bDepClosures, null, ProjectProperties.getInstallableProjects(), 
				currentWorkspaceInstance);
		if (null != duplicateProjects) {
			bundlesToUpdate.removeAll(bundleRegion.getBundles(duplicateProjects));
		}
	}

	/**
	 * Formats and log exceptions specified in the status list. Removes all bundles and their requiring bundles
	 * with an update exception from being refreshed by removing them from the specified bundles to refresh
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
					Collection<Bundle> affectedBundles = bs.sortDeclaredRequiringBundles(Collections.singleton(bundle),
							bundlesTorRefresh);
					bundles.addAll(affectedBundles);
				}
			}
			bundlesTorRefresh.removeAll(bundles);
		}
	}

	/**
	 * Detect circular symbolic name collisions and order the specified collection of bundles based on existing
	 * and new symbolic keys (symbolic name and version) before they are updated.
	 * <p>
	 * Bundles must be ordered when a bundle changes its symbolic key to the same symbolic key as an other
	 * bundle to update, and this other bundle at the same time changes its symbolic key to a new value. The
	 * other bundle must then be updated first to avoid that the first bundle becomes a duplicate of the other
	 * bundle. A special case, called a circular name collision, occurs if the other bundle in addition changes
	 * it symbolic key to the same as the current (or existing) symbolic key of the first bundle.
	 * 
	 * 
	 * @param bundlesToUpdate collection of bundles to update
	 * @return ordered collection of bundles to update
	 */
	protected Collection<Bundle> getUpdateOrder(Collection<Bundle> bundlesToUpdate) {

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
				Bundle collisionBundle = bundleRegion.get(collisionProject);
				if (null == collisionBundle) {
					continue;
				}
				String collisionBundleKey = bundleRegion.getSymbolicKey(collisionBundle, null);
				// Existing symbolic key of the other bundle is the same as the new symbolic key of this bundle
				if (collisionBundleKey.equals(newProjectKey)) {
					String msg = ErrorMessage.getInstance().formatString("circular_names", bundle, newProjectKey,
							collisionBundle, bundleRegion.getSymbolicKey(null, collisionProject));
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
		if (null != status) {
			String rootMsg = ErrorMessage.getInstance().formatString("circular_name_conflict");
			createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, rootMsg), status);
		}
		return sortedBundlesToUpdate;
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
