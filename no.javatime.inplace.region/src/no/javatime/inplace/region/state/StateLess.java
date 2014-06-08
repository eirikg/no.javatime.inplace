package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.InPlaceException;

/**
 * Triggers the FSM by installing the specified bundle moving it to the initial {@link org.osgi.framework.Bundle#INSTALLED} state.
 */
public class StateLess extends BundleState {

	public StateLess() {
		super();
	}

	/**
	 * Moves the bundle to state {@link org.osgi.framework.Bundle#ININSTALLED}. This is the
	 * only transition from an entry position.
	 */
	public void install(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);	
	}
}
