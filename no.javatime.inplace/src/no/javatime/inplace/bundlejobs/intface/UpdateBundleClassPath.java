package no.javatime.inplace.bundlejobs.intface;

public interface UpdateBundleClassPath extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the update bundle class path operation
	 */
	public final static String UPDATE_BUNDLE_CLASS_PATH_SERVICE = "Update-Bundle-Class-Path-Service";

	/**
	 * Determines whether the default output folder is added to the Bundle-ClassPath or removed from
	 * the Bundle-ClassPath
	 * 
	 * @return true if the default output folder is added and false if default output folder is
	 * removed
	 */
	public boolean isAddToPath();

	/**
	 * Determines whether to add the default output folder to the Bundle-ClassPath or remove the
	 * default output folder from the Bundle-ClassPath
	 * <p>
	 * Default is to add the default output folder to the Bundle-ClassPath
	 * 
	 * @param addToPath add default output folder if true and remove default output folder if false
	 */
	public void setAddToPath(boolean addToPath);

}