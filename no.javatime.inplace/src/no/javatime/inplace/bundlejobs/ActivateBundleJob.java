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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ManifestUtil;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Bundle;

/**
 * Activates a bundle or set of bundles by installing, resolving and starting the bundle(s). A bundle is only activated
 * if its corresponding project already has been activated. See
 * {@link no.javatime.inplace.bundlejobs.ActivateProjectJob ActivateProjectJob} for activating projects. The workspace
 * is activated if this is the first pending project or set of pending projects that have been activated. If no projects
 * have been activated the workspace is said to be deactivated and all bundles are in state UNINSTALLED
 * <p>
 * The following principles and conditions determine the states bundles are moved to when their projects are activated:
 * <ol>
 * <li>When the first project or set of projects in the workspace have been activated the workspace is said to be
 * activated and bundles for not activated projects are installed (state INSTALLED) and bundles for activated projects
 * are installed, resolved and started (state ACTIVE or STARTING).
 * <li>If the workspace is activated when this job is scheduled bundles of new pending activated projects are resolved
 * and started (state ACTIVE or STARTING).
 * <li>When reactivating the workspace at startup deactivated bundles are installed and activated bundles are moved to
 * the same state as at shut down. Possible states for activated bundles are RESOLVED, ACTIVE and STARTING.
 * <li>If bundles to activate are dependent on other providing bundles, the independent providing bundles are added to
 * the job. The dependency is transitive.
 * </ol>
 * <p>
 * It is both a prerequisite and guaranteed by this package that all providing projects to the pending projects of this
 * job are activated (nature enabled) when this job is scheduled. Providing projects are either activated when a
 * requiring project is activated in {@link no.javatime.inplace.bundlejobs.ActivateProjectJob ActivateProjectJob} or
 * scheduled for project activation in the {@link no.javatime.inplace.builder.PostBuildListener PostBuildListener} when
 * a new deactivated project is imported by an activated project. Lastly, if none if this holds a deactivated project
 * providing capabilities to an activated bundle is scheduled for activation in the internal resolver hook and the
 * requiring activated bundles are excluded from the resolve list and then resolved when the deactivated project is
 * activated.
 * 
 * 
 * @see ActivateProjectJob
 * @see DeactivateJob
 * 
 */
public class ActivateBundleJob extends BundleJob {

	/** Reactivating or activating bundles at start up */
	final public static String activateStartupJobName = Message.getInstance().formatString(
			"startup_activate_bundle_job_name");
	/** Name to the set of operations needed to activate a bundle */
	final private static String activateTaskName = Message.getInstance().formatString(
			"activate_bundle_task_name");

	// Activate bundles according to their state in preference store
	private Boolean useStoredState = false;
	final private static String duplicateMessage = ErrorMessage.getInstance().formatString(
			"duplicate_ws_bundle_install");

	/**
	 * Construct an activate job with a given job name
	 * 
	 * @param name job name
	 * @see #activateJobName
	 * @see #activateStartupJobName
	 */
	public ActivateBundleJob(String name) {
		super(name);
	}

	/**
	 * Constructs an activation job with a given job name and pending bundle projects to activate
	 * 
	 * @param name job name
	 * @param projects pending projects to activate
	 * @see #activateJobName
	 * @see #activateStartupJobName
	 */
	public ActivateBundleJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs an activation job with a given job name and a pending bundle project to activate
	 * 
	 * @param name job name
	 * @param project pending project to activate
	 * @see #activateJobName
	 * @see #activateStartupJobName
	 */
	public ActivateBundleJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle(s) activation operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no status
	 *         objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the job fails or
	 *         {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(activateTaskName, getTicks());
			activate(monitor);
		} catch (InterruptedException e) {
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
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		}
	}

	/**
	 * Install, resolve and start pending bundles to activate. A bundle is marked as activated if its corresponding
	 * project is activated (nature enabled). If no projects are activated the activate bundle job will terminate
	 * silently. If the workspace is in a deactivated state and there are bundles to activate
	 * all deactivated bundles are installed.
	 * <p>
	 * Closed and non-existing projects are discarded.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return Status of last added {@code IBundleStatus} object is returned or a {@code IBundleStatus} status with a
	 *         {@code StatusCode.OK} if no errors. All failures are added to the job status list.
	 * @throws OperationCanceledException after install and resolve
	 * @throws InterruptedException Checks for and interrupts right before call to start bundle. Start is also interrupted
	 *           if the task running the stop method is terminated abnormally (timeout or manually)
	 * @throws InPlaceException if encountering closed or non-existing projects after they are discarded or a bundle to
	 *           activate becomes null
	 */
	private IBundleStatus activate(IProgressMonitor monitor) throws OperationCanceledException,
			InterruptedException, InPlaceException {

		Collection<Bundle> activatedBundles = null;
		ProjectSorter projectSorter = new ProjectSorter();
		// At least one project must be activated (nature enabled) for workspace bundles to be activated
		if (ProjectProperties.isProjectWorkspaceActivated()) {
			// If this is the first set of workspace project(s) that have been activated no bundle(s) have been activated yet
			// and all deactivated bundles should be installed in an activated workspace (except erroneous bundles)
			if (!bundleRegion.isBundleWorkspaceActivated()) {
				addPendingProjects(ProjectProperties.getPlugInProjects());
			} else {
				// If any, add uninstalled bundles to be installed in an activated workspace
				for (IProject project : ProjectProperties.getPlugInProjects()) {
					if (null == bundleRegion.get(project)) {
						addPendingProject(project);
					}
				}
			}
			// Add providing projects and remove projects with build errors, cycles, duplicates and affected
			// dependent projects before installing
			try {
				resetPendingProjects(projectSorter.sortProvidingProjects(getPendingProjects()));
			} catch (CircularReferenceException e) {
				String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
				BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
				multiStatus.add(e.getStatusList());
				addStatus(multiStatus);
				// Remove all pending projects that participate in the cycle(s)
				if (null != e.getProjects()) {
					removePendingProjects(e.getProjects());
				}
			}
			// All circular references, closed and non-existing projects should have been discarded by now
			removeErrorClosures(projectSorter, activatedBundles);
			activatedBundles = install(getPendingProjects(), monitor);
		}
		// No projects are activated or no activated bundle projects have been installed
		if (null == activatedBundles || activatedBundles.size() == 0) {
			return getLastStatus();
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// At this point the workspace is activated and all remaining workspace bundles are free of errors and at
		// least installed. Resolve and start activated bundles
		// Only resolve bundles in state installed
		Collection<Bundle> bundlesToResolve = new LinkedHashSet<Bundle>(activatedBundles.size());
		// Only resolve bundles in state installed
		for (Bundle bundle : activatedBundles) {
			if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
				bundlesToResolve.add(bundle);
			}
		}
		if (bundlesToResolve.size() == 0) {
			if (InPlace.get().msgOpt().isBundleOperations())
				InPlace.get().trace("already_activated",
						bundleRegion.formatBundleList(activatedBundles, true));
			return getLastStatus();
		}
		Collection<Bundle> notResolvedBundles = resolve(bundlesToResolve, new SubProgressMonitor(monitor, 1));
		if (notResolvedBundles.size() > 0) {
			// This should include dependency closures, so no dependent bundles should be started
			activatedBundles.removeAll(notResolvedBundles);
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// Set the bundle class path on start up if settings (dev and/or update bundle class path) are changed
		if (getName().equals(ActivateBundleJob.activateStartupJobName)
				&& (null != BundleProject.inDevelopmentMode() || getOptionsService().isUpdateDefaultOutPutFolder())) {
			for (Bundle bundle : activatedBundles) {
				resolveBundleClasspath(bundleRegion.getProject(bundle));
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		restoreSessionStates(activatedBundles);
		start(activatedBundles, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
		return getLastStatus();
	}

	/**
	 * Remove build error closure bundles from the set of activated projects. If there are no activated projects left the
	 * error closures are added to the status list. Otherwise the error closures are only removed from the set of pending
	 * projects.
	 * 
	 * @param projectSorter topological sort of error closure
	 * @return {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. Return {@code IBundleStatus} status
	 *         with a {@code StatusCode.BUILDERROR} if there are projects with build errors and there are no pending
	 *         projects left. If there are build errors they are added to the job status list.
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 * @throws CircularReferenceException if cycles are detected among the specified projects
	 */
	private IBundleStatus removeErrorClosures(ProjectSorter projectSorter, Collection<Bundle> installedBundles)
			throws InPlaceException, CircularReferenceException {

		IBundleStatus status = createStatus();
		Collection<IProject> projectErrorClosures = null;
		try {
			projectErrorClosures = projectSorter.getRequiringBuildErrorClosure(getPendingProjects());
			if (projectErrorClosures.size() > 0) {
				String msg = ProjectProperties.formatBuildErrorsFromClosure(projectErrorClosures, getName());
				if (null != msg) {
					status = addBuildError(msg, null);
				}
				removePendingProjects(projectErrorClosures);
			}
			bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
			removeExternalDuplicates(getPendingProjects(), null, null);
			Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null,
					ProjectProperties.getInstallableProjects(), duplicateMessage);
			if (null != duplicates) {
				Collection<IProject> installedRequirers = projectSorter.sortRequiringProjects(duplicates, true);
				if (installedRequirers.size() > 0) {
					removePendingProjects(installedRequirers);
				}
			}

		} catch (CircularReferenceException e) {
			projectErrorClosures = ProjectProperties.getBuildErrors(getPendingProjects());
			projectErrorClosures.addAll(ProjectProperties.hasBuildState(getPendingProjects()));
			if (projectErrorClosures.size() > 0) {
				removePendingProjects(projectErrorClosures);
				if (null != installedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(projectErrorClosures);
					installedBundles.removeAll(bundleErrorClosure);
				}
			}
		}
		return status;
	}

	/**
	 * Restore bundle state from previous session. If active bundles to start has requirements on a bundle to resolve,
	 * start the bundle to resolve if the dependency option on the bundle allows it.
	 * 
	 * @param bundles bundles to activate
	 * @param bs dependency sorter
	 * @throws InPlaceException if one of the specified bundles is null
	 */
	private void restoreSessionStates(Collection<Bundle> bundles) throws InPlaceException {
		BundleSorter bundleSorter = new BundleSorter();
		if (getUseStoredState() && bundles.size() > 0) {
			IEclipsePreferences store = InPlace.getEclipsePreferenceStore();
			if (null != store) {
				// Default is to start the bundle, so only consider bundles with state resolved
				for (Bundle bundle : bundleRegion.getActivatedBundles()) {
					if (bundles.contains(bundle) && !ManifestUtil.isFragment(bundle)) {
						String symbolicKey = bundleRegion.getSymbolicKey(bundle, null);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						int state = 0;
						try {
							state = store.getInt(symbolicKey, Bundle.UNINSTALLED);
						} catch (IllegalStateException e) {
							continue; // Node removed
						}
						if ((state & (Bundle.RESOLVED)) != 0) {
							// If active bundles to start have requirements on this bundle,
							// start the bundle if the dependency option allow it
							Collection<Bundle> reqBundles = bundleSorter.sortRequiringBundles(
									Collections.<Bundle>singletonList(bundle), bundleRegion.getActivatedBundles());
							reqBundles.remove(bundle);
							Boolean startBundle = false;
							if (reqBundles.size() > 0) {
								for (Bundle reqBundle : reqBundles) {
									// Fragments are only resolved (not started)
									if (ManifestUtil.isFragment(reqBundle)) {
										continue;
									}
									int reqState = 0;
									String reqKey = bundleRegion.getSymbolicKey(reqBundle, null);
									if (reqKey.isEmpty()) {
										continue;
									}
									try {
										reqState = store.getInt(reqKey, Bundle.UNINSTALLED);
									} catch (IllegalStateException e) {
										continue; // Node removed
									}
									// The activated and requiring bundle will be started
									if ((reqState & (Bundle.UNINSTALLED)) != 0) {
										String msg = WarnMessage.getInstance().formatString("has_uninstalled_requiring_bundles",
												bundleRegion.formatBundleList(reqBundles, true),
												bundleRegion.getSymbolicKey(bundle, null));
										addWarning(null, msg, null);
										startBundle = true;
										break;
									}
								}
							}
							if (startBundle) {
								// To start the bundle, the dependency option for start should include providing closure
								if (getDepOpt().get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING) 
										|| getDepOpt().get(Operation.ACTIVATE_BUNDLE, Closure.SINGLE)) {
									bundles.remove(bundle); // Do not start this bundle
								} else {
									String msg = WarnMessage.getInstance().formatString("starting_bundle", bundle);
									addWarning(null, msg, null);
								}
							} else {
								bundles.remove(bundle); // Do not start this bundle
							}
						}
					}
				}
			}
			InPlace.get().savePluginSettings(true, true);
		}
	}

	/**
	 * Set preference for activating bundles according to bundle state in preference store
	 * 
	 * @param useStoredState true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public void setUseStoredState(Boolean useStoredState) {
		this.useStoredState = useStoredState;
	}

	/**
	 * Check if to activate bundles according to bundle state in preference store
	 * 
	 * @return true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public Boolean getUseStoredState() {
		return useStoredState;
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public static int getTicks() {
		return 3; // install (activate workspace), resolve, start
	}
}
