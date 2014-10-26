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
import java.util.LinkedHashSet;

import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.command.contributions.BundleMainCommandsContributionItems;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Execute menu selections from the main menu
 */
public class BundleMainActivationHandler extends BundleMenuActivationHandler {

	private BundleRegion region = Activator.getBundleRegionService();
	private BundleCommand command = Activator.getBundleCommandService(); 

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		String parameterId = event.getParameter(BundleMainCommandsContributionItems.menuIdPar);
		if (null == parameterId) {
			return null;
		}
		try {
			switch (parameterId) {
			case BundleCommandsContributionItems.deactivateParamId:
				deactivateHandler(BundleProjectState.getNatureEnabledProjects());
				break;
			case BundleCommandsContributionItems.activateProjectParamId:
				activateProjectHandler(ProjectProperties.getCandidateProjects());
				break;
			case BundleCommandsContributionItems.startParamId:
				Collection<Bundle> startProjects = new LinkedHashSet<Bundle>(); 
				for (IProject project : BundleProjectState.getNatureEnabledProjects()) {
					Bundle bundle = region.get(project);
					if (null == bundle) {
						continue;
					}
					int state = command.getState(bundle);
					if (Bundle.INSTALLED == state || Bundle.RESOLVED == state|| Bundle.STOPPING == state) {
						startProjects.add(bundle);
					}
				}
				if (startProjects.size() > 0) {
					startHandler(region.getBundleProjects(startProjects));
				}
				break;
			case BundleCommandsContributionItems.stopParamId:
				Collection<Bundle> stopProjects = new LinkedHashSet<Bundle>(); 
				for (IProject project : BundleProjectState.getNatureEnabledProjects()) {
					Bundle bundle = region.get(project);
					int state = command.getState(bundle);
					if (Bundle.ACTIVE == state || Bundle.STARTING == state) {
						stopProjects.add(bundle);
					}
				}
				if (stopProjects.size() > 0) {
					stopHandler(region.getBundleProjects(stopProjects));
				}
				break;
			case BundleCommandsContributionItems.refreshParamId:
				refreshHandler(BundleProjectState.getNatureEnabledProjects());
				break;
			case BundleCommandsContributionItems.refreshPendingParamId:
				Collection<Bundle> refreshProjects = new LinkedHashSet<Bundle>(); 
				for (IProject project : BundleProjectState.getNatureEnabledProjects()) {
					Bundle bundle = region.get(project);
					if (command.getBundleRevisions(bundle).size() > 1) {
						refreshProjects.add(bundle);
					}
				}
				if (refreshProjects.size() > 0) {
					refreshHandler(region.getBundleProjects(refreshProjects));
				}
				break;
			case BundleCommandsContributionItems.updateParamId:
				Collection<IProject> projectsToUpdate = 
						Activator.getBundleTransitionService().getPendingProjects(BundleProjectState.getNatureEnabledProjects(), 
						BundleTransition.Transition.UPDATE);			
				updateHandler(projectsToUpdate);
				break;
			case BundleCommandsContributionItems.resetParamId:
				resetHandler(ProjectProperties.getInstallableProjects());
				break;
			case BundleCommandsContributionItems.bundleViewParamId:
				bundleViewHandler(ProjectProperties.getInstallableProjects());
				break;
			case BundleCommandsContributionItems.bundleConsolePageParamId:
				bundleConsoleHandler();
				break;
			case BundleCommandsContributionItems.bundleLogViewParamId:
				bundleLogViewViewHandler();
				break;
			case BundleCommandsContributionItems.addClassPathParamId:
				updateClassPathHandler(ProjectProperties.getPlugInProjects(), true);
				break;
			case BundleCommandsContributionItems.removeClassPathParamId:
				updateClassPathHandler(ProjectProperties.getPlugInProjects(), false);
				break;
			case BundleCommandsContributionItems.dependencyDialogParamId:
				dependencyDialogHandler();
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
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public boolean isHandled() {
		return true;
	}
}
