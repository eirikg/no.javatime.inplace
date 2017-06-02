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

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEvent;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventListener;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
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

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements BundleExecutorEventListener {

	// The plug-in ID
	final public static String PLUGIN_ID = "no.javatime.inplace.ui"; //$NON-NLS-1$

	private static Activator activator;
	private static BundleContext context;
	private static Bundle bundle;

	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;
	// Register (extend) and track services
	private static ExtenderTracker extenderTracker;

	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		activator = this;
		Activator.context = context;
		bundle = context.getBundle();
		try {
			extenderTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
			extenderTracker.open();
			getBundleExecEventService().addListener(this);
			loadCheckedMenus();
		} catch (IllegalStateException | InPlaceException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			throw e;
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		getBundleExecEventService().removeListener(this);
		extenderTracker.close();
		super.stop(context);
		activator = null;
	}

	@Override
	public void bundleJobEvent(BundleExecutorEvent event) {

		scheduleViaBundleView(event.getJob(), event.getDelay(), false);
	}

	public static ExtenderTracker getTracker() {

		return extenderTracker;
	}

	public static BundleExecutorEventManager getBundleExecEventService() {

		return extenderTracker.bundleExecManagerExtender.getService(bundle);
	}

	public static BundleRegion getBundleRegionService() {

		return extenderTracker.bundleRegionExtender.getService(bundle);
	}

	public static BundleCommand getBundleCommandService() {

		return extenderTracker.bundleCommandExtender.getService(bundle);
	}

	public static BundleTransition getBundleTransitionService() {

		return extenderTracker.bundleTransitionExtender.getService(bundle);
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService() {

		return extenderTracker.bundleProjectCandidatesExtender.getService(bundle);
	}

	public static BundleProjectMeta getBundleProjectMetaService() {

		return extenderTracker.bundleProjectMetaExtender.getService(bundle);
	}

	public static CommandOptions getCommandOptionsService() {

		return extenderTracker.commandOptionsExtender.getService(bundle);
	}

	public static MessageOptions getMessageOptionsService() {

		return extenderTracker.messageOptionsExtender.getService(bundle);
	}

	public static ResourceState getResourceStateService() {

		return extenderTracker.resourceStateExtender.getService(bundle);
	}

	/**
	 * Schedules the specified job through the {@linkplain no.javatime.inplace.ui.views.BundleView
	 * BundleView} if it is visible. Alters the presentation of the mouse cursor to half busy and sets
	 * the part to a transient state while the specified job is running.
	 * 
	 * @param bundleJob The bundle job to schedule
	 * @param delay The delay in milliseconds before the job is scheduled
	 * @param setUser Set user to true if job progress is to be shown
	 */
	public static void scheduleViaBundleView(final WorkspaceJob bundleJob, final long delay,
			Boolean setUser) {

		bundleJob.setUser(setUser);
		IWorkbench workbench = Activator.getDefault().getWorkbench();
		if (null == workbench || workbench.isClosing()) {
			bundleJob.schedule(delay);
		} else {
			// Run synchronized in case the caller tries to wait on (join) this job 
			Activator.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					BundleView bv = (BundleView) ViewUtil.get(BundleView.ID);
					if (null != bv) {
						IWorkbenchPartSite partSite = bv.getSite();
						IWorkbenchSiteProgressService siteService = (null != partSite) ? (IWorkbenchSiteProgressService) partSite
								.getService(IWorkbenchSiteProgressService.class) : null;
								if (null != siteService) {
									siteService.showBusyForFamily(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
									siteService.showBusyForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
									siteService.showBusyForFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
									siteService.schedule(bundleJob, delay, true);
								} else {
									bundleJob.schedule(delay);
								}
					} else {
						bundleJob.schedule(delay);
					}
				}
			});
		}
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
	 * 
	 * @throws InPlaceException - if the command service is unavailable
	 * @throws ExtenderException - if the command options extender or service is unavailable
	 */
	public void loadCheckedMenus() throws InPlaceException, ExtenderException {

		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(
				ICommandService.class);
		if (null == commandService) {
			throw new InPlaceException("invalid_service", ICommandService.class.getName());
		}
		CommandOptions commandOptions = getCommandOptionsService();
		Command command = commandService.getCommand(EagerActivationHandler.commandId);
		State state = command.getState(EagerActivationHandler.getCommandStateId());
		state.setValue(commandOptions.isEagerOnActivate());

		command = commandService.getCommand(AutoExternalCommandHandler.commandId);
		state = command.getState(AutoExternalCommandHandler.getCommandStateId());
		state.setValue(commandOptions.isAutoHandleExternalCommands());

		command = commandService.getCommand(AutoRefreshHandler.commandId);
		state = command.getState(AutoRefreshHandler.getCommandStateId());
		state.setValue(commandOptions.isRefreshOnUpdate());

		command = commandService.getCommand(AutoUpdateHandler.commandId);
		state = command.getState(AutoUpdateHandler.getCommandStateId());
		state.setValue(commandOptions.isUpdateOnBuild());

		command = commandService.getCommand(DeactivateOnExitHandler.commandId);
		state = command.getState(DeactivateOnExitHandler.getCommandStateId());
		state.setValue(commandOptions.isDeactivateOnExit());

		command = commandService.getCommand(UpdateClassPathOnActivateHandler.commandId);
		state = command.getState(UpdateClassPathOnActivateHandler.getCommandStateId());
		state.setValue(commandOptions.isUpdateDefaultOutPutFolder());

		command = commandService.getCommand(UIContributorsHandler.commandId);
		state = command.getState(UIContributorsHandler.getCommandStateId());
		state.setValue(commandOptions.isAllowUIContributions());
		commandService.refreshElements(command.getId(), null);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return activator;
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
	 * 
	 * @return the active workbench window or null if not found
	 */
	public IWorkbenchWindow getActiveWorkbenchWindow() {
		if (activator == null) {
			return null;
		}
		final IWorkbench workBench = activator.getWorkbench();
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
