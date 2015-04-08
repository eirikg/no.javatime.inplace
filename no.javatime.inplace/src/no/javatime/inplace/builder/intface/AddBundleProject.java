package no.javatime.inplace.builder.intface;

import no.javatime.inplace.bundlejobs.intface.BundleExecutor;


/**
 * Add external bundle projects to the workspace region. An external or new bundle project is a
 * project that is opened, imported, created or renamed and needs to be installed or resolved.
 * <p>
 * A renamed project in an activated workspace is uninstalled in the pre change listener before it
 * is installed or resolved by this job, thus it can be viewed as an external project. This job does
 * nothing if the workspace is deactivated and there are no activated external projects to add.
 * 
 * <p>
 * Deactivated projects providing capabilities to nature enabled projects are scheduled for
 * activation. Other nature enabled projects are resolved and started while deactivated projects are
 * installed.
 * <p>
 * This job must make allowance for multiple new projects, internal dependencies among the set of
 * external bundle projects and intra dependencies between external an existing workspace projects
 * to install and resolve.
 * <p>
 * This is the only place projects are added (import, create, open and rename) to the workspace.
 */
public interface AddBundleProject extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the add bundle
	 * project bundle operation
	 */
	public final static String ADD_BUNDLE_PROJECT_SERVICE = "Add-Bundle-Project-Service";

}