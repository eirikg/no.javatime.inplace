package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.builder.SaveSnapShotOption;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Save dirty files and/or a workspace snapshot. To save files run this as a service operation.
 * <p>
 * The settings for saving (files and a workspace snapshot) are calculated based on the preference
 * settings (command options) for saving. For saving files to be true, there must also exist dirty
 * files in the workspace.
 * <p>
 * Saving files in activated bundle projects may automatically trigger an update operation.
 * <p>
 * Preference settings for saving dirty files and a workspace snapshot my be set by using the
 * command options service.
 * 
 * @see CommandOptions#setIsSaveFilesBeforeBundleOperation(boolean)
 * @see CommandOptions#setIsSaveSnapshotBeforeBundleOperation(boolean)
 */
public interface SaveOptions extends BundleExecutor {

	/**
	 * Manifest header for accessing the default implementation class name of the save options service
	 */
	public final static String SAVE_OPTONS_SERVICE = "Save-Options-Service";

	/**
	 * If there exist dirty files belonging to an activated bundle project and auto build and auto
	 * update is switched on, saving dirty files will generate an update job after build.
	 * 
	 * @return If saving files triggers an update operation return true. Otherwise false
	 * @throws ExtenderException If failing to get any services needed to run the bundle executor
	 */
	public boolean isTriggerUpdate() throws ExtenderException;

	/**
	 * Determines if there are dirty files to be saved based on the the save files preference
	 * settings. For saving files to be true there must also exist dirty files in the workspace.
	 * 
	 * @return True if there are dirty files and the preference setting for saving files is true.
	 * Otherwise false
	 * @throws ExtenderException If failing to get the command options service
	 */
	public boolean isSaveFiles() throws ExtenderException;

	/**
	 * Override the save files preference setting
	 * 
	 * @param disable If {@code true} files are not saved when invoking {@link #saveFiles()} or
	 * executing this service as bundle job. If {@code false} the save files preference setting is
	 * used.
	 */
	public void disableSaveFiles(boolean disable);

	/**
	 * Saves dirty files
	 * <p>
	 * {@link #isSaveFiles()} is called first and if {@code true} is returned the files are saved
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 * @throws ExtenderException If failing to get the message options service
	 */
	public IBundleStatus saveFiles() throws ExtenderException;

	/**
	 * Save a snapshot of the workspace
	 * 
	 * @param monitor Shows progress of the save workspace snapshot operation
	 * @see SaveSnapShotOption
	 */
	public void saveWorkspace(IProgressMonitor monitor);

}