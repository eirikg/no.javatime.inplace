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
package no.javatime.inplace.bundleproject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.builder.JavaTimeNature;
import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.bundlemanager.ProjectLocationException;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Utility to:
 * <ol>
 * <li>Get activation status of the workspace
 * <li>Retrieve workspace projects and differentiate them by nature.
 * <li>Get bundle location from projects.
 * <li>Switch between general and java projects
 * <li>Check build state of projects and state of the build automatic switch
 * <li>Get selected project in package explorer, project explorer and bundle list page
 * </ol>
 * <p>
 * The workspace is activated if one or more projects have the JavaTime nature.
 */
public class ProjectProperties {

	final public static String PLUGIN_NATURE_ID = "org.eclipse.pde.PluginNature";
	final public static String PACKAGE_EXPLORER_ID = org.eclipse.jdt.ui.JavaUI.ID_PACKAGES;
	final public static String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";

	final public static String bundleReferenceLocationScheme = Message.getInstance().formatString(
			"bundle_identifier_reference_scheme");
	final public static String bundleFileLocationScheme = Message.getInstance().formatString(
			"bundle_identifier_file_scheme");
	final public static String MANIFEST_FILE_NAME = Message.getInstance().formatString("manifest_file_name");

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
	 * Get all open and closed projects in workspace
	 * 
	 * @return a list of all projects in workspace
	 */
	public static Collection<IProject> getProjects() {
		return Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
	}

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
	 * Find all projects that fulfill the requirements to be activated. Already activated projects and projects
	 * that contributes to the UI when UI contributors are not allowed are not part of the returned collection
	 * 
	 * @return all projects that may be activated or an empty collection
	 * @see #isCandidateProject(IProject)
	 */
	public static Collection<IProject> getCandidateProjects() {

		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : getProjects()) {
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
						&& !isProjectActivated(project)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			try {
				if (!InPlace.getDefault().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIContributors());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
				// assume allow ui contributers
			}
		}
		return projects;
	}

	/**
	 * Return all plug-in projects that have Java and plug-in nature enabled and not contributes to the UI if UI
	 * contributors are not allowed
	 * 
	 * @return all plug-in projects with Java, plug-in nature and not contributing to the UI, when not allowed
	 *         or an empty collection
	 */
	public static Collection<IProject> getInstallableProjects() {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			try {
				if (!InPlace.getDefault().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIContributors());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
				// assume allow ui contributers
			}
		}
		return projects;
	}

	/**
	 * Return all plug-in projects that have the Java and plug-in nature enabled. 
	 * Closed and non-existing projects are discarded
	 * 
	 * @return all plug-in projects with Java and plug-in nature or an empty collection
	 */
	public static Collection<IProject> getPlugInProjects() {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
		}
		return projects;
	}

	/**
	 * When a project has JavaTime nature enabled the project is activated.
	 * 
	 * @param project to check for JavaTime nature
	 * @return true if JavaTime nature is enabled for the project and false if not
	 * @see no.javatime.inplace.bundlemanager.BundleWorkspaceImpl#isActivated(IProject)
	 * @see no.javatime.inplace.bundlemanager.BundleWorkspaceImpl#isActivated(Long)
	 * @see no.javatime.inplace.bundlemanager.BundleWorkspaceImpl#isActivated(Bundle)
	 */
	public static Boolean isProjectActivated(IProject project) {
		try {
			if (null != project && project.isNatureEnabled(JavaTimeNature.JAVATIME_NATURE_ID)) {
				return true;
			}
		} catch (CoreException e) {
			// Ignore closed or non-existing project
		}
		return false;
	}
	/**
	 * Checks if this project has the Java and plug-in nature enabled.
	 * 
	 * @return true if this is a Java plug-in project or false
	 */
	public static Boolean isPlugInProject(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)) {
				return true;
			}
		} catch (CoreException e) {
			// Ignore closed or non-existing project
		}
		return false;
	}

	/**
	 * Checks if this project has the Java and plug-in or the JavaTime nature enabled. Projects in the UI name space are
	 * included if UI contributed projects are allowed.
	 * 
	 * @return true if this is a Java plug-in or JavaTime project or false
	 */
	public static Boolean isInstallableProject(IProject project) {

		if (isCandidateProject(project) || isProjectActivated(project)) {
			return true;
		}
		return false;
	}

	/**
	 * A project is a candidate project if it is not activated, has the plug-in and Java nature enabled, and not
	 * contributes to the UI if UI contributors are not allowed
	 * 
	 * @param project to check for candidate nature attributes
	 * @return true if project is not activated, a plug-in project with Java, plug-in nature and not
	 *         contributing to the UI, when not allowed. Otherwise false.
	 */
	public static Boolean isCandidateProject(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
					&& !isProjectActivated(project)) {
				if (InPlace.getDefault().getCommandOptionsService().isAllowUIContributions()) {
					return true;
				} else {
					Collection<IProject> uiContributors = getUIContributors();
					if (uiContributors.contains(project)) {
						return false;
					} else {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			// Ignore closed or non-existing project
		} catch (CircularReferenceException e) {
			// Ignore. Cycles are detected in any bundle job
		}	catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			// assume not candidate
		}

		return false;
	}

	/**
	 * Get all plug-in projects that contributes to the UI and their requiring projects
	 * 
	 * @return set of plug-in projects contributing to the UI or an empty collection
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public static Collection<IProject> getUIContributors() throws CircularReferenceException {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
						&& contributesToTheUI(project)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			} catch (ExtenderException e) {
				// Ignore problems getting project description
			}
		}
		// Get requiring projects of UI contributors
		if (projects.size() > 0) {
			ProjectSorter bs = new ProjectSorter();
			Collection<IProject> reqProjects = bs.sortRequiringProjects(projects);
			projects.addAll(reqProjects);
		}
		return projects;
	}

	/**
	 * Check if this project is dependent on the Eclipse UI plug-in (org.eclipse.ui)
	 * 
	 * @param project to check for dependency on the UI plug-in
	 * @return true if this project is dependent on the UI plug-in, otherwise false
	 * @throws ExtenderException if project is null or failed to get the bundle project description
	 */
	public static Boolean contributesToTheUI(IProject project) throws ExtenderException {

		if (null == project) {
			throw new ExtenderException("project_null_location");
		}
		IBundleProjectDescription bundleProjDesc = InPlace.getDefault().getBundleDescription(project);
		if (null == bundleProjDesc) {
			return false;
		}
		IRequiredBundleDescription[] bd = bundleProjDesc.getRequiredBundles();
		if (null == bd) {
			return false;
		}
		for (int i = 0; i < bd.length; i++) {
			if (bd[i].getName().equals("org.eclipse.ui")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param projectName name of a given project
	 * @return the java project or null if the given project is null
	 * @throws ExtenderException
	 * @throws ExtenderException if the project does not exist or is not open
	 */
	public static IJavaProject getJavaProject(String projectName) throws ExtenderException{
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
	 * @throws ExtenderException if the project does not exist or is not open
	 */
	public static IJavaProject getJavaProject(IProject project) throws ExtenderException {
		IJavaProject javaProject = null;
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
			throw new ExtenderException("project_not_accessible", e);
		}
		return javaProject;
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
	 * Get a project based on it's name.
	 * 
	 * @param name a project name
	 * @return the project or null if the given project is null
	 */
	public static IProject getProject(String name) {
		if (null == name) {
			throw new ExtenderException("project_null");
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject p = root.getProject(name);
		return p;
	}

	/**
	 * Find a project based on a location identifier.
	 * 
	 * @param location identifier string prepended with the file protocol (by value)
	 * @param referenceScheme true if the path is by reference (path prepended with: reference:file:/) and false
	 *          (path prepended with: file:/) if by value
	 * @return the project or null if not found
	 */
	public static IProject getProjectFromLocation(String location, boolean referenceScheme) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		for (IProject project : root.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.hasNature(PLUGIN_NATURE_ID) && project.isOpen()) {
					URL pLoc = new URL(getProjectLocationIdentifier(project, referenceScheme));
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
						new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, e.getLocalizedMessage(), e),
						StatusManager.LOG);
			} catch (CoreException e) {
				// Ignore closed or non-existing project
				String msg = WarnMessage.getInstance().formatString("project_missing_at_location", location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			} catch (MalformedURLException e) {
				String msg = ExceptionMessage.getInstance()
						.formatString("project_location_malformed_error", location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			}
		}
		return null;
	}

	/**
	 * Retrieves the project location identifier as an absolute file system path of the specified project
	 * prepended with the reference and/or file scheme. Uses the platform-dependent path separator. This method
	 * is used internally with the specified reference scheme set to {@code true} when bundles are installed.
	 * <p>
	 * After a bundle is installed the path returned from {@linkplain Bundle#getLocation()} equals the path
	 * returned from this method with the reference scheme parameter set to {@code true}. This method use
	 * {@linkplain IProject#getLocation()} internally.
	 * 
	 * @param project which is the base for finding the path
	 * @param referenceScheme true if the path is by reference (path prepended with: reference:file:/) and false
	 *          (path prepended with: file:/) if by value
	 * @return the absolute file system path of the project prepended with the specified URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found
	 * @see IProject#getLocation()
	 * @see Bundle#getLocation()
	 */
	public static String getProjectLocationIdentifier(IProject project, Boolean referenceScheme)
			throws ProjectLocationException {
		if (null == project) {
			throw new ProjectLocationException("project_null_location");
		}
		StringBuffer locScheme = null;
		if (referenceScheme) {
			locScheme = new StringBuffer(bundleReferenceLocationScheme);
		} else {
			locScheme = new StringBuffer(bundleFileLocationScheme);
		}
		IPath path = project.getLocation();
		if (null == path || path.isEmpty()) {
			throw new ProjectLocationException("project_location_find", project.getName());
		}
		String locIdent = path.toOSString();
		return locScheme.append(locIdent).toString();
	}

	/**
	 * Finds all source class path entries and return the relative path of source folders
	 * 
	 * @param project with source folders
	 * @return source folders or an empty collection
	 * @throws JavaModelException when accessing the project resource or the class path element does not exist
	 * @throws ExtenderException if the project could not be accessed
	 */
	public static Collection<IPath> getJavaProjectSourceFolders(IProject project) throws JavaModelException,
			ExtenderException {
		ArrayList<IPath> paths = new ArrayList<IPath>();
		IJavaProject javaProject = getJavaProject(project);
		IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry entry = classpathEntries[i];
			if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
				IPath path = entry.getPath();
				String segment = path.segment(path.segmentCount() - 1);
				if (null != segment) {
					paths.add(new Path(segment));
				}
			}
		}
		return paths;
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
	 * Check if there are build errors from the most recent build.
	 * 
	 * @return cancel list of projects with errors or an empty list
	 * @throws ExtenderException if one of the specified projects does not exist or is closed
	 */
	public static Collection<IProject> getBuildErrors(Collection<IProject> projects) throws ExtenderException {
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
	 * @throws ExtenderException if project does not exist or is closed
	 */
	public static boolean hasBuildErrors(IProject project) throws ExtenderException {

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
			throw new ExtenderException(e, "has_build_errors", project);
		}
		return false;
	}

	public static boolean hasManifestBuildErrors(IProject project) throws ExtenderException {

		try {
			if (!BundleProject.hasManifest(project)) {
				return true;
			}
			IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			// check if any of these have a severity attribute that indicates an error
			for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
				if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_INFO)) {
					IResource resource = problems[problemsIndex].getResource();
					if (resource instanceof IFile && resource.getName().equals(MANIFEST_FILE_NAME)) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			throw new ExtenderException(e, "manifest_has_errors", project);
		}
		return false;
	}

	/**
	 * Checks if all the projects have build state
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

	/**
	 * Checks if the project has build state
	 * 
	 * @param project to check for build state
	 * @return true if the project has build state, otherwise false
	 */
	public static boolean hasBuildState(IProject project) throws ExtenderException {

		if (null == project) {
			throw new ExtenderException("null_project_build_state");
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
	 * If one or more of the workspace projects have build errors, issue a warning and return the error closure.
	 * Adds a job build error if one or more projects have build errors.
	 * 
	 * @param projectScope set of projects to search for errors in. If scope is null or empty, search in all
	 *          activated projects
	 * @param activated if true only consider activated bundles. If false consider all projects
	 * @return projects with errors and their requiring projects (error closure) or an empty collection
	 */
	static public Collection<IProject> reportBuildErrorClosure(Collection<IProject> projectScope, String name) {
		ProjectSorter ps = new ProjectSorter();
		if (null == projectScope || projectScope.size() == 0) {
			projectScope = getInstallableProjects();
		}
		Collection<IProject> errorClosure = ps.getRequiringBuildErrorClosure(projectScope);
		String msg = formatBuildErrorsFromClosure(errorClosure, name);
		if (null != msg) {
			String warnMsg = WarnMessage.getInstance().formatString(WarnMessage.defKey, msg);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, warnMsg),
					StatusManager.LOG);
		}
		return errorClosure;
	}

	/**
	 * If one or more of the workspace projects have build errors, issue a warning and return the error closure.
	 * Adds a job build error if one or more projects have build errors.
	 * 
	 * @param projectScope set of projects to search for errors in. If scope is null or empty, search in all
	 *          activated projects
	 * @param activated if true only consider activated bundles. If false consider all projects
	 * @return projects with errors and their requiring projects (error closure) or an empty collection
	 */
	static public Collection<IProject> reportBuildErrorClosure(Collection<IProject> projectScope,
			Boolean activated, String name) {

		ProjectSorter bs = new ProjectSorter();
		if (null == projectScope || projectScope.size() == 0) {
			if (activated) {
				projectScope = getActivatedProjects();
			} else {
				projectScope = getCandidateProjects();
			}
		}
		Collection<IProject> errorClosure = bs.getRequiringBuildErrorClosure(projectScope, activated);
		String msg = formatBuildErrorsFromClosure(errorClosure, name);
		if (null != msg) {
			String warnMsg = WarnMessage.getInstance().formatString(WarnMessage.defKey, msg);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, warnMsg),
					StatusManager.LOG);
		}
		return errorClosure;
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
	 * A delegate for checking if auto build is enabled
	 * 
	 * @return true if auto build is enabled, false if not
	 */
	public static Boolean isAutoBuilding() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		return workspace.isAutoBuilding();
	}

	/**
	 * Enables auto build
	 * 
	 * @param autoBuild true to enable and false to disable
	 * @return previous state of auto build
	 */
	public static Boolean setAutoBuild(Boolean autoBuild) {
		Boolean autoBuilding = isAutoBuilding();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			IWorkspaceDescription desc = workspace.getDescription();
			desc.setAutoBuilding(autoBuild);
			workspace.setDescription(desc);
		} catch (CoreException e) {
			throw new ExtenderException(e, "error_set_autobuild");
		}
		return autoBuilding;
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
}
