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
package no.javatime.inplace.region.project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;

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
public class BundleProjectCandidatesImpl implements BundleProjectCandidates {
	
	public final static BundleProjectCandidatesImpl INSTANCE = new BundleProjectCandidatesImpl();

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getPlugIns()
	 */
	public Collection<IProject> getBundleProjects() throws InPlaceException {
	
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			if (isBundleProject(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	public Collection<IProject> getProjects() {
	
		Collection<IProject> openProjects = new LinkedHashSet<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (projects[i].isAccessible()) {
				openProjects.add(projects[i]);
			}
		}
		return openProjects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getCandidates()
	 */
	public Collection<IProject> getCandidates() throws InPlaceException {

		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : getProjects()) {
			if (isBundleProject(project)
					&& !WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
				projects.add(project);
			}
			try {
				if (!Activator.getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIPlugins());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (ExtenderException e) {
				throw new InPlaceException(e);
			}
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getInstallable()
	 */
	public Collection<IProject> getInstallable() throws InPlaceException {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			if (isBundleProject(project)) {
				projects.add(project);
			}
			try {
				if (!Activator.getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIPlugins());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (ExtenderException e) {
				throw new InPlaceException(e);
			}
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getUIContributors()
	 */
	public Collection<IProject> getUIPlugins() throws InPlaceException, CircularReferenceException {
	
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			if (isBundleProject(project) && isUIPlugin(project)) {
				projects.add(project);
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

	public IProject getProject(String name) {
		if (null == name) {
			throw new InPlaceException(ExceptionMessage.getInstance().getString("project_null"));
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject p = root.getProject(name);
		return p;
	}

	public IProject getProjectFromLocation(String location, String locationScheme) 
			throws ProjectLocationException, InPlaceException, MalformedURLException {
	
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
	
		for (IProject project : root.getProjects()) {
//			try {
				if (isBundleProject(project)) {
					URL pLoc = new URL(WorkspaceRegionImpl.INSTANCE.getProjectLocationIdentifier(project,
							locationScheme));
					URL bLoc = new URL(location);
					if (Category.DEBUG) {
						TraceMessage.getInstance().getString("display", "Project location: 	" + pLoc.getPath());
						TraceMessage.getInstance().getString("display", "Bundle  location: 	" + bLoc.getPath());
					}
					if (pLoc.getPath().equalsIgnoreCase(bLoc.getPath())) {
						return project;
					}
				}
//			} catch (ProjectLocationException e) {
//				StatusManager.getManager().handle(
//						new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e),
//						StatusManager.LOG);
//			} catch (InPlaceException e) {
//				String msg = NLS.bind(Msg.PROJECT_MISSING_AT_LOC_WARN, location);
//				StatusManager.getManager().handle(
//						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
//			} catch (MalformedURLException e) {
//				String msg = ExceptionMessage.getInstance().formatString(
//						"project_location_malformed_error", location);
//				StatusManager.getManager().handle(
//						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
//			}
		}
		return null;
	}

	public IJavaProject getJavaProject(IProject project) throws InPlaceException {
	
		IJavaProject javaProject = null;
			if (isNatureEnabled(project, JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		return javaProject;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isInstallable(org.eclipse.core.resources.IProject)
	 */
	public Boolean isInstallable(IProject project) throws InPlaceException {

		if (isCandidate(project) || WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isCandidate(org.eclipse.core.resources.IProject)
	 */
	public Boolean isCandidate(IProject project) throws InPlaceException {
		try {
			if (isBundleProject(project)
					&& !WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
				if (Activator.getCommandOptionsService().isAllowUIContributions()) {
					return true;
				} else {
					Collection<IProject> uiContributors = getUIPlugins();
					if (uiContributors.contains(project)) {
						return false;
					} else {
						return true;
					}
				}
			}
		} catch (CircularReferenceException e) {
			// Ignore. Cycles are detected in any bundle job
		}	catch (ExtenderException e) {
			throw new InPlaceException(e);
		}
		return false;
	}

	public Boolean isUIPlugin(IProject project) throws InPlaceException {

		if (null == project) {
			throw new InPlaceException(ExceptionMessage.getInstance().getString("project_null_location"));
		}
		ManifestElement[] elements = BundleProjectMetaImpl.INSTANCE.getRequiredBundles(project);
		if (elements != null && elements.length > 0) {
			for (int i = 0; i < elements.length; i++) {
				ManifestElement rb = elements[i];
				if (rb.getValue().equals("org.eclipse.ui")) {
					return true;
				}
			}
		}
		return false;
	}

	public Boolean isBundleProject(IProject project)  throws InPlaceException {

		if (isNatureEnabled(project, JavaCore.NATURE_ID) && 
					isNatureEnabled(project, BundleProjectCandidates.PLUGIN_NATURE_ID)) {
				return true;
			}
		return false;
	}

	public Boolean isNatureEnabled(IProject project, String natureId) throws InPlaceException {
	
		if (null == project) {
			throw new InPlaceException(NLS.bind(Msg.PROJECT_NATURE_NULL_EXP, natureId));
		}
		if (project.isOpen()) {
			if (!project.exists()) {
				// This should not be the case for projects
				throw new InPlaceException(NLS.bind(Msg.PROJECT_OPEN_NOT_EXIST_EXP, natureId));
			}
			try {
				return project.hasNature(natureId) ? true : false;
			} catch (CoreException e) {
				// Closed or non-existing project should not happen at this point
				// TODO Problems if projects is closed in source IDE and 
				// open and imported with reference to source IDE in target IDE
				// throw new InPlaceException(NLS.bind(Msg.PROJECT_NATURE_CORE_EXP, natureId));
			}
		}
		return false;
	}

	public static Boolean isFragment(IProject project) throws InPlaceException {
	
		return BundleProjectMetaImpl.INSTANCE.isFragment(project);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isAutoBuilding()
	 */
	public Boolean isAutoBuilding() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		return workspace.isAutoBuilding();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#setAutoBuild(java.lang.Boolean)
	 */
	public Boolean setAutoBuild(Boolean autoBuild) {
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

	public String formatProjectList(Collection<IProject> projects) {
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

	public Collection<IProject> toProjects(Collection<IJavaProject> javaProjects) {
	
		Collection<IProject> projects = new ArrayList<IProject>();
		for (IJavaProject javaProject : javaProjects) {
			IProject project = javaProject.getProject();
			if (null != project) {
				projects.add(project);
			}
		}
		return projects;
	}

	public Collection<IJavaProject> toJavaProjects(Collection<IProject> projects) {
	
		Collection<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		for (IProject project : projects) {
			IJavaProject javaProject = JavaCore.create(project);
			if (null != javaProject) {
				javaProjects.add(javaProject);
			}
		}
		return javaProjects;
	}
}
