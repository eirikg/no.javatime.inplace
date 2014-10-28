package no.javatime.inplace.region.project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

public class BundleProjectState {

	final public static String PLUGIN_NATURE_ID = "org.eclipse.pde.PluginNature";
	final public static String JAVATIME_NATURE_ID = "no.javatime.inplace.builder.javatimenature";

	final public static String BUNDLE_REF_LOC_SCHEME = Msg.BUNDLE_ID_REF_SCHEME_REF; 
	final public static String BUNDLE_FILE_LOC_SCHEME = Msg.BUNDLE_ID_FILE_SCHEME_REF; 


	public static Boolean isFragment(IProject project) throws InPlaceException {
	
		IBundleProjectDescription bundleProjDesc = Activator.getDefault().getBundleDescription(project);
		IHostDescription host = bundleProjDesc.getHost();
		return null != host ? true : false;
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
				String msg = NLS.bind(Msg.PROJECT_MISSING_AT_LOC_WARN, location);
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

	/**
	 * Convert from java projects to general projects
	 * 
	 * @param javaProjects set of java projects
	 * @return set of projects
	 */
	public static Collection<IProject> toProjects(Collection<IJavaProject> javaProjects) {
	
		Collection<IProject> projects = new ArrayList<IProject>();
		for (IJavaProject javaProject : javaProjects) {
			IProject project = javaProject.getProject();
			if (null != project) {
				projects.add(project);
			}
		}
		return projects;
	}

	/**
	 * Convert from general projects to java projects
	 * 
	 * @param projects set of general projects
	 * @return set of java projects
	 */
	public static Collection<IJavaProject> toJavaProjects(Collection<IProject> projects) {
	
		Collection<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		for (IProject project : projects) {
			IJavaProject javaProject = JavaCore.create(project);
			if (null != javaProject) {
				javaProjects.add(javaProject);
			}
		}
		return javaProjects;
	}

	/**
	 * Get a project based on it's name.
	 * 
	 * @param name a project name
	 * @return the project or null if the given project is null
	 */
	public static IProject getProject(String name) {
		if (null == name) {
			throw new InPlaceException("project_null");
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject p = root.getProject(name);
		return p;
	}
}
