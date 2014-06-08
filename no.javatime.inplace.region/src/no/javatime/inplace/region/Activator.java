package no.javatime.inplace.region;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.region.manager.InPlaceException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "no.javatime.inplace.region"; //$NON-NLS-1$
	private static Activator plugin;
	private static BundleContext context;
	
	private ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectTracker;
	private Extension<MessageOptions> messageOptions;

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

		messageOptions = new Extension<>(MessageOptions.class);

		bundleProjectTracker =  new ServiceTracker<IBundleProjectService, IBundleProjectService>
				(context, IBundleProjectService.class.getName(), null);
		bundleProjectTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		bundleProjectTracker.close();
		bundleProjectTracker = null;		
		super.stop(context);
		plugin = null;
		Activator.context = null;
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

	public MessageOptions msgOpt() throws InPlaceException {

		MessageOptions msgOpt = messageOptions.getService();
		if (null == msgOpt) {
			throw new InPlaceException("invalid_service", MessageOptions.class.getName());			
		}
		return msgOpt;
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
