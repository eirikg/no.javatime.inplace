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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.project.BundleProjectImpl;
import no.javatime.inplace.region.project.BundleProjectStateImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;

/**
 * Topological sort of projects in requiring (referencing) and providing (referenced) project dependency order.
 * <p>
 * All cycles from an initial set (start projects) of projects are detected for each sort. To detect all cycles in the
 * workspace include all workspace projects as the initial set to sort.
 * 
 * @see CircularReferenceException
 * @see BundleSorter
 */
public class ProjectSorter extends BaseSorter {

	/**
	 * Sorted collection of projects
	 */
	private Collection<IProject> projectOrder = null;

	/**
	 * Default constructor
	 */
	public ProjectSorter() {
	}

	/**
	 * The order is according to the last sort (requiring or providing order)
	 * 
	 * @return an ordered collection of projects or an empty collection if no sort has been performed
	 */
	public Collection<IProject> getProjectOrder() {
		if (null == projectOrder) {
			return Collections.<IProject>emptySet();
		}
		return projectOrder;
	}

	/**
	 * Topological sort in referenced project order among all valid workspace projects. 
	 * The initial set of specified projects are included in the result set.
	 * 
	 * @param projects a collection of start projects included in the result set
	 * @return collection of projects in referenced sort order
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> sortRequiringProjects(final Collection<IProject> projects)
			throws CircularReferenceException {
		projectOrder = new LinkedHashSet<IProject>();
		circularException = null;
		for (final IProject project : projects) {
			visitRequiringProject(project, null, new LinkedHashSet<IProject>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return projectOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial project. The graph is in dependency order and
	 * added to the internal stored project order which is accessible from {@linkplain ProjectSorter#getProjectOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial providing project
	 * @param parent a requiring project to the {@code child} project parameter. May be null.
	 * @param visited is a list of projects in the current call stack (used for detecting cycles)
	 */
	protected void visitRequiringProject(final IProject child, final IProject parent, final Collection<IProject> visited) {

		// Has this start bundle element been visited before (not through recursion)
		if (!projectOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleProjectCycle(child, parent);
				return;
			}
			visited.add(child);
			Collection<IProject> requirers = ProjectDependencies.getRequiringProjects(child);
			for (IProject requirer : requirers) {
				visitRequiringProject(requirer, child, visited);
			}
			projectOrder.add(child);
		}
	}

	/**
	 * Topological sort in referenced project order, using depth-first search. For all workspace projects use
	 * {@linkplain #sortRequiringProjects(Collection)} Initial set of specified projects are included in the result set.
	 * 
	 * @param projects a collection of start projects included in the result set
	 * @param natureEnabled if true only consider activated projects (nature enabled) in the result set or if false only
	 *          consider projects that are not activated
	 * @return collection of projects in referenced sort order
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> sortRequiringProjects(final Collection<IProject> projects, final Boolean natureEnabled)
			throws CircularReferenceException {
		projectOrder = new LinkedHashSet<IProject>();
		circularException = null;
		for (final IProject project : projects) {
			visitRequiringProject(project, null, natureEnabled, new LinkedHashSet<IProject>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return projectOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial project. The graph is in dependency order and
	 * added to the internal stored project order which is accessible from {@linkplain ProjectSorter#getProjectOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial providing project
	 * @param parent a requiring project to the {@code child} project parameter. May be null.
	 * @param natureEnabled if true only consider activated projects and only deactivated projects if false
	 * @param visited is a list of projects in the current call stack (used for detecting cycles)
	 */
	protected void visitRequiringProject(final IProject child, final IProject parent, final Boolean natureEnabled,
			final Collection<IProject> visited) {

		// Has this start bundle element been visited before (not through recursion)
		if (!projectOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleProjectCycle(child, parent);
				return;
			}
			visited.add(child);
			Collection<IProject> requirers = ProjectDependencies.getRequiringProjects(child);
			for (IProject requirer : requirers) {
				if (natureEnabled.equals(BundleProjectImpl.INSTANCE.isNatureEnabled(requirer))) {
					visitRequiringProject(requirer, child, natureEnabled, visited);
				}
			}
			projectOrder.add(child);
		}
	}

	/**
	 * Topological sort in referencing project order among all valid workspace projects. Initial set of specified projects
	 * are included in the result set.
	 * 
	 * @param projects a collection of start bundles included in the result set
	 * @return collection of projects in referencing sort order
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	public Collection<IProject> sortProvidingProjects(final Collection<IProject> projects)
			throws CircularReferenceException, InPlaceException {
		projectOrder = new LinkedHashSet<IProject>();
		circularException = null;
		for (final IProject project : projects) {
			visitProvidingProject(project, null, new LinkedHashSet<IProject>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return projectOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial project. The graph is in dependency order and
	 * added to the internal stored project order which is accessible from {@linkplain ProjectSorter#getProjectOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial requiring project
	 * @param parent a providing project to the {@code child} project parameter. May be null.
	 * @param visited is a list of projects in the current call stack (used for detecting cycles)
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	protected void visitProvidingProject(final IProject child, final IProject parent, final Collection<IProject> visited) 
			throws InPlaceException {

		// Has this start bundle element been visited before (not through recursion)
		if (!projectOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleProjectCycle(child, parent);
				return;
			}
			visited.add(child); // Overlook self providing
			Collection<IProject> providers = ProjectDependencies.getProvidingProjects(child);
			for (IProject provider : providers) {
				visitProvidingProject(provider, child, visited);
			}
			projectOrder.add(child);
		}
	}

	/**
	 * Topological sort in referencing project order among all valid workspace projects For all workspace projects use
	 * {@linkplain #sortProvidingProjects(Collection)} Initial set of specified projects are included in the result set.
	 * 
	 * @param projects a collection of start bundles included in the result set
	 * @param natureEnabled if true only consider activated projects (nature enabled) in the result set or if false only
	 *          consider projects that are not activated
	 * @return collection of projects in referencing sort order
	 * @throws CircularReferenceException if cycles are detected in the project graph. All Cycles are detected.
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	public Collection<IProject> sortProvidingProjects(final Collection<IProject> projects, final Boolean natureEnabled)
			throws CircularReferenceException, InPlaceException {
		projectOrder = new LinkedHashSet<IProject>();
		circularException = null;
		for (final IProject project : projects) {
			visitProvidingProject(project, null, natureEnabled, new LinkedHashSet<IProject>());
		}
		if (null != circularException) {
			throw circularException;
		}
		return projectOrder;
	}

	/**
	 * Traverse the partial graph of the specified child as the initial project. The graph is in dependency order and
	 * added to the internal stored project order which is accessible from {@linkplain ProjectSorter#getProjectOrder()}.
	 * Any cycles in the graph are stored in {@linkplain CircularReferenceException}
	 * 
	 * @param child the initial requiring project
	 * @param parent a providing project to the {@code child} project parameter. May be null.
	 * @param natureEnabled if true only consider activated projects and only deactivated projects if false
	 * @param visited is a list of projects in the current call stack (used for detecting cycles)
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	protected void visitProvidingProject(final IProject child, final IProject parent, final Boolean natureEnabled,
			final Collection<IProject> visited) throws InPlaceException {

		// Has this start bundle element been visited before (not through recursion)
		if (!projectOrder.contains(child)) {
			// If visited before during this nested sequence of recursive calls, it's a cycle
			if (visited.contains(child)) {
				handleProjectCycle(child, parent);
				return;
			}
			visited.add(child); // Overlook self providing
			Collection<IProject> providers = ProjectDependencies.getProvidingProjects(child);
			for (IProject provider : providers) {
				if (natureEnabled.equals(BundleProjectImpl.INSTANCE.isNatureEnabled(provider))) {
					visitProvidingProject(provider, child, natureEnabled, visited);
				}
			}
			projectOrder.add(child);
		}
	}

	/**
	 * Adds projects included in the cycle and a status message describing the cycle to the
	 * {@linkplain CircularReferenceException}.
	 * 
	 * @param child project that refers to the parent project parameter
	 * @param parent project that refers to the child project parameter
	 */
	protected void handleProjectCycle(final IProject child, final IProject parent) {
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
		// Hosts can import packages from fragment (no complaints from PDE),
		// even if fragment is an inherent part of the host. Is this a kind of self reference?
		// Must check both parent and child, due to traversal order (providing or requiring)
		if (!getAllowCycles() && (!BundleProjectImpl.INSTANCE.isFragment(child) && !BundleProjectImpl.INSTANCE.isFragment(parent))) {
			ProjectSorter ps = new ProjectSorter();
			ps.setAllowCycles(true);
			Collection<IProject> projects = ps.sortRequiringProjects(Collections.<IProject>singletonList(parent));
			projects.addAll(ps.sortRequiringProjects(Collections.<IProject>singletonList(child)));
			BundleTransitionImpl.INSTANCE.setTransitionError(parent, TransitionError.CYCLE);
			BundleTransitionImpl.INSTANCE.setTransitionError(child, TransitionError.CYCLE);
			if (null == circularException) {
				circularException = new CircularReferenceException();
			}
			String msg = ExceptionMessage.getInstance().formatString("affected_bundles",
					BundleProjectImpl.INSTANCE.formatProjectList(projects));
			circularException.addToStatusList(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg, null));
			if (directRecursion) {
				msg = ExceptionMessage.getInstance().formatString("direct_circular_reference_with_bundles",
						parent.getName());
			} else {
				msg = ExceptionMessage.getInstance().formatString("circular_reference_with_bundles",
						parent.getName(), child.getName());
			}
			circularException.addToStatusList(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, null));
			circularException.addProjects(projects);
		}
	}

	/**
	 * Prints the set of projects to {@code System.out} in referenced order
	 */
	@SuppressWarnings("unused")
	private void printProjectOrder() {
		if (null == projectOrder) {
			return;
		}
		System.out.println("Project topological Order " + BundleProjectImpl.INSTANCE.formatProjectList(projectOrder));
	}
}
