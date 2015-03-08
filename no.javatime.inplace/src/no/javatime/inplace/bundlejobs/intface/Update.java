package no.javatime.inplace.bundlejobs.intface;

/**
 * Service to update modified bundles after activated projects are built. The service is
 * automatically scheduled after a build of activated bundle projects. The scheduled bundles are
 * updated and together with their requiring bundles, unresolved, resolved, optionally refreshed and
 * started as part of the update process.
 * <p>
 * Bundle dependency closures are calculated and added as pending projects to this service according
 * to the current dependency option as part of the update process:
 * <ol>
 * <li>Requiring bundles to a bundle to update are resolved and optionally refreshed
 * <li>New deactivated imported projects of a project to update are activated. In this case where a
 * bundle to be updated is dependent on other not activated bundles, this is handled in the resolver
 * hook. The resolver hook is visited by the framework during resolve.
 * </ol>
 * <p>
 * After updated, bundles are moved to the same state as before update if possible.
 * <p>
 * Plug-ins are usually singletons, and it is a requirement, if the plug-in contributes to the UI.
 * When resolving bundles, a collision may occur with earlier resolved bundles with the same
 * symbolic name. In these cases the duplicate (the earlier resolved bundle) is removed and replaced
 * by the new one in the resolving process.
 * <p>
 */
public interface Update extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the update
	 * bundle operation
	 */
	public final static String UPDATE_BUNDLE_SERVICE = "Update-Bundle-Service";

}