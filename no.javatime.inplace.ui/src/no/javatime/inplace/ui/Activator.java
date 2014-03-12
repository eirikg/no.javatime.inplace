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
package no.javatime.inplace.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.events.BundleJobEvent;
import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.command.handlers.AutoDependencyHandler;
import no.javatime.inplace.ui.command.handlers.AutoRefreshHandler;
import no.javatime.inplace.ui.command.handlers.AutoUpdateHandler;
import no.javatime.inplace.ui.command.handlers.BundleMenuActivationHandler;
import no.javatime.inplace.ui.command.handlers.DeactivateOnExitHandler;
import no.javatime.inplace.ui.command.handlers.EagerActivationHandler;
import no.javatime.inplace.ui.command.handlers.UIContributorsHandler;
import no.javatime.inplace.ui.command.handlers.UpdateClassPathOnActivateHandler;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements BundleJobEventListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "no.javatime.inplace.ui"; //$NON-NLS-1$
	private static Activator plugin;
	private static BundleContext context;
	
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		BundleManager.addBundleJobListener(Activator.getDefault());
		loadPluginSettings(true);
		loadCheckedMenus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		savePluginSettings(true);
		BundleManager.removeBundleJobListener(this);
		plugin = null;
		super.stop(context);
	}
	
	@Override
	public void bundleJobEvent(BundleJobEvent event) {
		WorkspaceJob bundleJob = event.getBundleJob();
		scheduleViaPart(bundleJob, event.getDelay(), false);
	}
	/**
	 * Schedule job through the {@linkplain no.javatime.inplace.ui.views.BundleView BundleView} if it is visible.
	 * Alters the presentation of the mouse cursor to half busy when a job is running.
	 * 
	 * @param bundleJob to schedule
	 * @param delay schedule of job in milliseconds
	 * @param setUser to true if job progress is to be shown
	 */
	public static void scheduleViaPart(final WorkspaceJob bundleJob, final long delay, Boolean setUser) {

		bundleJob.setUser(setUser);
		Activator.getDisplay().asyncExec(new Runnable() {
			public void run() {
				BundleView bv = (BundleView) Message.getView(BundleView.ID);
				if (null != bv) {
					IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) bv.getSite().getAdapter(
							IWorkbenchSiteProgressService.class);
					// TODO check for null on adapter and site
					siteService.showBusyForFamily(BundleJob.FAMILY_BUNDLE_LIFECYCLE);
					siteService.showBusyForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
					siteService.showBusyForFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
					siteService.schedule(bundleJob, delay, true);
				} else {
					bundleJob.schedule(delay);
				}
			}
		});
	}

	/**
	 * The context for interacting with the FrameWork
	 * 
	 * @return the bundle execution context within the FrameWork
	 */
	public static BundleContext getContext() {
		return context;
	}

	static public IEclipsePreferences getEclipsePreferenceStore() {
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}
	
	/**
	 * Restore state of checked menu entries
	 */
	public void loadCheckedMenus() {

		BundleMenuActivationHandler.setCheckedMenuEntry(Category.eagerActivation,
				EagerActivationHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.autoDependency, 
				AutoDependencyHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.autoRefresh, 
				AutoRefreshHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.autoUpdate, 
				AutoUpdateHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.deactivateOnExit, 
				DeactivateOnExitHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.updateClassPathOnActivate,
				UpdateClassPathOnActivateHandler.commandId);
		BundleMenuActivationHandler.setCheckedMenuEntry(Category.uiContributors, 
				UIContributorsHandler.commandId);
	}

	public void loadPluginSettings(Boolean sync) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			return;
		}
		// Activate
		Category.setState(Category.partialGraphOnActivate, prefs.getBoolean(Category.partialGraphOnActivate, 
				Category.getState(Category.partialGraphOnActivate)));
		Category.setState(Category.requiringOnActivate, prefs.getBoolean(Category.requiringOnActivate, 
				Category.getState(Category.requiringOnActivate)));	
		// Start
		Category.setState(Category.partialGraphOnStart, prefs.getBoolean(Category.partialGraphOnStart, 
				Category.getState(Category.partialGraphOnStart)));		
		Category.setState(Category.requiringOnStart, prefs.getBoolean(Category.requiringOnStart, 
				Category.getState(Category.requiringOnStart)));		
		Category.setState(Category.providingOnStart, prefs.getBoolean(Category.providingOnStart, 
				Category.getState(Category.providingOnStart)));		
		// Deactivate
		Category.setState(Category.partialGraphOnDeactivate, prefs.getBoolean(Category.partialGraphOnDeactivate, 
				Category.getState(Category.partialGraphOnDeactivate)));		
		Category.setState(Category.providingOnDeactivate, prefs.getBoolean(Category.providingOnDeactivate, 
				Category.getState(Category.providingOnDeactivate)));		
		// Stop
		Category.setState(Category.partialGraphOnStop, prefs.getBoolean(Category.partialGraphOnStop, 
				Category.getState(Category.partialGraphOnStop)));		
		Category.setState(Category.requiringOnStop, prefs.getBoolean(Category.requiringOnStop, 
				Category.getState(Category.requiringOnStop)));		
		Category.setState(Category.providingOnStop, prefs.getBoolean(Category.providingOnStop, 
				Category.getState(Category.providingOnStop)));		
	}
	/**
	 * Save various settings through the preference service at the workspace level
	 * 
	 * @param flush true to save settings to storage
	 */
	public void savePluginSettings(Boolean flush) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
			return;
		}
		try {
			prefs.clear();
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_clearing_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			return; // Use existing values
		}
		if (BundleManager.getRegion().isBundleWorkspaceActivated()) {
			for (Bundle bundle : BundleManager.getRegion().getBundles()) {
				String symbolicKey = BundleManager.getRegion().getSymbolicKey(bundle, null);
				if ((bundle.getState() & (Bundle.RESOLVED)) != 0) {
					prefs.putInt(symbolicKey, bundle.getState());
				}
			}
		}
		// Activate
		prefs.putBoolean(Category.partialGraphOnActivate, Category.getState(Category.partialGraphOnActivate));
		prefs.putBoolean(Category.requiringOnActivate, Category.getState(Category.requiringOnActivate));
		// Start
		prefs.putBoolean(Category.requiringOnStart, Category.getState(Category.requiringOnStart));
		prefs.putBoolean(Category.providingOnStart, Category.getState(Category.providingOnStart));
		prefs.putBoolean(Category.partialGraphOnStart, Category.getState(Category.partialGraphOnStart));
		// Deactivate
		prefs.putBoolean(Category.partialGraphOnDeactivate, Category.getState(Category.partialGraphOnDeactivate));
		prefs.putBoolean(Category.providingOnDeactivate, Category.getState(Category.providingOnDeactivate));
		// Stop
		prefs.putBoolean(Category.partialGraphOnStop, Category.getState(Category.partialGraphOnStop));		
		prefs.putBoolean(Category.requiringOnStop, Category.getState(Category.requiringOnStop));
		prefs.putBoolean(Category.providingOnStop, Category.getState(Category.providingOnStop));
		
		
		try {
			if (flush) {
				prefs.flush();
			}
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_flushing_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Get a valid display
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// May be null if outside the UI thread
		if (display == null) {
			display = Display.getDefault();
		}
		if (display.isDisposed()) {
			return null;
		}
		return display;
	}

	/**
	 * Returns the active workbench window
	 * @return the active workbench window or null if not found
	 */
	public IWorkbenchWindow getActiveWorkbenchWindow() {
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
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
