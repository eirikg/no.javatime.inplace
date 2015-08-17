package no.javatime.inplace.region.intface;

import java.util.Collection;
import java.util.Map;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.msg.Msg;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * The bundle region isolates all bundles that can be associated with a workspace project from other
 * bundles. Use the bundle region to access, add and modify bundle projects and their attributes.
 * <p>
 * You can access related service interfaces from the bundle region service to manage bundle life
 * cycle, bundle transitions, bundle meta data, project descriptions, and identify candidate bundle
 * projects.
 * <p>
 * There is a 1:1 relationship between a project and its bundle. The term bundle project refers to a
 * project with the java and plug-in nature enabled, the relation (e.g. the symbolic name and
 * version or the location identifier) and the bundle combined.
 * <p>
 * A bundle project is either activated or deactivated. A bundle project is activated when
 * registered or installed with the activated attribute set to {@code true}. A deactivated bundle
 * can be installed but not resolved. If one or more bundle projects are activated the workspace is
 * said to be activated. In a deactivated workspace all bundles are deactivated and individual
 * bundles are in state uninstalled or installed.
 * 
 */
public interface BundleRegion {

	/**
	 * Manifest header for accessing the default service implementation class name of the bundle
	 * region
	 */
	public final static String BUNDLE_REGION_SERVICE = "Bundle-Region-Service";

	/**
	 * Reference location of bundle projects used when installing bundles.
	 * 
	 * @see #getBundleLocationIdentifier(IProject)
	 */
	final public static String BUNDLE_REF_LOC_SCHEME = Msg.BUNDLE_ID_REF_SCHEME_REF;

	/**
	 * File location scheme
	 * 
	 * @see #getProjectLocationIdentifier(IProject, String)
	 */
	final public static String BUNDLE_FILE_LOC_SCHEME = Msg.BUNDLE_ID_FILE_SCHEME_REF;

	/**
	 * Get the bundle command service
	 * <p>
	 * Any extender exceptions are sent to the error log view and null is returned
	 * 
	 * @param user TODO
	 * @user The bundle using this service
	 * @return the bundle command service interface or null if not available
	 * @throws ExtenderException If the bundle context of the service is no longer valid or the
	 * returned service from the framework is null
	 */
	public BundleCommand getCommandService(Bundle user) throws ExtenderException;

	/**
	 * Get the bundle transition service
	 * <p>
	 * Any extender exceptions are sent to the error log view and null is returned
	 * 
	 * @param user TODO
	 * 
	 * @user The bundle using this service
	 * @return the bundle transition service interface or null if not available
	 * @throws ExtenderException If the bundle context of the service is no longer valid or the
	 * returned service from the framework is null
	 */
	public BundleTransition getTransitionService(Bundle user) throws ExtenderException;

	/**
	 * Get the bundle project candidates service
	 * <p>
	 * Any extender exceptions are sent to the error log view and null is returned
	 * 
	 * @param user TODO
	 * 
	 * @user The bundle using this service
	 * @return the bundle project candidates service interface or null if not available
	 * @throws ExtenderException If the bundle context of the service is no longer valid or the
	 * returned service from the framework is null
	 */
	public BundleProjectCandidates getCandidatesService(Bundle user) throws ExtenderException;

	/**
	 * Get the bundle project meta service
	 * <p>
	 * Any extender exceptions are sent to the error log view and null is returned
	 * 
	 * @param user TODO
	 * 
	 * @user The bundle using this service
	 * @return the project meta service interface or null if not available
	 * @throws ExtenderException If the bundle context of the service is no longer valid or the
	 * returned service from the framework is null
	 */
	public BundleProjectMeta getMetaService(Bundle user) throws ExtenderException;

	/**
	 * Retrieves the project location identifier as an absolute file system path of the specified
	 * project prepended with the specified URI scheme. Uses the platform-dependent path separator.
	 * This method is used internally with the {@link BundleRegion#BUNDLE_REF_LOC_SCHEME} scheme when
	 * bundles are installed.
	 * <p>
	 * After a bundle is installed the path returned from {@linkplain Bundle#getLocation()} equals the
	 * path returned from this method when the reference scheme parameter is used. This method use
	 * {@linkplain IProject#getLocation()} internally.
	 * 
	 * @param project which is the base for finding the path
	 * @param locationScheme a valid URI location scheme or null. If null the reference location
	 * schema is used
	 * @return the absolute file system path of the project prepended with the specified URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @see IProject#getLocation()
	 * @see Bundle#getLocation()
	 * @see BundleRegion#BUNDLE_REF_LOC_SCHEME
	 * @see BundleRegion#BUNDLE_FILE_LOC_SCHEME
	 */
	public String getProjectLocationIdentifier(IProject project, String locationScheme);

	/**
	 * Retrieves the bundle location identifier as an absolute platform-dependent file system path of
	 * the specified project prepended with the reference file scheme (reference:file:/).
	 * <p>
	 * If the associated workspace bundle of the specified project is installed
	 * {@link Bundle#getLocation()} is used. The bundle location identifier of an installed bundle
	 * equals the project location identifier.
	 * 
	 * @param project which is the base for finding the path
	 * @return the absolute file system path of project prepended with the URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found. This exception can only occur for uninstalled bundles as
	 * long as the specified project is valid.
	 * @throws InPlaceException If the caller does not have the appropriate
	 * AdminPermission[this,METADATA], and the Java Runtime Environment supports permissions.
	 * @see #getProjectLocationIdentifier(IProject, String)
	 */
	public String getBundleLocationIdentifier(IProject project) throws ProjectLocationException,
			InPlaceException;

	/**
	 * Register the specified project and the associated bundle with the region as a bundle project.
	 * If the specified bundle does not exist, is invalid or is null it is initialized with state
	 * {@code StateLess} and {@code Transition.NO_TRANSITION}
	 * <p>
	 * If the specified activation status is true and the bundle is in state
	 * {@code Bundle.UNINSTALLED} or in state {@code Bundle.INSTALLED} a
	 * {@code Transition.ACTIVATE_BUNDLE} is added as a pending command. This indicates that the
	 * bundle should be resolved and optionally started.
	 * <p>
	 * If the the specified activation status is false and the bundle state is {@code Bundle.RESOLVED}, {@code Bundle.STARTING}, {@code Bundle.ACTIVE} or {@code Bundle.STOPPING} a
	 * {@code Transition.DEACTIVATE} is added as a pending bundle command. This indicates that the
	 * bundle should be deactivated.
	 * <p>
	 * You can control this manually by removing pending operations on a registered bundle, and then
	 * invoke one or more of the bundle commands
	 * <p>
	 * A bundle project is otherwise registered automatically when installed. If the bundle project is
	 * already registered it is updated with the specified bundle and the specified activation status
	 * 
	 * When the same bundle project is registered multiple times the bundle and the activation status
	 * of the bundle is updated and any existing information about the bundle project is retained.
	 * 
	 * @param project project to register. Must not be null
	 * @param bundle bundle to register. May be null
	 * @param acivateBundle true to mark the bundle as activated and false to mark the bundle as
	 * deactivated.
	 * @see #unregisterBundleProject(IProject)
	 * @see #install(IProject, Boolean)
	 */
	public void registerBundleProject(IProject project, Bundle bundle, boolean activate);

	/**
	 * Unregister the specified workspace region project. Unregistering a project also unregisters
	 * it's associated bundle
	 * 
	 * @param bundle bundle project to unregister
	 * @see #registerBundleProject(IProject, Bundle, boolean)
	 * @see #isProjectRegistered(IProject)
	 * @see #uninstall(Bundle, Boolean)
	 */
	public void unregisterBundleProject(IProject project);

	/**
	 * Check if the specified project is registered as a workspace bundle project
	 * 
	 * @param project to check for registration as a workspace bundle project
	 * @return true if bundle project is registered as a bundle project and false if not
	 */
	public boolean isProjectRegistered(IProject project);

	/**
	 * Get associated projects for all installed (activated and deactivated) bundles
	 * 
	 * @return associated projects for all installed bundles
	 */
	public Collection<IProject> getProjects();

	/**
	 * Get associated projects for the specified bundles
	 * 
	 * @param bundles with associated projects
	 * @return associated projects for the specified bundles
	 */
	public Collection<IProject> getProjects(Collection<Bundle> bundles);

	/**
	 * Get all deactivated projects or all activated projects in an activated workspace
	 * 
	 * @param activated if true get all activated projects. If false get all deactivated projects.
	 * @return all activated projects if the specified activation parameter is true, otherwise return
	 * all deactivated projects
	 */
	public Collection<IProject> getProjects(Boolean activated);

	/**
	 * Get the registered project associated with the specified bundle. Note that only projects
	 * registered as workspace region bundle projects are searched.
	 * <p>
	 * If a project is not registered as bundle project manually using
	 * {@link BundleCommand#registerBundleProject(IProject, Bundle, boolean)} it will always be
	 * registered by the {@link BundleCommand#install(IProject, Boolean)} command
	 * 
	 * @param bundle the bundle associated with the project to return
	 * @return the associated project of the specified bundle or null if no project is found
	 * @see BundleCommand#registerBundleProject(IProject, Bundle, boolean)
	 */
	public IProject getProject(Bundle bundle);

	/**
	 * Get the project containing the specified symbolic name and version.
	 * 
	 * @param symbolicName of the project to return
	 * @param version version of the specified symbolic name
	 * @return project containing the specified symbolic name and version {@code null} if no project
	 * matching the specified symbolic name and version.
	 */
	public IProject getProject(String symbolicName, String version);

	/**
	 * Check if the workspace is activated. The condition is satisfied if one bundle is activated
	 * 
	 * @return true if at least one bundle is activated. Otherwise false
	 * @see BundleRegion#isBundleActivated(Bundle)
	 */
	public Boolean isRegionActivated();

	/**
	 * Determine if a bundle that is member of the workspace region is currently executing a bundle
	 * operation
	 * 
	 * @return the bundle currently executing a bundle operation
	 */
	public Bundle isRegionStateChanging();

	/**
	 * Check if the bundle associated with the specified project is activated. The condition is
	 * satisfied if the project and the associated bundle is registered with the region.
	 * 
	 * @param bundleProject to check for activation
	 * @return true if the specified project is registered with the region and the bundle object is
	 * registered and activated. If not activated or the specified project or the bundle is not
	 * registered with the region, false is returned.
	 * @see BundleRegion#isBundleActivated(Bundle)
	 */
	public Boolean isBundleActivated(IProject bundleProject);

	/**
	 * Check if the bundle with the specified bundle id is activated. The condition is satisfied if
	 * the project and the associated bundle is registered with the region.
	 * 
	 * @param bundleId id of bundle object to check for activation mode
	 * @return true if the bundle object is activated. If not activated or the specified bundle is not
	 * registered with the region, false is returned.
	 */
	public Boolean isBundleActivated(Long bundleId);

	/**
	 * Check if the specified bundle is activated. The condition is satisfied if the associated
	 * project of the bundle and the specified bundle is registered with the region.
	 * 
	 * @param bundle bundle object to check for activation mode
	 * @return true if the bundle object is activated. If not activated or the specified bundle is not
	 * registered with the region, false is returned.
	 */
	public Boolean isBundleActivated(Bundle bundle);

	/**
	 * Get all activated projects
	 * 
	 * @return all activated projects or an empty collection
	 */
	public Collection<IProject> getActivatedProjects();

	/**
	 * Get all activated bundles.
	 * 
	 * @return all activated bundles or an empty collection
	 */
	public Collection<Bundle> getActivatedBundles();

	/**
	 * Get all installed (activated and not activated) bundles
	 * 
	 * @return all installed bundles or an empty collection
	 */
	public Collection<Bundle> getBundles();

	/**
	 * Get all deactivated (installed) bundles in an activated workspace. If the workspace is
	 * deactivated all bundles are in state {@code Bundle#UNINSTALLED}
	 * 
	 * @return all deactivated bundles or an empty collection
	 */
	public Collection<Bundle> getDeactivatedBundles();

	/**
	 * Get all activated bundles with the specified state(s). Note that uninstalled bundles are not
	 * activated and thus not part of the bundle region.
	 * 
	 * @param state a bundle state obtained from on or more of the {@Bundle} state constants
	 * except {@linkplain Bundle#UNINSTALLED}
	 * @return all bundles that matches the specified state(s) or an empty collection
	 */
	public Collection<Bundle> getBundles(int state);

	/**
	 * Return all specified bundles with the specified state(s).
	 * <p>
	 * Bundles not registered with the associated project in the region are ignored.
	 * 
	 * @param bundles bundles to check for the specified state
	 * @param state a bundle state obtained from on or more of the {@Bundle} state constants
	 * except {@linkplain Bundle#UNINSTALLED}
	 * @return all bundles that matches the specified state(s) or an empty collection
	 */
	public Collection<Bundle> getBundles(Collection<Bundle> bundles, int state);

	/**
	 * Get associated bundles for the specified projects.
	 * <p>
	 * Bundles not registered with the associated project in the region are ignored.
	 * 
	 * @param projects with associated bundles
	 * @return associated bundles of the specified projects, or an empty collection
	 */
	public Collection<Bundle> getBundles(Collection<IProject> projects);

	/**
	 * Get the bundle associated with the specified project
	 * 
	 * @param project bundle project with an associated bundle
	 * @return the associated bundle of the specified project or null if the bundle is not registered
	 * with the project in the region or null is returned by the framework
	 */
	public Bundle getBundle(IProject project);

	/**
	 * Get the bundle object from the specified bundle id
	 * 
	 * @param bundleId the id used to retrieve the bundle object
	 * @return the associated bundle of the specified project or null if the bundle is not registered
	 * with the project in the region or null is returned by the framework
	 */
	public Bundle getBundle(Long bundleId);

	/**
	 * Get all installed external bundles
	 * 
	 * @return all installed jar bundles or an empty collection
	 */
	public Collection<Bundle> getJarBundles();

	/**
	 * If the symbolic name and version of one of the specified projects equals one of the specified
	 * candidate projects its a duplicate. Only activated projects are considered. It is not the
	 * cached symbolic keys, but the symbolic keys from the manifest that are compared
	 * 
	 * @param projects to check for duplicates against the candidate bundles.
	 * @param candidates a set of projects to find duplicates among
	 * @return map containing the specified project and the bundle which the specified project is a
	 * duplicate of
	 */
	public Map<IProject, IProject> getWorkspaceDuplicates(Collection<IProject> projects,
			Collection<IProject> candidates);

	/**
	 * If the symbolic name (the version may be different) of one of the specified projects equals one
	 * of the specified candidate bundles its a duplicate.
	 * <p>
	 * The specified candidate bundles and projects sets may contain common bundle projects.
	 * <p>
	 * The persisted symbolic name of the specified projects is compared for equality against the
	 * cached symbolic name of the candidate bundles
	 * 
	 * @param projects to check for duplicates against the candidate bundles.
	 * @param candidateBundles a set of candidate bundles to find duplicates among
	 * @return map containing the specified project and the candidate bundle which the specified
	 * project is a duplicate of
	 */
	public Map<IProject, Bundle> getSymbolicNameDuplicates(Collection<IProject> projects,
			Collection<Bundle> candidateBundles);

	/**
	 * A bundle exist in the bundle region if it at least is installed. In an activated workspace all
	 * bundles are at least installed.
	 * 
	 * @param bundle the bundle to check for existence
	 * @return true if the bundle exist in the bundle region. Otherwise false
	 */
	public boolean exist(Bundle bundle);

	/**
	 * A bundle exist in the bundle region if it at least is installed. In an activated workspace all
	 * bundles are at least installed.
	 * 
	 * @param symbolicName the symbolic name of bundle to check for existence
	 * @param version the version of the bundle with the specified symbolic name
	 * @return true if the bundle exist in the bundle region. Otherwise false
	 */
	public Boolean exist(String symbolicName, String version);

	/**
	 * If the specified status parameter is {@code true} activate the bundle. If the status parameter
	 * is {@code false}, deactivate the bundle.
	 * 
	 * @param bundle the bundle to activate or deactivate
	 * @param status if status is {@code true} activate the bundle. If {@code false} deactivate the
	 * bundle
	 * @return true if the activation setting was performed or {@code false} if the bundle does not
	 * exist in the region.
	 */
	public boolean setActivation(Bundle bundle, Boolean status);

	/**
	 * If the specified status parameter is {@code true} activate the bundle. If the status parameter
	 * is {@code false}, deactivate the bundle.
	 * 
	 * @param project the project associated with the bundle (bundle project) to activate or
	 * deactivate
	 * @param status if status is {@code true} activate the bundle. If {@code false} deactivate the
	 * bundle
	 * @return true if the activation setting was performed or {@code false} if the bundle does not
	 * exist in the region.
	 */
	public boolean setActivation(IProject project, Boolean status);

	/**
	 * Concatenates symbolic name and bundle version (<symbolic name>_<version>). If the bundle is not
	 * {@code null} the specified bundle is used to get the symbolic key, otherwise the specified
	 * project is used. If both are {@code null} or the key could not be obtained an empty string is
	 * returned.
	 * 
	 * @param project containing key to format
	 * @param bundle containing the key to format
	 * @return the symbolic key as a concatenation of the symbolic name and the version or an empty
	 * string
	 */
	public String getSymbolicKey(Bundle bundle, IProject project);

	/**
	 * Formats a comma separated list of bundle symbolic names optionally appended with the version
	 * 
	 * @param bundles bundles to format
	 * @param includeVersion appends the version to the symbolic name. The symbolic name end version
	 * is delimited with an underscore
	 * @return a comma separated list of bundle or an empty string
	 */
	public String formatBundleList(Collection<Bundle> bundles, boolean includeVersion);
}