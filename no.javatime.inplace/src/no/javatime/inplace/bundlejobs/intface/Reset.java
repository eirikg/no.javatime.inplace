package no.javatime.inplace.bundlejobs.intface;

/**
 * Reset first uninstalls and then activates bundles to the same state as they had before the reset
 * job.
 * <p>
 * Resetting a pending bundle creates a set containing the bundle to reset, and all providing
 * bundles and than the requiring bundles of both bundles to reset and the providing bundles.
 */
public interface Reset extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * project operation
	 */
	public final static String RESET_BUNDLE_SERVICE = "Reset-Bundle-Service";

}