package no.javatime.inplace.bundlejobs.intface;


/**
 * Stops pending bundle projects with an initial state of ACTIVE and STARTING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this bundle executor
 * before the bundles are stopped according to the current dependency option. 
 * 
 * @see Start
 */
public interface Stop extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the stop bundle
	 * operation.
	 */
	public final static String STOP_BUNDLE_SERVICE = "Stop-Bundle-Service";
}