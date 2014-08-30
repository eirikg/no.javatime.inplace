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
package no.javatime.inplace.ui.views;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * A bundle or project model object. Provides labels and attributes for bundles and projects. Typically used
 * as model in MVC and in property sheets.
 */
public class BundleProperties {

	public static String bundleLabelName = Message.getInstance().formatString("bundle_label_name");
	public static String bundleSymbolicNameLabelName = Message.getInstance().formatString(
			"bundle_symbolic_label_name");
	public static String bundleVersionLabelName = Message.getInstance().formatString(
			"bundle_version_label_name");
	public static String projectLabelName = Message.getInstance().formatString("project_label_name");
	public static String bundleIdLabelName = Message.getInstance().formatString("bundle_id_label_name");
	public static String activationStatusLabelName = Message.getInstance().formatString(
			"activation_mode_label_name");
	public static String bundleStateLabelName = Message.getInstance().formatString("bundle_state_label_name");
	public static String servicesInUseLabelName = Message.getInstance().formatString(
			"services_in_use_label_name");
	public static String numberOfRevisionsLabelName = Message.getInstance().formatString(
			"number_of_revisions_label_name");
	public static String activationPolicyLabelName = Message.getInstance().formatString(
			"activation_policy_label_name");
	public static String bundleStatusLabelName = Message.getInstance().formatString("bundle_status_label_name");
	public static String locationLabelName = Message.getInstance().formatString("location_label_name");
	public static String lastInstalledOrUpdatedLabelName = Message.getInstance().formatString(
			"last_installed_or_updated_label_name");
	public static String lastModifiedLabelName = Message.getInstance().formatString("last_modified_label_name");
	public static String referencedProjectsLabelName = Message.getInstance().formatString(
			"referenced_all_project_label_name");
	public static String referencingProjectsLabelName = Message.getInstance().formatString(
			"referencing_all_project_label_name");
	public static String requiringAllBundlesLabelName = Message.getInstance().formatString(
			"requiring_all_bundle_label_name");
	public static String providingAllBundlesLabelName = Message.getInstance().formatString(
			"providing_all_bundle_label_name");
	public static String requiringResolvedBundlesLabelName = Message.getInstance().formatString(
			"requiring_resolved_bundle_label_name");
	public static String providingResolvedBundlesLabelName = Message.getInstance().formatString(
			"providing_resolved_bundle_label_name");
	public static String UIExtensionsLabelName = Message.getInstance().formatString("ui_extensions_label_name");
	public static String lastCmdLabelName = Message.getInstance().formatString("last_cmd_label_name");

	private static String lazyyValueName = "Lazy";
	private static String eagerValueName = "Eager";

	private String name = null;
	private String value = null;
	private IProject project = null;
	private IJavaProject javaProject = null;
	private Bundle bundle = null;
	static private BundleSorter bundleSorter = new BundleSorter();
	static private ProjectSorter projectSorter = new ProjectSorter();

	public BundleProperties(IProject project) {
		this.project = project;
		try {
			if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
		}
		bundle = BundleJobManager.getRegion().get(project);
	}

	public BundleProperties(IJavaProject javaProject) {
		this.project = javaProject.getProject();
		this.javaProject = javaProject;
		bundle = BundleJobManager.getRegion().get(project);
	}

	public BundleProperties(IProject project, String name, String value) {
		this.project = project;
		try {
			if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
		}
		bundle = BundleJobManager.getRegion().get(project);
		this.name = name;
		this.value = value;

	}

	public BundleProperties(IJavaProject javaProject, String name, String value) {
		this.project = javaProject.getProject();
		this.javaProject = javaProject;
		bundle = BundleJobManager.getRegion().get(project);
		this.name = name;
		this.value = value;

	}

	public String getBundleLabelName() {
		String bundleProjectLabelName = bundleLabelName;
		String symbolicName = null;
		String version = null;
		if (null == bundle) { // Uninstalled
			try {
				symbolicName = BundleProjectSettings.getSymbolicNameFromManifest(project);
				version = BundleProjectSettings.getBundleVersionFromManifest(project);
			} catch (InPlaceException e) {
			}
			if (null == symbolicName || null == version) {
				bundleProjectLabelName = BundleProperties.projectLabelName;
			}
		}
		return bundleProjectLabelName;
	}

	/**
	 * @return the symbolic name and version
	 */
	public final String getBundleName() {
		String bundleKey = null;
		bundleKey = BundleJobManager.getRegion().getSymbolicKey(null, project);
		if (bundleKey.isEmpty()) {
			return "?_?";
		}
		return bundleKey;
	}

	/**
	 * Get the cached symbolic name if available, otherwise get it from the manifest
	 * 
	 * @return the symbolic name or the project name appended wit "(P)" if not found
	 */
	public final String getSymbolicName() {
		String name = null;
		if (null != bundle) {
			name = bundle.getSymbolicName();
		}
		if (null == name) {
			try {
				name = BundleProjectSettings.getSymbolicNameFromManifest(project);
			} catch (Exception e) {
				return project.getName() + " (P)";
			}
			if (null == name) {
				return project.getName() + " (P)";
			}
		}		
		return name;
	}

	/**
	 * Get the cached version if available, otherwise get it from the manifest
	 * 
	 * @return the version or the project or "?" if not found
	 */
	public final String getBundleVersion() {
		String ver = null;
		if (null != bundle) {
			ver = bundle.getVersion().toString();
		}
		if (null == ver) {
			try {
				ver = BundleProjectSettings.getBundleVersionFromManifest(project);
			} catch (Exception e) {
				return "?";
			}
			if (null == ver) {
				return "?";
			}
		}
		return ver;
	}

	public String getProjectName() {
		return project.getName();
	}

	public final String getBundleId() {
		String bundleId = null;
		if (null == bundle) {
			bundleId = "";
		} else {
			bundleId = Long.toString(bundle.getBundleId());
		}
		setName(bundleId);
		return bundleId;
	}

	public String getActivationMode() {
		boolean activated = BundleProjectState.isNatureEnabled(project);
		if (activated) {
			return "Activated";
		} else {
			return "Deactivated";
		}
	}

	public String getBundleStatus() {

		boolean isProjectActivated = BundleProjectState.isNatureEnabled(project);
		BundleCommand bundleCommand = BundleJobManager.getCommand();
		BundleTransition bundleTransition = BundleJobManager.getTransition();

		try {
			if (!BuildErrorClosure.hasBuildState(project)) {
				return "Missing Build State";
			} else if (BuildErrorClosure.hasBuildErrors(project)) {
				return "Build Problems";
			} else if (bundleTransition.containsPending(project, Transition.BUILD, false)) {
				return "Build Pending";
			} else if (isProjectActivated && bundleTransition.containsPending(project, Transition.UPDATE, false)) {
				return "Update Pending";
			} else if (bundleTransition.hasTransitionError(project)) {
				TransitionError error = bundleTransition.getError(project);
				if (error == TransitionError.DUPLICATE) {
					return "Duplicate";
				} else if (error == TransitionError.CYCLE) {
					return "Cycle";
				} else if (error == TransitionError.DEPENDENCY) {
					return "Dependent Bundle";
				} else if (error == TransitionError.INCOMPLETE) {
					return "Incomplete";
				} else if (error == TransitionError.BUILD) {
					return "Build Problems";
				} else if (error == TransitionError.EXCEPTION) {
					return "Exception";
				} else if (error == TransitionError.STATECHANGE) {
					return "State error";
				} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.UNINSTALLED)) != 0) {
					return "Install Problems"; 
				} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.INSTALLED)) != 0) {
					return "Resolve Problems";
				} else {
					return "Bundle Problems";
				}
			} else if (null != bundle && bundleCommand.getBundleRevisions(bundle).size() > 1) {
				return "Refresh Pending" + " (" + getBundleRevisions() + ")";
			} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.RESOLVED)) != 0
					&& !BundleSorter.isFragment(bundle)) {
				return "Start Pending";
			} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.STARTING)) != 0) {
				return "Lazy Loading";
			} else {
				if (isProjectActivated) {
					if ((bundleCommand.getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
						return "Activated";
					} else {
						return "Running";
					}
				} else {
					return "Deactivated"; // Activate Pending
				}
			}
		} catch (ProjectLocationException e) {
			return "Project Location Problem";
		}
	}

	public String getLastTransition() {
		try {
			return BundleJobManager.getTransition().getTransitionName(project);
		} catch (ProjectLocationException e) {
		}
		return BundleJobManager.getTransition().getTransitionName(Transition.NOTRANSITION, false, false);
	}

	/**
	 * @return the bundleState
	 */
	public String getBundleState() {
		return BundleJobManager.getCommand().getStateName(bundle);
	}

	public String getServicesInUse() {
		Bundle bundle = BundleJobManager.getRegion().get(project);
		StringBuffer buf = new StringBuffer();
		if (null != bundle) {
			ServiceReference<?>[] sr = bundle.getServicesInUse();
			if (null != sr) {
				for (int i = 0; i < sr.length; i++) {
					Bundle serviceBundle = sr[i].getBundle();
					String serviceBundleName = serviceBundle.getSymbolicName() + " ["
							+ Long.toString(serviceBundle.getBundleId()) + "]";
					if (buf.indexOf(serviceBundleName) == -1) {
						buf.append(serviceBundleName);
						buf.append(", ");
					}
				}
				buf.deleteCharAt(buf.lastIndexOf(","));
			}
		}
		return buf.toString();
	}

	/**
	 * Calculates number of revision for a bundle
	 * 
	 * @return Number of revisions for the bundle
	 */
	public String getBundleRevisions() {
		int noOfRevisions = 0;
		if (null == bundle) {
			return String.valueOf(noOfRevisions);
		} else {
			try {
				noOfRevisions = BundleJobManager.getCommand().getBundleRevisions(bundle).size();
			} catch (InPlaceException e) {
				// Ignore and return zero revisions
			}
			return String.valueOf(noOfRevisions);
		}
	}

	/**
	 * @return the activationPolicy
	 */
	public String getActivationPolicy() {

		try {
			if ((null == bundle || (bundle.getState() & (Bundle.INSTALLED)) != 0)) {
				return (BundleProjectSettings.getLazyActivationPolicyFromManifest(project)) ? lazyyValueName : eagerValueName;
			} else {
				return (ManifestOptions.getlazyActivationPolicy(bundle)) ? lazyyValueName : eagerValueName;
			}
		} catch (InPlaceException e) {
			// Don't spam this meassage.
			if (!BuildErrorClosure.hasManifestBuildErrors(project) && BuildErrorClosure.hasBuildState(project)) {
				String msg = ErrorMessage.getInstance().formatString("error_get_policy", project.getName());
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
			}
		}
		return "?";
	}

	/**
	 * Get the location identifier for the project or the bundle if it is installed. If the bundle is
	 * uninstalled the reference scheme is used when retrieving the location identifier.
	 * 
	 * @return the location identifier for the bundle or the project if the bundle is null throws
	 *         ProjectLocationException if the specified project is null or the location of the specified
	 *         project could not be found
	 */
	public String getBundleLocationIdentifier() throws ProjectLocationException {
		return BundleJobManager.getRegion().getBundleLocationIdentifier(project);
	}

	public String getLastInstalledOrUpdated() {
		if (null == bundle) {
			return "";
		}
		return new Date(bundle.getLastModified()).toString();
	}

	public String getModifiedDate() {
		return new Date(project.getLocalTimeStamp()).toString();
	}

	public String getAllRequiringLabelName() {
		String requiringLabel = null;
		if (null == bundle) {
			requiringLabel = BundleProperties.referencingProjectsLabelName;
		} else {
			requiringLabel = BundleProperties.requiringAllBundlesLabelName;
		}
		return requiringLabel;
	}

	/**
	 * Get all bundle projects requiring capabilities from this bundle project
	 * 
	 * @return all requiring bundles
	 */
	public String getDeclaredRequiringBundleProjects() {
		String requires = null;
		if (null == bundle) {
			try {
				Collection<IProject> projects = projectSorter.sortRequiringProjects(Collections.singleton(project));
				projects.remove(project);
				requires = BundleProjectState.formatProjectList(projects);
			} catch (CircularReferenceException e) {
				requires = "Cycles";
			}
		} else {
			try {
				Collection<Bundle> bundles = bundleSorter.sortDeclaredRequiringBundles(Collections.singleton(bundle),
						BundleJobManager.getRegion().getBundles());
				bundles.remove(bundle);
				requires = BundleJobManager.getRegion().formatBundleList(bundles, false);
			} catch (CircularReferenceException e) {
				requires = "Cycles";
			}
		}
		return requires;
	}

	public String getResolvedRequiringLabelName() {
		return BundleProperties.requiringResolvedBundlesLabelName;
	}

	/**
	 * Get resolved bundles requiring capabilities from this resolved bundle
	 * 
	 * @return all requiring bundles
	 */
	public String getResolvedRequiringBundleProjects() {
		String requires = null;
		if (null != bundle && (bundle.getState() & (Bundle.UNINSTALLED | Bundle.INSTALLED)) == 0) {
			try {
				Collection<Bundle> bundles = bundleSorter.sortRequiringBundles(Collections.singleton(bundle));
				bundles.remove(bundle);
				requires = BundleJobManager.getRegion().formatBundleList(bundles, false);
			} catch (CircularReferenceException e) {
				requires = "Cycles";
			}
		}
		return requires;
	}

	public String getAllProvidingLabelName() {
		String providingLabel = null;
		if (null == bundle) {
			providingLabel = BundleProperties.referencedProjectsLabelName;
		} else {
			providingLabel = BundleProperties.providingAllBundlesLabelName;
		}
		return providingLabel;
	}

	/**
	 * Get all bundle projects providing capabilities to this bundle project
	 * 
	 * @return all providing bundles
	 */
	public String getAllProvidingBundleProjects() {
		String providers = null;
		if (null == bundle) {
			try {
				Collection<IProject> projects = projectSorter.sortProvidingProjects(Collections.singleton(project));
				projects.remove(project);
				providers = BundleProjectState.formatProjectList(projects);
			} catch (CircularReferenceException e) {
				providers = "Cycles";
			}
		} else {
			try {
				Collection<Bundle> bundles = bundleSorter.sortDeclaredProvidingBundles(Collections.singleton(bundle),
						BundleJobManager.getRegion().getBundles());
				bundles.remove(bundle);
				providers = BundleJobManager.getRegion().formatBundleList(bundles, false);
			} catch (CircularReferenceException e) {
				providers = "Cycles";
			}
		}
		return providers;
	}

	public String getResolvedProvidingLabelName() {
		return BundleProperties.providingResolvedBundlesLabelName;
	}

	/**
	 * Get resolved bundles providing capabilities to this resolved bundle
	 * 
	 * @return all providing bundles
	 */
	public String getResolvedProvidingBundleProjects() {
		String providers = null;
		if (null != bundle && (bundle.getState() & (Bundle.UNINSTALLED | Bundle.INSTALLED)) == 0) {
			try {
				Collection<Bundle> bundles = bundleSorter.sortProvidingBundles(Collections.singleton(bundle));
				bundles.remove(bundle);
				providers = BundleJobManager.getRegion().formatBundleList(bundles, false);
			} catch (CircularReferenceException e) {
				providers = "Cycles";
			}
		}
		return providers;
	}

	public String getUIExtension() {
		Boolean uiExtensions = false;
		try {
			uiExtensions = ProjectProperties.isUIContributor(project);
		} catch (InPlaceException e) {
		}
		return uiExtensions.toString();
	}

	/**
	 * @return the bundle object
	 */
	public final Bundle getBundle() {
		return bundle;
	}

	/**
	 * @return the value
	 */
	public final String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public final void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public final void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the java project or null
	 */
	public final IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * @return the java project or null
	 */
	public final IProject getProject() {
		return project;
	}

	/**
	 * @param project the project to set
	 */
	public final void setProject(IProject project) {
		this.project = project;
	}
}
