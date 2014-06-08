package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.InPlaceException;

/**
 * Bundle events sent by the framework: {@link org.osgi.framework.BundleEvent#INSTALLED}
 * Incoming transitions: install, uninstall, refresh, update
 * Loop transitions: update, refresh
 * Outgoing transitions: uninstall, resolve
 */
public class InstalledState extends BundleState {

	public InstalledState() {
		super();
	}
	
	/**
	 * Bundle events sent by the framework: {@link org.osgi.framework.BundleEvent#RESOLVED RESOLVED} and
	 * {@link org.osgi.framework.BundleEvent#LAZY_ACTIVATION LAZY_ACTIVATION}. Resolving a lazy bundle with an
	 * initial state of {@link org.osgi.framework.Bundle#INSTALLED INSTALLED} moves it to state
	 * {@link org.osgi.framework.Bundle#STARTING STARTING} or to state
	 * {@link org.osgi.framework.Bundle#RESOLVED RESOLVED} depending on the activation policy. This is not the
	 * case when the initial state is {@link org.osgi.framework.Bundle#RESOLVED RESOLVED}.
	 * 
	 * @see ResolvedState#resolve()
	 */
	public void resolve(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.resolvedState);
	}

	/**
	 * Bundle events sent by the framework: {@link org.osgi.framework.BundleEvent#UPDATED}
	 */
	public void update(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.installedState);
	}

	/**
	 * (A) When a bundle is refreshed the bundle to refresh and its requiring bundles are refreshed. (1) Refresh
	 * bundle. (2) Resolve bundle (moves is to state RESOLVED) (3) If bundle is lazy it is moved to state
	 * STARTING or ACTIVE (demand loading). If policy is eager the bundle is moved to state RESOLVED.
	 */
	public void refresh(BundleNode bundleNode) throws InPlaceException {
		// Deactivated bundles in an activated workspace are not resolved (rejected by the resolver hook)
		// TODO Test new statement
		if (bundleNode.isActivated()) {
			// if (BundleCommandImpl.INSTANCE.isActivated(getBundle(bundleNode))) {
			resolve(bundleNode);
		}
	}

	/**
	 * Bundle events sent by the framework: {@link org.osgi.framework.BundleEvent#UNINSTALLED}
	 */
	public void uninstall(BundleNode bundleNode) throws InPlaceException {
		bundleNode.setCurrentState(BundleStateFactory.INSTANCE.uninstalledState);
	}

}
