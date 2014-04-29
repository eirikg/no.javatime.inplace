package no.javatime.inplace.dependencies;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.util.messages.Category;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * Utility members that performs a topological sort of projects and bundles according to a set of dependency rules. 
 * There is a total of 16 options divided among projects activation (3) and deactivation (3) and bundle activation (5) 
 * and deactivation (5).
 * <p>
 * In general there are 6 different types of options; - requiring; - providing; - partial graph; - requiring and providing;
 * - providing and requiring; and - single.
 * <p>
 * For project activation providing is mandatory and and default. In addition it is possible to include requiring projects 
 * or include the partial graph calculated from initial set of bundles to return.  
 * <p>
 * For project deactivation requiring is mandatory and default. In addition it is possible to include providing projects or include the 
 * partial graph calculated from the initial set of bundles to return.
 * <p>
 * For bundle activation the default is providing. The options are providing, requiring, requiring and providing, partial graph
 * or single. In the case of single the bundles are sorted and no additional bundles are included in the result set.  
 * <p>
 * For bundle deactivation the default is requiring. The options are requiring, providing, providing and requiring, partial graph
 * or single.   
 * <p>
 */
public class PartialDependencies {
	
	/**
	 * Topological sort of projects according to dependency options. Providing projects to the specified initial set 
	 * are always included. In addition requiring projects are included if the requiring on activate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, Collection, boolean)}. If both requiring on activate
	 * and partial graph is true, partial graph is used.
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> projectActivationDependencies(Collection<IProject> initialSet, boolean activated) {

		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> resultSet = null;

		if (null != initialSet && initialSet.size() > 0) {
			if (Category.getState(Category.partialGraphOnActivate)) {
				// Requiring and then providing graph
				resultSet = partialGraph(initialSet, activated, true);
			} else if (Category.getState(Category.requiringOnActivate)) {
				// Requiring and then providing
				resultSet = ps.sortRequiringProjects(initialSet, activated);
				resultSet = ps.sortProvidingProjects(resultSet, activated);
			} else {
				// Sort projects in dependency order when providing option (default)is set
				resultSet = ps.sortProvidingProjects(initialSet, activated);
			}
		} else {
			return Collections.emptySet();
		}	
		return resultSet;
	}
	
	/**
	 * Topological sort of projects according to dependency options. Requiring projects to the specified initial set 
	 * are always included. In addition providing projects are included if the provide on deactivate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, Collection, boolean)}. If both providing on deactivate
	 * and partial graph is true, partial graph is used.
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> projectDeactivationDependencies(Collection<IProject> initialSet, boolean activated) throws CircularReferenceException {
		
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> resultSet = null;

		if (null != initialSet && initialSet.size() > 0) {
			ps.setAllowCycles(true);
			if (Category.getState(Category.partialGraphOnDeactivate)) {
				// Providing and requiring graph
				resultSet = partialGraph(initialSet, activated, false);
			} else if (Category.getState(Category.providingOnDeactivate)) {
				// Providing and then requiring
				resultSet = ps.sortProvidingProjects(initialSet, activated);
				resultSet = ps.sortRequiringProjects(resultSet, activated);
			} else {
				// Sort projects in dependency order when requiring option (default) is set
				resultSet = ps.sortRequiringProjects(initialSet, activated);
			}
		} else {
			return Collections.emptySet();
		}
		return resultSet;
	}
	/**
	 * Topological sort of bundles according to dependency options. The options are providing (default), requiring, requiring and providing,
	 * partial graph and single. 
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace
	 * @return set of sorted bundles according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> bundleActivationDependencies(Collection<Bundle> initialSet, Collection<Bundle> scope) throws CircularReferenceException {

		BundleSorter bs = new BundleSorter();
		Collection<Bundle> resultSet = null;
		if (null != initialSet && initialSet.size() > 0 && null != scope) {
			if (Category.getState(Category.partialGraphOnStart)) {
				// Requiring and then providing graph					
				resultSet = partialGraph(initialSet, scope, true);
				// When both requiring and providing is enabled this is the same as
				// the requiring and providing option
			} else if (Category.getState(Category.requiringOnStart) && Category.getState(Category.providingOnStart)) {
				resultSet = bs.sortRequiringBundles(initialSet, scope);
				resultSet = bs.sortProvidingBundles(resultSet, scope);
			} else if (Category.getState(Category.providingOnStart)) {
				resultSet = bs.sortProvidingBundles(initialSet, scope);
			} else if (Category.getState(Category.requiringOnStart)) {
				resultSet = bs.sortRequiringBundles(initialSet, scope);
			} else {
				// No options set (or same as single). Only sort the bundles
				resultSet = bs.sortProvidingBundles(initialSet, initialSet);
			}
		} else {
			return Collections.emptySet();
		}
		return resultSet;
	}

	/**
	 * Topological sort of bundles according to dependency options. The options are requiring (default), providing, providing and requiring,
	 * partial graph and single. 
	 * 
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace
	 * @return set of sorted bundles according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> bundleDeactivationDependencies(Collection<Bundle> initialSet, Collection<Bundle> scope) throws CircularReferenceException {

		BundleSorter bs = new BundleSorter();
		Collection<Bundle> resultSet = null;
		if (null != initialSet && initialSet.size() > 0 && null != scope) {
			if (Category.getState(Category.partialGraphOnStop)) {
				// Providing and then requiring graph					
				resultSet = partialGraph(initialSet, scope, false);
				// When both providing and requiring is enabled this is the same as
				// the providing and requiring option
			} else if (Category.getState(Category.providingOnStop) && Category.getState(Category.requiringOnStop)) {
				resultSet = bs.sortProvidingBundles(initialSet, scope);
				resultSet = bs.sortRequiringBundles(resultSet, scope);
			} else if (Category.getState(Category.requiringOnStop)) {
				resultSet = bs.sortRequiringBundles(initialSet, scope);
			} else if (Category.getState(Category.providingOnStop)) {
				resultSet = bs.sortProvidingBundles(initialSet, scope);
			} else {
				// No options set (or same as single). Only sort the bundles
				resultSet = bs.sortRequiringBundles(initialSet, initialSet);
			}
		} else {
			return Collections.emptySet();
		}
		return resultSet;
	}	
	/**
	 * Topological sort of all projects that are directly or indirectly reachable through dependencies 
	 * from an initial set of projects until it is not possible to extend the directed graph further
	 * 
	 * @param initialSet the set of start projects to include in the topological sort 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @param requiring sort in requiring order and then in providing order if true and in opposite order if false
	 * @return the result graph of the topological sort 
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> partialGraph(Collection<IProject> initialSet, boolean activated, boolean requiring) throws CircularReferenceException {
		
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> resultSet = new LinkedHashSet<IProject>(initialSet);
		int count = 0;
		do {
			count = resultSet.size();
			if (requiring) {
				resultSet = ps.sortRequiringProjects(resultSet, activated);
				resultSet = ps.sortProvidingProjects(resultSet, activated);
			} else {
				resultSet = ps.sortProvidingProjects(resultSet, activated);
				resultSet = ps.sortRequiringProjects(resultSet, activated);				
			}
		} while (resultSet.size() > count);		
		return resultSet;
	}

	/**
	 * Topological sort of all bundles that are directly or indirectly reachable through dependencies 
	 * from an initial set of bundles until it is not possible to extend the directed graph further
	 * 
	 * @param initialSet the set of start projects to include in the topological sort 
	 * @param scope the set of bundles to consider as candidates in the result graph
	 * @param requiring sort in requiring order and then in providing order if true and in opposite order if false
	 * @return the result graph of the topological sort 
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	public Collection<Bundle> partialGraph(Collection<Bundle> initialSet, Collection<Bundle> scope, boolean requiring) throws CircularReferenceException {
		
		BundleSorter bs = new BundleSorter();
		Collection<Bundle> resultSet = new LinkedHashSet<Bundle>(initialSet);
		int count = 0;
		do {
			count = resultSet.size();
			if (requiring) {
				resultSet = bs.sortRequiringBundles(resultSet, scope);
				resultSet = bs.sortProvidingBundles(resultSet, scope);
			} else {
				resultSet = bs.sortProvidingBundles(resultSet, scope);
				resultSet = bs.sortRequiringBundles(resultSet, scope);				
			}
		} while (resultSet.size() > count);		
		return resultSet;
	}
}
