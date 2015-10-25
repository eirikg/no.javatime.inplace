package no.javatime.inplace.region;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.closure.ExternalDuplicates;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.resolver.BundleResolveHookFactory;
import no.javatime.inplace.region.state.BundleStateEvents;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "no.javatime.inplace.region"; //$NON-NLS-1$
	private static Activator plugin;
	private static BundleContext context;
	private static Bundle bundle;

	private BundleStateEvents bundleEvents = new BundleStateEvents();
	private ExternalDuplicates duplicateEvents = new ExternalDuplicates();

	private static ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectTracker;

	/**
	 * Factory creating resolver hook objects for filtering and detection of duplicate bundle
	 * instances
	 */
	protected BundleResolveHookFactory resolverHookFactory = new BundleResolveHookFactory();

	/**
	 * Service registrator for the resolve hook factory.
	 */
	private ServiceRegistration<ResolverHookFactory> resolveHookRegistration;

	// Register and track extenders and get and unget services provided by this and other bundles
	private static ExtenderTracker extenderTracker;
	
	public Activator() {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		bundle = context.getBundle();
		registerResolverHook();
		Activator.context.addBundleListener(bundleEvents);
		BundleCommandImpl bundleCommandImpl = BundleCommandImpl.INSTANCE;
		bundleCommandImpl.initFrameworkWiring();
		extenderTracker = new ExtenderTracker(context, Bundle.INSTALLED | Bundle.UNINSTALLED | Bundle.ACTIVE, null);
		extenderTracker.open();
		extenderTracker.trackOwn();		
		bundleProjectTracker = new ServiceTracker<IBundleProjectService, IBundleProjectService>(
				context, IBundleProjectService.class.getName(), null);
		bundleProjectTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		Activator.context.removeBundleListener(duplicateEvents);
		Activator.context.removeBundleListener(bundleEvents);
		bundleProjectTracker.close();
		bundleProjectTracker = null;
		extenderTracker.close();
		extenderTracker = null;
		unregisterResolverHook();
		super.stop(context);
		plugin = null;
		Activator.context = null;
	}

	public static BundleRegion getBundleRegionService() throws ExtenderException {

		return extenderTracker.bundleRegionExtender.getService();
	}

	public static BundleCommand getBundleCommandService(Bundle bundle) throws ExtenderException {

		return extenderTracker.bundleCommandExtender.getService(bundle);
	}

	public static BundleTransition getBundleTransitionService(Bundle bundle) throws ExtenderException {

		return extenderTracker.bundleTransitionExtender.getService(bundle);
	}

	public static BundleProjectCandidates getBundleProjectCandidatesService(Bundle bundle)
			throws ExtenderException {

		return extenderTracker.bundleProjectCandidatesExtender.getService(bundle);
	}

	public static BundleProjectMeta getbundlePrrojectMetaService(Bundle bundle) throws ExtenderException {

		return extenderTracker.bundleProjectMetaExtender.getService(bundle);
	}

	/**
	 * Get the resolver hook factory
	 * 
	 * @return the resolver hook factory object
	 */
	public final BundleResolveHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	public void registerResolverHook() {
		resolveHookRegistration = getContext().registerService(ResolverHookFactory.class,
				resolverHookFactory, null);
	}

	/**
	 * Unregister the resolver hook.
	 * <p>
	 * This is redundant. Unregistered by the OSGi service implementation
	 */
	public void unregisterResolverHook() {
		resolveHookRegistration.unregister();
	}
	
	/**
	 * Get the duplicate event handler
	 * @return The duplicate event handler
	 */
	public ExternalDuplicates getDuplicateEvents() {
		return duplicateEvents;
	}

	/**
	 * Finds and return the bundle description for a given project.
	 * 
	 * @param project to get the bundle description for
	 * @return the bundle description for the specified project
	 * @throws InPlaceException if the description could not be obtained or is invalid
	 */
	public static IBundleProjectDescription getBundleDescription(IProject project) throws InPlaceException {

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

	public static IBundleProjectService getBundleProjectService(IProject project) throws InPlaceException {

		IBundleProjectService bundleProjectService = null;
		bundleProjectService = bundleProjectTracker.getService();
		if (null == bundleProjectService) {
			throw new InPlaceException("invalid_project_description_service", project.getName());
		}
		return bundleProjectService;
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

	public static MessageOptions getMessageOptionsService() throws ExtenderException {

		return extenderTracker.messageOptionsExtender.getService(bundle);
	}

	public static DependencyOptions getDependencyOptionsService() throws InPlaceException {

		return extenderTracker.dependencyOptionsExtender.getService(bundle);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static BundleContext getContext() {
		return context;
	}
}
