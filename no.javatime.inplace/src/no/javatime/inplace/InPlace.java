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

import java.util.Collection;

import no.javatime.inplace.builder.PostBuildListener;
import no.javatime.inplace.builder.PreBuildListener;
import no.javatime.inplace.builder.PreChangeListener;
import no.javatime.inplace.builder.ProjectChangeListener;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.BundleJobListener;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.events.BundleJobEvent;
import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dialogs.ExternalTransition;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.ExtenderBundleTracker;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.statushandler.ActionSetContexts;
import no.javatime.inplace.statushandler.DynamicExtensionContribution;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;
import no.javatime.util.messages.views.BundleConsoleFactory;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandEvent;
import org.eclipse.core.commands.ICommandListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
/**
 * A bundle manager plug-in. Provides the following functionality:
 * <ul>
 * <li>Activating and deactivating managed bundles at startup
 * and shutdown.
 * <p>
 * <li>Management of dynamic bundles through the static declared
 * {@code bundleCommand}.
 * <li>Bundle project service for managing of bundle meta information
 * <ul/>
 */
public class InPlace extends AbstractUIPlugin implements BundleJobEventListener, ICommandListener {

	public static final String PLUGIN_ID = "no.javatime.inplace"; //$NON-NLS-1$
	private static InPlace plugin;
	private static BundleContext context;


	/**
	 * Framework launching property specifying whether Equinox's FrameworkWiring
	 * implementation should refresh bundles with equal symbolic names.
	 *
	 * <p>
	 * Default value is <b>TRUE</b> in this release of the Equinox.
	 * This default may change to <b>FALSE</b> in a future Equinox release.
	 * Therefore, code must not assume the default behavior is
	 * <b>TRUE</b> and should interrogate the value of this property to
	 * determine the behavior.
	 *
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * {@code BundleContext.getProperty} method.
	 * @see  <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=351519">bug 351519</a>
	 * @since 3.7.1
	 */
	public static final String REFRESH_DUPLICATE_BSN = "equinox.refresh.duplicate.bsn";
	private boolean allowRefreshDuplicateBSN;

	/*
	 * Dynamically load contexts for action sets
	 */
	private ActionSetContexts actionSetContexts = new ActionSetContexts();
	
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow;

	/**
	 * Receives notification before a project is deleted or closed
	 */
	private IResourceChangeListener preChangeListener;

	/**
	 * Receives notification after a build.
	 */
	private IResourceChangeListener postBuildListener;

	/**
	* Receives notification after a resource change, before pre build and after post build.
	 */
	private IResourceChangeListener preBuildListener;
	/**
	 * Debug listener.
	 */
	private IResourceChangeListener projectChangeListener;

	private BundleJobListener jobChangeListener = new BundleJobListener();

	private ExternalTransition externalTransitionListener = new ExternalTransition();
	private Command autoBuildCommand;
	
	private BundleRegion bundleRegion;

	// Don't know the interface to extend yet. Can be any interface 
	private BundleTracker<Extender<?>> extenderBundleTracker;
	private BundleTrackerCustomizer<Extender<?>> extenderBundleTrackerCustomizer;

	// Service interfaces
	private ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectTracker;
	private Extension<DependencyOptions> dependencyOptions;
	private Extension<CommandOptions> commandOptions;
	private Extension<MessageOptions> messageOptions;
	private Extension<BundleLog> traceService;


	public InPlace() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		InPlace.context = context;		
		extenderBundleTrackerCustomizer = new ExtenderBundleTracker();
		//int trackStates = Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED | Bundle.UNINSTALLED;
		extenderBundleTracker = new BundleTracker<Extender<?>>(context, Bundle.ACTIVE | Bundle.STARTING, extenderBundleTrackerCustomizer);
		extenderBundleTracker.open();
		String refreshBSNResult = context.getProperty(REFRESH_DUPLICATE_BSN);
		allowRefreshDuplicateBSN = Boolean.TRUE.toString().equals(refreshBSNResult != null ? refreshBSNResult : Boolean.TRUE.toString());

		traceService = new Extension<>(BundleLog.class);		
		commandOptions = new Extension<>(CommandOptions.class);
		dependencyOptions = new Extension<>(DependencyOptions.class);
		messageOptions = new Extension<>(MessageOptions.class);
		
		bundleProjectTracker =  new ServiceTracker<IBundleProjectService, IBundleProjectService>
				(context, IBundleProjectService.class.getName(), null);
		bundleProjectTracker.open();

		addDynamicExtensions();
		BundleManager.addBundleTransitionListener(externalTransitionListener);
		Job.getJobManager().addJobChangeListener(jobChangeListener);
		BundleJobManager.addBundleJobListener(get());
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (null != workbench) {
			ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
			autoBuildCommand = service.getCommand("org.eclipse.ui.project.buildAutomatically");
			if (autoBuildCommand.isDefined()) {
				autoBuildCommand.addCommandListener(this);
			}
		}
		BundleCommandImpl.INSTANCE.init();
		bundleRegion = BundleJobManager.getRegion();
	}	
	
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			// Remove resource listeners as soon as possible to prevent scheduling of new bundle jobs
			removeResourceListeners();
			shutDownBundles();
		} finally {
			extenderBundleTracker.close();
			extenderBundleTracker = null;
			bundleProjectTracker.close();
			bundleProjectTracker = null;		
			BundleJobManager.removeBundleJobListener(get());
			if (autoBuildCommand.isDefined()) {
				autoBuildCommand.removeCommandListener(this);
			}
			BundleManager.removeBundleTransitionListener(externalTransitionListener);
			Job.getJobManager().removeJobChangeListener(jobChangeListener);
			removeDynamicExtensions();
			super.stop(context);
			plugin = null;
			InPlace.context = null;
		}
	}

	public BundleTracker<Extender<?>> getExtenderBundleTracker() {
		return extenderBundleTracker;
	}

	/**
	 * Finds and return the bundle description for a given project.
	 * @param project to get the bundle description for
	 * @return the bundle description for the specified project
	 * @throws InPlaceException if the description could not be obtained or is invalid
	 */
	public IBundleProjectDescription getBundleDescription(IProject project) throws InPlaceException {

		IBundleProjectService bundleProjectService = null;

		bundleProjectService = bundleProjectTracker.getService();
			if (null == bundleProjectService) {
				throw new InPlaceException("invalid_project_description_service", project.getName());	
			}
		try {
			return bundleProjectService.getDescription(project);
		} catch (CoreException e) {
			// Core and Bundle exception has same message
			Throwable cause = e.getCause();
			if (null == cause || !(cause.getMessage().equals(e.getMessage()))) {
				cause = e;
			}
			throw new InPlaceException(cause, "invalid_project_description", project.getName());
		}
	}

	public IBundleProjectService getBundleProjectService(IProject project) throws InPlaceException {

		IBundleProjectService bundleProjectService = null;
		bundleProjectService = bundleProjectTracker.getService();
		if (null == bundleProjectService) {
			throw new InPlaceException("invalid_project_description_service", project.getName());	
		}
		return bundleProjectService;
	}

	public CommandOptions getCommandOptionsService() throws InPlaceException {

		CommandOptions cmdOpt = commandOptions.getService();
		if (null == cmdOpt) {
			throw new InPlaceException("invalid_service", CommandOptions.class.getName());			
		}
		return cmdOpt;
	}

	public MessageOptions msgOpt() throws InPlaceException {

		MessageOptions msgOpt = messageOptions.getService();
		if (null == msgOpt) {
			throw new InPlaceException("invalid_service", MessageOptions.class.getName());			
		}
		return msgOpt;
	}

	public DependencyOptions getDependencyOptionsService() throws InPlaceException {
		DependencyOptions dpOpt = dependencyOptions.getService();
		if (null == dpOpt) {
			throw new InPlaceException("invalid_service", DependencyOptions.class.getName());			
		}
		return dpOpt;
	}

	public BundleLog getTraceService() throws InPlaceException {

		BundleLog trace = traceService.getService();
		if (null == trace) {
			throw new InPlaceException("invalid_service", BundleLog.class.getName());			
		}
		return trace;
	}
	
	public String trace(IBundleStatus status) {
		BundleLog t = getTraceService();
		return t.trace(status);		
	}
	
//	public String trace(String key, Object... substitutions) {
//		String msg = TraceMessage.getInstance().formatString(key, substitutions);
//		IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg);
//		BundleLog t = getTraceContainerService();
//		return t.trace(status);		
//	}
	
	/**
	 * Uninstalls or deactivates all workspace bundles. All messages
	 * are sent to error console
	 */
	public void shutDownBundles() {
		// Send output to standard console when shutting down
		BundleConsoleFactory.getConsole().setSystemOutToIDEDefault();
		if (BundleProjectState.isWorkspaceNatureEnabled()) {
			try {
				BundleJob shutDownJob = null;
				Collection<IProject> projects = BundleProjectState.getNatureEnabledProjects();
				if (getCommandOptionsService().isDeactivateOnExit()) {
					shutDownJob = new DeactivateJob(DeactivateJob.deactivateOnshutDownJobName);
				} else {
					BuildErrorClosure be = new BuildErrorClosure(projects, Transition.DEACTIVATE);
					if (be.hasBuildErrors()) {
						Collection<IProject> errorClosure = be.getProjectErrorClosures();
						DeactivateJob deactivateErrorClosureJob = 
								new DeactivateJob(DeactivateJob.deactivateOnshutDownJobName, errorClosure);						
						deactivateErrorClosureJob.setUser(false);
						deactivateErrorClosureJob.schedule();
					} else {
						savePluginSettings(true, false);
					}
					projects = ProjectProperties.getPlugInProjects();
					shutDownJob = new UninstallJob(UninstallJob.shutDownJobName);
					((UninstallJob) shutDownJob).unregisterBundleProject(true);
				}
				shutDownJob.addPendingProjects(projects);
				shutDownJob.setUser(false);
				shutDownJob.schedule();
				IJobManager jobManager = Job.getJobManager();
				// Wait for build and bundle jobs
				jobManager.join(BundleJob.FAMILY_BUNDLE_LIFECYCLE, null);
				jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				if (shutDownJob.getStatusList().size() > 0) {
					final IBundleStatus multiStatus = 
							shutDownJob.createMultiStatus(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, shutDownJob.getName()));
					// The custom or standard status handler is not invoked at shutdown
					Runnable trace = new Runnable() {
						public void run() {
							trace(multiStatus);
						}
					};	
					trace.run();

					System.err.println(Msg.BEGIN_SHUTDOWN_ERROR);
					printStatus(multiStatus);
					System.err.println(Msg.END_SHUTDOWN_ERROR);
				}
			} catch (InPlaceException e) {
				ExceptionMessage.getInstance().handleMessage(e, e.getMessage());
			} catch (IllegalStateException e) {
				String msg = ExceptionMessage.getInstance()
						.formatString("job_state_exception", e.getLocalizedMessage());
				ExceptionMessage.getInstance().handleMessage(e, msg);
			} catch (Exception e) {
				ExceptionMessage.getInstance().handleMessage(e, e.getLocalizedMessage());
			}
		}				
	}
	
	/**
	 * Print status and sub status objects to system err
	 * @param status status object to print to system err
	 */
	private void printStatus(IStatus status) {
		Throwable t = status.getException();
		if (null != t) {
			t.printStackTrace();
		} else {
			System.err.println(status.getMessage());
		}
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			printStatus(children[i]);
		}
	}
	
	/**
	 * Adds custom status handler, a command extension for the debug line break point
	 * and management for defining undefined action sets.
	 */
	private void addDynamicExtensions() {
		DynamicExtensionContribution.INSTANCE.addCustomStatusHandler();
		// Add missing line break point command
		DynamicExtensionContribution.INSTANCE.addToggleLineBreakPointCommand();		
		Boolean isInstalled = actionSetContexts.init();
		if (isInstalled) {
			if (Category.getState(Category.infoMessages)) {
				UserMessage.getInstance().getString("command_action_sets");
			}
		} else {
			String msg = WarnMessage.getInstance().formatString("not_installed_context_for_action_set");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Removes custom status handler, command extension for the debug line break point
	 * and disposal of management for defining undefined action sets.
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
	 * Callback for the "Build Automatically" main menu option.
	 * Auto build is set to true when "Build Automatically" is switched on.
	 * <p>
	 * When auto build is switched on the post builder is not invoked,
	 * so an update job is scheduled here to update projects being built when
	 * the auto build option is switched on.
	 */
	@Override
	public void commandChanged(CommandEvent commandEvent) {
		if (null == plugin) {
			return;
		}
		IWorkbench workbench = getWorkbench();
		if (!BundleProjectState.isWorkspaceNatureEnabled() || 
				(null != workbench && workbench.isClosing())) {
			return;
		}
		Command autoBuildCmd = commandEvent.getCommand();
		try {
			if (autoBuildCmd.isDefined() && !ProjectProperties.isAutoBuilding()) {
				if (getCommandOptionsService().isUpdateOnBuild()) {
					BundleJobManager.getRegion().setAutoBuildChanged(true);
					// Wait for builder to star. The post build listener does not
					// always receive all projects to update when auto build is switched on
					BundleJobManager.addBundleJob(new UpdateJob(UpdateJob.updateJobName), 1000);
				}
			} else {
				BundleJobManager.getRegion().setAutoBuildChanged(false);			
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static InPlace get() {
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

	static public IEclipsePreferences getEclipsePreferenceStore() {
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}

	/**
	 * Save various settings through the preference service at the workspace level
	 * <p>
	 * When <code>allResolve</code> parameter is true and a project is activated the
	 * stored state is set to resolve.
	 * 
	 * @param flush true to save settings to storage
	 * @param allResolve true to save state as resolve for all activated bundle projects
	 */
	public void savePluginSettings(Boolean flush, Boolean allResolve) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
			return;
		}
		try {
			prefs.clear();
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_clearing_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			return; // Use existing values
		}
		if (allResolve) {
			if (BundleProjectState.isWorkspaceNatureEnabled()) {
				for (IProject project : BundleProjectState.getNatureEnabledProjects()) {
					try {
						String symbolicKey = bundleRegion.getSymbolicKey(null, project);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						prefs.putInt(symbolicKey, Bundle.RESOLVED);					
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, project, msg, null),
								StatusManager.LOG);
					}
				}
			} 			
		} else {
			if (bundleRegion.isBundleWorkspaceActivated()) {
				for (Bundle bundle : bundleRegion.getBundles()) {
					try {
						String symbolicKey = bundleRegion.getSymbolicKey(bundle, null);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						if ((bundle.getState() & (Bundle.RESOLVED)) != 0) {
							prefs.putInt(symbolicKey, bundle.getState());
						}
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
								StatusManager.LOG);
					}
				} 
			} else {
				for (IProject project : BundleProjectState.getProjects()) {
					try {					
						Transition transition = BundleJobManager.getTransition().getTransition(project);
						if (!BundleProjectState.isNatureEnabled(project) && 
								transition == Transition.UNINSTALL) {
							String symbolicKey = bundleRegion.getSymbolicKey(null, project);
							if (symbolicKey.isEmpty()) {
								continue;
							}
							prefs.putInt(symbolicKey, transition.ordinal());					
						}
					} catch (ProjectLocationException e) {
						// Ignore. Will be defined as no transition when loaded again
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
								StatusManager.LOG);
					}
				}
			}
		}
		try {
			if (flush) {
				prefs.flush();
			}
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_flushing_preference_store");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Access previous saved state for resource change events that occurred since the last save. Restore checked
	 * menu entries through the command service and other settings through the preference service
	 * 
	 * @param sync true to prevent others changing the settings
	 */
	public void processLastSavedState(Boolean sync) {

		// Access previous saved state so change events will be created for
		// changes that have occurred since the last save
		ISaveParticipant saveParticipant = new WorkspaceSaveParticipant();
		ISavedState lastState;
		try {
			lastState = ResourcesPlugin.getWorkspace().addSaveParticipant(
					get().getBundle().getSymbolicName(), saveParticipant);
			if (lastState != null) {
				lastState.processResourceChangeEvents(postBuildListener);
			}
		} catch (CoreException e) {
			// Ignore
		}
	}	

	public boolean isRefreshDuplicateBSNAllowed() {
		return allowRefreshDuplicateBSN;
	}

	@Override
	public void bundleJobEvent(BundleJobEvent event) {		
	}
}
