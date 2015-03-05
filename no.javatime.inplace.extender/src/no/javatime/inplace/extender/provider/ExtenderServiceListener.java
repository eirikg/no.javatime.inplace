package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * This listener should only listen to services registered by extenders. The filter is
 * {@link Extender#EXTENDER_FILTER}.
 * <p>
 * If the filter is removed or its value is set to {@code false} the extender is removed but not
 * unregistered. The service must than be unregistered by using the service layer. 
 * <p>
 * Registered services are added to the extender map when registered and modified properties are
 * synchronized with the service when added or modified in {@code ExtenderImpl}.
 * <p>
 * Extenders are removed from the extender map in this listener if not explicit unregistered from
 * the extender.
 * 
 * @param <S> type of extender
 */
public class ExtenderServiceListener<S> implements ServiceListener {

	/**
	 * Remove the extender from the extender map when a service is unregistered
	 * or the extender filter for this listener is removed or set to {@code false}
	 * 
	 * @param event {@code ServiceEvent} object from the framework.
	 */
	final public void serviceChanged(final ServiceEvent event) {
		
		ExtenderServiceMap<S> map = null;
		ServiceReference<?> sr = null;
		Long sid = null;
		
		switch (event.getType()) {
		// case ServiceEvent.MODIFIED:
		// Remove if the extender filter value is changed to false or filter property is removed
		case ServiceEvent.MODIFIED_ENDMATCH:
		case ServiceEvent.UNREGISTERING:
			map = Activator.getExtenderServiceMap();
			sr = (ServiceReference<?>) event.getServiceReference();
			sid = (Long) sr.getProperty(Constants.SERVICE_ID);			
			if (null == sid) {
				break;
			}
			map.remove(sid);
			break;
		}
	}
}
