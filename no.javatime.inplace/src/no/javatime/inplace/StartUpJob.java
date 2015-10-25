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
package no.javatime.inplace;

import java.util.Collection;

import no.javatime.inplace.builder.AutoBuildListener;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Start up activates or deactivates bundle projects at start up. The activation
 * level, transition state and pending transitions added to bundles is determined by the
 * {@link StatePersistParticipant} class.
 * <p>
 * If activated bundle projects had build errors at shut down or the "Deactivate on Exit" preference
 * option was on at shutdown (or manually changed to on after shutdown), the workspace will be
 * deactivated, if not at shutdown, but otherwise activated.
 * <p>
 * After an abnormal termination of the workspace, states are regenerated based on activation rules
 * and states from the previous session according to rules in {@code StatePersistParticipant}.
 */
class StartUpJob extends ActivateBundleJob {

	/**
	 * Construct a startup job with a given name
	 * 
	 * @param name job name
	 */
	public StartUpJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to start
	 * 
	 * @param name job name
	 * @param projects projects to toggle
	 */
	public StartUpJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a startup job with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to toggle
	 */
	public StartUpJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) startup operation
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if the job terminated
	 * normally or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 * status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			BundleTransitionListener.addBundleTransitionListener(this);
			// If workspace session is true it implies an IDE crash, and false indicates a normal exit
			boolean isRecoveryMode = StatePersistParticipant.isWorkspaceSession();
			// Setting the session to false signals that the workbench is not yet up and running
			// and the workbench state must be recovered at startup
			StatePersistParticipant.setWorkspaceSession(!isRecoveryMode);
			initServices();
			startTime = System.currentTimeMillis();
			startUpInit();
			Collection<IProject> activatedPendingProjects = getPendingProjects();
			if (activatedPendingProjects.size() > 0) {
				// Indicates an IDE crash. Bundle projects have not been deactivated at shutdown
				if (!deactivateWorkspace(monitor, activatedPendingProjects, isRecoveryMode)) {
					// Activate workspace
					if (isRecoveryMode) {
						addLogStatus(Msg.RECOVERY_RESOLVE_BUNDLE_INFO);
					}
					// Bundle projects are registered when installed by the activate bundle job
					super.runInWorkspace(monitor);
				}
			} else {
				// Deactivated workspace
				registerBundleProjects();
				if (isRecoveryMode) {
					addLogStatus(Msg.RECOVERY_NO_ACTION_BUNDLE_INFO);
				}
				StatePersistParticipant.restoreSessionState();
			}
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			addError(e, msg);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			addError(e, msg);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,
					null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (InPlaceException | ExtenderException e) {
			addError(e, Msg.INIT_BUNDLE_STATE_ERROR);
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			try {
				// Flag a normal start up (after recovery)
				StatePersistParticipant.setWorkspaceSession(true);
			} catch (IllegalStateException e) {
				String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
				addError(e, msg);
			} catch (BackingStoreException e) {
				String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
				addError(e, msg);
			}		
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();
	}

	/**
	 * Deactivate workspace if the "deactivate on exit" preference is on or if there are build errors
	 * among the specified projects or any requiring projects to the specified projects
	 * <p>
	 * If the IDE terminated the bundles are refreshed (this is strictly not necessary)
	 * 
	 * @param monitor progress monitor
	 * @param activatedPendingProjects All activated bundle projects in workspace
	 * @return True if the workspace has been deactivated. Otherwise false
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws InPlaceException Failing to access an open project
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * 
	 */
	private boolean deactivateWorkspace(IProgressMonitor monitor,
			Collection<IProject> activatedPendingProjects, boolean isRecoveryMode)
			throws IllegalStateException, ExtenderException, InPlaceException, BackingStoreException {
		
		SubMonitor progress = SubMonitor.convert(monitor, activatedPendingProjects.size());
		try {
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			Collection<IProject> bundleProjects = bundleProjectCandidates.getBundleProjects();
			Collection<IProject> projectsToDeactivate = SessionManager.isDeactivateOnExit(bundleProjects, activatedPendingProjects); 
			if (projectsToDeactivate.size() > 0) {
				setName(Msg.DEACTIVATE_WORKSPACE_JOB);
				monitor.beginTask(Msg.DEACTIVATE_TASK_JOB, getTicks());
				deactivateNature(activatedPendingProjects, new SubProgressMonitor(monitor, 1));
				if (isRecoveryMode) {
					// Bundles have not been refreshed when IDE crashes
					boolean isBundleOperation = messageOptions.isBundleOperations();
					Collection<Bundle> bundles = null;
					try {
						messageOptions.setIsBundleOperations(false);
						try {
							install(bundleProjects, monitor);
						} catch (InPlaceException | WorkspaceDuplicateException | ProjectLocationException e) {
						}
						bundles = bundleRegion.getBundles();
						uninstall(bundles, monitor, false, false);
					} finally {
						messageOptions.setIsBundleOperations(isBundleOperation);						
					}
					refresh(bundles, monitor);
					addLogStatus(Msg.RECOVERY_DEACTIVATE_BUNDLE_INFO);
				} 
				if (commandOptions.isDeactivateOnExit()) {
					addLogStatus(Msg.STARTUP_DEACTIVATE_ON_EXIT_INFO);
				} else {
					addLogStatus(Msg.STARTUP_DEACTIVATE_BUILD_ERROR_INFO);
				}
				StatePersistParticipant.restoreSessionState();
				return true;
			}
		} finally {
			progress.worked(1);
		}
		return false;
	}

	private void startUpInit() {

		final IBundleStatus multiStatus = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
				"Session Settings");
		final Activator activator = Activator.getInstance();
		activator.processLastSavedState();
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (null != workbench && !workbench.isStarting()) {
			// Not strictly necessary to run in an UI thread
			Activator.getDisplay().asyncExec(new Runnable() {
				public void run() {
					// Adding at this point should ensure that all static contexts are loaded
					IBundleStatus status = activator.addDynamicExtensions();
					if (messageOptions.isBundleOperations()) {
						if (null != status) {
							multiStatus.add(status);
						}
					}
				}
			});
			// Listen to toggling of auto build
			ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
			if (null != service) {
				service.addExecutionListener(new AutoBuildListener());
			} else {
				addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
						Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
			}
		} else {
			addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
					Msg.DYNAMIC_MONITORING_WARN));
			addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
					Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
		}
		// Log development mode
		if (messageOptions.isBundleOperations()) {
			String osgiDev = bundleProjectMeta.inDevelopmentMode();
			if (null != osgiDev) {
				String msg = NLS.bind(Msg.CLASS_PATH_DEV_PARAM_INFO, osgiDev);
				multiStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
			}
		}
		addLogStatus(multiStatus);
	}

	/**
	 * Register all bundle projects in the workspace region assuming that no bundle projects have been
	 * installed
	 * <p>
	 * Bundles are registered when first installed
	 */
	private Collection<IProject> registerBundleProjects() {

		Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
		for (IProject project : projects) {
			bundleRegion.registerBundleProject(project, null, false);
		}
		return projects;
	}
}
