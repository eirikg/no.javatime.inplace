package no.javatime.inplace.region.state;

import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;

/**
 * Triggers the FSM by running an install transition moving the bundle to the initial
 * {@link org.osgi.framework.Bundle#INSTALLED INSTALLED} state.
 */
public class StateLess extends BundleState {

	public StateLess() {
		super();
	}

	/**
	 * Moves the bundle to state {@link org.osgi.framework.Bundle#ININSTALLED INSTALLED}. This is the only
	 * transition from an entry position.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void install(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.INSTALL, StateFactory.INSTANCE.installedState);
	}
}
