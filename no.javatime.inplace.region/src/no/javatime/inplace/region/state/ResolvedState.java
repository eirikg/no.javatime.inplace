package no.javatime.inplace.region.state;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.ManifestOptions;

/**
 * Begins a set of transitions specified by the standard OSGi state machine with
 * {@link org.osgi.framework.Bundle#RESOLVED RESOLVED} as the current state.
 * Each method in this class represents a valid transition for a bundle in state resolved
 */
public class ResolvedState extends BundleState {

	public ResolvedState() {
		super();
	}

	/**
	 * Begins an uninstall transition with installed as the terminal state
	 * <p>
	 * There is no explicit command to unresolve (move from state RESOLVED to INSTALLED) a bundle.
	 * Uninstalling a bundle in state resolved generates an
	 * {@link org.osgi.framework.BundleEvent#UNRESOLVED UNRESOLVED} bundle event in
	 * {@link BundleStateEvents#bundleChanged(org.osgi.framework.BundleEvent)}. This transition is
	 * generated from this unresolved event and indirectly by the uninstall command.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 * @see BundleStateEvents#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void uninstall(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.UNINSTALL, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a resolve transition with resolved as the terminal state
	 * <p>
	 * The terminal state of a bundle in state resolved is resolveed independent of the activation
	 * policy of the bundle
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void resolve(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.RESOLVE, StateFactory.INSTANCE.resolvedState);
	}

	/**
	 * Begins a refresh transition with installed as the terminal state
	 * <p>
	 * If a bundle project is deactivated in an activated workspace the bundle will first be
	 * unresolved (moved to state INSTALLED) by refresh but will not be resolved (by design ignored by
	 * the resolver hook) again during refresh. For activated bundle projects the bundle will be
	 * unresolved and then resolved again during refresh reentering state RESOLVED.
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void refresh(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.REFRESH, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins an update transition with installed as the terminal state
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void update(BundleNode bundleNode) throws InPlaceException {
		bundleNode.begin(Transition.UPDATE, StateFactory.INSTANCE.installedState);
	}

	/**
	 * Begins a start transition with state starting as the terminal state if the activation policy is
	 * lazy and active as the terminal state if the policy is eager
	 * 
	 * @param bundleNode saves and updates the current transition and state of the bundle
	 */
	public void start(BundleNode bundleNode) throws InPlaceException {
		if (ManifestOptions.getlazyActivationPolicy(getBundle(bundleNode))) {
			bundleNode.begin(Transition.LAZY_LOAD, StateFactory.INSTANCE.lazyState);
		} else {
			bundleNode.begin(Transition.START, StateFactory.INSTANCE.activeState);
		}
	}
}
