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
package no.javatime.inplace.region.project;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleProjectDescription;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.msg.Msg;
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
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class BundleProjectDescriptionImpl extends CachedManifestOperationsImpl implements BundleProjectDescription {

	public final static BundleProjectDescriptionImpl INSTANCE = new BundleProjectDescriptionImpl();

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#isDefaultOutputFolder(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean isDefaultOutputFolder(IProject project) throws InPlaceException {
		if (!BuildErrorClosure.hasManifest(project)) {
			throw new InPlaceException("no_manifest_found_project", project.getName());
		}

		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		IPath defaultOutpUtPath = bundleProjDesc.getDefaultOutputFolder();
		String bundleClassPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);

		if (null == bundleClassPath || null == defaultOutpUtPath) {
			return false;
		}
		if (Category.DEBUG && Category.getState(Category.binpath)) {
			TraceMessage.getInstance().getString("default_output_folder", project.getName(),
					defaultOutpUtPath);
		}
		return BundleProjectDescriptionImpl.INSTANCE.verifyPathInCachedClassPath(defaultOutpUtPath, bundleClassPath,
				project.getName());
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getDefaultOutputFolder(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IPath getDefaultOutputFolder(IProject project) {
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		return bundleProjDesc.getDefaultOutputFolder();
	}
	
	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getBundleClassPath(org.eclipse.core.resources.IProject)
	 */
	@Override
	public String getBundleClassPath(IProject project) {
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		return bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);
	}
	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#createClassPathEntry(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean createClassPathEntry(IProject project) {
		boolean outputLocationAdded = false;
		try {
			IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
			IPath defaultOutputPath = bundleProjDesc.getDefaultOutputFolder();
			if (null == defaultOutputPath) {
				return false;
			}
			Collection<IPath> srcPaths = getSourceFolders(project);
			ArrayList<IBundleClasspathEntry> entries = new ArrayList<IBundleClasspathEntry>();
			for (IPath srcPath : srcPaths) {
				IBundleProjectService service = Activator.getBundleProjectService(project);
				IBundleClasspathEntry entry = service.newBundleClasspathEntry(srcPath, defaultOutputPath,
						null);
				if (!entry.getBinaryPath().equals(defaultOutputPath)) {
					entries.add(entry);
				}
			}
			if (entries.size() > 0) {
				bundleProjDesc
						.setBundleClasspath(entries.toArray(new IBundleClasspathEntry[entries.size()]));
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

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getSourceFolders(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Collection<IPath> getSourceFolders(IProject project)
			throws JavaModelException, InPlaceException {
		ArrayList<IPath> paths = new ArrayList<IPath>();
		IJavaProject javaProject = BundleProjectImpl.INSTANCE.getJavaProject(project);
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

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#addDefaultOutputFolder(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean addDefaultOutputFolder(IProject project) throws InPlaceException {
		try {
			if (!BuildErrorClosure.hasManifest(project)) {
				throw new InPlaceException("no_manifest_found_project", project.getName());
			}
			IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
			IPath defaultOutputPath = bundleProjDesc.getDefaultOutputFolder();
			if (null == defaultOutputPath) {
				return false;
			}
			String storedClassPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);
			// Class path header does not exist
			if (null == storedClassPath) {
				bundleProjDesc.setHeader(Constants.BUNDLE_CLASSPATH, defaultOutputPath.toString());
				bundleProjDesc.apply(null);
				BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
						Transition.UPDATE_CLASSPATH));
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
				BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
						Transition.UPDATE_CLASSPATH));
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#removeDefaultOutputFolder(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean removeDefaultOutputFolder(IProject project) throws InPlaceException {

		// Output folder initially not removed
		boolean removed = false;

		try {
			if (!BuildErrorClosure.hasManifest(project)) {
				throw new InPlaceException("no_manifest_found_project", project.getName());
			}
			IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
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
					BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
							Transition.REMOVE_CLASSPATH));
				}
			}
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
		return removed;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#toggleActivationPolicy(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void toggleActivationPolicy(IProject project) throws InPlaceException {

		if (null == project) {
			return;
		}
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		String policy = bundleProjDesc.getActivationPolicy();
		// Policy header does not exist
		if (null == policy) {
			bundleProjDesc.setActivationPolicy(Constants.ACTIVATION_LAZY);
		} else {
			bundleProjDesc.setActivationPolicy(null);
		}
		try {
			bundleProjDesc.apply(null);
			BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
					Transition.UPDATE_ACTIVATION_POLICY));
		} catch (CoreException e) {
			throw new InPlaceException(e, "manifest_io_project", project.getName());
		}
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getActivationPolicy(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean getActivationPolicy(IProject project)
			throws InPlaceException {
		if (null == project) {
			throw new InPlaceException("project_null");
		}
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		if (null == bundleProjDesc) {
			throw new InPlaceException("project_description_null");
		}
		String policy = bundleProjDesc.getActivationPolicy();
		if (null != policy && policy.equals(Constants.ACTIVATION_LAZY)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getSymbolicName(org.eclipse.core.resources.IProject)
	 */
	@Override
	public String getSymbolicName(IProject project) throws InPlaceException {

		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		return bundleProjDesc.getSymbolicName();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getBundleVersion(org.eclipse.core.resources.IProject)
	 */
	@Override
	public String getBundleVersion(IProject project) throws InPlaceException {
		if (null == project) {
			return null;
		}
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		if (null == bundleProjDesc) {
			return null;
		}
		Version version = bundleProjDesc.getBundleVersion();
		if (null != version) {
			return version.toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#getProject(org.osgi.framework.Bundle)
	 */
	@Override
	public IProject getProject(Bundle bundle) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		for (IProject project : root.getProjects()) {
			try {
				IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
				String symbolicName = bundleProjDesc.getSymbolicName();
				if (null != symbolicName && symbolicName.equals(bundle.getSymbolicName())) {
					Version version = bundleProjDesc.getBundleVersion();
					if (null != version && version.equals(bundle.getVersion())) {
						return project;
					}
				}
			} catch (InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, null, e), StatusManager.LOG);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#inDevelopmentMode()
	 */
	@Override
	public String inDevelopmentMode() {
		return Activator.getContext().getProperty("osgi.dev");
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectDescription#setDevClasspath(org.eclipse.core.resources.IProject)
	 */
	@Override
	public Boolean setDevClasspath(IProject project)
			throws InPlaceException {

		String osgiDev = inDevelopmentMode();
		String symbolicName = getSymbolicName(project);
		if (null == symbolicName) {
			throw new InPlaceException(Msg.SYMBOLIC_NAME_ERROR, symbolicName);			
		}
		if (null == osgiDev) {
			throw new InPlaceException("classpath_property_error", symbolicName);
		}
		IPath classPath = getDefaultOutputFolder(project);
		if (null == classPath) {
			throw new InPlaceException("default_output_folder_error", symbolicName);			
		}
		String defOutputFolder = classPath.toString();
		URL url;
		try {
			url = new URL(osgiDev);
		} catch (MalformedURLException e) {
			// Using common comma-separated class path entries (dev=<class path entries>) for all bundles
			if (Activator.getDefault().getMsgOptService().isBundleOperations()) {				
				BundleTransitionListener.addBundleTransition(new TransitionEvent(project, Transition.UPDATE_DEV_CLASSPATH));
			}
			String[] devDefaultClasspath = DevClassPathHelper.getDevClassPath(symbolicName);
			boolean found = false;
			if (null != devDefaultClasspath) {
				IPath outputFolder = new Path(defOutputFolder);
				for (int i = 0; i < devDefaultClasspath.length; i++) {
					IPath path = new Path(devDefaultClasspath[i]);
					if (outputFolder.equals(path)) {
						found = true;
						break;
					}
				}
				if (!found) {
					String msg = WarnMessage.getInstance().formatString("bundle_class_path_mismatch",
							symbolicName, defOutputFolder, osgiDev);
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
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
		props.setProperty(symbolicName, defOutputFolder);
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