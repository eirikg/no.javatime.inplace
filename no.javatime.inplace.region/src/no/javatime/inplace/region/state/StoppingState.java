package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;

/**
 * A stop transition with {@link org.osgi.framework.Bundle#ACTIVE ACTIVE} as the
 * current state. The stop transition represents a valid transition for a bundle in state
 * active.
 */
public class StoppingState extends BundleState {

	public StoppingState() {
		super();
	}

	/**
	 * Begins a stop transition with state resolved as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void stop(BundleNode bundleNode) {
		bundleNode.begin(Transition.STOP, StateFactory.INSTANCE.resolvedState);
	}
}
