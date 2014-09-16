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
package no.javatime.inplace.pl.console.view;

import no.javatime.inplace.pl.console.Activator;
import no.javatime.inplace.pl.console.impl.BundleConsoleFactoryImpl;
import no.javatime.inplace.pl.console.msg.Msg;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.IConsole;

/**
 * Action to shut down the VM.
 */
public class ShutDownJVMAction extends Action {

	public ShutDownJVMAction(IConsole console) {
		super(Msg.SYSTEM_EXIT_BUTTON_TXT, Activator.getImageDescriptor("icons/stop.gif")); //$NON-NLS-1$
		setToolTipText(Msg.SYSTEM_EXIT_ACTION_TOOLTIP);
	}

	/**
	 * @see BundleConsoleFactoryImpl#shutDown()
	 */
	public void run() {
		BundleConsoleFactoryImpl.shutDown();
	}
}