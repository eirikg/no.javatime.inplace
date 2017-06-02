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

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Checked menu item to update bundle projects after build
 */
public class AutoUpdateHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.autoupdate";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		super.execute(event);
		try {
			if (getStoredValue()) {
				BundleTransition transition = Activator.getBundleTransitionService();
				BundleRegion region = Activator.getBundleRegionService();
				Collection<IProject> projects = transition.getPendingProjects(region.getActivatedProjects(),
						Transition.UPDATE);
				if (projects.size() > 0) {
					BundleMenuActivationHandler.updateHandler(projects);
				}
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		return null;
	}

	@Override
	protected void storeValue(Boolean value) throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		cmdStore.setIsUpdateOnBuild(value);
	}
	
	@Override
	protected boolean getStoredValue() throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		boolean isUpdate = cmdStore.isUpdateOnBuild();
		return isUpdate;
	}

	@Override
	public String getCommandId() {

		return commandId;
	}
}
