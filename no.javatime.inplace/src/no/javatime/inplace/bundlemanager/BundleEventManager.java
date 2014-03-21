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
package no.javatime.inplace.bundlemanager;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandEvent;
import org.eclipse.core.commands.ICommandListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.BundleJobListener;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.UpdateScheduler;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.state.ActiveState;
import no.javatime.inplace.bundlemanager.state.BundleNode;
import no.javatime.inplace.bundlemanager.state.BundleState;
import no.javatime.inplace.bundlemanager.state.BundleStateFactory;
import no.javatime.inplace.bundlemanager.state.InstalledState;
import no.javatime.inplace.bundlemanager.state.LazyState;
import no.javatime.inplace.bundlemanager.state.ResolvedState;
import no.javatime.inplace.bundlemanager.state.UninstalledState;
import no.javatime.inplace.bundleproject.ManifestUtil;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Registers a bundle job listener and acts on events received from the OSGI framework, bundles and the log
 * service. Framework and bundle events are initiated by internal bundle operations in
 * {@link BundleCommandImpl} or from an external source. Internal bundle operations usually has its origin
 * from a bundle job.
 * <p>
 * Events not generated by BundleManager operations from {@link BundleCommandImpl} are marked as external
 * transitions and flagged as such if the user information option is switched on. BundleManager operations
 * spanning multiple transitions are adjusted according to an internal maintained state for each bundle.
 * <p>
 * If a bundle is uninstalled from an external source in an activated workspace the workspace is automatically
 * deactivated if automatic handling of external commands is switched on. If off, the user has the option to
 * deactivate the workspace or install, resolve and possibly start the uninstalled bundle again.
 * <p>
 * Installed and uninstalled bundles are registered and unregistered as workspace bundles respectively.
 * <p>
 * The design supports a concept of a region bounded bundle structure ({@link BundleWorkspaceImpl}) acted on
 * by bundle operations ({@link BundleCommandImpl}), which in turn creates a result (events) to interpret and
 * react upon ({@code BundleEventManager}). This interrelationship is not interpreted as a sequence or a flow,
 * although present, but as a structural coherence.
 * <p>
 */
class BundleEventManager implements FrameworkListener, SynchronousBundleListener, ICommandListener {

	private BundleJobListener jobListener = new BundleJobListener();
	private BundleWorkspaceImpl bundleRegion = BundleWorkspaceImpl.INSTANCE;
	private Command autoBuildCommand;
	BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
	BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;

	/**
	 * Default empty constructor.
	 */
	public BundleEventManager() {
	}

	/**
	 * Registers the framework, bundle and bundle job listeners. Other listeners are registered in the
	 * {@linkplain no.javatime.inplace.InPlace activator}.
	 */
	public void init() {

		bundleCommand.getContext().addFrameworkListener(this);
		bundleCommand.getContext().addBundleListener(this);
		Job.getJobManager().addJobChangeListener(jobListener);
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (null != workbench) {
			ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
			autoBuildCommand = service.getCommand("org.eclipse.ui.project.buildAutomatically");
			if (autoBuildCommand.isDefined()) {
				autoBuildCommand.addCommandListener(this);
			}
		}
	}

	/**
	 * Removes the framework, bundle and bundle job listeners.
	 */
	public void dispose() {

		bundleCommand.getContext().removeFrameworkListener(this);
		bundleCommand.getContext().removeBundleListener(this);
		Job.getJobManager().removeJobChangeListener(jobListener);
		if (autoBuildCommand.isDefined()) {
			autoBuildCommand.removeCommandListener(this);
		}
	}

	/**
	 * Callback for the "Build Automatically" main menu option.
	 * Auto build is set to true when "Build Automatically" is switched on.
	 * <p>
	 * When auto build is switched on the post builder is not invoked,
	 * so an update job is scheduled to update projects being built when
	 * the auto build option is switched on.
	 */
	@Override
	public void commandChanged(CommandEvent commandEvent) {
		InPlace activator =InPlace.getDefault();
		if (null == activator) {
			return;
		}
		IWorkbench workbench = activator.getWorkbench();
		if (!ProjectProperties.isProjectWorkspaceActivated() || 
				(null != workbench && workbench.isClosing())) {
			return;
		}
		Command autoBuildCmd = commandEvent.getCommand();
		if (autoBuildCmd.isDefined() && !ProjectProperties.isAutoBuilding()) {
			if (InPlace.getDefault().getPrefService().isUpdateOnBuild()) {
				BundleManager.getRegion().setAutoBuild(true);
				Collection<IProject> activatedProjects = ProjectProperties.getActivatedProjects();
				Collection<IProject> pendingProjects = bundleTransition.getPendingProjects(
						activatedProjects, Transition.BUILD);
				Collection<IProject> pendingProjectsToUpdate = bundleTransition.getPendingProjects(
						activatedProjects, Transition.UPDATE);
				if (pendingProjectsToUpdate.size() > 0) {
					pendingProjects.addAll(pendingProjectsToUpdate);
				}
				if (pendingProjects.size() > 0) {
					UpdateScheduler.scheduleUpdateJob(pendingProjects, 1000);
				}
			}
		} else {
			BundleManager.getRegion().setAutoBuild(false);			
		}
	}

	/**
	 * Trace events and report on framework errors.
	 */
	@Override
	public void frameworkEvent(FrameworkEvent event) {

		if (Category.getState(Category.bundleEvents)) {
			TraceMessage.getInstance().getString("framework_event", BundleCommandImpl.INSTANCE.getStateName(event),
					event.getBundle().getSymbolicName());
		}
		if ((event.getType() & (FrameworkEvent.ERROR)) != 0) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, event.getBundle().getBundleId(), null,
							event.getThrowable()), StatusManager.LOG);
		}
	}

	/**
	 * Identify and adjust bundle state for external bundle operations, bundle operations spanning multiple
	 * transitions and "on demand" class loading of lazy activated bundles.The current state of a bundle, which
	 * changes after an internal (or external) workspace bundle operation is executed, is adjusted and recorded
	 * along with the bundle in a bundle node. If received bundle events from the framework deviates from the
	 * current recorded state of the bundle the transition (operation) who initiated the events is treated as an
	 * external operation and actions are taken to update the bundle node with the new state caused by the
	 * external transition.
	 * <p>
	 * An exception is when a bundle with lazy activation policy is started due to "on demand" class loading.
	 * This is a transition generated by the framework, and the maintained state of the bundle is adjusted after
	 * the fact from a lazy activation state to an active state.
	 * <p>
	 * It is allowed to execute external commands both in an activated and a deactivated workspace except for
	 * uninstalling workspace bundles or resolving or starting deactivated bundles in an activated workspace.
	 * Uninstalling workspace bundles in an activated workspace from an external source is acted upon and bundle
	 * operations are executed - either automatically or based on user choice - to maintain the workspace
	 * consistent with the definition of an activated workspace. In the second case, resolving or starting a
	 * deactivated bundle from an external source in an activated workspace, is rejected by the resolver hook.
	 */
	@Override
	public void bundleChanged(BundleEvent event) {

		final Bundle bundle = event.getBundle();
		// Only consider workspace bundles
		final IProject project = bundleRegion.getProject(bundle);
		if (null == project) {
			return; // jar bundle
		}

		// Get the recorded internal state of this bundle
		BundleState state = bundleRegion.getActiveState(bundle);
		final int eventType = event.getType();
		final String bundleLocation = bundle.getLocation();
		Transition transition = null;
		try {
			transition = bundleTransition.getTransition(bundleRegion.getProject(bundle));
		} catch (ProjectLocationException e) {
			return; // Avoid spam. Delegate the exception reporting to others
		}

		// If bundle has no recorded state and this is an internal install operation this method is called
		// synchronously by install and the bundle will be registered as an installed workspace bundle.
		if (null == state && Transition.INSTALL == transition) {
			bundleCommand.registerBundleNode(project, bundle, ProjectProperties.isProjectActivated(project));
			// Get the new current state (installed) of the registered bundle
			state = bundleRegion.getActiveState(bundle);
		}
		BundleNode bundleNode = bundleRegion.getBundleNode(bundle);
		// Examine all bundle events sent by the framework and update bundle state when external bundle
		// operations are executed, when lazy activated bundles are loaded on demand by the framework and
		// BundleManager commands (transitions) spans multiple states
		switch (eventType) {
		// @formatter:off 
		/*
		 * Incoming transitions with Installed as the current state: 
		 * Previous state: Uninstalled. Possible transitions: Install 
		 * Previous state: Installed. Possible transitions: Update, Refresh
		 * Comments: 
		 * Updating an installed bundle is relevant when activating a bundle in an activated workspace.
		 * Refresh on an installed bundle is not put to use
		 */
		case BundleEvent.INSTALLED: {
			// Transition: Install. Source: External
			if (null == state || !(state instanceof InstalledState)) {
				// Register the external installed workspace bundle
				bundleNode = bundleCommand.registerBundleNode(project, bundle, ProjectProperties.isProjectActivated(project));
				bundleTransition.setTransition(bundle, Transition.EXTERNAL);
				// External transition message
				if (Category.getState(Category.infoMessages)) {
					final String originName = bundleRegion.getSymbolicKey(event.getOrigin(), null);
					final String symbolicName = bundleRegion.getSymbolicKey(bundle, null);
					final String stateName = bundleCommand.getStateName(event);
					UserMessage.getInstance().getString("external_bundle_operation_origin", symbolicName, stateName,
							originName, bundleLocation);
				}
			}
			break;
		}
		
		/*
		 * Incoming transitions with Installed as the current state: 
		 * Previous state: Resolved. Possible transitions: Refresh, Update, Uninstall 
		 * Comments: 
		 * Refresh unresolves and resolves a bundle. If the bundle is already deactivated it is rejected by the 
		 * resolver hook and not resolved again. 
		 * A bundle to update is unresolved before the installed bundle is updated.
		 */
		case BundleEvent.UNRESOLVED: {
			if ((state instanceof InstalledState)) {
				// The bundle was initially uninstalled from state resolved. (Multiple states:
				// Resolved-Installed-Uninstalled)
				if (Transition.UNINSTALL == transition) {
					state.uninstall(bundleNode);
					// The bundle was initially refreshed from state resolved. (Multiple states:
					// Resolved-Installed-Resolved)
				} else if (Transition.REFRESH == transition) {
					state.refresh(bundleNode);
				}
			} else {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
				externalTransitionMsg(event);
			}
			break;
		}
		/*
		 * Incoming transitions with Installed as the current state: 
		 * Previous state: Installed. Possible transitions: Update 
		 * Comments: 
		 * Update does not alter the state of the installed bundle.
		 */
		case BundleEvent.UPDATED: {
			if (!(state instanceof InstalledState)) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
				externalTransitionMsg(event);
			}
			break;
		}
		/*
		 * Incoming transitions with Uninstalled as the current state: 
		 * Previous state: Installed. Possible transitions: Uninstall
		 */
		case BundleEvent.UNINSTALLED: {
			if (!(state instanceof UninstalledState)) {
				if (null != bundleNode) {
					bundleNode.setCurrentState(BundleStateFactory.INSTANCE.uninstalledState);
				}
				externalTransitionMsg(event);
				// Uninstalling a bundle from an external source is not permitted in an activated workspace
				if (ProjectProperties.isProjectWorkspaceActivated()) {
					externalUninstall(bundle, project);
				} else {
					// Remove the externally uninstalled bundle from the workspace region
					bundleCommand.unregisterBundle(bundle);
				}
			}
			break;
		}

		/*
		 * Enters state <<LAZY>>
		 */
		case BundleEvent.LAZY_ACTIVATION: {
			if (!(state instanceof LazyState)) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.lazyState);
			}
			break;
		}

		/*
		 * Incoming transitions with Starting as the current state: 
		 * Previous state: Resolved. Possible transitions: Start 
		 * Comments: 
		 * Activates a bundle with eager activation policy. Lazy bundles are activated
		 * by the framework due to "on demand" class loading.
		 */
		case BundleEvent.STARTING: {
			if (state instanceof LazyState) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.activeState);
				// Generate a transition on behalf of the framework and if the previous transition was
				// from an external source it follows that this one is external too
				bundleTransition.setTransition(bundle, Transition.LAZY_LOAD);
				if (Category.getState(Category.infoMessages)) {
					UserMessage.getInstance().getString("on_demand_loading_bundle", bundle, bundleCommand.getStateName(event));
				}
			} else if (!(state instanceof ActiveState)) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.activeState);
				externalTransitionMsg(event);
			}
			break;
		}
		/*
		 * Incoming transitions with Active as the current state: 
		 * Previous state: Starting. Possible transitions: Start
		 */
		case BundleEvent.STARTED: {
			if (!(state instanceof ActiveState)) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.activeState);
				externalTransitionMsg(event);
			}
			break;
		}
		/*
		 * Incoming transitions with Stopping as the current state: 
		 * Previous state: Active. Possible transitions: Stop
		 */
		case BundleEvent.STOPPING:
			/*
			 * Incoming transitions with Resolved as the current state: 
			 * Previous state: Stopping. Possible transitions: Stop
			 */
		case BundleEvent.STOPPED: {
			if (transition == Transition.START) {
				// Error starting bundle. The framework is stopping the bundle
			} //else // Denne tuller når vi kjører en extern operasjon
			if (!(state instanceof ResolvedState)) {
				bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);
				externalTransitionMsg(event);
			}
			break;
		}
		/*
		 * Incoming transitions with Resolved as the current state: 
		 * Previous state: Resolved. Possible transitions: Refresh 
		 * Previous state: Installed. Possible transitions: Resolve 
		 * Comments: 
		 * Resolving (and refreshing) bundles with lazy activation policy are moved to state 
		 * Starting by the framework
		 */
		case BundleEvent.RESOLVED: {
			if (state instanceof ResolvedState) {
				if (Transition.RESOLVE == transition) {
					if (ManifestUtil.getlazyActivationPolicy(bundle)) {
						bundleNode.setCurrentState(BundleStateFactory.INSTANCE.lazyState);
					}
				} else if (Transition.REFRESH == transition) {
					if (ManifestUtil.getlazyActivationPolicy(bundle)) {
						bundleNode.setCurrentState(BundleStateFactory.INSTANCE.lazyState);
					}
				}
			} else {
				if (ManifestUtil.getlazyActivationPolicy(bundle)) {
					bundleNode.setCurrentState(BundleStateFactory.INSTANCE.lazyState);
				} else {
					bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);
				}
				externalTransitionMsg(event);
			}
			break;
		}
		default: {
			externalTransitionMsg(event);
		}
		} // switch
		// @formatter:on
		// Event trace
		if (Category.getState(Category.bundleEvents)) {
			try {
				TraceMessage.getInstance().getString("bundle_event", bundle, bundleCommand.getStateName(event),
						bundleCommand.getStateName(bundle), bundleTransition.getTransitionName(project));
			} catch (ProjectLocationException e) {
			}
		}
	}

	/**
	 * Register external transition and output that an external operation has been executed.
	 * 
	 * @param event bundle event after a bundle operation has been executed
	 */
	private void externalTransitionMsg(BundleEvent event) {
		Bundle bundle = event.getBundle();
		final String location = bundle.getLocation();
		BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
		BundleTransitionImpl.INSTANCE.setTransition(bundle, Transition.EXTERNAL);
		if (Category.getState(Category.infoMessages)) {
			final String symbolicName = bundleRegion.getSymbolicKey(bundle, null);
			final String stateName = bundleCommand.getStateName(event);
			UserMessage.getInstance().getString("external_bundle_operation", symbolicName, stateName, location);
		}
	}

	/**
	 * Bundle has been uninstalled from an external source in an activated workspace. Either restore (activate
	 * or install) the bundle or deactivate the workspace depending on default actions or user option/choice.
	 * 
	 * @param project the project to restore or deactivate
	 * @param bundle the bundle to restore or deactivate
	 */
	private void externalUninstall(final Bundle bundle, final IProject project) {

		final String symbolicName = bundle.getSymbolicName();
		final String location = bundle.getLocation();
		// After the fact
		InPlace.getDisplay().asyncExec(new Runnable() {
			public void run() {
				BundleCommandImpl bundleManager = BundleCommandImpl.INSTANCE;
				int autoDependencyAction = 1; // Default auto dependency action
				new OpenProjectHandler().saveModifiedFiles();
				Boolean dependencies = false;
				Collection<IProject> reqProjects = Collections.emptySet();
				if (bundleRegion.isActivated(bundle)) {
					ProjectSorter bs = new ProjectSorter();
					reqProjects = bs.sortRequiringProjects(Collections.singletonList(project), Boolean.TRUE);
					// Remove initial project from result set
					reqProjects.remove(project);
					dependencies = reqProjects.size() > 0;
					if (dependencies) {
						String msg = WarnMessage.getInstance().formatString("has_requiring_bundles",
								bundleRegion.formatBundleList(bundleRegion.getBundles(reqProjects), true),
								bundleRegion.getSymbolicKey(bundle, null));
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
								StatusManager.LOG);
					}
				}
				// User choice to deactivate workspace or restore uninstalled bundle
				if (!InPlace.getDefault().getPrefService().isAutoHandleExternalCommands()) {
					String question = null;
					int index = 0;
					if (dependencies) {
						question = Message.getInstance().formatString("deactivate_question_requirements", symbolicName,
								location, bundleRegion.formatBundleList(bundleRegion.getBundles(reqProjects), true));
						index = 1;
					} else {
						question = Message.getInstance().formatString("deactivate_question", symbolicName, location);
					}
					MessageDialog dialog = new MessageDialog(null, "InPlace Activator", null, question,
							MessageDialog.QUESTION, new String[] { "Yes", "No" }, index);
					autoDependencyAction = dialog.open();
				}
				bundleManager.unregisterBundle(bundle);
				if (autoDependencyAction == 0) {
					if (ProjectProperties.isProjectActivated(project)) {
						// Restore activated bundle and dependent bundles
						ActivateBundleJob activateBundleJob = new ActivateBundleJob(ActivateBundleJob.activateJobName,
								project);
						if (dependencies) {
							// Bring workspace back to a consistent state before restoring
							UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName, reqProjects);
							BundleManager.addBundleJob(uninstallJob, 0);
							activateBundleJob.addPendingProjects(reqProjects);
						}
						BundleManager.addBundleJob(activateBundleJob, 0);
					} else {
						// Workspace is activated but bundle is not. Install the bundle
						InstallJob installJob = new InstallJob(InstallJob.installJobName, project);
						BundleManager.addBundleJob(installJob, 0);
					}
				} else if (autoDependencyAction == 1) {
					// Deactivate workspace to obtain a consistent state between all workspace bundles
					DeactivateJob deactivateJob = new DeactivateJob(DeactivateJob.deactivateJobName);
					deactivateJob.addPendingProjects(ProjectProperties.getActivatedProjects());
					BundleManager.addBundleJob(deactivateJob, 0);
				}
			}
		});
	}
}