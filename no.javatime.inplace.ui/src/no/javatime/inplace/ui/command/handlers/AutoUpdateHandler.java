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

import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;

import org.eclipse.ui.commands.IElementUpdater;

public class AutoUpdateHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.autoupdate";

	@Override
	protected void storeValue(Boolean value) throws InPlaceException {
		CommandOptions cmdStore = getOptionsService();
		cmdStore.setIsUpdateOnBuild(value);
	}

	@Override
	protected boolean getStoredValue() throws InPlaceException {
		CommandOptions cmdStore = getOptionsService();
		return cmdStore.isUpdateOnBuild();
	}

	@Override
	protected String getCommandId() {
		return commandId;
	}
}
