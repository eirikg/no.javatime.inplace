package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

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
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	public static Collection<IProject> getProvidingProjects(IProject project) throws InPlaceException{

		Collection<IProject> projects = new LinkedHashSet<IProject>();
		if (null != project) {
			try {
				IProject[] referencedProjects = project.getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					IProject refProject = referencedProjects[i];
						if (refProject.hasNature(JavaCore.NATURE_ID) 
								&& refProject.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
							projects.add(refProject);
						}
				}
			} catch (CoreException e) {
				throw new InPlaceException(e, "error_get_providing_projects", BundleProjectState.formatProjectList(projects));
			}
		}
		return projects;
	}

	/**
	 * Calculates transitive references of projects to this project. Self is included in the providing projects.
	 * @param project the initial project
	 * @param projects are the set of detected providing projects. The parameter is typically an empty collection. 
	 * @return providing projects. Never null.
	 * @throws InPlaceException if any referenced project is closed or does nor exist
	 */
	public static Collection<IProject> getProvidingProjects(IProject project, Collection<IProject> projects ) 
			throws InPlaceException {

		if (null != project && !projects.contains(project)) {
			projects.add(project);
			try {
				IProject[] referencedProjects = project.getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					getProvidingProjects(referencedProjects[i], projects);
				}
			} catch (CoreException e) {
				throw new InPlaceException(e, "error_get_providing_projects", BundleProjectState.formatProjectList(projects));
			}
		}
		return projects;
	}

	/**
	 * Get direct references of projects from this project. Self is not included in the requiring projects.
	 * <p>
	 * Note that non accessible (closed and non existing projects) are ignored. This implies that projects
	 * closures involving non accessible projects that requires this project are incomplete 
	 * 
	 * @param project the initial project
	 * @param project the providing project
	 * @return requiring projects. Never null. 
	 * @throws InPlaceException if test for the plug in nature fails
	 * @see IProject#getReferencingProjects()
	 */
	public static Collection<IProject> getRequiringProjects(IProject project) 
			throws InPlaceException {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		if (null != project) {
			IProject[] referencingProjects = project.getReferencingProjects();
			for (int i = 0; i < referencingProjects.length; i++) {
				try {
					IProject refProject = referencingProjects[i];
					if (refProject.hasNature(JavaCore.NATURE_ID) 
							&& refProject.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
						projects.add(refProject);
					}
				} catch (CoreException e) {
					throw new InPlaceException(e, "error_get_requiring_projects", BundleProjectState.formatProjectList(projects));
				}
			}
		}
		return projects;
	}
	
	/**
	 * Calculates transitive references of projects from this project. Self is included in the requiring projects.
	 * <p>
	 * Note that non accessible (closed and non existing projects) are ignored. This implies that projects
	 * closures involving non accessible projects that requires this project are incomplete 
	 * 
	 * @param project the initial project
	 * @param projects are the set of detected requiring projects. The parameter is typically an empty collection. 
	 * @return requiring projects. Never null. 
	 * @throws InPlaceException if test for the plug in nature fails
	 */
	public static Collection<IProject> getRequiringProjects(IProject project, Collection<IProject> projects ) throws InPlaceException {

		if (null != project && !projects.contains(project)) {
			projects.add(project);
			IProject[] referencingProjects = project.getReferencingProjects();
			for (int i = 0; i < referencingProjects.length; i++) {
				try {
					IProject refProject = referencingProjects[i];
					if (refProject.hasNature(JavaCore.NATURE_ID) 
							&& refProject.isNatureEnabled(BundleProjectState.PLUGIN_NATURE_ID)) {
						getRequiringProjects(referencingProjects[i], projects);
					}
				} catch (CoreException e) {
					throw new InPlaceException(e, "error_get_requiring_projects", BundleProjectState.formatProjectList(projects));
				}
			}
		}
		return projects;
	}
}
