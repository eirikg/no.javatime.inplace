package no.javatime.inplace.bundlejobs.intface;

/**
 * Bundle projects are deactivated by removing the internal nature from the projects and moving them
 * to state INSTALLED in an activated workspace and state UNINSTALLED in a deactivated workspace.
 * <p>
 * If the workspace becomes deactivated (that is when the last bundle in the workspace is
 * deactivated) as a result of running the deactivate executor all bundles are moved to state
 * UNINSTALLED.
 * <p>
 * Calculate closure of bundle projects and add them as pending projects to this job before the
 * projects are deactivated according to the current dependency option. Requiring bundles to pending
 * bundle projects are always added as pending bundle projects before bundles are deactivated.
 * 
 * @see ActivateProject
 * @see ActivateBundle
 */
public interface Deactivate extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the deactivate
	 * bundle operation
	 */
	public final static String DEACTIVATE_BUNDLE_SERVICE = "Deactivate-Bundle-Service";

	/**
	 * Determine to log build errors on deactivate
	 * 
	 * @return true if true, log build errors on deactivate. Otherwise false.
	 * @see #setCheckBuildErrors(boolean)
	 */
	public boolean isCheckBuildErrors();

	/**
	 * Check build errors of pending projects on deactivate. It is not possible to activate projects
	 * with build errors.
	 * <p>
	 * If there are build errors the projects with build errors are reported as errors and can be
	 * obtained from {@link #getErrorStatusList()} after deactivation.
	 * 
	 * @param checkBuildErrors true to report build errors and false to not report
	 * @see #isCheckBuildErrors()
	 */
	public void setCheckBuildErrors(boolean checkBuildErrors);

}