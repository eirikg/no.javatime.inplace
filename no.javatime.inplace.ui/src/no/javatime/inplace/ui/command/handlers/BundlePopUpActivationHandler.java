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

import java.util.Collections;
import java.util.Map;

import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.command.contributions.BundlePopUpCommandsContributionItems;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Handles pop-up menu from java project in package explorer and bundle view.
 * 
 *@see no.javatime.inplace.ui.command.contributions.BundlePopUpCommandsContributionItems
 */
public class BundlePopUpActivationHandler extends BundleMenuActivationHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String parameterId = event.getParameter(BundlePopUpCommandsContributionItems.menuIdPar);
		if (null == parameterId) {
			return null;
		}
		// Get project from selected item
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object object = selection.getFirstElement();		
		IProject project = null;
		if (null != object) {
			project = (IProject) Platform.getAdapterManager().getAdapter(object, IProject.class);
			if (null == project) {
				String msg = WarnMessage.getInstance().formatString("no_selection_for_command", parameterId);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
						StatusManager.LOG);
				return null;
			}
		} else {
			// No new selection from handler util. Get the most recent selection from the active page
			IJavaProject jProject = getSelectedJavaProject();
			if (null == jProject) {
				String msg = WarnMessage.getInstance().formatString("no_selection_for_command", parameterId);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
						StatusManager.LOG);
				return null;
			} else {
				project = jProject.getProject();
			}
		}
		try {
			switch (parameterId) {
			case BundleCommandsContributionItems.installParamId:
				installHandler(Collections.<IProject>singletonList(project));			
				break;
			case BundleCommandsContributionItems.activateProjectParamId:
				activateProjectHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.deactivateParamId:
				deactivateHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.refreshParamId:
				refreshHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.updateParamId:
				updateHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.resetParamId:
				resetHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.startParamId:
				startHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.stopParamId:
				stopHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.bundleViewParamId:
				bundleViewHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.bundleConsolePageParamId:
				bundleConsoleHandler();
				break;
			case BundleCommandsContributionItems.bundleLogViewParamId:
				bundleLogViewViewHandler();
				break;
			case BundlePopUpCommandsContributionItems.policyParamId:
				policyHandler(Collections.<IProject>singletonList(project));
				break;
			case BundleCommandsContributionItems.addClassPathParamId:
				updateClassPathHandler(Collections.<IProject>singletonList(project), true);
				break;
			case BundleCommandsContributionItems.removeClassPathParamId:
				updateClassPathHandler(Collections.<IProject>singletonList(project), false);
				break;
			case BundleCommandsContributionItems.interruptParamId:
				interruptHandler();
				break;
			case BundleCommandsContributionItems.stopOperationParamId:
				stopOperationHandler();
				break;
			default:
				break;
			}
		} catch (InPlaceException | ExtenderException e) {
			StatusManager.getManager()
			.handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
							Msg.ADD_MENU_EXEC_ERROR, e), StatusManager.LOG);
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {

		Object parameterId = parameters.get(BundlePopUpCommandsContributionItems.menuIdPar);
		// Set checked activation policy
		if (null != parameterId && parameterId.equals(BundlePopUpCommandsContributionItems.policyParamId)) {
			// Project selected in package or project explorer
			IJavaProject javaProject = getSelectedJavaProject();
			if (null == javaProject) {
				return;
			}
			IProject project = javaProject.getProject();
			try {
				// Set current activation policy from manifest
				boolean isLazy = BundleProjectSettings.getActivationPolicy(project);
				element.setChecked(!isLazy);
			} catch (InPlaceException e) {
				// Don't spam this meassage.
				if (!BuildErrorClosure.hasManifestBuildErrors(project) && BuildErrorClosure.hasBuildState(project)) {	
					String msg = ExceptionMessage.getInstance().formatString("error_set_policy", javaProject.getProject().getName());
					StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
							StatusManager.LOG);
				}
				element.setChecked(false);
			}
		}
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
}
