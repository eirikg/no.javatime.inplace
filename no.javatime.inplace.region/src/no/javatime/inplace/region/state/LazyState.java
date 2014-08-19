package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.ManifestOptions;

public class LazyState extends BundleState {

	public LazyState() {
		super();
	}

	public void stop(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.STOP, StateFactory.INSTANCE.resolvedState);
		// bundleNode.setCurrentState(StateFactory.INSTANCE.resolvedState);
	}

	/**
	 * Begins a start transition with state active as the terminal state
	 * <p>
	 * Bundles with lazy activation policy (lazy activation) in state starting are activated by the framework due to
	 * "on demand" class loading. 
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void start(BundleNode bundleNode) throws InPlaceException {
//		bundleNode.begin(Transition.START, StateFactory.INSTANCE.activeState);
		 if (!ManifestOptions.getlazyActivationPolicy(getBundle(bundleNode))) {
			 bundleNode.begin(Transition.START, StateFactory.INSTANCE.activeState);
		 }
	}

	public void refresh(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.REFRESH, StateFactory.INSTANCE.resolvedState);
		// bundleNode.setCurrentState(StateFactory.INSTANCE.resolvedState);
		// Move one more transition to state installed
		bundleNode.getState().refresh(bundleNode);
	}
}
