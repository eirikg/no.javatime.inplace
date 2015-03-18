package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;

/**
 * Toggles between lazy and eager activation by updating the manifest files of the pending bundle
 * projects.
 * <p>
 * For a lazy policy the Bundle-ActivationPolicy header is added and for an eager police the header
 * is removed.
 * <p>
 * Changing the policy of an activated project triggers a build which again triggers an update of
 * the bundle project causing the bundle to enter a state according to the new policy setting. If
 * the "Build Automatically" option is off the update will be delayed with a "Build Pending" status
 * and occur when the "Build Automatically" option is switched on or by a manually build.
 */
public interface TogglePolicy extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the toggle
	 * policy operation
	 */
	public final static String TOGGLE_POLICY_SERVICE = "Toggle-Policy-Service";

	/**
	 * Gets the activation policy header from the manifest file of the specified project. This is a
	 * convenience method for
	 * {@link no.javatime.inplace.region.intface.BundleProjectMeta#getActivationPolicy(IProject)
	 * BundleProjectMeta.getActivationPolicy(IProject)}
	 * 
	 * @param project containing the meta file information
	 * @return true if lazy activation and false if eager activation.
	 * @throws InPlaceException if project is null or failed to obtain the project description
	 */
	public boolean getActivationPolicy(IProject project);

}