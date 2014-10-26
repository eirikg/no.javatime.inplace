package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.Extender;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ExtenderServiceListener<S> implements ServiceListener {

	ExtenderServiceMap<S> extenders;
	Extender<S> extender;

	public ExtenderServiceListener(ExtenderServiceMap<S> extenders) {
		this.extenders = extenders;
	}

	public Extender<S> getExtender() {
		return extender;
	}

	@SuppressWarnings("unchecked")
	public void setExtender(Extender<?> extender) {
			this.extender = (Extender<S>) extender;
	}

	/**
	 * {@code ServiceListener} method for the {@code ServiceTracker} class. This method must NOT be
	 * synchronized to avoid deadlock potential.
	 * 
	 * @param event {@code ServiceEvent} object from the framework.
	 */
	final public void serviceChanged(final ServiceEvent event) {

		final ServiceReference<?> sr = (ServiceReference<?>) event.getServiceReference();
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		switch (event.getType()) {
		case ServiceEvent.REGISTERED:
			// extenders.addExtender(sr, getExtender());
			System.out.println("ServiceTracker.Tracked.register. SID: (" + sid + ") Type: ["
					+ event.getType() + "]:" + extender.getServiceInterfaceName());
			break;
		case ServiceEvent.MODIFIED:
			System.out.println("ServiceTracker.Tracked.modified. SID: (" + sid + ") Type: ["
					+ event.getType() + "]: " + extender.getServiceInterfaceName());
			break;
		case ServiceEvent.MODIFIED_ENDMATCH:
			break;
		case ServiceEvent.UNREGISTERING:
			extender = extenders.removeExtender(sr);
			System.out.println("ServiceTracker.Tracked.unregister. SID: (" + sid + ") Type: ["
					+ event.getType() + "]: " + extender.getServiceInterfaceName());
			break;
		}
	}
}
