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
package no.javatime.inplace.region.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.closure.BundleDependencies;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.state.BundleNode;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

/**
 * A bundle resolver that intercepts the resolve process to filter bundles to resolve and remove
 * duplicate bundles.
 * <p>
 * An instance of this resolver hook is returned by the resolver hook factory service for each
 * resolve process.
 * <p>
 * <b>Removing duplicates.</b>
 * <p>
 * Duplicates, in this context, may occur when a workspace bundle (or plug-in project) has been
 * modified and needs to be updated. If the workspace bundle to update is a singleton a collision
 * occur with the current revision of the bundle to update, when the bundle is resolved as part of
 * the update process. By removing the current resolved bundle from being resolved (the duplicate) a
 * new current revision with the changed code will be wired to the bundles that depends on it as
 * part of the update (and resolve) process.
 * <p>
 * <b>Filtering bundles to resolve.</b>
 * <p>
 * In an activated workspace all bundles are at least in state INSTALLED, also those who are not
 * activated. The OSGI resolver suggest to resolve all installed bundles. The filtering process
 * excludes installed not activated bundles from being resolved. Also, if one or more activated
 * bundles have requirements on deactivated bundles the activated bundles are removed from the
 * candidate resolve list. The deactivated bundles are the marked as pending for activation.
 * <p>
 * If a plug-in use extensions or implement extension-points it is required that the plug-in is a
 * singleton.
 * 
 * @see BundleResolveHookFactory
 */
class BundleResolveHandler implements ResolverHook {

	// Groups of singletons
	private Map<Bundle, Set<Bundle>> groups = null;
	private BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;

	@Override
	public void filterMatches(BundleRequirement r, Collection<BundleCapability> candidates) {
	}

	/**
	 * Remove bundle closures for deactivated bundles and activated bundles dependent on deactivated
	 * bundles from the resolvable candidate list.
	 * <p>
	 * Deactivated bundles will be excluded (removed) from the resolve candidate list. If an activated
	 * bundle is dependent on an installed (not activated) bundle, resolve of the activated bundle is
	 * delayed and a pending activate transition is added to the deactivated bundle project. Any
	 * additional dependencies on the bundle project to activate should be handled by the activation
	 * job.
	 */
	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {

		// Do not infer when workspace is deactivated
		if (!WorkspaceRegionImpl.INSTANCE.isRegionActivated()) {
			return;
		}
		Collection<Bundle> bundles = WorkspaceRegionImpl.INSTANCE.getBundles();
		if (bundles.size() == 0) {
			return;
		}
		Collection<BundleRevision> deactivatedBundles = new LinkedHashSet<BundleRevision>();
		Collection<BundleRevision> activatedBundles = new LinkedHashSet<BundleRevision>();
		// Restrict the scope of bundles to resolve to workspace bundles
		Collection<BundleRevision> workspaceCandidates = BundleDependencies.getRevisionsFrom(bundles);
		// Restrict workspace candidate bundles to candidate bundles closures to resolve
		workspaceCandidates.retainAll(candidates);
		// Split candidates in those activated and those deactivated
		for (BundleRevision workspaceCandidate : workspaceCandidates) {
			Bundle bundle = workspaceCandidate.getBundle();
			BundleNode node = WorkspaceRegionImpl.INSTANCE.getBundleNode(bundle);
			if (node.isActivated()) {
				activatedBundles.add(workspaceCandidate);
			} else {
				deactivatedBundles.add(workspaceCandidate);
			}
		}
		// If no deactivated bundles, all error free bundles are activated and will be resolved
		if (!deactivatedBundles.isEmpty()) {
			candidates.removeAll(deactivatedBundles);
			// If no activated bundles, the candidate list should be empty
			// Note this is not among all workspace bundles, but the ones relevant for this resolve
			if (activatedBundles.isEmpty() && candidates.isEmpty()) {
				return;
			}
		}
		// All deactivated providers are by default removed form the candidate list so there is no need
		// to remove deactivated providers here.
		delayResolve(activatedBundles, deactivatedBundles);
	}

	/**
	 * Filters out deactivated candidate bundles that provide capabilities to activated bundles and
	 * add them as pending transitions for activation
	 * 
	 * <p>
	 * The set of activated candidate bundles that have requirements on deactivated candidate bundles
	 * will be unresolved by this resolve as long as the returned provider(s) are removed from the
	 * candidate list to resolve. They will than be resolved again later together with the deactivated
	 * provider when the provider is resolved as part of the activation process.
	 * <p>
	 * A second alternative is to remove the dependency closure (activated requires and deactivated
	 * providers) from the candidate list to resolve. The activated bundles should than be rolled back
	 * before returning from the resolver hook. The closure would than be resolved when the
	 * deactivated providers are activated.
	 * <p>
	 * A third alternative is to add the deactivated providers to the candidate list to resolve. The
	 * dependency closure will then be resolved again a second time when the deactivated providers are
	 * activated.
	 * <p>
	 * Note the transitivity with respect to activated bundles with dependencies on deactivated
	 * bundles. From this follows that deactivated bundles providing capabilities to other deactivated
	 * bundles providing capabilities to an activated bundle are activated when the deactivated bundle
	 * in question is activated.
	 * 
	 * @param activatedBundleRevisons all activated candidate workspace bundles
	 * @param deactivatedBundleRevisions all deactivated candidate workspace bundles
	 * @return all deactivated bundle revisions providing capabilities to the specified set of
	 * activated bundles
	 */
	private Collection<BundleRevision> delayResolve(
			Collection<BundleRevision> activatedBundleRevisons,
			Collection<BundleRevision> deactivatedBundleRevisions) {

		// Deactivated bundles providing capabilities to activated bundles
		Collection<BundleRevision> deactivatedProviders = null;

		// Does any bundles in the set of activated candidate bundles to resolve have requirements on
		// any deactivated candidate bundles
		for (BundleRevision deactivatedRev : deactivatedBundleRevisions) {
			Collection<BundleRevision> activatedReqs = BundleDependencies.getRequiringBundles(
					deactivatedRev, activatedBundleRevisons, null, new LinkedHashSet<BundleRevision>());
			if (activatedReqs.size() > 0) {
				if (null == deactivatedProviders) {
					deactivatedProviders = new LinkedHashSet<BundleRevision>();
				}
				// Inform others (build and job listeners) that this bundle project should be activated
				bundleTransition.addPending(deactivatedRev.getBundle(), Transition.ACTIVATE_PROJECT);
				deactivatedProviders.add(deactivatedRev);
			}
		}
		return null != deactivatedProviders ? deactivatedProviders : Collections
				.<BundleRevision> emptySet();
	}

	/**
	 * Set bundle and transition states to the state before resolve for the specified list of bundle
	 * revisions
	 * 
	 * @param notResolveList bundles to roll back
	 * @return the specified list to roll back or an empty list if the specified list is null
	 */
	@SuppressWarnings("unused")
	private Collection<BundleRevision> rollBackTransition(Collection<BundleRevision> notResolveList) {

		if (null == notResolveList) {
			return Collections.<BundleRevision> emptySet();
		}
		if (notResolveList.size() > 0) {
			for (BundleRevision notResolveRev : notResolveList) {
				Bundle notResloveBundle = notResolveRev.getBundle();
				BundleNode node = WorkspaceRegionImpl.INSTANCE.getBundleNode(notResloveBundle);
				node.rollBack();
			}
		}
		return notResolveList;
	}

	/**
	 * See also: The Resolve Operation Resolver Hook Service Specification Version 1.0, at page 316 in
	 * BundleJobManager Service Platform Release 4, Version 4.3
	 * 
	 */
	@Override
	public void filterSingletonCollisions(BundleCapability singleton,
			Collection<BundleCapability> collisionCandidates) {

		try {
			if (Category.DEBUG && Activator.getMessageOptionsService().isBundleOperations())
				TraceMessage.getInstance().getString("singleton_collisions",
						singleton.getRevision().getBundle().getSymbolicName(),
						formatBundleCapabilityList(collisionCandidates));
			if (null != groups) {
				Set<Bundle> group = groups.get(singleton.getRevision().getBundle());
				if (Category.DEBUG && Activator.getMessageOptionsService().isBundleOperations())
					TraceMessage.getInstance().getString("singleton_collisions_group",
							singleton.getRevision().getBundle().getSymbolicName(),
							WorkspaceRegionImpl.INSTANCE.formatBundleList(group, true));
				for (Iterator<BundleCapability> i = collisionCandidates.iterator(); i.hasNext();) {
					BundleCapability candidate = i.next();
					Bundle candidateBundle = candidate.getRevision().getBundle();
					Set<Bundle> otherGroup = groups.get(candidateBundle);
					if (Category.DEBUG && Activator.getMessageOptionsService().isBundleOperations())
						TraceMessage.getInstance().getString("singleton_collisions_other_group",
								candidateBundle.getSymbolicName(),
								WorkspaceRegionImpl.INSTANCE.formatBundleList(otherGroup, true));
					// If this singleton is in the group and at the same time is a candidate (other group)
					// Remove it so the same but new updated instance of the bundle can be resolved
					// Note this is opposite to the sample in the OSGI 4.3 specification
					if (group == otherGroup || otherGroup == null) // Same group
						i.remove(); // the duplicate
					if (Category.getState(Category.bundleEvents)) {
						TraceMessage.getInstance().getString("singleton_collision_remove_duplicate",
								candidateBundle.getSymbolicName(),
								WorkspaceRegionImpl.INSTANCE.formatBundleList(otherGroup, true));
					}
				}
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	@Override
	public void end() {
		groups = null;
	}

	/**
	 * Groups of duplicate singletons
	 * 
	 * @param groups the groups to set
	 */
	public final void setGroups(Map<Bundle, Set<Bundle>> groups) {
		this.groups = groups;
	}

	/**
	 * Groups of duplicate singletons
	 * 
	 * @return the singletons groups. May be null
	 */
	public final Map<Bundle, Set<Bundle>> getGroups() {
		return groups;
	}

	/**
	 * Constructs a comma separated string of symbolic names from a collection of bundles.
	 * 
	 * @param bundles to format
	 * @return a comma separated list of symbolic names
	 */
	public static String formatBundleRevisionList(Collection<BundleRevision> bundles) {
		StringBuffer sb = new StringBuffer();
		if (null != bundles && bundles.size() > 0) {
			for (BundleRevision b : bundles) {
				sb.append(b.getBundle().getSymbolicName());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}

	/**
	 * Constructs a comma separated string of symbolic names from a collection of bundles.
	 * 
	 * @param bundles to format
	 * @return a comma separated list of symbolic names
	 */
	public static String formatBundleCapabilityList(Collection<BundleCapability> bundles) {
		StringBuffer sb = new StringBuffer();
		if (null != bundles && bundles.size() > 0) {
			for (BundleCapability b : bundles) {
				sb.append(b.getRevision().getBundle().getSymbolicName());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}

}