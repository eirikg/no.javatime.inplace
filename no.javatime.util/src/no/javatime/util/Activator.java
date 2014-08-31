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
package no.javatime.util;

import no.javatime.util.messages.Category;
import no.javatime.util.messages.WarnMessage;
import no.javatime.util.messages.views.BundleConsole;
import no.javatime.util.messages.views.BundleConsoleFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The Activator for the bundle console and retrieval of messages from resource bundles
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "no.javatime.util";

	/**
	 * Console of the current IDE
	 */
	public static String CONSOLE_NAME = "Bundle Console";

	public static Activator plugin;
	private static BundleContext context;
	private BundleConsole bundleConsole = null;
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;

	/**
	 * Empty constructor
	 */
	public Activator() {
	}

	/**
	 * Adds a service listener and log listeners to all log reader services
	 * 
	 * @param context this bundle context
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		loadPluginSettings(true);

		bundleConsole = BundleConsoleFactory.findConsole(CONSOLE_NAME);

	}

	/**
	 * Remove a service listener and log listeners for all log reader services
	 * 
	 * @param context this bundle context
	 */
	public void stop(BundleContext context) throws Exception {
		savePluginSettings(true);
		plugin = null;
		super.stop(context);
	}

	public BundleConsole getBundleConsole() {
		return bundleConsole;
	}

	/**
	 * Return the bundle context
	 * 
	 * @return the shared bundle context
	 */
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * The shared activator or plug-in object
	 * 
	 * @return the shared activator instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns the active workbench shell
	 * 
	 * @return the active workbench shell or null if not found
	 */
	public Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow = getActiveWorkbenchWindow();
		if (workBenchWindow == null) {
			return null;
		}
		return workBenchWindow.getShell();
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window or null if not found
	 */
	private IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null) {
			return null;
		}
		final IWorkbench workBench = plugin.getWorkbench();
		if (workBench == null) {
			return null;
		}
		workBenchWindow = null;
		getDisplay().syncExec(new Runnable() {
			public void run() {
				workBenchWindow = workBench.getActiveWorkbenchWindow();
			}
		});
		return workBenchWindow;
	}

	/**
	 * Returns the active page
	 * 
	 * @return the active page or null if not found
	 */
	public IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		return activeWorkbenchWindow.getActivePage();
	}

	/**
	 * Returns the active or the default display if the active display is null
	 * 
	 * @return the active or default display or null if the display is disposed
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null) {
			display = Display.getDefault();
		}
		if (display.isDisposed()) {
			return null;
		}
		return display;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	static public IEclipsePreferences getEclipsePreferenceStore() {
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}

	/**
	 * Save various setting through the preference service at the workspace level
	 * 
	 * @param flush true to save settings to storage
	 */
	private void savePluginSettings(Boolean flush) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			return;
		}
		try {
			prefs.clear();
		} catch (BackingStoreException e1) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			StatusManager.getManager().handle(new Status(IStatus.WARNING, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
			return;
		}
		prefs.putBoolean(Category.bundleEvents, Category.getState(Category.bundleEvents));
		prefs.putBoolean(Category.bundleOperations, Category.getState(Category.bundleOperations));
		prefs.putBoolean(Category.infoMessages, Category.getState(Category.infoMessages));
		prefs.putBoolean(Category.systemOut, Category.getState(Category.systemOut));
		try {
			if (flush) {
				prefs.flush();
			}
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_flushing_preference_store");
			StatusManager.getManager().handle(new Status(IStatus.WARNING, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Access previous saved state for resource change events that occurred since the last save.
	 * Restore checked menu entries through the command service and other setting through the
	 * preference service
	 * 
	 * @param sync true to prevent others changing the settings
	 */
	public void loadPluginSettings(Boolean sync) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			return;
		}
		try {
			if (sync) {
				prefs.sync();
			}
		} catch (BackingStoreException e) {
			return;
		}
		Category.setState(Category.bundleEvents,
				prefs.getBoolean(Category.bundleEvents, Category.getState(Category.bundleEvents)));
		// Category.setState(Category.bundleOperations,
		// prefs.getBoolean(Category.bundleOperations, Category.getState(Category.bundleOperations)));
		Category.setState(Category.infoMessages,
				prefs.getBoolean(Category.infoMessages, Category.getState(Category.infoMessages)));
		Category.setState(Category.systemOut,
				prefs.getBoolean(Category.systemOut, Category.getState(Category.systemOut)));
	}

}
