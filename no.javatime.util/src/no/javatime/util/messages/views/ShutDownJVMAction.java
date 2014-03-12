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

import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.actions.CloseConsoleAction;

import no.javatime.util.Activator;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;

/**
 * Action to close the bundle console.
 */
public class ShutDownJVMAction extends Action {

  public ShutDownJVMAction(IConsole console) {
    	super(Message.getInstance().formatString("system_exit_button"), //$NON-NLS-1$
      		Activator.getImageDescriptor("icons/stop.gif"));  //$NON-NLS-1$
      setToolTipText(Message.getInstance().formatString("system_exit_action_tooltip")); //$NON-NLS-1$
  }
  
  /**
   * @see BundleConsoleFactory#shutDown()
   */
  public void run() {
      BundleConsoleFactory.shutDown();
  }
}