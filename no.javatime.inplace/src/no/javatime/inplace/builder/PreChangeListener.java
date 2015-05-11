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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.intface.RemoveBundleProject;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.dialogs.ResourceStateHandler;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Listen to and uninstall projects being removed (closed, deleted) and renamed.
 * <p>
 * It is possible to remove multiple projects in one operation from the UI and this resource
 * listener is called once for each project being removed. Activated requiring projects to activated
 * projects being removed are deactivated.
 * <p>
 * This listener tries to collect as many removed projects as possible in one job before the job is
 * run for removal of the projects.
 * 
 * <p>
 * It is only possible to rename one project at the time from the UI. When a project is renamed an
 * uninstall job is scheduled causing all requiring bundles to be uninstalled as well. The project
 * closure is scheduled for install and resolve again after build of the renamed project. This job
 * is typically scheduled by the post build listener.
 * 
 * @see RemoveBundleProjectJob
 */
public class PreChangeListener implements IResourceChangeListener {

	final private ActivateProject projectActivation = new ActivateProjectJob();

	/**
	 * Handle pre-resource events for project delete, close and rename.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		
		
		// Do not return if waiting for additional projects to uninstall
		// May occur if the workspace becomes deactivated while waiting for new projects to remove
		if (!projectActivation.isProjectWorkspaceActivated()
				&& !(ResourceStateHandler.getWaitingBundleJob() instanceof RemoveBundleProject)) {
			return;
		}
		final IResource resource = event.getResource();
		if (null != resource && resource.isAccessible()
				&& (resource.getType() & (IResource.PROJECT)) != 0) {
			final IProject project = resource.getProject();
			try {
				BundleRegion bundleRegion = Activator.getBundleRegionService();
				Bundle bundle = bundleRegion.getBundle(project);
				if (null == bundle) {
					return;
				}
				BundleTransition transition = Activator.getBundleTransitionService();
				if (transition.containsPending(bundle, Transition.RENAME_PROJECT, true)) {
					// The renamed bundle and requiring bundles are scheduled for install again by the post
					// build listener with the new name after the bundle projects have been built
					Uninstall uninstall = new UninstallJob(UninstallJob.uninstallJobName, project);
					// A new project entry with a new name is created by the rename operation
					// Unregister the project with the original name
					uninstall.setUnregister(true);
					Activator.getBundleExecutorEventService().add(uninstall);
				} else {
					// Schedule the removed project for uninstall
					transition.addPending(project, Transition.REMOVE_PROJECT);
					scheduleRemoveBundleProject(project);
				}
			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}

	/**
	 * Add the specified removed (deleted or closed) project for uninstall and return the scheduled
	 * job handling removal of projects. If there exists such a bundle job in state waiting add the
	 * specified project and return the job, otherwise add the project to the job, schedule and return
	 * a new instance of the job.
	 * <p>
	 * The removal (uninstall) bundle job is typically waiting on the Eclipse removal job to finish
	 * before running. This gives this resource listener a chance to add the project to remove to the
	 * same bundle job as previous added projects to remove due to the waiting state of the bundle
	 * job.
	 * <p>
	 * It is not guaranteed that all projects that have been removed at the same time from the UI is
	 * scheduled in one job.
	 * 
	 * @param project this project is added to the returned job
	 * @return a scheduled bundle job handling removal of bundle projects. Never null.
	 */
	private BundleExecutor scheduleRemoveBundleProject(IProject project) {

		// If any, add this project to an existing and waiting remove bundle job
		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			// Add this project to the waiting remove bundle project executor job
			if (job.getState() == Job.WAITING && job instanceof RemoveBundleProjectJob) {
				BundleExecutor bundleExecutor = (BundleExecutor) job;
				bundleExecutor.addPendingProject(project);
				return bundleExecutor;
			}
		}
		// A new remove bundle project is created the first time a project is added for removal
		// and if needed a new one after the job has finished. A new job will wait for the close or
		// remove project(s) to finish so it should suffice with only one remove bundle project job
		final RemoveBundleProject removeBundleProject = new RemoveBundleProjectJob(
				RemoveBundleProjectJob.removeBundleProjectName, project);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					new ResourceStateHandler().waitOnBuilder(true);
					// Put in waiting queue until delete or close project (eclipse) job finish
					Activator.getBundleExecutorEventService().add(removeBundleProject);
				}
			});
		} catch (RejectedExecutionException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			// Don't wait for for the executed/submitted task
			executor.shutdown();
		}
		return removeBundleProject;
	}
}
