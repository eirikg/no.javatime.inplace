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

import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.core.resources.IProject;

/**
 * Checked menu item allowing/disallowing activating bundles with ui contributions
 */
public class UIContributorsHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.uicontributors";

	@Override
	protected void storeValue(Boolean value) throws InPlaceException {
		CommandOptions cmdStore = getOptionsService();
		Boolean storedValue = cmdStore.isAllowUIContributions();
		Collection<IProject> uIProjects = ProjectProperties.getUIContributors();
		if (!storedValue.equals(value) && uIProjects.size() > 0) {
			cmdStore.setIsAllowUIContributions(value);
			Activator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					BundleMenuActivationHandler.updateBundleListPage(ProjectProperties
							.toJavaProjects(ProjectProperties.getInstallableProjects()));
				}			
			});
			if (!value) {
				// Deactivate projects allowing UI extensions
				DeactivateJob daj = 
						new DeactivateJob(Msg.DEACTIVATE_UI_CONTRIBOTRS_JOB, uIProjects);			
				BundleJobManager.addBundleJob(daj, 0);
			}
		}
	}

	@Override
	protected boolean getStoredValue() throws InPlaceException {
		CommandOptions cmdStore = getOptionsService();
		return cmdStore.isAllowUIContributions();
	}

	@Override
	protected String getCommandId() {
		return commandId;
	}
}
