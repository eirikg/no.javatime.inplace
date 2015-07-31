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
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectDependencies;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
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
 * Checks and adds pending bundle operations to bundle projects that are JavaTime nature enabled
 * (activated). What kind of bundle operation to add to the bundle project is determined by the
 * resource delta and the life cycle state of the bundle project.
 * <p>
 * An exception - to only considering the resource delta and the state - is the move CRUD operation
 * for a bundle project. If the moved project is in at least state installed it is tagged with an
 * uninstall operation followed by an activate operation.
 * <ol>
 * <li>Bundle projects with resource deltas or null delta and in at least state installed are tagged
 * for update.
 * <li>Bundle projects with resource deltas or null delta and in state uninstalled are tagged for
 * activate.
 * <li>A project with new requirements on UI plug-in(s), when UI plug-ins are not allowed is tagged
 * for deactivation instead of update. A warning is sent to the Log View
 * <li>Projects with an empty resource delta (not changed since last build) are not tagged with any
 * pending bundle operation.
 * <li>Projects with build errors are not tagged and a warning is sent to the log view
 * </ol>
 */
public class JavaTimeBuilder extends IncrementalProjectBuilder {

	public static final String JAVATIME_BUILDER_ID = "no.javatime.inplace.JavaTimeBuilder";

	// Log information
	private static Collection<IBundleStatus> builds = new ArrayList<>();
	private static long startTime;
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
		
		startTime = System.currentTimeMillis();
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
			if (Activator.getMessageOptionsService().isBundleOperations()) {
				if (!builds.isEmpty()) {
					IBundleStatus mStatus = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
							NLS.bind(Msg.BUILD_HEADER_TRACE, 
									new DecimalFormat().format(System.currentTimeMillis() - startTime)));
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
			bundleTransition.removeTransitionError(project, TransitionError.BUILD);
			bundleTransition.removePending(project, Transition.BUILD);
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("start_build");
			IResourceDelta delta = getDelta(project);
			if (kind == FULL_BUILD) {
				fullBuild(monitor);
			} else { // (kind == INCREMENTAL_BUILD || kind == AUTO_BUILD)
				incrementalBuild(delta, monitor);
			}
			Bundle bundle = bundleRegion.getBundle(project);
			// Uninstalled project with no deltas
			IResourceDelta[] resourceDelta = null;
			if (null != delta) {
				resourceDelta = delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED,
						IResource.NONE);
			} else if (kind != FULL_BUILD) {
				// null delta when not a full build imply an unspecified change
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.NO_RESOURCE_DELTA_BUILD_AVAILABLE_TRACE,
							new Object[] { project.getName() });
					IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
					synchronized (builds) {
						builds.add(status);
					}
				}
			}
			if (null != resourceDelta && resourceDelta.length == 0) { // no change since last build
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.NO_RESOURCE_DELTA_BUILD_TRACE,
							new Object[] { project.getName() });
					IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
					synchronized (builds) {
						builds.add(status);
					}
				}
				// TODO NB Test
				return null;
			}

			// Activated project is imported, opened or has new requirements on UI plug-in(s), when UI
			// plug-ins are not allowed
			if (!commandOptions.isAllowUIContributions()
					&& bundleProjectCandidates.getUIPlugins().contains(project)) {
				if (null == bundle) {
					ActivateProject activate = new ActivateProjectJob();
					if (!bundleRegion.isProjectRegistered(project)) {
						boolean natureEnabled = activate.isProjectActivated(project);
						bundleRegion.registerBundleProject(project, bundle, natureEnabled);
					}
					// When an activated project is imported or opened, install in an activated workspace
					// before deactivating the bundle
					if (activate.getActivatedProjects().size() > 1) {
						bundleTransition.addPending(project, Transition.INSTALL);
					}
				}
				logDependentUIContributors(project);
				bundleTransition.addPending(project, Transition.DEACTIVATE);
			} else {
				if (!BuildErrorClosure.hasBuildErrors(project) && BuildErrorClosure.hasBuildState(project)) {
					// Do not handle newly opened, created or imported bundles
					if (null != bundle) {
						// Moved projects requires a reactivate (uninstall and bundle activate)
						try {
							if (isMoveOperation(project)) {
								bundleTransition.addPending(project, Transition.UNINSTALL);
								bundleTransition.addPending(project, Transition.ACTIVATE_BUNDLE);
								// Project changed since last build
							} else if (null != resourceDelta && resourceDelta.length > 0) {
								bundleTransition.addPending(project, Transition.UPDATE);
								// Unspecified change or a full build
							} else if (null == delta) {
								bundleTransition.addPending(project, Transition.UPDATE);
							}
						} catch (ProjectLocationException e) {
							String msg = null;
							if (project.isAccessible()) {
								msg = ExceptionMessage.getInstance().formatString("project_location_error",
										project.getName());
							} else {
								msg = ExceptionMessage.getInstance().formatString(
										"project_to_move_is_not_accessible");
							}
							IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,
									e);
							StatusManager.getManager().handle(status, StatusManager.LOG);
						}
					} else {
						// Any source change is safeguarded by install. Implies that any update is superfluous 
						bundleTransition.removePending(project, Transition.UPDATE);
					}
				} else {
					logBuildError(project);
				}
			}
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("end_build");
		} catch (InPlaceException | ExtenderException | CoreException | ProjectLocationException e) {
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e), StatusManager.LOG);
		}
		return null; // ok to return null;
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException, ExtenderException {
		if (messageOptions.isBundleOperations()) {
			IProject project = getProject();
			Bundle bundle = bundleRegion.getBundle(project);
			String msg = NLS.bind(Msg.FULL_BUILD_TRACE, new Object[] { project.getName(),
					project.getLocation().toOSString() });
			IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
			synchronized (builds) {
				builds.add(status);
			}
		}
		if (Category.DEBUG && Category.getState(Category.build))
			getProject().accept(new ResourceVisitor());
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
			throws CoreException, ExtenderException {

		if (messageOptions.isBundleOperations()) {
				IProject project = getProject();
				Bundle bundle = bundleRegion.getBundle(project);
				String msg = NLS.bind(Msg.INCREMENTAL_BUILD_TRACE, new Object[] { project.getName(),
						project.getLocation().toOSString() });
				IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
				synchronized (builds) {
					builds.add(status);
				}
			}
			if (Category.DEBUG && Category.getState(Category.build))
				delta.accept(new DeltaVisitor());
	}

	private boolean isMoveOperation(IProject project) throws ProjectLocationException, ExtenderException {

		String projectLoaction = bundleRegion.getProjectLocationIdentifier(project, null);
		String bundleLocation = bundleRegion.getBundleLocationIdentifier(project);
		if (!projectLoaction.equals(bundleLocation) && bundleProjectCandidates.isInstallable(project)) {
			return true;
		}
		return false;
	}

	/**
	 * Log messages to Log View. Include requiring projects if any and select message depending on the
	 * auto build option.
	 * 
	 * @param project with build error
	 */
	private void logBuildError(IProject project) throws ExtenderException {

		BuildErrorClosure be = null;
		boolean cycle = false;
		try {
			ProjectDependencies.getProvidingProjects(project);
			ProjectDependencies.getRequiringProjects(project);
			try {
				// Use same closure rules as update
				be = new BuildErrorClosure(Collections.<IProject> singletonList(project),
						Transition.UPDATE, Closure.REQUIRING);
				// Get the closures to reveal any cycles
				if (be.getBuildErrors().size() > 0) {
					if (BuildErrorClosure.hasBuildErrors(project)
							|| !BuildErrorClosure.hasBuildState(project)) {
						bundleTransition.setTransitionError(project, TransitionError.BUILD);
					}
				}
			} catch (CircularReferenceException e) {
				String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
				IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
						project, msg, e);
				multiStatus.add(e.getStatusList());
				StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
				cycle = true;
			}
			if (!cycle && messageOptions.isBundleOperations()) {
				String msg = null;
				Bundle bundle = bundleRegion.getBundle(project);
				if (commandOptions.isUpdateOnBuild()) {
					msg = NLS.bind(Msg.BUILD_ERROR_UPDATE_TRACE, project.getName());
				} else {
					msg = NLS.bind(Msg.BUILD_ERROR_TRACE, project.getName());
				}
				IBundleStatus buildStatus = new BundleStatus(StatusCode.BUILDERROR, bundle, project, msg,
						null);
				if (be.hasBuildErrors()) {
					String buildMsg = NLS.bind(Msg.UPDATE_BUILD_ERROR_INFO, new Object[] { project.getName(),
							bundleProjectCandidates.formatProjectList(be.getBuildErrors()) });
					be.setBuildErrorHeaderMessage(buildMsg);
					IBundleStatus errorStatus = be.getErrorClosureStatus();
					buildStatus.add(errorStatus);
					if (null != bundle && (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
						msg = NLS.bind(Msg.USING_CURRENT_REVISION_TRACE, bundle);
						IBundleStatus revisionStatus = new BundleStatus(StatusCode.INFO, bundle, project, msg,
								null);
						errorStatus.add(revisionStatus);
						// buildStatus.add(revisionStatus);
					}
					synchronized (builds) {
						builds.add(errorStatus);
					}
				}
			}
		} catch (InPlaceException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Sends a warning to the Log View that the specified project has providing projects that allows
	 * UI contributions using Extensions when not allowed. If the project have no such providing
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
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
					project, msg, null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		} catch (InPlaceException e) {
			String msg = WarnMessage.getInstance().formatString("uicontributors_fail_get",
					project.getName());
			IBundleStatus status = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, msg,
					null);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}
}
