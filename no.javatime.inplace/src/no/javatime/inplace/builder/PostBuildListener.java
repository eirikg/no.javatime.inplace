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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.intface.AddBundleProject;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dialogs.ResourceStateHandler;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Listen to projects that have been built. This callback is also invoked when auto build is
 * switched off. Schedules bundle jobs after a project is changed. This include created (create new,
 * import and open), renamed (rename and move), updated and projects being activated. Already
 * activated projects that have been changed are tagged with a pending bundle transition. The tag in
 * combination with the state of the bundle project determines the bundle job to schedule. Tagging
 * projects with pending transitions is left to others which usually are but not limited to bundle
 * jobs, resource listeners and the {@link JavaTimeBuilder}. Deactivated projects not tagged with a
 * pending transition are recognized based on it's resource delta and scheduled for bundle jobs
 * according to the executed CRUD operation on the project.
 * <p>
 * Deleted, closed and renamed projects are scheduled for uninstall in
 * {@linkplain no.javatime.inplace.builder.PreChangeListener PreChangeListener}. Deleted and closed
 * projects are not considered in this resource listener. If the workspace is deactivated, no jobs
 * are scheduled.
 * <p>
 * The following determines which jobs are scheduled for different CRU operations in an activated
 * workspace:
 * <ol>
 * <li>Create (create new, import, open and deactivated renamed projects):
 * <ol>
 * <li>New projects are delegated to the new project job for install and if activated, resolve
 * </ol>
 * <li>Rename and move
 * <ol>
 * <li>If a project is renamed, the project with the new name is started if it was activated before
 * the rename operation. Otherwise it is delegated to the new project job.
 * <li>A project that is moved is first uninstalled and then installed and in addition resolved and
 * started if it is activated.
 * </ol>
 * <li>Update (the project has been built)
 * <ol>
 * <li>Updated projects are scheduled for a bundle update job. Built projects with an empty resource
 * delta are not updated unless tagged with a pending update transition. Projects with a null delta
 * are usually tagged with a pending update transition and hence updated.
 * </ol>
 * <li>Activate and Deactivate:
 * <ol>
 * <li>Activated projects ({@link ActivateProjectJob}) are scheduled for a bundle activate job (
 * {@link ActivateBundleJob}) in state uninstalled and a bundle update job ({@link UpdateJob}) if in
 * state installed at arrival
 * <li>If a project to update has new requirements on UI plug-in(s), when UI plug-ins are not
 * allowed it is scheduled for deactivation instead of update
 * </ol>
 * <p>
 * For completeness note that deactivated providing projects of bundle projects being updated are
 * added as pending for activation in the bundle resolve handler and scheduled for activation in the
 * bundle job listener.
 */
public class PostBuildListener implements IResourceChangeListener {

	private BundleRegion bundleRegion;
	private BundleTransition bundleTransition;
	private BundleProjectCandidates bundleProjectCandidates;
	private BundleExecutorEventManager bundleExecutorEventmanager;
	private CommandOptions commandOptions;
	final private ActivateProject projectActivator;

	public PostBuildListener() {

		try {
			bundleRegion = Activator.getBundleRegionService();
			bundleTransition = Activator.getBundleTransitionService();
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
			bundleExecutorEventmanager = Activator.getBundleExecutorEventService();
			commandOptions = Activator.getCommandOptionsService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		projectActivator = new ActivateProjectJob();
	}

	/**
	 * Schedules bundle jobs for new and modified projects. Removed and closed projects are handled in
	 * the {@link PreChangeListener}
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		try {
			// Nothing to do in a deactivated workspace where all bundle projects are uninstalled
			if (!projectActivator.isProjectWorkspaceActivated()) {
				return;
			}
			// Updates a project if it is activated in an activated workspace
			// and after a build where the project is pending for update and auto build is on
			final Update update = new UpdateJob();
			// After projects are activated in a deactivated workspace and when moved in an active
			// workspace
			final ActivateBundle activateBundle = new ActivateBundleJob();
			// Project with new requirements on UI plug-in(s), when UI plug-ins are not allowed
			final Deactivate deactivate = new DeactivateJob();
			// Project probably moved or needs a reactivation for some reason
			final Uninstall uninstall = new UninstallJob();
			// When a project is activated and in state uninstalled or deactivated and in state
			// uninstalled
			final Install install = new InstallJob();
			// Add new (opened, imported, created) and renamed projects to the workspace region
			// Deactivated bundle projects are installed and activated bundle projects are activated
			final AddBundleProject addBundleProject = new AddBundleProjectJob();

			final int buildType = event.getBuildKind();
			final IResourceDelta rootDelta = event.getDelta();
			// Ignore removed (deleted or closed) projects uninstalled in the pre-change listener
			IResourceDelta[] resourceDeltas = (null != rootDelta ? rootDelta.getAffectedChildren(
					IResourceDelta.ADDED | IResourceDelta.CHANGED, IResource.NONE) : null);
			// A null resource delta implies an unspecified change or a full build. An empty resource
			// delta implies no change since last build, but resources may have pending transitions
			if (null == resourceDeltas || resourceDeltas.length == 0) {
				for (IProject project : bundleProjectCandidates.getInstallable()) {
					try {
						removePendingBuildTransition(buildType, project);
						if (null != bundleTransition.getPendingTransitions(project)) {
							handlePendingTransition(project, activateBundle, update, deactivate, uninstall,
									install);
						}
					} catch (InPlaceException e) {
						String msg = ExceptionMessage.getInstance().formatString("schedule_bundle_jobs",
								project.getName());
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
				}
			} else {
				// Cycle all built projects with a delta and add them to the appropriate bundle jobs
				// according to type of transition and project CRUD operation
				for (IResourceDelta projectDelta : resourceDeltas) {
					IResource projectResource = projectDelta.getResource();
					if (projectResource.isAccessible()
							&& (projectResource.getType() & (IResource.PROJECT)) != 0) {
						IProject project = projectResource.getProject();
						try {
							if (Category.DEBUG && Category.getState(Category.listeners))
								ProjectChangeListener.traceDeltaKind(event, projectDelta, project);
							removePendingBuildTransition(buildType, project);
							if (!handlePendingTransition(project, activateBundle, update, deactivate, uninstall,
									install)) {
								handleCRUDOperation(projectDelta, project, activateBundle, addBundleProject);
							}
						} catch (InPlaceException e) {
							String msg = ExceptionMessage.getInstance().formatString("schedule_bundle_jobs",
									project.getName());
							StatusManager.getManager().handle(
									new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
									StatusManager.LOG);
						} catch (ProjectLocationException e) {
							String msg = ErrorMessage.getInstance().formatString("project_location",
									project.getName());
							IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,
									e);
							msg = NLS.bind(Msg.REFRESH_HINT_INFO, project.getName());
							status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, project, msg, null));
							StatusManager.getManager().handle(status, StatusManager.LOG);
						}
					}
				}
			}
			// Post build listener is not always called with all activated projects that have been built
			ActivateBundle postActivateBundle = addBuiltProjects(update, activateBundle);
			execute(addBundleProject, uninstall, install, deactivate, activateBundle, update,
					postActivateBundle);
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Removes the the pending {@link Transition#BUILD build} transition from the specified project
	 * 
	 * @param buildType determines if the pending build transition should be removed. Has to be one of
	 * incremental build, full build, clean build or auto build
	 * @param project to remove the pending build transition from
	 * @return true if the pending build transition is cleared from the specified project. The
	 * transition does not have to be present before the build transition is cleared, Otherwise false,
	 */
	private boolean removePendingBuildTransition(final int buildType, final IProject project) {
		if ((bundleProjectCandidates.isAutoBuilding()
				|| buildType == IncrementalProjectBuilder.INCREMENTAL_BUILD
				|| buildType == IncrementalProjectBuilder.FULL_BUILD || buildType == IncrementalProjectBuilder.CLEAN_BUILD)) {
			// Build is no longer pending
			bundleTransition.removePending(project, Transition.BUILD);
			return true;
		}
		return false;
	}

	/**
	 * Add the specified project as pending to one of the specified jobs if the project has one or
	 * more pending transitions that equals or is a combination of one of
	 * {@link Transition#ACTIVATE_BUNDLE}, {@link Transition#UPDATE}, {@link Transition#UNINSTALL} or
	 * {@link Transition#DEACTIVATE}.
	 * <p>
	 * The project is only added to the update job if auto update is on or
	 * {@link Transition#UPDATE_ON_ACTIVATE} is set on the project in addition to the update
	 * transition.
	 * 
	 * @param project to add to one of the specified bundle jobs as a pending project
	 * @param activateBundle installs deactivated projects and installs, resolves and optionally
	 * starts activated projects
	 * @param update updates a project
	 * @param deactivate deactivates a project by removing the JavaTime nature
	 * @param uninstall uninstalls a bundle project
	 * @param install innstalls a bundle project
	 * @return true if the specified project has been added as a pending project to one of the
	 * specified bundle jobs.Otherwise false
	 * @throws InPlaceException if the specified project is null or open but does not exist
	 * @throws ExtenderException If failing to get the project candidates service
	 */
	private boolean handlePendingTransition(final IProject project,
			final ActivateBundle activateBundle, final Update update, final Deactivate deactivate,
			final Uninstall uninstall, final Install install) throws InPlaceException, ExtenderException {

		boolean ishandled = false;

		// Update activated modified projects. Deactivated projects are usually not updated but may be
		if (bundleTransition.containsPending(project, Transition.UPDATE, Boolean.FALSE)) {
			// If this project is part of an activate process and auto update is off, the project is
			// tagged with an update on activate transition and should be updated
			if (commandOptions.isUpdateOnBuild()
					|| bundleTransition.containsPending(project, Transition.UPDATE_ON_ACTIVATE, Boolean.TRUE)) {
				// Deactivated providing bundle projects to this project are identified and marked as
				// pending for activation in the resolver hook and scheduled in the bundle job listener
				UpdateScheduler.addProjectToUpdateJob(project, update);
				ishandled = true;
			}
		}
		// Usually set when a project is activated and in state uninstalled or deactivated and in state
		// uninstalled in an activated workspace
		if (bundleTransition.containsPending(project, Transition.INSTALL, Boolean.TRUE)) {
			install.addPendingProject(project);
			ishandled = true;
		}
		if (bundleTransition.containsPending(project, Transition.ACTIVATE_BUNDLE, Boolean.TRUE)) {
			// TODO Check this check
			if (bundleProjectCandidates.isInstallable(project)) {
				activateBundle.addPendingProject(project);
				ishandled = true;
			}
		}
		// Most commonly used when an activated project has new requirements on UI plug-in(s), when UI
		// plug-ins
		// are not allowed
		if (bundleTransition.containsPending(project, Transition.DEACTIVATE, Boolean.TRUE)) {
			deactivate.addPendingProject(project);
			ishandled = true;
		}
		// Project probably moved or needs a reactivation for some reason
		if (bundleTransition.containsPending(project, Transition.UNINSTALL, Boolean.TRUE)) {
			uninstall.addPendingProject(project);
			ishandled = true;
		}
		return ishandled;
	}

	/**
	 * Schedules an uninstall job and than an activate job for the specified bundle project when it
	 * has been moved. Adds imported, opened, created and renamed projects as pending to the specified
	 * new project job.
	 * <p>
	 * Deactivated bundle projects being renamed, are uninstalled in the pre change listener and added
	 * as a pending project to the specified new project job. Deleted and closed projects are
	 * uninstalled in the pre change listener and not considered here.
	 * <p>
	 * Adds a pending {@code Transition.NEW_PROJECT} and delegates the activation and dependency
	 * closure handling of imported, opened, created and renamed projects to the to new project job.
	 * 
	 * Activated renamed bundle projects have a pending activate transition and are scheduled for an
	 * activate bundle job in
	 * {@link #handlePendingTransition(IProject, ActivateBundleJob, UpdateJob, DeactivateJob, UninstallJob, InstallJob)
	 * handlePendingTransition}.
	 * <p>
	 * A project that has been moved is always at least in state installed. A moved project is first
	 * scheduled for an uninstall job and than added as a pending project to the activate bundle job
	 * 
	 * @param projectDelta changes in a project specified as deltas since the last build
	 * @param project The project has a specified delta which is the basis for which bundle operation
	 * to perform
	 * @param activateBundle add moved projects to this activate job
	 * @param addBundleProject add imported, created, opened and renamed projects to this job
	 * @return true if the specified project has been added as a pending project to the specified
	 * activate bundle and/or new project job. Otherwise false.
	 * @throws ExtenderException If failing to get the bundle command service
	 * @throws ProjectLocationException If failing to get the location of the specified project
	 * @throws InPlaceException if missing permission to get project or bundle location of the
	 * specified project
	 */
	private boolean handleCRUDOperation(final IResourceDelta projectDelta, final IProject project,
			final ActivateBundle activateBundle, final AddBundleProject addBundleProject)
			throws ExtenderException, ProjectLocationException, InPlaceException {

		if (!bundleProjectCandidates.isBundleProject(project)) {
			return false;
		}
		// A renamed project has already been uninstalled in pre change listener
		// Create (always deactivated), import and rename. All always in state uninstalled
		if ((projectDelta.getKind() & (IResourceDelta.ADDED)) != 0) {
			bundleTransition.addPending(project, Transition.NEW_PROJECT);
			addBundleProject.addPendingProject(project);
			return true;
		} else if ((projectDelta.getKind() & (IResourceDelta.CHANGED)) != 0) {
			// Move. Never in state uninstalled
			if ((projectDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
				Bundle bundle = bundleRegion.getBundle(project);
				BundleCommand bundleCommand = Activator.getBundleCommandService();
				boolean unInstalled = (bundleCommand.getState(bundle) & (Bundle.UNINSTALLED)) != 0;
				if (!unInstalled && addMovedProject(project, activateBundle)) {
					return true;
				}
			}
			// Open. Always in state uninstalled
			if ((projectDelta.getFlags() & IResourceDelta.OPEN) != 0) {
				bundleTransition.addPending(project, Transition.NEW_PROJECT);
				addBundleProject.addPendingProject(project);
				return true;
			}
		}
		return false;
	}

	/**
	 * If any, add bundle projects that are:
	 * <ol>
	 * <li>Built and marked for update by the java time builder but not received by this post build
	 * listener
	 * <li>Possible no longer duplicates due to changes in the symbolic key of bundles to update
	 * </ol>
	 * 
	 * @param update Add new bundles to update or any installed bundle that is a duplicate of any of
	 * the bundles to update
	 * @param activateBundle Remove bundles from this job that are returned
	 * @return Uninstalled duplicate bundles that are duplicates of the bundles to update or null if
	 * no uninstalled duplicates exist compared to the bundles to update
	 * @throws ExtenderException If failing to get region, transition and/or the candidate service
	 */
	private ActivateBundle addBuiltProjects(final Update update, final ActivateBundle activateBundle)
			throws ExtenderException {

		ActivateBundle postActivateBundle = null;

		if (commandOptions.isUpdateOnBuild()) {
			Collection<IProject> activatedProjects = bundleRegion.getActivatedProjects();
			// Get all projects identified for update by the java time builder
			Collection<IProject> projectsToUpdate = bundleTransition.getPendingProjects(
					activatedProjects, Transition.UPDATE);
			// This is the difference between projects recognized by the java time builder and those
			// actually sent to the post build listener
			projectsToUpdate.removeAll(update.getPendingProjects());
			if (projectsToUpdate.size() > 0) {
				for (IProject projectToUpdate : projectsToUpdate) {
					UpdateScheduler.addProjectToUpdateJob(projectToUpdate, update);
				}
			}
			// Activate bundles that are no longer duplicates
			if (update.pendingProjects() > 0) {
				postActivateBundle = UpdateScheduler.resolveduplicates(activateBundle, update,
						bundleRegion, bundleTransition, bundleProjectCandidates);
			}
		}
		return postActivateBundle;
	}

	/**
	 * Wait on builder before scheduling jobs with pending projects.
	 * 
	 * @param addBundleProject New (import, open, create) and renamed projects
	 * @param uninstall Moved projects and projects that needs to be ractivated
	 * @param install Activated projects and in state uninstalled or deactivated and in state
	 * uninstalled in an activated workspace
	 * @param deactivate Deactivated projects
	 * @param activateBundle Activated projects in a deactivated workspace and when moved in an active
	 * workspace
	 * @param update Modified activated projects
	 * @param postActivateBundle Activate bundles that are no longer duplicates
	 */
	private void execute(final AddBundleProject addBundleProject, final Uninstall uninstall,
			final Install install, final Deactivate deactivate, final ActivateBundle activateBundle,
			final Update update, final ActivateBundle postActivateBundle) {

		// Run in a separate thread to avoid builder deadlock
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			executor.execute(new Runnable() {

				private boolean hasBuild = false;

				/**
				 * Wait on builder to finish before adding jobs for execution
				 */
				@Override
				public void run() {
					new ResourceStateHandler().waitOnBuilder(false);
					add(addBundleProject);
					add(uninstall);
					add(install);
					add(deactivate);
					add(activateBundle);
					add(update);
					add(postActivateBundle);
					if (hasBuild) {
						JavaTimeBuilder.postBuild();
					}
				}

				/**
				 * Schedules the specified job for execution
				 * 
				 * @param bundleExecutor job to schedule and execute
				 */
				private boolean add(BundleExecutor bundleExecutor) {

					if (null != bundleExecutor && bundleExecutor.hasPendingProjects()) {
						bundleExecutorEventmanager.add(bundleExecutor);
						return hasBuild = true;
					}
					return false;
				}
			});
		} catch (RejectedExecutionException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			// This is part of the building process so don't wait on the executed/submitted task
			// Waiting will always cause termination on timeout
			executor.shutdown();
		}
	}

	/**
	 * Check if a project is moved. If moved an uninstall job is scheduled and the moved project is
	 * added as a pending project to the specified activate bundle job
	 * 
	 * @param project to reactivate if moved
	 * @param activateBundle if project is moved it is added as a pending project to this job
	 * @return true if the project is moved. Otherwise false
	 * @throws ProjectLocationException If failing to get the location of the specified project
	 * @throws ProjectLocationException if failing to get the project or bundle location of the
	 * specified project
	 * @throws InPlaceException if permissions to get project or bundle location of the specified
	 * project
	 */
	private boolean addMovedProject(IProject project, ActivateBundle activateBundle)
			throws ProjectLocationException, InPlaceException {

		String projectLoaction = bundleRegion.getProjectLocationIdentifier(project, null);
		String bundleLocation = bundleRegion.getBundleLocationIdentifier(project);
		// If path is different its a move (the path of the project description is changed)
		// The replaced flag is set on files being moved but not set on project level.
		// For all other modifications of the project description, use update bundle
		if (!projectLoaction.equals(bundleLocation) && bundleProjectCandidates.isInstallable(project)) {
			Uninstall uninstall = new UninstallJob(UninstallJob.uninstallJobName, project);
			bundleExecutorEventmanager.add(uninstall, 0);
			activateBundle.addPendingProject(project);
			return true;
		}
		return false;
	}
}
