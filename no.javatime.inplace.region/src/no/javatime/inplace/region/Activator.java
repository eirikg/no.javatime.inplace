package no.javatime.inplace.region;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
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
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.project.BundleProjectCandidatesImpl;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
import no.javatime.inplace.region.resolver.BundleResolveHookFactory;
import no.javatime.inplace.region.state.BundleStateEvents;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
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

	private BundleStateEvents bundleEvents = new BundleStateEvents();;

	private static ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectTracker;
	private Extension<MessageOptions> messageOptions;
	private Extension<CommandOptions> commandOptions;
	private Extension<DependencyOptions> dependencyOptions;

	private static Extender<BundleCommand> extenderCommand;
	private static Extender<BundleRegion> extenderRegion;
	private static Extender<BundleTransition> extenderTransition;
	private static Extender<BundleProjectCandidates> extenderBundleProjectCandidates;
	private static Extender<BundleProjectMeta> extenderBundleProjectMeta;

	/**
	 * Factory creating resolver hook objects for filtering and detection of duplicate bundle
	 * instances
	 */
	protected BundleResolveHookFactory resolverHookFactory = new BundleResolveHookFactory();

	/**
	 * Service registrator for the resolve hook factory.
	 */
	private ServiceRegistration<ResolverHookFactory> resolveHookRegistration;

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
		registerResolverHook();
		Activator.context.addBundleListener(bundleEvents);
		BundleCommandImpl bundleCommandImpl = BundleCommandImpl.INSTANCE;
		bundleCommandImpl.initFrameworkWiring();

		extenderCommand = Extenders.register(context.getBundle(), BundleCommand.class.getName(),
				bundleCommandImpl, null); //	new BundleCommandServiceFactory(), null);
		extenderRegion = Extenders.register(context.getBundle(), BundleRegion.class.getName(),
				WorkspaceRegionImpl.INSTANCE, null); // new BundleRegionServiceFactory(), null);
		extenderTransition = Extenders.register(context.getBundle(), BundleTransition.class.getName(),
				BundleTransitionImpl.INSTANCE, null); // new BundleTransitionServiceFactory(), null);
		extenderBundleProjectCandidates = Extenders.register(context.getBundle(), BundleProjectCandidates.class.getName(),
				BundleProjectCandidatesImpl.INSTANCE, null);
//				new BundleProjectCandidatesServiceFactory(), null);
		bundleProjectTracker = new ServiceTracker<IBundleProjectService, IBundleProjectService>(
				context, IBundleProjectService.class.getName(), null);
		bundleProjectTracker.open();
		extenderBundleProjectMeta = Extenders.register(context.getBundle(),
				BundleProjectMeta.class.getName(), BundleProjectMetaImpl.INSTANCE, null);
//				new BundleProjectMetaServiceFactory(), null);

		commandOptions = Extenders.getExtension(CommandOptions.class.getName());
		messageOptions = Extenders.getExtension(MessageOptions.class.getName());
		dependencyOptions = Extenders.getExtension(DependencyOptions.class.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		Activator.context.removeBundleListener(bundleEvents);
		bundleProjectTracker.close();
		bundleProjectTracker = null;
		unregisterResolverHook();
		super.stop(context);
		plugin = null;
		Activator.context = null;
	}

	public static Extender<BundleCommand> getExtenderCommand() {
		return extenderCommand;
	}

	public static Extender<BundleRegion> getExtenderRegion() {
		return extenderRegion;
	}

	public static Extender<BundleTransition> getExtenderTransition() {
		return extenderTransition;
	}

	public static Extender<BundleProjectCandidates> getExtenderBundleCandidatesProject() {
		return extenderBundleProjectCandidates;
	}

	public static Extender<BundleProjectMeta> getExtenderBundleMeta() {
		return extenderBundleProjectMeta;
	}

	/**
	 * Obtain the resolver hook factory for singletons.
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
	public CommandOptions getCommandOptionsService() throws InPlaceException, ExtenderException {

		CommandOptions cmdOpt = commandOptions.getService();
		if (null == cmdOpt) {
			throw new InPlaceException("invalid_service", CommandOptions.class.getName());
		}
		return cmdOpt;
	}

	public MessageOptions getMsgOptService() throws InPlaceException {

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
