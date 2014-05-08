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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleCommand;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleRegion;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.bundlemanager.ProjectLocationException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
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
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Checks and tags bundle projects that are JavaTime nature enabled with pending bundle operations. What kind
 * of bundle operation to add to the bundle project is determined by the resource delta and the state of the
 * bundle project.
 * <p>
 * An exception to only considering the resource delta and the state is the move CRUD operation for a bundle
 * project. If the moved project is in at least state installed it is tagged with an uninstall operation followed by
 * an activate operation.
 * <ol>
 * <li>Bundle projects with resource deltas or null delta and in at least state installed are tagged for
 * update.
 * <li>Bundle projects with resource deltas or null delta and in state uninstalled are tagged for activate.
 * <li>A project with new requirements on UI plug-in(s), when UI plug-ins are not allowed is tagged for
 * deactivation instead of update. A warning is sent to the Log View
 * <li>Projects with an empty resource delta (not changed since last build) are not tagged with any pending
 * bundle operation.
 * <li>Projects with build errors are not tagged and a warning is sent to the log view
 * </ol>
 */
public class JavaTimeBuilder extends IncrementalProjectBuilder {

	public static final String JAVATIME_BUILDER_ID = "no.javatime.inplace.JavaTimeBuilder";

	public JavaTimeBuilder() {
	}

	// Incremental build
	class DeltaVisitor implements IResourceDeltaVisitor {

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					if (Category.DEBUG && Category.getState(Category.build))
						TraceMessage.getInstance().getString("added_resource", Category.build, resource.getName());
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
				if (resource instanceof IFile && resource.getName().endsWith(BundleProject.MANIFEST_FILE_NAME)) {
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
			if (resource instanceof IFile && resource.getName().endsWith(BundleProject.MANIFEST_FILE_NAME)) {
				if (Category.DEBUG && Category.getState(Category.build))
					TraceMessage.getInstance().getString("full_build_resource", resource.getName());
			}
			return true;
		}
	}

	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {

		try {
			IProject project = getProject();
			BundleTransition bundleTransition = BundleManager.getTransition();
			BundleCommand bundleCommand = BundleManager.getCommand();
			// Build is no longer pending. Remove as early as possible
			// Also removed in the post build listener
			bundleTransition.removePending(project, Transition.BUILD);
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("start_build");
			IResourceDelta delta = getDelta(project);
			if (kind == FULL_BUILD) {
				fullBuild(monitor);
			} else { // (kind == INCREMENTAL_BUILD || kind == AUTO_BUILD)
				incrementalBuild(delta, monitor);
			}
			BundleRegion bundleRegion = BundleManager.getRegion();
			Bundle bundle = bundleRegion.get(project);
			// Uninstalled project with no deltas
			IResourceDelta[] resourceDelta = null;
			if (null != delta) {
				resourceDelta = delta
						.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED, IResource.NONE);
			} else if (kind != FULL_BUILD) { // null delta when not a full build imply an unspecified change
				if (Category.getState(Category.bundleOperations)) {
					TraceMessage.getInstance().getString("no_build_delta_available", project.getName());
				}
			}
			if (null != resourceDelta && resourceDelta.length == 0) { // no change since last build
				if (Category.getState(Category.bundleOperations)) {
					TraceMessage.getInstance().getString("no_build_delta", project.getName());
				}
			}

			// Activated project is imported, opened or has new requirements on UI plug-in(s), when UI plug-ins are
			// not allowed
			if (!InPlace.getDefault().getCommandOptionsService().isAllowUIContributions()
					&& ProjectProperties.getUIContributors().contains(project)) {
				if (null == bundle) {
					bundleCommand.registerBundleProject(project, bundle, null != bundle ? true : false);
					// When an activated project is imported or opened, install in an activated workspace before
					// deactivating the bundle
					if (ProjectProperties.getActivatedProjects().size() > 1) {
						bundleTransition.addPending(project, Transition.INSTALL);
					}
				}
				logDependentUIContributors(project);
				bundleTransition.addPending(project, Transition.DEACTIVATE);
			} else {
				if (!ProjectProperties.hasBuildErrors(project) && ProjectProperties.hasBuildState(project)) {
					if (null != bundle) {
						// Moved projects requires a reactivate (uninstall and bundle activate)
						try {
							if (isMoveOperation(project)) {
								bundleTransition.addPending(project, Transition.UNINSTALL);
								bundleTransition.addPending(project, Transition.ACTIVATE);
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
								msg = ExceptionMessage.getInstance().formatString("project_location_error", project.getName());
							} else {
								msg = ExceptionMessage.getInstance().formatString("project_to_move_is_not_accessible");							
							}
							IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e);
							StatusManager.getManager().handle(status, StatusManager.LOG);
						}
					} else {
						// Always tag an uninstalled activated project for bundle activation
						bundleCommand.registerBundleProject(project, bundle, null != bundle ? true : false);
						bundleTransition.addPending(project, Transition.ACTIVATE);
					}
				} else {
					logBuildError(project);
				}
			}
			if (Category.DEBUG && Category.getState(Category.build))
				TraceMessage.getInstance().getString("end_build");
		} catch (ExtenderException e) {
			ExceptionMessage.getInstance().handleMessage(e, e.getMessage());
		}
		return null; // ok to return null;
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		if (Category.getState(Category.bundleOperations))
			TraceMessage.getInstance().getString("full_bundle_build", getProject().getName(),
					getProject().getLocation().toOSString());
		if (Category.DEBUG && Category.getState(Category.build))
			getProject().accept(new ResourceVisitor());
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		if (Category.getState(Category.bundleOperations))
			TraceMessage.getInstance().getString("incremental_bundle_build", getProject().getName(),
					getProject().getLocation().toOSString());
		if (Category.DEBUG && Category.getState(Category.build))
			delta.accept(new DeltaVisitor());
	}

	private boolean isMoveOperation(IProject project) throws ProjectLocationException{

		String projectLoaction = ProjectProperties.getProjectLocationIdentifier(project, true);
		String bundleLocation = BundleManager.getRegion().getBundleLocationIdentifier(project);
		if (!projectLoaction.equals(bundleLocation) && ProjectProperties.isInstallableProject(project)) {
			return true;
		}
		return false;
	}

	/**
	 * Log messages to Log View. Include requiring projects if any and differentiate message depending on the
	 * auto build option.
	 * 
	 * @param project with build error
	 */
	private void logBuildError(IProject project) {
		Collection<IProject> projectErrorClosures = null;
		boolean cycle = false;
		try {
			ProjectSorter ps = new ProjectSorter();
			projectErrorClosures = ps.getRequiringBuildErrorClosure(Collections.singletonList(project));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
			cycle = true;
		}
		try {
			String msg = null;
			if (InPlace.getDefault().getCommandOptionsService().isUpdateOnBuild()) {
				msg = WarnMessage.getInstance().formatString("build_error_in_project_to_update", project.getName());
			} else {
				msg = WarnMessage.getInstance().formatString("build_error_in_project", project.getName());
			}
			IBundleStatus buildStatus = new BundleStatus(StatusCode.BUILDERROR, InPlace.PLUGIN_ID, msg);
			Bundle bundle = BundleManager.getRegion().get(project);
			if (null != bundle && (bundle.getState() & (Bundle.INSTALLED)) == 0) {
				msg = WarnMessage.getInstance().formatString("using_current_revision", bundle);
				IBundleStatus revisionStatus = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg);
				buildStatus.add(revisionStatus);
			}
			if (!cycle) {
				projectErrorClosures.remove(project);
				if (projectErrorClosures.size() > 0) {
					// Add requiring bundles to build error message
					msg = WarnMessage.getInstance().formatString("requiring_bundles",
							ProjectProperties.formatProjectList(projectErrorClosures));
					IBundleStatus status = new BundleStatus(StatusCode.BUILDERROR, InPlace.PLUGIN_ID, msg);
					buildStatus.add(status);
				}
			}
			StatusManager.getManager().handle(buildStatus, StatusManager.LOG);
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
	}

	/**
	 * Sends a warning to the Log View that the specified project has providing projects that allows UI
	 * contributions using Extensions when not allowed. If the project have no such providing projects or does
	 * not contribute to the UI itself an internal warning message is sent to the Log View
	 * 
	 * @param project which contributes to the UI using Extensions and/or with providing projects that
	 *          contributes to the UI using Extensions
	 */
	private void logDependentUIContributors(IProject project) {

		try {
			ProjectSorter ps = new ProjectSorter();
			Collection<IProject> projects = ps.sortProvidingProjects(Collections.singleton(project));
			Collection<IProject> uiContributers = ProjectProperties.getUIContributors();
			projects.retainAll(uiContributers);
			if (!ProjectProperties.contributesToTheUI(project)) {
				projects.remove(project);
			}
			IBundleStatus buildStatus = null;
			if (projects.size() == 0) {
				String msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_internal",
						project.getName());
				buildStatus = new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg);

			} else {
				String msg = WarnMessage.getInstance().formatString("uicontributors_deactivate", project.getName(),
						ProjectProperties.formatProjectList(projects));
				buildStatus = new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg);
				msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_info_deactivate",
						project.getName());
				buildStatus.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
				msg = WarnMessage.getInstance().formatString("uicontributors_deactivate_info");
				buildStatus.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
			}
			StatusManager.getManager().handle(buildStatus, StatusManager.LOG);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		} catch (ExtenderException e) {
			String msg = WarnMessage.getInstance().formatString("uicontributors_fail_get", project.getName());
			IBundleStatus status = new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}
}
