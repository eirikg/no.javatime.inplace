package no.javatime.inplace.region.project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

public class BundleProjectState {

	final public static String PLUGIN_NATURE_ID = "org.eclipse.pde.PluginNature";
	final public static String JAVATIME_NATURE_ID = "no.javatime.inplace.builder.javatimenature";

	final public static String BUNDLE_REF_LOC_SCHEME = Message.getInstance().formatString(
			"bundle_identifier_reference_scheme");
	final public static String BUNDLE_FILE_LOC_SCHEME = Message.getInstance().formatString(
			"bundle_identifier_file_scheme");


	/**
	 * Filters out all projects in the workspace registered with the JavaTime nature
	 * 
	 * @return a list of projects with the JavaTime nature or an empty collection
	 */
	public static Collection<IProject> getActivatedProjects() {
	
		Collection<IProject> projects = new LinkedHashSet<IProject>();
	
		for (IProject project : getProjects()) {
			if (isProjectActivated(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	/**
		 * When a project has JavaTime nature enabled the project is activated.
		 * 
		 * @param project to check for JavaTime nature
		 * @return true if JavaTime nature is enabled for the project and false if not
		 * @see no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl#isActivated(IProject)
		 * @see no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl#isActivated(Long)
		 * @see no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl#isActivated(Bundle)
		 */
		public static Boolean isProjectActivated(IProject project) {
			try {
				if (null != project && project.isNatureEnabled(JAVATIME_NATURE_ID)) {
					return true;
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			return false;
		}

	/**
	 * Check if a project is JavaTime nature enabled (activated). The condition is satisfied if one workspace
	 * project is activated. This only implies that one or more projects are JavaTime enabled, and does not
	 * necessary mean that the corresponding bundle is activated.
	 * 
	 * @return true if at least one project is activated or false if no projects are activated
	 */
	public static Boolean isProjectWorkspaceActivated() {
		for (IProject project : getProjects()) {
			if (isProjectActivated(project)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if there are build errors from the most recent build.
	 * 
	 * @return cancel list of projects with errors or an empty list
	 * @throws InPlaceException if one of the specified projects does not exist or is closed
	 */
	public static Collection<IProject> getBuildErrors(Collection<IProject> projects) throws InPlaceException {
		Collection<IProject> errors = new LinkedHashSet<IProject>();
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
	 *         <code>false</code> otherwise
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
			IJavaProject javaProject = getJavaProject(project.getName());
			if (javaProject.hasBuildState()) {
				return true;
			}
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
		Collection<IProject> missingBuildState = new LinkedHashSet<IProject>();
		for (IProject project : projects) {
			if (!hasBuildState(project)) {
				missingBuildState.add(project);
			}
		}
		return missingBuildState;
	}

	public static Boolean isFragment(IProject project) throws InPlaceException {
	
		IBundleProjectDescription bundleProjDesc = Activator.getDefault().getBundleDescription(project);
		IHostDescription host = bundleProjDesc.getHost();
		if (null != host) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get all open and closed projects in workspace
	 * 
	 * @return a list of all projects in workspace
	 */
	public static Collection<IProject> getProjects() {
		return Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
	}

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param projectName name of a given project
	 * @return the java project or null if the given project is null
	 * @throws InPlaceException
	 * @throws InPlaceException if the project does not exist or is not open
	 */
	public static IJavaProject getJavaProject(String projectName) throws InPlaceException{
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (null == project) {
			return null;
		}
		return getJavaProject(project);
	}

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param project a given project
	 * @return the java project or null if the given project is null
	 * @throws InPlaceException if the project does not exist or is not open
	 */
	public static IJavaProject getJavaProject(IProject project) throws InPlaceException {
		IJavaProject javaProject = null;
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
			throw new InPlaceException("project_not_accessible", e);
		}
		return javaProject;
	}

	/**
	 * Formats the collection as a comma separated list.
	 * 
	 * @param projects collection of projects to format
	 * @return a comma separated list of projects or an empty string
	 */
	public static String formatProjectList(Collection<IProject> projects) {
		StringBuffer sb = new StringBuffer();
		if (null != projects && projects.size() >= 1) {
			for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
				IProject project = iterator.next();
				sb.append(project.getName());
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Format and report projects with build errors and their requiring projects
	 * 
	 * @param errorClosure error closures containing projects with build errors and their requiring projects
	 * @return null if no errors, otherwise the error message
	 */
	static public String formatBuildErrorsFromClosure(Collection<IProject> errorClosure, String name) {
		String msg = null;
		if (errorClosure.size() > 0) {
			Collection<IProject> errorProjects = getBuildErrors(errorClosure);
			errorProjects.addAll(hasBuildState(errorClosure));
			Collection<IProject> closure = new ArrayList<IProject>(errorClosure);
			closure.removeAll(errorProjects);
			if (closure.size() > 0) {
				msg = WarnMessage.getInstance().formatString("build_errors_with_requring", name,
						formatProjectList(errorProjects), formatProjectList(closure));
			} else if (errorClosure.size() > 0) {
				msg = WarnMessage.getInstance().formatString("build_errors", name, formatProjectList(errorProjects));
			}
		}
		return msg;
	}

	/**
	 * Retrieves the project location identifier as an absolute file system path of the specified project
	 * prepended with the specified URI scheme. Uses the platform-dependent path separator. This method
	 * is used internally with the {@link #BUNDLE_REF_LOC_SCHEME} scheme when bundles are installed.
	 * <p>
	 * After a bundle is installed the path returned from {@linkplain Bundle#getLocation()} equals the path
	 * returned from this method when the reference scheme parameter is used. This method use
	 * {@linkplain IProject#getLocation()} internally.
	 * 
	 * @param project which is the base for finding the path
	 * @param locationScheme a valid URI location scheme
	 * @return the absolute file system path of the project prepended with the specified URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found
	 * @see IProject#getLocation()
	 * @see Bundle#getLocation()
	 * @see #BUNDLE_REF_LOC_SCHEME
	 * @see #BUNDLE_FILE_LOC_SCHEME
	 */
	public static String getLocationIdentifier(IProject project, String locationScheme)
			throws ProjectLocationException {
		if (null == project) {
			throw new ProjectLocationException("project_null_location");
		}
		StringBuffer scheme = new StringBuffer(locationScheme);
		IPath path = project.getLocation();
		if (null == path || path.isEmpty()) {
			throw new ProjectLocationException("project_location_find", project.getName());
		}
		String locIdent = path.toOSString();
		return scheme.append(locIdent).toString();
	}

	/**
	 * Find a project based on a location identifier.
	 * 
	 * @param location identifier string prepended with the file protocol (by value)
	 * @param locationScheme a valid URI location scheme
	 * @return the project or null if not found
	 * @see #getLocationIdentifier(IProject, String)
	 * @see #BUNDLE_REF_LOC_SCHEME
	 * @see #BUNDLE_FILE_LOC_SCHEME
	 */
	public static IProject getProjectFromLocation(String location, String locationScheme) {
	
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
	
		for (IProject project : root.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.hasNature(PLUGIN_NATURE_ID) && project.isOpen()) {
					URL pLoc = new URL(getLocationIdentifier(project, locationScheme));
					URL bLoc = new URL(location);
					if (Category.DEBUG) {
						TraceMessage.getInstance().getString("display", "Project location: 	" + pLoc.getPath());
						TraceMessage.getInstance().getString("display", "Bundle  location: 	" + bLoc.getPath());
					}
					if (pLoc.getPath().equalsIgnoreCase(bLoc.getPath())) {
						return project;
					}
				}
			} catch (ProjectLocationException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e),
						StatusManager.LOG);
			} catch (CoreException e) {
				// Ignore closed or non-existing project
				String msg = WarnMessage.getInstance().formatString("project_missing_at_location", location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			} catch (MalformedURLException e) {
				String msg = ExceptionMessage.getInstance()
						.formatString("project_location_malformed_error", location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			}
		}
		return null;
	}
}
