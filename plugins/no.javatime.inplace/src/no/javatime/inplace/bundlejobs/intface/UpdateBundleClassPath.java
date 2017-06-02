package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;

/**
 * Removes or inserts the default output folder of pending projects in the Bundle-ClassPath header
 * in the manifest file.
 * <p>
 * When running on the source or development platform it is necessary to have the default output
 * folder in the bundle class path to start (state ACTIVE) a bundle.
 * <P>
 * If the option "Update Bundle-ClassPath on Activate/Deactivate" is switched on than the default
 * output folder is added and removed respectively on the bundle class path when projects are
 * {@link ActivateProject activated} and {@link Deactivate deactivated}.
 * <p>
 * If the "Build Automatic" option is off, a {@link Reset reset} job is scheduled for the pending
 * bundle projects that have their default output folder updated.
 * 
 * @see CommandOptions#setIsUpdateDefaultOutPutFolder(boolean)
 */
public interface UpdateBundleClassPath extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the update
	 * bundle class path operation
	 */
	public final static String UPDATE_BUNDLE_CLASS_PATH_SERVICE = "Update-Bundle-Class-Path-Service";

	/**
	 * Determines whether the default output folder is in the the Bundle-ClassPath or removed from the
	 * Bundle-ClassPath
	 * 
	 * @return true if the default output folder is in the bundle class path and false if the default
	 * output folder is not present
	 */
	public boolean isAddToPath();

	/**
	 * Determines whether to add the default output folder to the Bundle-ClassPath or remove the
	 * default output folder from the Bundle-ClassPath when executing this bundle operation
	 * <p>
	 * Default is to add the default output folder to the Bundle-ClassPath
	 * 
	 * @param addToPath add default output folder if true and remove default output folder if false
	 */
	public void setAddToPath(boolean addToPath);

}