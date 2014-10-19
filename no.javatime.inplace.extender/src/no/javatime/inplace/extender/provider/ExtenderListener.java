package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ExtenderListener<S> implements ServiceListener{
	/**
	 * {@code ServiceListener} method for the {@code ServiceTracker} class.
	 * This method must NOT be synchronized to avoid deadlock potential.
	 * 
	 * @param event {@code ServiceEvent} object from the framework.
	 */
	final public void serviceChanged(final ServiceEvent event) {

		final ServiceReference<?> reference = (ServiceReference<?>) event.getServiceReference();
		@SuppressWarnings("unchecked")
		ExtenderServiceMap<S> ems = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
		final Extender<S> extender = ems.getExtender(reference);
		Long sid = Long.valueOf(0L);
		if (null != extender) {
			sid = (Long) reference.getProperty(Constants.SERVICE_ID);
		} else {
			return;
		}
		switch (event.getType()) {
		case ServiceEvent.REGISTERED :
			//ems.register(extender);
			//System.out.println("ServiceTracker.Tracked.register. SID: (" + sid + ") Type: [" + event.getType() + "]: " + extender.getServiceInterfaceName());					
			break;
		case ServiceEvent.MODIFIED :
			//System.out.println("ServiceTracker.Tracked.modified. SID: (" + sid + ") Type: [" + event.getType() + "]: " + extender.getServiceInterfaceName());					
			break;
		case ServiceEvent.MODIFIED_ENDMATCH :
			break;
		case ServiceEvent.UNREGISTERING :
			//System.out.println("ServiceTracker.Tracked.unregister. SID: (" + sid + ") Type: [" + event.getType() + "]: " + extender.getServiceInterfaceName());					
			ems.removeExtender(reference);
			break;
		}
	}

}
