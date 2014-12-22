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

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.command.contributions.BundleMainCommandsContributionItems;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Execute menu selections from the main menu
 */
public class BundleMainActivationHandler extends BundleMenuActivationHandler {

	private BundleRegion region = Activator.getBundleRegionService();
	private BundleCommand command = Activator.getBundleCommandService(); 
	private BundleProjectCandidates bundleProjectCandidates = Activator.getBundleProjectCandidatesService(); 
	private BundleRegion bundleRegion = Activator.getBundleRegionService();
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String parameterId = event.getParameter(BundleMainCommandsContributionItems.menuIdPar);
		if (null == parameterId) {
			return null;
		}
		try {
			switch (parameterId) {
			case BundleCommandsContributionItems.deactivateParamId:
				deactivateHandler(bundleRegion.getActivatedProjects());
				break;
			case BundleCommandsContributionItems.activateProjectParamId:
				activateProjectHandler(bundleProjectCandidates.getCandidates());
				break;
			case BundleCommandsContributionItems.startParamId:
				Collection<Bundle> startProjects = new LinkedHashSet<Bundle>(); 
				Collection<IProject> startNatures = bundleRegion.getActivatedProjects();
				for (IProject project : startNatures) {
					Bundle bundle = region.getBundle(project);
					if (null == bundle) {
						continue;
					}
					int state = command.getState(bundle);
					if (Bundle.INSTALLED == state || Bundle.RESOLVED == state|| Bundle.STOPPING == state) {
						startProjects.add(bundle);
					}
				}
				if (startProjects.size() > 0) {
					startHandler(region.getProjects(startProjects));
				}
				break;
			case BundleCommandsContributionItems.stopParamId:
				Collection<Bundle> stopProjects = new LinkedHashSet<Bundle>(); 
				Collection<IProject> stopNatures = bundleRegion.getActivatedProjects();
				for (IProject project : stopNatures) {
					Bundle bundle = region.getBundle(project);
					int state = command.getState(bundle);
					if (Bundle.ACTIVE == state || Bundle.STARTING == state) {
						stopProjects.add(bundle);
					}
				}
				if (stopProjects.size() > 0) {
					stopHandler(region.getProjects(stopProjects));
				}
				break;
			case BundleCommandsContributionItems.refreshParamId:
				refreshHandler(bundleRegion.getActivatedProjects());
				break;
			case BundleCommandsContributionItems.refreshPendingParamId:
				Collection<Bundle> refreshProjects = new LinkedHashSet<Bundle>(); 
				Collection<IProject> refreshNatures = bundleRegion.getActivatedProjects();
				for (IProject project : refreshNatures) {
					Bundle bundle = region.getBundle(project);
					if (command.getBundleRevisions(bundle).size() > 1) {
						refreshProjects.add(bundle);
					}
				}
				if (refreshProjects.size() > 0) {
					refreshHandler(region.getProjects(refreshProjects));
				}
				break;
			case BundleCommandsContributionItems.updateParamId:
				Collection<IProject> projectsToUpdate = 
						Activator.getBundleTransitionService().getPendingProjects(bundleRegion.getActivatedProjects(), 
						BundleTransition.Transition.UPDATE);			
				updateHandler(projectsToUpdate);
				break;
			case BundleCommandsContributionItems.resetParamId:
				resetHandler(bundleProjectCandidates.getInstallable());
				break;
			case BundleCommandsContributionItems.bundleViewParamId:
				bundleViewHandler(bundleProjectCandidates.getInstallable());
				break;
			case BundleCommandsContributionItems.bundleConsolePageParamId:
				bundleConsoleHandler();
				break;
			case BundleCommandsContributionItems.bundleLogViewParamId:
				bundleLogViewViewHandler();
				break;
			case BundleCommandsContributionItems.addClassPathParamId:
				updateClassPathHandler(bundleProjectCandidates.getBundleProjects(), true);
				break;
			case BundleCommandsContributionItems.removeClassPathParamId:
				updateClassPathHandler(bundleProjectCandidates.getBundleProjects(), false);
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
			String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, parameterId);
			StatusManager.getManager()
			.handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
							msg, e), StatusManager.LOG);
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
