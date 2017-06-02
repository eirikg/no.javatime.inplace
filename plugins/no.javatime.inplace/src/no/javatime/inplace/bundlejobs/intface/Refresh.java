package no.javatime.inplace.bundlejobs.intface;

/**
 * Refreshes pending projects.
 * <p>
 * Active (state ACTIVE and STARTING) bundles are first stopped, refreshed and then started.
 * <p>
 * A requiring dependency closure is calculated and requiring bundle projects to pending bundle
 * projects to refresh are added as pending bundle projects.
 * 
 */
public interface Refresh extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * project operation
	 */
	public final static String REFRESH_BUNDLE_SERVICE = "Refresh-Bundle-Service";

}