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
import java.util.concurrent.TimeoutException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleMainCommandsContributionItems;
import no.javatime.inplace.ui.command.contributions.BundlePopUpCommandsContributionItems;

/**
 * Handles in-place main menu for java projects.
 */
public class BundleMainActivationHandler extends BundleMenuActivationHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		String parameterId = event.getParameter(BundleMainCommandsContributionItems.menuIdPar);
		if (null == parameterId) {
			return null;
		}
		
		if (parameterId.equals(BundleMainCommandsContributionItems.deactivateParamId)) {
		 Collection<IProject> javaTimeProjects = ProjectProperties.getActivatedProjects();
		 deactivateHandler(javaTimeProjects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.activateParamId)) {
			Collection<IProject> projects = ProjectProperties.getCandidateProjects();
			activateProjectHandler(projects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.startParamId)) {

			Collection<Bundle> startProjects = new LinkedHashSet<Bundle>(); 
			for (IProject project : ProjectProperties.getActivatedProjects()) {
				Bundle bundle = BundleManager.getRegion().get(project);
				if (null == bundle) {
					continue;
				}
				int state = BundleManager.getCommand().getState(bundle);
				if (Bundle.INSTALLED == state || Bundle.RESOLVED == state|| Bundle.STOPPING == state) {
					startProjects.add(bundle);
				}
			}
			if (startProjects.size() > 0) {
				Collection<IProject> projects = BundleManager.getRegion().getProjects(startProjects);
				startHandler(projects);
			}
		} else if (parameterId.equals(BundleMainCommandsContributionItems.stopParamId)) {
			Collection<Bundle> stopProjects = new LinkedHashSet<Bundle>(); 
			for (IProject project : ProjectProperties.getActivatedProjects()) {
				Bundle bundle = BundleManager.getRegion().get(project);
    		int state = BundleManager.getCommand().getState(bundle);
    		if (Bundle.ACTIVE == state || Bundle.STARTING == state) {
    			stopProjects.add(bundle);
    		}
			}
			if (stopProjects.size() > 0) {
				Collection<IProject> projects = BundleManager.getRegion().getProjects(stopProjects);
				stopHandler(projects);
			}
		} else if (parameterId.equals(BundleMainCommandsContributionItems.refreshParamId)) {
			Collection<IProject> projects = ProjectProperties.getActivatedProjects();
			refreshHandler(projects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.refreshPendingParamId)) {
			Collection<IProject> projects = ProjectProperties.getActivatedProjects();
			refreshHandler(projects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.updateParamId)) {
			Collection<IProject> projects = ProjectProperties.getActivatedProjects();
			Collection<IProject> projectsToUpdate = 
					BundleManager.getTransition().getPendingProjects(projects, BundleTransition.Transition.UPDATE);			
			updateHandler(projectsToUpdate);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.resetParamId)) {
			Collection<IProject> projects = ProjectProperties.getInstallableProjects();
			resetHandler(projects);
		} else if (parameterId.equals(BundleMainCommandsContributionItems.bundleViewParamId)) {
			bundleViewHandler(ProjectProperties.getInstallableProjects());
		} else if (parameterId.equals(BundleMainCommandsContributionItems.consoleParamId)) {
			consoleHandler(ProjectProperties.getInstallableProjects());
		} else if (parameterId.equals(BundleMainCommandsContributionItems.messageViewParamId)) {
			messageViewHandler();
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.addClassPathParamId)) {
			Collection<IProject> projects = ProjectProperties.getPlugInProjects();
			updateClassPathHandler(projects, true);
		} else if (parameterId.equals(BundlePopUpCommandsContributionItems.removeClassPathParamId)) {
			Collection<IProject> projects = ProjectProperties.getPlugInProjects();
			updateClassPathHandler(projects, false);
		}	else if (parameterId.equals(BundlePopUpCommandsContributionItems.busyParamId)) {
			BundleJob job = OpenProjectHandler.getRunningBundleJob();
			if (null != job) {
				job.cancel();
				Thread thread = job.getThread();
				if (null != thread) {
					// Requires that the user code (e.g. in the start method) is aware of interrupts
					thread.interrupt();
				}
			}
		}	else if (parameterId.equals(BundlePopUpCommandsContributionItems.stopOperationParamId)) {
			Activator.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						BundleJob job = OpenProjectHandler.getRunningBundleJob();
						CommandOptions co = Activator.getDefault().getOptionsService();
						if (null != job && (!co.isTimeOut()) && BundleManager.getCommand().isStateChanging()) {			
							BundleManager.getCommand().stopCurrentBundleOperation();
						}
					} catch (IllegalStateException e) {
						// Also caught by the bundle API
					} catch (TimeoutException e) {
						StatusManager.getManager().handle(
								new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e),
								StatusManager.LOG);
					}
				}
			});
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
