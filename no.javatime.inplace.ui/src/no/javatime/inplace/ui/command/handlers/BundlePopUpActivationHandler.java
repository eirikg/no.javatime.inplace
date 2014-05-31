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
import java.util.Collections;
import java.util.Map;

import no.javatime.inplace.bundle.log.status.BundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus.StatusCode;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleMainCommandsContributionItems;
import no.javatime.inplace.ui.command.contributions.BundlePopUpCommandsContributionItems;
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
		IStructuredSelection ss = selection;
		Object object = ss.getFirstElement();		
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
		Collection<IProject> projects = Collections.<IProject>singletonList(project);		
		if (parameterId.equals(BundlePopUpCommandsContributionItems.installParamId)) {
			installHandler(ProjectProperties.getPlugInProjects());
		} else if (parameterId.equals(BundleMainCommandsContributionItems.activateParamId)) {
			activateProjectHandler(projects);
		}	else if (parameterId.equals(BundleMainCommandsContributionItems.deactivateParamId)) {
			deactivateHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.refreshParamId)) {
			refreshHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.updateParamId)) {
			updateHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.resetParamId)) {
			resetHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.startParamId)) {
			startHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.stopParamId)) {
			stopHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.bundleViewParamId)) {
			bundleViewHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.consoleParamId)) {
			consoleHandler(projects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.messageViewParamId)) {
			messageViewHandler();
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.policyParamId)) {
			policyHandler(projects);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.addClassPathParamId)) {
			updateClassPathHandler(projects, true);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.removeClassPathParamId)) {
			updateClassPathHandler(projects, false);
		}	else if (parameterId.equals(BundlePopUpCommandsContributionItems.inerruptParamId)) {
			interruptHandler();
		}	else if (parameterId.equals(BundlePopUpCommandsContributionItems.stopOperationParamId)) {
			stopOperation();
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
				boolean isLazy = BundleProject.getLazyActivationPolicyFromManifest(project);
				element.setChecked(!isLazy);
			} catch (InPlaceException e) {
				// Don't spam this meassage.
				if (!ProjectProperties.hasManifestBuildErrors(project) && ProjectProperties.hasBuildState(project)) {	
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
