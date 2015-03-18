package no.javatime.inplace.bundlejobs.intface;

/**
 * Reset first uninstalls and then activates bundles to the same state as they had before the reset job.
 * <p>
 * Resetting a pending bundle creates a set containing the bundle to reset, and all bundles that have
 * requirements on the bundle to reset and its providing bundles. The set is the closure of bundles containing
 * the pending bundle to reset and all bundles with a direct or indirect declared dependency (requiring and
 * providing) on the pending bundle.
 */
public interface Reset extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate project operation
	 */
	public final static String RESET_BUNDLE_SERVICE = "Reset-Bundle-Service";

}