package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.ManifestOptions;

public class LazyState extends BundleState {

	public LazyState() {
		super();
	}

	public void stop(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);		
	}

	public void start(BundleNode bundleNode) throws InPlaceException {		
		if (!ManifestOptions.getlazyActivationPolicy(getBundle(bundleNode))) {
			bundleNode.setCurrentState(BundleStateFactory.INSTANCE.activeState);		
		}
	}

	public void refresh(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);
		bundleNode.getState().refresh(bundleNode);
	}
}
