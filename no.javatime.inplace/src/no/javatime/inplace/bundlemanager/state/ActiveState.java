package no.javatime.inplace.bundlemanager.state;

import no.javatime.inplace.bundlemanager.InPlaceException;

public class ActiveState extends BundleState {

	public ActiveState() {
		super();
	}

	public void stop(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);		
	}
}
