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
import no.javatime.inplace.pl.console.msg.Msg;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsole;

/**
 * An action for streaming messages and toggling {@code System.out} and {@code System.err} streams
 * to the bundle console. When not redirected, default IDE output device is used. IDE default is
 * typically the development IDE when running the target IDE, and no output or the shell when
 * running the development IDE.
 */
public class BundleConsoleSystemOutAction extends Action implements IPropertyChangeListener {

	public BundleConsoleSystemOutAction(IConsole console) {

		super(Msg.SYSTEM_OUT_BUTTON_TXT, Activator.getImageDescriptor("icons/system_in_out.gif")); //$NON-NLS-1$
		setToolTipText(Msg.SYSTEM_OUT_ACTION_TOOLTIP);
		// Redirects system out and err to this console
		boolean systemOut = Activator.getDefault().getMsgOpt().isSystemOutBundleConsole();
		setChecked(systemOut);
		addPropertyChangeListener(this);
	}

	/**
	 * Toggle between directing output to the bundle console and the IDE default
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Boolean state = (Boolean) event.getNewValue();
		if (null != state) {
			Activator.getDefault().getMsgOpt().setIsSystemOutBundleConsole(state);
			if (state) {
				Activator.getDefault().getBundleConsole().setSystemOutToBundleConsole();
			} else {
				Activator.getDefault().getBundleConsole().setSystemOutToIDEDefault();
			}
		}
	}

}
