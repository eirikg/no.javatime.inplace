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

import no.javatime.util.Activator;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsole;

/**
 * An action for streaming messages and toggling {@code System.out} and {@code System.err} streams to the bundle console.
 * When not redirected, default IDE output device is used. IDE default is typically the development IDE when
 * running the target IDE, and no output or the shell when running the development IDE.
 */
public class BundleConsoleSystemOutAction extends Action implements IPropertyChangeListener {
	
  
  public BundleConsoleSystemOutAction(IConsole console) {
 
  	super(Message.getInstance().formatString("system_out_button"), //$NON-NLS-1$
    		Activator.getImageDescriptor("icons/system_in_out.gif"));  //$NON-NLS-1$
    setToolTipText(Message.getInstance().formatString("system_out_action_tooltip")); //$NON-NLS-1$
    // Redirects system out and err to this console
    setChecked(Category.getState(Category.systemOut));       
    addPropertyChangeListener(this);
  }
  
  /**
   * Toggle between directing output to the bundle console and the IDE default
   */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Boolean state = (Boolean) event.getNewValue();
		if (null != state) {
	    Category.setState(Category.systemOut, state);
			if (state) {
		    Activator.getDefault().getBundleConsole().setSystemOutToBundleConsole();
			} else {
				Activator.getDefault().getBundleConsole().setSystemOutToIDEDefault();
			}
		}
	}
	
}
