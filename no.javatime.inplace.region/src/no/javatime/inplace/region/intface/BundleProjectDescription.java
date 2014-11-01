package no.javatime.inplace.region.intface;

import java.util.Collection;

import no.javatime.inplace.region.msg.Msg;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

public interface BundleProjectDescription {

	/**
	 * Path to manifest file relative to workspace root
	 */
	final public static String MANIFEST_RELATIVE_PATH = Msg.MANIFEST_FILE_RELATIVE_PATH_REF;
	/**
	 * Standard file name of the manifest file
	 */
	final public static String MANIFEST_FILE_NAME = Msg.MANIFEST_FILE_NAME_REF; // Message.getInstance().formatString("manifest_file_name");

	/**
	 * Verify that the default output folder is part of the bundle class path header in the manifest
	 * 
	 * @param project containing the class path
	 * @return true if the default output folder is contained in the class path for the specified
	 * project
	 * @throws InPlaceException if the manifest file is missing or the projects description could not
	 * be obtained
	 */
	public Boolean isDefaultOutputFolder(IProject project) throws InPlaceException;

	/**
	 * Returns a project relative path for the bundle's default output folder used on the
	 * Java build path, or null to indicate the default output location is used.
	 * 
	 * @param project The project with a default output folder
	 * @return Default project relative output folder path or null
	 */
	public IPath getDefaultOutputFolder(IProject project);

	public String getBundleClassPath(IProject project);

	/**
	 * Adds default output location to the source folders of the specified projects
	 * 
	 * @param project containing one or more source folders
	 * @return true if one or more output locations was added, false if no output location was added,
	 * does not exist or any exception was thrown
	 */
	public Boolean createClassPathEntry(IProject project);

	/**
	 * Finds all source class path entries and return the relative path of source folders
	 * 
	 * @param project with source folders
	 * @return source folders or an empty collection
	 * @throws JavaModelException when accessing the project resource or the class path element does
	 * not exist
	 * @throws InPlaceException if the project could not be accessed
	 */
	public Collection<IPath> getSourceFolders(IProject project) throws JavaModelException,
			InPlaceException;

	/**
	 * Set the default output folder to the bundle class path, if it does not exists.
	 * 
	 * @param project to set the bundle class path header on
	 * @return true if the bundle class path is modified, otherwise false (already in path)
	 * @throws InPlaceException if failed to get bundle project description or if the manifest file is
	 * invalid
	 */
	public Boolean addDefaultOutputFolder(IProject project) throws InPlaceException;

	/**
	 * Removes the default output folder entry from the bundle class path, if it exists.
	 * 
	 * @param project to remove the bundle class path entry from
	 * @return true if the bundle class path is removed, otherwise false (not in path)
	 * @throws InPlaceException if failed to get bundle project description, failed to read or parse
	 * manifest and when the header is empty or contains space(s) only
	 */
	public Boolean removeDefaultOutputFolder(IProject project) throws InPlaceException;

	/**
	 * Toggles between lazy and eager activation
	 * 
	 * @param project of bundle containing the activation policy
	 * @throws InPlaceException if failed to get bundle project description or saving activation
	 * policy to manifest
	 */
	public void toggleActivationPolicy(IProject project) throws InPlaceException;

	/**
	 * Gets the activation policy header from the manifest file
	 * 
	 * @param project containing the meta information
	 * @return true if lazy activation and false if eager activation.
	 * @throws InPlaceException if project is null or failed to obtain the project description
	 */
	public Boolean getActivationPolicy(IProject project) throws InPlaceException;

	/**
	 * Reads the current symbolic name from the manifest file (not the cache)
	 * 
	 * @param project containing the meta information
	 * @return current symbolic name in manifest file or null
	 * @throws InPlaceException if the project description could not be obtained
	 */
	public String getSymbolicName(IProject project) throws InPlaceException;

	/**
	 * Reads the current version from the manifest file (not the cache)
	 * 
	 * @param project containing the meta information
	 * @return current version from manifest file as a string or null
	 * @throws InPlaceException if the bundle project description could not be obtained
	 */
	public String getBundleVersion(IProject project) throws InPlaceException;

	/**
	 * Returns the project with the same symbolic name and version as the specified bundle
	 * 
	 * @param bundle of the corresponding project to find
	 * @return project with the same symbolic name an version as the specified bundle or null
	 */
	public IProject getProject(Bundle bundle);

	/**
	 * Return the dev parameter If dev mode is on
	 * 
	 * @return dev parameter or null if dev mode is off
	 */
	public String inDevelopmentMode();

	/**
	 * Updates the {@code classPath} of the bundle with {@code symbolicName} as a framework property.
	 * There is a configuration option -dev that PDE uses to launch the framework. This option points
	 * to a dev.properties file. This file contains configuration data that tells the framework what
	 * additional class path entries to add to the bundles class path. This is a design choice of PDE
	 * that provides an approximation of the bundles content when run directly out of the workspace.
	 * 
	 * @return true if the osgi.dev property file with the default output folder for the specified project was inserted. True otherwise
	 * @throws InPlaceException if "dev.mode" is off, an IO or property error occurs updating build properties file or if
	 * the symbolic name of the specified project is null or default output folder is missing 
	 */
	public Boolean setDevClasspath(IProject project) throws InPlaceException;
	/**
	 * Gets the cached activation policy header
	 * 
	 * @param bundle containing the meta information
	 * @return true if lazy activation and false if eager
	 */
	public Boolean getCachedActivationPolicy(Bundle bundle) throws InPlaceException;

	/**
	 * Checks if this bundle is a fragment
	 * 
	 * @param bundle bundle to check
	 * @return true if the bundle is a fragment. Otherwise false.
	 * @throws InPlaceException if bundle is null or a security violation
	 */
	public Boolean isFragment(Bundle bundle) throws InPlaceException;

	/**
	 * Verify that the specified path is part of the cached class path in the specified bundle
	 * 
	 * @param bundle containing class path
	 * @param path a single path that is checked for existence within the specified class path
	 * @return true if the specified path is contained in the class path of the specified bundle
	 * @exception InPlaceException if paring error or an i/o error occurs reading the manifest
	 */
	public Boolean verifyPathInCachedClassPath(Bundle bundle, IPath path) throws InPlaceException;

	/**
	 * Verify that the specified path is part of the specified class path.
	 * 
	 * @param path a single path that is checked for existence within the specified class path
	 * @param classPath containing class path
	 * @return true if the specified path is contained in the class path of the specified class path string
	 * @exception InPlaceException if parsing error or an i/o error occurs reading the manifest
	 */
	public Boolean verifyPathInCachedClassPath(IPath path, String classPath, String name)
			throws InPlaceException;
}