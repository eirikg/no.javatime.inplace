package no.javatime.inplace.bundlejobs.intface;


/**
 * Starts pending bundle projects with an initial state of INSTALLED, RESOLVED and STOPPING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this bundle executor
 * before the bundles are started according to the current dependency option.
 * 
 * @see Stop
 */
public interface Start extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * project operation
	 */
	public final static String START_BUNDLE_SERVICE = "Start-Bundle-Service";
}