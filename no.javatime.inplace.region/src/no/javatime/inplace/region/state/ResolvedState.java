package no.javatime.inplace.region.state;

import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;

import org.osgi.framework.BundleEvent;

/**
 * Begins a set of outgoing transitions specified by the standard OSGi state machine with
 * {@link org.osgi.framework.Bundle#RESOLVED RESOLVED} as the current state. Each method in this
 * class represents a valid transition for a bundle in state resolved
 */
public class ResolvedState extends BundleState {

	public ResolvedState() {
		super();
	}

	/**
	 * Begins an uninstall transition with installed as the terminal state
	 * <p>
	 * Uninstalling a bundle from state resolved is a two step process. First this transition
	 * moves the bundle to state install. The uninstall command generates an unresolved event in
	 * {@link BundleStateEvents#bundleChanged(org.osgi.framework.BundleEvent) BundleStateEvents}.
	 * This event triggers an uninstall transition with state installed as the initial state and
	 * state uninstalled as the terminal state.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void uninstall(BundleNode bundleNode) {
		bundleNode.begin(Transition.UNINSTALL, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins an unresolve transition with installed as the terminal state
	 * <p>
	 * Unresolve is usually generated from the framework as an unresolved event in
	 * {@link BundleStateEvents#bundleChanged(org.osgi.framework.BundleEvent) BundleStateEvents}.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void unresolve(BundleNode bundleNode) {
		bundleNode.begin(Transition.UNRESOLVE, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a refresh transition with installed as the terminal state
	 * <p>
	 * Refresh is comprised of two transitions. The first transition is unresolve and performed by this
	 * refresh transition and then resolved again during refresh reentering state resolved. The
	 * resolve transition is triggered by the {@link BundleEvent#UNRESOLVED} event.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void refresh(BundleNode bundleNode) {
		bundleNode.begin(Transition.REFRESH, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins an update transition with installed as the terminal state
	 * <p>
	 * The bundle is unresolved before update
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void update(BundleNode bundleNode) {
		bundleNode.begin(Transition.UPDATE, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a start transition with state active as the terminal state 
	 * if the activation policy is eager and state starting if the activation 
	 * policy is lazy
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void start(BundleNode bundleNode) {
		if (BundleProjectMetaImpl.INSTANCE.getCachedActivationPolicy(bundleNode.getBundle())) {
			bundleNode.begin(Transition.LAZY_ACTIVATE, StateFactory.INSTANCE.startingState);
		} else {
			bundleNode.begin(Transition.START, StateFactory.INSTANCE.activeState);
		}
	}
}
