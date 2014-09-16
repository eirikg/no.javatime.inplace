package no.javatime.inplace.pl.dependencies.intface;

/**
 * Modeless dialog where to set dependency options for activating, deactivating, starting and
 * stopping bundles. A dependency option determines which dependent bundles to activate/deactivate
 * or star/stop when one of the mentioned bundle commands is issued on a bundle.
 * 
 * This bundle can be registered as a service by using the extender pattern.
 */
public interface DependencyDialog {

	/**
	 * Manifest header for accessing the implementation class name of the bundle console.
	 * May be used when extending this bundle
	 */
	public final static String DEPENDENCY_DIALOG_HEADER = "Dependency-Dialog";

	/**
	 * Open the dependency dialog
	 */
	public int open();

	/**
	 * Close the dependency dialog
	 */
	public boolean close();
}
