package no.javatime.inplace.bundlemanager.state;

import no.javatime.inplace.bundlemanager.ExtenderException;

/**
 * Uninstalled is the initial state for a specified bundle. The only atomic transition is install, which may be initiated by
 * activate and install operations.
 */
public class UninstalledState extends BundleState {

	public UninstalledState() {
		super();
	}

	public void install(BundleNode bundleNode) throws ExtenderException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
	}
}
