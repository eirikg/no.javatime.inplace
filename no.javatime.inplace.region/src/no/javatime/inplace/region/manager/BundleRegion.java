package no.javatime.inplace.region.manager;

import java.util.Collection;
import java.util.Map;

import no.javatime.inplace.region.project.BundleProjectState;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public interface BundleRegion {

	/**
	 * Get the registered project associated with the specified bundle. Note that only projects
	 * registered as workspace region bundle projects are searched.
	 * <p>
	 * There is no requirement that the bundle is registered to find a registered project 
	 * <p>
	 * If a project is not registered as bundle project manually using 
	 * {@link BundleCommand#registerBundleProject(IProject, Bundle, boolean)} it will always
	 * be registered by the {@link BundleCommand#install(IProject, Boolean)} command  
	 * 
	 * 
	 * @param bundle the bundle associated with the project to return
	 * @return the associated project of the specified bundle or null if no project is found
	 * @see BundleCommand#registerBundleProject(IProject, Bundle, boolean)
	 */
	public IProject getRegisteredBundleProject(Bundle bundle);

	/**
	 * Get the associated project of the specified bundle. 
	 * <p>
	 * First search registered bundle projects than search the entire workspace for the project
	 * 
	 * @param bundle the bundle associated with the project to return
	 * @return the associated project of the specified bundle or null if no project is found
	 */
	public IProject getBundleProject(Bundle bundle);
	
	/**
	 * Get the project containing the specified symbolic name and version.
	 * 
	 * @param symbolicName of the project to return
	 * @param version version of the specified symbolic name
	 * @return project containing the specified symbolic name and version {@code null} if no project matching
	 *         the specified symbolic name and version.
	 */
	public IProject getBundleProject(String symbolicName, String version);

	/**
	 * Retrieves the bundle location identifier as an absolute platform-dependent file system path of the
	 * specified project prepended with the reference file scheme (reference:file:/).
	 * <p>
	 * If the associated workspace bundle of the specified project is installed {@link Bundle#getLocation()} is
	 * used. The bundle location identifier equals the project location identifier used when the bundle was
	 * installed.
	 * 
	 * @param project which is the base for finding the path
	 * @return the absolute file system path of project prepended with the URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found. This exception can only occur for uninstalled bundles as long as
	 *           the specified project is valid.
	 * @throws InPlaceException If the caller does not have the appropriate AdminPermission[this,METADATA], 
	 *           and the Java Runtime Environment supports permissions.
	 */
	public String getBundleLocationIdentifier(IProject project) throws ProjectLocationException, InPlaceException;

	/**
	 * Check if the workspace is activated. The condition is satisfied if one project is JavaTime nature enabled
	 * and its bundle project is at least installed.
	 * 
	 * @return true if at least one project is JavaTime nature enabled and its bundle project is not
	 *         uninstalled. Otherwise false
	 * @see BundleWorkspaceRegionImpl#isActivated(Bundle)
	 * @see BundleProjectState#isWorkspaceNatureEnabled()
	 */
	public Boolean isBundleWorkspaceActivated();

	/**
	 * Check if the bundle is activated. The condition is satisfied if the project is JavaTime nature enabled
	 * and its bundle project is at least installed.
	 * 
	 * @param bundleProject to check for activation
	 * @return true if the specified project is JavaTime nature enabled and its bundle project is not
	 *         uninstalled. Otherwise false
	 * @see BundleWorkspaceRegionImpl#isActivated(Bundle)
	 * @see BundleProjectState#isWorkspaceNatureEnabled()
	 */
	public Boolean isActivated(IProject bundleProject);

	/**
	 * Check if the bundle with the specified bundle id is activated. The condition is satisfied if it
	 * associated project is JavaTime nature enabled and the bundle is at least installed.
	 * 
	 * @param bundleId id of bundle object to check for activation mode
	 * @return true if the bundle object of the specified bundle id is activated. Otherwise false
	 */
	public Boolean isActivated(Long bundleId);

	/**
	 * Check if the specified bundle is activated. The condition is satisfied if it associated project is
	 * JavaTime nature enabled and the bundle is at least installed.
	 * 
	 * @param bundle bundle object to check for activation mode
	 * @return true if the bundle object is activated. Otherwise false
	 */
	public Boolean isActivated(Bundle bundle);

	/**
	 * Get either all deactivated projects in an activated workspace or all activated projects dependent on the
	 * specified activated switch
	 * 
	 * @param activated if true get all activated projects. If false get all deactivated projects.
	 * @return all activated projects if the specified activation parameter is true, otherwise return all
	 *         deactivated projects
	 */
	public Collection<IProject> getBundleProjects(Boolean activated);

	/**
	 * Get all activated bundles.
	 * 
	 * @return all activated bundles.
	 */
	public Collection<Bundle> getActivatedBundles();

	/**
	 * Get all deactivated (installed) bundles in an activated workspace. If the workspace is deactivated all
	 * bundles are in state {@code Bundle#UNINSTALLED}
	 * 
	 * @return all deactivated bundles.
	 */
	public Collection<Bundle> getDeactivatedBundles();

	/**
	 * Get all activated bundles with the specified state(s). Note that uninstalled bundles are not activated
	 * and thus not part of the bundle region.
	 * 
	 * @param state a bundle state obtained from on or more of the {@Bundle} state constants except
	 *          {@linkplain Bundle#UNINSTALLED}
	 * @return all bundles that matches the specified state(s) or an empty set
	 */
	public Collection<Bundle> getBundles(int state);

	/**
	 * Return all specified bundles with the specified state(s).
	 * 
	 * @param bundles bundles to check for the specified state
	 * @param state a bundle state obtained from on or more of the {@Bundle} state constants except
	 *          {@linkplain Bundle#UNINSTALLED}
	 * @return all bundles that matches the specified state(s) or an empty set
	 */
	public Collection<Bundle> getBundles(Collection<Bundle> bundles, int state);

	/**
	 * Get associated projects for all installed (activated and not activated) bundles
	 * 
	 * @return associated projects for all installed bundles
	 */
	public Collection<IProject> getBundleProjects();

	/**
	 * Get associated projects for the specified bundles
	 * 
	 * @param bundles with associated projects
	 * @return associated projects for the specified bundles
	 */
	public Collection<IProject> getBundleProjects(Collection<Bundle> bundles);

	/**
	 * Get associated bundles for the specified projects
	 * 
	 * @param projects with associated bundles
	 * @return associated bundles of the specified projects, or an empty collection
	 */
	public Collection<Bundle> getBundles(Collection<IProject> projects);

	/**
	 * Get all installed (activated and not activated) bundles
	 * 
	 * @return all installed bundles or an empty collection
	 */
	public Collection<Bundle> getBundles();

	/**
	 * Get all installed external bundles
	 * 
	 * @return all installed jar bundles or an empty collection
	 */
	public Collection<Bundle> getJarBundles();

	/**
	 * Get the bundle object from the specified bundle id
	 * 
	 * @param bundleId the id used to retrieve the bundle object
	 * @return the bundle object or null
	 */
	public Bundle get(Long bundleId);

	/**
	 * Finds duplicates among the specified duplicate project candidates. Only activated projects are
	 * considered. It is not the cached symbolic keys, but the symbolic keys from the manifest that are compared
	 * 
	 * @param projects duplicate candidates
	 * @param scope TODO
	 * @return map containing the specified project and the bundle which the specified project is a duplicate of
	 */
	public Map<IProject, IProject> getWorkspaceDuplicates(Collection<IProject> projects, Collection<IProject> scope);

	/**
	 * Finds duplicates to the specified workspace duplicate project candidates. If the symbolic
	 * name (the version may be different) of one of the specified projects equals one of the specified
	 * candidate bundle its a duplicate. 
	 * <p>
	 * The persisted symbolic name of the specified projects is compared for equality against the cached
	 * symbolic name of the candidate bundles
	 * @param projects to check for duplicates against the candidate bundles.
	 * @param candidateBundles a set of candidate bundles to find duplicates among
	 * @param disjoint set to true to check and remove bundles in candidate bundles that also exist in the
	 * specified collection of projects. If false no check or removal is performed.
	 * @return map containing the specified project and the candidate bundle which the specified project is a
	 *         duplicate of
	 */
	public Map<IProject, Bundle> getSymbolicNameDuplicates(Collection<IProject> projects,
			Collection<Bundle> candidateBundles, boolean disjoint);

	/**
	 * A bundle exist in the bundle region if it at least is installed. In an activated workspace all bundles
	 * are at least installed. An exception is if at shutdown a bundle is a duplicate or part of a cycle. The
	 * bundle will then be in state uninstalled at startup, but installed and if activated started when the
	 * error is corrected.
	 * 
	 * @param bundle the bundle to check for existence
	 * @return true if the bundle exist in the bundle region. Otherwise false
	 */
	public Boolean exist(Bundle bundle);

	/**
	 * A bundle exist in the bundle region if it at least is installed. In an activated workspace all bundles
	 * are at least installed. An exception is if at shutdown a bundle is a duplicate or part of a cycle. The
	 * bundle will then be in state uninstalled at startup, but installed and if activated started when the
	 * error is corrected.
	 * 
	 * @param symbolicName the symbolic name of bundle to check for existence
	 * @param version the version of the bundle with the specified symbolic name
	 * @return true if the bundle exist in the bundle region. Otherwise false
	 */
	public Boolean exist(String symbolicName, String version);

	/**
	 * If the specified status parameter is {@code true} activate the bundle. If the status parameter is
	 * {@code false}, deactivate the bundle.
	 * 
	 * @param bundle the bundle to activate or deactivate
	 * @param status if status is {@code true} activate the bundle. If {@code false} deactivate the bundle
	 * @return true if the activation setting was performed or {@code false} if the bundle does not exist in the region.
	 */
	public boolean setActivation(Bundle bundle, Boolean status);

	/**
	 * If the specified status parameter is {@code true} activate the bundle. If the status parameter is
	 * {@code false}, deactivate the bundle.
	 * 
	 * @param project the project associated with the bundle (bundle project) to activate or deactivate
	 * @param status if status is {@code true} activate the bundle. If {@code false} deactivate the bundle
	 * @return true if the activation setting was performed or {@code false} if the bundle does not exist in the region.
	 */
	public boolean setActivation(IProject project, Boolean status);
	
		/**
	 * Concatenates symbolic name and bundle version (<symbolic name>_<version>). If the bundle is not
	 * {@code null} the specified bundle is used to get the symbolic key, otherwise the specified project is
	 * used. If both are {@code null} or the key could not be obtained an empty string is returned.
	 * 
	 * @param project containing key to format
	 * @param bundle containing the key to format
	 * @return the symbolic key as a concatenation of the symbolic name and the version or an empty string
	 */
	public String getSymbolicKey(Bundle bundle, IProject project);

	/**
	 * Get the bundle associated with specifies project
	 * 
	 * @param project project with an associated bundle
	 * @return the associated bundle of the specified project or null if the bundle is
	 * not registered
	 */
	public Bundle get(IProject project);

	/**
	 * If changed to automatic build. May be false even if auto build is true.
	 * 
	 * @param disable set true to indicate that the auto build is not switched on.
	 * @return true if auto build were set. Otherwise false
	 */
	public boolean isAutoBuildActivated(boolean disable);

	/**
	 * Set if auto build changes to true.
	 * 
	 * @param autoBuild true if auto build is switched on. Otherwise false
	 */
	public void setAutoBuildChanged(boolean autoBuild);

	/**
	 * Formats the collection as a comma separated list.
	 * 
	 * @param bundles collection of bundles to format
	 * @return a comma separated list of bundles or an empty string
	 */
	public String formatBundleList(Collection<Bundle> bundles, boolean includeVersion);

	// TODO Duplicated from bundleproject
	public String getSymbolicNameFromManifest(IProject project) throws InPlaceException;
	public String getBundleVersionFromManifest(IProject project) throws InPlaceException;	
}