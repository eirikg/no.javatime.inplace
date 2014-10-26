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
	private Extender<S> extender;
	private Bundle userBundle;

	private ServiceTracker<S, S> tracker;

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
		userBundle = extender.getRegistrarBundle();
	}

	public ExtensionImpl(String interfaceName)  throws ExtenderException {

		this.extender = Extenders.getExtender(interfaceName);
		userBundle = extender.getRegistrarBundle();
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

	public void setUserBundle(Bundle userBundle) {
		closeServiceTracker();
		this.userBundle = userBundle;
	}
	
	public void openServiceTracker(Bundle userBundle,  ServiceTrackerCustomizer<S, S> customizer) throws ExtenderException {

		if (null == tracker) {
			try {
				tracker = new ServiceTracker<S, S>(userBundle.getBundleContext(), extender.getServicereReference(),
						customizer);
				tracker.open();
			} catch (IllegalStateException e) {
				tracker = null;
				throw new ExtenderException("failed_to_open_tracker", extender.getServiceInterfaceName());
			}
		}
	}

	public S getTrackedService() throws ExtenderException {

		try {
			if (null == tracker) {
				openServiceTracker(userBundle, null);
			}
			return tracker.getService();
		} catch (ExtenderException e) {
			// Ignore
			// throw new ExtenderException("failed_to_open_tracker", getServiceInterfaceName());
		}
		return null;
	}

	public void closeServiceTracker() {
		// synchronized (tracker) {
		if (null != tracker && tracker.getTrackingCount() != -1) {
			tracker.close();
			tracker = null;
		}
		// }
	}
}
