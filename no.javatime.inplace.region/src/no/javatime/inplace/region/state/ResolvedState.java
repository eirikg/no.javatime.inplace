package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.ManifestOptions;

public class ResolvedState extends BundleState {

	public ResolvedState() {
		super();
	}

	/**
	 * Bundle events sent by the framework: {@link org.osgi.framework.BundleEvent#UNRESOLVED UNRESOLVED} and
	 * {@link org.osgi.framework.BundleEvent#UNINSTALLED UNINSTALLED}. The bundle is moved directly to state
	 * {@link org.osgi.framework.Bundle#UNINSTALLED UNINSTALLED}. This is because there is no explicit
	 * transition in BundleManager to unresolve (move from state RESOLVED to INSTALLED) a bundle except indirectly
	 * through refresh for a deactivated bundle in an active workspace (resolve is rejected by the resolver hook)
	 * 
	 * @see #refresh()
	 */
	public void uninstall(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
	}

	// Is there any point in resolving a bundle in state resolved
	// Never called but shows that resolving bundles with an initial state of resolved always
	// moves the bundle to state resolve independent of the activation policy of the bundle 
	public void resolve(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);
	}

	/**
	 * If a project is already deactivated due to removal of its nature the bundle will first be unresolved
	 * (moved to state INSTALLED) and will not be resolved (by design rejected by the resolver hook) again
	 * during refresh (refresh tries to resolve the bundle) and thus enter state INSTALLED. For activated
	 * projects the bundle will be unresolved and then resolved again during refresh reentering state RESOLVED.
	 */
	public void refresh(BundleNode bundleNode) throws InPlaceException {
//		if (InPlace.bm().isActivated(getBundle(bundleNode))) {
//			bundleNode.setCurrentState(StateFactory.resolvedState);
//		} else {
//			bundleNode.setCurrentState(StateFactory.installedState);
//		}
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
	}

	/**
	 * (1) Unresolve and uninstall bundle. (2) Read input. (3) Install bundle.
	 */
	public void update(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
	}

	public void start(BundleNode bundleNode) throws InPlaceException {
		if (ManifestOptions.getlazyActivationPolicy(getBundle(bundleNode))) {
			bundleNode.setCurrentState(BundleStateFactory.INSTANCE.lazyState);
		} else {
			bundleNode.setCurrentState(BundleStateFactory.INSTANCE.activeState);
		}
	}
}
