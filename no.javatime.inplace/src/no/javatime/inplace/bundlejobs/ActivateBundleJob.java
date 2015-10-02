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
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
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
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

public class ActivateBundleJob extends NatureJob implements ActivateBundle {

	// Bundles to activate
	private Collection<Bundle> activatedBundles;
	private ProjectSorter projectSorter = new ProjectSorter();

	/**
	 * Default constructor with a default job name
	 */
	public ActivateBundleJob() {
		super(Msg.ACTIVATE_BUNDLE_JOB);
		init();
	}

	/**
	 * Construct an activate job with a given job name
	 * 
	 * @param name job name
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name) {
		super(name);
		init();
	}

	/**
	 * Constructs an activation job with a given job name and pending bundle projects to activate
	 * 
	 * @param name job name
	 * @param projects pending projects to activate
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name, Collection<IProject> projects) {
		super(name, projects);
		init();
	}

	/**
	 * Constructs an activation job with a given job name and a pending bundle project to activate
	 * 
	 * @param name job name
	 * @param project pending project to activate
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name, IProject project) {
		super(name, project);
	}

	private void init() {
		
		activatedBundles = null;
	}
	
	@Override
	public void end() {
		super.end();		
		init();
	}

	/**
	 * Runs the bundle(s) activation operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(Msg.ACTIVATE_BUNDLE_JOB, getTicks());
			activate(monitor);
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
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
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
	 * @throws ExtenderException If failing to get an extender service
	 * @throws BackingStoreException Failure to access the preference store for bundle states 
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 */
	protected IBundleStatus activate(IProgressMonitor monitor) throws OperationCanceledException,
			InterruptedException, InPlaceException, ExtenderException, BackingStoreException, IllegalStateException {

		// At least one project must be activated (nature enabled) for workspace bundles to be activated
		if (isProjectWorkspaceActivated()) {
			// If this is the first set of workspace project(s) that have been activated no bundle(s) have
			// been activated yet and all deactivated bundles should be installed in an activated
			// workspace
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
				BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
				multiStatus.add(e.getStatusList());
				addStatus(multiStatus);
				// Remove all pending projects that participate in the cycle(s)
				if (null != e.getProjects()) {
					removePendingProjects(e.getProjects());
				}
			}
			activatedBundles = install(getPendingProjects(), monitor);
			if (!getLastErrorStatus().isOK()) {
				Deactivate daj = new DeactivateJob(Msg.DEACTIVATE_BUNDLES_JOB, getPendingProjects());
				Activator.getBundleExecutorEventService().add(daj, 0);
				return addStatus(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));
			}
			// All circular references, closed and non-existing projects should have been discarded by now
			handleDuplicates();
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
			if (messageOptions.isBundleOperations())
				addLogStatus(Msg.ACTIVATED_BUNDLES_INFO, new Object[] { bundleRegion.formatBundleList(
						activatedBundles, true) }, Activator.getContext().getBundle());
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
		// Set the bundle class path on start up in case settings (dev and/or update bundle class path) are
		// changed
		if (getName().equals(Msg.STARTUP_ACTIVATE_BUNDLE_JOB)
				&& (null != bundleProjectMeta.inDevelopmentMode() || commandOptions
						.isUpdateDefaultOutPutFolder())) {
			for (Bundle bundle : activatedBundles) {
				resolveBundleClasspath(bundleRegion.getProject(bundle));
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		StatePersistParticipant.restoreSessionState();
		start(activatedBundles, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
		return getLastErrorStatus();
	}
	/**
	 * Remove external and workspace duplicates from the the pending projects to activate.
	 * 
	 * @return {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. Return
	 * {@code IBundleStatus} status with a {@code StatusCode.BUILDERROR} if there are projects with
	 * build errors and there are no pending projects left. If there are build errors they are added
	 * to the job status list.
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 * @throws CircularReferenceException if cycles are detected among the specified projects
	 */
	private IBundleStatus handleDuplicates() throws InPlaceException, CircularReferenceException {

		IBundleStatus status = createStatus();
		bundleTransition.removeTransitionError(TransitionError.DUPLICATE);
		Collection<IProject> externalDuplicates = getExternalDuplicateClosures(getPendingProjects(),
				null);
		if (null != externalDuplicates) {
			removePendingProjects(externalDuplicates);
		}
		Collection<IProject> duplicates = removeWorkspaceDuplicates(getPendingProjects(), null, null,
				bundleProjectCandidates.getInstallable(), Msg.DUPLICATE_WS_BUNDLE_INSTALL_ERROR);
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
	 * Restore bundle state and any pending transitions from the previous session. Bundles in state
	 * resolve of the IDE should retain their resolved state from their previous
	 * session.
	 * <p>
	 * An exception to this rule, is to start bundles to only resolve when activated bundles to start
	 * have requirements on a bundle to only resolve.
	 * <p>
	 * Note that additional bundles to start are added and bundles to not start are removed from the
	 * set of bundles to activate
	 * 
	 * @throws CircularReferenceException - if cycles are detected in the bundle graph
	 */
//	private void restoreSessionStates() throws CircularReferenceException {
//
//		if (getIsRestoreSessionState() && null != activatedBundles && activatedBundles.size() > 0) {
//			IEclipsePreferences prefs = Activator.getEclipsePreferenceStore();
//			if (null == prefs) {
//				addStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
//						Msg.INIT_WORKSPACE_STORE_WARN, null));
//				return;
//			}
//			Collection<Bundle> resolveStateBundles = StatePersistParticipant.restoreActivationLevel(prefs,
//					activatedBundles, Bundle.RESOLVED);
//			if (resolveStateBundles.size() > 0) {
//				// Do not start bundles that were in state resolved at last shutdown
//				activatedBundles.removeAll(resolveStateBundles);
//				// This is an additional verification check in case stored states or bundle activation mode
//				// has been changed since last shutdown
//				BundleClosures bc = new BundleClosures();
//				Collection<Bundle> providingBundles = bc.bundleActivation(Closure.PROVIDING,
//						activatedBundles, resolveStateBundles);
//				// Does any bundles to only resolve provide capabilities to any of the bundles to start
//				providingBundles.retainAll(resolveStateBundles);
//				if (providingBundles.size() > 0) {
//					// Start these, due to requirements from bundles to start
//					activatedBundles.addAll(providingBundles);
//					if (messageOptions.isBundleOperations()) {
//						String msg = NLS.bind(Msg.CONDITIONAL_START_BUNDLE_INFO,
//								new Object[] { bundleRegion.formatBundleList(providingBundles, true) });
//						addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
//					}
//				}
//			}
//			StatePersistParticipant.restorePendingTransitions(prefs);
//			// Set to recovery mode in case of abnormal shutdown of the IDE
//			StatePersistParticipant.saveSessionState(true);
//		}
//	}

	@SuppressWarnings("unused")
	private IBundleStatus removeBuildErrorClosures(Collection<Bundle> activatedBundles)
			throws InPlaceException, CircularReferenceException {

		IBundleStatus status = createStatus();
		Collection<IProject> bundleErrorClosures = null;
		Collection<IProject> activatedProjects = bundleRegion.getProjects(activatedBundles);
		BundleBuildErrorClosure be = null;
		try {
			be = new BundleBuildErrorClosure(activatedProjects, Transition.ACTIVATE_BUNDLE,
					Closure.REQUIRING, Bundle.INSTALLED, ActivationScope.ACTIVATED);
			if (be.hasBuildErrors()) {
				bundleErrorClosures = be.getBuildErrorClosures();
				activatedProjects.removeAll(bundleErrorClosures);
				if (messageOptions.isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			}
			be = new BundleBuildErrorClosure(activatedProjects, Transition.ACTIVATE_BUNDLE, Closure.PROVIDING,
					Bundle.INSTALLED, ActivationScope.ALL);
			if (be.hasBuildErrors()) {
				if (null != bundleErrorClosures) {
					bundleErrorClosures.addAll(be.getBuildErrorClosures());
				} else {
					bundleErrorClosures = be.getBuildErrorClosures();
				}
				if (messageOptions.isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			}
			if (null != bundleErrorClosures) {

				Deactivate daj = new DeactivateJob(Msg.DEACTIVATE_BUNDLES_JOB, bundleErrorClosures);
				Activator.getBundleExecutorEventService().add(daj, 0);
				removePendingProjects(bundleErrorClosures);
				if (null != activatedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(bundleErrorClosures);
					// Do not resolve activated bundle closures with build errors
					for (Bundle bundle : bundleErrorClosure) {
						bundleRegion.setActivation(bundle, false);
					}
					activatedBundles.removeAll(bundleErrorClosure);
				}
			}
		} catch (CircularReferenceException e) {
			bundleErrorClosures = be.getBuildErrors();
			if (bundleErrorClosures.size() > 0) {
				removePendingProjects(bundleErrorClosures);
				if (null != activatedBundles) {
					Collection<Bundle> bundleErrorClosure = bundleRegion.getBundles(bundleErrorClosures);
					activatedBundles.removeAll(bundleErrorClosure);
				}
			}
		}
		return status;
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
