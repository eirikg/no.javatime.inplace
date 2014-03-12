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
import no.javatime.util.messages.Message;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.internal.console.ConsoleView;

/**
 * Factory extension opening and closing the bundle console.
 * <p>
 * {@code ConsoleView} is discouraged.
 */
@SuppressWarnings("restriction")
public class BundleConsoleFactory implements IConsoleFactory { 
	
	/** Opens the bundle console
	 * @see #showConsole()
	 */
	public void openConsole() {
    showConsole();
  }
	
	/**
	 * Shows an existing or creates a new bundle console
	 */
	public static void showConsole() {
		BundleConsole console = getConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] consoles = manager.getConsoles();
			boolean exists = false;
			for (int i = 0; i < consoles.length; i++) {
				if (console == consoles[i]) {
					exists = true;
				}
			}
			if (!exists) {
				manager.addConsoles(new IConsole[] {console});
				ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new ConsoleLifecycle());
			}
			console.activate();
		}
	}

	/**
	 * Removes the bundle console. Dispose should do nothing to retain the document.
	 */
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		BundleConsole console = getConsole();
		if (console != null) {
			manager.removeConsoles(new IConsole[] {console});
		}
	}
	
	/**
	 * Terminates the JVM unconditionally
	 */
	public static void shutDown() {
		System.exit(0);
	}
	
	/**
	 * Get the bundle console
	 * @return the bundle console
	 */
	public static BundleConsole getConsole () {
		return Activator.getDefault().getBundleConsole();
	}

	/**
	 * Check if the bundle console is the current visible console in the console view
	 * @return true if the bundle console is visible in the console view. Else false.
	 */
	public static Boolean isConsoleVisible () {
		BundleConsole console = getConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] consoles = manager.getConsoles();
			boolean isOpen = false;
			for (int i = 0; i < consoles.length; i++) {
				if (console == consoles[i]) {
					isOpen = true;
				}
			}
			if (!isOpen) {
				return false;
			}
			// Console is open. Check if it is the current visible console
			IWorkbenchPage page = Activator.getDefault().getActivePage();
			if (null != page) {
				// ConsoleView is discouraged. Need it to find the current visible console
				ConsoleView view = (ConsoleView) page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
				if (null != view) {
					IConsole currConsole = view.getConsole();
					// Open and visible
					if (console == currConsole) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Use the console in the current IDE. 
	 * @param name name of the console
	 * @return an existing or a new console
	 */
	public static BundleConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager manager = plugin.getConsoleManager();
		IConsole[] existing = manager.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (BundleConsole) existing[i];
			}
		}
		// No console found. Create a new one
		final BundleConsole bundleConsole = new BundleConsole(name, null);
		manager.addConsoles(new IConsole[] { bundleConsole });
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				bundleConsole.initializeStreams();
			}
		});
		return bundleConsole;
	}
	
	/**
	 * Show the generic console view
	 */
	public static void showConsoleView() {
		showConsole();
		Message.showView(IConsoleConstants.ID_CONSOLE_VIEW);
	}

	/**
	 * Close the generic console view when the bundle console id the
	 * last page in the console view
	 */
	public static void closeConsoleView() {
		closeConsole();
		// TODO Consider to not close down the generic console view when the console is the last page.
		// Only the console page
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = manager.getConsoles();
		if (consoles.length == 0) { 
			Message.hideView(IConsoleConstants.ID_CONSOLE_VIEW);
		}
	}
	
	/**
	 * Check if generic console view is visible
	 * @return true if the generic console is visible, otherwise false
	 */
	public static Boolean isConsoleViewVisible() {
		Boolean visible = Message.isViewVisible(IConsoleConstants.ID_CONSOLE_VIEW);
		if (visible) {
			if (!isConsoleVisible()) {
				return false;
			}
			return true;
		}
		return false;
	}
}

