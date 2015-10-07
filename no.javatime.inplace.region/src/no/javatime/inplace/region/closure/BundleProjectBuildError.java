package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectCandidatesImpl;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Utility to discover build errors in bundle projects.
 * <p>
 * Build errors are divided in to two categorizes;
 * <ol>
 * <li>Build errors which include all build errors reported by the build system; and
 * <li>Bundle errors which include errors in projects with no build state or build errors in project
 * manifest files
 * </ol>
 * <p>
 * Compile time errors is for now defined the difference between build errors and bundle errors. An
 * exception is bundle duplicates.
 * <p>
 * It is possible to:
 * <ol>
 * <li>Install bundles as long as a project has a build state
 * <li>Resolve bundles as long as there are no errors in the manifest file of a project
 * <li>Start and stop a bundle as long as the execution path does not encounter any compile time
 * errors. In this case a runtime exception is thrown.
 * </ol>
 */
public class BundleProjectBuildError {

	/**
	 * Check if the specified project has build state or there are build errors from the most recent
	 * build.
	 * 
	 * @return List of projects with build errors or an empty list
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	public static Collection<IProject> getBuildErrors(Collection<IProject> projects)
			throws InPlaceException {

		Collection<IProject> errors = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (!hasBuildState(project) || hasBuildErrors(project)) {
				errors.add(project);
			}
		}
		return errors;
	}

	/**
	 * Check if the specified projects have build state or there are errors in manifest files from the
	 * most recent build.
	 * 
	 * @return List of projects with bundle errors or an empty list
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	public static Collection<IProject> getBundleErrors(Collection<IProject> projects)
			throws ExtenderException, InPlaceException, CircularReferenceException {

		Collection<IProject> errProjects = new LinkedHashSet<>();
		for (IProject project : projects) {
			if (hasBundleErrors(project)) {
				errProjects.add(project);
			}
		}
		return errProjects;
	}

	/**
	 * Check if the specified project has build state or there are errors in the manifest file from the
	 * most recent build.
	 * 
	 * @param project the {@link IJavaProject} to check for errors
	 * @return <code>true</code> if the project has bundle errors (or has never been built),
	 * <code>false</code> otherwise
	 * @throws InPlaceException if project does not exist or is closed
	 */
	public static boolean hasBundleErrors(IProject project)
			throws ExtenderException, InPlaceException, CircularReferenceException {

		if (!hasBuildState(project)
				|| hasManifestBuildErrors(project) || !hasProjectDescription(project)) {
			return true;
		}
		return false;
	}


	/**
	 * Check if the {@link IJavaProject} has build errors.
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
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown (should not be the case)
	 */
	public static boolean hasBuildState(IProject project) throws InPlaceException {

		IJavaProject javaProject = BundleProjectCandidatesImpl.INSTANCE.getJavaProject(project);
		if (null != javaProject && javaProject.hasBuildState()) {
			return true;
		}
		return false;
	}

	/**
	 * Check for existence and build errors in the manifest file in the specified project
	 * <p>
	 * Assumes that the manifest file is located at the default location
	 * 
	 * @param project to check for the existence and build errors in the manifest file at the default
	 * location
	 * @return true if the manifest file does not exist or contains build errors and false otherwise
	 * @throws InPlaceException if a core exception occurs. The exception contains a status object
	 * describing the failure
	 * @see #hasManifest(IProject)
	 * @see BundleProjectMeta#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectMeta#MANIFEST_FILE_NAME
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
					if (resource instanceof IFile
							&& resource.getName().equals(BundleProjectMetaImpl.MANIFEST_FILE_NAME)) {
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
	 * @see BundleProjectMeta#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectMeta#MANIFEST_FILE_NAME
	 */
	public static Boolean hasManifest(IProject project) {
		if (null != project && project.isAccessible()) {
			IFile manifestFile = project.getFile(BundleProjectMeta.MANIFEST_RELATIVE_PATH
					+ BundleProjectMetaImpl.MANIFEST_FILE_NAME);
			if (manifestFile.exists()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check for existence of a project description file at the default location in the specified project
	 * 
	 * @param project to check for the existence of a project description file at the default location
	 * @return true if the project description file exist at the default location and false otherwise
	 * @see BundleProjectMeta#PROJECT_META_FILE_NAME
	 */
	public static Boolean hasProjectDescription(IProject project) {
		if (null != project && project.isAccessible()) {
			IFile projectDesc = project.getFile(BundleProjectMetaImpl.PROJECT_META_FILE_NAME);
			if (projectDesc.exists()) {
				return true;
			}
		}
		return false;
	}

	private BundleProjectBuildError() {
	}
}
