package no.javatime.inplace.extender.provider;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ExtenderServiceMapImpl<S> extends ConcurrentHashMap<Long, Extender<S>> implements ExtenderServiceMap<S> {

	private static final long serialVersionUID = 1L;

	public Extender<S> addExtender(Extender<S> extender) throws ExtenderException {
			put(extender.getServiceId(), extender);
			return extender;
	}

	public Extender<S> removeExtender(ServiceReference<?> sr) {
		if (null == sr) {
			return null;
		}
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		return remove(sid);
	}
	
	public Extender<S> getExtender(ServiceReference<?> sr) {
		if (null == sr) {
			return null;
		}
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		return get(sid);
	}

	public Extender<S> getExtender(String serviceInterfaceName) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		return getExtender(sr);
	}
	
	public boolean isExtended(String serviceInterfaceName) {

		if (null != getExtender(serviceInterfaceName)) {
			return true;
		}
		return false;
	}

	public void validateUnregister() {

		if (size() > 0) {
			Iterator<Entry<Long, Extender<S>>> it = entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Long, Extender<S>> entry = it.next();
				System.err.println("\nThis one has not been unregistered. SID: " + entry.getKey() + " "
						+ entry.getValue().getServiceInterfaceName() + ". Revise.");
			}
			clear();
		}
	}
}
