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
import java.util.LinkedHashSet;

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Maintain a persistent workspace region state (activation level (bundle state), bundle transition
 * state and pending transitions). The workspace region state is persisted when ending a session and
 * restored when starting an new session. Examples of sessions are startup/shutdown,
 * deactivate/activate workspace and when the Reset (uninstall/activate) bundle command is executed.
 * <p>
 * For each bundle project the bundle state, transition state and pending transitions are persisted.
 * With this information at hand it is possible to restore the active state from the persisted
 * state.
 * <p>
 * <li>Activation levels (Bundle states)</li>
 * <p>
 * In a deactivated workspace all bundles are in state {@code Bundle.UNINSTALLED} implying that no
 * state is persisted. In an activated workspace default is to start activated bundle projects and
 * install deactivated bundle projects. Given these two rules it is only necessary to persist
 * bundles in state {@code Bundle.RESOLVED}.
 * <li>Bundle transition states</li>
 * <p>
 * In a deactivated workspace the last transition executed on a bundle is stored. In an activated
 * workspace transition states are calculated - not persisted - based on the activation mode
 * (activated/deactivated) and the persisted activation level.
 * <li>Pending transitions</li>
 * <p>
 * If bundle projects have any pending transitions they are saved at the end of a session and added
 * as pending transitions to bundle projects when a session starts.
 * <li>Recovery mode.</li>
 * <p>
 * A persisted property indicating that the workspace is running is enabled at start up and disabled
 * at shut down. See{@link #setWorkspaceSession(boolean)} and {@link #isWorkspaceSession()} for
 * details. If the property is enabled when accessed from the preference store at start up this
 * indicates an abnormal termination and the workspace region state is recovered when preparing a
 * new session.
 * 
 * The save participant mechanism is only used to request for workspace delta information to be
 * handled by the post build listener during start up.
 */
public class StatePersistParticipant implements ISaveParticipant {

	/** Preference node for persisted bundle states */
	private static String bundleStateNode = "bundle.state";
	/** Preference node for persisted transition states */
	private static String bundleTransitionNode = "bundle.transition.state";
	/** Bundle Project state when deactivated */
	private static String deactivateState = "deactivate";
	/** Preference node for persisted pending transitions */
	private static String bundlePendingTransitionNode = "bundle.pending.transition";
	/** Preference node for persisted state of the workspace region */
	private static String workspaceRegionNode = "workspace.region.state";
	/** Indicates if the the workspace region is active or not */
	private static String workspaceSession = "active.session";
	/** Calculate time used by a full workspace save */
	private long startTime;

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {

		startTime = System.currentTimeMillis();
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		// Needed by the post build listener at startup
		context.needDelta();
	}

	@Override
	public void rollback(ISaveContext context) {
	}

	@Override
	public void doneSaving(ISaveContext context) {

		// Inform about full saves during a session
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (null != workbench && !workbench.isClosing()
					&& context.getKind() == ISaveContext.FULL_SAVE) {
				MessageOptions messageOptions = Activator.getMessageOptionsService();
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.SAVE_WORKSPACE_INFO, (System.currentTimeMillis() - startTime));
					IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg);
					Activator.log(status);
				}
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Restore bundle activation level (bundle state), transition state and any pending transitions
	 * from the previous session.
	 * <p>
	 * See {@link #restoreActivationLevel(IEclipsePreferences, Bundle)} for a specification on how
	 * activation levels are restored. An exception to these rules, is to start bundles with
	 * {@code Bundle.RESOLVED} as the activation level when activated bundles to start have
	 * requirements on bundles with {@code Bundle.RESOLVED} as the activation level.
	 * <p>
	 * See {@link #restoreTransitionState(IEclipsePreferences)} for a specification on how transitions
	 * states are restored
	 * <p>
	 * See {@link #restorePendingTransitions(IEclipsePreferences)} for a specification on how pending
	 * transitions are restored
	 * <p>
	 * See class description for a specification on how states are recovered if a session terminates
	 * abnormally.
	 * 
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws InPlaceException Failing to access an open project
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 */
	public static void restoreSessionState() throws CircularReferenceException,
			BackingStoreException, ExtenderException, InPlaceException, IllegalStateException {

		IEclipsePreferences prefs = getSessionPreferences();
		BundleRegion bundleRegion = Activator.getBundleRegionService();
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		if (activatedBundles.size() > 0) {
			BundleTransition bundleTransition = Activator.getBundleTransitionService();
			if (isWorkspaceSession()) {
				// Normal mode
				Collection<Bundle> resolveStateBundles = restoreActivationLevel(prefs, activatedBundles,
						Bundle.RESOLVED);
				if (resolveStateBundles.size() > 0) {
					activatedBundles.removeAll(resolveStateBundles);
					// This is an additional verification check in case stored states or bundle activation
					// mode has been changes since last session
					BundleClosures bc = new BundleClosures();
					Collection<Bundle> providingBundles = bc.bundleActivation(Closure.PROVIDING,
							activatedBundles, resolveStateBundles);
					// Does any bundles to only resolve provide capabilities to any of the bundles to start
					providingBundles.retainAll(resolveStateBundles);
					if (providingBundles.size() > 0) {
						resolveStateBundles.removeAll(providingBundles);
						// Start these, due to requirements from bundles to start
						// activatedBundles.addAll(providingBundles);
						MessageOptions messageOptions = Activator.getMessageOptionsService();
						if (messageOptions.isBundleOperations()) {
							String msg = NLS.bind(Msg.CONDITIONAL_START_BUNDLE_INFO,
									new Object[] { bundleRegion.formatBundleList(providingBundles, true) });
							BundleLog logger = Activator.getBundleLogService();
							logger.log(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
						}
					}
					for (Bundle bundle : resolveStateBundles) {
						// Do not start bundles that were in state resolved in last session
						bundleTransition.addPending(bundle, Transition.RESOLVE);

					}
				}
			} else {
				// Recovery mode
				for (Bundle bundle : activatedBundles) {
					bundleTransition.addPending(bundle, Transition.RESOLVE);
				}
			}
			restorePendingTransitions(prefs);
			// Set to recovery mode in case of abnormal termination of a session
			prefs.node(bundleStateNode).clear();
			prefs.node(bundleTransitionNode).clear();
			prefs.node(bundlePendingTransitionNode).clear();
		} else {
			restoreTransitionState(prefs);
		}
		prefs.flush();
	}

	/**
	 * Find all bundles among the specified bundles which should be activated to the specified
	 * activation level.
	 * <p>
	 * See {@link #restoreActivationLevel(IEclipsePreferences, Bundle) for a specification of
	 * activation levels. An exception to these rules, is to start bundles with
	 * 
	 * {@code Bundle.RESOLVED} as the activation level when activated bundles to start have
	 * requirements on bundles with {@code Bundle.RESOLVED} as the activation level.
	 * 
	 * @param prefs Root of the preference store
	 * @param bundles Set of bundles to match the specified activation level against
	 * @param activationLevel The state specifying which activation level to match the specified
	 * bundles to
	 * @return Set of bundles to activate to the specified activation level
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #saveActivationLevel(IEclipsePreferences, boolean)
	 */
	private static Collection<Bundle> restoreActivationLevel(IEclipsePreferences prefs,
			Collection<Bundle> bundles, int activationLevel) throws BackingStoreException,
			ExtenderException, IllegalStateException {

		Collection<Bundle> activationLevelBundles = new LinkedHashSet<>();
		if (prefs.nodeExists(bundleStateNode)) {
			Preferences stateNode = prefs.node(bundleStateNode);
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			BundleProjectMeta bundleProjectMeta = Activator.getbundlePrrojectMetaService();
			BundleCommand bundleCommand = Activator.getBundleCommandService();
			if (bundleRegion.isRegionActivated()) {
				String activeStateName = bundleCommand.getStateName(Bundle.ACTIVE);
				String installedStateName = bundleCommand.getStateName(Bundle.INSTALLED);
				String stateName = bundleCommand.getStateName(activationLevel);
				for (Bundle bundle : bundles) {
					if (!bundleRegion.isBundleActivated(bundle)) {
						if (stateName.equals(installedStateName)) {
							activationLevelBundles.add(bundle);
						}
					} else {
						String symbolicKey = bundleRegion.getSymbolicKey(bundle, null);
						if (!symbolicKey.isEmpty()) {
							String prefsStateName = stateNode.get(symbolicKey, activeStateName);
							if (prefsStateName.equals(stateName)) {
								if ((activationLevel & (Bundle.RESOLVED)) != 0
										&& bundleProjectMeta.isFragment(bundle)) {
									activationLevelBundles.add(bundle);
								} else {
									activationLevelBundles.add(bundle);
								}
							}
						}
					}
				}
			} else {
				if ((activationLevel & (Bundle.UNINSTALLED)) != 0) {
					activationLevelBundles.addAll(bundles);
				}
			}
		}
		return activationLevelBundles;
	}

	/**
	 * Get the activation level of the specified bundle from the preference store.
	 * <p>
	 * The following rules are used to determine the activation level:
	 * <li>If the workspace is deactivated the activation level is {@code Bundle.UNINSTALLED} for all
	 * bundle project
	 * <li>If the workspace is activated and the bundle project is deactivated the activation level is
	 * {@code Bundle.INSTALLED}
	 * <li>If the bundle project is activated and has {@code Bundle.RESOLVED} as the persisted value
	 * the activation level is {@code Bundle.RESOLVED}
	 * <li>If the bundle project is activated and has no persisted value the activation level is
	 * {@code Bundle.ACTIVE}
	 * <li>Fragment bundles always have {@code Bundle.RESOLVED} as their activation level in an
	 * activated workspace
	 * 
	 * @param prefs Root of the preference store
	 * @param bundle The bundle to determine the activation level for
	 * @return The activation level for the specified bundle
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 */
	@SuppressWarnings("unused")
	private static int restoreActivationLevel(IEclipsePreferences prefs, Bundle bundle)
			throws BackingStoreException, ExtenderException, IllegalStateException {

		int state = Bundle.UNINSTALLED;
		if (prefs.nodeExists(bundleStateNode)) {
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			BundleProjectMeta bundleProjectMeta = Activator.getbundlePrrojectMetaService();
			BundleCommand bundleCommand = Activator.getBundleCommandService();
			Preferences stateNode = prefs.node(bundleStateNode);
			if (bundleRegion.isRegionActivated()) {
				if (null != bundle) {
					String symbolicKey = bundleRegion.getSymbolicKey(bundle, null);
					if (!symbolicKey.isEmpty()) {
						if (!bundleRegion.isBundleActivated(bundle)) {
							state = Bundle.INSTALLED;
						} else {
							// Only the resolved activation level is stored
							String prefsStateName = stateNode.get(symbolicKey,
									bundleCommand.getStateName(Bundle.ACTIVE));
							if (prefsStateName.equals(bundleCommand.getStateName(Bundle.RESOLVED))
									|| bundleProjectMeta.isFragment(bundle)) {
								state = Bundle.RESOLVED;
							} else if (prefsStateName.equals(bundleCommand.getStateName(Bundle.ACTIVE))) {
								state = Bundle.ACTIVE;
							}
						}
					}
				}
			}
		}
		return state;
	}

	/**
	 * Restore all bundle projects and set the transition for all deactivated projects to the
	 * transition saved by the previous session.
	 * <p>
	 * If a project has never been activated the default state for the transition will be
	 * {@code Transition.NO_TRANSITION}
	 * <p>
	 * Transition state is only restored if the workspace is deactivated. In an activated workspace
	 * transition state are calculated based on the activation mode (activated/deactivated) and the
	 * activation level of bundle projects.
	 * 
	 * @param prefs The preference store
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws InPlaceException Failing to access an open project
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #saveTransitionState(IEclipsePreferences, boolean)
	 */
	private static void restoreTransitionState(IEclipsePreferences prefs)
			throws BackingStoreException, ExtenderException, InPlaceException, IllegalStateException {

		if (prefs.nodeExists(bundleTransitionNode)) {
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			BundleTransition bundleTransition = Activator.getBundleTransitionService();
			BundleProjectCandidates bundleProjectCandidates = Activator
					.getBundleProjectCandidatesService();
			Preferences transitionNode = prefs.node(bundleTransitionNode);
			String noTransitionName = bundleTransition.getTransitionName(Transition.NO_TRANSITION, false,
					false);

			String prefsTransitionName = transitionNode.get(deactivateState, noTransitionName);

			Collection<IProject> bundleProjects = bundleProjectCandidates.getBundleProjects();
			for (IProject project : bundleProjects) {
				try {
					String symbolicKey = bundleRegion.getSymbolicKey(null, project);
					if (symbolicKey.isEmpty()) {
						continue;
					}
					// String prefsTransitionName = transitionNode.get(symbolicKey, noTransitionName);
					bundleTransition.setTransition(project,
							bundleTransition.getTransition(prefsTransitionName));
				} catch (IllegalStateException e) {
					String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
				}
			}
		}
	}

	/**
	 * Restore pending transitions and add them as pending transitions to bundle projects
	 * 
	 * @param prefs The preference store
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws InPlaceException Failing to access an open project
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see {@link #savePendingBuildTransitions(IEclipsePreferences)}
	 */
	private static void restorePendingTransitions(IEclipsePreferences prefs)
			throws ExtenderException, BackingStoreException, InPlaceException, IllegalStateException {

		if (prefs.nodeExists(bundlePendingTransitionNode)) {
			Preferences pendingPrefs = prefs.node(bundlePendingTransitionNode);
			BundleProjectCandidates bundleProjects = Activator.getBundleProjectCandidatesService();
			BundleTransition bundleTransition = Activator.getBundleTransitionService();
			Collection<IProject> projects = bundleProjects.getBundleProjects();
			String buildTransitionName = bundleTransition.getTransitionName(Transition.BUILD, false,
					false);
			String noName = bundleTransition.getTransitionName(Transition.NO_TRANSITION, false, false);
			for (IProject project : projects) {
				String symbolicKey = Activator.getBundleRegionService().getSymbolicKey(null, project);
				if (symbolicKey.isEmpty()) {
					continue;
				}
				String name = pendingPrefs.get(symbolicKey, noName);
				if (name.equals(buildTransitionName)) {
					bundleTransition.addPending(project, Transition.BUILD);
				}
			}
		}
	}

	/**
	 * Save workspace region state (activation level (bundle state), transition state and pending
	 * transitions)information for all bundle projects.
	 * <p>
	 * For each bundle project persist bundle state, transition state and pending transitions. The
	 * saved information should be sufficient to restore the state of the workspace region.
	 * <p>
	 * See {@link #saveActivationLevel(IEclipsePreferences, boolean)} for a specification on how
	 * activation levels are saved.
	 * <p>
	 * See {@link #saveTransitionState(IEclipsePreferences)} for a specification on how transitions
	 * states are saved
	 * <p>
	 * See {@link #savePendingTransitions(IEclipsePreferences)} for a specification on how pending
	 * transitions are saved
	 * <p>
	 * See class description for a specification on how states are recovered if a session terminates
	 * abnormally.
	 * 
	 * @param isDeactivate Different rules apply to which states are stored when a workspace is or is
	 * being deactivated than in a workspace that is or is being activated. See method comments.
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #restoreSessionState()
	 */
	public static void saveSessionState(boolean isDeactivate) throws ExtenderException,
			BackingStoreException, IllegalStateException {

		IEclipsePreferences prefs = getSessionPreferences();
		prefs.clear();
		saveActivationLevel(prefs, isDeactivate);
		saveTransitionState(prefs, isDeactivate);
		savePendingBuildTransitions(prefs);
		prefs.flush();
	}

	/**
	 * Save the activation level of bundle projects.
	 * <p>
	 * See {@link #restoreActivationLevel(IEclipsePreferences, Bundle)} for a specification of
	 * activation levels.
	 * <p>
	 * If the workspace is being deactivated no activation levels are saved. Bundle projects in state
	 * {@code Bundle.RESOLVED} are saved with {@code Bundle.RESOLVED} as their activation level. This
	 * implies that all other activated bundle projects are ins state {@code Bundle.ACTIVE} and should
	 * be started when the workspace region state is restores.
	 * <p>
	 * 
	 * @param prefs Root of the preference store
	 * @param isDeactivate If {@code true} no state is saved. Otherwise bundle projects in state
	 * {@code Bundle.RESOLVED} are saved with {@code Bundle.RESOLVED} as their activation level
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #restoreActivationLevel(IEclipsePreferences, Bundle)
	 */
	private static void saveActivationLevel(IEclipsePreferences prefs, boolean isDeactivate)
			throws ExtenderException, BackingStoreException, IllegalStateException {

		Preferences stateNode = prefs.node(bundleStateNode);
		stateNode.clear();
		if (isDeactivate) {
			return;
		}
		ActivateProject activateProjects = new ActivateProjectJob();
		if (activateProjects.isProjectWorkspaceActivated()) {
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			BundleCommand bundleCommand = Activator.getBundleCommandService();
			BundleRegion region = Activator.getBundleRegionService();
			Collection<IProject> natureEnabled = activateProjects.getActivatedProjects();
			for (IProject project : natureEnabled) {
				String symbolicKey = bundleRegion.getSymbolicKey(null, project);
				if (symbolicKey.isEmpty()) {
					continue;
				}
				Bundle bundle = region.getBundle(project);
				if (null != bundle && (bundle.getState() & (Bundle.RESOLVED)) != 0) {
					stateNode.put(symbolicKey, bundleCommand.getStateName(Bundle.RESOLVED));
				}
			}
		}
		stateNode.flush();
	}

	/**
	 * Save state of bundle projects in a deactivated workspace to preference store based on their
	 * last transition.
	 * <p>
	 * Transition state is only saved if the workspace is deactivated. In an activated workspace
	 * transition state are calculated based on the activation mode (activated/deactivated) and the
	 * activation level of bundle projects.
	 * <p>
	 * Use to set the initial transition of bundles at start of a session. Should be called after
	 * bundle are uninstalled and optionally refreshed to get the state of bundles in a deactivated
	 * state.
	 * 
	 * @param prefs The preference store
	 * @param isDeactivate If false no state information is stored. Otherwise the current transition
	 * state for each bundle project is stored.
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #restoreTransitionState(IEclipsePreferences)
	 */
	public static void saveTransitionState(IEclipsePreferences prefs, boolean isDeactivate)
			throws ExtenderException, BackingStoreException, IllegalStateException {

		Preferences transitionNode = prefs.node(bundleTransitionNode);
		transitionNode.clear();
		BundleRegion bundleRegion = Activator.getBundleRegionService();
		if (!bundleRegion.isRegionActivated() || isDeactivate) {
			BundleTransition bundleTransition = Activator.getBundleTransitionService();
			Transition transition = Transition.NO_TRANSITION;
			// All bundle projects have the same transition state after deactivation
			for (IProject project : bundleRegion.getProjects(false)) {
				try {
					transition = bundleTransition.getTransition(project);
				} catch (ProjectLocationException e) {
					// No transition
				}
				break;
			}
			transitionNode.put(deactivateState,
					bundleTransition.getTransitionName(transition, false, false));
			transitionNode.flush();
		}
	}

	/**
	 * Save pending transitions found in bundle projects independent of activation mode
	 * 
	 * @param prefs The preference store
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws ExtenderException General failure obtaining extender service(s)
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #restorePendingTransitions(IEclipsePreferences)
	 */
	public static void savePendingBuildTransitions(IEclipsePreferences prefs)
			throws ExtenderException, BackingStoreException, IllegalStateException {

		Preferences pendingPrefs = prefs.node(bundlePendingTransitionNode);
		pendingPrefs.clear();
		BundleProjectCandidates bundleProject = Activator.getBundleProjectCandidatesService();
		BundleTransition bundleTransition = Activator.getBundleTransitionService();
		Collection<IProject> projects = bundleTransition.getPendingProjects(
				bundleProject.getBundleProjects(), Transition.BUILD);
		String buildTransitionName = bundleTransition.getTransitionName(Transition.BUILD, false, false);
		for (IProject project : projects) {
			String symbolicKey = Activator.getBundleRegionService().getSymbolicKey(null, project);
			if (symbolicKey.isEmpty()) {
				continue;
			}
			pendingPrefs.put(symbolicKey, buildTransitionName);
		}
		pendingPrefs.flush();
	}

	/**
	 * Preference store used to maintain persistent workspace region state
	 * 
	 * @return The preference store for bundle states, transition states and pending transitions
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 */
	public static IEclipsePreferences getSessionPreferences() throws BackingStoreException {

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		if (null == prefs) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			throw new BackingStoreException(msg);
		}
		return prefs;
	}

	/**
	 * Always {@code true} when the workspace is running. If the the workspace terminates abnormally
	 * the session state will be {@code true} and {@code false} when terminated normally. An abnormal
	 * termination indicates that the workspace region state should be recovered. For a normal
	 * termination persisted states and pending transitions are initialized from the preference store.
	 * <p>
	 * If the workspace terminates unexpectedly, appropriate actions is taken to recover bundle
	 * states, transition states and pending transition (workspace region state) in the
	 * {@link StartUpJob}
	 * 
	 * @param isWorkspaceSession Set to {@code true} when the workspace starts normally and after
	 * recovery and {@code false} when terminating.
	 * 
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @see #isWorkspaceSession()
	 */
	public static void setWorkspaceSession(boolean isWorkspaceSession) throws IllegalStateException,
			BackingStoreException {

		IEclipsePreferences prefs = getSessionPreferences();
		Preferences workspacePrefs = prefs.node(workspaceRegionNode);
		workspacePrefs.putBoolean(workspaceSession, isWorkspaceSession);
		workspacePrefs.flush();
	}

	/**
	 * Check the state of the current workspace session
	 * 
	 * @return True if the workspace is running or terminated abnormally. Otherwise false
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @see #setWorkspaceSession(boolean)
	 */
	public static boolean isWorkspaceSession() throws IllegalStateException, BackingStoreException {

		IEclipsePreferences prefs = getSessionPreferences();
		Preferences workspacePrefs = prefs.node(workspaceRegionNode);
		return workspacePrefs.getBoolean(workspaceSession, false);
	}
}
