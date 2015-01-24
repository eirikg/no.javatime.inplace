package no.javatime.inplace.extender.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ExtenderServiceMapImpl<S> extends ConcurrentHashMap<Long, Extender<S>> implements ExtenderServiceMap<S> {

	private static final long serialVersionUID = 1L;

	public Extender<S> put(ServiceReference<?> sr, Extender<S> extender) throws ExtenderException {

		if (null == sr) {
			return null;
		}
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		put(sid, extender);
		return extender;
	}

	public Extender<S> put(Extender<S> extender) throws ExtenderException {

		Long sid = extender.getServiceId();
		put(sid, extender);
		return extender;
	}

	public Extender<S> remove(ServiceReference<?> sr) {
		
		if (null == sr) {
			return null;
		}
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		return remove(sid);
	}

	public Extender<S> remove(Extender<S> extender) {
		
		Long sid = extender.getServiceId();
		return remove(sid);
	}
	
	public Extender<S> get(ServiceReference<?> sr) {
		
		if (null == sr) {
			return null;
		}
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		return get(sid);
	}

	public Extender<S> get(String serviceInterfaceName) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		return get(sr);
	}

	public Collection<Extender<S>> get(String serviceInterfaceName, String filter) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?>[] srs;
		try {
			srs = context.getServiceReferences(serviceInterfaceName, filter);
		} catch (InvalidSyntaxException e) {
			throw new ExtenderException(e, "Failed to parse filter: {0}", filter);
		}
		if (null != srs && srs.length > 0) {
			Collection<Extender<S>> extenders = new ArrayList<Extender<S>>();
			for (int i = 0; i < srs.length; i++) {
				Extender<S> extender = get(srs[i]);
				if (null != extender) {
					extenders.add(extender);
				}
			}
			if (extenders.size() > 0) {
				return extenders;
			}
		}
		return null;
	}
	
	public boolean isExtended(String serviceInterfaceName) {

		if (null != get(serviceInterfaceName)) {
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
