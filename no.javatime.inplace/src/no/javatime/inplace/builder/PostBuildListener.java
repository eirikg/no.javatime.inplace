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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.UpdateScheduler;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.UserMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Schedules bundle jobs after a project is changed. This include created (create new, import and open),
 * renamed (rename and move), updated and projects being activated. Already activated projects that have been
 * changed are tagged with a pending bundle transition. The tag in combination with the state of the bundle
 * project determines the bundle job to schedule. Tagging projects with pending transitions is left to others
 * which usually are but not limited to bundle jobs, resource listeners and the {@link JavaTimeBuilder}.
 * Deactivated projects not tagged with a pending transition are recognized based on it's resource delta and
 * scheduled for bundle jobs according to the executed CRUD operation on the project.
 * <p>
 * Deleted, closed and renamed projects are scheduled for uninstall in
 * {@linkplain no.javatime.inplace.builder.PreChangeListener PreChangeListener}. Deleted and closed projects
 * are not considered in this resource listener. If the workspace is deactivated, no jobs are scheduled.
 * <p>
 * The following determines which jobs are scheduled for different CRU operations in an activated workspace:
 * <ol>
 * <li>Create (create new, import and open):
 * <ol>
 * <li>New projects are scheduled for installation (not activated) in the activate bundle job
 * <li>Opened and imported projects are started if they were activated before they where closed or exported,
 * otherwise they are installed by the activate bundle job.
 * </ol>
 * <li>Rename and move
 * <ol>
 * <li>If a project is renamed, the project with the new name is started if it was activated before the rename
 * operation. Otherwise it is installed.
 * <li>A project that is moved is first uninstalled and then installed and in addition resolved and started if
 * it is activated.
 * </ol>
 * <li>Update (the project has been built)
 * <ol>
 * <li>Updated projects are scheduled for a bundle update job. Built projects with an empty resource delta is
 * not updated unless it is tagged with a pending update transition. Projects with a null delta are usually
 * tagged with a pending update transition and hence updated.
 * </ol>
 * <li>Activate and Deactivate:
 * <ol>
 * <li>Deactivated providing projects of bundle projects being updated are scheduled for activation.
 * <li>Activated projects ({@link ActivateProjectJob}) are scheduled for a bundle activate job 
 * ({@link ActivateBundleJob})
 * <li>If a project to update has new requirements on UI plug-in(s), when UI plug-ins are not allowed it is
 * scheduled for deactivation instead of update
 * </ol>
 */
public class PostBuildListener implements IResourceChangeListener {

	private BundleCommand bundleCommand = BundleJobManager.getCommand();
	private BundleRegion bundleRegion = BundleJobManager.getRegion();
	private BundleTransition bundleTransition = BundleJobManager.getTransition();

	/**
	 * Schedules bundle jobs for changed projects. Removed and closed projects are handled in the
	 * {@link PreChangeListener}
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		// Nothing to do in a deactivated workspace where all bundle projects are uninstalled
		if (!ProjectProperties.isProjectWorkspaceActivated()) {
			return;
		}
		// After a build when source has changed and the project is pending for update and auto build is on
		UpdateJob updateJob = new UpdateJob(UpdateJob.updateJobName);
		// Projects that are moved, renamed, created, opened and imported in an active workspace and
		// projects with activate as pending transition
		ActivateBundleJob activateBundleJob = new ActivateBundleJob(ActivateBundleJob.activateJobName);
		// If a project to update has requirements on deactivated projects, the deactivated projects are scheduled
		// for project activation and update
		ActivateProjectJob activateProjectJob = new ActivateProjectJob(ActivateProjectJob.activateProjectsJobName);
		// Project with new requirements on UI plug-in(s), when UI plug-ins are not allowed
		DeactivateJob deactivateJob = new DeactivateJob(DeactivateJob.deactivateJobName);
		// Project probably moved or needs a reactivation for some reason
		UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName);
		// When a project is activated and in state uninstalled or deactivated and in state 
		// uninstalled in an activated workspace
		InstallJob installJob = new InstallJob(InstallJob.installJobName);
		traceBuilds();
		int buildType = event.getBuildKind();
		IResourceDelta rootDelta = event.getDelta();
		IResourceDelta[] resourceDeltas = null;
		if (null != rootDelta) {
			// Ignore removed (deleted or closed) projects which have been uninstalled in the pre-resource listener
			resourceDeltas = rootDelta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED,
					IResource.NONE);
		}
		// A null resource delta implies an unspecified change or a full build. An empty resource delta implies 
		// no change since last build, but the resource may have a pending transition
		if (null == resourceDeltas || resourceDeltas.length == 0) {
			for (IProject project : ProjectProperties.getInstallableProjects()) {
				try {
					removePendingBuildTransition(buildType, project);
					if (null != bundleTransition.getPendingTransitions(project)) {
						handlePendingTransition(project, activateProjectJob, activateBundleJob, updateJob, deactivateJob, uninstallJob, installJob);
					}
				} catch (InPlaceException e) {
					String msg = ExceptionMessage.getInstance().formatString("schedule_bundle_jobs", project.getName());
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e), StatusManager.LOG);
				}
			}
		} else {
			// Cycle all built projects with a delta and schedule appropriate bundle jobs according to type of
			// transition and project CRUD operation
			for (IResourceDelta projectDelta : resourceDeltas) {
				IResource projectResource = projectDelta.getResource();
				if (projectResource.isAccessible() && (projectResource.getType() & (IResource.PROJECT)) != 0) {
					IProject project = projectResource.getProject();
					try {
						if (Category.DEBUG && Category.getState(Category.listeners))
							ProjectChangeListener.traceDeltaKind(event, projectDelta, project);
						removePendingBuildTransition(buildType, project);
						if (!handlePendingTransition(project, activateProjectJob, activateBundleJob, updateJob,
								deactivateJob, uninstallJob, installJob)) {
							handleCRUDOperation(projectDelta, project, activateBundleJob);
						}
					} catch (InPlaceException e) {
						String msg = ExceptionMessage.getInstance().formatString("schedule_bundle_jobs",
								project.getName());
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e), StatusManager.LOG);
					}
				}
			}
		}
		// Include bundles that are no longer duplicates
		ActivateBundleJob postActivateBundleJob = null;
		try {
			if (InPlace.get().getCommandOptionsService().isUpdateOnBuild() && updateJob.pendingProjects() > 0) {
				postActivateBundleJob = UpdateScheduler.resolveduplicates(activateProjectJob, activateBundleJob,
						updateJob);
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
		scheduleJob(uninstallJob);
		scheduleJob(installJob);
		scheduleJob(deactivateJob);
		scheduleJob(activateProjectJob);
		scheduleJob(activateBundleJob);
		scheduleJob(updateJob);
		scheduleJob(postActivateBundleJob);
	}

	/**
	 * Schedules a job with pending projects. The job is not scheduled if it has no pending projects.
	 * 
	 * @param job to schedule. Can be null
	 * @return true if job is scheduled, otherwise false
	 */
	private boolean scheduleJob(BundleJob job) {
		if (null != job && job.hasPendingProjects()) {
			BundleJobManager.addBundleJob(job, 0);
			return true;
		}
		return false;
	}

	/**
	 * Removes the the pending {@link Transition#BUILD build} transition from the specified project
	 * 
	 * @param buildType determines if the pending build transition should be removed. Has to be one of
	 *          incremental build, full build, clean build or auto build
	 * @param project to remove the pending build transition from
	 * @return true if the pending build transition is cleared from the specified project. The transition does
	 *         not have to be present before the build transition is cleared, Otherwise false,
	 */
	private boolean removePendingBuildTransition(int buildType, IProject project) {
		if ((ProjectProperties.isAutoBuilding() || buildType == IncrementalProjectBuilder.INCREMENTAL_BUILD
				|| buildType == IncrementalProjectBuilder.FULL_BUILD || buildType == IncrementalProjectBuilder.CLEAN_BUILD)) {
			// Build is no longer pending
			bundleTransition.removePending(project, Transition.BUILD);
			return true;
		}
		return false;
	}

	/**
	 * Add the specified project as pending to one of the specified jobs if the project has one or more pending
	 * transitions that equals or is a combination of one of {@link Transition#ACTIVATE_BUNDLE},
	 * {@link Transition#UPDATE}, {@link Transition#UNINSTALL} or {@link Transition#DEACTIVATE}.
	 * <p>
	 * The project is only added to the update job if auto update is on or {@link Transition#UPDATE_ON_ACTIVATE}
	 * is set on the project in addition to the update transition.
	 * 
	 * @param project to add to one of the specified bundle jobs as a pending project
	 * @param activateProjectJob activates a project by setting the JavaTime nature on the project
	 * @param activateBundleJob installs deactivated projects and installs, resolves and optionally starts
	 *          activated projects
	 * @param updateJob updates a project
	 * @param deactivateJob deactivates a project by removing the JavaTime nature
	 * @param uninstallJob TODO
	 * @param installJob TODO
	 * @return true if the specified project has been added as a pending project to one of the specified bundle
	 *         jobs.Otherwise false
	 */
	private boolean handlePendingTransition(IProject project, ActivateProjectJob activateProjectJob,
			ActivateBundleJob activateBundleJob, UpdateJob updateJob, DeactivateJob deactivateJob, UninstallJob uninstallJob, InstallJob installJob) {
		boolean isPending = false;
		
		// Update activated modified projects. Deactivated projects are usually not updated but may be
		if (bundleTransition.containsPending(project, Transition.UPDATE, Boolean.FALSE)) {
			// If this project is part of an activate process and auto update is off, the project is tagged
			// with an update on activate transition and should be updated
			try {
				if (InPlace.get().getCommandOptionsService().isUpdateOnBuild()
						|| bundleTransition.containsPending(project, Transition.UPDATE_ON_ACTIVATE, Boolean.FALSE)) {
					UpdateScheduler.addChangedProject(project, updateJob, activateProjectJob);
					isPending = true;
				}
			} catch (InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);			
			}
		}
		// Usually set when a project is activated and in state uninstalled or deactivated and in state 
		// uninstalled in an activated workspace
		if (bundleTransition.containsPending(project, Transition.INSTALL, Boolean.TRUE)) {
			installJob.addPendingProject(project);
			isPending = true;
		}
		if (bundleTransition.containsPending(project, Transition.ACTIVATE_BUNDLE, Boolean.TRUE)) {
			// TODO Check this check
			if (ProjectProperties.isInstallableProject(project)) {
				activateBundleJob.addPendingProject(project);
				isPending = true;
			}
		}
		// Most commonly used when an activated project has new requirements on UI plug-in(s), when UI plug-ins
		// are not allowed
		if (bundleTransition.containsPending(project, Transition.DEACTIVATE, Boolean.TRUE)) {
			deactivateJob.addPendingProject(project);
			isPending = true;
		}
		// Project probably moved or needs a reactivation for some reason
		if (bundleTransition.containsPending(project, Transition.UNINSTALL, Boolean.TRUE)) {
				uninstallJob.addPendingProject(project);
				isPending = true;
		}
		return isPending;
	}

	/**
	 * Schedules an uninstall job for the specified bundle project when it has been moved and adds a moved,
	 * imported, opened, created or a renamed project as pending to the specified activate bundle job.
	 * <p>
	 * Create operations for uninstalled projects including import, open and create (always deactivated) are
	 * added as pending to the activate bundle job.
	 * <p>
	 * Deactivated bundle projects being renamed, are uninstalled in the pre change listener and added as a
	 * pending project to the specified activate bundle job. Activated renamed bundle projects have a pending
	 * activate transition and are scheduled for an activate bundle job in
	 * {@link #handlePendingTransition(IProject, ActivateProjectJob, ActivateBundleJob, UpdateJob, DeactivateJob, UninstallJob, InstallJob)
	 * handlePendingTransition}.
	 * <p>
	 * A project that has been moved (classified as a rename operation) is always at least in state installed. A
	 * moved project is first scheduled for an uninstall job and than added as a pending project to the activate
	 * bundle job
	 * <p>
	 * Deleted and closed projects are uninstalled in the pre change listener and not considered here.
	 * <p>
	 * It is a prerequisite that the project is accessible (exists and open)
	 * 
	 * @param projectDelta changes in a project specified as deltas since the last build
	 * @param project which has a specified delta
	 * @param activateBundleJob installs deactivated projects and installs, resolves and optionally starts
	 *          activated projects
	 * @return true if the specified project has been added as a pending project to the specified activate
	 *         bundle job. Otherwise false.
	 */
	private boolean handleCRUDOperation(IResourceDelta projectDelta, IProject project,
			ActivateBundleJob activateBundleJob) {

		if (!ProjectProperties.isPlugInProject(project)) {
			return false;
		}
		// A renamed project has already been uninstalled in pre change listener
		// Create, import and rename (only deactivated projects). Always in state uninstalled
		if ((projectDelta.getKind() & (IResourceDelta.ADDED)) != 0) {
			activateBundleJob.addPendingProject(project);
			return true;
		} else if ((projectDelta.getKind() & (IResourceDelta.CHANGED)) != 0) {
			// Move. Never in state uninstalled
			if ((projectDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
				Bundle bundle = bundleRegion.get(project);
				boolean unInstalled = (bundleCommand.getState(bundle) & (Bundle.UNINSTALLED)) != 0;
				if (!unInstalled && addMovedProject(project, activateBundleJob)) {
					return true;
				}
			}
			// Open. Always in state uninstalled
			if ((projectDelta.getFlags() & IResourceDelta.OPEN) != 0) {
				activateBundleJob.addPendingProject(project);
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a project is moved. If moved an uninstall job is scheduled and the moved project is added as a
	 * pending project to the specified activate bundle job
	 * 
	 * @param project to reactivate if moved
	 * @param activateBundleJob if project is moved it is added as a pending project to this job
	 * @return true if the project is moved. Otherwise false
	 */
	private boolean addMovedProject(IProject project, ActivateBundleJob activateBundleJob) {

		try {
			String projectLoaction = ProjectProperties.getProjectLocationIdentifier(project, true);
			String bundleLocation = bundleRegion.getBundleLocationIdentifier(project);
			// If path is different its a move (the path of the project description is changed)
			// The replaced flag is set on files being moved but not set on project level.
			// For all other modifications of the project description, use update bundle
			if (!projectLoaction.equals(bundleLocation) && ProjectProperties.isInstallableProject(project)) {
				UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName, project);
				BundleJobManager.addBundleJob(uninstallJob, 0);
				activateBundleJob.addPendingProject(project);
				return true;
			}
		} catch (ProjectLocationException e) {
			String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e);
			msg = UserMessage.getInstance().formatString("refresh_hint", project.getName());
			status.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, project, msg, null));
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return false;
	}

	/**
	 * Add activated projects that have been built to the bundle log
	 * @see JavaTimeBuilder
	 */
	private void traceBuilds() {
		if (InPlace.get().msgOpt().isBundleOperations()) {
			Map<IProject, IBundleStatus> builds = JavaTimeBuilder.getBuilds();
			if (!builds.isEmpty()) {
				IBundleStatus mStatus = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, Msg.BUILD_HEADER_TRACE);
				Iterator<Entry<IProject, IBundleStatus>> it = builds.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<IProject, IBundleStatus> entry = it.next();
					IBundleStatus status = entry.getValue();
					mStatus.add(status);
				}			
				JavaTimeBuilder.clearBuilds();
				InPlace.get().trace(mStatus);
			}
		} else {
			JavaTimeBuilder.clearBuilds();
		}
	}
}
