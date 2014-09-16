/************************E*******************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.bundleproject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.internal.runtime.DevClassPathHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class BundleProjectSettings {

	/**
	 * Verify that the default output folder is part of the bundle class path header in the manifest
	 * 
	 * @param project containing the class path
	 * @return true if the default output folder is contained in the class path for the specified project
	 */
	public static Boolean isOutputFolderInBundleClassPath(IProject project) throws InPlaceException {
		if (!BuildErrorClosure.hasManifest(project)) {
			throw new InPlaceException("no_manifest_found_project", project.getName());
		}

		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		IPath defaultOutpUtPath = bundleProjDesc.getDefaultOutputFolder();
		String bundleClassPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);

		if (null == bundleClassPath || null == defaultOutpUtPath) {
			return false;
		}
		if (Category.DEBUG && Category.getState(Category.binpath)) {
			TraceMessage.getInstance().getString("default_output_folder", project.getName(), defaultOutpUtPath);
		}
		return ManifestOptions.verifyPathInClassPath(defaultOutpUtPath, bundleClassPath, project.getName());
	}

	public static IPath getDefaultOutputLocation(IProject project) {
		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		return bundleProjDesc.getDefaultOutputFolder();
	}

	/**
	 * Adds default output location to the source folders of the specified projects
	 * 
	 * @param project containing one or more source folders
	 * @return true if one or more output locations was added, false if no output location was added, does not
	 *         exist or any exception was thrown
	 */
	public static Boolean createClassPathEntry(IProject project) {
		boolean outputLocationAdded = false;
		try {
			IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
			IPath defaultOutputPath = bundleProjDesc.getDefaultOutputFolder();
			if (null == defaultOutputPath) {
				return false;
			}
			Collection<IPath> srcPaths = getJavaProjectSourceFolders(project);
			ArrayList<IBundleClasspathEntry> entries = new ArrayList<IBundleClasspathEntry>();
			for (IPath srcPath : srcPaths) {
				IBundleProjectService service = InPlace.get().getBundleProjectService(project);
				IBundleClasspathEntry entry = service.newBundleClasspathEntry(srcPath, defaultOutputPath, null);
				if (!entry.getBinaryPath().equals(defaultOutputPath)) {
					entries.add(entry);
				}
			}
			if (entries.size() > 0) {
				bundleProjDesc.setBundleClasspath(entries.toArray(new IBundleClasspathEntry[entries.size()]));
				bundleProjDesc.apply(null);
				outputLocationAdded = true;
			}
		} catch (InPlaceException e) {
			outputLocationAdded = false;
		} catch (JavaModelException e) {
			outputLocationAdded = false;
		} catch (CoreException e) {
			outputLocationAdded = false;
		}
		return outputLocationAdded;
	}

	/**
	 * Finds all source class path entries and return the relative path of source folders
	 * 
	 * @param project with source folders
	 * @return source folders or an empty collection
	 * @throws JavaModelException when accessing the project resource or the class path element does not exist
	 * @throws InPlaceException if the project could not be accessed
	 */
	public static Collection<IPath> getJavaProjectSourceFolders(IProject project) throws JavaModelException,
			InPlaceException {
		ArrayList<IPath> paths = new ArrayList<IPath>();
		IJavaProject javaProject = BundleProjectState.getJavaProject(project);
		IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry entry = classpathEntries[i];
			if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
				IPath path = entry.getPath();
				String segment = path.segment(path.segmentCount() - 1);
				if (null != segment) {
					paths.add(new Path(segment));
				}
			}
		}
		return paths;
	}

	/**
	 * Set the default output folder, if it does not exists.
	 * 
	 * @param project to set the bundle class path header on
	 * @return true if the bundle class path is modified, otherwise false (already in path)
	 * @throws InPlaceException if failed to get bundle project description or if the manifest file is invalid
	 */
	public static Boolean addOutputLocationToBundleClassPath(IProject project) throws InPlaceException {
		try {
			if (!BuildErrorClosure.hasManifest(project)) {
				throw new InPlaceException("no_manifest_found_project", project.getName());
			}
			IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
			IPath defaultOutputPath = bundleProjDesc.getDefaultOutputFolder();
			if (null == defaultOutputPath) {
				return false;
			}
			String storedClassPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);
			// Class path header does not exist
			if (null == storedClassPath) {
				bundleProjDesc.setHeader(Constants.BUNDLE_CLASSPATH, defaultOutputPath.toString());
				bundleProjDesc.apply(null);
				BundleManager.addBundleTransition(new TransitionEvent(project, Transition.UPDATE_CLASSPATH));
				return true;
			}
			// Search for the output class path entry in the class path header
			if (storedClassPath.isEmpty()) {
				throw new InPlaceException("empty_classpath", project.getName());
			} else if (storedClassPath.equals(" ")) {
				throw new InPlaceException("space_classpath", project.getName());
			} else {
				// Identify output folder in class path
				ManifestElement[] elements = null;
				try {
					elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, storedClassPath);
				} catch (BundleException e) {
					throw new InPlaceException(e, "parsing_manifest", project.getName());
				}
				if (elements != null && elements.length > 0) {
					for (int i = 0; i < elements.length; i++) {
						IPath path = new Path(elements[i].getValue());
						if (path.equals(defaultOutputPath)) {
							return false;
						}
					}
				}
				// Append output folder to class path
				String updatedClassPath = storedClassPath.trim();
				if (updatedClassPath.length() > 0) {
					updatedClassPath = updatedClassPath.concat(",");
				}
				updatedClassPath = updatedClassPath.concat(defaultOutputPath.toString());
				bundleProjDesc.setHeader(Constants.BUNDLE_CLASSPATH, updatedClassPath);
				bundleProjDesc.apply(null);
				BundleManager.addBundleTransition(new TransitionEvent(project, Transition.UPDATE_CLASSPATH));
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
		return true;
	}

	/**
	 * Removes the default output folder, if it exists.
	 * 
	 * @param project to remove the bundle class path header from
	 * @return true if the bundle class path is removed, otherwise false (not in path)
	 * @throws InPlaceException if failed to get bundle project description, failed to read or parse manifest and when
	 *           the header is empty or contains space(s) only
	 */
	public static Boolean removeOutputLocationFromClassPath(IProject project) throws InPlaceException {

		// Output folder initially not removed
		boolean removed = false;

		try {
			if (!BuildErrorClosure.hasManifest(project)) {
				throw new InPlaceException("no_manifest_found_project", project.getName());
			}
			IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
			IPath defaultOutpUtPath = bundleProjDesc.getDefaultOutputFolder();
			if (null == defaultOutpUtPath) {
				return false;
			}
			String storedClassPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);
			if (null == storedClassPath) {
				return false;
			}
			// Search for the output class path entry in the class path header
			if (storedClassPath.isEmpty()) {
				throw new InPlaceException("empty_classpath", project.getName());
			} else if (storedClassPath.equals(" ")) {
				throw new InPlaceException("space_classpath", project.getName());
			} else {
				// Identify output in class path
				ManifestElement[] elements = null;
				try {
					elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, storedClassPath);
				} catch (BundleException e) {
					throw new InPlaceException(e, "parsing_manifest", project.getName());
				}
				StringBuffer updatedClassPath = new StringBuffer();
				if (elements != null && elements.length > 0) {
					for (int i = 0; i < elements.length; i++) {
						IPath path = new Path(elements[i].getValue());
						if (!path.equals(defaultOutpUtPath)) {
							updatedClassPath.append(path.toString());
							updatedClassPath.append(',');
						} else {
							removed = true;
						}
					}
				}
				String newClassPath = updatedClassPath.toString().trim();
				if (removed) {
					if (newClassPath.length() > 0) {
						// Remove trailing comma
						int lastIndex = newClassPath.length() - 1;
						if (newClassPath.charAt(lastIndex) == ',') {
							newClassPath = newClassPath.substring(0, lastIndex);
						}
						bundleProjDesc.setHeader(Constants.BUNDLE_CLASSPATH, newClassPath);
					} else {
						bundleProjDesc.setHeader(Constants.BUNDLE_CLASSPATH, null);
					}
					bundleProjDesc.apply(null);
					BundleManager.addBundleTransition(new TransitionEvent(project, Transition.REMOVE_CLASSPATH));
				}
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
		return removed;
	}

	/**
	 * Toggles between lazy and eager activation
	 * 
	 * @param project of bundle containing the activation policy
	 * @throws InPlaceException if failed to get bundle project description or saving activation policy to
	 *           manifest
	 */
	public static void toggleActivationPolicy(IProject project) throws InPlaceException {

		if (null == project) {
			return;
		}
		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		String policy = bundleProjDesc.getActivationPolicy();
		// Policy header does not exist
		if (null == policy) {
			bundleProjDesc.setActivationPolicy(Constants.ACTIVATION_LAZY);
		} else {
			bundleProjDesc.setActivationPolicy(null);
		}
		try {
			bundleProjDesc.apply(null);
			BundleManager.addBundleTransition(new TransitionEvent(project, Transition.UPDATE_ACTIVATION_POLICY));
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
	}

	/**
	 * Gets the activation policy header from the manifest file
	 * 
	 * @param project containing the meta information
	 * @return true if lazy activation and false if not.
	 * @throws InPlaceException if project is null or failed to obtain the project description
	 */
	public static Boolean getLazyActivationPolicyFromManifest(IProject project) throws InPlaceException {
		if (null == project) {
			throw new InPlaceException("project_null");
		}
		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		if (null == bundleProjDesc) {
			throw new InPlaceException("project_description_null");
		}
		String policy = bundleProjDesc.getActivationPolicy();
		if (null != policy && policy.equals(Constants.ACTIVATION_LAZY)) {
			return true;
		}
		return false;
	}

	/**
	 * Reads the current symbolic name from the manifest file (not the cache)
	 * 
	 * @param project containing the meta information
	 * @return current symbolic name in manifest file or null
	 * @throws InPlaceException if the project description could not be obtained
	 */
	public static String getSymbolicNameFromManifest(IProject project) throws InPlaceException {

		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		if (null == bundleProjDesc) {
			return null;
		}
		return bundleProjDesc.getSymbolicName();
	}

	/**
	 * Reads the current version from the manifest file (not the cache)
	 * 
	 * @param project containing the meta information
	 * @return current version from manifest file as a string or null
	 * @throws InPlaceException if the bundle project description could not be obtained
	 */
	public static String getBundleVersionFromManifest(IProject project) throws InPlaceException {
		if (null == project) {
			return null;
		}
		IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
		if (null == bundleProjDesc) {
			return null;
		}
		Version version = bundleProjDesc.getBundleVersion();
		if (null != version) {
			return version.toString();
		}
		return null;
	}

	/**
	 * Returns the project with the same symbolic name and version as the specified bundle
	 * 
	 * @param bundle of the corresponding project to find
	 * @return project with the same symbolic name an version as the specified bundle or null
	 */
	public static IProject getProject(Bundle bundle) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		for (IProject project : root.getProjects()) {
			try {
				IBundleProjectDescription bundleProjDesc = InPlace.get().getBundleDescription(project);
				String symbolicName = bundleProjDesc.getSymbolicName();
				if (null != symbolicName && symbolicName.equals(bundle.getSymbolicName())) {
					Version version = bundleProjDesc.getBundleVersion();
					if (null != version && version.equals(bundle.getVersion())) {
						return project;
					}
				}
			} catch (InPlaceException e) {
				StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, null, e),
						StatusManager.LOG);
			}
		}
		return null;
	}

	/**
	 * Return the dev parameter If dev mode is on
	 * 
	 * @return dev parameter or null if dev mode is off
	 */
	public static String inDevelopmentMode() {
		return InPlace.getContext().getProperty("osgi.dev");
	}

	/**
	 * Updates the {@code classPath} of the bundle with {@code symbolicName} as a framework property. There is a
	 * configuration option -dev that PDE uses to launch the framework. This option points to a dev.properties
	 * file. This file contains configuration data that tells the framework what additional class path entries
	 * to add to the bundles class path. This is a design choice of PDE that provides an approximation of the
	 * bundles content when run directly out of the workspace.
	 * 
	 * @param symbolicName of an installed bundle
	 * @param classPath a valid path (e.g. "bin")
	 * @throws InPlaceException if an IO or property error occurs updating build properties file
	 */
	public static boolean setDevClasspath(String symbolicName, String classPath) throws InPlaceException {

		String osgiDev = inDevelopmentMode();
		if (null == osgiDev || null == symbolicName || null == classPath) {
			return false;
			// throw new InPlaceException("classpath_property_error", symbolicName);
		}
		URL url;
		try {
			url = new URL(osgiDev);
		} catch (MalformedURLException e) {
			// Using common comma-separated class path entries (dev=<class path entries>) for all bundles
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				InPlace.get().trace(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, 
						NLS.bind(Msg.CLASS_PATH_COMMON_INFO, osgiDev, symbolicName)));
			}
			String[] devDefaultClasspath = DevClassPathHelper.getDevClassPath(symbolicName);
			boolean found = false;
			if (null != devDefaultClasspath) {
				IPath outputFolder = new Path(classPath);
				for (int i = 0; i < devDefaultClasspath.length; i++) {
					IPath path = new Path(devDefaultClasspath[i]);
					if (outputFolder.equals(path)) {
						found = true;
						break;
					}
				}
				if (!found) {
					String msg = WarnMessage.getInstance().formatString("bundle_class_path_mismatch", symbolicName,
							classPath, osgiDev);
					StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
							StatusManager.LOG);
				}
			}
			return found;
		}
		Properties props = new Properties();
		try {
			InputStream is = null;
			try {
				is = url.openStream();
				props.load(is);
			} finally {
				if (is != null)
					is.close();
			}
		} catch (IOException e) {
			throw new InPlaceException(e, "classpath_read_error", symbolicName);
		}
		props.setProperty(symbolicName, classPath);
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(url.getPath());
			props.store(os, null);
		} catch (FileNotFoundException e) {
			throw new InPlaceException(e, "classpath_file_not_found_error", symbolicName);
		} catch (IOException e) {
			throw new InPlaceException(e, "classpath_write_error_bundle", symbolicName);
		} finally {
			try {
				if (null != os) {
					os.close();
				}
			} catch (IOException e) {
				throw new InPlaceException(e, "io_exception_set_classpath", symbolicName);
			}
		}
		return true;
	}
}