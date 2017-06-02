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

import no.javatime.inplace.pl.console.impl.BundleConsoleFactoryImpl;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.actions.CloseConsoleAction;

/**
 * Action to close the bundle console.
 */
public class BundleConsoleRemoveAction extends CloseConsoleAction {

  public BundleConsoleRemoveAction(IConsole console) {
      super(console);
  }
  
  /**
   * @see BundleConsoleFactoryImpl#closeConsole()
   */
  public void run() {
      BundleConsoleFactoryImpl.closeConsole();
  }
}