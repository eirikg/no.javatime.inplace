package no.javatime.inplace.bundlemanager.state;

import no.javatime.inplace.bundlemanager.InPlaceException;

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
