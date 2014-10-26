package no.javatime.inplace.region.state;

import no.javatime.inplace.region.intface.BundleTransition.Transition;

/**
 * Begins a outgoing start and stop transition with {@link org.osgi.framework.Bundle#STARTING
 * STARTING} as the current state. Each method in this class represents a valid transition for a
 * bundle in state starting.
 * <p>
 * The stop transition is added for stopping lazy activated bundles but is not according to the OSGi
 * life cycle FSM specification. E.g. OSGi Core Release 5 Chapter 4.4.2. As far as I can see the FSM
 * diagram should include a stop transition with starting as the initial state and stopping or
 * resolve as the terminal state. The bundle has not yet reached state active and there is no
 * transition to move the bundle to state resolved.
 * <p>
 * For a discussion about stopping lazy activated bundles see:
 * http://osdir.com/ml/java-osgi-devel/2009-07/msg00020.html
 */
public class StartingState extends BundleState {

	public StartingState() {
		super();
	}

	/**
	 * Begins a stop transition with state stopping as the terminal state
	 * <p>
	 * Bundles with lazy activation policy (lazy activation) in state starting are stopped
	 * <p>
	 * This is not according to the FSM specification.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void stop(BundleNode bundleNode) {
		bundleNode.begin(Transition.STOP, StateFactory.INSTANCE.stoppingState);
	}

	/**
	 * Begins a start transition with state active as the terminal state
	 * <p>
	 * Bundles with lazy activation policy (lazy activation) in state starting are activated by the
	 * framework due to "on demand" class loading.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void start(BundleNode bundleNode) {
		bundleNode.begin(Transition.START, StateFactory.INSTANCE.activeState);
	}
}
