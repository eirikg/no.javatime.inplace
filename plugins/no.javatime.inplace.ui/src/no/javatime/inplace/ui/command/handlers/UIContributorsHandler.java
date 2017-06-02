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

import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.ui.Activator;

import org.eclipse.core.resources.IProject;

/**
 * Checked menu item allowing/disallowing activating bundles with ui contributions
 */
public class UIContributorsHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.uicontributors";

	@Override
	protected void storeValue(Boolean value) throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		final BundleProjectCandidates bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		Boolean storedValue = cmdStore.isAllowUIContributions();
		Collection<IProject> uIProjects = bundleProjectCandidates.getUIPlugins();
		if (!storedValue.equals(value)) {
			cmdStore.setIsAllowUIContributions(value);
			if (uIProjects.size() > 0) {
				Activator.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						BundleMenuActivationHandler.updateBundleListPage(bundleProjectCandidates.toJavaProjects(bundleProjectCandidates.getInstallable()));
					}			
				});
				if (!value) {
					// Deactivate projects allowing UI extensions
					Extension<Deactivate> deactivateExtender = 
							Activator.getTracker().getExtension(Deactivate.class.getName());
					Deactivate deactivate = deactivateExtender.getTrackedService();
					deactivate.addPendingProjects(uIProjects);
					Activator.getBundleExecEventService().add(deactivate);
					deactivateExtender.closeTrackedService();
				}
			}
		}
	}

	@Override
	protected boolean getStoredValue() throws InPlaceException {

		CommandOptions cmdStore = getOptionsService();
		boolean isAllowUI = cmdStore.isAllowUIContributions();
		return isAllowUI;
	}

	@Override
	public String getCommandId() {

		return commandId;
	}
}
