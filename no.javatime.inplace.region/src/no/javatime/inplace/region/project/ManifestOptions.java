/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.region.project;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.msg.Msg;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * After a bundle is installed header information in the manifest file and the cached manifest accessed
 * through the bundle may be different. To synchronize the manifest headers an update followed by a refresh is
 * not enough. A re-installation of the bundle is necessary.
 * 
 * @see BundleProject
 */
public class ManifestOptions {

	/**
	 * Path to manifest file relative to workspace root
	 */
	final public static String MANIFEST_RELATIVE_PATH = Msg.MANIFEST_FILE_RELATIVE_PATH_REF; 

	/**
	 * Standard file name of the manifest file
	 */
	final public static String MANIFEST_FILE_NAME =  Msg.MANIFEST_FILE_NAME_REF; // Message.getInstance().formatString("manifest_file_name");

	private static void setActivationPolicy(Collection<IProject> projects, Boolean eagerActivation) {
		if (null == projects) {
			return;
		}
		for (IProject project : projects) {
			IFile manifestFile = project.getFile(MANIFEST_FILE_NAME);
			Manifest manifest = loadManifest(project, manifestFile);
			Attributes attributes = manifest.getMainAttributes();
			String lazyPolicy = attributes.getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
			try {
				if (eagerActivation) {
					if (null != lazyPolicy) {
						attributes.remove(new Attributes.Name(Constants.BUNDLE_ACTIVATIONPOLICY));
						saveManifest(project, manifest);
					}
				} else {
					if (null == lazyPolicy) {
						attributes.putValue(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
						saveManifest(project, manifest);
					}
				}
			} catch (ProjectLocationException e) {
				throw new InPlaceException(e, "parsing_manifest", project.getName());				
			} catch (Exception e) {
				throw new InPlaceException(e, "parsing_manifest", project.getName());
			}
		}
	}

	/**
	 * Toggles lazy activation
	 * 
	 * @param project of bundle containing the activation policy
	 */
	private static void setActivationPolicy(IProject project) {
		if (null == project) {
			return;
		}
		IFile manifestFile = project.getFile(MANIFEST_FILE_NAME);
		Manifest manifest = loadManifest(project, manifestFile);
		Attributes attributes = manifest.getMainAttributes();
		String policy = attributes.getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
		try {
			if (null == policy) {
				attributes.putValue(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
			} else {
				attributes.remove(new Attributes.Name(Constants.BUNDLE_ACTIVATIONPOLICY));
			}
			saveManifest(project, manifest);
		} catch (ProjectLocationException e) {
			throw new InPlaceException(e, "parsing_manifest", project.getName());				
		} catch (Exception e) {
			throw new InPlaceException(e, "parsing_manifest", project.getName());
		}
	}

	/**
	 * Gets the cached activation policy header
	 * 
	 * @param bundle containing the meta information
	 * @return true if lazy activation or false if not
	 */
	public static Boolean getlazyActivationPolicy(Bundle bundle) throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException("null_bundle_activation_policy");
		}
		Dictionary<String, String> headers = bundle.getHeaders();
		String policy = headers.get(Constants.BUNDLE_ACTIVATIONPOLICY);
		if (null != policy && policy.equals(Constants.ACTIVATION_LAZY)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if this bundle is a fragment
	 * 
	 * @param bundle bundle to check
	 * @return true if the bundle is a fragment. Otherwise false
	 * @throws InPlaceException
	 */
	public static Boolean isFragment(Bundle bundle) throws InPlaceException {
		Dictionary<String, String> headers = bundle.getHeaders();
		// Key is always not null
		String fragment = headers.get(Constants.FRAGMENT_HOST);
		if (null != fragment) {
			return true;
		}
		return false;
	}

	/**
	 * Verify that the specified path is part of the cached class path in the specified bundle
	 * 
	 * @param bundle containing class path
	 * @param path a single path that is checked for existence within the specified class path
	 * @return true if the specified path is contained in the class path of the specified bundle
	 * @exception InPlaceException if paring error or an i/o error occurs reading the manifest
	 */
	public static Boolean verifyPathInClassPath(Bundle bundle, IPath path) throws InPlaceException {
		if (null == bundle) {
			return false;
		}
		Dictionary<String, String> headers = bundle.getHeaders();
		String classPath = headers.get(Constants.BUNDLE_CLASSPATH);
		return verifyPathInClassPath(path, classPath, bundle.getSymbolicName());
	}

	/**
	 * Verify that the specified path is part of the specified class path.
	 * 
	 * @param path a single path that is checked for existence within the specified class path
	 * @param classPath containing class path
	 * @return true if the specified path is contained in the class path of the specified class path string
	 * @exception InPlaceException if parsing error or an i/o error occurs reading the manifest
	 */
	public static Boolean verifyPathInClassPath(IPath path, String classPath, String name) throws InPlaceException {
		try {

			// Class path header does not exist
			if (null == classPath || null == path) {
				return false;
			}
// TODO Use trace from new plug-in
//			if (Category.DEBUG && Category.getState(Category.binpath))
//				InPlace.get().trace("classpath_header", name, classPath);
			// Search for the bin class path entry in the class path header
			if (classPath.isEmpty()) {
				return false;
			} else if (classPath.equals(" ")) {
				return false;
			} else {
				// Identify path in class path
				ManifestElement[] elements = null;
				try {
					elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classPath);
				} catch (BundleException e) {
					throw new InPlaceException(e, "parsing_manifest", name);
				}
				if (null != elements && elements.length > 0) {
					for (int i = 0; i < elements.length; i++) {
						IPath pathElement = new Path(elements[i].getValue());
						if (pathElement.equals(path)) {
							return true;
						}
					}
				}
			}
		} catch (InPlaceException e) {
			throw new InPlaceException(e, "manifest_io_project", name);
		}
		return false;
	}

	/**
	 * Reads the manifest from the cached plugin.xml. Requires that the bundle is installed.
	 * 
	 * @param bundle to which the manifest is associated with
	 * @return the manifest file
	 * @exception InPlaceException if {@code Bundle#getEntry(String)} return null or an i/o error occurs reading
	 *              the manifest
	 */
	public static Manifest loadManifest(Bundle bundle) throws InPlaceException {

		URL url = null;
		try {
			url = bundle.getEntry(MANIFEST_FILE_NAME);
		} catch (IllegalStateException e) {
			throw new InPlaceException(e, "bundle_state_error", bundle.getSymbolicName());
		} catch (NullPointerException e) {
			throw new InPlaceException(e, "no_manifest_found", bundle.getSymbolicName());
		}
		try {
			InputStream is = null;
			try {
				is = url.openStream();
				return new Manifest(is);
			} catch (NullPointerException e) {
				throw new InPlaceException(e, "no_manifest_found", bundle.getSymbolicName());
			} finally {
				if (null != is)
					is.close();
			}
		} catch (IOException e) {
			throw new InPlaceException(e, "manifest_io", bundle.getSymbolicName());
		}
	}

	/**
	 * Reads the manifest file. This method, using {@code IResource}, is independent of OSGI.
	 * 
	 * @param resource the manifest resource
	 * @return the content of the manifest file
	 */
	public static Manifest loadManifest(IResource resource) {
		Manifest manifest = null;
		if (resource instanceof IFile) {
			manifest = loadManifest(resource.getProject(), (IFile) resource);
		}
		return manifest;
	}

	/**
	 * Reads the manifest file. This method, using {@code IResource}, is independent of OSGI.
	 * 
	 * @param project containing the manifest file
	 * @param file the manifest file
	 * @return the content of the manifest file
	 */
	public static Manifest loadManifest(IProject project, IFile file) {
		try {
			InputStream is = null;
			try {
				is = file.getContents();
				return new Manifest(is);
			} finally {
				if (null != is)
					is.close();
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "no_manifest_found_project", project.getName());
		} catch (IOException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
	}

	/**
	 * Store the content of {@code Manifest} to the manifest file
	 * 
	 * @param project containing the manifest file
	 * @param manifest file
	 * @throws InPlaceException if the specified project is null, the location identifier for the specified
	 *           project could not be found, manifest file not found, write error or any other IO error updating
	 *           the manifest file.
	 */
	private static void saveManifest(IProject project, Manifest manifest) throws InPlaceException, ProjectLocationException {
		String location = null;
		location = BundleProjectState.getLocationIdentifier(project, BundleProjectState.BUNDLE_FILE_LOC_SCHEME);
		URL urlLoc;
		try {
			FileOutputStream os = null;
			try {
				urlLoc = new URL(location);
				String path = urlLoc.getPath();
				path = path.concat(MANIFEST_FILE_NAME);
				os = new FileOutputStream(path);
				manifest.write(os);
			} finally {
				if (null != os)
					os.close();
			}
			project.refreshLocal(IProject.DEPTH_INFINITE, null);
		} catch (FileNotFoundException e) {
			throw new InPlaceException(e, "classpath_file_not_found_error", project.getName());
		} catch (IOException e) {
			throw new InPlaceException(e, "classpath_write_error", project.getName());
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
	}	
}
