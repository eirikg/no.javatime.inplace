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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dialogs.SaveProjectHandler;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * Listen to and uninstall projects being closed, deleted and renamed.
 * <p>
 * It is possible to delete or close (remove) multiple projects in one operation from the UI and
 * this resource listener is called once for each project being removed. Activated requiring
 * projects to an activated project being removed are deactivated. One job is scheduled for all
 * projects being removed.
 * 
 * <p>
 * It is only possible to rename one project at the time from the UI. When a project is renamed an
 * uninstall job is scheduled causing all requiring bundles to be uninstalled as well. The project
 * closure is scheduled for install and resolve again after build of the renamed project. This job is
 * typically scheduled by the post build listener.
 * 
 * @see RemoveBundleProjectJob
 */
public class PreChangeListener implements IResourceChangeListener {

	/**
	 * Handle pre-resource events for project delete, close and rename.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		BundleRegion bundleRegion = InPlace.getBundleRegionService();
		// Do not return if waiting for additional projects to uninstall
		// May occur if the workspace becomes deactivated while waiting for new projects to remove
		if (!NatureJob.isWorkspaceNatureEnabled() && 
				!(SaveProjectHandler.getWaitingBundleJob() instanceof RemoveBundleProjectJob)) {
			return;
		}
		final IResource resource = event.getResource();
		if (null != resource && resource.isAccessible()
				&& (resource.getType() & (IResource.PROJECT)) != 0) {
			final IProject project = resource.getProject();
			Bundle bundle = bundleRegion.getBundle(project);
			if (null == bundle) {
				return;
			}
			if (InPlace.getBundleTransitionService().containsPending(bundle, Transition.RENAME_PROJECT,
					true)) {
				// The renamed bundle and requiring bundles are scheduled for install again by the post
				// build listener with the new name after the bundle projects have been built
				UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName, project);
				BundleJobManager.addBundleJob(uninstallJob, 0);
			} else {
				// Schedule the removed project for uninstall
				scheduleRemoveBundleProjects(project);
			}
		}
	}

	/**
	 * Add the specified removed (deleted or closed) project for uninstall and return the scheduled job
	 * handling removal of projects. If there exists such a bundle job in state waiting add the
	 * specified project and return the job, otherwise add the project and schedule and return a new
	 * instance of the job.
	 * <p>
	 * The removal (uninstall) bundle job is typically waiting on the Eclipse removal job to finish
	 * before running. This gives this resource listener a chance to add the project to remove to the
	 * same bundle job as previous added projects to remove due to the waiting state of the bundle job.
	 * 
	 * @param project this project is added to the returned job
	 * @return a scheduled bundle job handling removal of bundle projects. Never null.
	 */
	private BundleJob scheduleRemoveBundleProjects(IProject project) {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.WAITING && job instanceof RemoveBundleProjectJob) {
				BundleJob bj = (BundleJob) job;
				bj.addPendingProject(project);
				return bj;
			}
		}
		RemoveBundleProjectJob rj = new RemoveBundleProjectJob(RemoveBundleProjectJob.removeBundleProjectName, project);
		BundleJobManager.addBundleJob(rj, 0);
		return rj;
	}
}
