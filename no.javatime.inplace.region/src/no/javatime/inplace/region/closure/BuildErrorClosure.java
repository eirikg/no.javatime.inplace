package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Detects build errors and missing build state for single projects and build error closures based on an initial set
 * of bundle projects. A build error closure is all "requiring and providing" bundles or all "providing and requiring" 
 * bundles to a bundle with build errors. Type of closure is determined by the type of transition specified together
 * with the initial set of bundles at construction time.
 * <p>
 * The returned closure is the union of an error closure and a closure of the same type of the 
 * initial set of bundle projects specified together with the kind of transition the error closure should be calculated for.    
 *
 */
public class BuildErrorClosure {

	final private EnumSet<Transition> activate = EnumSet.of(Transition.INSTALL, Transition.UPDATE,
			Transition.ACTIVATE_BUNDLE, Transition.ACTIVATE_PROJECT, Transition.RESOLVE,
			Transition.REFRESH, Transition.BUILD);

	final private EnumSet<Transition> deactivate = EnumSet.of(Transition.DEACTIVATE,
			Transition.UNINSTALL, Transition.UNRESOLVE);

	final private BundleRegion bundleRegion = BundleManager.getRegion();
	final private BundleTransition bundleTransition = BundleManager.getTransition();

	private Collection<IProject> initialProjects;
	private Collection<IProject> projectClosures;
	private Collection<IProject> errorProjects;
	private Collection<IProject> errorClosures;
	private Transition currentTransition;
	final private BundleClosures bc = new BundleClosures();

	public BuildErrorClosure(Collection<IProject> initialProjects, Transition transition) {
		this.initialProjects = new LinkedHashSet<>(initialProjects);
		this.currentTransition = transition;
	}

	/**
	 * Calculate closures from the initial (starter) set projects.
	 * <p>
	 * Type of closure is determined by the transition type set at construction time.
	 * 
	 * @return closures constructed from the initial (starter) set of projects
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 */
	private Collection<IProject> getStartprojectsClosures() throws InPlaceException,
			CircularReferenceException {
		if (null == projectClosures) {
			projectClosures = getBundleProjectClosures(initialProjects, currentTransition);
		}
		return projectClosures;
	}

	/**
	 * Construct a closure for each project with build errors.
	 * <p>
	 * Type of closure is determined by the transition type set at construction time.
	 * 
	 * @return all closures of projects with build errors
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 */
	private Collection<IProject> getErrorClosures() throws InPlaceException,
			CircularReferenceException {

		if (null == errorClosures) {
			errorClosures = getBundleProjectClosures(getBuildErrors(), currentTransition);
		}
		return errorClosures;
	}

	/**
	 * Construct a set of closures based on the specified set of projects
	 * <p>
	 * Type of closure is determined by the specified transition type
	 * 
	 * @return all closures of the specified projects projects and the transition type. If the
	 * specified projects is null an empty set is returned
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 */
	private Collection<IProject> getBundleProjectClosures(Collection<IProject> projects,
			Transition transition) throws InPlaceException, CircularReferenceException {

		if (null == projects) {
			return Collections.<IProject> emptySet();
		}
		if (projects.size() > 0) {
//			if (bundleRegion.isBundleWorkspaceActivated()) {
//				Collection<Bundle> bundleClosures = bundleRegion.getBundles(projects);
//				Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
//				if (activate.contains(transition)) {
//					bundleClosures = bc.bundleActivation(Closure.REQUIRING_AND_PROVIDING, bundleClosures,
//							activatedBundles);
//				} else if (deactivate.contains(transition)) {
//					bundleClosures = bc.bundleDeactivation(Closure.PROVIDING_AND_REQURING, bundleClosures, activatedBundles);
//				} else {
//					bundleClosures = bc.bundleActivation(Closure.REQUIRING_AND_PROVIDING, bundleClosures,
//							activatedBundles);
//				}
//				projects = bundleRegion.getBundleProjects(bundleClosures);
//			} else {
				if (activate.contains(transition)) {
					projects = bc.projectActivation(Closure.REQUIRING_AND_PROVIDING, projects, true);
				} else if (deactivate.contains(transition)) {
					projects = bc.projectDeactivation(Closure.PROVIDING_AND_REQURING, projects, true);
				} else {
					projects = bc.projectActivation(Closure.REQUIRING_AND_PROVIDING, projects, true);
				}
//			}
		}
		return projects;
	}

	/**
	 * Check all projects for build errors among the closures constructed from the initial (starter)
	 * set of projects.
	 * 
	 * @return true if there are any projects with build errors among the closure of the initial
	 * (starter) set of projects.
	 * @see #getBuildErrors()
	 */
	public boolean hasBuildErrors() {
		return getBuildErrors().size() > 0 ? true : false;
	}

	/**
	 * All projects with build errors that are member of the closure constructed for the initial
	 * (starter) set of projects.
	 * 
	 * @return projects with build errors among the closures of the initial (starter) set of projects.
	 * @see #getStartprojectsClosures()
	 */
	public Collection<IProject> getBuildErrors() {
		if (null == errorProjects) {
			errorProjects = getBuildErrors(getStartprojectsClosures());
			errorProjects.addAll(hasBuildState(getStartprojectsClosures()));
		}
		return errorProjects;
	}

	/**
	 * Find all projects among the initial (starter) set of projects that are member of the closures
	 * of projects with build errors
	 * <p>
	 * All closures of projects with build errors contains the requiring closure or the requiring and
	 * providing closure if the include providing closure is set to true at construction time
	 * 
	 * @return projects from the initial (starter) set which are members of the closure of projects
	 * with build errors
	 * @see #getErrorClosures()
	 */
	private Collection<IProject> getExludeClosure() {

		if (getBuildErrors().size() > 0) {
			Collection<IProject> excludeSet = new LinkedHashSet<>(getErrorClosures());
			//excludeSet.addAll(getStartprojectsClosures());
			excludeSet.retainAll(getStartprojectsClosures());
			return excludeSet;
		}
		return Collections.<IProject> emptySet();
	}

	/**
	 * Find all projects among the initial (starter) set of projects that are member of the closures
	 * of projects with build errors.
	 * <p>
	 * All closures of projects with build errors contains the requiring closure or the requiring and
	 * providing closure if the include providing closure is set to true at construction time
	 * 
	 * @param activatedProjects if true only consider activated projects. If false, consider all
	 * projects.
	 * @return projects from the initial (starter) set which are members of the closure of projects
	 * with build errors
	 * @see #getBundleErrorClosures(boolean)
	 */
	public Collection<IProject> getProjectErrorClosures(boolean activatedProjects) {

		Collection<IProject> projects = getExludeClosure();
		if (projects.size() > 0) {
			if (activatedProjects) {
				projects.retainAll(BundleProjectState.getActivatedProjects());
			}
		}
		return projects;
	}

	/**
	 * Find all bundle projects among the initial (starter) set of bundle projects that are member of
	 * the closures of projects with build errors.
	 * <p>
	 * All closures of projects with build errors contains the requiring closure or the requiring and
	 * providing closure if the include providing closure is set to true at construction time
	 * 
	 * @param activatedBundles if true only consider activated bundles. If false, consider all bundles
	 * that are at least in state installed.
	 * @return bundle projects from the initial (starter) set which are members of the closure of
	 * projects with build errors
	 * @see #getProjectErrorClosures(boolean)
	 */
	public Collection<Bundle> getBundleErrorClosures(boolean activatedBundles) {
		Collection<IProject> projects = getProjectErrorClosures(activatedBundles);
		return bundleRegion.getBundles(projects);
	}

	/**
	 * Constructs a status object with information about which bundle projects to exclude based on the
	 * calculated set of error closures and the initial (starter) set of projects.
	 * <p>
	 * Uses the {@linkplain #getProjectErrorClosures(boolean)} to determine which projects to exclude.
	 * 
	 * @param activatedProjects true to consider activate projects and false to consider all projects
	 * 
	 * @return status object describing bundle projects to exclude based on the initial set of bundle
	 * projects and the calculated error closures. Returns null if there are no build errors.
	 * @see #getProjectErrorClosures(boolean)
	 * @see #getBundleErrorClosures(boolean)
	 */
	public IBundleStatus getProjectErrorClosureStatus(boolean activatedProjects) {

		IBundleStatus buildStatus = null;
		Collection<IProject> excludeClosure = getProjectErrorClosures(activatedProjects);
		if (excludeClosure.size() > 0) {
			Collection<IProject> errorProjects = getBuildErrors();
			Collection<IProject> directAffectedProjects = new LinkedHashSet<>(initialProjects);
			directAffectedProjects.retainAll(excludeClosure);
			String name = bundleTransition.getTransitionName(currentTransition, true, false);
			String msg = NLS.bind(
					Msg.AWAITING_BUILD_INFO,
					new Object[] { name, BundleProjectState.formatProjectList(directAffectedProjects),
							BundleProjectState.formatProjectList(errorProjects)});
			buildStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, null);
			if (bundleRegion.isBundleWorkspaceActivated()) {
				Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
				for (IProject errorProject : errorProjects) {
					Bundle errorBundle = bundleRegion.get(errorProject);
					Collection<Bundle> bundleClosures = bc.bundleActivation(Closure.PROVIDING,
							Collections.<Bundle> singletonList(errorBundle), activatedBundles);
					bundleClosures.remove(errorBundle);
					if (bundleClosures.size() > 0) {
						msg = NLS.bind(Msg.PROVIDING_BUNDLES_INFO, errorBundle.getSymbolicName(),
								bundleRegion.formatBundleList(bundleClosures, false));
						buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
					}
					bundleClosures = bc.bundleDeactivation(Closure.REQUIRING,
							Collections.<Bundle> singletonList(errorBundle), activatedBundles);
					bundleClosures.remove(errorBundle);
					if (bundleClosures.size() > 0) {
						msg = NLS.bind(Msg.REQUIRING_BUNDLES_INFO, errorBundle.getSymbolicName(),
								bundleRegion.formatBundleList(bundleClosures, false));
						buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
					}
				}
			}
		}
		return buildStatus;
	}

	/**
	 * Check if there are build errors from the most recent build.
	 * 
	 * @return cancel list of projects with errors or an empty list
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	public static Collection<IProject> getBuildErrors(Collection<IProject> projects)
			throws InPlaceException {
		Collection<IProject> errors = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (hasBuildErrors(project)) {
				errors.add(project);
			}
		}
		return errors;
	}

	/**
	 * Check if the compilation state of an {@link IJavaProject} has errors.
	 * 
	 * @param project the {@link IJavaProject} to check for errors
	 * @return <code>true</code> if the project has compilation errors (or has never been built),
	 * <code>false</code> otherwise
	 * @throws InPlaceException if project does not exist or is closed
	 */
	public static boolean hasBuildErrors(IProject project) throws InPlaceException {

		try {
			if (null != project && project.isAccessible()) {
				IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				// check if any of these have a severity attribute that indicates an error
				for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
					if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
							IMarker.SEVERITY_INFO)) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "has_build_errors", project);
		}
		return false;
	}

	/**
	 * Check if the specified projects have build state
	 * 
	 * @param projects to check for build state
	 * @return Set of projects missing build state among the specified projects or an empty set
	 */
	public static Collection<IProject> hasBuildState(Collection<IProject> projects) {
		Collection<IProject> missingBuildState = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (!hasBuildState(project)) {
				missingBuildState.add(project);
			}
		}
		return missingBuildState;
	}

	/**
	 * Checks if the project has build state
	 * 
	 * @param project to check for build state
	 * @return true if the project has build state, otherwise false
	 */
	public static boolean hasBuildState(IProject project) throws InPlaceException {

		if (null == project) {
			throw new InPlaceException("null_project_build_state");
		}
		if (project.isAccessible()) {
			IJavaProject javaProject = BundleProjectState.getJavaProject(project.getName());
			if (javaProject.hasBuildState()) {
				return true;
			}
		}
		return false;
	}
}
