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

import no.javatime.inplace.bundle.log.status.BundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus.StatusCode;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.events.BundleJobEvent;
import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.ui.command.handlers.AutoExternalCommandHandler;
import no.javatime.inplace.ui.command.handlers.AutoRefreshHandler;
import no.javatime.inplace.ui.command.handlers.AutoUpdateHandler;
import no.javatime.inplace.ui.command.handlers.DeactivateOnExitHandler;
import no.javatime.inplace.ui.command.handlers.EagerActivationHandler;
import no.javatime.inplace.ui.command.handlers.UIContributorsHandler;
import no.javatime.inplace.ui.command.handlers.UpdateClassPathOnActivateHandler;
import no.javatime.inplace.ui.extender.ExtenderBundleTracker;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Message;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;


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
	
	private Extension<CommandOptions> commandOptions;

	// Don't know the interface to extend yet. Can be any interface 
	private BundleTracker<Extender<?>> extenderBundleTracker;
	private BundleTrackerCustomizer<Extender<?>> extenderBundleTrackerCustomizer;

	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		try {			
			extenderBundleTrackerCustomizer = new ExtenderBundleTracker();
			// int trackStates = Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED | Bundle.UNINSTALLED;
			extenderBundleTracker = new BundleTracker<Extender<?>>(context, Bundle.ACTIVE | Bundle.STARTING, extenderBundleTrackerCustomizer);
			extenderBundleTracker.open();
			BundleManager.addBundleJobListener(Activator.getDefault());
			commandOptions = new Extension<>(CommandOptions.class);
			loadCheckedMenus();
		} catch (IllegalStateException | InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		BundleManager.removeBundleJobListener(this);
		extenderBundleTracker.close();
		extenderBundleTracker = null;
		super.stop(context);
		plugin = null;
	}
	
	public BundleTracker<Extender<?>> getExtenderBundleTracker() {
		return extenderBundleTracker;
	}
	
	public CommandOptions getCommandOptionsService() throws InPlaceException {
		
		CommandOptions cmdOpt = commandOptions.getService();
		if (null == cmdOpt) {
			throw new InPlaceException("invalid_service", CommandOptions.class.getName());			
		}
		return cmdOpt;
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
			@Override
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
	 * <p>
	 * Only necessary on startup
	 * @throws InPlaceException - if the command service or the option store is unavailable
	 */
	public void loadCheckedMenus() throws InPlaceException {

		ICommandService service =
				(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		if (null == service) {
			throw new InPlaceException("invalid_service", ICommandService.class.getName());
		}
		CommandOptions cmdOpt = getCommandOptionsService();
		Command command = service.getCommand(EagerActivationHandler.commandId);
		State state = command.getState(EagerActivationHandler.stateId);
		state.setValue(cmdOpt.isEagerOnActivate());

		command = service.getCommand(AutoExternalCommandHandler.commandId);
		state = command.getState(AutoExternalCommandHandler.stateId);
		state.setValue(cmdOpt.isAutoHandleExternalCommands());

		command = service.getCommand(AutoRefreshHandler.commandId);
		state = command.getState(AutoRefreshHandler.stateId);
		state.setValue(cmdOpt.isRefreshOnUpdate());
		service.refreshElements(command.getId(), null);

		command = service.getCommand(AutoUpdateHandler.commandId);
		state = command.getState(AutoUpdateHandler.stateId);
		state.setValue(cmdOpt.isUpdateOnBuild());

		command = service.getCommand(DeactivateOnExitHandler.commandId);
		state = command.getState(DeactivateOnExitHandler.stateId);
		state.setValue(cmdOpt.isDeactivateOnExit());

		command = service.getCommand(UpdateClassPathOnActivateHandler.commandId);
		state = command.getState(UpdateClassPathOnActivateHandler.stateId);
		state.setValue(cmdOpt.isUpdateDefaultOutPutFolder());

		command = service.getCommand(UIContributorsHandler.commandId);
		state = command.getState(UIContributorsHandler.stateId);
		state.setValue(cmdOpt.isAllowUIContributions());
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
			@Override
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
