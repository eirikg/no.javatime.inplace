package no.javatime.inplace.bundlemanager.state;

import no.javatime.inplace.bundlemanager.ExtenderException;

public class ActiveState extends BundleState {

	public ActiveState() {
		super();
	}

	public void stop(BundleNode bundleNode) throws ExtenderException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);		
	}
}
