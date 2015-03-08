package no.javatime.inplace.bundlejobs.intface;


public interface Deactivate extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the deactivate bundle operation
	 */
	public final static String DEACTIVATE_BUNDLE_SERVICE = "Deactivate-Bundle-Service";

	public boolean isCheckBuildErrors();

	public void setCheckBuildErrors(boolean checkBuildErrors);

}