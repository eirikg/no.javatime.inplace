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

import java.util.Map;

import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.dialogs.DependencyDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
/**
 * Checked menu item allowing/disallowing activating bundles with ui contributions
 */
public class DependencyHandler extends AbstractHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DependencyDialog dialog = new 
				DependencyDialog(Activator.getDefault().getActiveWorkbenchWindow().getShell());
		dialog.create();
		dialog.open();
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		// TODO Auto-generated method stub
	}

}
