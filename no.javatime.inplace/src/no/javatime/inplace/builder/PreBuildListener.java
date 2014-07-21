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
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Listen to projects that are going to be built. This callback is also invoked when auto build is
 * switched off. Register a bundle as pending for build after workspace resources have been changed.
 * Listen to pre build notifications.
 */
public class PreBuildListener implements IResourceChangeListener {

	/**
	 * Register bundle as pending for build when receiving the pre build change event and auto build
	 * is off
	 * <p>
	 * Note that when auto build is off the pre build listener receives a notification with a build
	 * type of automatic build
	 */
	public void resourceChanged(IResourceChangeEvent event) {

		// Nothing to do in a deactivated workspace where all bundle projects are uninstalled
		if (!BundleProjectState.isWorkspaceNatureEnabled()) {
			return;
		}
		IResourceDelta rootDelta = event.getDelta();
		IResourceDelta[] projectDeltas = rootDelta.getAffectedChildren(IResourceDelta.ADDED
				| IResourceDelta.CHANGED, IResource.NONE);
		for (IResourceDelta projectDelta : projectDeltas) {
			IResource projectResource = projectDelta.getResource();
			if (projectResource.isAccessible() && (projectResource.getType() & (IResource.PROJECT)) != 0) {
				IProject project = projectResource.getProject();
				try {
					if (!ProjectProperties.isAutoBuilding() && BundleProjectState.isNatureEnabled(project)) {
						BundleJobManager.getTransition().addPending(project, Transition.BUILD);
					}
				} catch (InPlaceException e) {
					String msg = ExceptionMessage.getInstance().formatString("preparing_osgi_command",
							project.getName());
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e), StatusManager.LOG);
				}
			}
		}
	}
}
