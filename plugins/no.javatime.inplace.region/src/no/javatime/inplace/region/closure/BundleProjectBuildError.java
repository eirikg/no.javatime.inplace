package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.ExternalDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.project.BundleProjectCandidatesImpl;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
import no.javatime.inplace.region.project.CachedManifestOperationsImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Utility to discover bundle and build errors in bundle projects detected at compile time and
 * before bundle projects are installed or updated
 * <p>
 * Errors are divided into bundle errors and build errors where bundle errors is a subset of build
 * errors:
 * <ol>
 * <li>Bundle errors include projects with no build state, build errors in project manifest files,
 * missing project descriptions, workspace bundles that are duplicates of external bundles or other
 * workspace bundles and projects that are involved in circular references between bundle projects.
 * <li>Build errors include all build errors reported by the build system and bundle errors
 * </ol>
 * Bundle errors detected by OSGi are life cycle errors and in essence include duplicates among
 * workspace region bundles, project location access failures, security violations, bundle state and
 * state change errors and bundle runtime errors.
 */
public class BundleProjectBuildError {

	/**
	 * Check if the specified project has bundle errors or build errors.
	 * <p>
	 * Check bundle errors if the "Activate on Build Error" option is on and build errors if off. Note
	 * that bundle errors is a subset of build errors.
	 * 
	 * @param projects the {@link IJavaProject} to check for errors
	 * @return projects with bundle errors
	 * @see #getBuildErrors(Collection, boolean)
	 * @throws ExtenderException If failing to obtain the command options service
	 * @see #getBundleErrors(Collection, boolean)
	 * @see #getBuildErrors(Collection, boolean)
	 */
	public static Collection<IProject> getErrors(Collection<IProject> projects)
			throws ExtenderException {

		if (Activator.getCommandOptionsService().isActivateOnCompileError()) {
			return getBundleErrors(projects, false);
		} else {
			return getBuildErrors(projects, false);
		}
	}

	/**
	 * Check if the specified projects has bundle errors. Bundle errors include missing build state,
	 * manifest file or syntax errors, missing the project description file or is a duplicate of an
	 * external bundle from the most recent build.
	 * 
	 * If one of the specified projects is open but does not exist or closed this is regarded as both
	 * a build and a bundle error and the project is returned instead of throwing an exception.
	 * 
	 * @param projects the {@link IJavaProject} to check for errors
	 * @param includeDuplicates TODO
	 * @return projects with bundle errors
	 * @see #getBuildErrors(Collection, boolean)
	 */
	public static Collection<IProject> getBundleErrors(Collection<IProject> projects,
			boolean includeDuplicates) {

		Collection<IProject> errProjects = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (hasBundleErrors(project, includeDuplicates)) {
				errProjects.add(project);
			}
		}
		return errProjects;
	}

	/**
	 * Check if the {@link IJavaProject} has build errors. Build errors are all errors detected by the
	 * build system. Bundle errors include missing build state, manifest file and syntax errors,
	 * missing a project description file or is a duplicate of an external bundle from the most recent
	 * build.
	 * 
	 * <p>
	 * If one of the specified project is null or not accessible (open but does not exist or closed)
	 * this is regarded as both a build and a bundle error
	 * 
	 * @param projects the {@link IJavaProject} to check for errors
	 * @param includeDuplicates TODO
	 * @return projects with build errors
	 * @see #getBundleErrors(Collection, boolean)
	 */
	public static Collection<IProject> getBuildErrors(Collection<IProject> projects,
			boolean includeDuplicates) {

		Collection<IProject> errors = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (hasBuildErrors(project, includeDuplicates)) {
				errors.add(project);
			}
		}
		return errors;
	}

	/**
	 * Check if the specified project has bundle errors or build errors.
	 * <p>
	 * Check bundle errors if the "Activate on Build Error" option is on and build errors if off.
	 * 
	 * @param project the {@link IJavaProject} to check for errors
	 * @param includeDuplicates true to check for duplicates. Otherwise false
	 * @return <code>true</code> if the "Activate on Build Error" option is on and the projects has
	 * bundle errors and true</code> if the "Activate on Build Error" option is off and the projects
	 * has build errors. Otherwise false
	 * @throws ExtenderException If failing to get command options service
	 * @see #hasBundleErrors(IProject, boolean)
	 * @see #hasBuildErrors(IProject, boolean)
	 */
	public static boolean hasErrors(IProject project, boolean includeDuplicates)
			throws ExtenderException {

		if (Activator.getCommandOptionsService().isActivateOnCompileError()) {
			return hasBundleErrors(project, includeDuplicates);
		} else {
			return hasBuildErrors(project, includeDuplicates);
		}
	}

	/**
	 * Check if the specified projects has bundle errors.
	 * <p>
	 * Bundle errors include:
	 * <ol>
	 * <li>Missing build state {@link #hasBuildState(IProject)}
	 * <li>Has manifest errors {@link #hasManifestBuildErrors(IProject)}
	 * <li>Missing project description file {@link #hasProjectDescriptionFile(IProject)}
	 * <li>Is a duplicate of an external bundle {@link #isExternalDuplicate(IProject)}
	 * <li>Cycles {@link #hasCycles()} between bundles.
	 * </ol>
	 * <p>
	 * Cycles are detected by the builder for build errors - but not bundle errors - and reported as a
	 * build error. To explicit check for cycles between bundles use one of the "hasCycles" methods.
	 * {@link BundleBuildErrorClosure} and {@link ProjectBuildErrorClosure} detects circular
	 * references independent of the builder.
	 * <p>
	 * To check for a specific bundle error, use one of the referenced bundle errors checking methods
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project the {@link IJavaProject} to check for errors
	 * @param includeDuplicates TODO
	 * @return <code>true</code> if the project has bundle errors <code>false</code> otherwise
	 * @see #hasBuildErrors(IProject, boolean)
	 */
	public static boolean hasBundleErrors(IProject project, boolean includeDuplicates) {

		try {
			if (!hasBuildState(project) || hasManifestBuildErrors(project)
					|| !hasProjectDescriptionFile(project)) {
				return true;
			}
			if (includeDuplicates && (isExternalDuplicate(project) || isWorkspaceDuplicate(project))) {
				return true;
			}
		} catch (InPlaceException e) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the {@link IJavaProject} has build or bundle errors.
	 * 
	 * Build errors are all errors detected by the build system plus any bundle errors. See
	 * {@link #hasBundleErrors(IProject, boolean)} for a description of all bundle errors
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project the {@link IJavaProject} to check for errors
	 * @param includeDuplicates TODO
	 * @return <code>true</code> if the project has build errors <code>false</code> otherwise
	 * @see #hasBundleErrors(IProject, boolean)
	 */
	public static boolean hasBuildErrors(IProject project, boolean includeDuplicates) {

		try {
			
//			return hasBundleErrors(project, includeDuplicates) || hasCompileErrors(project) ? true : false; 
			boolean isBundleError = hasBundleErrors(project, includeDuplicates);
			if (isBundleError) {
				return true;
			} else if (hasCompileErrors(project)) {
				return true;
			}
		} catch (InPlaceException e) {
			return true;
		}
		return false;
	}

	public static boolean hasCompileErrors(IProject project) {

		try {
			IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			// check if any of these have a severity attribute that indicates an error
			for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
				if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_INFO)) {
					boolean activateOnCompileErrors = Activator.getCommandOptionsService()
							.isActivateOnCompileError();
					StatusCode statusCode = null;
					String msg = null;
					if (activateOnCompileErrors) {
						statusCode = StatusCode.BUILD_WARNING;
						msg = "Running " + project.getName() + " with compile time errors";
					} else {
						statusCode = StatusCode.BUILD_ERROR;
						msg = "Build problems in project " + project.getName();
					}
					IBundleStatus multiStatus = new BundleStatus(statusCode, Activator.PLUGIN_ID, project,
							msg, null);
					BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;
					bundleTransition.setBuildStatus(project, TransitionError.BUILD, multiStatus);
					return true;
				}
			}
		} catch (CoreException | InPlaceException e) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the specified projects have build state
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param projects to check for build state
	 * @return Set of projects missing build state or an empty set.
	 * 
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
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project to check for build state
	 * @return true if the project has build state and and false if not
	 */
	public static boolean hasBuildState(IProject project) {

		try {

			IJavaProject javaProject = BundleProjectCandidatesImpl.INSTANCE.getJavaProject(project);
			if (null != javaProject && javaProject.hasBuildState()) {
				return true;
			}
		} catch (InPlaceException e) {
		}
		IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project,
				"Missing build state in " + project.getName(), null);
		BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;
		bundleTransition.setBuildStatus(project, TransitionError.BUILD_STATE, multiStatus);
		return false;
	}

	/**
	 * Check if the specified project has a symbolic name that is a duplicate of the symbolic name of
	 * an external bundle
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project The project with a symbolic name to check against external bundles
	 * @return True if the symbolic name of the specified project matches the symbolic name of an
	 * external bundle. Return false if no duplicates are found, the symbolic name of the specified
	 * project could not be obtained, the manifest has syntax errors or if an error occurs while
	 * reading the manifest
	 */
	public static boolean isExternalDuplicate(IProject project) {

		try {
			Activator.getDefault().getDuplicateEvents().symbolicNameDuplicate(project);
		} catch (ExternalDuplicateException e) {
			// Status already set
			return true;
		} catch (InPlaceException e) {
		}
		return false;
	}

	/**
	 * Check if the specified project has a symbolic name and version that is a duplicate of the
	 * symbolic name of a workspace bundle
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project The project with a symbolic name and version to check against workspace bundles
	 * @return True if the symbolic name and version of the specified project matches the symbolic
	 * name of another workspace bundle. Return false if no duplicates are found, the symbolic name of
	 * the specified project could not be obtained, the manifest has syntax errors or if an error
	 * occurs while reading the manifest
	 */
	public static boolean isWorkspaceDuplicate(IProject project) {

		try {
			BundleRegion br = WorkspaceRegionImpl.INSTANCE;
			br.workspaceDuplicate(project);
		} catch (WorkspaceDuplicateException e) {
			// Status already set
			return true;
		}
		return false;
	}

	/**
	 * Check for circular references among all workspace bundle projects
	 * <p>
	 * If a specified project is null or not accessible (open but does not exist or closed) it is
	 * ignored and will result an incomplete graph if there are any projects having requirements on
	 * the ignored project
	 * 
	 * @return true if any cycles are found among all workspace region bundle projects. Otherwise
	 * false.
	 */
	public static boolean hasCycles() {

		try {
			Collection<IProject> projects = WorkspaceRegionImpl.INSTANCE.getProjects();
			ProjectSorter ps = new ProjectSorter();
			ps.sortRequiringProjects(projects);
			return false;
		} catch (CircularReferenceException e) {
		}
		return true;
	}

	/**
	 * Check if the specified project is involved in a circular reference
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) it is
	 * ignored and will result an incomplete graph if there are any projects having requirements on
	 * the ignored project
	 * 
	 * @return true if any cycles are found involving the specified bundle project. Otherwise false.
	 */
	public static boolean hasCycles(IProject project) {

		try {
			ProjectSorter ps = new ProjectSorter();
			ps.sortRequiringProjects(Collections.<IProject> singletonList(project));
			ps.sortProvidingProjects(Collections.<IProject> singletonList(project));
			return false;
		} catch (CircularReferenceException e) {
			// BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE;
			// String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			// IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
			// project, msg, e);
			// multiStatus.add(e.getStatusList());
			// bundleRegion.setBuildStatus(project, TransitionError.BUILD_CYCLE, multiStatus);
		}
		return true;
	}

	/**
	 * Check if the specified projects are involved in circular references
	 * <p>
	 * If a specified project is null or not accessible (open but does not exist or closed) it is
	 * ignored and will result an incomplete graph if there are any projects having requirements on
	 * the ignored project
	 * 
	 * @return true if any cycles are found involving the specified bundle projects. Otherwise false.
	 */
	public static boolean hasCycles(Collection<IProject> projects) {

		try {
			ProjectSorter ps = new ProjectSorter();
			ps.sortRequiringProjects(projects);
			ps.sortProvidingProjects(projects);
			return false;
		} catch (CircularReferenceException e) {
		}
		return true;
	}

	/**
	 * Check for existence and build errors in the manifest file in the specified project
	 * <p>
	 * Assumes that the manifest file is located at the default location
	 * <p>
	 * If the specified project is null or not accessible (open but does not exist or closed) this is
	 * regarded as both a build and a bundle error and {@code true} is returned
	 * 
	 * @param project to check for the existence and build errors in the manifest file at the default
	 * location
	 * @return true if the manifest contains build errors and false otherwise
	 * @see CachedManifestOperationsImpl#hasManifest(IProject)
	 * @see BundleProjectMeta#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectMeta#MANIFEST_FILE_NAME
	 */
	public static boolean hasManifestBuildErrors(IProject project) {

		try {
			if (null == project || !project.isAccessible()) {
				return true;
			}
			IFile manifestFile = BundleProjectMetaImpl.INSTANCE.getManifestFile(project);
			if (null != manifestFile && manifestFile.exists()) {
				IMarker[] problems = manifestFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
				for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
					if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
							IMarker.SEVERITY_INFO)) {
						IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
								project, "Error in manifest for " + project.getName(), null);
						BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;
						bundleTransition.setBuildStatus(project, TransitionError.BUILD_MANIFEST, multiStatus);
						return true;
					}
				}
			}
		} catch (CoreException e) {
			// Unreachable
		}

		return false;
	}

	/**
	 * Check for existence of a project description file and validates the different at the default
	 * location in the specified project
	 * <p>
	 * Note that the project is defined to miss a description file if it is null or not accessible
	 * (open but not existing or closed)
	 * 
	 * @param project to check for the existence of a project description file at the default location
	 * @return true if the project has a description at the default location and false otherwise
	 * @see BundleProjectMeta#PROJECT_META_FILE_NAME
	 */
	public static Boolean hasProjectDescriptionFile(IProject project) {

		if (null != project && project.isAccessible()) {
			IFile projectDesc = project.getFile(BundleProjectMetaImpl.PROJECT_META_FILE_NAME);
			if (projectDesc.exists()) {
				return true;
			}
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
					project, "Missing description file in " + project.getName(), null);
			BundleTransition bundleTransition = BundleTransitionImpl.INSTANCE;
			bundleTransition.setBuildStatus(project, TransitionError.BUILD_DESCRIPTION_FILE, multiStatus);
		}
		return false;
	}

	private BundleProjectBuildError() {
	}
}
