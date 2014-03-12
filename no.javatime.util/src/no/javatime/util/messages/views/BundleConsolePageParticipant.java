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
package no.javatime.util.messages.views;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Adds a remove button closing the console, a check button for redirection of {@code System.out}
 * and {@code System.err} to the bundle console and a shut down button exiting the IDE.
 */
public class BundleConsolePageParticipant implements IConsolePageParticipant {

	private ShutDownJVMAction systemShutDownAction;
	private CloseConsoleAction closeAction;
	private BundleConsoleSystemOutAction systemOutAction;
	
	public void init(IPageBookViewPage page, IConsole console) {

		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		systemShutDownAction = new ShutDownJVMAction(console);
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, systemShutDownAction);
		closeAction = new BundleConsoleRemoveAction(console);
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
		systemOutAction = new BundleConsoleSystemOutAction(console);
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, systemOutAction);
	}

	public void dispose() {
		systemShutDownAction = null;
		closeAction = null;
		systemOutAction = null;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	public void activated() {
	}

	public void deactivated() {
	}

}
