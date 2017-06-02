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

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;

/**
 * Checked menu item to update bundle projects after build
 */
public class ActivateOnErrorHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.activateonerror";

	@Override
	protected void storeValue(Boolean value) throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		cmdStore.setIsActivateOnCompileError(value);
	}
	
	@Override
	protected boolean getStoredValue() throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		boolean isActivate = cmdStore.isActivateOnCompileError();
		return isActivate;
	}

	@Override
	public String getCommandId() {

		return commandId;
	}
}
