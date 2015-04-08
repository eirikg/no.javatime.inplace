package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Service to check state of workspace resources and bundle executors.
 * <p>
 * Get modification state of and save modified workspace resources
 * <p>
 * Get job state information about scheduled {@link BundleExecutor bundle executors}
 */
public interface ResourceState {

	/**
	 * Manifest header for accessing the default implementation class name of the resource state
	 * service
	 */
	public final static String RESOURCE_STATE_SERVICE = "Resource-State-Service";

	/**
	 * Get projects containing modified not saved resources
	 * 
	 * @return projects containing modified not saved resources or an empty collection
	 */
	public Collection<IProject> getDirtyProjects();

	/**
	 * Get projects containing modified not saved resources among the specified projects
	 * 
	 * @param projects Collection of projects with possible unsaved resources
	 * @return projects containing modified not saved resources or an empty collection
	 */
	public Collection<IProject> getDirtyProjects(Collection<IProject> projects);

	/**
	 * Check if there are any projects in the workspace with unmodified resources
	 * 
	 * @return true if there are any projects in the workspace with unmodified resources. Otherwise
	 * false
	 */
	public Boolean areResourcesDirty();

	/**
	 * Displays a save file dialog with a list of all dirty editors in the workspace that needs to be
	 * saved. If no files are dirty the save file dialog is not displayed and {@code true} is
	 * returned.
	 * 
	 * @return true if files are saved or no files are modified. False if modified files are not saved
	 */
	public Boolean saveModifiedResources();

	/**
	 * Displays a save file dialog with a list of editors that apply to the next build that need to be
	 * saved. If no files are dirty the save file dialog is not displayed and {@code true} is
	 * returned.
	 * 
	 * @return true if files are saved or no files are modified. False if modified files are not saved
	 */
	public Boolean saveModifiedFiles();

	/**
	 * Check if there is a bundle executor job currently running
	 * 
	 * @return the bundle executor job currently or null if no bundle executor job is running.
	 */
	public Job getRunningBundleJob();

	/**
	 * Blocks execution while the java builder is running
	 * <p>
	 * Wait for all build jobs. If multiple build jobs only log once when the specified log parameter
	 * is true and logging is enabled
	 * 
	 * @param log If waiting for the builder to finish, the log parameter is true and logging is
	 * enabled log a message indicating a builder wait state, otherwise no logging is performed
	 */
	public void waitOnBuilder(boolean log);

	/**
	 * Check if there is a job belonging to the {@code BundleExecutor.FAMILY_BUNDLE_LIFECYCLE} and is
	 * either running, waiting or sleeping
	 * 
	 * @return true if a bundle job is running, waiting or sleeping, otherwise false.
	 */
	public Boolean hasBundleJobState();
}