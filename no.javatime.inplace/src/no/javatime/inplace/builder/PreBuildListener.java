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
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Listen to pre build notifications. Remove transition errors from and add pending build
 * transitions to bundle projects.
 * <p>
 * Error transitions are removed from all bundle projects when the workspace is deactivated.
 * <p>
 * Pending build transitions are added to all projects in an activated workspace not being opened or
 * moved. Pending build state is preserved for closed and for source location of moved projects.
 * <p>
 * Note that this callback is also invoked when auto build is switched off.
 */
public class PreBuildListener implements IResourceChangeListener, BundleTransitionEventListener {

	private BundleTransition bundleTransition;
	private BundleRegion bundleRegion;
	private BundleProjectCandidates bundlProjecteCandidates;
	final private ActivateProject projectActivator;

	public PreBuildListener() {

		try {
			bundlProjecteCandidates = Activator.getBundleProjectCandidatesService();
			bundleTransition = Activator.getBundleTransitionService();
			bundleRegion = Activator.getBundleRegionService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		projectActivator = new ActivateProjectJob();
	}

	/**
	 * Register bundle as pending for build when receiving the pre build change event
	 * <p>
	 * Note that when auto build is off the pre build listener receives a notification with a build
	 * type of automatic build
	 */
	public void resourceChanged(IResourceChangeEvent event) {

		final int buildKind = event.getBuildKind();
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (null == workbench || workbench.isClosing()) {
			return;
		}
		JavaTimeBuilder.preBuild();
		if (buildKind == 0) {
			return;
		}
		boolean isWorkspaceActivated = projectActivator.isProjectWorkspaceActivated();
		if (!isWorkspaceActivated) {
		}
		IResourceDelta rootDelta = event.getDelta();
		IResourceDelta[] projectDeltas = (null != rootDelta ? rootDelta.getAffectedChildren(
				IResourceDelta.ADDED | IResourceDelta.CHANGED, IResource.NONE) : null);
		if (null != projectDeltas) {
			for (IResourceDelta projectDelta : projectDeltas) {
				IResource projectResource = projectDelta.getResource();
				if (projectResource.isAccessible()
						&& (projectResource.getType() & (IResource.PROJECT)) != 0) {
					IProject project = projectResource.getProject();
					try {
						if (!isWorkspaceActivated) {
							// The error should be visible in a deactivated workspace until the project is built
							bundleTransition.clearTransitionError(project);
						} 
						try {
							// Add a pending build to the preference store to retain pending build between sessions
							// A request for auto build when auto build is switched off
							if (!bundlProjecteCandidates.isAutoBuilding()) {
								if (buildKind == IncrementalProjectBuilder.AUTO_BUILD) {
								StatePersistParticipant.savePendingBuildTransition(
										StatePersistParticipant.getSessionPreferences(), project, true);
								}
							}
						} catch (IllegalStateException | BackingStoreException e) {
							String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
							StatusManager.getManager().handle(
									new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
									StatusManager.LOG);
						}
						// Preserve build pending state for projects being opened or moved
						if (!isOpenOrMove(projectDelta, project)) {
							bundleTransition.addPending(project, Transition.BUILD);
						} else {
							BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
									Transition.BUILD));
						}
					} catch (ExtenderException | InPlaceException e) {
						String msg = ExceptionMessage.getInstance().formatString("preparing_osgi_command",
								project.getName());
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
				}
			}
		}
	}

	/**
	 * Check if the specified project is being opened or moved
	 * 
	 * @param projectDelta changes in a project specified as deltas since the last build
	 * @param project The project has a specified delta which is the basis for which bundle operation
	 * to perform
	 * @return True if the project is being opened or moved. Otherwise false
	 */
	private boolean isOpenOrMove(final IResourceDelta projectDelta, final IProject project) {

		if ((projectDelta.getKind() & (IResourceDelta.CHANGED)) != 0) {
			if ((projectDelta.getFlags() & IResourceDelta.OPEN) != 0) {
				return true;
			} else if ((projectDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
				String projectLoaction = bundleRegion.getProjectLocationIdentifier(project, null);
				String bundleLocation = bundleRegion.getBundleLocationIdentifier(project);
				// If path is different its a move (the path of the project description is changed)
				// The replaced flag is set on files being moved but not set on project level.
				if (!projectLoaction.equals(bundleLocation)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void bundleTransitionChanged(BundleTransitionEvent event) {
		Transition transition = event.getTransition();
		if (transition == Transition.DEACTIVATE) {
			// bundleTransition.removePending(bundleRegion.getProjects(), Transition.BUILD);	
		}
	}

}
