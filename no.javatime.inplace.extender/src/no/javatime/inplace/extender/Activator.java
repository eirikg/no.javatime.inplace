package no.javatime.inplace.extender;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.provider.ExtenderBundleTracker;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderListener;
import no.javatime.inplace.extender.provider.ExtenderMapService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

/**
 * Implements the extender pattern, offering functionality to register services on behalf of bundles
 * providing interface and implementation classes. One way to provide the implementation class is
 * to register it as a header entry in the manifest file of the bundle housing the provided interface.
 * <?>
 * To register and use for instance a message view with interface MessageView and the class name
 * implementing the interface as a header entry the {@link ExtenderImpl} can be used in the following way:
 * <p>
 * </h1>&nbsp;		String implClass = context.getBundle().getHeaders().get(MessageView.MESSAGE_VIEW_HEADER);
 * </h1>&nbsp;		ExtenderImpl.<MessageView>register(context.getBundle().getBundleId(), MessageView.class, implClass);
 * <p> 
 * To access the message view as a service one approach is to use the {@link Extenders} class:
 * <p>
 * </h1>&nbsp;		Extenders<MessageView> ext = new Extenders<>(MessageView.class>());
 * </h1>&nbsp;		MessageView mv = ext.getService();
 * </h1>&nbsp;		mv.show(); 
 * @see ExtenderImpl
 * @see Extenders
 */
public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "no.javatime.inplace.extender"; //$NON-NLS-1$
	
	private static Activator plugin;
	private static BundleContext context;
	// Register (extend) services for use facilitated by other bundles  
	private BundleTracker<ExtenderImpl<?>> extenderBundleTracker;

	private ExtenderListener extenderListener = new ExtenderListener();

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		context.addServiceListener(extenderListener, ExtenderImpl.filter);
		Bundle bundle = context.getBundle();
		ExtenderImpl.<ExtenderImpl<?>>init(bundle, bundle);
		

		extenderBundleTracker = new ExtenderBundleTracker(context, Bundle.ACTIVE, null);
		extenderBundleTracker.open();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		ServiceReference<?> sr = context.getServiceReference(Extender.class.getName());
		if (null != sr) {
			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
		}
		sr = context.getServiceReference(ExtenderMapService.class.getName());
		if (null != sr) {
			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
		}
		// If missed any, print to system err
		// ExtenderImpl.validateUnregister();
		extenderBundleTracker.close();
		extenderBundleTracker = null;
		context.removeServiceListener(extenderListener);;
		Activator.context = null;
		plugin = null;
	}
	
	public ExtenderListener getExtenderListener() {
		return extenderListener;
	}

	public BundleTracker<ExtenderImpl<?>> getExtenderBundleTracker() {
		return extenderBundleTracker;
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
