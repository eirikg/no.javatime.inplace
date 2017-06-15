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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

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
 * After a bundle is installed header information in the manifest file and the cached manifest
 * accessed through the bundle may be different. To synchronize the manifest headers an update
 * followed by a refresh is not enough. A re-installation of the bundle is necessary.
 * 
 * @see BundleProjectCandidates
 */
public class CachedManifestOperationsImpl {

	/**
	 * Get the header value of the specified header for the specified project
	 * 
	 * @param project project with the specified header
	 * @param header header with a set of values
	 * @return the value in the specified header or <code>null</code>
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 */
	public String getHeaderValues(IProject project, String header) throws InPlaceException {

		String key = null;
		if (null != project && project.isAccessible()) {
			IFile manifestFile = getManifestFile(project);
			if (manifestFile.exists()) {
				try {
					Map<String, String> headers = ManifestElement
							.parseBundleManifest(manifestFile.getContents(), null);
					key = (String) headers.get(Constants.BUNDLE_CLASSPATH);
				} catch (CoreException | IOException | BundleException e) {
					throw new InPlaceException(
							new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
				}
			}
		}
		return key;
	}

	/**
	 * Get the first header value of the specified header for the specified project
	 * 
	 * @param project project with the specified header
	 * @param header header with a set of values
	 * @return the first value in the specified header or <code>null</code>
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 */
	public String getFirstHeaderValue(IProject project, String header) throws InPlaceException {

		String key = null;
		if (null != project && project.isAccessible()) {
			IFile manifestFile = getManifestFile(project);
			if (manifestFile.exists()) {
				try {
					Map<String, String> headers = ManifestElement
							.parseBundleManifest(manifestFile.getContents(), null);
					ManifestElement[] elements = parseHeader(headers, header);
					if (elements != null) {
						if (elements.length > 0) {
							key = elements[0].getValue();
						}
					}
				} catch (CoreException | IOException | BundleException e) {
					throw new InPlaceException(
							new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
				}
			}
		}
		return key;
	}

	/**
	 * Parses the specified header.
	 * 
	 * @param headers all manifest headers
	 * @param key the key of the header values to return
	 * @return elements or <code>null</code> if none
	 * @throws InPlaceException if the header value is invalid
	 */
	protected ManifestElement[] parseHeader(Map<String, String> headers, String key)
			throws InPlaceException {

		String value = headers.get(key);
		if (value != null) {
			if (value.trim().length() > 0) {
				try {
					return ManifestElement.parseHeader(key, value);
				} catch (BundleException e) {
					throw new InPlaceException(
							new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
				}
			}
		}
		return null;
	}

	/**
	 * Get the manifest file of the specified project
	 * 
	 * @param project containing the manifest file
	 * @return the handle of the manifest file or null if project is null
	 */
	public IFile getManifestFile(IProject project) {
		if (null != project) {
			IFile manifestFile = project.getFile(
					BundleProjectMeta.MANIFEST_RELATIVE_PATH + BundleProjectMetaImpl.MANIFEST_FILE_NAME);
			return manifestFile;
		}
		return null;
	}

	@SuppressWarnings("unused")
	private void setActivationPolicy(Collection<IProject> projects, Boolean eagerActivation) {
		if (null == projects) {
			return;
		}
		for (IProject project : projects) {
			IFile manifestFile = getManifestFile(project);
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
	@SuppressWarnings("unused")
	private void setActivationPolicy(IProject project) {
		if (null == project) {
			return;
		}
		IFile manifestFile = getManifestFile(project);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * no.javatime.inplace.region.project.CachedmanifestOperations#getCachedActivationPolicy(org.osgi.
	 * framework.Bundle)
	 */
	public Boolean getCachedActivationPolicy(Bundle bundle) throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException(
					ExceptionMessage.getInstance().getString("null_bundle_activation_policy"));
		}
		Dictionary<String, String> headers = bundle.getHeaders();
		String policy = headers.get(Constants.BUNDLE_ACTIVATIONPOLICY);
		if (null != policy && policy.equals(Constants.ACTIVATION_LAZY)) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.region.project.CachedmanifestOperations#isFragment(org.osgi.framework.
	 * Bundle)
	 */
	public Boolean isCachedFragment(Bundle bundle) throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException(
					ExceptionMessage.getInstance().getString("null_bundle_check_fragment"));
		}
		try {
			Dictionary<String, String> headers = bundle.getHeaders();
			// Key is always not null
			String fragment = headers.get(Constants.FRAGMENT_HOST);
			if (null != fragment) {
				return true;
			}
		} catch (SecurityException e) {
			throw new InPlaceException(e, "bundle_security_error", bundle);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * no.javatime.inplace.region.project.CachedmanifestOperations#verifyPathInCachedClassPath(org.
	 * osgi.framework.Bundle, org.eclipse.core.runtime.IPath)
	 */
	public Boolean verifyPathInCachedClassPath(Bundle bundle, IPath path) throws InPlaceException {
		if (null == bundle) {
			return false;
		}
		Dictionary<String, String> headers = bundle.getHeaders();
		String classPath = headers.get(Constants.BUNDLE_CLASSPATH);
		return verifyPathInCachedClassPath(path, classPath, bundle.getSymbolicName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * no.javatime.inplace.region.project.CachedmanifestOperations#verifyPathInCachedClassPath(org.
	 * eclipse.core.runtime.IPath, java.lang.String, java.lang.String)
	 */
	public Boolean verifyPathInCachedClassPath(IPath path, String classPath, String name)
			throws InPlaceException {
		try {

			// Class path header does not exist
			if (null == classPath || null == path) {
				return false;
			}
			// TODO Use trace from new plug-in
			// if (Category.DEBUG && Category.getState(Category.binpath))
			// InPlace.get().trace("classpath_header", name, classPath);
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
	 * @exception InPlaceException if {@code Bundle#getEntry(String)} return null or an i/o error
	 * occurs reading the manifest
	 */
	@SuppressWarnings("unused")
	private Manifest loadManifest(Bundle bundle) throws InPlaceException {

		URL url = null;
		try {
			url = bundle.getEntry(BundleProjectMetaImpl.MANIFEST_FILE_NAME);
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
	@SuppressWarnings("unused")
	private Manifest loadManifest(IResource resource) {
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
	private Manifest loadManifest(IProject project, IFile file) {
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
	 * @throws InPlaceException if the specified project is null, the location identifier for the
	 * specified project could not be found, manifest file not found, write error or any other IO
	 * error updating the manifest file.
	 */
	private void saveManifest(IProject project, Manifest manifest)
			throws InPlaceException, ProjectLocationException {
		String location = null;
		location = WorkspaceRegionImpl.INSTANCE.getProjectLocationIdentifier(project,
				BundleRegion.BUNDLE_FILE_LOC_SCHEME);
		URL urlLoc;
		try {
			FileOutputStream os = null;
			try {
				urlLoc = new URL(location);
				String path = urlLoc.getPath();
				path = path.concat(BundleProjectMetaImpl.MANIFEST_FILE_NAME);
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

	/**
	 * Check for existence of a manifest file at the default location in the specified project
	 * 
	 * @param project to check for the existence of a manifest file at the default location
	 * @return true if the manifest file exist at the default location and false otherwise
	 * @see BundleProjectMeta#MANIFEST_RELATIVE_PATH
	 * @see BundleProjectMeta#MANIFEST_FILE_NAME
	 */
	public boolean hasManifest(IProject project) {
		if (null != project && project.isAccessible()) {
			IFile manifestFile = project.getFile(
					BundleProjectMeta.MANIFEST_RELATIVE_PATH + BundleProjectMetaImpl.MANIFEST_FILE_NAME);
			if (manifestFile.exists()) {
				return true;
			}
		}
		return false;
	}
}
