package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;

/**
 * Begins a set of outgoing transitions with {@link org.osgi.framework.Bundle#UNINSTALLED
 * UNINSTALLED} as the current state. Each method in this class represents a valid transition for a
 * bundle in state uninstalled.
 * <p>
 * Refreshing an uninstalled bundle is not according to the FSM specification in the OSGi Core
 * Release 5 Chapter 4.4.2 but it should have the effect of releasing wires as long as the
 * unisntalled bundle has requirements on activated bundles when uninstalled from state resolved and
 * not refreshed when in state installed.
 */
public class UninstalledState extends BundleState {

	public UninstalledState() {
		super();
	}
	
	/**
	 * Begins an install transition with installed as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void install(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.INSTALL, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a refresh transition with uninstalled as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void refresh(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.REFRESH, StateFactory.INSTANCE.uninstalledState);
	}
}
