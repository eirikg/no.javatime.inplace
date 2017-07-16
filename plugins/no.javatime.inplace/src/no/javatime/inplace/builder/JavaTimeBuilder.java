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
package no.javatime.inplace.builder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Remove and add pending bundle transitions to bundle projects that are JavaTime nature enabled. A
 * project is said to be activated when enabled with the JavaTime nature.
 * <p>
 * Type of bundle transition to add is mainly determined by the resource delta and/or the life cycle
 * state of the bundle project.
 * <p>
 * The following conditions determine type of pending transition to add:
 * <ol>
 * <li>Bundle projects with resource deltas or null delta and in at least state installed are tagged
 * for update.
 * <li>A project with new requirements on UI plug-in(s), when UI plug-ins are not allowed is tagged
 * for deactivation independent of its state.
 * <li>If a moved project is in at least state installed, it is tagged with an uninstall transition
 * followed by an activate transition.
 * <li>Bundle projects with resource deltas or null delta and in state uninstalled are tagged for
 * activate.
 * <li>Projects with an empty resource delta (not changed since last build) are not tagged with any
 * pending bundle transition.
 * <li>Projects with build errors are not tagged. Instead a warning is sent to the log view.
 * </ol>
 */
public class JavaTimeBuilder extends IncrementalProjectBuilder {

	public static final String JAVATIME_BUILDER_ID = "no.javatime.inplace.JavaTimeBuilder";

	// Log information
	private static Collection<IBundleStatus> builds = new ArrayList<>();
	private static long startTime;
	private static boolean autoBuildOff;
	private BundleProjectCandidates bundleProjectCandidates;
	private BundleTransition bundleTransition;
	private BundleRegion bundleRegion;
	private MessageOptions messageOptions;
	private CommandOptions commandOptions;

	public JavaTimeBuilder() {

		try {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
			bundleTransition = Activator.getBundleTransitionService();
			bundleRegion = Activator.getBundleRegionService();
			messageOptions = Activator.getMessageOptionsService();
			commandOptions = Activator.getCommandOptionsService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Record build start time
	 * <p>
	 * Invoke before build in pre build listener
	 */
	public static synchronized void preBuild() {
		try {
			if (!Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
				autoBuildOff = true;
			} else {
				autoBuildOff = false;
				startTime = System.currentTimeMillis();
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			autoBuildOff = false;
		} finally {
			builds.clear();
		}
	}

	public static boolean hasBuild() {
		return builds.size() > 0 ? true : false;
	}

	/**
	 * Log build time and projects built
	 * <p>
	 * Invoke after build in post build listener
	 */
	public synchronized static void postBuild() {

		try {
			IBundleStatus mStatus = null;
			if (Activator.getMessageOptionsService().isBundleOperations()) {
				if (!builds.isEmpty()) {
					if (autoBuildOff) {
						mStatus = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID,
								Msg.BUILD_HEADER_TRACE_AUTO_BUILD_OFF);
					} else {
						mStatus = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID,
								NLS.bind(Msg.BUILD_HEADER_TRACE,
										new DecimalFormat().format(System.currentTimeMillis() - startTime)));
					}
					for (IBundleStatus status : builds) {
						mStatus.add(status);
					}
					Activator.log(mStatus);
				}
			}
		} catch (ExtenderException | BundleLogException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			builds.clear();
			autoBuildOff = false;
		}
	}

	// Incremental build
	class DeltaVisitor implements IResourceDeltaVisitor {

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					if (Category.DEBUG && Category.getState(Category.build))
						TraceMessage.getInstance().getString("added_resource", Category.build,
								resource.getName());
				}
				break;
			case IResourceDelta.REMOVED:
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					if (Category.DEBUG && Category.getState(Category.build))
						TraceMessage.getInstance().getString("removed_resource", resource.getName());
				}
				break;
			case IResourceDelta.CHANGED:
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					if (Category.DEBUG && Category.getState(Category.build))
						TraceMessage.getInstance().getString("changed_resource", resource.getName());
				}
				if (resource instanceof IFile
						&& resource.getName().endsWith(BundleProjectMeta.MANIFEST_FILE_NAME)) {
					if (Category.DEBUG && Category.getState(Category.build))
						TraceMessage.getInstance().getString("changed_resource", resource.getName());
				}
				break;
			default:
				break;
			}
			return true;
		}
	}

	// Full build
	class ResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			if (resource instanceof IFile && resource.getName().endsWith(".java")) {
				if (Category.DEBUG && Category.getState(Category.build))
					TraceMessage.getInstance().getString("full_build_resource", resource.getName());
			}
			if (resource instanceof IFile
					&& resource.getName().endsWith(BundleProjectMeta.MANIFEST_FILE_NAME)) {
				if (Category.DEBUG && Category.getState(Category.build))
					TraceMessage.getInstance().getString("full_build_resource", resource.getName());
			}
			return true;
		}
	}

	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args,
			IProgressMonitor monitor) throws CoreException {

		try {
			IProject project = getProject();
			bundleTransition.clearBuildTransitionError(project);
			bundleTransition.removePending(project, Transition.BUILD);
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("start_build");
			IResourceDelta projectDelta = getDelta(project);
			if (kind == FULL_BUILD) {
				fullBuild(monitor);
			} else {
				incrementalBuild(projectDelta, monitor);
			}
			Bundle bundle = bundleRegion.getBundle(project);
			IResourceDelta[] projectChildrenDelta = null;
			if (null != projectDelta) {
				projectChildrenDelta = projectDelta
						.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED, IResource.NONE);
			} else if (kind != FULL_BUILD) {
				// No project delta and not a full build imply an unspecified change
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.NO_RESOURCE_DELTA_BUILD_AVAILABLE_TRACE,
							new Object[] { project.getName() });
					IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
					synchronized (builds) {
						builds.add(status);
					}
				}
			}
			// No change since last build
			if (null != projectChildrenDelta && projectChildrenDelta.length == 0) {
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.NO_RESOURCE_DELTA_BUILD_TRACE,
							new Object[] { project.getName() });
					IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
					synchronized (builds) {
						builds.add(status);
					}
				}
				return null;
			}
			// Check dependency on UI plug-in(s), when UI plug-ins are not allowed
			if (!commandOptions.isAllowUIContributions()
					&& bundleProjectCandidates.getUIPlugins().contains(project)) {
				if (null == bundle) {
					// This is a new (opened or imported) project in state uninstalled
					ActivateProject activate = new ActivateProjectJob();
					if (!bundleRegion.isProjectRegistered(project)) {
						bundleRegion.registerBundleProject(project, bundle, false);
					}
					// Install the new project before deactivating the bundle
					if (activate.getActivatedProjects().size() > 1) {
						bundleTransition.addPending(project, Transition.INSTALL);
					}
				}
				logDependentUIContributors(project);
				bundleTransition.addPending(project, Transition.DEACTIVATE);
			} else {
				if (!hasBuildError(project)) {
					// Do not handle newly opened, created or imported bundles
					if (null != bundle) {
						// Moved projects requires a reactivate (uninstall and bundle activate)
						try {
							if (isMoveOperation(project)) {
								bundleTransition.addPending(project, Transition.UNINSTALL);
								bundleTransition.addPending(project, Transition.ACTIVATE_BUNDLE);
								// Project changed since last build
							} else if (null != projectChildrenDelta && projectChildrenDelta.length > 0) {
								bundleTransition.addPending(project, Transition.UPDATE);
								// Unspecified change or a full build
							} else if (null == projectDelta) {
								bundleTransition.addPending(project, Transition.UPDATE);
							}
						} catch (ProjectLocationException | InPlaceException e) {
							String msg = null;
							if (null != project && project.isAccessible()) {
								msg = ExceptionMessage.getInstance().formatString("project_location_error",
										project.getName());
							} else {
								msg = ExceptionMessage.getInstance()
										.formatString("project_to_move_is_not_accessible");
							}
							IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
									msg, e);
							StatusManager.getManager().handle(status, StatusManager.LOG);
						}
					} else {
						// Any source change is safeguarded by install. Implies that any update is superfluous
						bundleTransition.removePending(project, Transition.UPDATE);
					}
				}
			}
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("end_build");
		} catch (InPlaceException | ExtenderException | CoreException | ProjectLocationException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		return null; // ok to return null;
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException, ExtenderException {

		addBuildStatusMessage(Msg.FULL_BUILD_TRACE);
		if (Category.DEBUG && Category.getState(Category.build))
			getProject().accept(new ResourceVisitor());
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
			throws CoreException, ExtenderException {

		addBuildStatusMessage(Msg.INCREMENTAL_BUILD_TRACE);
		if (Category.DEBUG && Category.getState(Category.build))
			delta.accept(new DeltaVisitor());
	}

	/**
	 * Examine the specified project for build errors and add errors to the build status list
	 * 
	 * @param project Project to examine for build errors
	 * @return True if the project has errors and false otherwise
	 */
	private boolean hasBuildError(IProject project) throws ExtenderException {

		if (BundleProjectBuildError.hasCycles(project) || BundleProjectBuildError.hasBundleErrors(project, true)) {
			IBundleStatus status = bundleTransition.getTransitionStatus(project);
			if (null != status) {
				synchronized (builds) {
					builds.add(status);
				}
			}
			return true;
		} else if (BundleProjectBuildError.hasCompileErrors(project)) {
			IBundleStatus status = bundleTransition.getTransitionStatus(project);
			if (null != status) {
				synchronized (builds) {
					builds.add(status);
				}
			}
			if (Activator.getCommandOptionsService().isActivateOnCompileError()) {
				addDependencyStatus(project, status);
				return false;
			} else {
				// Current revision status
				Bundle bundle = bundleRegion.getBundle(project);
				if (null != bundle && (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
					String msg = NLS.bind(Msg.USING_CURRENT_REVISION_TRACE, bundle);
					IBundleStatus revisionStatus = new BundleStatus(StatusCode.INFO, bundle, project, msg,
							null);
					status.add(revisionStatus);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Add status objects to the specified status describing dependent projects to the specified
	 * project
	 * 
	 * @param project Project with dependencies
	 * @param result Status object to add status objects to describing dependencies to the specified
	 * project or no added status objects if the project has no dependencies
	 */
	private void addDependencyStatus(IProject project, IBundleStatus result) {

		try {
			if (messageOptions.isBundleOperations()) {
				Bundle bundle = bundleRegion.getBundle(project);
				if (null != bundle) {
					Collection<IProject> projectClosure = null;
					BundleClosures closures = new BundleClosures();
					// Providing bundle status
					projectClosure = closures.projectActivation(Closure.PROVIDING,
							Collections.<IProject> singletonList(project), true);
					projectClosure.remove(project);
					if (projectClosure.size() > 0) {
						String msg = NLS.bind(Msg.PROVIDING_BUNDLES_INFO, project.getName(),
								bundleProjectCandidates.formatProjectList(projectClosure));
						result.add(new BundleStatus(StatusCode.INFO, bundle, project, msg, null));
					}
					// Requiring bundle status
					projectClosure = closures.projectDeactivation(Closure.REQUIRING,
							Collections.<IProject> singletonList(project), true);
					projectClosure.remove(project);
					if (projectClosure.size() > 0) {
						String msg = NLS.bind(Msg.REQUIRING_BUNDLES_INFO, project.getName(),
								bundleProjectCandidates.formatProjectList(projectClosure));
						result.add(new BundleStatus(StatusCode.INFO, bundle, project, msg, null));
					}
				}
			}
		} catch (InPlaceException | ExtenderException | CircularReferenceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Sends a warning to the Log View that the specified project has providing projects that allows
	 * UI contributions using Extensions when not allowed. If the project has no such providing
	 * projects or does not contribute to the UI itself an internal warning message is sent to the Log
	 * View
	 * 
	 * @param project which contributes to the UI using Extensions and/or with providing projects that
	 * contributes to the UI using Extensions
	 */
	private void logDependentUIContributors(IProject project) {
		try {
			ProjectSorter ps = new ProjectSorter();
			Collection<IProject> projects = ps.sortProvidingProjects(Collections.singleton(project));
			Collection<IProject> uiContributers = bundleProjectCandidates.getUIPlugins();
			projects.retainAll(uiContributers);
			if (!bundleProjectCandidates.isUIPlugin(project)) {
				projects.remove(project);
			}
			IBundleStatus buildStatus = null;
			if (projects.size() == 0) {
				String msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_internal",
						project.getName());
				buildStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, msg, null);

			} else {
				String msg = WarnMessage.getInstance().formatString("uicontributors_deactivate",
						project.getName(), bundleProjectCandidates.formatProjectList(projects));
				buildStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, msg, null);
				msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_info_deactivate",
						project.getName());
				buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, project, msg, null));
				msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_info");
				buildStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, project, msg, null));
			}
			StatusManager.getManager().handle(buildStatus, StatusManager.LOG);
		} catch (CircularReferenceException e) {
			IBundleStatus status = bundleTransition.getTransitionStatus(project);
			if (null != status) {
				StatusManager.getManager().handle(status, StatusManager.LOG);
			}
		} catch (InPlaceException e) {
			String msg = WarnMessage.getInstance().formatString("uicontributors_fail_get",
					project.getName());
			IBundleStatus status = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, msg,
					e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

	/**
	 * Determine if the reason for the build of the specified project is a move operation
	 * @param project The project being moved
	 * @return True if the project is being moved. Otherwise false
	 * @throws ProjectLocationException  if the specified project is null or the location of the specified project could not be found
	 * @throws InPlaceException  if the specified project is null, open but does not exist or a core exception is thrown internally
	 */
	private boolean isMoveOperation(IProject project) throws ProjectLocationException, InPlaceException {

		String projectLoaction = bundleRegion.getProjectLocationIdentifier(project, null);
		String bundleLocation = bundleRegion.getBundleLocationIdentifier(project);
		return !projectLoaction.equals(bundleLocation) && bundleProjectCandidates.isInstallable(project)
				? true
				: false;
	}

	/**
	 * Add an informative build message to the build status list
	 * 
	 * @param msgKind A message with one substitution for the project name to build 
	 */
	private void addBuildStatusMessage(String msgKind) {

		if (messageOptions.isBundleOperations()) {
			IProject project = getProject();
			Bundle bundle = bundleRegion.getBundle(project);
			String locMsg = NLS.bind(Msg.BUNDLE_LOCATION_TRACE, project.getLocation().toOSString());
			IBundleStatus locStatus = new BundleStatus(StatusCode.INFO, bundle, project, locMsg, null);
			String msg = NLS.bind(msgKind, new Object[] { project.getName() });
			IBundleStatus buildStatus = new BundleStatus(StatusCode.OK, bundle, project, msg, null);
			buildStatus.add(locStatus);
			synchronized (builds) {
				builds.add(buildStatus);
			}
		}		
	}	
}
