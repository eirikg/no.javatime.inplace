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

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BundleDependencies;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.state.BundleStateFactory;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

/**
 * A bundle resolver that intercepts the resolve process to filter bundles to resolve and remove duplicate
 * bundles.
 * <p>
 * An instance of this resolver hook is returned by the resolver hook factory service for each resolve
 * process.
 * <p>
 * <b>Removing duplicates.</b>
 * <p>
 * Duplicates,in this context, may occur when a workspace bundle (or plug-in project) has been modified and
 * needs to be updated. If the workspace bundle to update is a singleton a collision occur with the current
 * revision of the bundle to update, when the bundle is resolved as part of the update process. By removing
 * the current resolved bundle from being resolved (the duplicate) a new current revision with the changed
 * code will be wired to the bundles that depends on it as part of the update (and resolve) process.
 * <p>
 * <b>Filtering bundles to resolve.</b>
 * <p>
 * In an activated workspace all bundles are at least in state INSTALLED, also those who are not activated
 * in-place. The OSGI resolver suggest to resolve all installed bundles. The filtering process excludes
 * installed not activated bundles from being resolved. Also, if one or more activated bundles have
 * requirements on deactivated bundles the activated bundles are removed from the candidate resolve list. The
 * deactivated bundles are then scheduled for activation and included in the set of bundles to resolve
 * together with the removed activated bundles during the activation process.
 * <p>
 * If a plug-in use extensions or implement extension-points it is required that the plug-in is a singleton.
 * 
 * @see BundleResolveHookFactory
 */
class BundleResolveHandler implements ResolverHook {

	// Groups of singletons
	private Map<Bundle, Set<Bundle>> groups = null;
	private Collection<Long> resolved = new LinkedHashSet<Long>();
	private BundleRegion bundleRegion = BundleManager.getRegion();
	private BundleTransition bundleTransition = BundleManager.getTransition();

	@Override
	public void filterMatches(BundleRequirement r, Collection<BundleCapability> candidates) {
	}

	/**
	 * Remove bundle closures for bundles with build errors, deactivated bundles and activated bundles dependent
	 * on deactivated bundles from the resolvable candidate list.
	 * <p>
	 * Deactivated bundles will in principle be excluded (removed) from the resolve candidate list. If an
	 * activated bundle is dependent on an installed (not activated) bundle, resolve of the activated bundle is
	 * delayed and an activation job is scheduled for the deactivated bundle project. Any further dependencies
	 * on the bundle project to activate is handled by the activation job.
	 */
	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {

		// Do not infer in a deactivated workspace
		if (!BundleProjectState.isWorkspaceNatureEnabled()) {
			return;
		}
		
		Collection<Bundle> bundles = BundleManager.getRegion().getBundles();
		if (bundles.size() == 0) {
			return;
		}
		Collection<BundleRevision> deactivatedBundles = new LinkedHashSet<BundleRevision>();
		Collection<BundleRevision> activatedBundles = new LinkedHashSet<BundleRevision>();
		// Restrict the scope to workspace bundles
		Collection<BundleRevision> workspaceCandidates = BundleDependencies
				.getRevisionsFrom(bundles);
		// Extract workspace candidate bundles from all candidate bundles
		workspaceCandidates.retainAll(candidates);

				
		// Split candidates in those activated and those deactivated
		for (BundleRevision workspaceCandidate : workspaceCandidates) {
			if (BundleManager.getRegion().isActivated(workspaceCandidate.getBundle())) {
				activatedBundles.add(workspaceCandidate);
			} else {
				deactivatedBundles.add(workspaceCandidate);
			}
		}
		// Remove bundle closure(s) with errors. Include all bundles
		// Activated bundles may have requirements on deactivated bundles with build errors
		Collection<BundleRevision> errorClosure = getErrorClosure(activatedBundles, deactivatedBundles);
		activatedBundles.removeAll(errorClosure);
		deactivatedBundles.removeAll(errorClosure);
		candidates.removeAll(errorClosure);
		
		// If no deactivated bundles, all error free bundles are activated and will be resolved
		if (!deactivatedBundles.isEmpty()) {
			candidates.removeAll(deactivatedBundles);
			// If no activated bundles, the candidate list should be empty
			// Note this is not among all workspace bundles, but the ones relevant for this resolve
			if (activatedBundles.isEmpty() && candidates.isEmpty()) {
				// traceResolved(errorClosure, deactivatedBundles, workspaceCandidates);
				return;
			}
		}
		// Delay resolve for bundles dependent on deactivated bundles until deactivated bundles are activated
		Collection<BundleRevision> dependentBundles = activateProvidingProjects(activatedBundles, deactivatedBundles);
		if (dependentBundles.size() > 0) {
			candidates.removeAll(dependentBundles);
		}
		// traceResolved(errorClosure, deactivatedBundles, workspaceCandidates);
		// Record bundles to resolve
		for (BundleRevision bundleRevision : candidates) {
			Long bundleId = bundleRevision.getBundle().getBundleId();
			resolved.add(bundleId);
		}
	}

	/**
	 * Filters out activated candidate bundles that has requirements on deactivated candidate bundles.
	 * <p>
	 * A pending transition for later activation is assigned to deactivated candidate bundles providing 
	 * capabilities to activated candidate bundles.
	 * .   
	 * 
	 * @param activatedRevs all activated candidate workspace bundles
	 * @param deactivatedRevs all deactivated candidate workspace bundles
	 * @return all activated bundles that requires capabilities from deactivated bundles or an empty collection
	 *         if no such requirements exist
	 */
	private Collection<BundleRevision> activateProvidingProjects (Collection<BundleRevision> activatedRevs,
			Collection<BundleRevision> deactivatedRevs) {
		
		// Activated bundles requiring capabilities from deactivated bundles
		Collection<BundleRevision> bundlesToNotResolve = new LinkedHashSet<BundleRevision>();

		// Does any bundles in the set of activated bundles to resolve have requirements on any deactivated candidate bundles
		for (BundleRevision deactivatedRev : deactivatedRevs) {
			// Note this is transitive. Activated bundles dependent on an activated bundle which is dependent 
			// on a deactivated bundle is included in the list of activated requiring bundle revisions
			Collection<BundleRevision> activatedReqs = 
					BundleDependencies.getRequiringBundles(deactivatedRev, activatedRevs, null, new LinkedHashSet<BundleRevision>());
			if (activatedReqs.size() > 0) {
				// Inform others that this bundle project should be activated
				bundleTransition.addPending(deactivatedRev.getBundle(), Transition.ACTIVATE_PROJECT);					
				bundlesToNotResolve.addAll(rollbackState(activatedReqs));
			}
		}
		return bundlesToNotResolve;
	}

	/**
	 * Get all bundles with build errors and their closures.
	 * 
	 * @param activatedBundles all activated workspace bundles
	 * @param deactivatedBundles all deactivated workspace bundles
	 * @return all bundles with errors and their closures
	 */
	private Collection<BundleRevision> getErrorClosure(Collection<BundleRevision> activatedBundles,
			Collection<BundleRevision> deactivatedBundles) {

		// Add activated bundles dependent on deactivated bundles with build errors to the not resolve list
		Collection<IProject> deactivatedProjects = BundleManager.getRegion().getBundleProjects(BundleDependencies
				.getBundlesFrom(deactivatedBundles));
		Collection<IProject> deactivatedErrorProjects = BuildErrorClosure.getBuildErrors(deactivatedProjects);
		deactivatedErrorProjects.addAll(BuildErrorClosure.hasBuildState(deactivatedProjects));
		Collection<BundleRevision> notResolveList = new LinkedHashSet<BundleRevision>();
		
		if (deactivatedErrorProjects.size() > 0) {
			for (IProject deactivatedErrorProject : deactivatedErrorProjects) {
				Bundle deactivatedErrorBundle = BundleManager.getRegion().get(deactivatedErrorProject);
				BundleRevision deactivatedErrorBundleRev = deactivatedErrorBundle.adapt(BundleRevision.class);
				try {
					notResolveList.addAll(BundleDependencies.getRequiringBundles(deactivatedErrorBundleRev,
							activatedBundles, null, new LinkedHashSet<BundleRevision>()));
				} catch (CircularReferenceException e) {
					if (null != e.getBundles()) {
						Collection<BundleRevision> deactivatedErrorBundlerevisions = BundleDependencies
								.getRevisionsFrom(e.getBundles());
						notResolveList.addAll(deactivatedErrorBundlerevisions);
					}
					String msg = ExceptionMessage.getInstance().formatString("circular_reference",
							BundleResolveHandler.class.getSimpleName());
					BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
					multiStatus.add(e.getStatusList());
					StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
				}
			}
			if (notResolveList.size() > 0) {
				String msg = NLS.bind(Msg.BUILD_ERROR_ON_DEACTIVATED_DEP_WARN, 
						BundleProjectState.formatProjectList(deactivatedErrorProjects),
						BundleManager.getRegion().formatBundleList(BundleDependencies.getBundlesFrom(notResolveList), true));
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		}
		// Add activated bundles dependent on other activated bundles with build errors to the not resolve list
		try {
			Collection<BundleRevision> newActivatedBundles = new LinkedHashSet<>(activatedBundles);
			newActivatedBundles.removeAll(notResolveList);
			Collection<IProject> activatedProjects = BundleManager.getRegion().getBundleProjects(BundleDependencies
					.getBundlesFrom(newActivatedBundles));
			BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.RESOLVE);
			if (be.hasBuildErrors()) {
				Collection<Bundle> buildErrClosure = be.getBundleErrorClosures();
				notResolveList.addAll(BundleDependencies.getRevisionsFrom(buildErrClosure));
				if (Activator.getDefault().msgOpt().isBundleOperations()) {
					IBundleStatus bundleStatus = be.getProjectErrorClosureStatus();
					if (null != bundleStatus) {
						StatusManager.getManager().handle(bundleStatus, StatusManager.LOG);
					}
				}
			}		
		} catch (CircularReferenceException e) {
			if (null != e.getBundles()) {
				Collection<BundleRevision> activatedErrorBundlerevisions = BundleDependencies.getRevisionsFrom(e
						.getBundles());
				notResolveList.addAll(activatedErrorBundlerevisions);
			}
			String msg = ExceptionMessage.getInstance().formatString("circular_reference",
					BundleResolveHandler.class.getSimpleName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		}
		return rollbackState(notResolveList);
	}

	/**
	 * Set bundle and transition states to installed for the specified list of bundle revisions
	 * 
	 * @param notResolveList bundles to roll back to state installed
	 * @return the specified list to revert or an empty list if the specified list is null
	 */
	private Collection<BundleRevision> rollbackState(Collection<BundleRevision> notResolveList) {

		if (null == notResolveList) {
			return Collections.<BundleRevision>emptySet();
		}
		if (notResolveList.size() > 0) {
			for (BundleRevision notResolveRev : notResolveList) {
				Bundle notResloveBundle = notResolveRev.getBundle();
				bundleTransition.setTransition(bundleRegion.getRegisteredBundleProject(notResloveBundle), Transition.INSTALL);
				BundleWorkspaceRegionImpl.INSTANCE.setActiveState(notResloveBundle, BundleStateFactory.INSTANCE.installedState);
			}
		}
		return notResolveList;
	}
	/**
	 * See also: The Resolve Operation Resolver Hook Service Specification Version 1.0, at page 316 in BundleJobManager
	 * Service Platform Release 4, Version 4.3
	 * 
	 */
	@Override
	public void filterSingletonCollisions(BundleCapability singleton,
			Collection<BundleCapability> collisionCandidates) {
		if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
			TraceMessage.getInstance().getString("singleton_collisions",
					singleton.getRevision().getBundle().getSymbolicName(),
					formatBundleCapabilityList(collisionCandidates));
		if (null != groups) {
			Set<Bundle> group = groups.get(singleton.getRevision().getBundle());
			if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
				TraceMessage.getInstance().getString("singleton_collisions_group",
						singleton.getRevision().getBundle().getSymbolicName(),
						BundleManager.getRegion().formatBundleList(group, true));
			for (Iterator<BundleCapability> i = collisionCandidates.iterator(); i.hasNext();) {
				BundleCapability candidate = i.next();
				Bundle candidateBundle = candidate.getRevision().getBundle();
				Set<Bundle> otherGroup = groups.get(candidateBundle);
				if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
					TraceMessage.getInstance().getString("singleton_collisions_other_group",
							candidateBundle.getSymbolicName(), BundleManager.getRegion().formatBundleList(otherGroup, true));
				// If this singleton is in the group and at the same time is a candidate (other group)
				// Remove it so the same but new updated instance of the bundle can be resolved
				// Note this is opposite to the sample in the OSGI 4.3 specification
				if (group == otherGroup || otherGroup == null) // Same group
					i.remove(); // the duplicate
				if (Category.getState(Category.bundleEvents)) {
					TraceMessage.getInstance().getString("singleton_collision_remove_duplicate",
							candidateBundle.getSymbolicName(), BundleManager.getRegion().formatBundleList(otherGroup, true));
				}
			}
		}
	}

	private void traceResolved(Collection<BundleRevision> errorClosure, Collection<BundleRevision> rejected,
			Collection<BundleRevision> workspaceCandidates) {
		if (!Activator.getDefault().msgOpt().isBundleOperations()) {
			return;
		}
		if (workspaceCandidates.size() > 0) {
			Collection<BundleRevision> toResolve = new LinkedHashSet<BundleRevision>(workspaceCandidates);
			toResolve.removeAll(rejected);
			toResolve.removeAll(errorClosure);
			if (toResolve.size() > 0) {
				for (BundleRevision bundleRevision : toResolve) {
					TraceMessage.getInstance().getString("bundles_to_resolve",
							BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
				}
			}
		}
		if (rejected.size() > 0 && Category.getState(Category.dag)) {
			for (BundleRevision bundleRevision : rejected) {
				TraceMessage.getInstance().getString("rejected_bundles_to_not_resolve",
						BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
			}
		}
		if (errorClosure.size() > 0) {
			for (BundleRevision bundleRevision : errorClosure) {
				TraceMessage.getInstance().getString("error_bundles_to_not_resolve",
						BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
			}
		}
	}

	@Override
	public void end() {
		BundleRegion bundleRegion = BundleManager.getRegion();
		for (Long bundleId : resolved) {
			Bundle bundle = bundleRegion.get(bundleId);	
			if (null != bundle) {
				BundleManager.addBundleTransition(new TransitionEvent(bundle, Transition.RESOLVE));
			}
		}
		resolved.clear();
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