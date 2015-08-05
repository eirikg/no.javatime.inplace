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
package no.javatime.inplace;

import java.util.Collection;

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Access previous saved state so change events will be created for all changes that have occurred
 * since the last save
 */
public class WorkspaceSaveParticipant implements ISaveParticipant {

	long startTime;

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {

		startTime = System.currentTimeMillis();
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		context.needDelta();
	}

	@Override
	public void doneSaving(ISaveContext context) {

		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (null != workbench && !workbench.isClosing() && context.getKind() == ISaveContext.FULL_SAVE) {
				MessageOptions messageOptions = Activator.getMessageOptionsService();
				if (messageOptions.isBundleOperations()) {
					String msg = NLS.bind(Msg.SAVE_WORKSPACE_INFO,
							(System.currentTimeMillis() - startTime));
					IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg);
					Activator.log(status);
				}
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	@Override
	public void rollback(ISaveContext context) {
	}

	/**
	 * Saves bundle state of activated bundle projects. The saved state information is used when the
	 * IDE starts up. The default is to start bundle projects at startup.
	 * <p>
	 * There are two modes for activated bundle projects:
	 * <ol>
	 * <li>In recovery mode all bundle projects are stored with state {@code Bundle.RESOLVED}
	 * <li>In normal mode state information is only stored if the bundle is in state
	 * {@code Bundle.RESOLVED}
	 * </ol>
	 * Recovery mode is used if an uncontrolled shutdown of the IDE occurs. In this mode all activated
	 * bundle projects are resolved but not started at startup of the IDE. In normal mode, activated
	 * projects with no state information are started and bundle projects with a
	 * {@code Bundle.RESOLVED} state are resolved. In an activated workspace deactivated bundle
	 * projects are installed.
	 * <p>
	 * Recovery mode should be applied when the IDE is running and the workspace is activated. Normal
	 * mode should be applied right before shutdown.
	 * <p>
	 * The recovery settings should be updated when a bundle project is activated or deactivated.
	 * 
	 * @param flush If true flush the store after saving state information.
	 * @param recoveryMode Save state information according to recovery mode if true, and according to
	 * normal mode if false.
	 */
	public static void saveBundleStateSettings(boolean flush, boolean recoveryMode) {

		try {
			ActivateProject activateProjects = new ActivateProjectJob();
			IEclipsePreferences prefs = Activator.getEclipsePreferenceStore();
			if (null == prefs) {
				String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
				return;
			}
			// Clear store before saving
			try {
				prefs.clear();
			} catch (BackingStoreException e) {
				String msg = WarnMessage.getInstance().formatString("failed_clearing_preference_store");
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
				return; // Use existing values
			}
			// Save in recovery or normal mode
			if (activateProjects.isProjectWorkspaceActivated()) {
				BundleRegion region = Activator.getBundleRegionService();
				Collection<IProject> natureEnabled = activateProjects.getActivatedProjects();
				for (IProject project : natureEnabled) {
					try {
						String symbolicKey = Activator.getBundleRegionService().getSymbolicKey(null, project);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						if (recoveryMode) {
							prefs.putInt(symbolicKey, Bundle.RESOLVED);
						} else {
							// Normal mode
							Bundle bundle = region.getBundle(project);
							if (null != bundle && (bundle.getState() & (Bundle.RESOLVED)) != 0) {
								prefs.putInt(symbolicKey, Bundle.RESOLVED);
							}
						}
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, msg, null),
								StatusManager.LOG);
					}
				}
			} else {
				if (!recoveryMode) {
					saveBundleTransitionSettings(prefs);
				}
			}
			try {
				if (flush) {
					prefs.flush();
				}
			} catch (BackingStoreException | IllegalStateException e) {
				String msg = WarnMessage.getInstance().formatString("failed_flushing_preference_store");
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Save state of a deactivated project to preference store based on their last transition. This is
	 * used to set the initial transition of bundles at startup.
	 * 
	 * @param prefs The preference store
	 */
	private static void saveBundleTransitionSettings(IEclipsePreferences prefs)
			throws ExtenderException {

		BundleProjectCandidates bundleProject = Activator.getBundleProjectCandidatesService();
		Collection<IProject> projects = bundleProject.getBundleProjects();
		for (IProject project : projects) {
			try {
				Transition transition = Activator.getBundleTransitionService().getTransition(project);
				if (transition == Transition.REFRESH || transition == Transition.UNINSTALL) {
					String symbolicKey = Activator.getBundleRegionService().getSymbolicKey(null, project);
					if (symbolicKey.isEmpty()) {
						continue;
					}
					prefs.putInt(symbolicKey, transition.ordinal());
				}
			} catch (ProjectLocationException e) {
				// Ignore. Will be defined as no transition when loaded again
			} catch (IllegalStateException e) {
				String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
			}
		}
	}
}
