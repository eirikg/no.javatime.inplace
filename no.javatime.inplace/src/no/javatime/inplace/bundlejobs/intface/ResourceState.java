package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;

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
	 * Get the save options
	 * <p>
	 * The save options may also be used as a service 
	 * 
	 * @return The save options
	 * @see SaveOptions#SAVE_OPTONS_SERVICE
	 */
	public SaveOptions getSaveOptions();

	/**
	 * Get projects containing modified not saved resources
	 * 
	 * @return projects containing modified not saved resources or an empty collection
	 * @throws ExtenderException If failing to get the bundle project candidate service
	 */
	public Collection<IProject> getDirtyProjects() throws ExtenderException;

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
	 * @throws ExtenderException If failing to get the bundle project candidate service
	 */
	public boolean areResourcesDirty() throws ExtenderException;

	/**
	 * If there exist dirty files belonging to an activated bundle project and auto build and auto
	 * update is switched on, saving dirty files will generate an update job after build.
	 * 
	 * @return If saving files triggers an update operation return true. Otherwise false
	 * @see SaveOptions#isTriggerUpdate()
	 */
	public boolean isTriggerUpdate();

	/**
	 * Determines if there are dirty files to be saved based on the the save files preference settings.
	 * For saving files to be true there must also exist dirty files in the workspace.
	 * 
	 * @return True if there are dirty files and the preference setting for saving files is true. Otherwise
	 * false
	 * @see SaveOptions
	 */
	public boolean isSaveFiles();

	/**
	 * If the options for saving dirty files on, save all dirty files without any prompt.
	 * <p>
	 * If auto build is on, activated bundle projects are saved and the "Update on Build" option is on
	 * a build is triggered after save followed by an update. To manually save the workspace or a
	 * snapshot of the workspace use {@link #saveWorkspace(boolean)}
	 * 
	 * @throws ExtenderException If failing to get the bundle executor service
	 * @throws InPlaceException if the save options job fails
	 * @see CommandOptions#setIsSaveFilesBeforeBundleOperation(boolean)
	 * @see CommandOptions#isSaveFilesBeforeBundleOperation()
	 */
	public void saveFiles() throws InPlaceException, ExtenderException;

	/**
	 * Determines if a snapshot of the workspace should be saved based on the save workspace snapshot
	 * preference setting
	 * <p>
	 * 
	 * @return If the workspace should be saved based on its preference setting return {@code true}.
	 * Otherwise false.
	 * @see BundleExecutor#isSaveWorkspaceSnaphot()
	 */
	public boolean isSaveWorkspaceSnapshot();

	/**
	 * Save a snapshot of the workspace
	 * <p>
	 * To monitor the save operation use {@code ISaveParticipant}
	 * <p>
	 * Any errors during save are logged
	 */
	public void saveWorkspaceSnapshot();

	/**
	 * Check if there is a bundle executor job currently running
	 * 
	 * @return the bundle executor job currently running or null if no bundle executor job is running.
	 */
	public BundleExecutor getRunningBundleJob();

	/**
	 * Blocks execution while the java builder is running
	 * <p>
	 * Wait for all build jobs. If multiple build jobs only log once when the specified log parameter
	 * is true and logging is enabled.
	 * <p>
	 * Any interrupt or service failures exceptions are sent to the error log view
	 * 
	 * @param log If waiting for the builder to finish, the log parameter is true and logging is
	 * enabled, log a message indicating a builder wait state, otherwise no logging is performed
	 */
	public void waitOnBuilder(boolean log);

	/**
	 * Check if there is a job belonging to the {@code BundleExecutor.FAMILY_BUNDLE_LIFECYCLE} and is
	 * either running, waiting or sleeping
	 * 
	 * @return true if a bundle job is running, waiting or sleeping, otherwise false.
	 */
	public Boolean hasBundleJobState();

	/**
	 * Blocks execution while a bundle job is running
	 * <p>
	 * Any interrupt or service failures exceptions are sent to the error log view
	 * 
	 */
	public void waitOnBundleJob();
}