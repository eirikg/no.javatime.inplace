package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.bundlejobs.BundleJob;

import org.eclipse.core.resources.IProject;

public interface Bundles extends Status {

	/**
	 * Determine if this job belongs to the bundle family
	 * 
	 * @return true if the specified object == {@code Bundle.FAMILY_BUNDLE_LIFECYCLE} or is of type
	 * {@code BundleJob}, otherwise false
	 */
	public boolean belongsTo(Object family);

	/**
	 * Add a pending bundle project to process in a job
	 * 
	 * @param project to process
	 */
	public void addPendingProject(IProject project);

	/**
	 * Adds a collection of pending bundle projects to process in a job. To maintain the topological
	 * sort order use {@link #resetPendingProjects(Collection)}
	 * 
	 * @param projects bundle projects to process
	 * @see BundleJob#resetPendingProjects(Collection)
	 */
	public void addPendingProjects(Collection<IProject> projects);

	/**
	 * Replace existing pending projects with the specified collection The specified collection is
	 * copied and the topological sort order of the projects is maintained
	 * 
	 * @param projects bundle projects to replace
	 */
	public void resetPendingProjects(Collection<IProject> projects);

	/**
	 * Removes a pending bundle project from the project job list
	 * 
	 * @param project bundle project to remove
	 */
	public void removePendingProject(IProject project);

	/**
	 * Removes pending bundle projects from the project job list.
	 * 
	 * @param projects bundle projects to remove
	 */
	public void removePendingProjects(Collection<IProject> projects);

	/**
	 * Clears all pending bundle projects from the project job list.
	 */
	public void clearPendingProjects();

	/**
	 * Determine if the specified bundle project is a pending bundle project
	 * 
	 * @param project bundle project to check for pending status
	 * @return true if the specified bundle project is a pending bundle project, otherwise false.
	 */
	public Boolean isPendingProject(IProject project);

	/**
	 * Checks if there are any pending bundle projects to process.
	 * 
	 * @return true if there are any bundle projects to process, otherwise false
	 */
	public Boolean hasPendingProjects();

	/**
	 * Returns all pending bundle projects.
	 * 
	 * @return the set of pending bundle projects or an empty set
	 */
	public Collection<IProject> getPendingProjects();

	/**
	 * Number of pending bundle projects
	 * 
	 * @return number of pending bundle projects
	 */
	public int pendingProjects();
	
	public void run();
}