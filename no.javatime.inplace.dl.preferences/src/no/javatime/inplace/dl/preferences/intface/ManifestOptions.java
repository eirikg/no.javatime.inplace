package no.javatime.inplace.dl.preferences.intface;

import org.osgi.service.prefs.BackingStoreException;

public interface ManifestOptions {

	public static final String IS_UPDATE_DEFAULT_OUTPUT_FOLDER = "isupdateDefaultOutputFolder";
	public static final String IS_EAGER_ON_ACTIVATE = "isEagerOnActivate";

	/**
	 * Check whether the default output is going to be updated on activate and deactivate of bundles
	 * 
	 * @return true to update and false if not
	 */
	public abstract boolean isUpdateDefaultOutPutFolder();

	/**
	 * Get the default value of the default output folder option
	 * 
	 * @return true if default is to update default output folder on activate/deactivate, and false if not
	 */
	public abstract boolean getDefaultUpdateDefaultOutPutFolder();

	/**
	 * set whether the default output is going to be updated on activate and deactivate of bundles
	 * 
	 * @return true to update and false to not update
	 */
	public abstract void setIsUpdateDefaultOutPutFolder(boolean updateDefaultOutputFolder);
	
	/**
	 * Should policy be set toe eager on bundle activation
	 * @return true if policy should be set to eager on activation, otherwise false.
	 */
	public abstract boolean isEagerOnActivate();
	
	/**
	 * Get default option for setting activation policy to eager on activation
	 * @return true if activation policy should be set to eager on activation , otherwise false.
	 */
	public abstract boolean getDefaultIsEagerOnActivate();

	/**
	 * Set whether activation policy should be set toe eager on bundle activation
	 * @param eager true to set activation policy to eager on activation and false to not
	 */
	public abstract void setIsEagerOnActivate(boolean eager);

	/**
	 * Flush all changes to OSGi preference store
	 * @throws BackingStoreException thrown when the flush operation could not complete
	 */
	public abstract void flush() throws BackingStoreException;

}
