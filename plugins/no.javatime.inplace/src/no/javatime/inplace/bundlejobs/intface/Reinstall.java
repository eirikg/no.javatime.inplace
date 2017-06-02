package no.javatime.inplace.bundlejobs.intface;

/**
 * Reinstalls pending projects. 
 * <p>
 * Pending bundle projects are first uninstalled and then installed. Only pending bundle
 * projects in state INSTALLED are reinstalled and no dependency closures are calculated
 */
public interface Reinstall extends BundleExecutor {
	/**
	 * Manifest header for accessing the default service implementation class name of the activate project operation
	 */
	public final static String REINSTALL_BUNDLE_SERVICE = "Reinstall-Bundle-Service";

}