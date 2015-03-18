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
 * Checked menu item to set activation policy to eager on bundle activation
 */
public class EagerActivationHandler extends AbstractOptionsHandler {

	public static String commandId = "no.javatime.inplace.command.eageractivation";

	@Override
	protected void storeValue(Boolean value) throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		cmdStore.setIsEagerOnActivate(value);
	}

	@Override
	protected boolean getStoredValue() throws ExtenderException {

		CommandOptions cmdStore = getOptionsService();
		boolean isEager = cmdStore.isEagerOnActivate();
		return isEager;
	}

	@Override
	public String getCommandId() {

		return commandId;
	}
}
