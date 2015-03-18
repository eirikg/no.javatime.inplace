package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 * Create an extension for a given interface.
 *
 * @param <S> type of service
 */
public class ExtensionImpl<S> implements Extension<S> {

	/**
	 * The extender this extension is part of
	 */
	private final Extender<S> extender;
	private final Bundle userBundle;

	private ServiceTracker<S, S> tracker;
	/* internal object to use for synchronization */
	private final Object trackerLock = new Object();

	public ExtensionImpl(Class<S> intFace, Bundle userBundle)  throws ExtenderException {

		this.extender = Extenders.getExtender(intFace.getName());
		this.userBundle = userBundle;
	}

	public ExtensionImpl(String interfaceName, Bundle userBundle)  throws ExtenderException {

		this.extender = Extenders.getExtender(interfaceName);
		this.userBundle = userBundle;
	}

	public ExtensionImpl(Class<S> intFace)  throws ExtenderException {
		this.extender = Extenders.getExtender(intFace.getName());
		userBundle = extender.getRegistrar();
	}

	public ExtensionImpl(String interfaceName)  throws ExtenderException {

		this.extender = Extenders.getExtender(interfaceName);
		userBundle = extender.getRegistrar();
	}

	public S getService(Bundle bundle) throws ExtenderException {		
		return extender.getService(bundle);
	}		

	public S getService() throws ExtenderException {
		return extender.getService(userBundle);						
	}		
	
	public Boolean ungetService() {
		return extender.ungetService(userBundle);
	}

	public Boolean ungetService(Bundle bundle) {
		return extender.ungetService(bundle);
	}

	public Extender<S> getExtender() throws ExtenderException {
		return extender;
	}

	public Bundle getUserBundle() {
		return userBundle;
	}

	public void openServiceTracker(ServiceTrackerCustomizer<S, S> customizer) throws ExtenderException {

//		synchronized (trackerLock) {
			if (null == tracker) {
				try {
					tracker = new ServiceTracker<S, S>(this.userBundle.getBundleContext(), extender.getServiceReference(),
							customizer);
					tracker.open();
				} catch (IllegalStateException e) {
					tracker = null;
					throw new ExtenderException(e, "Failed to open the tracker for interface {0}", extender.getServiceInterfaceName());
				}
			}
//		}
	}

	public S getTrackedService() throws ExtenderException {

		try {
//			synchronized (trackerLock) {
				if (null == tracker) {
					openServiceTracker(null);
				} else if (tracker.getTrackingCount() == -1) {
					tracker.open();
				}
				return tracker.getService();
//			}
		} catch (ExtenderException | NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Failed to get tracked service tracker for interface {0}", extender.getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	public void closeTrackedService() {
//		synchronized (trackerLock) {
			if (null != tracker && tracker.getTrackingCount() != -1) {
				tracker.close();
				tracker = null;
			}
		}
//	}
}
