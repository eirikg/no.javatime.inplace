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
package no.javatime.inplace;

import no.javatime.inplace.builder.BundleExecutorInterceptor;
import no.javatime.inplace.builder.PostBuildListener;
import no.javatime.inplace.builder.PreBuildListener;
import no.javatime.inplace.builder.PreChangeListener;
import no.javatime.inplace.builder.ProjectChangeListener;
import no.javatime.inplace.bundlejobs.BundleJobListener;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.dialogs.ExternalTransition;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.statushandler.ActionSetContexts;
import no.javatime.inplace.statushandler.DynamicExtensionContribution;
import no.javatime.util.messages.Category;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A bundle manager plug-in. Provides the following functionality:
 * <ul>
 * <li>Activating and deactivating managed bundles at startup and shutdown.
 * <p>
 * <li>Management of dynamic bundles through the static declared {@code commandExtension}.
 * <li>Bundle project service for managing of bundle bundlePeojectMeta information
 * <ul/>
 */
public class Activator extends AbstractUIPlugin  {

	public static final String PLUGIN_ID = "no.javatime.inplace"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	private static Bundle bundle;

	/**
	 * Framework launching property specifying whether Equinox's FrameworkWiring implementation should
	 * refresh bundles with equal symbolic names.
	 * 
	 * <p>
	 * Default value is <b>TRUE</b> in this release of the Equinox. This default may change to
	 * <b>FALSE</b> in a future Equinox release. Therefore, code must not assume the default behavior
	 * is <b>TRUE</b> and should interrogate the value of this property to determine the behavior.
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the {@code BundleContext.getProperty}
	 * method.
	 * 
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=351519">bug 351519</a>
	 * @since 3.7.1
	 */
	public static final String REFRESH_DUPLICATE_BSN = "equinox.refresh.duplicate.bsn";
	private boolean allowRefreshDuplicateBSN;
	// Load contexts for action sets dynamically
	private ActionSetContexts actionSetContexts = new ActionSetContexts();
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow;
	// Receives notification before a project is renamed, deleted or closed
	private PreChangeListener preChangeListener;
	// Receives notification after a build
	private PostBuildListener postBuildListener;
	// Receives notification after a resource change, before build and after post build
	private PreBuildListener preBuildListener;
	// Debug listener
	private ProjectChangeListener projectChangeListener;
	// Listen to scheduled bundle jobs
	private BundleJobListener jobChangeListener = new BundleJobListener();
	// Listen to external bundle commands
	private ExternalTransition externalTransitionListener = new ExternalTransition();
	
	StatePersistParticipant saveParticipant = new StatePersistParticipant();
	private BundleExecutorInterceptor saveOptionsListener = new BundleExecutorInterceptor();
	
	// Register and track extenders and get and unget services provided by this and other bundles
	private static ExtenderTracker extenderTracker;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		plugin = this;
		Activator.context = context;
		bundle = context.getBundle();
		String refreshBSNResult = context.getProperty(REFRESH_DUPLICATE_BSN);
		allowRefreshDuplicateBSN = Boolean.TRUE.toString().equals(
				refreshBSNResult != null ? refreshBSNResult : Boolean.TRUE.toString());
		extenderTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
		extenderTracker.open();
		extenderTracker.trackOwn();
		getBundleExecutorEventService().addListener(saveOptionsListener);
		BundleTransitionListener.addBundleTransitionListener(externalTransitionListener);
		Job.getJobManager().addJobChangeListener(jobChangeListener);
		addResourceListeners();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		try {
			try {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				workspace.removeSaveParticipant(PLUGIN_ID);
			} catch (IllegalStateException e) {
				// Ignore
			}
			// Remove resource listeners as soon as possible to prevent scheduling of new bundle jobs
			removeResourceListeners();
			removeDynamicExtensions();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			// Let the builder and shutdown job finish before stopping
			getBundleExecutorEventService().removeListener(saveOptionsListener);
			BundleTransitionListener.removeBundleTransitionListener(externalTransitionListener);
			Job.getJobManager().removeJobChangeListener(jobChangeListener);
			extenderTracker.close();
			extenderTracker = null;
			super.stop(context);
			plugin = null;
			Activator.context = null;
		}
	}

	public static BundleExecutorEventManager getBundleExecutorEventService() throws ExtenderException {

		return extenderTracker.bundleExecutorEventManagerExtender.getService();
	}

	public static ResourceState getResourceStateService() {

		return extenderTracker.resourceStateExtender.getService();
	}

	public static SaveOptions getSaveOptionsService() {

		return extenderTracker.saveOptionsExtender.getService();
	}

	public static DependencyOptions getDependencyOptionsService() throws ExtenderException {

		return extenderTracker.dependencyOptionsExtender.getService(bundle);
	}

	public static BundleRegion getBundleRegionService() throws ExtenderException {

		return extenderTracker.bundleRegionExtender.getService(bundle);
	}

	public static BundleCommand getBundleCommandService() throws ExtenderException {

		return extenderTracker.bundleCommandExtender.getService(bundle);
	}

	public static BundleTransition getBundleTransitionService() throws ExtenderException {

		return extenderTracker.bundleTransitionExtender.getService(bundle);
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService()
			throws ExtenderException {

		return extenderTracker.bundleProjectCandidatesExtender.getService(bundle);
	}

	public static BundleProjectMeta getbundlePrrojectMetaService() throws ExtenderException {

		return extenderTracker.bundleProjectMetaExtender.getService(bundle);
	}

	/**
	 * Return the command preferences service
	 * 
	 * @return the command options service
	 * @throws ExtenderException if failing to get the extender service for the command options
	 * @throws InPlaceException if the command options service returns null
	 */
	public static CommandOptions getCommandOptionsService() throws ExtenderException {

		return extenderTracker.commandOptionsExtender.getService(bundle);
	}

	/**
	 * Return the message preferences service
	 * 
	 * @return the message options service
	 * @throws ExtenderException if failing to get the extender service for the message options
	 */
	public static MessageOptions getMessageOptionsService() throws ExtenderException {

		return extenderTracker.messageOptionsExtender.getService(bundle);
	}

	/**
	 * Return the bundle log service
	 * 
	 * @return the bundle log service
	 * @throws ExtenderException if failing to get the extender service for the bundle log
	 */
	public static BundleLog getBundleLogService() throws ExtenderException {

		return extenderTracker.bundleLogExtender.getService(bundle);
	}
	
	/**
	 * Log the specified status object to the bundle log
	 * 
	 * @return the bundle status message
	 * @throws ExtenderException if failing to get the extender service for the bundle log
	 * @throws BundleLogException If bundle in the specified status object is null and the
	 * {@code BundleContext} of this bundle is no longer valid
	 */
	public static String log(IBundleStatus status) throws BundleLogException, ExtenderException {

		return extenderTracker.bundleLogExtender.getService(bundle).log(status);
	}

	public static String logDirect(IBundleStatus status) throws BundleLogException, ExtenderException {

		return extenderTracker.bundleLogExtender.getService(bundle).logDirect(status);
	}

	/**
	 * Return the bundle console service view
	 * 
	 * @return the bundle log view service
	 * @throws ExtenderException if failing to get the extender service for the bundle console view
	 * @throws InPlaceException if the bundle console view service returns null
	 */
	public static BundleConsoleFactory getBundleConsoleService() throws ExtenderException {

		return extenderTracker.bundleConsoleFactoryExtender.getService(bundle);
	}

	/**
	 * Adds custom status handler, a command extension for the debug line break point and management
	 * for defining undefined action sets.
	 * @return Log status object if the extension was added. Otherwise null
	 * @throws InPlaceExeption If failing to add the status handler
	 */
	public IBundleStatus addDynamicExtensions() throws InPlaceException  {
		IBundleStatus status = DynamicExtensionContribution.INSTANCE.addCustomStatusHandler();
		// Add missing line break point command
		DynamicExtensionContribution.INSTANCE.addToggleLineBreakPointCommand();
		Boolean isInstalled = actionSetContexts.init();
		if (!isInstalled) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
							Msg.INSTALL_CONTEXT_FOR_ACTION_SET_WARN), StatusManager.LOG);
		}
		return status;
	}

	/**
	 * Removes custom status handler, command extension for the debug line break point and disposal of
	 * management for defining undefined action sets.
	 */
	private void removeDynamicExtensions() {
		actionSetContexts.dispose();
		DynamicExtensionContribution.INSTANCE.removeExtension(
				DynamicExtensionContribution.eclipseUICommandsExtensionPointId,
				DynamicExtensionContribution.eclipseToggleLineBreakPintExtension);
		DynamicExtensionContribution.INSTANCE.removeExtension(
				DynamicExtensionContribution.statusHandlerExtensionPointId,
				DynamicExtensionContribution.statusHandlerExtensionId);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getInstance() {
		return plugin;
	}

	/**
	 * The context for interacting with the FrameWork
	 * 
	 * @return the bundle execution context within the FrameWork
	 */
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Adds resource listener for project changes and builds
	 */
	public void addResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		preChangeListener = new PreChangeListener();
		workspace.addResourceChangeListener(preChangeListener, IResourceChangeEvent.PRE_CLOSE
				| IResourceChangeEvent.PRE_DELETE);
		postBuildListener = new PostBuildListener();
		workspace.addResourceChangeListener(postBuildListener, IResourceChangeEvent.POST_BUILD);
		preBuildListener = new PreBuildListener();
		workspace.addResourceChangeListener(preBuildListener, IResourceChangeEvent.PRE_BUILD);
		BundleTransitionListener.addBundleTransitionListener(preBuildListener);
		if (Category.DEBUG && Category.isEnabled(Category.listeners)) {
			projectChangeListener = new ProjectChangeListener();
			workspace.addResourceChangeListener(projectChangeListener, IResourceChangeEvent.PRE_CLOSE
					| IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_BUILD
					| IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.POST_CHANGE);
		}
	}

	/**
	 * Remove resource listener for project changes and builds
	 */
	public void removeResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(postBuildListener);
		BundleTransitionListener.removeBundleTransitionListener(preBuildListener);
		workspace.removeResourceChangeListener(preBuildListener);
		workspace.removeResourceChangeListener(preChangeListener);
		if (Category.DEBUG && Category.isEnabled(Category.listeners)) {
			// Test for null in case listeners flag was disabled after startup
			if (null != projectChangeListener) {
				workspace.removeResourceChangeListener(projectChangeListener);
			}
		}
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
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
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
	 * Access previous saved state for resource change events that occurred since the last save.
	 */
	public void processLastSavedState() {

		// Access previous saved state so change events will be created for
		// changes that have occurred since the last save
		ISavedState lastState;
		try {
			lastState = ResourcesPlugin.getWorkspace().addSaveParticipant(
					context.getBundle().getSymbolicName(), saveParticipant);
			if (lastState != null) {
				lastState.processResourceChangeEvents(postBuildListener);
			}
		} catch (CoreException e) {
			// Ignore
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

	public boolean isRefreshDuplicateBSNAllowed() {
		return allowRefreshDuplicateBSN;
	}
}
