package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;

public class ActiveState extends BundleState {

	public ActiveState() {
		super();
	}

	public void stop(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.STOP, StateFactory.INSTANCE.stoppingState);
	}
}
