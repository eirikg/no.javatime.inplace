package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;

/**
 * Begins a set of outgoing transitions with {@link org.osgi.framework.Bundle#INSTALLED INSTALLED}
 * as the current state. Each method in this class represents a valid transition for a bundle in
 * state installed. The state transitions are in line with the OSGi FSM specification.
 */
public class InstalledState extends BundleState {

	public InstalledState() {
		super();
	}

	/**
	 * Begins an install transition with installed as the terminal state
	 * <p>
	 * This transition happens when trying to install an already installed bundle
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void install(BundleNode bundleNode){
		bundleNode.begin(Transition.INSTALL, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a refresh transition with installed as the terminal state
	 * <p>
	 * Refresh involves two transitions not visualized by the state machine. Refresh resolves the
	 * bundle after unresolving it if in state resolved. When the initial state is installed the
	 * installed bundle is refreshed and than resolved removing any pending revisions.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void refresh(BundleNode bundleNode) {
		bundleNode.begin(Transition.REFRESH, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins an update transition with installed as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void update(BundleNode bundleNode) {
		bundleNode.begin(Transition.UPDATE, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins an uninstall transition with uninstalled as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void uninstall(BundleNode bundleNode) {
		bundleNode.begin(Transition.UNINSTALL, StateFactory.INSTANCE.uninstalledState);
	}

	/**
	 * Begins a resolve transition with resolved as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void resolve(BundleNode bundleNode) {
		
		// Resolve of deactivated bundles is rejected by the resolver hook by design
		if (bundleNode.isActivated()) {
			bundleNode.begin(Transition.RESOLVE, StateFactory.INSTANCE.resolvedState);
		}
	}

}
