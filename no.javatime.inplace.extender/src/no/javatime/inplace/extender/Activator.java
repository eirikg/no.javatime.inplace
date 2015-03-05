package no.javatime.inplace.extender;

import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.provider.ExtenderBundleTracker;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderServiceListener;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;
import no.javatime.inplace.extender.provider.ExtenderServiceMapImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Implements the extender pattern; - functionality to register services on behalf of bundles
 * providing service implementation classes.
 * 
 * @see Extender
 * @see Extenders
 * @see ExtenderBundleTracker
 * @see BundleServiceScopeFactory
 */
public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "no.javatime.inplace.extender"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	// Register (extend) services for use facilitated by other bundles
	private BundleTracker<ExtenderImpl<?>> extenderBundleTracker;
	private BundleTrackerCustomizer<ExtenderImpl<?>> extenderBundleTrackerCustomizer;

	private ExtenderServiceListener<?> extenderListener;
	private static ExtenderServiceMap<?> extenderMap;

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		extenderMap = new ExtenderServiceMapImpl<>();
		extenderListener = new ExtenderServiceListener<>();
		context.addServiceListener(extenderListener, Extender.EXTENDER_FILTER);
		extenderBundleTrackerCustomizer = new ExtenderBundleTracker();
		extenderBundleTracker = new BundleTracker<ExtenderImpl<?>>(context, Bundle.ACTIVE,
				extenderBundleTrackerCustomizer);
		extenderBundleTracker.open();
	}

	public void stop(BundleContext bundleContext) throws Exception {

		ServiceReference<?> sr = context.getServiceReference(Extender.class.getName());
		if (null != sr) {
			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
		}
		sr = context.getServiceReference(ExtenderServiceMap.class.getName());
		if (null != sr) {
			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
		}
		// If missed any, print to system err
		// ExtenderServiceMap<?> ems = getExtenderServiceMap();
		// ems.validateUnregister();
		extenderBundleTracker.close();
		extenderBundleTracker = null;
		context.removeServiceListener(extenderListener);
		Activator.context = null;
		plugin = null;
	}

	public <S> ExtenderServiceListener<?> getExtenderListener() {
		return extenderListener;
	}

	public BundleTracker<ExtenderImpl<?>> getExtenderBundleTracker() {
		return extenderBundleTracker;
	}

	/**
	 * Get the service map for extenders
	 * 
	 * @return The extender map service or null if the service could not be obtained
	 */
	@SuppressWarnings("unchecked")
	public static <S> ExtenderServiceMap<S> getExtenderServiceMap(){
		return (ExtenderServiceMap<S>) extenderMap;
	}

	// public static Extender<ExtenderServiceMap<?>> getExtenderMap() throws ExtenderException {
	// return extenderMap;
	// }

	public static <S> void ungetServiceMap() {
		// extenderMap.ungetService();
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
