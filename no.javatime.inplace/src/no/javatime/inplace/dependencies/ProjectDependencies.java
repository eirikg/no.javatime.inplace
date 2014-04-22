package no.javatime.inplace.dependencies;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Utility to get direct and transitive requiring (referencing) and providing (referenced) projects
 * given an initial project.
 */
public class ProjectDependencies {

	public ProjectDependencies() {
	}
	
	/**
	 * Get direct references of projects to this project. Self is not included in the providing projects.
	 * @param project the initial project
	 * @return providing projects. Never null.
	 */
	public static Collection<IProject> getProvidingProjects(IProject project) {

		Collection<IProject> projects = new LinkedHashSet<IProject>();
		if (null != project) {
			try {
				IProject[] referencedProjects = project.getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					if (referencedProjects[i].exists()) {
						projects.add(referencedProjects[i]);
					}
				}
			} catch (CoreException e) {
				String msg = ExceptionMessage.getInstance().formatString("error_get_providing_projects", ProjectProperties.formatProjectList(projects));
				StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			}
		}
		return projects;
	}

	/**
	 * Calculates transitive references of projects to this project. Self is included in the providing projects.
	 * @param project the initial project
	 * @param projects are the set of detected providing projects. The parameter is typically an empty collection. 
	 * @return providing projects. Never null.
	 */
	public static Collection<IProject> getProvidingProjects(IProject project, Collection<IProject> projects ) {

		if (null != project && !projects.contains(project)) {
			projects.add(project);
			try {
				IProject[] referencedProjects = project.getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					getProvidingProjects(referencedProjects[i], projects);
				}
			} catch (CoreException e) {
				String msg = ExceptionMessage.getInstance().formatString("error_get_providing_projects", ProjectProperties.formatProjectList(projects));
				StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			}
		}
		return projects;
	}

	/**
	 * Get direct references of projects from this project. Self is not included in the requiring projects.
	 * @param project the initial project
	 * @param project the providing project
	 * @return requiring projects. Never null. 
	 */
	public static Collection<IProject> getRequiringProjects(IProject project) {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		if (null != project) {
			IProject[] referencingProjects = project.getReferencingProjects();
			for (int i = 0; i < referencingProjects.length; i++) {
				try {
					IProject refProject = referencingProjects[i];
					if (refProject.hasNature(JavaCore.NATURE_ID) 
							&& refProject.isNatureEnabled(ProjectProperties.PLUGIN_NATURE_ID)) {
						projects.add(referencingProjects[i]);
					}
				} catch (CoreException e) {
					// Ignore closed or non-existing project 
				} catch (CircularReferenceException e) {
					// Ignore. Cycles are detected in any bundle job
				}
			}
		}
		return projects;
	}
	
	/**
	 * Calculates transitive references of projects from this project. Self is included in the requiring projects.
	 * @param project the initial project
	 * @param projects are the set of detected requiring projects. The parameter is typically an empty collection. 
	 * @return requiring projects. Never null. 
	 */
	public static Collection<IProject> getRequiringProjects(IProject project, Collection<IProject> projects ) {

		if (null != project && !projects.contains(project)) {
			projects.add(project);
			IProject[] referencingProjects = project.getReferencingProjects();
			for (int i = 0; i < referencingProjects.length; i++) {
				try {
					IProject refProject = referencingProjects[i];
					if (refProject.hasNature(JavaCore.NATURE_ID) 
							&& refProject.isNatureEnabled(ProjectProperties.PLUGIN_NATURE_ID)) {
						getRequiringProjects(referencingProjects[i], projects);
					}
				} catch (CoreException e) {
					// Ignore closed or non-existing project 
				} catch (CircularReferenceException e) {
					// Ignore. Cycles are detected in any bundle job
				}
			}
		}
		return projects;
	}
}
