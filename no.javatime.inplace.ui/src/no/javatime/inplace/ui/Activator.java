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
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
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

	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;

	// Register (extend) services for use facilitated by other bundles
	private static ExtenderTracker extenderBundleTracker;

	private Extension<BundleRegion> regionExtension;
	private static BundleRegion region;

	private Extension<BundleCommand> commandExtension;
	private static BundleCommand command;

	private Extension<BundleTransition> transitionExtension;
	private static BundleTransition transition;

	private Extension<CommandOptions> commandOptionsExtension;
	private static CommandOptions commandOptions;

	private Extension<ResourceState> resourceStateExtension;
	private static ResourceState resourceState;

	// Bundle candidate projects
	private Extension<BundleProjectCandidates> projectCandidatesExtension;
	private static BundleProjectCandidates projectCandidates;

	// Bundle project meta information
	private Extension<BundleProjectMeta> projectMetaExtension;
	private static BundleProjectMeta projectMeta;

	private Extension<BundleExecutorEventManager> eventExecManagerExtension;
	private static BundleExecutorEventManager eventExcecManager;

	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		activator = this;
		Activator.context = context;
		try {
			extenderBundleTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
			extenderBundleTracker.open();

			eventExecManagerExtension = getExtension(BundleExecutorEventManager.class.getName());
			eventExcecManager = eventExecManagerExtension.getTrackedService();
			eventExcecManager.addListener(this);

			regionExtension = getTrackedExtension(BundleRegion.class.getName());
			region = regionExtension.getTrackedService();

			commandExtension = getTrackedExtension(BundleCommand.class.getName());
			command = commandExtension.getTrackedService();

			transitionExtension = getTrackedExtension(BundleTransition.class.getName());
			transition = transitionExtension.getTrackedService();

			projectCandidatesExtension = getTrackedExtension(BundleProjectCandidates.class.getName());
			projectCandidates = projectCandidatesExtension.getTrackedService();

			projectMetaExtension = getTrackedExtension(BundleProjectMeta.class.getName());
			projectMeta = projectMetaExtension.getTrackedService();

			commandOptionsExtension = getTrackedExtension(CommandOptions.class.getName());
			commandOptions = commandOptionsExtension.getTrackedService();

			resourceStateExtension = getTrackedExtension(ResourceState.class.getName());
			resourceState = resourceStateExtension.getTrackedService();

			loadCheckedMenus();
		} catch (IllegalStateException | InPlaceException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		regionExtension.closeTrackedService();
		commandExtension.closeTrackedService();
		transitionExtension.closeTrackedService();
		projectCandidatesExtension.closeTrackedService();
		projectMetaExtension.closeTrackedService();
		commandOptionsExtension.closeTrackedService();
		resourceStateExtension.closeTrackedService();
		eventExcecManager.removeListener(this);
		eventExecManagerExtension.closeTrackedService();
		extenderBundleTracker.close();
		super.stop(context);
		activator = null;
	}

	@Override
	public void bundleJobEvent(BundleExecutorEvent event) {
		System.out.println("Added bundle job: " + event.getJob() + " State: " + event.getJobState());
		scheduleViaBundleView(event.getJob(), event.getDelay(), false);
	}

	/**
	 * Get an extension returned based on ranking order
	 * 
	 * @param serviceInterfaceName the interface service name used to locate the extension
	 * @return an extension located by the specified service interface name
	 * @throws ExtenderException if failed to get the extender or the extension is null
	 */
	public static <S> Extension<S> getExtension(String serviceInterfaceName) throws ExtenderException {

		Extension<S> extension = Extenders.getExtension(serviceInterfaceName, context.getBundle());
		if (null == extension) {
			throw new ExtenderException(NLS.bind(Msg.NULL_EXTENSION_EXP, context.getBundle()));
		}
		return extension;
	}

	/**
	 * Create an extension of the extender registered with the specified interface class
	 * <p>
	 * This extension access method has an additional check to verify if the interface class is loaded
	 * or not. A failure first indicates that the extender for the extension is not registered or the
	 * bundle hosting the class is not started yet and/or not loaded by or on behalf of the hosting
	 * bundle
	 * 
	 * @param interfaceClass service interface class
	 * @user The bundle using the extension
	 * @return the extension interface
	 * @throws ExtenderException if fail to get the registered extension due to no registered service
	 * with the specified interface name
	 */
	public static <E> Extension<E> getExtension(Class<E> interfaceClass) throws ExtenderException {

		Extension<E> ext = null;
		try {
			Class<?> c = Class.forName(interfaceClass.getName());
			ext = getExtension(c.getName());
			if (null == ext) {
				throw new ExtenderException("Null extender for the using {0} bundle", context.getBundle());
			}
		} catch (ClassNotFoundException e) {
			// Bundle hosting the class is not started yet and/or not loaded by or on behalf of the
			// hosting
			// bundle
			throw new ExtenderException(e, "Interface class for extension not found for user bundle {0}",
					context.getBundle());
		}
		return ext;
	}

	/**
	 * Get a tracked extension from an extender that is tracked by this bundle.
	 * <p>
	 * If more than one service is registered under the same interface name, ranking order is applied
	 * 
	 * @param serviceInterfaceName the interface service name used to locate the extension
	 * @return an extension located by the specified service interface name
	 * @throws ExtenderException if the extender has not been tracked or the extender or extension is
	 * null
	 */
	public static <S> Extension<S> getTrackedExtension(String serviceInterfaceName)
			throws ExtenderException {

		@SuppressWarnings("unchecked")
		Extender<S> extender = (Extender<S>) extenderBundleTracker
				.getTrackedExtender(serviceInterfaceName);
		if (null == extender) {
			throw new ExtenderException(NLS.bind(Msg.NULL_EXTENDER_EXP, serviceInterfaceName));
		}
		Extension<S> extension = extender.getExtension(serviceInterfaceName, context.getBundle());
		if (null == extension) {
			throw new ExtenderException(NLS.bind(Msg.NULL_EXTENSION_EXP, context.getBundle()));
		}
		return extension;
	}

	public static BundleExecutorEventManager getBundleJobEventService() {

		return eventExcecManager;
	}

	public static BundleRegion getBundleRegionService() {

		return region;
	}

	public static BundleCommand getBundleCommandService() {

		return command;
	}

	public static BundleTransition getBundleTransitionService() {

		return transition;
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService() {

		return projectCandidates;
	}

	public static BundleProjectMeta getBundleProjectMetaService() {

		return projectMeta;
	}

	public static CommandOptions getCommandOptionsService() {

		return commandOptions;
	}

	public static ResourceState getResourceStateService() {

		return resourceState;
	}

	/**
	 * Returning a service for a tracked extender but never null
	 * 
	 * @param Type of service
	 * @return the service
	 * @throws ExtenderException if failing to get the extender service
	 * @throws InPlaceException if the service returns null
	 */
	public static <S> S getTrackedService(String serviceInterfaceName) throws ExtenderException {

		@SuppressWarnings("unchecked")
		Extender<S> extender = (Extender<S>) extenderBundleTracker
				.getTrackedExtender(serviceInterfaceName);
		if (null == extender) {
			throw new ExtenderException(NLS.bind(Msg.GET_EXTENDER_EXP, serviceInterfaceName));
		}
		S s = extender.getService(context.getBundle());
		if (null == s) {
			throw new ExtenderException(NLS.bind(Msg.GET_SERVICE_EXP, serviceInterfaceName));
		}
		return s;
	}

	public static <S> S getTrackedService(Extender<S> extender) throws ExtenderException {

		if (null == extender) {
			throw new ExtenderException(NLS.bind(Msg.NULL_EXTENDER_EXP, context.getBundle()));
		}
		S s = extender.getService(context.getBundle());
		if (null == s) {
			throw new ExtenderException(NLS.bind(Msg.GET_SERVICE_EXP, extender.getServiceInterfaceName()));
		}
		return s;
	}

	public static <S> Extender<S> getTrackedExtender(String serviceInterfaceName)
			throws ExtenderException {

		@SuppressWarnings("unchecked")
		Extender<S> extender = (Extender<S>) extenderBundleTracker
				.getTrackedExtender(serviceInterfaceName);
		if (null == extender) {
			throw new ExtenderException(NLS.bind(Msg.GET_EXTENDER_EXP, serviceInterfaceName));
		}
		return extender;
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
		Activator.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				BundleView bv = (BundleView) ViewUtil.get(BundleView.ID);
				if (null != bv) {
					IWorkbenchSiteProgressService siteService = null;
					IWorkbenchPartSite partSite = bv.getSite();
					if (null != partSite) {
						siteService = (IWorkbenchSiteProgressService) partSite
								.getAdapter(IWorkbenchSiteProgressService.class);
					}
					if (null != siteService) {
						siteService.showBusyForFamily(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
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
		Extension<CommandOptions> commandOptionsExt = getExtension(CommandOptions.class.getName());
		CommandOptions commandOptions = commandOptionsExt.getTrackedService();
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
		commandOptionsExt.closeTrackedService();
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
