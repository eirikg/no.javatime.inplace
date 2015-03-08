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
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class ActivateBundleJob extends NatureJob implements ActivateBundle {

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
	 * Default constructor wit a default job name
	 */
	public ActivateBundleJob() {
		super(activateJobName);
	}
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
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list and
	 * {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if any
	 * status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(activateTaskName, getTicks());
			activate(monitor);
			// TODO Add extender exception to all jobs
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
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
	}

	/**
	 * Install, resolve and start pending bundles to activate. A bundle is marked as activated if its
	 * corresponding project is activated (nature enabled). If no projects are activated the activate
	 * bundle job will terminate silently. If the workspace is in a deactivated state and there are
	 * bundles to activate all deactivated bundles are installed.
	 * <p>
	 * Closed and non-existing projects are discarded.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return Status of last added {@code IBundleStatus} object is returned or a
	 * {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. All failures are added
	 * to the job status list.
	 * @throws OperationCanceledException after install and resolve
	 * @throws InterruptedException Checks for and interrupts right before call to start bundle. Start
	 * is also interrupted if the task running the stop method is terminated abnormally (timeout or
	 * manually)
	 * @throws InPlaceException if encountering closed or non-existing projects after they are
	 * discarded or a bundle to activate becomes null
	 */
	private IBundleStatus activate(IProgressMonitor monitor) throws OperationCanceledException,
			InterruptedException, InPlaceException {

		Collection<Bundle> activatedBundles = null;
		ProjectSorter projectSorter = new ProjectSorter();
		// At least one project must be activated (nature enabled) for workspace bundles to be activated
		if (isProjectWorkspaceActivated()) {
			// If this is the first set of workspace project(s) that have been activated no bundle(s) have
			// been activated yet and all deactivated bundles should be installed in an activated workspace
			if (!bundleRegion.isRegionActivated()) {
				addPendingProjects(bundleProjectCandidates.getBundleProjects());
			} else {
				Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
				projects.removeAll(getPendingProjects());
				// If any, add uninstalled bundles to be installed in an activated workspace
				for (IProject project : projects) {
					if (null == bundleRegion.getBundle(project)) {
						addPendingProject(project);
					}
				}
			}
			// Add providing projects and remove projects with build errors, cycles, duplicates and
			// affected dependent projects before installing
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
			activatedBundles = install(getPendingProjects(), monitor);
			if (!getLastErrorStatus().isOK()) {
				DeactivateJob daj = new DeactivateJob(DeactivateJob.deactivateJobName, getPendingProjects());
				BundleJobManager.addBundleJob(daj, 0);
				return addStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, Msg.INSTALL_ERROR));
			}			
			// All circular references, closed and non-existing projects should have been discarded by now
			handleDuplicates(projectSorter, activatedBundles);

			// Build errors are checked upon project activation
			if (getName().equals(activateStartupJobName)) {
				// removeBuildErrorClosures(activatedBundles);
			}
		}
		// No projects are activated or no activated bundle projects have been installed
		if (null == activatedBundles || activatedBundles.size() == 0) {
			return getLastErrorStatus();
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// At this point the workspace is activated and all remaining workspace bundles are free of
		// errors and at least installed. Resolve and start activated bundles
		// Only resolve bundles in state installed
		Collection<Bundle> bundlesToResolve = new LinkedHashSet<Bundle>(activatedBundles.size());
		// Only resolve bundles in state installed
		for (Bundle bundle : activatedBundles) {
			if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
				bundlesToResolve.add(bundle);
			}
		}
		if (bundlesToResolve.size() == 0) {
			if (InPlace.get().getMsgOpt().isBundleOperations())
				addLogStatus(Msg.ACTIVATED_BUNDLES_INFO, new Object[] { bundleRegion.formatBundleList(
						activatedBundles, true) }, InPlace.getContext().getBundle());
			return getLastErrorStatus();
		}
		Collection<Bundle> notResolvedBundles = resolve(bundlesToResolve, new SubProgressMonitor(
				monitor, 1));
		if (notResolvedBundles.size() > 0) {
			// This should include dependency closures, so no dependent bundles should be started
			activatedBundles.removeAll(notResolvedBundles);
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// Set the bundle class path on start up if settings (dev and/or update bundle class path) are
		// changed
		if (getName().equals(ActivateBundleJob.activateStartupJobName)
				&& (null != bundleProjectMeta.inDevelopmentMode() || getOptionsService()
						.isUpdateDefaultOutPutFolder())) {
			for (Bundle bundle : activatedBundles) {
				resolveBundleClasspath(bundleRegion.getProject(bundle));
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		restoreSessionStates(activatedBundles);
		start(activatedBundles, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
		return getLastErrorStatus();
	}

	@SuppressWarnings("unused")
	private IBundleStatus removeBuildErrorClosures(Collection<Bundle> activatedBundles)
			throws InPlaceException, CircularReferenceException {

		IBundleStatus status = createStatus();
		Collection<IProject> projectErrorClosures = null;
		Collection<IProject> activatedProjects = bundleRegion.getProjects(activatedBundles);
		try {
			BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.ACTIVATE_BUNDLE,
					Closure.REQUIRING, Bundle.INSTALLED, ActivationScope.ACTIVATED);
			if (be.hasBuildErrors()) {
				projectErrorClosures = be.getBuildErrorClosures();
				activatedProjects.removeAll(projectErrorClosures);
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			}
			be = new BuildErrorClosure(activatedProjects, Transition.ACTIVATE_BUNDLE, Closure.PROVIDING,
					Bundle.INSTALLED, ActivationScope.ALL);
			if (be.hasBuildErrors()) {
				if (null != projectErrorClosures) {
					projectErrorClosures.addAll(be.getBuildErrorClosures());
				} else {
					projectErrorClosures = be.getBuildErrorClosures();
				}
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			}
			if (null != projectErrorClosures) {

				DeactivateJob daj = new DeactivateJob(DeactivateJob.deactivateJobName, projectErrorClosures);
				BundleJobManager.addBundleJob(daj, 0);
				removePendingProjects(projectErrorClosures);
				if (null != activatedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(projectErrorClosures);
					// Do not resolve activated bundle closures with build errors
					for (Bundle bundle : bundleErrorClosure) {
						bundleRegion.setActivation(bundle, false);
					}
					activatedBundles.removeAll(bundleErrorClosure);
				}
			}
		} catch (CircularReferenceException e) {
			projectErrorClosures = BuildErrorClosure.getBuildErrors(getPendingProjects());
			projectErrorClosures.addAll(BuildErrorClosure.hasBuildState(getPendingProjects()));
			if (projectErrorClosures.size() > 0) {
				removePendingProjects(projectErrorClosures);
				if (null != activatedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(projectErrorClosures);
					activatedBundles.removeAll(bundleErrorClosure);
				}
			}
		}
		return status;
	}

	// TODO Change method comment
	/**
	 * Remove build error closure bundles from the set of activated projects. If there are no
	 * activated projects left the error closures are added to the status list. Otherwise the error
	 * closures are only removed from the set of pending projects.
	 * 
	 * @param projectSorter topological sort of error closure
	 * @return {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. Return
	 * {@code IBundleStatus} status with a {@code StatusCode.BUILDERROR} if there are projects with
	 * build errors and there are no pending projects left. If there are build errors they are added
	 * to the job status list.
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 * @throws CircularReferenceException if cycles are detected among the specified projects
	 */
	private IBundleStatus handleDuplicates(ProjectSorter projectSorter,
			Collection<Bundle> installedBundles) throws InPlaceException, CircularReferenceException {

		IBundleStatus status = createStatus();
		bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
		removeExternalDuplicates(getPendingProjects(), null, null);
		Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null,
				bundleProjectCandidates.getInstallable(), duplicateMessage);
		if (null != duplicates) {
			Collection<IProject> installedRequirers = projectSorter.sortRequiringProjects(duplicates,
					true);
			if (installedRequirers.size() > 0) {
				removePendingProjects(installedRequirers);
			}
		}

		return status;
	}

	/**
	 * Restore bundle state from previous session. If activated bundles to start has requirements on a
	 * bundle to resolve, start the bundle to resolve if the dependency option on the bundle allows
	 * it.
	 * 
	 * @param bundles bundles to activate
	 * @param bs dependency sorter
	 * @throws InPlaceException if one of the specified bundles is null
	 */
	private void restoreSessionStates(Collection<Bundle> bundles) throws InPlaceException {
		BundleSorter bundleSorter = new BundleSorter();
		if (getPersistState() && bundles.size() > 0) {
			IEclipsePreferences store = InPlace.getEclipsePreferenceStore();
			if (null == store) {
				addStatus(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
						Msg.INIT_WORKSPACE_STORE_WARN, null));
				return;
			}
			// Default is to start the bundle, so it is sufficient to handle bundles with state resolved
			Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
			for (Bundle bundle : activatedBundles) {
				if (bundles.contains(bundle) && !bundleProjectMeta.isFragment(bundle)) {
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
								Collections.<Bundle> singletonList(bundle), activatedBundles);
						reqBundles.remove(bundle);
						Boolean startBundle = false;
						if (reqBundles.size() > 0) {
							for (Bundle reqBundle : reqBundles) {
								// Fragments are only resolved (not started)
								if (bundleProjectMeta.isFragment(reqBundle)) {
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
									if (InPlace.get().getMsgOpt().isBundleOperations()) {
										String msg = NLS.bind(Msg.UNINSTALLED_REQUIRING_BUNDLES_INFO, 
												new Object[] {bundleRegion.formatBundleList(reqBundles, true), 
												bundleRegion.getSymbolicKey(bundle, null)});
										addLogStatus(msg, bundle, null);
									}
									startBundle = true;
									break;
								}
							}
						}
						if (startBundle) {
							// To start the bundle, the dependency option for start should include providing
							// closure
							if (getDepOpt().get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING)
									|| getDepOpt().get(Operation.ACTIVATE_BUNDLE, Closure.SINGLE)) {
								bundles.remove(bundle); // Do not start this bundle
							} else {
								if (InPlace.get().getMsgOpt().isBundleOperations()) {
									addLogStatus(NLS.bind(Msg.CONDITIONAL_START_BUNDLE_INFO, bundle), bundle, null);
								}
							}
						} else {
							bundles.remove(bundle); // Do not start this bundle
						}
					}
				}
			}
			InPlace.get().savePluginSettings(true, true);
		}
	}

	@Override
	public void setPersistState(Boolean persist) {
		this.useStoredState = persist;
	}

	@Override
	public Boolean getPersistState() {
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
