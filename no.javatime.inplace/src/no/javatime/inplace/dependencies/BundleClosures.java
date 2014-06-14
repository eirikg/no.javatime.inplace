package no.javatime.inplace.dependencies;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.manager.InPlaceException;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * Topological sort of projects and bundles according to a set of dependency closures. 
 * There is a total of 16 options named closures divided among project activation (3) and deactivation (3) and bundle activation (5) 
 * and deactivation (5).
 * <p>
 * In general there are 6 different types of options; - requiring; - providing; - partial graph; - requiring and providing;
 * - providing and requiring; and - single.
 * <p>
 * For project activation providing is mandatory and default. In addition it is possible to include requiring projects 
 * and include the partial graph calculated from an initial set of bundles.  
 * <p>
 * For project deactivation requiring is mandatory and default. In addition it is possible to include providing projects or include the 
 * partial graph calculated from the initial set of bundles.
 * <p>
 * For bundle activation the default is providing. The options are providing, requiring, requiring and providing, partial graph
 * and single. In the case of single the bundles are sorted and no additional bundles are included in the result set.  
 * <p>
 * For bundle deactivation the default is requiring. The options are requiring, providing, providing and requiring, partial graph
 * and single.   
 * <p>
 */
public class BundleClosures {

	
	/**
	 * Topological sort of projects according to the current dependency option (closure). Providing projects to the specified initial set 
	 * are always included. In addition requiring projects are included if the requiring on activate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, boolean, boolean).
	 * 
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<IProject> projectActivation(Collection<IProject> initialSet, boolean activated) 
			throws CircularReferenceException, InPlaceException {
		
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Closure closure = opt.get(Operation.ACTIVATE_PROJECT);
		return projectActivation(closure, initialSet, activated);
	}

	/**
	 * Topological sort of projects according to the specified dependency option (closure). Providing projects to the specified initial set 
	 * are always included. In addition requiring projects are included if the requiring on activate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, boolean, boolean)
	 * 
	 * @param closure sort according to closure. Valid closures are providing, requiring and providing and partial graph. An empty set is
	 * returned if closure is null.
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<IProject> projectActivation(Closure closure, Collection<IProject> initialSet, boolean activated) 
			throws CircularReferenceException, InPlaceException {
		
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> resultSet = null;		
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		
		if (null != closure && null != initialSet && initialSet.size() > 0) { 
			if (!opt.isAllowed(Operation.ACTIVATE_PROJECT, closure) ) {
				throw new InPlaceException("illegal_closure_exception", closure.name(), Operation.ACTIVATE_PROJECT.name());
			}
			switch (closure) {
			case PROVIDING:
				// Sort projects in dependency order when providing option (default)is set
				resultSet = ps.sortProvidingProjects(initialSet, activated);
				break;
			case REQUIRING_AND_PROVIDING:
				resultSet = ps.sortRequiringProjects(initialSet, activated);
				resultSet = ps.sortProvidingProjects(resultSet, activated);
				break;
			case PARTIAL_GRAPH:
				resultSet = partialGraph(initialSet, activated, true);
				break;
			default:
				resultSet = Collections.<IProject>emptySet();
				break;
			}
		} else {
			resultSet = Collections.<IProject>emptySet();			
		}
		return resultSet;
	}
	
	/**
	 * Topological sort of projects according to the current dependency option (closure). Requiring projects to the specified initial set 
	 * are always included. In addition providing projects are included if the provide on deactivate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, boolean, boolean).
	 * 
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<IProject> projectDeactivation(Collection<IProject> initialSet, boolean activated) 
			throws CircularReferenceException, InPlaceException {
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Closure closure = opt.get(Operation.DEACTIVATE_PROJECT);
		return projectDeactivation(closure, initialSet, activated);
	}

	/**
	 * Topological sort of projects according to the specified dependency option (closure). Requiring projects to the specified initial set 
	 * are always included. In addition providing projects are included if the provide on deactivate option is set.
	 * The last option is Partial graph. See ({@link #partialGraph(Collection, boolean, boolean).
	 * 
	 * @param closure sort according to closure. Valid closures are requiring, providing and requiring and partial graph. An empty set is
	 * returned if closure is null.
	 * @param initialSet the set of start projects to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @return set of sorted projects according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<IProject> projectDeactivation(Closure closure, Collection<IProject> initialSet, boolean activated) 
			throws CircularReferenceException, InPlaceException {
		
		ProjectSorter ps = new ProjectSorter();
		Collection<IProject> resultSet = null;		
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		
		if (null != closure && null != initialSet && initialSet.size() > 0) { 
			if (!opt.isAllowed(Operation.DEACTIVATE_PROJECT, closure) ) {
				throw new InPlaceException("illegal_closure_exception", closure.name(), Operation.DEACTIVATE_PROJECT.name());
			}
			switch (closure) {
			case REQUIRING:
				// Sort projects in dependency order when requiring option (default) is set
				resultSet = ps.sortRequiringProjects(initialSet, activated);
				break;
			case PROVIDING_AND_REQURING:
				resultSet = ps.sortProvidingProjects(initialSet, activated);
				resultSet = ps.sortRequiringProjects(resultSet, activated);
				break;
			case PARTIAL_GRAPH:
				resultSet = partialGraph(initialSet, activated, false);
				break;
			default:
				resultSet = Collections.<IProject>emptySet();
				break;
			}
		} else {
			resultSet = Collections.<IProject>emptySet();			
		}
		return resultSet;
	}

	/**
	 * Topological sort of bundles according to the current dependency option (closure). The options are providing (default), requiring, requiring and providing,
	 * partial graph and single. 
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace
	 * @return set of sorted bundles according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<Bundle> bundleActivation(Collection<Bundle> initialSet, Collection<Bundle> scope) 
			throws CircularReferenceException, InPlaceException {
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Closure closure = opt.get(Operation.ACTIVATE_BUNDLE);
		return bundleActivation(closure, initialSet, scope);

	}
	
	/**
	 * Topological sort of bundles according to the specified dependency option (closure). The options are providing (default), requiring, requiring and providing,
	 * partial graph and single. 
	 * @param closure sort according to closure. Valid closures are providing, requiring, requiring and providing, partial graph and single. An empty set is
	 * returned if closure is null.
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace
	 * @return set of sorted bundles according to dependency option or an empty set. If null or empty an empty collection is returned
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<Bundle> bundleActivation(Closure closure, Collection<Bundle> initialSet, Collection<Bundle> scope) 
			throws CircularReferenceException, InPlaceException {

		BundleSorter bs = new BundleSorter();
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Collection<Bundle> resultSet = null;
		
		if (null != closure && null != initialSet && initialSet.size() > 0 && null != scope && scope.size() > 0) {
			if (!opt.isAllowed(Operation.ACTIVATE_BUNDLE, closure) ) {
				throw new InPlaceException("illegal_closure_exception", closure.name(), Operation.ACTIVATE_BUNDLE.name());
			}
			switch (closure) {
			case PROVIDING:
				resultSet = bs.sortProvidingBundles(initialSet, scope);
				break;
			case REQUIRING:
				resultSet = bs.sortRequiringBundles(initialSet, scope);
				break;
			case REQUIRING_AND_PROVIDING:
				resultSet = bs.sortRequiringBundles(initialSet, scope);
				resultSet = bs.sortProvidingBundles(resultSet, scope);
				break;
			case PARTIAL_GRAPH:
				resultSet = partialGraph(initialSet, scope, true);
				break;
			case SINGLE:
				resultSet = bs.sortProvidingBundles(initialSet, initialSet);
				break;
			default:
				resultSet = Collections.<Bundle>emptySet();
				break;
			}
		} else {
			resultSet = Collections.<Bundle>emptySet();			
		}
		return resultSet;
	}

	/**
	 * Topological sort of bundles according to the current dependency option (closure). The options are requiring (default), providing, providing and requiring,
	 * partial graph and single. 
	 * 
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace. If null or empty an empty collection is returned
	 * @return set of sorted bundles according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<Bundle> bundleDeactivation(Collection<Bundle> initialSet, Collection<Bundle> scope) 
			throws CircularReferenceException, InPlaceException {
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Closure closure = opt.get(Operation.DEACTIVATE_BUNDLE);
		return bundleDeactivation(closure, initialSet, scope);
		
	}

	/**
	 * Topological sort of bundles according to the specified dependency option (closure). The options are requiring (default), providing, providing and requiring,
	 * partial graph and single. 
	 * 
	 * @param closure sort according to closure. Valid closures are requiring, providing, providing and requiring, partial graph and single. An empty set is
	 * returned if closure is null.
	 * @param initialSet the set of start bundles to include in the topological sort. If null or empty an empty collection is returned. 
	 * @param scope limit the set of bundles to search for dependencies in relative to the workspace
	 * @return set of sorted bundles according to dependency option or an empty set.
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal operation/closure combination
	 */
	public Collection<Bundle> bundleDeactivation(Closure closure, Collection<Bundle> initialSet, Collection<Bundle> scope) 
			throws CircularReferenceException, InPlaceException {

		BundleSorter bs = new BundleSorter();
		bs.setAllowCycles(true);
		DependencyOptions opt = InPlace.get().getDependencyOptionsService();
		Collection<Bundle> resultSet = null;

		if (null != closure && null != initialSet && initialSet.size() > 0 && null != scope && scope.size() > 0) {
			if (!opt.isAllowed(Operation.DEACTIVATE_BUNDLE, closure) ) {
				throw new InPlaceException("illegal_closure_exception", closure.name(), Operation.DEACTIVATE_BUNDLE.name());
			}
			switch (closure) {
			case PROVIDING:
				resultSet = bs.sortProvidingBundles(initialSet, scope);
				break;
			case REQUIRING:
				resultSet = bs.sortRequiringBundles(initialSet, scope);
				break;
			case PROVIDING_AND_REQURING:
				resultSet = bs.sortProvidingBundles(initialSet, scope);
				resultSet = bs.sortRequiringBundles(resultSet, scope);
				break;
			case PARTIAL_GRAPH:
				resultSet = partialGraph(initialSet, scope, false);
				break;
			case SINGLE:
				resultSet = bs.sortRequiringBundles(initialSet, initialSet);
				break;
			default:
				resultSet = Collections.<Bundle>emptySet();
				break;
			}
		} else {
			return Collections.<Bundle>emptySet();
		}
		return resultSet;
	}	
	/**
	 * Topological sort of all projects that are directly or indirectly reachable through dependencies 
	 * from an initial set of projects until it is not possible to extend the directed graph further
	 * 
	 * @param initialSet the set of start projects to include in the topological sort. Must not be null or empty 
	 * @param activated if true only consider activated projects and only deactivated projects if false
	 * @param requiring sort in requiring order and then in providing order if true and in opposite order if false
	 * @return the result graph of the topological sort 
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	protected Collection<IProject> partialGraph(Collection<IProject> initialSet, boolean activated, boolean requiring) throws CircularReferenceException {
		
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
	 * @param initialSet the set of start projects to include in the topological sort. Must not be null or empty 
	 * @param scope the set of bundles to consider as candidates in the result graph. Must not be null or empty
	 * @param requiring sort in requiring order and then in providing order if true and in opposite order if false
	 * @return the result graph of the topological sort 
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 */
	protected Collection<Bundle> partialGraph(Collection<Bundle> initialSet, Collection<Bundle> scope, boolean requiring) throws CircularReferenceException {
		
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
