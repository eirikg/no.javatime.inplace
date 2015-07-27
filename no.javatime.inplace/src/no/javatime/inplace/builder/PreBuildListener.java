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

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Listen to projects that are going to be built. This callback is also invoked when auto build is
 * switched off. Register opened, created and imported projects and add a pending build bundleTransition
 * after workspace resources have been changed. Listen to pre build notifications. Also clears any
 * errors (not build errors) on uninstalled bundle projects.
 */
public class PreBuildListener implements IResourceChangeListener {

	private BundleTransition bundleTransition;
	private BundleProjectCandidates bundleProjectCandidates;
	final private ActivateProject projectActivator;

	public PreBuildListener() {

		try {
			bundleTransition = Activator.getBundleTransitionService();
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		projectActivator = new ActivateProjectJob();
	}

	/**
	 * Register bundle as pending for build when receiving the pre build change event and auto build
	 * is off
	 * <p>
	 * Note that when auto build is off the pre build listener receives a notification with a build
	 * type of automatic build
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		
		final int buildKind = event.getBuildKind();

		// Clean build calls pre build listener twice. The first call is of kind {@code CLEAN_BUILD]
		if (buildKind == IncrementalProjectBuilder.CLEAN_BUILD || buildKind == 0) {
			return;
		}
		JavaTimeBuilder.preBuild();
		IResourceDelta rootDelta = event.getDelta();
		IResourceDelta[] projectDeltas	=	(null != rootDelta ? rootDelta.getAffectedChildren(IResourceDelta.ADDED
					| IResourceDelta.CHANGED, IResource.NONE) : null);
		boolean isWSNatureEnabled = projectActivator.isProjectWorkspaceActivated();
		if (null != projectDeltas) {
			for (IResourceDelta projectDelta : projectDeltas) {
				IResource projectResource = projectDelta.getResource();
				if (projectResource.isAccessible() && (projectResource.getType() & (IResource.PROJECT)) != 0) {
					IProject project = projectResource.getProject();
					try {
						if (!isWSNatureEnabled) {
							// Clear any errors detected from last activation that caused the workspace to be
							// deactivated. The error should be visible in a deactivated workspace until the project
							// is built
							bundleTransition.clearTransitionError(project);
						} else {
							if (projectActivator.isProjectActivated(project)) {
								if (!bundleProjectCandidates.isAutoBuilding()) {
									bundleTransition.addPending(project, Transition.BUILD);
								} else {
									BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
											Transition.BUILD));
								}
							}
						}
					} catch (InPlaceException e) {
						String msg = ExceptionMessage.getInstance().formatString("preparing_osgi_command",
								project.getName());
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
					}
				}
			}
		}
	}
}
