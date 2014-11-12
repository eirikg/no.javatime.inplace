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
import java.util.Collections;

import no.javatime.inplace.builder.PostBuildListener;
import no.javatime.inplace.builder.PreBuildListener;
import no.javatime.inplace.builder.PreChangeListener;
import no.javatime.inplace.builder.ProjectChangeListener;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.BundleJobListener;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.events.BundleJobEvent;
import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dialogs.ExternalTransition;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.statushandler.ActionSetContexts;
import no.javatime.inplace.statushandler.DynamicExtensionContribution;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

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

/**
 * A bundle manager plug-in. Provides the following functionality:
 * <ul>
 * <li>Activating and deactivating managed bundles at startup and shutdown.
 * <p>
 * <li>Management of dynamic bundles through the static declared {@code bundleCommand}.
 * <li>Bundle project service for managing of bundle meta information
 * <ul/>
 */
public class InPlace extends AbstractUIPlugin implements BundleJobEventListener, ICommandListener {

	public static final String PLUGIN_ID = "no.javatime.inplace"; //$NON-NLS-1$
	private static InPlace plugin;
	private static BundleContext context;
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
	// Dynamically load contexts for action sets
	private ActionSetContexts actionSetContexts = new ActionSetContexts();
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow;
	// Receives notification before a project is renamed, deleted or closed
	private IResourceChangeListener preChangeListener;
	// Receives notification after a build.
	private IResourceChangeListener postBuildListener;
	// Receives notification after a resource change, before build and after post build.
	private IResourceChangeListener preBuildListener;
	// Debug listener.
	private IResourceChangeListener projectChangeListener;
	// Listens to scheduled bundles
	private BundleJobListener jobChangeListener = new BundleJobListener();
	// Listen to external bundle commands
	private ExternalTransition externalTransitionListener = new ExternalTransition();
	// Listen to toggling of auto build
	private Command autoBuildCommand;
	// Register (extend) services provided by other bundles  
	private ExtenderBundleTracker extenderBundleTracker;
	// Workspace bundle region
	private static Extension<BundleRegion> bundleRegion;
	// Bundle life cycle commands
	private static Extension<BundleCommand> bundleCommand;
	// Bundle transitions and pending transitions
	private static Extension<BundleTransition> bundleTransition;
	// Bundle project and projects access
	private static Extension<BundleProjectCandidates> bundleProjectCandidates;
	// Bundle project meta information
	private static Extension<BundleProjectMeta> bundlePrrojectMeta;
	// Dependency closure options
	private Extension<DependencyOptions> dependencyOptions;
	// Bundle command options
	private Extension<CommandOptions> commandOptions;
	// Logging and user message options
	private Extension<MessageOptions> messageOptions;
	// The bundle console page for redirection of system out and err
	private Extension<BundleConsoleFactory> bundleConsoleFactory;
	// Log for bundle commands
	private Extension<BundleLog> bundleLog;
	
	
	public InPlace() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		InPlace.context = context;
		extenderBundleTracker = new ExtenderBundleTracker(context, Bundle.ACTIVE, null);
		extenderBundleTracker.open();
		String refreshBSNResult = context.getProperty(REFRESH_DUPLICATE_BSN);
		allowRefreshDuplicateBSN = Boolean.TRUE.toString().equals(
				refreshBSNResult != null ? refreshBSNResult : Boolean.TRUE.toString());
		bundleRegion = Extenders.getExtension(BundleRegion.class.getName());
		bundleCommand = Extenders.getExtension(BundleCommand.class.getName());
		bundleTransition = Extenders.getExtension(BundleTransition.class.getName());
		bundleProjectCandidates = Extenders.getExtension(BundleProjectCandidates.class.getName());
		bundlePrrojectMeta = Extenders.getExtension(BundleProjectMeta.class.getName());
		commandOptions = Extenders.getExtension(CommandOptions.class.getName()); 
		dependencyOptions = Extenders.getExtension(DependencyOptions.class.getName()); 
		messageOptions = Extenders.getExtension(MessageOptions.class.getName()); 
		bundleConsoleFactory = Extenders.getExtension(BundleConsoleFactory.class.getName()); 
		bundleLog = Extenders.getExtension(BundleLog.class.getName());
		addDynamicExtensions();
		//InPlace.context.addBundleListener(bundleEvents);
		BundleTransitionListener.addBundleTransitionListener(externalTransitionListener);
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
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			// InPlace.context.removeBundleListener(bundleEvents);
			// Remove resource listeners as soon as possible to prevent scheduling of new bundle jobs
			removeResourceListeners();
			shutDownBundles();
		} finally {
			extenderBundleTracker.close();
			extenderBundleTracker = null;
			BundleJobManager.removeBundleJobListener(get());
			if (autoBuildCommand.isDefined()) {
				autoBuildCommand.removeCommandListener(this);
			}
			BundleTransitionListener.removeBundleTransitionListener(externalTransitionListener);
			Job.getJobManager().removeJobChangeListener(jobChangeListener);
			removeDynamicExtensions();
			super.stop(context);
			plugin = null;
			InPlace.context = null;
		}
	}
	
	public static BundleRegion getBundleRegionService() throws InPlaceException, ExtenderException {

		BundleRegion br = bundleRegion.getService(context.getBundle());
		if (null == br) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, BundleRegion.class.getName());			
		}
		return br;
	}

	public static BundleCommand getBundleCommandService() throws InPlaceException, ExtenderException {

		BundleCommand br = bundleCommand.getService(context.getBundle());
		if (null == br) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, BundleCommand.class.getName());			
		}
		return br;
	}
	
	public static BundleTransition getBundleTransitionService() throws InPlaceException, ExtenderException {
		
		BundleTransition bt = bundleTransition.getService(context.getBundle());
		if (null == bt) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, BundleTransition.class.getName());			
		}
		return bt;
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService() throws InPlaceException, ExtenderException {
		
		BundleProjectCandidates bp = bundleProjectCandidates.getService(context.getBundle());
		if (null == bp) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, BundleProjectCandidates.class.getName());			
		}
		return bp;
	}

	public static BundleProjectMeta getbundlePrrojectMetaService() throws InPlaceException, ExtenderException {
		
		BundleProjectMeta bpd = bundlePrrojectMeta.getService(context.getBundle());
		if (null == bpd) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, BundleProjectMeta.class.getName());			
		}
		return bpd;
	}

	/**
	 * Return the command preferences service
	 * 
	 * @return the command options service
	 * @throws ExtenderException if failing to get the extender service for the command options
	 * @throws InPlaceException if the command options service returns null
	 */
	public CommandOptions getCommandOptionsService() throws InPlaceException, ExtenderException {
		
		return getService(commandOptions);
	}

	/**
	 * Return the message preferences service
	 * 
	 * @return the message options service
	 * @throws ExtenderException if failing to get the extender service for the message options
	 * @throws InPlaceException if the message options service returns null
	 */
	public MessageOptions getMsgOpt() throws InPlaceException, ExtenderException {

		return getService(messageOptions);
	}

	/**
	 * Return the dependency preferences service
	 * 
	 * @return the dependency options service
	 * @throws ExtenderException if failing to get the extender service for the dependency options
	 * @throws InPlaceException if the bundle log service returns null
	 */
	public DependencyOptions getDependencyOptionsService() throws InPlaceException, ExtenderException {
		return getService(dependencyOptions);
	}
	
	/**
	 * Return the bundle console service view
	 * 
	 * @return the bundle log view service
	 * @throws ExtenderException if failing to get the extender service for the bundle console view
	 * @throws InPlaceException if the bundle console view service returns null
	 */
	public BundleConsoleFactory getBundleConsoleService() throws InPlaceException, ExtenderException {
		return getService(bundleConsoleFactory);
	}

	/**
	 * Log the specified status object to the bundle log
	 * 
	 * @return the bundle status message 
	 * @throws ExtenderException if failing to get the extender service for the bundle log
	 * @throws InPlaceException if the bundle log service returns null
	 */
	public String log(IBundleStatus status) throws InPlaceException, ExtenderException {
		BundleLog t = getService(bundleLog);
		return t.trace(status);
	}

	/**
	 * Utility returning a service for an extension but never null 
	 * 
	 * @return the service
	 * @throws ExtenderException if failing to get the extender service 
	 * @throws InPlaceException if the service returns null
	 */
	private <S> S getService(Extension<S> extension) throws InPlaceException, ExtenderException {

		S s = extension.getService(context.getBundle());
		if (null == s) {
			throw new InPlaceException(Msg.GET_SERVICE_ERROR, extension.getExtender().getServiceInterfaceName());
		}
		return s;
}

	/**
	 * Uninstalls or deactivates (optional) all workspace bundles. Bundle closures with build errors
	 * are deactivated. Any runtime errors are sent to error console
	 */
	public void shutDownBundles() {
		// Send output to standard console when shutting down
		BundleConsoleFactory bundleConsoleService = getBundleConsoleService();
		// Ignore and send to current setting if null
		if (null != bundleConsoleService) {
			bundleConsoleService.setSystemOutToIDEDefault();
		}
		BundleRegion bundleRegion = getBundleRegionService();
		if (bundleRegion.isRegionActivated()) {
			try {
				BundleJob shutDownJob = null;
				Collection<IProject> activatedProjects = bundleRegion.getActivatedProjects();
				if (getCommandOptionsService().isDeactivateOnExit()) {
					shutDownJob = new DeactivateJob(DeactivateJob.deactivateOnshutDownJobName);
				} else {
					Collection<IProject> deactivatedProjects = deactivateBuildErrorClosures(activatedProjects);
					if (deactivatedProjects.size() > 0) {
						activatedProjects.removeAll(deactivatedProjects);
					} else {
						savePluginSettings(true, false);
					}
					if (activatedProjects.size() > 0) {
						BundleProjectCandidates bundleProjectcandidates = getBundleProjectCandidatesService();
						savePluginSettings(true, false);
						activatedProjects = bundleProjectcandidates.getBundleProjects();
						shutDownJob = new UninstallJob(UninstallJob.shutDownJobName);
						((UninstallJob) shutDownJob).unregisterBundleProject(true);
					}
				}
				if (activatedProjects.size() > 0) {
					shutDownJob.addPendingProjects(activatedProjects);
					shutDownJob.setUser(false);
					shutDownJob.schedule();
				}
				IJobManager jobManager = Job.getJobManager();
				// Wait for build and bundle jobs
				jobManager.join(BundleJob.FAMILY_BUNDLE_LIFECYCLE, null);
				jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				if (shutDownJob.getErrorStatusList().size() > 0) {
					final IBundleStatus multiStatus = shutDownJob.createMultiStatus(new BundleStatus(
							StatusCode.ERROR, InPlace.PLUGIN_ID, shutDownJob.getName()));
					// The custom or standard status handler is not invoked at shutdown
					Runnable trace = new Runnable() {
						public void run() {
							log(multiStatus);
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
				String msg = ExceptionMessage.getInstance().formatString("job_state_exception",
						e.getLocalizedMessage());
				ExceptionMessage.getInstance().handleMessage(e, msg);
			} catch (Exception e) {
				ExceptionMessage.getInstance().handleMessage(e, e.getLocalizedMessage());
			}
		} else {
			savePluginSettings(true, false);
		}
	}

	/**
	 * Deactivate bundle projects with build errors to prevent resolve and start of bundles with build
	 * errors closures at startup by the {@link ActivateBundleJob}. Note that the initial state of
	 * both deactivated and activated bundles are initially uninstalled at startup. Closures to
	 * deactivate are:
	 * <p>
	 * <ol>
	 * <li><b>Requiring deactivate closures</b>
	 * <p>
	 * <br>
	 * <b>Activated requiring closure.</b> An activated bundle is not deactivated when there exists
	 * activated bundles with build errors requiring capabilities from this activated bundle project
	 * to be resolved at startup. This closure overlap with the Activated providing closure
	 * <p>
	 * <br>
	 * <b>Deactivated requiring closure.</b> Deactivated bundle projects with build errors requiring
	 * capabilities from a project to activate is allowed due to the deactivated requiring bundle is
	 * not forced to be activated.
	 * <p>
	 * <br>
	 * <li><b>Providing resolve closures</b>
	 * <p>
	 * <br>
	 * <b>Deactivated providing closure.</b> Resolve is rejected when deactivated bundles with build
	 * errors provides capabilities to projects to resolve (and start). This closure require the
	 * providing bundles to be activated when the requiring bundles are resolved. This is usually an
	 * impossible position. Activating and updating does not allow a requiring bundle to activate
	 * without activating the providing bundle.
	 * <p>
	 * <br>
	 * <b>Activated providing closure.</b> It is illegal to resolve an activated project when there
	 * are activated bundles with build errors that provides capabilities to the project to resolve.
	 * The requiring bundles to resolve will force the providing bundles with build errors to resolve.
	 * </ol>
	 * 
	 * @param activatedProjects all activated bundle projects
	 * @return projects that are deactivated or an empty set
	 */
	private Collection<IProject> deactivateBuildErrorClosures(Collection<IProject> activatedProjects) {

		DeactivateJob deactivateErrorClosureJob = new DeactivateJob(
				DeactivateJob.deactivateOnshutDownJobName);
		try {
			// Deactivated and activated providing closure. Deactivated and activated projects with build
			// errors providing capabilities to project to resolve (and start) at startup
			BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.DEACTIVATE,
					Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
			if (be.hasBuildErrors()) {
				Collection<IProject> errorClosure = be.getBuildErrorClosures();
				deactivateErrorClosureJob.addPendingProjects(errorClosure);
				deactivateErrorClosureJob.setUser(false);
				deactivateErrorClosureJob.schedule();
				return errorClosure;
			}
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg,
					null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		}
		return Collections.<IProject> emptySet();
	}

	/**
	 * Print status and sub status objects to system err
	 * 
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
	 * Adds custom status handler, a command extension for the debug line break point and management
	 * for defining undefined action sets.
	 */
	private void addDynamicExtensions() {
		DynamicExtensionContribution.INSTANCE.addCustomStatusHandler();
		// Add missing line break point command
		DynamicExtensionContribution.INSTANCE.addToggleLineBreakPointCommand();
		Boolean isInstalled = actionSetContexts.init();
		if (!isInstalled) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
							Msg.INSTALL_CONTEXT_FOR_ACTION_SET_WARN), StatusManager.LOG);
		}
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
	 * Callback for the "Build Automatically" main menu option. Auto build is set to true when
	 * "Build Automatically" is switched on.
	 * <p>
	 * When auto build is switched on the post builder is not invoked, so an update job is scheduled
	 * here to update projects being built when the auto build option is switched on.
	 */
	@Override
	public void commandChanged(CommandEvent commandEvent) {
		if (null == plugin) {
			return;
		}
		IWorkbench workbench = getWorkbench();
		if (!getBundleRegionService().isRegionActivated()
				|| (null != workbench && workbench.isClosing())) {
			return;
		}
		Command autoBuildCmd = commandEvent.getCommand();
		try {
			if (autoBuildCmd.isDefined() && !getBundleProjectCandidatesService().isAutoBuilding()) {
				if (getCommandOptionsService().isUpdateOnBuild()) {
					getBundleRegionService().setAutoBuildChanged(true);
					// Wait for builder to star. The post build listener does not
					// always receive all projects to update when auto build is switched on
					BundleJobManager.addBundleJob(new UpdateJob(UpdateJob.updateJobName), 1000);
				}
			} else {
				getBundleRegionService().setAutoBuildChanged(false);
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

	static public IEclipsePreferences getEclipsePreferenceStore() {
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}

	/**
	 * Save various settings through the preference service at the workspace level
	 * <p>
	 * When <code>allResolve</code> parameter is true and a project is activated the stored state is
	 * set to resolve.
	 * 
	 * @param flush true to save settings to storage
	 * @param allResolve true to save state as resolve for all activated bundle projects
	 */
	public void savePluginSettings(Boolean flush, Boolean allResolve) {

		IEclipsePreferences prefs = getEclipsePreferenceStore();
		if (null == prefs) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg), StatusManager.LOG);
			return;
		}
		try {
			prefs.clear();
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_clearing_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg, e), StatusManager.LOG);
			return; // Use existing values
		}
		if (allResolve) {
			BundleRegion bundleRegion = getBundleRegionService();
			if (bundleRegion.isRegionActivated()) {
				Collection<IProject> natureEnabled = bundleRegion.getActivatedProjects();
				for (IProject project : natureEnabled) {
					try {
						String symbolicKey = getBundleRegionService().getSymbolicKey(null, project);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						prefs.putInt(symbolicKey, Bundle.RESOLVED);
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, project, msg, null),
								StatusManager.LOG);
					}
				}
			}
		} else {
			BundleRegion region = getBundleRegionService();
			if (region.isRegionActivated()) {
				for (Bundle bundle : region.getBundles()) {
					try {
						String symbolicKey = region.getSymbolicKey(bundle, null);
						if (symbolicKey.isEmpty()) {
							continue;
						}
						if ((bundle.getState() & (Bundle.RESOLVED)) != 0) {
							prefs.putInt(symbolicKey, bundle.getState());
						}
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg), StatusManager.LOG);
					}
				}
			} else {
				BundleProjectCandidates bundleProject = getBundleProjectCandidatesService();
				Collection<IProject> projects = bundleProject.getBundleProjects();
				for (IProject project : projects) {
					try {
						Transition transition = InPlace.getBundleTransitionService().getTransition(project);
						// if (!BundleProjectCandidatesImpl.INSTANCE.isNatureEnabled(project) &&
						if (transition == Transition.REFRESH || transition == Transition.UNINSTALL) {
							String symbolicKey = getBundleRegionService().getSymbolicKey(null, project);
							if (symbolicKey.isEmpty()) {
								continue;
							}
							prefs.putInt(symbolicKey, transition.ordinal());
						}
					} catch (ProjectLocationException e) {
						// Ignore. Will be defined as no transition when loaded again
					} catch (IllegalStateException e) {
						String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg), StatusManager.LOG);
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
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg), StatusManager.LOG);
		}
	}

	/**
	 * Access previous saved state for resource change events that occurred since the last save.
	 * Restore checked menu entries through the command service and other settings through the
	 * preference service
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
