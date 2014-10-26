/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.region.closure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Topological sort of bundles in requiring and providing bundle dependency order.
 * <p>
 * To include installed bundles in the sort use {@linkplain #sortDeclaredProvidingBundles(Collection, Collection)}
 * and {@linkplain #sortDeclaredRequiringBundles(Collection, Collection)}.
 * <p>
 * All cycles from an initial set (start bundles) of bundles are detected for each sort. To detect all cycles
 * in the workspace include all workspace bundles as the initial set to sort.
 * 
 * @see CircularReferenceException
 * @see ProjectSorter
 */
public class BundleSorter extends BaseSorter {

	/**
	 * Sorted collection of bundles
	 */
	private Collection<Bundle> bundleOrder;

	/**
	 * All in use non current wirings
	 */
	private Collection<Bundle> removalPendingBundles;

	/**
	 * Default constructor
	 */
	public BundleSorter() {
	}

	/**
	 * The order is according to the last sort (requiring or providing order)
	 * 
	 * @return an ordered collection of bundles or an empty collection if no sort has been performed
	 */
	public Collection<Bundle> getBundleOrder() {
		if (null == bundleOrder) {
			return Collections.<Bundle>emptySet();
		}
		return bundleOrder;
	}

	/**
	 * Topological sort in requiring bundle order where the specified scope is all workspace bundles.Installed
	 * bundles are not included. Initial set of specified bundles are included in the result set.
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of start bundles included in the result set
	 * @return collection of bundles in requiring sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> sortRequiringBundles(Collection<Bundle> bundles)
			throws CircularReferenceException {
		return sortRequiringBundles(bundles, BundleWorkspaceRegionImpl.INSTANCE.getBundles());
	}

	/**
	 * Topological sort in requiring bundle order constrained to the specified scope of bundles. Installed
	 * bundles are not included. Initial set of specified bundles are included in the result set.
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of start bundles included in the result set
	 * @param bundleScope of bundles to sort
	 * @return collection of bundles in requiring sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> sortRequiringBundles(final Collection<Bundle> bundles, final Collection<Bundle> bundleScope)
			throws CircularReferenceException {
		
		/*
		Returns all bundles which depend on this bundle. 
		A bundle depends on another bundle if it requires the bundle, 
		imports a package which is exported by the bundle, 
		is a fragment to the bundle or is the host of the bundle.
		*/
		removalPendingBundles = BundleCommandImpl.INSTANCE.getRemovalPending();
		removalPendingBundles.retainAll(bundleScope);
		if (removalPendingBundles.size() > 0) {
			sortDeclaredRequiringBundles(bundles, bundleScope);
		} else {
			circularException = null;
			bundleOrder = new LinkedHashSet<Bundle>();
			if (null == bundles) {
				return bundleOrder;
			}
			for (final Bundle bundle : bundles) {
				visitRequiringBundle(bundle, null, bundleScope, new LinkedHashSet<Bundle>());
			}
		}
		if (null != circularException) {
			throw circularException;
		}
		return bundleOrder;
	}

	/**
	 * Topological sort in requiring bundle order constrained to the specified scope of bundles. Installed
	 * bundles and the specified initial set of bundles are included in the result set.
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of start bundles included in the result set
	 * @param bundleScope of bundles to sort
	 * @return collection of bundles in requiring sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @see #sortRequiringBundles(Collection, Collection)
	 */
	public Collection<Bundle> sortDeclaredRequiringBundles(final Collection<Bundle> bundles,
			final Collection<Bundle> bundleScope) throws CircularReferenceException {
		bundleOrder = new LinkedHashSet<Bundle>();
		if (null == bundles) {
			return bundleOrder;
		}
		circularException = null;
		final Collection<BundleRevision> bundleRevisionsScope = BundleDependencies.getRevisionsFrom(bundleScope);
		final Collection<BundleRevision> bundleRevisions = BundleDependencies.getRevisionsFrom(bundles);
		for (final BundleRevision bundleRevision : bundleRevisions) {
			visitRequiringBundle(bundleRevision, null, bundleRevisionsScope, new LinkedHashSet<BundleRevision>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return bundleOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial bundle. The graph is in dependency order
	 * and added to the internal stored bundle order which is accessible from {@linkplain #getBundleOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial providing bundle
	 * @param parent a requiring bundle to the {@code child} bundle parameter. May be null.
	 * @param scope limit the set of bundles to search for dependencies relative to the workspace
	 * @param visited is a list of bundles in the current call stack (used for detecting cycles)
	 */
	protected void visitRequiringBundle(final BundleRevision child, final BundleRevision parent,
			final Collection<BundleRevision> scope, final Collection<BundleRevision> visited) {

		final Bundle childBundle = child.getBundle();
		// Has this start bundle element been visited before (not through recursion)
		if (!bundleOrder.contains(childBundle)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				if (null != parent) {
					handleBundleCycle(childBundle, parent.getBundle());
				} else {
					handleBundleCycle(childBundle, null);
				}
				return;
			}
			visited.add(child);
			final Collection<BundleRevision> requirers = BundleDependencies.getDirectRequiringBundles(child, scope);
			for (final BundleRevision requirer : requirers) {
				visitRequiringBundle(requirer, child, scope, visited);
			}
			bundleOrder.add(childBundle);
		}
	}

	/**
	 * Traverse the partial graph of the specified child as the initial bundle. The graph is in dependency order
	 * and added to the internal stored bundle order which is accessible from {@linkplain #getBundleOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial providing bundle
	 * @param parent a requiring bundle to the {@code child} bundle parameter. May be null.
	 * @param scope limit the set of bundles to search for dependencies relative to the workspace
	 * @param visited is a list of bundles in the current call stack (used for detecting cycles)
	 */
	protected void visitRequiringBundle(Bundle child, Bundle parent, Collection<Bundle> scope,
			Collection<Bundle> visited) throws CircularReferenceException {

		// Has this start bundle element been visited before (not through recursion)
		if (!bundleOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleBundleCycle(child, parent);
				return;
			}
			visited.add(child);
			final Collection<Bundle> requirers = getDirectRequiringBundles(child, scope);
			for (final Bundle requirer : requirers) {
				visitRequiringBundle(requirer, child, scope, visited);
			}
			bundleOrder.add(child);
		}
	}

	/**
	 * Get the neighboring bundles that requires capabilities from this bundle
	 * 
	 * @param bundle of the bundle which other bundles require capabilities from
	 * @param scope the scope of bundles to include in the result
	 * @return set of neighboring bundles who require capabilities from the specified bundle
	 */
	public Collection<Bundle> getDirectRequiringBundles(final Bundle bundle, final Collection<Bundle> scope) {

		if (null != bundle) {
			BundleWiring wiredReqBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredReqBundle && wiredReqBundle.isInUse()) {
				final List<Bundle> requiredBundles = new ArrayList<Bundle>();
				// Get the capabilities from all name spaces
				for (BundleWire wire : wiredReqBundle.getProvidedWires(null)) {
					Bundle reqBundle = wire.getRequirerWiring().getBundle();
					if (null != reqBundle && BundleWorkspaceRegionImpl.INSTANCE.exist(reqBundle)) {
						// Restrict to scope
						if (scope.contains(reqBundle)) {
							requiredBundles.add(reqBundle);
						}
					}
				}
				return requiredBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Topological sort in providing bundle order where the specified scope is all workspace bundles. Installed
	 * bundles are not included. Initial set of specified bundles are included in the result set.
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of start bundles
	 * @return collection of bundles in providing sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> sortProvidingBundles(Collection<Bundle> bundles)
			throws CircularReferenceException {
		return sortProvidingBundles(bundles, BundleWorkspaceRegionImpl.INSTANCE.getBundles());
	}

	/**
	 * Topological sort in providing bundle order constrained to the specified scope of bundles. Installed
	 * bundles are not included. Initial set of specified bundles are included in the result set.
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of start bundles included in the result set
	 * @param bundleScope of bundles to sort
	 * @return collection of bundles in providing sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> sortProvidingBundles(final Collection<Bundle> bundles, final Collection<Bundle> bundleScope)
			throws CircularReferenceException {
		circularException = null;
		removalPendingBundles = BundleCommandImpl.INSTANCE.getRemovalPending();
		removalPendingBundles.retainAll(bundleScope);
		if (removalPendingBundles.size() > 0) {
			sortDeclaredProvidingBundles(bundles, bundleScope);
		} else {
			bundleOrder = new LinkedHashSet<Bundle>();
			if (null == bundles) {
				return bundleOrder;
			}
			for (final Bundle bundle : bundles) {
				visitProvidingBundle(bundle, null, bundleScope, new LinkedHashSet<Bundle>());
			}
		}
		if (null != circularException) {
			throw circularException;
		}
		return bundleOrder;
	}

	/**
	 * Topological sort in providing bundle order constrained to the specified scope of bundles. Installed
	 * bundles and the specified initial set of bundles are included in the result set
	 * <p>
	 * If the specified bundle collection is null or empty an empty collection is returned.
	 * 
	 * @param bundles a collection of initial (start) bundles included in the result set
	 * @param bundleScope of bundles to sort
	 * @return collection of bundles in providing sort order
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @see #sortProvidingBundles(Collection, Collection)
	 */
	public Collection<Bundle> sortDeclaredProvidingBundles(final Collection<Bundle> bundles, final Collection<Bundle> bundleScope)
			throws CircularReferenceException {

		bundleOrder = new LinkedHashSet<Bundle>();
		if (null == bundles) {
			return bundleOrder;
		}
		circularException = null;
		final Collection<BundleRevision> bundleRevisionsScope = BundleDependencies.getRevisionsFrom(bundleScope);
		final Collection<BundleRevision> bundleRevisions = BundleDependencies.getRevisionsFrom(bundles);
		for (final BundleRevision bundleRevision : bundleRevisions) {
			visitProvidingBundle(bundleRevision, null, bundleRevisionsScope, new LinkedHashSet<BundleRevision>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return bundleOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial bundle. The graph is in dependency order
	 * and added to the internal stored bundle order which is accessible from {@linkplain #getBundleOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial requiring bundle
	 * @param parent a providing bundle to the {@code child} bundle parameter. May be null.
	 * @param scope limit the set of bundles to search for dependencies relative to the workspace
	 * @param visited is a list of bundles in the current call stack (used for detecting cycles)
	 */
	protected void visitProvidingBundle(final BundleRevision child, final BundleRevision parent,
			final Collection<BundleRevision> scope, final Collection<BundleRevision> visited) {

		final Bundle childBundle = child.getBundle();
		// Has this start bundle element been visited before (not through recursion)
		if (!bundleOrder.contains(childBundle)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				if (null != parent) {
					handleBundleCycle(childBundle, parent.getBundle());
				} else {
					handleBundleCycle(childBundle, null);
				}
				return;
			}
			visited.add(child);
			Collection<BundleRevision> providers = null;
			providers = BundleDependencies.getDirectProvidingBundles(child, scope);
			for (final BundleRevision provider : providers) {
				visitProvidingBundle(provider, child, scope, visited);
			}
			bundleOrder.add(childBundle);
		}
	}

	/**
	 * Traverse the partial graph of the specified child as the initial bundle. The graph is in dependency order
	 * and added to the internal stored bundle order which is accessible from {@linkplain #getBundleOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial requiring bundle
	 * @param parent a providing bundle to the {@code child} bundle parameter. May be null.
	 * @param scope limit the set of bundles to search for dependencies relative to the workspace
	 * @param visited is a list of bundles in the current call stack (used for detecting cycles)
	 */
	protected void visitProvidingBundle(Bundle child, Bundle parent, Collection<Bundle> scope,
			Collection<Bundle> visited) {

		// Has this start bundle element been visited before (not through recursion)
		if (!bundleOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleBundleCycle(child, parent);
				return;
			}
			visited.add(child);
			Collection<Bundle> providers = null;
			providers = getDirectProvidingBundles(child, scope);
			for (final Bundle provider : providers) {
				visitProvidingBundle(provider, child, scope, visited);
			}
			bundleOrder.add(child);
		}
	}

	/**
	 * Get the neighboring bundles that provides capabilities to this bundle.
	 * 
	 * @param bundle that requires capabilities from other bundles
	 * @param scope the scope of bundles to include in the result
	 * @return set of neighboring bundles who provide capabilities to the specified bundle
	 */
	public Collection<Bundle> getDirectProvidingBundles(final Bundle bundle, final Collection<Bundle> scope) {

		if (null != bundle) {
			BundleWiring wiredProvBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredProvBundle && wiredProvBundle.isInUse()) {
				List<Bundle> providedBundles = new ArrayList<Bundle>();
				// Get the requirements from all name spaces
				for (BundleWire wire : wiredProvBundle.getRequiredWires(null)) {
					Bundle provBundle = wire.getProviderWiring().getBundle();
					if (null != provBundle && BundleWorkspaceRegionImpl.INSTANCE.exist(provBundle)) {
						// Adjust to scope
						if (scope.contains(provBundle)) {
							providedBundles.add(provBundle);
						}
					}
				}
				return providedBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Adds bundles included in the cycle and a status message describing the cycle to the
	 * {@linkplain CircularReferenceException}.
	 * 
	 * @param child bundle that refers to the parent bundle parameter
	 * @param parent bundle that refers to the child bundle parameter
	 */
	protected void handleBundleCycle(final Bundle child, final Bundle parent) {
		boolean directRecursion = false;
		// Self reference
		if (parent == child) {
			if (getAllowSelfReference()) {
				return;
			}
			directRecursion = true;
		}
		if (null == child || null == parent) {
			String msg = ExceptionMessage.getInstance().formatString("internal_error_detecting_cycles");
			throw new CircularReferenceException(msg);
		}
		// Hosts can import packages from fragment (no complaints from Equinox),
		// even if fragment is an inherent part of the host. Is this a kind of self reference?
		// Must check both parent and child, due to traversal order (providing or requiring)
		if (!getAllowCycles() && (!isFragment(child) && !isFragment(parent))) {
			BundleSorter bs = new BundleSorter();
			bs.setAllowCycles(true);
			Collection<Bundle> bundles = bs.sortDeclaredRequiringBundles(Collections.<Bundle>singletonList(parent), BundleWorkspaceRegionImpl.INSTANCE.getBundles());
			BundleTransitionImpl.INSTANCE.setTransitionError(parent, TransitionError.CYCLE);
			bundles.addAll(bs.sortDeclaredRequiringBundles(Collections.<Bundle>singletonList(child), BundleWorkspaceRegionImpl.INSTANCE.getBundles()));
			BundleTransitionImpl.INSTANCE.setTransitionError(child, TransitionError.CYCLE);
			if (null == circularException) {
				circularException = new CircularReferenceException();
			}
			String msg = ExceptionMessage.getInstance().formatString("affected_bundles",
					BundleWorkspaceRegionImpl.INSTANCE.formatBundleList(bundles, false));
			circularException.addToStatusList(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg, null));
			if (directRecursion) {
				msg = ExceptionMessage.getInstance().formatString("direct_circular_reference_with_bundles",
						parent.getSymbolicName());
			} else {
				msg = ExceptionMessage.getInstance().formatString("circular_reference_with_bundles",
						parent.getSymbolicName(), child.getSymbolicName());
			}
			circularException.addToStatusList(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, null));
			circularException.addBundles(bundles);
		}
	}

	/**
	 * Get all the hosts the specified fragment bundle is attached to
	 * 
	 * @param fragmentBundle the fragment bundle attached to zero or more hosts
	 * @return a list of hosts or an empty list
	 */
	public static Collection<Bundle> getHosts(final Bundle fragmentBundle) {

		if (isFragment(fragmentBundle)) {
			final Collection<Bundle> hosts = new ArrayList<Bundle>();
			final BundleRevision br = fragmentBundle.adapt(BundleRevision.class);
			final BundleWiring bWiring = br.getWiring();
			if (null == bWiring) {
				return Collections.<Bundle>emptySet();
			}
			final Collection<BundleWire> wires = bWiring.getRequiredWires(BundleRevision.HOST_NAMESPACE);
			if (null != wires) {
				for (final BundleWire wire : wires) {
					final BundleWiring bw = wire.getProviderWiring();
					if (null != bw) {
						hosts.add(bw.getBundle());
					}
				}
				return hosts;
			}
		}
		return Collections.<Bundle>emptySet();
	}

	/**
	 * Get all fragments attached to the specified bundle
	 * 
	 * @param bundle the bundle with zero or more attached fragments
	 * @return a list of fragments or an empty list
	 */
	public static Collection<Bundle> getFragments(final Bundle bundle) {

		final Collection<Bundle> fragments = new ArrayList<Bundle>();
		final BundleRevision br = bundle.adapt(BundleRevision.class);
		final BundleWiring bWiring = br.getWiring();
		if (null == bWiring) {
			return Collections.<Bundle>emptySet();
		}
		final Collection<BundleWire> wires = bWiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
		if (null != wires) {
			for (final BundleWire wire : wires) {
				final BundleWiring bw = wire.getRequirerWiring();
				if (null != bw) {
					fragments.add(bw.getBundle());
				}
				return fragments;
			}
		}
		return Collections.<Bundle>emptySet();
	}

	/**
	 * Check if the specified bundle is a fragment
	 * 
	 * @param bundle that is either a fragment or not
	 * @return true if the bundle is a fragment and false if the bundles is not a fragment or the bundle could
	 *         not be adapted to a bundle revision
	 * @throws InPlaceException
	 */
	public static Boolean isFragment(Bundle bundle) throws InPlaceException {
		if (null != bundle) {
			BundleRevision rev = bundle.adapt(BundleRevision.class);
			if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prints the set of bundles to {@code System.out} in topological bundle order
	 */
	@SuppressWarnings("unused")
	private void printBundleOrder() {
		if (null == bundleOrder) {
			return;
		}
		System.out.println("Bundle topological Order "
				+ BundleWorkspaceRegionImpl.INSTANCE.formatBundleList(bundleOrder, true));
	}
}
