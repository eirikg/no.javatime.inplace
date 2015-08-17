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
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * In an activated workspace activate all bundle projects where the activation level (bundle state)
 * is the same as at the last shutdown. Transition states are calculated for both activated and
 * deactivated bundle projects. Any pending transitions from the previous session are added to both
 * deactivated and activated bundle projects
 * <p>
 * In a deactivated workspace the activation level for all bundle projects are
 * {@code Bundle.UNINSTALLED}. The transition state is set to the same as at shutdown and any
 * pending transition from the previous session are added. After first installation of the InPlace
 * Activator the activation level is {@code Bundle.UNINSTALLED} and the transition state is
 * {@code Transition.NO_TRANSITION}
 * <p>
 * If activated bundle projects had build errors at shut down or the "Deactivate on Exit" preference
 * option was on at shutdown (or manually changed to on after shutdown), the workspace will be
 * deactivated and the activation level, transition state and any pending transitions will be the
 * same as in a deactivated workspace.
 * <p>
 * After an abnormal termination of a session, states are regenerated based on activation rules and
 * states from the previous session. See {@link StatePersistParticipant} for further details.
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
				final IBundleStatus multiStatus = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, "Session Settings");
				initServices();
				startTime = System.currentTimeMillis();
				final Activator activator = Activator.getInstance();
				activator.processLastSavedState();
				final IWorkbench workbench = PlatformUI.getWorkbench();
				if (null != workbench && !workbench.isStarting()) {
					// Not strictly necessary to run in an UI thread
					Activator.getDisplay().asyncExec(new Runnable() {
						public void run() {
							// Adding at this point should ensure that all static contexts are loaded
							IBundleStatus status = activator.addDynamicExtensions();
							if (null != status) {
								multiStatus.add(status);
							}
						}
					});
					// Listen to toggling of auto build
					ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
					if (null == service) {
						addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
								Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
					} else {
						service.addExecutionListener(new AutoBuildListener());
					}
				} else {
					addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
							Msg.DYNAMIC_MONITORING_WARN));
					addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
							Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
				}
			
				if (messageOptions.isBundleOperations()) {
				String osgiDev = Activator.getbundlePrrojectMetaService().inDevelopmentMode();
				if (null != osgiDev) {
					String msg = NLS.bind(Msg.CLASS_PATH_DEV_PARAM_INFO, osgiDev);
					multiStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
				}
			}	
			addLogStatus(multiStatus);
			// If workspace session is true it implies an IDE crash, and false indicates a normal exit
			boolean isRecoveryMode = StatePersistParticipant.isWorkspaceSession();
			// Setting the session to false signals that the workbench is not yet up and running
			// and the workbench state must be recovered at startup
			StatePersistParticipant.setWorkspaceSession(!isRecoveryMode);
			Collection<IProject> activatedPendingProjects = getPendingProjects();
			if (activatedPendingProjects.size() > 0) {
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
			// Workspace has been recovered. Indicate a normal start up
			StatePersistParticipant.setWorkspaceSession(true);
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
			Collection<IProject> activatedPendingProjects, boolean isRecoveryMode) throws IllegalStateException,
			InPlaceException, BackingStoreException {

		if (SessionJobsInitiator.isDeactivateOnExit(activatedPendingProjects)) {
			try {
				setName(Msg.DEACTIVATE_WORKSPACE_JOB);
				BundleTransitionListener.addBundleTransitionListener(this);
				monitor.beginTask(Msg.DEACTIVATE_TASK_JOB, getTicks());
				registerBundleProjects();
				deactivateNature(activatedPendingProjects, new SubProgressMonitor(monitor, 1));
				if (isRecoveryMode) {
					// Bundles have not been refreshed when IDE crashes
					Collection<IProject> projects = bundleRegion.getProjects();
					boolean isBundleOperation = messageOptions.isBundleOperations();
					messageOptions.setIsBundleOperations(false);
					install(projects, monitor);
					Collection<Bundle> bundles = bundleRegion.getBundles();
					uninstall(bundles, monitor, false, false);
					messageOptions.setIsBundleOperations(isBundleOperation);
					refresh(bundles, monitor);
					addLogStatus(Msg.RECOVERY_DEACTIVATE_BUNDLE_INFO);
				} else {
					if (commandOptions.isDeactivateOnExit()) {
						addLogStatus(Msg.STARTUP_DEACTIVATE_ON_EXIT_INFO);
					} else {
						addLogStatus(Msg.STARTUP_DEACTIVATE_BUILD_ERROR_INFO);
					}
				}
				StatePersistParticipant.restoreSessionState();
				return true;
			} finally {
				BundleTransitionListener.removeBundleTransitionListener(this);
				monitor.done();
			}
		}
		return false;
	}

	/**
	 * Register all bundle projects in the workspace region assuming that no bundle projects have been
	 * installed
	 * <p>
	 * Bundle projects are registered when first installed
	 */
	private void registerBundleProjects() {

		Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
		for (IProject project : projects) {
			bundleRegion.registerBundleProject(project, null, false);
		}
	}
}
