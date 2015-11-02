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

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.ErrorMessage;

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

	// Label names 
	public static String bundleLabelName = Msg.BUNDLE_NAME_PROP_LABEL;
	public static String bundleSymbolicNameLabelName = Msg.BUNDLE_SYMBOLIC_NAME_PROP_LABEL;
	public static String bundleVersionLabelName = Msg.BUNDLE_VERSION_PROP_LABEL;
	public static String projectLabelName = Msg.PROJECT_NAME_PROP_LABEL;
	public static String bundleIdLabelName = Msg.BUNDLE_ID_PROP_LABEL;
	public static String activationStatusLabelName = Msg.ACTIVATION_MODE_PROP_LABEL;
	public static String bundleStateLabelName = Msg.BUNDLE_STATE_PROP_LABEL;
	public static String servicesInUseLabelName = Msg.SERVICES_IN_USE_PROP_LABEL;
	public static String numberOfRevisionsLabelName = Msg.NUMBER_OF_REVISIONS_PROP_LABEL;
	public static String activationPolicyLabelName = Msg.ACTIVATION_POLICY_PROP_LABEL;
	public static String bundleStatusLabelName = Msg.BUNDLE_STATUS_PROP_LABEL;
	public static String locationLabelName = Msg.LOCATION_PROP_LABEL;
	public static String lastInstalledOrUpdatedLabelName = Msg.LAST_INSTALLED_OR_UPDATED_PROP_LABEL;
	public static String lastModifiedLabelName = Msg.LAST_MODIFIED_PROP_LABEL;
	public static String referencedProjectsLabelName = Msg.REFERENCED_PROJECTS_PROP_LABEL;
	public static String referencingProjectsLabelName = Msg.REFERENCING_PROJECTS_PROP_LABEL;
	public static String requiringDeclaredBundlesLabelName = Msg.REQUIRING_DECLARED_BUNDLES_PROP_LABEL;
	public static String providingDeclaredBundlesLabelName = Msg.PROVIDING_DECLARED_BUNDLES_PROP_LABEL;
	public static String requiringResolvedBundlesLabelName = Msg.REQUIRING_RESOLVED_BUNDLES_PROP_LABEL;
	public static String providingResolvedBundlesLabelName = Msg.PROVIDING_RESOLVED_BUNDLES_PROP_LABEL;
	public static String UIExtensionsLabelName = Msg.UI_EXTENSIONS_PROP_LABEL;
	public static String lastTransitionLabelName = Msg.LAST_TRANSITION_PROP_LABEL;

	private static String lazyyValueName = "Lazy";
	private static String eagerValueName = "Eager";

	private String name = null;
	private String value = null;
	private IProject project = null;
	private IJavaProject javaProject = null;
	private Bundle bundle = null;
	static private final BundleSorter bundleSorter = new BundleSorter();
	static private final ProjectSorter projectSorter = new ProjectSorter();

	private BundleProjectCandidates bundleProjectCandidates;
	private BundleCommand bundleCommand; 
	private BundleTransition bundleTransition;
	private BundleRegion bundleRegion;
	private BundleProjectMeta bundlePrrojectMeta;
	
	public BundleProperties(IProject project) throws ExtenderException {
		this.project = project;
		try {
			initServices();
			if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
		}
		bundle = bundleRegion.getBundle(project);
	}

	public BundleProperties(IJavaProject javaProject) throws ExtenderException {
		this.project = javaProject.getProject();
		this.javaProject = javaProject;
		initServices();
		bundle = bundleRegion.getBundle(project);
	}

	public BundleProperties(IProject project, String name, String value) throws ExtenderException {
		this.project = project;
		try {
			if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
		}
		initServices();
		bundle = bundleRegion.getBundle(project);
		this.name = name;
		this.value = value;

	}

	public BundleProperties(IJavaProject javaProject, String name, String value) throws ExtenderException {
		this.project = javaProject.getProject();
		this.javaProject = javaProject;
		initServices();
		bundle = bundleRegion.getBundle(project);
		this.name = name;
		this.value = value;

	}
	
	private void initServices() throws ExtenderException {
		
		bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		bundleCommand = Activator.getBundleCommandService(); 
		bundleTransition = Activator.getBundleTransitionService();
		bundleRegion = Activator.getBundleRegionService();
		bundlePrrojectMeta = Activator.getBundleProjectMetaService();
	}
	
	public String getBundleLabelName() {

		String bundleProjectLabelName = bundleLabelName;
		String symbolicName = null;
		String version = null;
		if (null == bundle) { // Uninstalled
			try {
				symbolicName = bundlePrrojectMeta.getSymbolicName(project);
				version = bundlePrrojectMeta.getBundleVersion(project);
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
		bundleKey = bundleRegion.getSymbolicKey(null, project);
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
				name = bundlePrrojectMeta.getSymbolicName(project);
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
				ver = bundlePrrojectMeta.getBundleVersion(project);
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
		boolean activated = bundleRegion.isBundleActivated(project);
		if (activated) {
			return "Activated";
		} else {
			return "Deactivated";
		}
	}

	public String getBundleStatus() {

		boolean isProjectActivated = bundleRegion.isBundleActivated(project);
		try {
			TransitionError error = bundleTransition.getTransitionError(project);
			if (error != TransitionError.NOERROR) {
				if (error != TransitionError.NOERROR) {
					switch (error) {
					case BUILD_CYCLE:
						return "Circular Bundle Reference";
					case BUILD_STATE:
						return "Missing Build State";
					case BUILD_DESCRIPTION_FILE:
						return "Missing Description File";
					case BUILD_MANIFEST:
						return "Manifest Problems";
					case BUILD_MODULAR_EXTERNAL_DUPLICATE:
						return "Duplicate of External Bundle";
					case BUILD_MODULAR_WORKSPACE_DUPLICATE:
						return "Duplicate of Workspace Bundle";
					case MODULAR_REFRESH_ERROR:
						return "Refresh Error";
					case MODULAR_EXCEPTION:
						return "Modular Problems";
					case MODULAR_EXTERNAL_UNINSTALL:
						return "External Uninstall";
					case SERVICE_EXCEPTION:
						return "Runtime Error";
					case SERVICE_INCOMPLETE_TRANSITION:
						return "Incomplete Transition";
					case SERVICE_STATECHANGE:
						return "State Change Error";
					case BUILD:
						return "Build Problems";
					default:
						return "Bundle Problems";
					}
				} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.UNINSTALLED)) != 0) {
					return "Install Problems"; 
				} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.INSTALLED)) != 0) {
					return "Resolve Problems";
				} else {
					return "Bundle Problems";
				}
			} else if (bundleTransition.containsPending(project, Transition.BUILD, false)) {
				return "Build Pending";
			} else if (isProjectActivated && bundleTransition.containsPending(project, Transition.UPDATE, false)) {
				return "Update Pending";
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
//		} else if (!BundleProjectBuildError.hasBuildState(project)) {
//				return "Missing Build State";
//			} else if (!BundleProjectBuildError.hasProjectDescriptionFile(project)) {
//				return "Missing project description";
//			} else if (BundleProjectBuildError.hasManifestBuildErrors(project)) {
//				return "Manifest Problems"; 
//			} else if (BundleProjectBuildError.hasCompileErrors(project)) {
//				return "Build Problems";
//			} else if (bundleTransition.containsPending(project, Transition.BUILD, false)) {
//				return "Build Pending";
//			} else if (isProjectActivated && bundleTransition.containsPending(project, Transition.UPDATE, false)) {
//				return "Update Pending";
//			} else if (null != bundle && bundleCommand.getBundleRevisions(bundle).size() > 1) {
//				return "Refresh Pending" + " (" + getBundleRevisions() + ")";
//			} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.RESOLVED)) != 0
//					&& !BundleSorter.isFragment(bundle)) {
//				return "Start Pending";
//			} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.STARTING)) != 0) {
//				return "Lazy Loading";
//			} else {
//				if (isProjectActivated) {
//					if ((bundleCommand.getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
//						return "Activated";
//					} else {
//						return "Running";
//					}
//				} else {
//					return "Deactivated"; // Activate Pending
//				}
//			}
		} catch (ProjectLocationException e) {
			return "Project Location Problem";
		} catch (InPlaceException e) {
			return "Bundle Problem";
		}
	}

	public String getLastTransition() {
		try {
			Transition transitiont = bundleTransition.getTransition(project);
			if (transitiont != Transition.NO_TRANSITION) {
				return bundleTransition.getTransitionName(transitiont, false, false);
			}
		} catch (ProjectLocationException e) {
		}
		return "<".concat(bundleTransition.getTransitionName(Transition.NO_TRANSITION, true, true)).concat(">");
	}

	/**
	 * @return the bundleState
	 */
	public String getBundleState() {
		return bundleCommand.getStateName(bundle);
	}

	public String getServicesInUse() {
		Bundle bundle = bundleRegion.getBundle(project);
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
				noOfRevisions = bundleCommand.getBundleRevisions(bundle).size();
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
				return (bundlePrrojectMeta.getActivationPolicy(project)) ? lazyyValueName : eagerValueName;
			} else {
				return (bundlePrrojectMeta.getCachedActivationPolicy(bundle)) ? lazyyValueName : eagerValueName;
			}
		} catch (InPlaceException e) {
			try {				
				// Don't spam this meassage.
				if (!BundleProjectBuildError.hasManifestBuildErrors(project) && BundleProjectBuildError.hasBuildState(project)) {
					String msg = ErrorMessage.getInstance().formatString("error_get_policy", project.getName());
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
				}
			} catch (Exception e2) {
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
		return bundleRegion.getBundleLocationIdentifier(project);
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
			requiringLabel = BundleProperties.requiringDeclaredBundlesLabelName;
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
				requires = bundleProjectCandidates.formatProjectList(projects);
			} catch (CircularReferenceException e) {
				requires = "Cycles";
			}
		} else {
			try {
				Collection<Bundle> bundles = bundleSorter.sortDeclaredRequiringBundles(Collections.singleton(bundle),
						bundleRegion.getBundles());
				bundles.remove(bundle);
				requires = bundleRegion.formatBundleList(bundles, false);
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
				requires = bundleRegion.formatBundleList(bundles, false);
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
			providingLabel = BundleProperties.providingDeclaredBundlesLabelName;
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
				providers = bundleProjectCandidates.formatProjectList(projects);
			} catch (CircularReferenceException e) {
				providers = "Cycles";
			}
		} else {
			try {
				Collection<Bundle> bundles = bundleSorter.sortDeclaredProvidingBundles(Collections.singleton(bundle),
						bundleRegion.getBundles());
				bundles.remove(bundle);
				providers = bundleRegion.formatBundleList(bundles, false);
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
				providers = bundleRegion.formatBundleList(bundles, false);
			} catch (CircularReferenceException e) {
				providers = "Cycles";
			}
		}
		return providers;
	}

	public String getUIExtension() {
		Boolean uiExtensions = false;
		try {
			uiExtensions = bundleProjectCandidates.isUIPlugin(project);
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
