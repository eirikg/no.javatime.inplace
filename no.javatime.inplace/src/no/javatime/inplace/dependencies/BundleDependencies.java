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
package no.javatime.inplace.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundle.log.intface.BundleLog;
import no.javatime.inplace.bundle.log.intface.BundleLog.Device;
import no.javatime.inplace.bundle.log.intface.BundleLog.MessageType;
import no.javatime.inplace.bundle.log.status.BundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A set of methods returning direct and indirect dependencies between installed bundles. For installed
 * bundles there is one set of methods accepting {@code Bundle} as parameters and one accepting
 * {@code BundleRevision}.
 * <p>
 * Only a subset of methods includes installed (state INSTALLED) bundles. Also note the difference between
 * direct and transitive dependencies.
 */
public class BundleDependencies {

	/**
	 * Default constructor. No initializations.
	 */
	public BundleDependencies() {
	}

	/**
	 * Get requiring bundles from a providing bundle. If the providing bundle is in state INSTALLED all
	 * available bundles that are not UNINSTALLED are investigated. If the providing bundle is in state RESOLVED
	 * or ACTIVE/STARTING/STOPPING only resolved and active bundles are investigated. All available bundles in
	 * the workspace are visited.
	 * 
	 * @param provider is a bundle in any state (except UNINSTALLED), which provide capabilities to all
	 *          requiring bundles
	 * @return all bundles that the requirer need capabilities from
	 */
	public static Collection<Bundle> getRequiringBundles(Bundle provider) {

		Collection<Bundle> requirers = null;
		if (null != provider) {
			if ((provider.getState() & (Bundle.INSTALLED)) != 0) {
				requirers = getRequiringBundles(provider, BundleManager.getRegion().getBundles());
			} else {
				requirers = getRequiringBundles(provider, null, new LinkedHashSet<Bundle>());
			}
		}
		return requirers;
	}

	/**
	 * Get direct and indirect bundles dependent on the specified initial bundle. Requiring bundles include
	 * self.
	 * 
	 * @param child is the independent initial bundle
	 * @param parent of child, may be null if child is a root bundle
	 * @param visited bundles dependent on {@code child}
	 * @return all bundles which are dependent on {@code child}
	 */
	public static Collection<Bundle> getRequiringBundles(Bundle child, Bundle parent, Collection<Bundle> visited) {

		if (null != child && !visited.contains(child)) {
			if (null != parent) {
				visited.add(child);
			}
			Collection<Bundle> requiringBundles = getDirectRequiringBundles(child);
			for (Bundle requiringBundle : requiringBundles) {
				getRequiringBundles(requiringBundle, child, visited);
			}
		}
		return visited;
	}

	/**
	 * Get the nearest bundles that requires capabilities from this bundle
	 * 
	 * @param bundle which other bundles require capabilities from
	 * @return the list of direct bundles who require capabilities from this bundle
	 */
	public static Collection<Bundle> getDirectRequiringBundles(Bundle bundle) {

		if (null != bundle) {
			BundleWiring wiredReqBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredReqBundle && wiredReqBundle.isInUse()) {
				List<Bundle> requiredBundles = new ArrayList<Bundle>();
				// Get the capabilities from all name spaces
				for (BundleWire wire : wiredReqBundle.getProvidedWires(null)) {
					Bundle reqBundle = wire.getRequirerWiring().getBundle();
					if (null != reqBundle && BundleManager.getRegion().exist(reqBundle)) {
						requiredBundles.add(reqBundle);
					}
				}
				return requiredBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Get providing bundles from a requiring bundle. If the requiring bundle is in state INSTALLED all
	 * available bundles that are not UNINSTALLED are investigated. If the requiring bundle is in state RESOLVED
	 * or ACTIVE/STARTING/STOPPING only resolved and active bundles are investigated. All available bundles in
	 * the workspace are visited.
	 * 
	 * @param requirer is a bundle in any state (except UNINSTALLED), which require capabilities from providing
	 *          bundles
	 * @return all bundles that the requirer need capabilities from
	 */
	public static Collection<Bundle> getProvidingBundles(Bundle requirer) {

		Collection<Bundle> providers = null;
		if (null != requirer) {
			if ((requirer.getState() & (Bundle.INSTALLED)) != 0) {
				providers = getProvidingBundles(requirer, BundleManager.getRegion().getBundles());
			} else {
				providers = getProvidingBundles(requirer, null, new LinkedHashSet<Bundle>());
			}
		}
		return providers;
	}

	/**
	 * Get direct and indirect bundles dependent on the specified initial bundle. Providing bundles include self.
	 * 
	 * @param child is the dependent initial bundle
	 * @param parent of child, may be null if child is a root bundle
	 * @param visited bundles this bundle ({@code child}) is dependent on
	 * @return the list of bundles that this bundle requires capabilities from
	 */
	public static Collection<Bundle> getProvidingBundles(Bundle child, Bundle parent, Collection<Bundle> visited) {
		if (null != child && !visited.contains(child)) {
			if (null != parent) {
				visited.add(child);
			}
			Collection<Bundle> providingBundles = getDirectProvidingBundles(child);
			for (Bundle providingBundle : providingBundles) {
				getProvidingBundles(providingBundle, child, visited);
			}
		}
		return visited;
	}

	/**
	 * Returns the nearest bundles that this {@code bundle} requires capabilities from
	 * 
	 * @param bundle that requires capabilities from other bundles
	 * @return the list of direct bundles that this bundle requires capabilities from
	 */
	public static Collection<Bundle> getDirectProvidingBundles(Bundle bundle) {

		if (null != bundle) {
			BundleWiring wiredProvBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredProvBundle && wiredProvBundle.isInUse()) {
				List<Bundle> providedBundles = new ArrayList<Bundle>();
				// Get the requirements from all name spaces
				for (BundleWire wire : wiredProvBundle.getRequiredWires(null)) {
					Bundle provBundle = wire.getProviderWiring().getBundle();
					if (null != provBundle && BundleManager.getRegion().exist(provBundle)) {
						providedBundles.add(provBundle);
					}
				}
				return providedBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Check which bundles in a collection of {@code providers} this {@code requirer} bundle directly or
	 * indirectly need capabilities (require) from. Works on installed bundles. This is in opposite direction to
	 * {@linkplain #getRequiringBundles(Bundle, Collection)}
	 * 
	 * @param requirer is the bundle who require capabilities from a set provider bundles
	 * @param providers are a collection of bundles where each bundle provide one or more capabilities
	 * @return all bundles that the requirer need capabilities from
	 * @see #getProvidingBundles(BundleRevision, Collection, BundleRevision, Collection)
	 */
	public static Collection<Bundle> getProvidingBundles(Bundle requirer, Collection<Bundle> providers) {
		BundleRevision req = requirer.adapt(BundleRevision.class);
		Collection<BundleRevision> provs = getRevisionsFrom(providers);
		Collection<BundleRevision> br = getProvidingBundles(req, provs, null, new LinkedHashSet<BundleRevision>());
		return getBundlesFrom(br);
	}

	/**
	 * Check which bundles in a collection of {@code providers} this {@code requirer} bundle directly or
	 * indirectly need capabilities (require) from. Only works on installed bundles.
	 * 
	 * @param requirer is the bundle revision who require capabilities from the revisions of a set provider
	 *          bundles
	 * @param providers are a collection of bundles where each bundle provide one or more capabilities
	 * @param parent is the direct parent of the {@code requirer} parameter. Parent may be null
	 * @param visited are all bundle revisions that has requirements on them. Parameter may be empty.
	 * @return all bundle revisions that the requirer need capabilities from
	 * @see #getProvidingBundles(Bundle, Collection)
	 */
	public static Collection<BundleRevision> getProvidingBundles(BundleRevision requirer,
			Collection<BundleRevision> providers, BundleRevision parent, Collection<BundleRevision> visited) {

		if (null != requirer && !visited.contains(requirer)) {
			if (null != parent) {
				visited.add(requirer);
			}
			Collection<BundleRevision> requiringBundles = getDirectProvidingBundles(requirer, providers);
			for (BundleRevision requiringBundle : requiringBundles) {
				getProvidingBundles(requiringBundle, providers, requirer, visited);
			}
		}
		return visited;
	}

	/**
	 * Check which nearest bundles in a collection of {@code providers} this {@code requirer} bundle need
	 * capabilities (require) from. Only works on installed bundles.
	 * 
	 * @param requirer is the bundle who require capabilities from a set provider bundles
	 * @param providers are a collection of bundles where each bundle provide one or more capabilities
	 * @return all nearest (direct) bundles that the requirer need capabilities from
	 * @see #getDirectProvidingBundles(BundleRevision, Collection)
	 */
	public static Collection<Bundle> getDirectProvidingBundles(Bundle requirer, Collection<Bundle> providers) {
		BundleRevision req = requirer.adapt(BundleRevision.class);
		Collection<BundleRevision> provs = getRevisionsFrom(providers);
		Collection<BundleRevision> br = getDirectProvidingBundles(req, provs);
		return getBundlesFrom(br);
	}

	/**
	 * Check which nearest bundles in a collection of {@code providers} this {@code requirer} bundle need
	 * capabilities (require) from. Only works on installed bundles.
	 * 
	 * @param requirer is the bundle that requires capabilities from other bundles
	 * @param providers is the collection of bundles that provide capabilities to other requiring bundles
	 * @return a collection of bundles that the {@code requirer} needs capabilities from or an empty collection
	 * @see #getDirectProvidingBundles(Bundle, Collection)
	 */
	public static Collection<BundleRevision> getDirectProvidingBundles(BundleRevision requirer,
			Collection<BundleRevision> providers) {
		Collection<BundleRevision> bundles = new LinkedHashSet<BundleRevision>();
		// Get the requirements from all name spaces
		Collection<BundleRequirement> requirements = requirer.getDeclaredRequirements(null);
		for (BundleRequirement requirement : requirements) {
			for (BundleRevision provider : providers) {
				// Get the capabilities from all name spaces
				for (BundleCapability capability : provider.getDeclaredCapabilities(null)) {
					if (requirement.matches(capability)) {
						bundles.add(provider);
					}
				}
			}
		}
		return bundles;
	}

	/**
	 * Checks and return any of the bundles ({@code requires}) in a collection that directly or indirectly
	 * require capabilities from this {@code provider} bundle. The dependency check may be performed on
	 * installed bundles. This is in opposite direction to {@linkplain #getProvidingBundles(Bundle, Collection)}
	 * 
	 * @param provider is the bundle providing capabilities
	 * @param requirers are a set of bundles who may have requirements on the {@code provider} bundle
	 * @return all bundles that have requirements on a provider bundle
	 * @see #getRequiringBundles(BundleRevision, Collection, BundleRevision, Collection)
	 */
	public static Collection<Bundle> getRequiringBundles(Bundle provider, Collection<Bundle> requirers) {
		BundleRevision prov = provider.adapt(BundleRevision.class);
		Collection<BundleRevision> reqs = getRevisionsFrom(requirers);
		Collection<BundleRevision> br = getRequiringBundles(prov, reqs, null, new LinkedHashSet<BundleRevision>());
		return getBundlesFrom(br);
	}

	/**
	 * Checks and return any of the bundle revisions({@code requires}) in a collection that directly or
	 * indirectly require capabilities from this {@code provider} bundle revision. The dependency check may be
	 * performed on installed bundles. This is in opposite direction to
	 * {@linkplain #getProvidingBundles(Bundle, Collection)}
	 * 
	 * @param provider is the bundle providing capabilities
	 * @param requirers are a set of bundles who may have requirements on the {@code provider} bundle
	 * @param parent is the direct parent of the {@code requirer} parameter. Parent may be null
	 * @param visited are all bundle revisions that has requirements on them. Parameter may be empty.
	 * @return all bundles that have requirements on a provider bundle
	 * @see #getRequiringBundles(BundleRevision, Collection, BundleRevision, Collection)
	 */
	public static Collection<BundleRevision> getRequiringBundles(BundleRevision provider,
			Collection<BundleRevision> requirers, BundleRevision parent, Collection<BundleRevision> visited) {

		if (null != provider && !visited.contains(provider)) {
			if (null != parent) {
				visited.add(provider);
			}
			Collection<BundleRevision> providerBundles = getDirectRequiringBundles(provider, requirers);
			for (BundleRevision providerBundle : providerBundles) {
				getRequiringBundles(providerBundle, requirers, provider, visited);
			}
		}
		return visited;
	}

	/**
	 * Checks and return any of the nearest (direct) bundles ({@code requires}) in a collection that require
	 * capabilities from this {@code provider} bundle. The dependency check may be performed on installed
	 * bundles. This is in opposite direction to {@linkplain #getDirectProvidingBundles(Bundle, Collection)}
	 * 
	 * @param provider is the bundle providing capabilities
	 * @param requirers are a set of bundles who may have requirements on the {@code provider} bundle
	 * @return all bundles that have requirements on a provider bundle
	 * @see #getDirectRequiringBundles(BundleRevision, Collection)
	 */
	public static Collection<Bundle> getDirectRequiringBundles(Bundle provider, Collection<Bundle> requirers) {
		BundleRevision prov = provider.adapt(BundleRevision.class);
		Collection<BundleRevision> reqs = getRevisionsFrom(requirers);
		Collection<BundleRevision> br = getDirectRequiringBundles(prov, reqs);
		return getBundlesFrom(br);
	}

	/**
	 * Checks and return any of the nearest (direct) bundle revisions ({@code requires}) in a collection that
	 * require capabilities from this {@code provider} bundle revision. The dependency check may be performed on
	 * installed bundles. This is in opposite direction to {@linkplain #getProvidingBundles(Bundle, Collection)}
	 * 
	 * @param provider is the bundle providing capabilities
	 * @param requirers are a set of bundles who may have requirements on the {@code provider} bundle
	 * @return all bundles that have requirements on a provider bundle
	 * @see #getDirectRequiringBundles(Bundle, Collection)
	 */
	public static Collection<BundleRevision> getDirectRequiringBundles(BundleRevision provider,
			Collection<BundleRevision> requirers) {

		Collection<BundleRevision> requirements = new LinkedHashSet<BundleRevision>();

		// Get the capabilities from all name spaces
		Collection<BundleCapability> capabilities = provider.getDeclaredCapabilities(null);
		for (BundleRevision requierer : requirers) {
			// Get the requirements from all name spaces
			for (BundleRequirement requirement : requierer.getDeclaredRequirements(null)) {
				for (BundleCapability capability : capabilities) {
					if (requirement.matches(capability)) {
						requirements.add(requierer);
					}
				}
			}
		}
		return requirements;
	}

	/**
	 * Check if the specified requiring bundle requires capabilities from the Eclipse UI plug-in.
	 * 
	 * @param requirer the bundle to check for requirement on the UI plug-in
	 * @return true if the specified bundle requires capabilities from the UI plug-in or null if the UI plug-in
	 *         does not exist or the specified bundle does not require capabilities from the UI plug-in
	 * @throws InPlaceException if the specified requirer is null
	 */
	public static Boolean contributesToTheUI(Bundle requirer) throws InPlaceException {

		if (null == requirer) {
			throw new InPlaceException("bundle_null_location");
		}
		Bundle provider = Platform.getBundle("org.eclipse.ui");
		if (null == provider) {
			return false;
		}
		Collection<Bundle> requiresFrom = new LinkedHashSet<Bundle>();
		requiresFrom.add(provider);
		Collection<Bundle> providers = null;
		providers = getProvidingBundles(requirer, requiresFrom);
		if (providers.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Checks if any of the {@code requires} in a collection require capabilities from a
	 * {@code provider} bundle. The dependency check may be performed on installed bundles.
	 * 
	 * @param provider a bundle that provides some capabilities to other bundles
	 * @param requirers a collection of bundles that may require capabilities for the capability
	 *          provider
	 * @return true if one bundle requires capabilities from this capability provider or false if
	 * no capabilities are required
	 */
	public static Boolean hasRequirements(BundleRevision provider, Collection<BundleRevision> requirers) {
		// Get the capabilities of this bundle
		Collection<BundleCapability> capabilities = provider.getDeclaredCapabilities(null);
		for (BundleRevision requierer : requirers) {
	  	// Get the requirements from all name spaces 
			for (BundleRequirement requirement : requierer.getDeclaredRequirements(null)) {
				for (BundleCapability capability : capabilities) {
					if (requirement.matches(capability)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Get all the {@code requires} in a collection that require capabilities from a
	 * {@code provider} bundle. The traversal is not transitive.
	 * The dependency check may be performed on installed bundles.
	 * 
	 * @param provider a bundle that provides some capabilities to other bundles
	 * @param requirers a collection of bundles that may require capabilities for the capability
	 *          provider
	 * @return all bundles that requires capabilities from this capability provider or an empty
	 *         collection if no requirements exist
	 */
	public static Collection<BundleRevision> getRequirements(BundleRevision provider, Collection<BundleRevision> requirers) {

		// Get the capabilities of this bundle
		Collection<BundleCapability> capabilities = provider.getDeclaredCapabilities(null);
		Collection<BundleRevision> requirements = new LinkedHashSet<BundleRevision>();
		for (BundleRevision requierer : requirers) {
	  	// Get the requirements from all name spaces 
			for (BundleRequirement requirement : requierer.getDeclaredRequirements(null)) {
				for (BundleCapability capability : capabilities) {
					if (requirement.matches(capability)) {
						requirements.add(requierer);
					}
				}
			}
		}
		return requirements;
	}

	/**
	 * Takes a collection of bundles and returns their current revision.
	 * If a bundle does not adapt to a revision it is discarded 
	 * 
	 * @param bundles to get the revisions from
	 * @return a collection of current revisions from a collection of {@code bundles}. Never null.
	 */
	public static Collection<BundleRevision> getRevisionsFrom(Collection<Bundle> bundles) {
		Collection<BundleRevision> bundleRevisions = new LinkedHashSet<BundleRevision>();
		for (Bundle bundle : bundles) {
			BundleRevision br = bundle.adapt(BundleRevision.class);
			if (null != br) {
				bundleRevisions.add(br);
			} else {
				String msg = WarnMessage.getInstance().formatString("failed_to_adapt_to_revision", bundle);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		}
		return bundleRevisions;
	}

	/**
	 * Takes a collection of revisions and returns their corresponding bundles
	 * 
	 * @param bundleRevisions to get the bundles from
	 * @return a collection of bundles from a collection of {@code bundleRevisions} or an empty set
	 */
	public static Collection<Bundle> getBundlesFrom(Collection<BundleRevision> bundleRevisions) {
		Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
		for (BundleRevision br : bundleRevisions) {
			bundles.add(br.getBundle());
		}
		return bundles;
	}
}
