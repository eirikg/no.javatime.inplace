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
package no.javatime.inplace.region.state;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleCandidates;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Bundle listener acting on events received from the OSGI framework. Each bundle event consist, as
 * a minimum, of a bundle transition and a bundle state. A bundle command or operation (e.g.
 * refresh) may consist of multiple transitions and thus generate multiple bundle events. Bundle
 * events are either initiated by internal bundle operations in {@link BundleCommandImpl}, from an
 * external source(e.g. the OSGi Bundle Console or a third party bundle) or directly by the
 * framework itself as a consequence of internal or external operations.
 * <p>
 * This class is part of the internal bundle finite state machine implemented in this package and is
 * responsible for generating new transitions based on transitions generated from the framework
 * itself and correct the internal state of the machine based on commands initiated from external
 * sources
 * <p>
 * Events initiated from an external source are marked as external transitions and also flagged as
 * such if the bundle log option is switched on.
 * <p>
 * The framework generates new transitions under the following circumstances:
 * <ol>
 * <li>When a bundle is started due to "on demand class loading" for lazy activated bundles. See
 * comments on the lazy activate and staring events for details.
 * <li>A stop transition is generated when an exception occurs during a start operation. See
 * comments on the stopping and stop events for details.
 * <li>An unresolve transition is generated when an incomplete requiring closure is handed to the
 * resolver by uninstall or refresh. It is by design to not always provide a complete requiring
 * closure to the resolver. See the comments in the unresolve event and unresolve transition for
 * details.
 * <li>A resolve transition is generated when an incomplete providing closure is handed to the
 * resolver by resolve or refresh. This option is not in use. Complete closures are always handed
 * over to the resolver when bundles are resolved.
 * </ol>
 * <p>
 * An OSGi command is comprised of two or more transitions under the following circumstances:
 * <ol>
 * <li>Resolving a lazy activated bundle. The bundle is first resolved and than lazy activated with
 * terminal state starting
 * <li>Starting a bundle. The first transition is starting with terminal state starting and second
 * started with terminal state active
 * <li>Stopping a bundle. The first transition is stopping with terminal state stopping and second
 * stop with terminal state stopped
 * <li>Uninstalling a bundle in state resolved. Both transitions are uninstall, where the first
 * moves the bundle to state installed and the second to state uninstalled
 * <li>Refreshing a bundle from state resolved. The bundle is first unresolved and than resolved.
 * </ol>
 * <p>
 * The design supports a concept of a region bounded bundle structure (
 * {@link BundleWorkspaceRegionImpl}) acted on by bundle operations ({@link BundleCommandImpl}),
 * which in turn creates a result (events) to interpret and react upon ({@code BundleStateEvents}).
 * This interrelationship is not interpreted as a sequence or a flow, although present, but as a
 * structural coherence.
 * <p>
 */
public class BundleStateEvents implements SynchronousBundleListener {

	private final BundleWorkspaceRegionImpl bundleRegion = BundleWorkspaceRegionImpl.INSTANCE;
	private final BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
	private final BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;

	/**
	 * Default empty constructor.
	 */
	public BundleStateEvents() {
	}

	/**
	 * Identify and adjust bundle state for external bundle commands, bundle commands spanning
	 * multiple transitions and bundle transitions generated by the framework (e.g. lazy bundle
	 * activation).
	 * <p>
	 * The current state of a bundle, which changes after an internal (or external) workspace bundle
	 * operation is executed, is adjusted and recorded along with the bundle in a bundle node. If
	 * received bundle events from the framework deviates from the current recorded state of the
	 * bundle, the transition (operation) who initiated the events is treated as an external operation
	 * and actions are taken to update the bundle node with the new state caused by the external
	 * transition.
	 * <p>
	 * Known events generated by the framework (see class comments) are adjusted after the fact to
	 * maintain a consistent machine state. E.g. when a bundle with lazy activation policy is started
	 * due to "on demand" class loading a new transition is generated moving the bundle from a lazy
	 * activation state to an active state.
	 * <p>
	 * It is allowed to execute external commands both in an activated and a deactivated workspace
	 * except for uninstalling workspace bundles or resolving or starting deactivated bundles in an
	 * activated workspace. Uninstalling workspace bundles in an activated workspace from an external
	 * source is acted upon and bundle operations are executed - either automatically or based on user
	 * choice - to maintain the workspace consistent with the definition of an activated workspace. In
	 * the second case, resolving or starting a deactivated bundle from an external source in an
	 * activated workspace, is rejected by the resolver hook.
	 */
	@Override
	public void bundleChanged(BundleEvent event) {

		final Bundle bundle = event.getBundle();
		// Consider all workspace bundle projects
		final IProject project = bundleRegion.getBundleProject(bundle);
		if (null == project) {
			return; // jar bundle or java project
		}
		BundleNode node = bundleRegion.getBundleNode(project);
		// Project is not registered yet or it is registered but not its bundle
		if (null == node || null == node.getBundleId()) {
			// Bundle node will be registered as long as the project exists
			node = bundleCommand.registerBundleNode(project, bundle,
					BundleCandidates.isNatureEnabled(project));
		}
		/*
		 * Examine all bundle events and update state by executing intermediate transitions, identify
		 * and recover from transition errors and sync with external bundle commands
		 */
		switch (event.getType()) {
		// @formatter:off
		/*
		 * Install a bundle with initial state uninstalled and terminal state installed. For external
		 * commands force the bundle to state installed.
		 */
		case BundleEvent.INSTALLED: {
			if (!node.isStateChanging()) {
				node.commit(Transition.EXTERNAL, StateFactory.INSTANCE.installedState);
				final String originName = bundleRegion.getSymbolicKey(event.getOrigin(), null);
				final String symbolicName = bundleRegion.getSymbolicKey(bundle, null);
				final String stateName = bundleCommand.getStateName(event);
				final String bundleLocation = bundle.getLocation();
				String msg = NLS.bind(Msg.EXT_BUNDLE_OP_ORIGIN_INFO, new Object[] { symbolicName,
						stateName, originName, bundleLocation });
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null),
						StatusManager.LOG);
			}
			break;
		}
		/*
		 * Refresh, update and uninstall from state resolved triggers an unresolved event.
		 * 
		 * Refresh is comprised of two transitions; - unresolve; and - resolve. This runs the resolve
		 * transition from state installed. The unresolve transition part has been invoked by the
		 * refresh command executing the refresh transition with resolved as the initial state.
		 * 
		 * Update is comprised of of two transitions; - unresolve; and - update. The update command
		 * set the transition to update and the state to installed. 
		 * 
		 * When the uninstall command invokes the uninstall transition from state resolved the bundle is
		 * moved to state installed. This unresolved event triggers the uninstall transition from state
		 * installed and moves the bundle to the terminal state uninstalled. <p> In addition an
		 * unresolved event can be triggered by the framework (see comment below).
		 */
		case BundleEvent.UNRESOLVED: {
			if (node.isTransition(Transition.UPDATE)) {
				// Do nothing the bundle is in state installed
			} else if (node.isTransition(Transition.REFRESH)) {
				node.getState().resolve(node);
			} else if (node.isTransition(Transition.UNINSTALL)) {
				node.getState().uninstall(node);
			} else if (node.containsPendingCommand(Transition.UNRESOLVE, true)) {
				// The resolver initiated an unresolve event because this bundle was explicit
				// excluded from the requiring closure when uninstall or refresh was executed
				// May be in state installed or resolved. Always use the resolved state
				node.setCurrentState(StateFactory.INSTANCE.resolvedState);
				node.getState().unresolve(node);
				node.commit();
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition()));
			} else {
				node.getState().external(node, event, StateFactory.INSTANCE.installedState,
						Transition.EXTERNAL);
			}

			break;
		}
		/*
		 * A bundle is resolved with installed as the initial state. Possible transitions are resolve
		 * and the resolve part of the refresh transition.
		 * 
		 * Refresh is comprised of two transitions; - unresolve; and - resolve. The bundle was first
		 * unresolved with installed as the initial state and installed as the terminal state by the
		 * refresh command. This resolve event executes the resolve part moving the bundle to state
		 * resolved. See the unresolved event for refreshing bundles with resolved as the initial state
		 */
		case BundleEvent.RESOLVED: {
			if (!node.isStateChanging()) {
				node.getState().external(node, event, StateFactory.INSTANCE.resolvedState,
						Transition.EXTERNAL);
			} else {
				if (node.isTransition(Transition.REFRESH)) {
					node.getState().resolve(node);
				}
			}
			break;
		}
		/*
		 * The update command execute the update transition where the initial state is either resolved
		 * or installed.
		 */
		case BundleEvent.UPDATED: {
			if (!node.isStateChanging()) {
				node.getState().external(node, event, StateFactory.INSTANCE.installedState,
						Transition.EXTERNAL);
			}
			break;
		}
		/*
		 * A bundle can be uninstalled from an initial state of resolved or installed. Uninstalling a
		 * bundle with resolved as the initial state first moves the bundle to state install by the
		 * uninstall command, second an unresolved event is received running an uninstall transition on
		 * the bundle from state installed. This moves the bundle to state uninstalled.
		 */
		case BundleEvent.UNINSTALLED: {
			if (!node.isStateChanging()) {
				node.getState().external(node, event, StateFactory.INSTANCE.uninstalledState,
						Transition.EXTERNAL);
				if (BundleCandidates.isWorkspaceNatureEnabled()) {
					bundleTransition.setTransitionError(bundle, TransitionError.UNINSTALL);
					BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition()));
				} else {
					// Remove the externally uninstalled bundle from the workspace region
					bundleCommand.unregisterBundle(bundle);
				}
			}
			break;
		}

		/*
		 * When a lazy bundle is resolved it is moved to the starting state. This transition - moving a
		 * bundle from resolved to starting - is generated by the framework.
		 * 
		 * The bundle is first resolved from installed as the initial state and than moved to state
		 * resolved. This event runs the start transition for a resolved bundle with lazy activation
		 * policy moving the bundle to the starting state.
		 */
		case BundleEvent.LAZY_ACTIVATION: {

			if (node.getTransition() == Transition.EXTERNAL) {
				node.getState().external(node, event, StateFactory.INSTANCE.startingState,
						Transition.EXTERNAL);
			} else if (node.isState(Transition.RESOLVE, ResolvedState.class)) {
				node.getState().start(node);
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition()));
			}
			break;
		}

		/*
		 * Framework publish this event before calling BundleActivator.start. Activates a bundle with
		 * eager activation policy. Lazy bundles are activated implicit by the framework due to
		 * "on demand" class loading. To get a consistent state machine a new transition and state is
		 * generated for lazy loaded bundles.
		 */
		case BundleEvent.STARTING: {
			if (node.getTransition() == Transition.EXTERNAL) {
				if (ManifestOptions.getActivationPolicy(node.getBundle())) {
					node.getState().external(node, event, StateFactory.INSTANCE.startingState,
							Transition.EXTERNAL);
				} else {
					node.getState().external(node, event, StateFactory.INSTANCE.activeState,
							Transition.EXTERNAL);
				}
			}
			break;
		}
		/*
		 * Framework publish this event after calling BundleActivator.start. Bundles with lazy
		 * activation policy is moved to state active by running the start transition in state starting
		 */
		case BundleEvent.STARTED: {
			if (node.getTransition() == Transition.EXTERNAL) {
				node.getState().external(node, event, StateFactory.INSTANCE.activeState,
						Transition.EXTERNAL);
			}
			// This always comes from the framework. Commit the command on behalf of the framework.
			if (node.isState(StartingState.class)) {
				node.getState().start(node);
				node.getState().commit(node);
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition()));
			}
			break;
		}
		/*
		 * Framework publish this event before calling BundleActivator.stop
		 * 
		 * Executing stop on a lazy activated (state starting) bundle or an active bundle involves two
		 * transitions. The first transition is from state active or state starting and is executed by
		 * the stop command with stopping as the terminal state. This stopping event triggers, in the
		 * second step, a stop transition with resolved as the terminal state.
		 */
		case BundleEvent.STOPPING: {
			if (!node.isStateChanging()) {
				node.getState().external(node, event, StateFactory.INSTANCE.stoppingState,
						Transition.EXTERNAL);
			} else {
				if (node.isState(Transition.STOP, StoppingState.class)) {
					node.getState().stop(node);
				}
			}
		}
			break;

		/*
		 * The Framework publish this event after calling BundleActivator.stop or when a runtime error
		 * occurs during start (incomplete transition). If the BundleActivator.start method throws an
		 * exception the framework publish a STOPPED event but the internal transition was START (not
		 * STOP). This could also be an external command stopping the bundle or starting the bundle
		 * throwing an exception. For eager activated bundles when a stopped event is published and the
		 * initial start command was invoked internally just leave it. A roll back is issued by the
		 * start routine which invoked the start command. For lazy loaded bundles force a roll back (by
		 * using an explicit commit).
		 */
		case BundleEvent.STOPPED: {
			// An external command or an exception thrown in the start method
			if (!node.isStateChanging()) {
				node.getState().external(node, event, StateFactory.INSTANCE.resolvedState,
						Transition.EXTERNAL);
			} else {
				if (node.isTransition(Transition.LAZY_ACTIVATE)) {
					node.commit(Transition.RESOLVE, StateFactory.INSTANCE.resolvedState);
				}
			}
			break;
		}
		default: {
			node.getState().external(node, event, StateFactory.INSTANCE.stateLess, Transition.EXTERNAL);
		}
		} // switch
		// @formatter:on
		// Event trace
		if (Category.getState(Category.bundleEvents)) {
			try {
				TraceMessage.getInstance().getString("bundle_event", bundle,
						bundleCommand.getStateName(event), bundleCommand.getStateName(bundle),
						bundleTransition.getTransitionName(project));
			} catch (ProjectLocationException e) {
				String msg = ExceptionMessage.getInstance().formatString("project_location_error",
						project.getName());
				StatusManager.getManager().handle(new Status(Status.ERROR, Activator.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		}
	}
}