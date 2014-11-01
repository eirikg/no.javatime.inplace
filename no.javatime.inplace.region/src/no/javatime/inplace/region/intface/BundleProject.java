package no.javatime.inplace.region.intface;

import java.util.Collection;

import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.msg.Msg;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.Bundle;

public interface BundleProject {
	
	final public static String PLUGIN_NATURE_ID = Msg.PLUGIN_ID_NATURE_ID;
	/**
	 * Find all projects that fulfill the requirements to be activated. Already activated projects and projects
	 * that contributes to the UI when UI contributors are not allowed are not part of the returned collection
	 * 
	 * @return all projects that may be activated or an empty collection
	 * @see #isCandidate(IProject)
	 */
	public Collection<IProject> getCandidates();

	/**
	 * Return all plug-in projects that have Java and plug-in nature enabled and not contributes to the UI if UI
	 * contributors are not allowed
	 * 
	 * @return all plug-in projects with Java, plug-in nature and not contributing to the UI, when not allowed
	 *         or an empty collection
	 */
	public Collection<IProject> getInstallable();

	/**
	 * Return all plug-in projects that have the Java and plug-in nature enabled. 
	 * Closed and non-existing projects are discarded
	 * 
	 * @return all plug-in projects with Java and plug-in nature or an empty collection
	 */
	public Collection<IProject> getPlugIns();

	/**
	 * Checks if this project has the Java and plug-in nature enabled.
	 * 
	 * @return true if this is a Java plug-in project or false
	 */
	public Boolean isPlugIn(IProject project);

	/**
	 * Checks if this project has the Java and plug-in or the JavaTime nature enabled. Projects in the UI name space are
	 * included if UI contributed projects are allowed.
	 * 
	 * @return true if this is a Java plug-in or JavaTime project or false
	 */
	public Boolean isInstallable(IProject project);

	/**
	 * A project is a candidate project if it is not activated, has the plug-in and Java nature enabled, and not
	 * contributes to the UI if UI contributors are not allowed
	 * 
	 * @param project to check for candidate nature attributes
	 * @return true if project is not activated, a plug-in project with Java, plug-in nature and not
	 *         contributing to the UI, when not allowed. Otherwise false.
	 */
	public Boolean isCandidate(IProject project);

	/**
	 * Get all plug-in projects that contributes to the UI and their requiring projects
	 * 
	 * @return set of plug-in projects contributing to the UI or an empty collection
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 */
	public Collection<IProject> getUIContributors() throws CircularReferenceException;

	/**
	 * Check if this project is dependent on the Eclipse UI plug-in (org.eclipse.ui)
	 * 
	 * @param project to check for dependency on the UI plug-in
	 * @return true if this project is dependent on the UI plug-in, otherwise false
	 * @throws InPlaceException if project is null or failed to get the bundle project description
	 */
	public Boolean isUIContributor(IProject project) throws InPlaceException;

	/**
	 * Check if a project is JavaTime nature enabled (activated). The condition is satisfied if one workspace
	 * project is activated. This only implies that one or more projects are JavaTime enabled, and does not
	 * necessary mean that the corresponding bundle is activated.
	 * 
	 * @return true if at least one project is activated and false if no projects are activated
	 */
	public Boolean isWorkspaceNatureEnabled();

	/**
	 * When a project has JavaTime nature enabled the project is activated.
	 * 
	 * @param project to check for JavaTime nature
	 * @return true if JavaTime nature is enabled for the project and false if not
	 * @see BundleRegion#isBundleActivated(IProject)
	 * @see BundleRegion#isBundleActivated(Long)
	 * @see BundleRegion#isBundleActivated(Bundle)
	 */
	public Boolean isNatureEnabled(IProject project);

	/**
	 * Filters out all projects in the workspace registered with the JavaTime nature
	 * 
	 * @return a list of projects with the JavaTime nature or an empty collection
	 */
	public Collection<IProject> getNatureEnabled();

	/**
	 * Get all open and closed projects in workspace
	 * 
	 * @return a list of all projects in workspace
	 */
	public Collection<IProject> getProjects();

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param projectName name of a given project
	 * @return the java project or null if the given project is null
	 * @throws InPlaceException
	 * @throws InPlaceException if the project does not exist or is not open
	 */
	public IJavaProject getJavaProject(String projectName) throws InPlaceException;

	/**
	 * Returns the Java project corresponding to the given project.
	 * 
	 * @param project a given project
	 * @return the java project or null if the given project is null
	 * @throws InPlaceException if the project does not exist or is not open
	 */
	public IJavaProject getJavaProject(IProject project) throws InPlaceException;

	/**
	 * Formats the collection as a comma separated list.
	 * 
	 * @param projects collection of projects to format
	 * @return a comma separated list of projects or an empty string
	 */
	public String formatProjectList(Collection<IProject> projects);

	/**
	 * Find a project based on a location identifier.
	 * 
	 * @param location identifier string prepended with the file protocol (by value)
	 * @param locationScheme a valid URI location scheme
	 * @return the project or null if not found
	 * @see BundleRegion#getProjectLocationIdentifier(IProject, String)
	 * @see BundleRegion#BUNDLE_REF_LOC_SCHEME
	 * @see BundleRegion#BUNDLE_FILE_LOC_SCHEME
	 */
	public IProject getProjectFromLocation(String location, String locationScheme);

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
	 * Get a project based on it's name.
	 * 
	 * @param name a project name
	 * @return the project or null if the given project is null
	 */
	public IProject getProject(String name);

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