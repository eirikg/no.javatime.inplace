package no.javatime.inplace.extender;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.ExtenderBundleTracker;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.extender.provider.InPlaceException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements the extender pattern, offering functionality to register services on behalf of bundles
 * providing interface and implementation classes. One way to provide the implementation class is
 * to register it as a header entry in the manifest file of the bundle housing the provided interface.
 * <?>
 * To register and use for instance a message view with interface MessageView and the class name
 * implementing the interface as a header entry the {@link Extender} can be used in the following way:
 * <p>
 * </h1>&nbsp;		String implClass = context.getBundle().getHeaders().get(MessageView.MESSAGE_VIEW_HEADER);
 * </h1>&nbsp;		Extender.<MessageView>register(context.getBundle().getBundleId(), MessageView.class, implClass);
 * <p> 
 * To access the message view as a service one approach is to use the {@link Extension} class:
 * <p>
 * </h1>&nbsp;		Extension<MessageView> ext = new Extension<>(MessageView.class>());
 * </h1>&nbsp;		MessageView mv = ext.getService();
 * </h1>&nbsp;		mv.show(); 
 * @see Extender
 * @see Extension
 */
public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "no.javatime.inplace.extender"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	// Don't know the interface to extend yet. Can be any interface 
	private BundleTracker<Extender<?>> extenderBundleTracker;
	private BundleTrackerCustomizer<Extender<?>> extenderBundleTrackerCustomizer;

	private ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectTracker;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		extenderBundleTrackerCustomizer = new ExtenderBundleTracker();
		int trackStates = Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED | Bundle.UNINSTALLED;
				extenderBundleTracker = new BundleTracker<Extender<?>>(context, trackStates, extenderBundleTrackerCustomizer);
		extenderBundleTracker.open();
		
		bundleProjectTracker =  new ServiceTracker<IBundleProjectService, IBundleProjectService>
		(context, IBundleProjectService.class.getName(), null);
		bundleProjectTracker.open();

	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		extenderBundleTracker.close();
		extenderBundleTracker = null;
		Extender.close();
		bundleProjectTracker.close();
		bundleProjectTracker = null;		
		Activator.context = null;
		plugin = null;
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

	/**
	 * The context for interacting with the FrameWork
	 * 
	 * @return the bundle execution context within the FrameWork
	 */
	public static BundleContext getContext() {
		return context;
	}
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
