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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.ui.statushandlers.StatusManager;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

/**
 * Schedules a bundle uninstall job for a project being closed, deleted or renamed. If this is the last
 * activated bundle project, deactivate workspace by uninstalling all bundles.
 */
public class PreChangeListener implements IResourceChangeListener {

	/**
	 * Handle pre resource events types on projects.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		if (!ProjectProperties.isProjectWorkspaceActivated()) {
			return;
		}
		final IResource resource = event.getResource();
		if (resource.isAccessible() && (resource.getType() & (IResource.PROJECT)) != 0) {
			final IProject project = resource.getProject();
			try {
				// Uninstall projects that are going to be deleted, closed or renamed
				UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName);
				// If this is the last activated project, deactivate workspace by uninstalling all projects
				if (ProjectProperties.isProjectActivated(project) && ProjectProperties.getActivatedProjects().size() == 1) {
					uninstallJob.addPendingProjects(ProjectProperties.getPlugInProjects());
				} else {
					// A special case is that an activated bundle may be in state uninstall when the activated duplicate
					// project has been imported or opened
					if (null == BundleManager.getRegion().get(project)) {
						return;
					}
					uninstallJob.addPendingProject(project);
				}
				uninstallJob.unregisterBundleProject(true);
				BundleManager.addBundleJob(uninstallJob, 0);
			} catch (ExtenderException e) {
				String hint = ExceptionMessage.getInstance().formatString("project_uninstall_error",
						project.getName());
				StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, hint, e),
						StatusManager.LOG);
			}
		}
	}
}
