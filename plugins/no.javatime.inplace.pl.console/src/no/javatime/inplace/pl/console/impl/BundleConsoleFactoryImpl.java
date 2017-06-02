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
package no.javatime.inplace.pl.console.impl;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.pl.console.Activator;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.console.view.BundleConsole;
import no.javatime.util.view.ViewUtil;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.internal.console.ConsoleView;

/**
 * Console factory extension and service opening and closing the bundle console. This factory both
 * implements the console factory plug-in extension and the console service exposed through the
 * {@link Extender} interface.
 * <p>
 * The {@code #openConsole()} callback from the console extension point declared in
 * {@code IConsoleFactory} is hidden but wrapped in {@code #showConsoleView()} method and thus
 * available through the service interface.
 * <p>
 * {@code ConsoleView} is discouraged.
 */
@SuppressWarnings("restriction")
public class BundleConsoleFactoryImpl implements IConsoleFactory, BundleConsoleFactory {

	@Override
	public ImageDescriptor getConsoleViewImage() {
		return BundleConsole.consoleViewImage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.pl.console.view.BundleConsoleFactory#isConsoleViewVisible()
	 */
	@Override
	public Boolean isConsoleViewVisible() {
		Boolean visible = ViewUtil.isVisible(IConsoleConstants.ID_CONSOLE_VIEW);
		if (visible) {
			if (isConsoleVisible()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.pl.console.view.BundleConsoleFactory#showConsoleView()
	 */
	@Override
	public void showConsoleView() {
		showConsole();
		ViewUtil.show(IConsoleConstants.ID_CONSOLE_VIEW);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.pl.console.view.BundleConsoleFactory#closeConsoleView()
	 */
	@Override
	public void closeConsoleView() {
		closeConsole();
		// TODO Consider to not close down the generic console view when the console is the last page.
		// Only the console page
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = manager.getConsoles();
		if (consoles.length == 0) {
			ViewUtil.hide(IConsoleConstants.ID_CONSOLE_VIEW);
		}
	}

	@Override
	public void setSystemOutToBundleConsole() {
		getConsole().setSystemOutToBundleConsole();
	}

	@Override
	public void setSystemOutToIDEDefault() {
		getConsole().setSystemOutToIDEDefault();
	}

	/**
	 * Opens the bundle console
	 * 
	 * @see #showConsole()
	 */
	public void openConsole() {
		showConsole();
	}

	/**
	 * Use the console in the current IDE.
	 * 
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
	 * Removes the bundle console. Dispose should do nothing to retain the document.
	 */
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		BundleConsole console = getConsole();
		if (console != null) {
			manager.removeConsoles(new IConsole[] { console });
		}
	}

	/**
	 * Terminates the JVM unconditionally
	 */
	public static void shutDown() {
		System.exit(0);
	}

	/**
	 * Check if the bundle console is the current visible console in the console view
	 * 
	 * @return true if the bundle console is visible in the console view. Else false.
	 */
	private Boolean isConsoleVisible() {
		BundleConsole console = getConsole();
		if (console != null) {
			// Console is open. Check if it is the current visible console
			IWorkbenchPage page = Activator.getDefault().getActivePage();
			if (null != page) {
				// ConsoleView is discouraged. Need it to find the current visible console
				ConsoleView view = (ConsoleView) page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
				if (null != view) {
					IConsole currConsole = view.getConsole();
					// Open and visible
					if (null != currConsole && console.equals(currConsole)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Get the bundle console
	 * 
	 * @return the bundle console
	 */
	private static BundleConsole getConsole() {
		return Activator.getDefault().getBundleConsole();
	}

	/**
	 * Shows an existing or creates a new bundle console
	 */
	private void showConsole() {
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
				manager.addConsoles(new IConsole[] { console });
				ConsolePlugin.getDefault().getConsoleManager()
						.addConsoleListener(console.new ConsoleLifecycle());
			}
			console.activate();
		}
	}
}
