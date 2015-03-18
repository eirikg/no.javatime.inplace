package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.region.intface.BundleTransition;

import org.eclipse.core.resources.IProject;

/**
 * Service to update modified bundles after activated projects are built. The service is
 * automatically scheduled after a build of activated bundle projects. The scheduled bundles are
 * updated and together with their requiring bundles, unresolved, resolved, optionally refreshed and
 * started as part of the update process.
 * <p>
 * To manually control and schedule the update bundle executor, switch off the "Update on Build"
 * option.
 * <p>
 * If the option "Refresh on Update" is off bundle projects are resolved after updated. If bundle
 * projects have more than one revision after resolve, refresh is pending and the status is set to
 * "Refresh Pending". To check if refresh is pending use
 * {@link no.javatime.inplace.region.intface.BundleCommand#getBundleRevisions(org.osgi.framework.Bundle)
 * BundleCommand.getBundleRevisions(Bundle)}
 * <p>
 * If bundles are to be refreshed after update bundle dependency closures are calculated and added
 * as pending projects to this service according to the current dependency option as part of the
 * update process:
 * <ol>
 * <li>Requiring bundles to a bundle to update are resolved and optionally refreshed
 * <li>New deactivated imported projects of a project to update are activated. In the case where a
 * bundle to be updated is dependent on other not activated bundles, this is handled by the resolver
 * hook. The resolver hook is visited by the framework during resolve.
 * </ol>
 * <p>
 * Bundle projects with name collisions (same symbolic name and version> are detected, excluded from
 * the update process and an error status object is generated and reported after update has
 * finished. To access the error objects see {@link #getErrorStatusList()}
 * <p>
 * After updated, bundles are moved to the same state as before update if possible.
 * <p>
 * Plug-ins are usually singletons, and it is a requirement, if the plug-in contributes to the UI.
 * When resolving bundles, a collision may occur with earlier resolved bundles with the same
 * symbolic name. In these cases the duplicate (the earlier resolved bundle) is removed and replaced
 * by the new one during the resolve process.
 * 
 * @see CommandOptions#setIsUpdateOnBuild(boolean)
 * @see CommandOptions#setIsRefreshOnUpdate(boolean)
 */
public interface Update extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the update
	 * bundle operation
	 */
	public final static String UPDATE_BUNDLE_SERVICE = "Update-Bundle-Service";

	/**
	 * This is convenience method to add a pending update transition to a bundle project.
	 * <p>
	 * A pending update transition is automatically added to an activated project that has been built
	 * but not yet updated and is required to update the project. Projects that have been built are
	 * always updated automatically as long as they are free of build errors and the "Update on Build"
	 * option is switched on.
	 * <p>
	 * To force an update add a pending update transition to each of the specified projects.
	 * <p>
	 * Updating a project which not has been built since last update should have no effect except
	 * updating the project with the same byte code, but may have it on projects that are pending (has
	 * been built and the update on build option is off) and depends (requiring projects) on the
	 * project to update.
	 * 
	 * @param projects Add a pending update transition to the specified projects
	 * @see #isPendingForUpdate(IProject)
	 * @see BundleTransition#addPending(IProject, BundleTransition.Transition)
	 */
	public void addUpdateTransition(Collection<IProject> projects);

	/**
	 * Checks if the specified project is pending for update.
	 * <p>
	 * An activated project that have been built is always pending for update if the "Update on Build"
	 * option is switched off.
	 * 
	 * @param project Check this project for a pending update operation
	 * @return true if the project is pending for update and false if not
	 * @see #addUpdateTransition(Collection)
	 */
	public boolean isPendingForUpdate(IProject project);

	/**
	 * Detect circular symbolic name collisions and order the pending collection of bundle projects
	 * based on existing and new symbolic keys (symbolic name and version) before they are updated.
	 * <p>
	 * The set of collision projects are the difference between the set of pending projects and the
	 * ordered set of returned projects
	 * <p>
	 * Bundles must be ordered when a bundle changes its symbolic key to the same symbolic key as an
	 * other bundle to update, and this other bundle at the same time changes its symbolic key to a
	 * new value. The other bundle must then be updated first to avoid that the first bundle becomes a
	 * duplicate of the other bundle. A special case, called a circular name collision, occurs if the
	 * other bundle in addition changes it symbolic key to the same as the current (or existing)
	 * symbolic key of the first bundle.
	 * 
	 * 
	 * @return ordered collection of projects to update. If collisions are detected the involved
	 * projects are not added to the returned collection.
	 */
	public Collection<IProject> getUpdateOrder();
}