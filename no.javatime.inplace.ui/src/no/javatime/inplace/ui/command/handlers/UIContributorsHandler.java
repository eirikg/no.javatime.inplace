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
package no.javatime.inplace.ui.command.handlers;

import java.util.Collection;
import java.util.Map;

import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Checked menu item allowing/disallowing activating bundles with ui contributions
 */
public class UIContributorsHandler extends AbstractHandler implements IElementUpdater {

	public static String commandId = "no.javatime.inplace.command.uicontributors";
	public static String stateId = "no.javatime.inplace.command.uicontributiors.toggleState";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		State state = event.getCommand().getState(stateId);
		CommandOptions cmdStore = Activator.getDefault().getPrefService();
		// Flip state value, update state and sync state with store
		Boolean stateVal = !(Boolean) state.getValue();
		state.setValue(stateVal);
		cmdStore.setIsAllowUIContributions(stateVal);
		try {
			cmdStore.flush();
		} catch (BackingStoreException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.PREFERENCE_FLUSH_EXCEPTION, e),
					StatusManager.LOG);
		}
		
		// Deactivate all activated projects that are involved in UI contributions when
		// UI contributions are not allowed
		DeactivateJob deactivateJob = null;
		if (!stateVal) {
			try {
				Collection<IProject> uiProjects = ProjectProperties.getUIContributors();
				deactivateJob = new DeactivateJob(DeactivateJob.deactivateJobName);
				for (IProject project : uiProjects) {
					if (BundleManager.getRegion().isActivated(project)) {
						deactivateJob.addPendingProject(project);
					}
				}
				if (deactivateJob.pendingProjects() > 0) {
					BundleMenuActivationHandler.jobHandler(deactivateJob);
				}
			} catch (final CircularReferenceException e) {
				Activator.getDisplay().asyncExec(new Runnable() {
					public void run() {
						String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
						BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
						msg = ExceptionMessage.getInstance().formatString("uicontibutors_not_disabled");
						multiStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
						multiStatus.add(e.getStatusList());					
						StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
					}
				});
			}
		}
		BundleMenuActivationHandler.updateBundleListPage(ProjectProperties.toJavaProjects(ProjectProperties.getInstallableProjects()));
		return null;
	}


	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {

		CommandOptions cmdStore = Activator.getDefault().getPrefService();
		element.setChecked(cmdStore.isAllowUIContributions());
	}

	@Override
	public boolean isEnabled() {
		ICommandService service = (ICommandService) Activator.getDefault().getWorkbench().getService(ICommandService.class);
		if (null != service) {
			// Get stored value and synch with state. 
			Command command = service.getCommand(commandId);
			CommandOptions cmdStore = Activator.getDefault().getPrefService();
			State state = command.getState(stateId);
			Boolean stateVal = (Boolean) state.getValue();
			Boolean storeVal = cmdStore.isAllowUIContributions();
			// Values may be different if stored  value has been changed elsewhere (e.g. preference page)
			// If different update checked menu element before the menu becomes visible by broadcasting the change 
			if (!stateVal.equals(storeVal)) {
				state.setValue(storeVal);
				service.refreshElements(command.getId(), null);
			}
		}
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
}
