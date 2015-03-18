package no.javatime.inplace.dl.preferences.intface;

import org.osgi.service.prefs.BackingStoreException;

public interface ManifestOptions {

	public static final String IS_UPDATE_DEFAULT_OUTPUT_FOLDER = "isUpdateDefaultOutputFolder";
	public static final String IS_EAGER_ON_ACTIVATE = "isEagerOnActivate";

	/**
	 * Check whether the default output is going to be updated on activate and deactivate of bundles
	 * 
	 * @return true to update and false if not
	 */
	public boolean isUpdateDefaultOutPutFolder();

	/**
	 * Get the default value of the default output folder option
	 * 
	 * @return true if default is to update default output folder on activate/deactivate, and false if
	 * not
	 */
	public boolean getDefaultUpdateDefaultOutPutFolder();

	/**
	 * Set whether to update the default output folder on activate and deactivate of bundles or not
	 * 
	 * @param updateDefaultOutputFolder {@code true} to add the default output folder on activate and remove
	 * the default output folder on deactivate and {@code false} to not update the default output folder on
	 * activate and deactivate
	 */
	public void setIsUpdateDefaultOutPutFolder(boolean updateDefaultOutputFolder);

	/**
	 * Should policy be set toe eager on bundle activation
	 * 
	 * @return true if policy should be set to eager on activation, otherwise false.
	 */
	public boolean isEagerOnActivate();

	/**
	 * Get default option for setting activation policy to eager on activation
	 * 
	 * @return true if activation policy should be set to eager on activation , otherwise false.
	 */
	public boolean getDefaultIsEagerOnActivate();

	/**
	 * Set whether activation policy should be set toe eager on bundle activation
	 * 
	 * @param eager true to set activation policy to eager on activation and false to not
	 */
	public void setIsEagerOnActivate(boolean eager);

	/**
	 * Flush all changes to OSGi preference store
	 * 
	 * @throws BackingStoreException thrown when the flush operation could not complete
	 */
	public void flush() throws BackingStoreException;

}
