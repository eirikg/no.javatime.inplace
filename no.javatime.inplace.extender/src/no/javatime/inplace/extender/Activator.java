package no.javatime.inplace.extender;

import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.PrototypeServiceScopeFactory;
import no.javatime.inplace.extender.provider.ExtenderBundleListener;
import no.javatime.inplace.extender.provider.ExtenderServiceListener;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;
import no.javatime.inplace.extender.provider.ExtenderServiceMapImpl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Implements the extender pattern; - functionality to register services on behalf of bundles
 * providing service implementation classes.
 * 
 * @see Extender
 * @see Extenders
 * @see ExtenderBundleTracker
 * @see BundleServiceScopeFactory
 * @see PrototypeServiceScopeFactory
 */
public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "no.javatime.inplace.extender"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	private ExtenderServiceListener<?> extenderListener;
	private static ExtenderServiceMap<?> extenderMap;
	private ExtenderBundleListener bundlelistener;
	
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		bundlelistener = new ExtenderBundleListener();
		context.addBundleListener(bundlelistener);
		extenderMap = new ExtenderServiceMapImpl<>();
		// Extenders.register(context.getBundle(), ExtenderServiceMap.class.getName(), extenderMap, null);
		extenderListener = new ExtenderServiceListener<>();
		context.addServiceListener(extenderListener, Extender.EXTENDER_FILTER);
	}

	public void stop(BundleContext bundleContext) throws Exception {
			
			// Use when registering extender and extender service map as services
//		ServiceReference<?> sr = context.getServiceReference(Extender.class.getName());
//		if (null != sr) {
//			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
//		}
//		sr = context.getServiceReference(ExtenderServiceMap.class.getName());
//		if (null != sr) {
//			getExtenderListener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
//		}
		// If missed any, print to system err
		ExtenderServiceMap<?> esm = getExtenderServiceMap();
		
		esm.validateUnregister();
		context.removeServiceListener(extenderListener);
		context.removeBundleListener(bundlelistener);
		context = null;
		plugin = null;
	}

	public <S> ExtenderServiceListener<?> getExtenderListener() {
		return extenderListener;
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
