package no.javatime.inplace.bundlejobs.intface;

/**
 * Installs pending projects in state UNINSTALLED. Installed bundle projects are moved to state
 * INSTALLED.
 * <p>
 * If this is the first set of pending projects to install, all candidate bundle projects in the
 * workspace are installed. Otherwise only the pending projects are installed.
 * 
 * @see Uninstall
 * @see Deactivate
 */
public interface Install extends BundleExecutor {
	/**
	 * Manifest header for accessing the default service implementation class name of the bundle operation.
	 */
	public final static String INSTALL_BUNDLE_SERVICE = "Install-Bundle-Service";

}