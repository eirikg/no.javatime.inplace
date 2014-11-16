package no.javatime.inplace.region.intface;

import java.net.MalformedURLException;
import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.msg.Msg;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Use this service interface to determine and select valid projects to activate. A project is a
 * candidate for activation if it is a bundle project. A bundle project has the Java and plug-in
 * natures enabled.
 * <p>
 * There is an option to allow/disallow bundle projects that contributes to the UI using the Eclipse
 * UI plug-in to be activated. The default is to allow projects contributing to the UI. Different
 * methods in this class filters bundle projects that contributes to the UI based on the current
 * setting of the UI option.
 * <p>
 * To access and examine bundles projects use the {@link BundleRegion} interface. To activate and
 * deactivate bundle projects use the {@link BundleCommand} interface.
 * <p>
 * There is also a service interface to access and modify bundle project {@link BundleProjectMeta
 * meta information}. 
 * <p>
 * Use the OSGi service layer or the {@link Extenders} to access this service
 * interface.
 * <p>
 * <pre>
 * {@code Extension<BundleProjectCandidates> candidates = Extenders.getExtension(BundleProjectCandidates.class.getName());}
 * </pre>
 * 
 * @see CommandOptions#isAllowUIContributions()
 * @see CommandOptions#setIsAllowUIContributions(boolean)
 */
public interface BundleProjectCandidates {

	final public static String PLUGIN_NATURE_ID = Msg.PLUGIN_ID_NATURE_ID;

	/**
	 * Get all workspace projects that are open and exists. This includes non bundle projects
	 * 
	 * @return a list of all open and existing projects in the workspace
	 * @see #getBundleProjects()
	 */
	public Collection<IProject> getProjects();

	/**
	 * Return all open plug-in projects that have the Java and plug-in nature enabled.
	 * 
	 * @return all open plug-in projects with Java and plug-in nature or an empty collection
	 * @throws InPlaceException open projects that does not exist or a core exception when accessing
	 * projects is thrown internally (should not be the case for open and existing projects)
	 */
	public Collection<IProject> getBundleProjects() throws InPlaceException;

	/**
	 * Find all projects that fulfill the requirements to be activated. Already activated projects and
	 * projects that contributes to the UI when UI contributors are not allowed are not part of the
	 * returned collection
	 * 
	 * @return all projects that may be activated or an empty collection
	 * @throws InPlaceException open projects that does not exist or a core exception when accessing
	 * projects is thrown internally (should not be the case for open and existing projects)
	 * @see #isCandidate(IProject)
	 */
	public Collection<IProject> getCandidates() throws InPlaceException;

	/**
	 * Return all bundle projects that not contributes to the UI if UI contributors are not allowed.
	 * If UI contributers are allowed this is the same as calling {@link #getBundleProjects()}
	 * 
	 * @return all bundle projects which not contributes to the UI, when UI contributions are not
	 * allowed If UI contributions are allowed, return all bundle projects or an empty collection
	 * @throws InPlaceException open projects that does not exist or a core exception when accessing
	 * projects is thrown internally (should not be the case for open and existing projects)
	 * @see #isUIPlugin(IProject)
	 * @see #getUIPlugins()
	 */
	public Collection<IProject> getInstallable() throws InPlaceException;

	/**
	 * Get all plug-in projects that contributes to the UI and their requiring projects
	 * 
	 * @return set of plug-in projects contributing to the UI or an empty collection
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @throws InPlaceException open projects that does not exist or a core exception when accessing
	 * projects is thrown internally (should not be the case for open and existing projects)
	 */
	public Collection<IProject> getUIPlugins() throws InPlaceException, CircularReferenceException;

	/**
	 * Get a general project based on it's name. The project does not have to be a bundle project.
	 * 
	 * @param name a project name
	 * @return the project or null if the given project is null
	 * @throws InPlaceException project is null
	 */
	public IProject getProject(String name) throws InPlaceException;

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param project a given project
	 * @return the java project or null if the java project is closed or the java project is null
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown (should not be the case)
	 */
	public IJavaProject getJavaProject(IProject project) throws InPlaceException;

	/**
	 * Find and return a project based on the specified location identifier and scheme
	 * 
	 * @param location identifier string prepended with the file protocol (by value)
	 * @param locationScheme a valid URI location scheme
	 * @return the project or null if not found or if the project is closed
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @throws InPlaceException open but does not exist or a core exception is thrown internally
	 * (should not be the case for open and existing projects)
	 * @throws MalformedURLException if no protocol is specified, or an unknown protocol is found, or
	 * specification is null.
	 * @see BundleRegion#getProjectLocationIdentifier(IProject, String)
	 * @see BundleRegion#BUNDLE_REF_LOC_SCHEME
	 * @see BundleRegion#BUNDLE_FILE_LOC_SCHEME
	 */
	public IProject getProjectFromLocation(String location, String locationScheme)
			throws ProjectLocationException, InPlaceException, MalformedURLException;

	/**
	 * Return true if this project is open and has the Java and plug-in nature enabled.
	 * 
	 * @param project the project to check for the Java and and plug-in nature
	 * @return true if the project is open and has the Java and plug-in nature enabled. False
	 * otherwise
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown internally (should not be the case for open and existing projects)
	 */
	public Boolean isBundleProject(IProject project) throws InPlaceException;

	/**
	 * A project is installable if it is open, has the Java and plug-in nature enabled, activated or
	 * deactivated, not contributes to the UI if UI contributors are not allowed or contributes to the
	 * UI if UI contributions are allowed.
	 * 
	 * @return true if this is a Java plug-in or JavaTime project or false
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown internally (should not be the case for open and existing projects)
	 */
	public Boolean isInstallable(IProject project) throws InPlaceException;

	/**
	 * A project is a candidate project if it is open, not activated, has the plug-in and Java nature
	 * enabled, and not contributes to the UI if UI contributors are not allowed or contributes to the
	 * UI if UI contributions are allowed
	 * 
	 * @param project to check for candidate nature attributes
	 * @return true if project is not activated, a plug-in project with Java, plug-in nature and not
	 * contributing to the UI, when not allowed. Otherwise false.
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown internally (should not be the case for open and existing projects)
	 */
	public Boolean isCandidate(IProject project) throws InPlaceException;

	/**
	 * Check if this project is dependent on the Eclipse UI plug-in (org.eclipse.ui)
	 * 
	 * @param project to check for dependency on the UI plug-in
	 * @return true if this project is dependent on the UI plug-in, otherwise false
	 * @throws InPlaceException if project is null or failed to get the bundle project description
	 */
	public Boolean isUIPlugin(IProject project) throws InPlaceException;

	/**
	 * Check if the project is open and enabled with the specified nature.
	 * <P>
	 * Note that false is returned if the project is closed.
	 * 
	 * @param project the project enabled with the specific nature
	 * @param natureId the nature identifier to check against the specified project
	 * @return true if the project is nature enabled with the specified nature. False if the project
	 * is closed or not nature enabled with the specified nature
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown internally (should not be the case for open and existing projects)
	 */
	public Boolean isNatureEnabled(IProject project, String natureId) throws InPlaceException;

	/**
	 * Convert from java projects to general projects
	 * 
	 * @param javaProjects set of java projects
	 * @return set of projects
	 */
	public Collection<IProject> toProjects(Collection<IJavaProject> javaProjects);

	/**
	 * Convert from general projects to java projects
	 * 
	 * @param projects set of general projects
	 * @return set of java projects
	 */
	public Collection<IJavaProject> toJavaProjects(Collection<IProject> projects);

	/**
	 * Formats the collection as a comma separated list.
	 * 
	 * @param projects collection of projects to format
	 * @return a comma separated list of projects or an empty string
	 */
	public String formatProjectList(Collection<IProject> projects);

	/**
	 * Enables auto build
	 * 
	 * @param autoBuild true to enable and false to disable
	 * @return previous state of auto build
	 */
	public Boolean setAutoBuild(Boolean autoBuild);

	/**
	 * A delegate for checking if auto build is enabled
	 * 
	 * @return true if auto build is enabled, false if not
	 */
	public Boolean isAutoBuilding();

}