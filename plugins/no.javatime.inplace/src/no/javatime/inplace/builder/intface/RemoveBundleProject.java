package no.javatime.inplace.builder.intface;

import no.javatime.inplace.bundlejobs.intface.BundleExecutor;

/**
 * Uninstall all bundle projects that have been removed (closed or deleted) from the workspace.
 * Removed bundle projects are added as pending projects to this bundle executor job before
 * scheduling the job.
 * <p>
 * If there exists removed projects in the workspace that are not added to this bundle executor when
 * it starts running the projects are added automatically as pending projects by this bundle
 * executor job.
 * <p>
 * When removing bundle projects with requiring bundles the requiring closure set becomes
 * incomplete. This inconsistency is solved by deactivating the requiring bundles in the closure
 * before uninstalling the removed projects.
 */
public interface RemoveBundleProject extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the remove
	 * bundle project bundle operation
	 */
	public final static String REMOVE_BUNDLE_PROJECT_SERVICE = "Remove-Bundle-Project-Service";

}