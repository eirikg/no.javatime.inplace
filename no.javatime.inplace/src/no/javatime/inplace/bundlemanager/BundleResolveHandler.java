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
package no.javatime.inplace.bundlemanager;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.state.BundleStateFactory;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.BundleDependencies;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.extender.status.BundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
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

	@Override
	public void filterMatches(BundleRequirement r, Collection<BundleCapability> candidates) {
		return;
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
		if (!ProjectProperties.isProjectWorkspaceActivated()) {
			return;
		}
		
		Collection<BundleRevision> deactivatedBundles = new LinkedHashSet<BundleRevision>();
		Collection<BundleRevision> activatedBundles = new LinkedHashSet<BundleRevision>();
		Collection<Bundle> bundles = BundleManager.getRegion().getBundles();
		if (bundles.size() == 0) {
			return;
		}
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
		if (deactivatedBundles.isEmpty()) {
			traceResolved(errorClosure, deactivatedBundles, workspaceCandidates);
			return;
		}
		candidates.removeAll(deactivatedBundles);
		// If no activated bundles, the candidate list should be empty
		// Note this is not among all workspace bundles, but the ones relevant for this resolve
		if (activatedBundles.isEmpty()) {
			traceResolved(errorClosure, deactivatedBundles, workspaceCandidates);
			return;
		}
		// Delay resolve for bundles dependent on deactivated bundles until deactivated bundles are activated
		Collection<BundleRevision> dependentBundles = activateDependentBundles(activatedBundles, deactivatedBundles);
		if (dependentBundles.size() > 0) {
			candidates.removeAll(dependentBundles);
		}
	}

	/**
	 * Schedule an activate project job for deactivated bundles providing capabilities to activated bundles.
	 * 
	 * @param activatedBundleRevisions all activated workspace bundles
	 * @param deactivatedBundleRevisions all deactivated workspace bundles
	 * @return all activated bundles that requires capabilities from deactivated bundles or an empty collection
	 *         if no such requirements exist
	 */
	public Collection<BundleRevision> activateDependentBundles(Collection<BundleRevision> activatedBundleRevisions,
			Collection<BundleRevision> deactivatedBundleRevisions) {

		Collection<IProject> projectsToActivate = null;
		Collection<BundleRevision> bundlesToNotResolve = new LinkedHashSet<BundleRevision>();

		for (BundleRevision deactivatedBundleRevision : deactivatedBundleRevisions) {
			// Does the set of activated bundles have requirements on this non
			// activated (installed) candidate bundle
			Collection<BundleRevision> activatedRequireres = BundleDependencies.getRequiringBundles(
					deactivatedBundleRevision, activatedBundleRevisions, null, new LinkedHashSet<BundleRevision>());
			if (!activatedRequireres.isEmpty()) {
				BundleRegion bundleRegion = BundleManager.getRegion();
				for (BundleRevision activatedRequirer : activatedRequireres) {
					Bundle activatedRequirerBundle = activatedRequirer.getBundle();
					// Tag as not resolved and adjust state
					BundleTransition transition = BundleManager.getTransition();
					transition.setTransition(bundleRegion.getProject(activatedRequirerBundle), Transition.INSTALL);
					BundleWorkspaceImpl.INSTANCE.setActiveState(activatedRequirerBundle, BundleStateFactory.INSTANCE.installedState);
				}
				if (null == projectsToActivate) {
					projectsToActivate = new LinkedHashSet<IProject>();
				}
				projectsToActivate.add(bundleRegion.getProject(deactivatedBundleRevision.getBundle()));
				bundlesToNotResolve.addAll(activatedRequireres);
				if (Category.getState(Category.infoMessages)) {
					Collection<IProject> activatedProjects = bundleRegion.getProjects(BundleDependencies
							.getBundlesFrom(activatedRequireres));
					IProject deactivatedProject = bundleRegion.getProject(deactivatedBundleRevision
							.getBundle());
					UserMessage.getInstance().getString("implicit_activation",
							ProjectProperties.formatProjectList(activatedProjects), deactivatedProject.getName());
					UserMessage.getInstance().getString("delayed_resolve",
							ProjectProperties.formatProjectList(activatedProjects), deactivatedProject.getName());
				}
			}
		}
		if (null != projectsToActivate) {
			ActivateProjectJob activateProjectJob = new ActivateProjectJob(
					ActivateProjectJob.activateNatureJobName, projectsToActivate);
			BundleManager.addBundleJob(activateProjectJob, 0);
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
	public Collection<BundleRevision> getErrorClosure(Collection<BundleRevision> activatedBundles,
			Collection<BundleRevision> deactivatedBundles) {

		// Add activated bundles dependent on deactivated bundles with build errors to the not resolve list
		ProjectSorter bs = new ProjectSorter();
		Collection<IProject> deactivatedProjects = BundleManager.getRegion().getProjects(BundleDependencies
				.getBundlesFrom(deactivatedBundles));
		Collection<IProject> deactivatedErrorProjects = ProjectProperties.getBuildErrors(deactivatedProjects);
		deactivatedErrorProjects.addAll(ProjectProperties.hasBuildState(deactivatedProjects));
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
					BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
					multiStatus.add(e.getStatusList());
					StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
				}
			}
			if (notResolveList.size() > 0) {
				String msg = WarnMessage.getInstance().formatString("build_error_on_deactivated_dependencies",
						ProjectProperties.formatProjectList(deactivatedErrorProjects),
						BundleManager.getRegion().formatBundleList(BundleDependencies.getBundlesFrom(notResolveList), true));
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		}
		// Add activated bundles dependent on other activated bundles with build errors to the not resolve list
		try {
			Collection<IProject> projectScope = BundleManager.getRegion().getProjects(BundleDependencies
					.getBundlesFrom(activatedBundles));
			Collection<IProject> projectErrorClosure = bs.getRequiringBuildErrorClosure(projectScope, true);
			if (projectErrorClosure.size() > 0) {
				String msg = ProjectProperties.formatBuildErrorsFromClosure(projectErrorClosure, "Resolve");
				if (null != msg) {
					String warnMsg = WarnMessage.getInstance().formatString(WarnMessage.defKey, msg);
					StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, warnMsg),
							StatusManager.LOG);
				}
				Collection<Bundle> bundles = BundleManager.getRegion().getBundles(projectErrorClosure);
				notResolveList.addAll(BundleDependencies.getRevisionsFrom(bundles));
			}
		} catch (CircularReferenceException e) {
			if (null != e.getBundles()) {
				Collection<BundleRevision> activatedErrorBundlerevisions = BundleDependencies.getRevisionsFrom(e
						.getBundles());
				notResolveList.addAll(activatedErrorBundlerevisions);
			}
			String msg = ExceptionMessage.getInstance().formatString("circular_reference",
					BundleResolveHandler.class.getSimpleName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		}
		return notResolveList;
	}

	/**
	 * See also: The Resolve Operation Resolver Hook Service Specification Version 1.0, at page 316 in BundleManager
	 * Service Platform Release 4, Version 4.3
	 * 
	 */
	@Override
	public void filterSingletonCollisions(BundleCapability singleton,
			Collection<BundleCapability> collisionCandidates) {
		if (Category.DEBUG && InPlace.get().msgOpt().isBundleOperations())
			InPlace.get().trace("singleton_collisions",
					singleton.getRevision().getBundle().getSymbolicName(),
					formatBundleCapabilityList(collisionCandidates));
		if (null != groups) {
			Set<Bundle> group = groups.get(singleton.getRevision().getBundle());
			if (Category.DEBUG && InPlace.get().msgOpt().isBundleOperations())
				InPlace.get().trace("singleton_collisions_group",
						singleton.getRevision().getBundle().getSymbolicName(),
						BundleManager.getRegion().formatBundleList(group, true));
			for (Iterator<BundleCapability> i = collisionCandidates.iterator(); i.hasNext();) {
				BundleCapability candidate = i.next();
				Bundle candidateBundle = candidate.getRevision().getBundle();
				Set<Bundle> otherGroup = groups.get(candidateBundle);
				if (Category.DEBUG && InPlace.get().msgOpt().isBundleOperations())
					InPlace.get().trace("singleton_collisions_other_group",
							candidateBundle.getSymbolicName(), BundleManager.getRegion().formatBundleList(otherGroup, true));
				// If this singleton is in the group and at the same time is a candidate (other group)
				// Remove it so the same but new updated instance of the bundle can be resolved
				// Note this is opposite to the sample in the OSGI 4.3 specification
				if (group == otherGroup || otherGroup == null) // Same group
					i.remove(); // the duplicate
				if (Category.getState(Category.bundleEvents)) {
					InPlace.get().trace("singleton_collision_remove_duplicate",
							candidateBundle.getSymbolicName(), BundleManager.getRegion().formatBundleList(otherGroup, true));
				}
			}
		}
	}

	private void traceResolved(Collection<BundleRevision> errorClosure, Collection<BundleRevision> rejected,
			Collection<BundleRevision> workspaceCandidates) {
		if (!InPlace.get().msgOpt().isBundleOperations()) {
			return;
		}
		if (workspaceCandidates.size() > 0) {
			Collection<BundleRevision> toResolve = new LinkedHashSet<BundleRevision>(workspaceCandidates);
			toResolve.removeAll(rejected);
			toResolve.removeAll(errorClosure);
			if (toResolve.size() > 0) {
				for (BundleRevision bundleRevision : toResolve) {
					InPlace.get().trace("bundles_to_resolve",
							BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
				}
			}
		}
		if (rejected.size() > 0 && Category.getState(Category.dag)) {
			for (BundleRevision bundleRevision : rejected) {
				InPlace.get().trace("rejected_bundles_to_not_resolve",
						BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
			}
		}
		if (errorClosure.size() > 0) {
			for (BundleRevision bundleRevision : errorClosure) {
				InPlace.get().trace("error_bundles_to_not_resolve",
						BundleManager.getRegion().getSymbolicKey(bundleRevision.getBundle(), null));
			}
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
	 * Constructs a string of symbolic names based on a collection of bundles.
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