package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleTransition;

import org.eclipse.core.resources.IProject;

/**
 * Service to update bundles. The service is automatically scheduled after a build of activated
 * bundle projects. The scheduled bundles are updated and together with their requiring bundles,
 * stopped, unresolved, resolved, optionally refreshed and started as part of the update process.
 * <p>
 * To manually control and schedule bundle updates, switch off the "Update on Build" option.
 * <p>
 * If the option "Refresh on Update" is off bundle projects are resolved after updated. If bundle
 * projects have more than one revision after resolve, refresh is pending and the status is set to
 * "Refresh Pending". To check if refresh is pending use
 * {@link no.javatime.inplace.region.intface.BundleCommand#getBundleRevisions(org.osgi.framework.Bundle)
 * BundleCommand.getBundleRevisions(Bundle)}
 * <p>
 * Requiring and providing bundles:
 * <ol>
 * <li>Requiring activated bundles to a bundle to update are resolved and optionally refreshed
 * <li>Deactivated bundles imported (providing bundles) by bundles to update are activated. This is
 * handled by the resolver hook. The resolver hook is visited by the framework during resolve.
 * </ol>
 * <p>
 * Duplicates
 * <ol>
 * <li>Workspace bundles that are duplicates (bundles with same symbolic name (version may be
 * different)) of external or jar bundles are excluded from the update process and an error status
 * object is generated and reported after update has finished. To access the error objects see
 * {@link #getErrorStatusList()}
 * <li>Workspace duplicates (bundles with same symbolic name as other workspace bundles) and their
 * requiring bundles are in contrast added to the requiring bundles of bundles to update (they are
 * in one way dependent of each other and have to be resolved. External bundles are not resolved)
 * </ol>
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
	 * This is convenience method to add the pending update transition to a set of bundle projects.
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
	 * @throws ExtenderException If failing to get the bundle transition service
	 * @see #isPendingForUpdate(IProject)
	 * @see BundleTransition#addPending(IProject, BundleTransition.Transition)
	 */
	public void addUpdateTransition(Collection<IProject> projects) throws ExtenderException;

	
	/**
	 * This is convenience method to add a pending update transition to a bundle project.
	 * 
	 * @param projects Add a pending update transition to the specified project 
	 * @throws ExtenderException If failing to get the bundle transition service
	 * @see #addPendingProjects(Collection)
	 * @see #isPendingForUpdate(IProject)
	 * @see BundleTransition#addPending(IProject, BundleTransition.Transition)
	 */
	public void addUpdateTransition(IProject project) throws ExtenderException;

	/**
	 * Checks if the specified project is pending for update.
	 * <p>
	 * An activated project that have been built is always pending for update if the "Update on Build"
	 * option is switched off.
	 * 
	 * @param project Check this project for a pending update operation
	 * @return true if the project is pending for update and false if not throws ExtenderException If
	 * failing to get the bundle transition service
	 * @see #addUpdateTransition(Collection)
	 */
	public boolean isPendingForUpdate(IProject project) throws ExtenderException;

	/**
	 * Validate the specified project for update. Build error closures that allows and prevents an
	 * update of an activated bundle project are:
	 * <ol>
	 * <li><b>Requiring update closures</b>
	 * <p>
	 * <br>
	 * <b>Activated requiring closure.</b> An update is rejected when there exists activated bundles
	 * with build errors requiring capabilities from the project to update. An update would imply a
	 * resolve (and a stop/start if in state active) of the activated requiring bundles with build
	 * errors. The preferred solution is to suspend the resolve and possibly a start to avoid the
	 * framework to throw exceptions when an activated requiring bundle with build errors is started
	 * as part of the update process (stop/resolve/start) or when started later on. From this follows
	 * that there is nothing to gain by constructing a new revision (resolving or refreshing) of a
	 * requiring bundle with build errors.
	 * <p>
	 * <br>
	 * <b>Deactivated requiring closure.</b> Deactivated bundle projects with build errors requiring
	 * capabilities from the project to update will not trigger an activation of the deactivated
	 * bundles and as a consequence the bundles will not be resolved and are therefore allowed.
	 * <p>
	 * <br>
	 * <li><b>Providing update closures</b>
	 * <p>
	 * <br>
	 * <b>Deactivated providing closure.</b> Update is rejected when deactivated bundles with build
	 * errors provides capabilities to a project to update. This would trigger an activation in the
	 * resolver hook (update and resolve) of the providing bundle project(s) and result in errors of
	 * the same type as in the <b>Activated requiring closure</b>.
	 * <p>
	 * <br>
	 * <b>Activated providing closure.</b> It is legal to update the project when there are activated
	 * bundles with build errors that provides capabilities to the project to update. The providing
	 * bundles will not be affected (updated and resolved) when the project is updated. The project to
	 * update will get wired to the current revision (that is from the last successful resolve) of the
	 * activated bundles with build errors when resolved.
	 * </ol>
	 * 
	 * @param project to validate for update
	 * @return true if the specified project may be updated and false if not
	 * @throws ExtenderException If failing to get the candidate and message options services
	 */
	public boolean canUpdate(IProject project) throws ExtenderException;

	/**
	 * Identify bundle projects to update based on the current set of pending bundle projects added to
	 * this job and calculate and return the requiring (update) closure of those bundle projects. Use
	 * {@link #addPendingProject(IProject)} or {@link #addPendingProjects(Collection)} to add bundle
	 * projects to update before calculating the requiring closure.
	 * <p>
	 * The domain or region of the requiring closure is all activated bundle projects in the workspace
	 * region.
	 * 
	 * The new set of bundle projects to update may be equal, a sub set or super set within the
	 * specified domain of the current set of bundle projects to update. The resulting set of pending
	 * bundle projects to update is a subset of the requiring (update) closure within the specified
	 * domain of bundle projects.
	 * <p>
	 * Only pending bundle projects tagged with {@code Transition#UPDATE} are added.
	 * <p>
	 * The following steps are executed to collect bundle projects to update and the requiring closure
	 * of those pending bundle projects:
	 * <ol>
	 * <li>Get all pending projects already added to this job.
	 * <li>If auto build has been switched off and than switched on again between two update jobs; -
	 * include all bundles that have been built.
	 * <li>If the "Refresh on Update" option is on, all requiring bundles tagged for update to the
	 * bundles to update are added to the list of bundles to update
	 * <li>Duplicate workspace bundles and their requiring bundles to bundles to update are added to
	 * requiring closure (see below) of the set of bundles to update (they are in one way dependent of
	 * each other and have to be resolved)
	 * <li>Any bundles to update and their requiring bundles that are duplicates of external bundles
	 * are removed for the list of bundles to update
	 * </ol>
	 * After the set of bundle projects to update are collected, the requiring closure of those bundle
	 * projects are calculated and returned. The requiring closures is the set of activated bundles
	 * that require capabilities from the bundle projects to update.
	 * 
	 * @return The requiring (update) closure of the set of identified bundle projects to update
	 */
	public Collection<IProject> getRequiringUpdateClosure() throws ExtenderException;

	/**
	 * Identify bundle projects to update based on the current set of pending bundle projects added to
	 * this job. Use {@link #addPendingProject(IProject)} or {@link #addPendingProjects(Collection)}
	 * to add bundle projects to update before calculating the new set of bundle projects to update
	 * <p>
	 * The domain or region of bundle projects to update is all activated bundle projects in the
	 * workspace region.
	 * 
	 * The new set of bundle projects to update may be equal, a sub set or super set within the
	 * specified domain of the current set of bundle projects to update. The resulting set of pending
	 * bundle projects to update is a subset of the requiring (update) closure within the specified
	 * domain of bundle projects.
	 * <p>
	 * Only pending bundle projects tagged with {@code Transition#UPDATE} are added.
	 * <p>
	 * The following steps are executed to collect bundle projects to update:
	 * <ol>
	 * <li>Get all pending projects already added to this job.
	 * <li>If auto build has been switched off and than switched on again between two update jobs; -
	 * include all bundles that have been built.
	 * <li>If the "Refresh on Update" option is on, all requiring bundles tagged for update to the
	 * bundles to update are added to the list of bundles to update
	 * <li>Duplicate workspace bundles and their requiring bundles to bundles to update are added to
	 * requiring closure (see below) of the set of bundles to update (they are in one way dependent of
	 * each other and have to be resolved)
	 * <li>Any bundles to update and their requiring bundles that are duplicates of external bundles
	 * are removed for the list of bundles to update
	 * </ol>
	 * 
	 * @return The set of bundle projects to update or an empty set
	 */
	public Collection<IProject> getBundlesToUpdate() throws ExtenderException;

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
	 * projects are not added to the returned collection. throws ExtenderException If failing to get
	 * the bundle region service
	 */
	public Collection<IProject> getUpdateOrder() throws ExtenderException;
}