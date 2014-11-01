package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleProjectDescription;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleProjectDescriptionImpl;
import no.javatime.inplace.region.project.BundleProjectImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Detects and reports build errors and missing build state for single projects and build error
 * closures based on an initial set of bundle projects.
 * <p>
 * A build error closure is identified when the closure of an initial set of projects (project
 * closure) contains one or more projects with build errors. Type of closure is set at construction
 * time and is providing ({@code Closure.PROVIDING}) and requiring ({@code Closure.REQUIRING}). The
 * providing/requiring project closure is defined as the set of bundle projects providing/requiring
 * capabilities to/from an initial project within a specified activation level and scope.
 * <p>
 * The activation level is used to specify that all projects ({@code Bundle#UNINSTALLED}), installed
 * resolved and activated bundles ({@code Bundle#INSTALLED}) or only resolved and activated (
 * {@code Bundle#RESOLVED}) bundles should be included when calculating the error closure.
 * <p>
 * The scope of a closure for a set of initial projects is determined by the activation scope
 * specified at construction time. The activation scope is deactivated (
 * {@code ActivationLevel.DEACTIVATED}), activated ( {@code ActivationLevel.ACTIVATED}) or all (
 * {@code ActivationLevel.ALL}) projects
 * <p>
 * The reason for using both activation level an scope is illustrated by the following example.
 * Setting the activation level to installed and the the activation scope to activated means that
 * all bundles that are at least installed and activated will be considered when calculating the
 * closures. There may be a need for this combination in an activation process; - that is when an
 * installed bundle is activated but not yet resolved.
 */
public class BuildErrorClosure {

	final static private BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE;
	final static private BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;

	public enum ActivationScope {
		ACTIVATED, DEACTIVATED, ALL
	};

	private Collection<IProject> initialProjects;
	private Collection<IProject> projectClosures;
	private Collection<IProject> errorProjects;
	private Collection<IProject> errorClosures;
	private Transition currentTransition;
	private Closure closure = Closure.REQUIRING;
	private int activationLevel = Bundle.INSTALLED;
	private ActivationScope activationScope = ActivationScope.ACTIVATED;

	private String buildErrorHeaderMessage;

	/**
	 * Calculate the build error closures for the specified projects where the build error closure is
	 * determined by type of closure, activation level and activation scope.
	 * <p>
	 * See
	 * {@link BuildErrorClosure#BuildErrorClosure(Collection, Transition, Closure, int, ActivationScope)}
	 * for a specification of default values for the closure, activation level and activation scope.
	 * 
	 * @param initialProjects set of projects to calculate the error closures from
	 * @param transition type of transition the error closure is used for. The transition type is only
	 * used when constructing the default warning message of the detected error closures
	 * @param closure type of closure is providing ({@code Closure.PROVIDING}) or requiring (
	 * {@code Closure.REQUIRING}) The providing/requiring closure is interpreted as bundle projects
	 * providing/requiring capabilities to/from an initial project.
	 */
	public BuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure) {
		this.initialProjects = new LinkedHashSet<>(initialProjects);
		this.currentTransition = transition;
		this.closure = closure;
	}

	/**
	 * Calculate the build error closures for the specified projects where the build error closure is
	 * determined by type of closure, activation level and activation scope
	 * <p>
	 * The closure defaults to requiring ({@code Closure.REQUIRING}) which means that the requiring
	 * projects of the specified projects are included in the closures.
	 * <p>
	 * The activation level defaults to {@code Bundle.INSTALLED} which means that all installed,
	 * resolved and active requiring or providing (depending on the specified closure type) bundles
	 * are included in the calculated closures.
	 * <p>
	 * The activation scope defaults to activated bundles which means that only activated bundles are
	 * considered when calculating the closures.
	 * 
	 * @param initialProjects set of projects to calculate the error closures from
	 * @param transition type of transition the error closure is used for. The transition type is only
	 * used when constructing the default warning message of the detected error closures
	 * @param closure type of closure is providing ({@code Closure.PROVIDING}) or requiring (
	 * {@code Closure.REQUIRING}) The providing/requiring project closure is interpreted as bundle
	 * projects providing/requiring capabilities to/from an initial project.
	 * @param activationLevel specifies that all projects ({@code Bundle#UNINSTALLED}), installed
	 * resolved and activated bundles ({@code Bundle#INSTALLED}) or only resolved and activated (
	 * {@code Bundle#RESOLVED}) bundles should be used when calculating the error closure.
	 * @param activationScope is deactivated ({@code ActivationLevel.DEACTIVATED}), activated (
	 * {@code ActivationLevel.ACTIVATED}) or all ({@code ActivationLevel.ALL}) projects
	 */
	public BuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure, int activationLevel, ActivationScope scope) {
		this.initialProjects = new LinkedHashSet<>(initialProjects);
		this.currentTransition = transition;
		this.closure = closure;
		this.activationLevel = activationLevel;
		this.activationScope = scope;
	}

	/**
	 * Construct a build error closure for each project with build errors that is member in one or
	 * more of the closures calculated from the initial set of projects (project closures). Given a
	 * set of independent projects with build errors - restricted to the project closures -, construct
	 * the build error closures by calculating the requiring closure for each project with build
	 * errors.
	 * 
	 * The final calculated build error closures than includes the the error projects and their
	 * requiring closures restricted to the project closures. This means that providing projects to
	 * projects with build errors are allowed given the position of the project with build errors in
	 * the partial graph of the project closure (the closure calculated from the initial project).
	 * 
	 * @return all closures of projects with build errors
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 */
	public Collection<IProject> getBuildErrorClosures() throws InPlaceException,
			CircularReferenceException {
		if (null == errorClosures) {
			// Get projects with build errors and their requiring projects. Providing projects to projects
			// with build errors are excluded from the build error closure
			errorClosures = getBundleProjectClosures(getBuildErrors(), Closure.REQUIRING, activationScope);
			// The build error closure is a subset of the project closure. 
			// Restrict the build error closures to the project closures (closures calculated from the
			// initial set of projects given the type of closure)
			errorClosures.retainAll(getProjectClosures());
		}
		return errorClosures;
	}

	/**
	 * Construct a set of closures based on the specified set of projects
	 * <p>
	 * 
	 * @param projects initial set of projects to calculate the closure from
	 * @param closuree type of closure to calculate, Valid closures are providing and requiring
	 * @param scope determines the scope of projects to use when constructing closures. The scope is
	 * all, deactivated or activated bundle projects.
	 * @return all closures of the specified projects. If the specified projects is null or empty an
	 * empty set is returned
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 */
	private Collection<IProject> getBundleProjectClosures(Collection<IProject> projects,
			Closure closure, ActivationScope scope) throws InPlaceException, CircularReferenceException {

		if (null == projects || projects.size() == 0) {
			return Collections.<IProject> emptySet();
		}
		if ((activationLevel & (Bundle.INSTALLED | Bundle.RESOLVED)) != 0) {
			Collection<Bundle> bundleClosures = bundleRegion.getBundles(projects);
			Collection<Bundle> bundleScope = null;
			switch (scope) {
			case ACTIVATED:
				bundleScope = bundleRegion.getActivatedBundles();
				break;
			case DEACTIVATED:
				bundleScope = bundleRegion.getDeactivatedBundles();
				break;
			case ALL:
				bundleScope = bundleRegion.getBundles();
			default:
				break;
			}
			BundleSorter bs = new BundleSorter();
			switch (closure) {
			case PROVIDING:
				if ((activationLevel & (Bundle.INSTALLED)) != 0) {
					bundleClosures = bs.sortDeclaredProvidingBundles(bundleClosures, bundleScope);
				} else {
					bundleClosures = bs.sortProvidingBundles(bundleClosures, bundleScope);
				}
				break;
			case REQUIRING:
				if ((activationLevel & (Bundle.INSTALLED)) != 0) {
					bundleClosures = bs.sortDeclaredRequiringBundles(bundleClosures, bundleScope);
				} else {
					bundleClosures = bs.sortRequiringBundles(bundleClosures, bundleScope);
				}
				break;
			default:
				break;
			}
			return bundleRegion.getProjects(bundleClosures);
		} else {
			Collection<IProject> projectClosures = null;
			ProjectSorter ps = new ProjectSorter();
			switch (closure) {
			case PROVIDING: {
				switch (scope) {
				case ACTIVATED:
					projectClosures = ps.sortProvidingProjects(projects, true);
					break;
				case DEACTIVATED:
					projectClosures = ps.sortProvidingProjects(projects, false);
					break;
				case ALL:
					projectClosures = ps.sortProvidingProjects(projects);
				default:
					break;
				}
				break;
			}
			case REQUIRING: {
				switch (scope) {
				case ACTIVATED:
					projectClosures = ps.sortRequiringProjects(projects, true);
					// projectClosures = bc.projectDeactivation(closure, projects, true);
					break;
				case DEACTIVATED:
					projectClosures = ps.sortRequiringProjects(projects, false);
					// projectClosures = bc.projectDeactivation(closure, projects, false);
					break;
				case ALL:
					projectClosures = ps.sortRequiringProjects(projects);
					// projectClosures = bc.projectDeactivation(closure, projects);
				default:
					break;
				}
				break;
			}
			default: {
				projectClosures = new LinkedHashSet<IProject>(projects);
				break;
			}
			}
			return projectClosures;
		}
	}

	/**
	 * Check all projects for build errors among the closures constructed from the initial (starter)
	 * set of projects.
	 * 
	 * @return true if there are any projects with build errors among the calculated closures based on
	 * the initial (starter) set of projects specified at construction time
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 * @see #getBuildErrors()
	 */
	public boolean hasBuildErrors() throws CircularReferenceException,
			InPlaceException {
		return getBuildErrors().size() > 0 ? true : false;
	}

	/**
	 * All projects with build errors that are member of the closure constructed for the initial
	 * (starter) set of projects.
	 * 
	 * @return projects with build errors among the closures of the initial (starter) set of projects.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 * @see #hasBuildErrors()
	 */
	public Collection<IProject> getBuildErrors() throws CircularReferenceException,
	InPlaceException {
		if (null == errorProjects) {
			errorProjects = getBuildErrors(getProjectClosures());
			errorProjects.addAll(hasBuildState(getProjectClosures()));
		}
		return errorProjects;
	}

	/**
	 * Calculate project closures from the initial (starter) set projects based on closure type,
	 * activation level and scope.
	 * <p>
	 * Type of closure, activation level and scope is is set at construction time.
	 * 
	 * @return project closures constructed from the initial (starter) set of projects
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 * @see #getBundleClosures()
	 */
	public Collection<IProject> getProjectClosures() throws CircularReferenceException,
			InPlaceException {
		if (null == projectClosures) {
			projectClosures = getBundleProjectClosures(initialProjects, closure, activationScope);
		}
		return projectClosures;
	}

	/**
	 * Calculate project closures from the initial (starter) set projects and return their associated
	 * bundles. This is a convenience method returning bundles instead of projects
	 * <p>
	 * 
	 * @return closures constructed from the initial (starter) set of projects
	 * @throws CircularReferenceException if cycles are detected in the bundle graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 * @see #getProjectClosures()
	 */
	public Collection<Bundle> getBundleClosures() throws CircularReferenceException, InPlaceException {
		Collection<IProject> projects = getProjectClosures();
		return bundleRegion.getBundles(projects);
	}

	/**
	 * Get the message text of the root build warning message
	 * 
	 * @return the root build error message. If no message is set return null.
	 */
	public String getBuildErrorHeaderMessage() {
		return buildErrorHeaderMessage;
	}

	/**
	 * Set the message text of the root build warning message
	 * <p>
	 * A default message is used if no message is set
	 * 
	 * @param message the root message of the build error message
	 */
	public void setBuildErrorHeaderMessage(String message) {
		this.buildErrorHeaderMessage = message;
	}

	/**
	 * Constructs a status object with information about the build error closures based on the initial
	 * (starter) set of projects specified at construction time
	 * 
	 * @return status a multi status object with {@code StatusCode.WARNING} describing the build error
	 * closures based on the initial set of bundle projects and the calculated error closures. Returns
	 * a status object with {@code StatusCode.OK} along with the list of the project closures if there
	 * are no build errors.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException if failing to get the dependency options service or illegal
	 * operation/closure combination
	 * @see #getBuildErrorClosures()
	 * @see #getProjectClosures()
	 */
	public IBundleStatus getErrorClosureStatus() throws InPlaceException,
			CircularReferenceException {

		Collection<IProject> buildErrors = getBuildErrors();
		if (buildErrors.size() == 0) {
			String okMsg = NLS.bind(Msg.NO_BUILD_ERROR_INFO,
					BundleProjectImpl.INSTANCE.formatProjectList(getProjectClosures()));
			return new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, okMsg, null);
		}
		Collection<IProject> buildErrorClosures = getBuildErrorClosures();
		String name = bundleTransition.getTransitionName(currentTransition, true, false);
		String msg = getBuildErrorHeaderMessage();
		if (null == msg) {
			msg = NLS.bind(Msg.AWAITING_BUILD_ERROR_INFO,
					new Object[] { name, BundleProjectImpl.INSTANCE.formatProjectList(buildErrorClosures),
							BundleProjectImpl.INSTANCE.formatProjectList(buildErrors) });
		}
		IBundleStatus buildStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, null);
		for (IProject errorProject : buildErrors) {
			Collection<IProject> projectClosures = getBundleProjectClosures(
					Collections.<IProject> singletonList(errorProject), Closure.PROVIDING,
					ActivationScope.ALL);
			projectClosures.remove(errorProject);
			String errProjectIdent = bundleRegion.getSymbolicNameFromManifest(errorProject);
			if (null == errProjectIdent) {
				errProjectIdent = errorProject.getName();
				errProjectIdent += " (P)";
			}
			if (projectClosures.size() > 0) {
				msg = NLS.bind(Msg.PROVIDING_BUNDLES_INFO,
						errProjectIdent, BundleProjectImpl.INSTANCE.formatProjectList(projectClosures));
				buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
			}
			projectClosures = getBundleProjectClosures(
					Collections.<IProject> singletonList(errorProject), Closure.REQUIRING,
					ActivationScope.ALL);
			projectClosures.remove(errorProject);
			if (projectClosures.size() > 0) {
				msg = NLS.bind(Msg.REQUIRING_BUNDLES_INFO,
						errProjectIdent, BundleProjectImpl.INSTANCE.formatProjectList(projectClosures));
				buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
			}
		}
		return buildStatus;
	}

	/**
	 * Check if there are build errors from the most recent build.
	 * 
	 * @return List of projects with build errors or an empty list
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
			IJavaProject javaProject = BundleProjectImpl.INSTANCE.getJavaProject(project.getName());
			if (javaProject.hasBuildState()) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Check for existence and build errors in the manifest file in the specified project
	 * <p>
	 * Assumes that the manifest file is located at the default location
	 * 
	 * @param project to check for the existence and build errors in the manifest file at the default location
	 * @return true if the manifest file does not exist or contains build errors and false otherwise
	 * @throws InPlaceException if a core exception occurs. The exception contains a status object describing the failure
	 * @see #hasManifest(IProject)
	 * @see BundleProjectDescription#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectDescription#MANIFEST_FILE_NAME
	 */
	public static boolean hasManifestBuildErrors(IProject project) throws InPlaceException {
		
		try {
			if (!hasManifest(project)) {
				return true;
			}
			IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			// check if any of these have a severity attribute that indicates an error
			for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
				if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_INFO)) {
					IResource resource = problems[problemsIndex].getResource();
					if (resource instanceof IFile && resource.getName().equals(BundleProjectDescriptionImpl.MANIFEST_FILE_NAME)) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_has_errors", project);
		}
		return false;
	}

	/**
	 * Check for existence of a manifest file at the default location in the specified project
	 * 
	 * @param project to check for the existence of a manifest file at the default location
	 * @return true if the manifest file exist at the default location and false otherwise
	 * @see BundleProjectDescription#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectDescription#MANIFEST_FILE_NAME
	 */
	public static Boolean hasManifest(IProject project) {
		if (null != project && project.isAccessible()) {
			IFile manifestFile = project.getFile(BundleProjectDescription.MANIFEST_RELATIVE_PATH + BundleProjectDescriptionImpl.MANIFEST_FILE_NAME);
			if (manifestFile.exists()) {
				return true;
			}
		}
		return false;
	}
}
