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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.ui.statushandlers.StatusManager;

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

	/**
	 * Find all projects that fulfill the requirements to be activated. Already activated projects and projects
	 * that contributes to the UI when UI contributors are not allowed are not part of the returned collection
	 * 
	 * @return all projects that may be activated or an empty collection
	 * @see #isBundleCandidate(IProject)
	 */
	public static Collection<IProject> getCandidateProjects() {

		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : BundleProjectState.getProjects()) {
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)
						&& !BundleProjectState.isProjectActivated(project)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			try {
				if (!InPlace.get().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIContributors());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (InPlaceException e) {
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
		for (IProject project : BundleProjectState.getProjects()) {
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			try {
				if (!InPlace.get().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIContributors());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (InPlaceException e) {
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
		for (IProject project : BundleProjectState.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
		}
		return projects;
	}

	/**
	 * Checks if this project has the Java and plug-in nature enabled.
	 * 
	 * @return true if this is a Java plug-in project or false
	 */
	public static Boolean isPlugIn(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
				return true;
			}
		} catch (CoreException e) {
			// Ignore closed and non-existing project
		}
		return false;
	}

	/**
	 * Checks if this project has the Java and plug-in or the JavaTime nature enabled. Projects in the UI name space are
	 * included if UI contributed projects are allowed.
	 * 
	 * @return true if this is a Java plug-in or JavaTime project or false
	 */
	public static Boolean isInstallable(IProject project) {

		if (isBundleCandidate(project) || BundleProjectState.isProjectActivated(project)) {
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
	public static Boolean isBundleCandidate(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)
					&& !BundleProjectState.isProjectActivated(project)) {
				if (InPlace.get().getCommandOptionsService().isAllowUIContributions()) {
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
		}	catch (InPlaceException e) {
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
		for (IProject project : BundleProjectState.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)
						&& isUIContributor(project)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			} catch (InPlaceException e) {
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
	 * @throws InPlaceException if project is null or failed to get the bundle project description
	 */
	public static Boolean isUIContributor(IProject project) throws InPlaceException {

		if (null == project) {
			throw new InPlaceException("project_null_location");
		}
		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
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
			throw new InPlaceException("project_null");
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject p = root.getProject(name);
		return p;
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

	public static boolean hasManifestBuildErrors(IProject project) throws InPlaceException {
		
		try {
			if (!BundleProjectSettings.hasManifest(project)) {
				return true;
			}
			IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			// check if any of these have a severity attribute that indicates an error
			for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
				if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_INFO)) {
					IResource resource = problems[problemsIndex].getResource();
					if (resource instanceof IFile && resource.getName().equals(ManifestOptions.MANIFEST_FILE_NAME)) {
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
			throw new InPlaceException(e, "error_set_autobuild");
		}
		return autoBuilding;
	}
}
