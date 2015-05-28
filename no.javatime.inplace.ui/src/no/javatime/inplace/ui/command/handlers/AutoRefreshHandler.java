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

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Checked menu item to refresh bundle projects after update
 */
public class AutoRefreshHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.autorefresh";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		super.execute(event);
		try {
			if (getStoredValue()) {
				BundleCommand command = Activator.getBundleCommandService();
				BundleRegion region = Activator.getBundleRegionService();
				Collection<Bundle> bundles = region.getActivatedBundles();
				Collection<IProject> projects = new LinkedHashSet<>();
				for (Bundle bundle : bundles) {
					if (command.getBundleRevisions(bundle).size() > 1) {
						projects.add(region.getProject(bundle));
					}
				}
				if (projects.size() > 0) {
					BundleMenuActivationHandler.refreshHandler(projects);
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
		cmdStore.setIsRefreshOnUpdate(value);
	}

	@Override
	protected boolean getStoredValue() throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		boolean isRefresh =  cmdStore.isRefreshOnUpdate();
		return isRefresh;
	}

	@Override
	public String getCommandId() {

		return commandId;
	}
}
