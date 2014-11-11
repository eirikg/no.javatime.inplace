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

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.events.BundleJobEvent;
import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.command.handlers.AutoExternalCommandHandler;
import no.javatime.inplace.ui.command.handlers.AutoRefreshHandler;
import no.javatime.inplace.ui.command.handlers.AutoUpdateHandler;
import no.javatime.inplace.ui.command.handlers.DeactivateOnExitHandler;
import no.javatime.inplace.ui.command.handlers.EagerActivationHandler;
import no.javatime.inplace.ui.command.handlers.UIContributorsHandler;
import no.javatime.inplace.ui.command.handlers.UpdateClassPathOnActivateHandler;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.view.ViewUtil;

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
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements BundleJobEventListener {

	// The plug-in ID
	final public static String PLUGIN_ID = "no.javatime.inplace.ui"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;
	
	private Extension<CommandOptions> commandOptions;
	private static Extension<BundleRegion> bundleRegion;
	private static Extension<BundleCommand> bundleCommand;
	private static Extension<BundleTransition> bundleTransition;
	// Bundle candidate projects
	private static Extension<BundleProjectCandidates> bundleProjectCandidates;
	// Bundle project meta information
	private static Extension<BundleProjectMeta> bundlePrrojectMeta;
	
	// Register (extend) services for use facilitated by other bundles  
	private BundleTracker<Extender<?>> extenderBundleTracker;

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
			extenderBundleTracker = new ExtenderBundleTracker(context, Bundle.ACTIVE, null);
			extenderBundleTracker.open();
			BundleJobManager.addBundleJobListener(Activator.getDefault());
			commandOptions = Extenders.getExtension(CommandOptions.class.getName());
			bundleRegion = Extenders.getExtension(BundleRegion.class.getName());
			bundleCommand = Extenders.getExtension(BundleCommand.class.getName());
			bundleTransition = Extenders.getExtension(BundleTransition.class.getName());
			bundleProjectCandidates = Extenders.getExtension(BundleProjectCandidates.class.getName());
			bundlePrrojectMeta = Extenders.getExtension(BundleProjectMeta.class.getName());
	
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

		BundleJobManager.removeBundleJobListener(this);
		extenderBundleTracker.close();
		super.stop(context);
		plugin = null;
	}
	
	public static BundleRegion getBundleRegionService() throws InPlaceException, ExtenderException {

		BundleRegion br = bundleRegion.getService(context.getBundle());
		if (null == br) {
			throw new InPlaceException("invalid_service", BundleRegion.class.getName());			
		}
		return br;
	}

	public static BundleCommand getBundleCommandService() throws InPlaceException, ExtenderException {

		BundleCommand br = bundleCommand.getService(context.getBundle());
		if (null == br) {
			throw new InPlaceException("invalid_service", BundleCommand.class.getName());			
		}
		return br;
	}
	
	public static BundleTransition getBundleTransitionService() throws InPlaceException, ExtenderException {

		BundleTransition bt = bundleTransition.getService(context.getBundle());
		if (null == bt) {
			throw new InPlaceException("invalid_service", BundleTransition.class.getName());			
		}
		return bt;
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService() throws InPlaceException, ExtenderException {
		
		BundleProjectCandidates bp = bundleProjectCandidates.getService(context.getBundle());
		if (null == bp) {
			throw new InPlaceException("invalid_service", BundleProjectCandidates.class.getName());			
		}
		return bp;
	}

	public static BundleProjectMeta getBundleProjectMetaService() throws InPlaceException, ExtenderException {
		
		BundleProjectMeta bpd = bundlePrrojectMeta.getService(context.getBundle());
		if (null == bpd) {
			throw new InPlaceException("invalid_service", BundleProjectMeta.class.getName());			
		}
		return bpd;
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
		scheduleViaBundleView(bundleJob, event.getDelay(), false);
	}
	/**
	 * Schedule job through the {@linkplain no.javatime.inplace.ui.views.BundleView BundleView} if it is visible.
	 * Alters the presentation of the mouse cursor to half busy when a job is running.
	 * 
	 * @param bundleJob to schedule
	 * @param delay schedule of job in milliseconds
	 * @param setUser to true if job progress is to be shown
	 */
	public static void scheduleViaBundleView(final WorkspaceJob bundleJob, final long delay, Boolean setUser) {

		bundleJob.setUser(setUser);
		Activator.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				BundleView bv = (BundleView) ViewUtil.get(BundleView.ID);
				if (null != bv) {
					IWorkbenchSiteProgressService siteService = null;
					IWorkbenchPartSite partSite = bv.getSite();
					if (null != partSite) {
						siteService = (IWorkbenchSiteProgressService) partSite.getAdapter(
								IWorkbenchSiteProgressService.class);						
					}
					if (null != siteService) {
						siteService.showBusyForFamily(BundleJob.FAMILY_BUNDLE_LIFECYCLE);
						siteService.showBusyForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
						siteService.showBusyForFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
						siteService.schedule(bundleJob, delay, true);
					}
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
