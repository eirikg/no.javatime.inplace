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

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
public class BundleProjectCandidatesImpl extends BundleProjectStateImpl implements BundleProjectCandidates {
	
	public final static BundleProjectCandidatesImpl INSTANCE = new BundleProjectCandidatesImpl();
	final public static String JAVATIME_NATURE_ID = "no.javatime.inplace.builder.javatimenature";

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getCandidates()
	 */
	public Collection<IProject> getCandidates() {

		Collection<IProject> projects = new LinkedHashSet<IProject>();

		for (IProject project : getProjects()) {
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
						&& !WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
					projects.add(project);
				}
			} catch (CoreException e) {
				// Ignore closed or non-existing project
			}
			try {
				if (!Activator.getDefault().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIPlugins());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
				// assume allow ui contributers
			}
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getInstallable()
	 */
	public Collection<IProject> getInstallable() {
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
				if (!Activator.getDefault().getCommandOptionsService().isAllowUIContributions()) {
					projects.removeAll(getUIPlugins());
				}
			} catch (CircularReferenceException e) {
				// Ignore. Cycles are detected in any bundle job
			}	catch (InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
				// assume allow ui contributers
			}
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getPlugIns()
	 */
	public Collection<IProject> getBundleProjects() {
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

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isPlugIn(org.eclipse.core.resources.IProject)
	 */
	public Boolean isBundleProject(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)) {
				return true;
			}
		} catch (CoreException e) {
			// Ignore closed and non-existing project
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isInstallable(org.eclipse.core.resources.IProject)
	 */
	public Boolean isInstallable(IProject project) {

		if (isCandidate(project) || WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isCandidate(org.eclipse.core.resources.IProject)
	 */
	public Boolean isCandidate(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
					&& !WorkspaceRegionImpl.INSTANCE.isBundleActivated(project)) {
				if (Activator.getDefault().getCommandOptionsService().isAllowUIContributions()) {
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
		} catch (CoreException e) {
			// Ignore closed or non-existing project
		} catch (CircularReferenceException e) {
			// Ignore. Cycles are detected in any bundle job
		}	catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			// assume not candidate
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#getUIContributors()
	 */
	public Collection<IProject> getUIPlugins() throws CircularReferenceException {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.isNatureEnabled(PLUGIN_NATURE_ID)
						&& isUIPlugin(project)) {
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

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isUIContributor(org.eclipse.core.resources.IProject)
	 */
	public Boolean isUIPlugin(IProject project) throws InPlaceException {

		if (null == project) {
			throw new InPlaceException("project_null_location");
		}
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
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

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleCandidates#isAutoBuilding()
	 */
	public Boolean isAutoBuilding() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		return workspace.isAutoBuilding();
	}


}
