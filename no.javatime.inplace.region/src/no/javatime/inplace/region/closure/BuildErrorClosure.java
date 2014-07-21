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
 * <p>
 * Default is to use the bundle sort to get closures. Specify {@code Bundle.INSTALL} in the constructor to use project sort  
 */
public class BuildErrorClosure {

	final static private EnumSet<Transition> activate = EnumSet.of(Transition.INSTALL, Transition.UPDATE,
			Transition.ACTIVATE_BUNDLE, Transition.ACTIVATE_PROJECT, Transition.RESOLVE,
			Transition.REFRESH, Transition.BUILD);

	final static private EnumSet<Transition> deactivate = EnumSet.of(Transition.DEACTIVATE,
			Transition.UNINSTALL, Transition.UNRESOLVE);

	final static private BundleRegion bundleRegion = BundleManager.getRegion();
	final static private BundleTransition bundleTransition = BundleManager.getTransition();

	private Collection<IProject> initialProjects;
	private Collection<IProject> projectClosures;
	private Collection<IProject> errorProjects;
	private Collection<IProject> errorClosures;
	private Transition currentTransition;
	private int sortLevel = Bundle.INSTALLED;
	private boolean activated = true;
	private String buildErrorheadermessage;
	
	final private BundleClosures bc = new BundleClosures();

	public BuildErrorClosure(Collection<IProject> initialProjects, Transition transition) {
		this.initialProjects = new LinkedHashSet<>(initialProjects);
		this.currentTransition = transition;
	}

	public BuildErrorClosure(Collection<Bundle> initialBundles, Transition transition, int sortLevel) {
		this.initialProjects = BundleManager.getRegion().getBundleProjects(initialBundles);
		this.currentTransition = transition;
		this.sortLevel = sortLevel;
	}

	/**
	 * Calculate closures from the initial (starter) set projects.
	 * <p>
	 * Type of closure is determined by the transition type set at construction time.
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
			if ((sortLevel & (Bundle.INSTALLED | Bundle.RESOLVED)) != 0) {
				Collection<Bundle> bundleClosures = bundleRegion.getBundles(projects);
				Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
				if (activate.contains(transition)) {
					bundleClosures = bc.bundleActivation(Closure.REQUIRING_AND_PROVIDING, bundleClosures,
							activatedBundles);
				} else if (deactivate.contains(transition)) {
					bundleClosures = bc.bundleDeactivation(Closure.PROVIDING_AND_REQURING, bundleClosures, activatedBundles);
				} else {
					bundleClosures = bc.bundleActivation(Closure.REQUIRING_AND_PROVIDING, bundleClosures,
							activatedBundles);
				}
				projects = bundleRegion.getBundleProjects(bundleClosures);
			} else {
				if (activate.contains(transition)) {
					projects = bc.projectActivation(Closure.REQUIRING_AND_PROVIDING, projects, activated);
				} else if (deactivate.contains(transition)) {
					projects = bc.projectDeactivation(Closure.PROVIDING_AND_REQURING, projects, activated);
				} else {
					projects = bc.projectActivation(Closure.REQUIRING_AND_PROVIDING, projects, activated);
				}
			}
		}
		return projects;
	}
	
	/**
	 * Return the sort level that will be used when calculating error closures 
	 * @return {@code Bundle.UNINSTALLED} if projects are sorted to find error closures and {@code Bundle.INSTALL}
	 * to sort bundles
	 */
	public int getSortLevel() {
		return sortLevel;
	}
	
	/**
	 * Set the sort level to use when calculating error closures
	 * @param sortLevel Use {@code Bundle.UNINSTALLED} to sort projects when calculating error closures 
	 * and {@code Bundle.INSTALL} to sort bundles
	 * @param activated true to get error closures among activated projects 
	 * and false to get error closures among deactivated projects
	 */
	public void setSortLevel(int sortLevel, boolean activated) {
		this.sortLevel = sortLevel;
		this.activated = activated; 
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
	 * Find all projects among the closures of the initial (starter) set of projects that are member of the closures
	 * of projects with build errors
	 * <p>
	 * All closures of projects with build errors contains the requiring closure or the requiring and
	 * providing closure if the include providing closure is set to true at construction time
	 * 
	 * @return projects from the initial (starter) set which are members of the closure of projects
	 * with build errors
	 * @see #getBundleErrorClosures()
	 */
	public Collection<IProject> getProjectErrorClosures() {

		Collection<IProject> projects = null;
		if (getBuildErrors().size() > 0) {
			projects = new LinkedHashSet<>(getErrorClosures());
			projects.retainAll(getStartprojectsClosures());
			return projects;
		} else {
			projects = Collections.<IProject> emptySet();
		}
		return projects;
	}

	/**
	 * Find all bundle projects among the closures of the initial (starter) set of bundle projects that are member of
	 * the closures of projects with build errors.
	 * 
	 * @return bundle projects from the initial (starter) set which are members of the closure of
	 * projects with build errors
	 * @see #getProjectErrorClosures()
	 */
	public Collection<Bundle> getBundleErrorClosures() {
		Collection<IProject> projects = getProjectErrorClosures();
		return bundleRegion.getBundles(projects);
	}
	
	/**
	 * Get all bundle projects among the initial (starter) set of bundle projects that are member of
	 * the closures of projects with build errors.
	 * 
	 * @return
	 */
	public Collection<IProject> getDirectAffectedProjects() {

		Collection<IProject> excludeClosure = getProjectErrorClosures();
		if (excludeClosure.size() > 0) {
			Collection<IProject> directAffectedProjects = new LinkedHashSet<>(initialProjects);
			directAffectedProjects.retainAll(excludeClosure);
		}
		return excludeClosure;
	}
	
	/**
	 * Get the message text of the root build error message
	 *  
	 * @return the root build error message. If no message is
	 * set return null.
	 */
	public String getBuildErrorHeaderMessage() {
		return buildErrorheadermessage;	
	}
	/**
	 * Set the message text of the root build error message
	 * <p>
	 * A default message is used if no message is set
	 * 
	 * @param message the root message of the build error message 
	 */
	public void setBuildErrorHeaderMessage(String message) {
		this.buildErrorheadermessage = message;	
	}

	/**
	 * Constructs a status object with information about which bundle projects to exclude based on the
	 * calculated set of error closures and the initial (starter) set of projects.
	 * <p>
	 * Uses the {@linkplain #getProjectErrorClosures()} to determine which projects to exclude.
	 * 
	 * @return status object describing bundle projects to exclude based on the initial set of bundle
	 * projects and the calculated error closures. Returns null if there are no build errors.
	 * @see #getProjectErrorClosures()
	 * @see #getBundleErrorClosures()
	 */
	public IBundleStatus getProjectErrorClosureStatus() {

		IBundleStatus buildStatus = null;
		Collection<IProject> directAffectedProjects = getDirectAffectedProjects();
		if (directAffectedProjects.size() > 0) {
			Collection<IProject> errorProjects = getBuildErrors();
			String name = bundleTransition.getTransitionName(currentTransition, true, false);
			String msg = getBuildErrorHeaderMessage();
			if (null == msg) {
				msg = NLS.bind(
						Msg.AWAITING_BUILD_ERROR_INFO,
						new Object[] { name, BundleProjectState.formatProjectList(directAffectedProjects),
								BundleProjectState.formatProjectList(errorProjects)});
			}
			buildStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, 
					msg, null);
			if (bundleRegion.isBundleWorkspaceActivated() && activated) {
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
			} else {
				for (IProject errorProject : errorProjects) {
					Collection<IProject> projectClosures = bc.projectActivation(Closure.PROVIDING, 
							Collections.<IProject> singletonList(errorProject), activated);
					projectClosures.remove(errorProject);
					if (projectClosures.size() > 0) {
						msg = NLS.bind(Msg.PROVIDING_BUNDLES_INFO, bundleRegion.getSymbolicNameFromManifest(errorProject),
								BundleProjectState.formatProjectList(projectClosures));
						buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
					}
					projectClosures = bc.projectDeactivation(Closure.REQUIRING, 
							Collections.<IProject> singletonList(errorProject), activated);
					projectClosures.remove(errorProject);
					if (projectClosures.size() > 0) {
						msg = NLS.bind(Msg.REQUIRING_BUNDLES_INFO, bundleRegion.getSymbolicNameFromManifest(errorProject),
								BundleProjectState.formatProjectList(projectClosures));
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
