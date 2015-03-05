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
	// Internal object to use for synchronization
	// final private Object serviceLock = new Object();


	public Extender<S> put(ServiceReference<?> sr, Extender<S> extender) throws ExtenderException {

		Extender<S> status = null;
		try {
			Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			status = put(sid, extender);			
		} catch (NullPointerException e) {
			if (null == extender) {
				throw new ExtenderException("Null extender when storing to the service map" );												
			} else {
				throw new ExtenderException("Invalid service (service id or reference is null) for {0}" , extender.getServiceInterfaceName());								
			}
		}
		return status;
	}

	public Extender<S> put(Extender<S> extender) throws ExtenderException {

		if (null == extender) {
			return null;
		}
		Long sid = extender.getServiceId();
		return null == sid ? null : put(sid, extender);
	}

	public Extender<S> remove(ServiceReference<?> sr) {
		
		Extender<S> status = null;
		try {
			Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			status = remove(sid);			
		} catch (NullPointerException e) {
			if (null == sr) {
				throw new ExtenderException("Null service reference when removing extender from the service map" );												
			} else {
				throw new ExtenderException("Null or invalid service (service id)");								
			}
		}
		return status;
	}

	public void remove(Extender<S> extender) {
		
		try {
			Long sid = extender.getServiceId();
			remove(sid);			
		} catch (NullPointerException e) {
			if (null == extender) {
				throw new ExtenderException("Null extender when removing extender from the service map" );												
			} else {
				throw new ExtenderException("Invalid service (service id or reference is null) for {0}" , extender.getServiceInterfaceName());								
			}
		}
	}
	
	public Extender<S> get(ServiceReference<?> sr) {
		
		try {
			Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			return get(sid);			
		} catch (NullPointerException e) {
			throw new ExtenderException("Invalid service ({0} is null) when getting service",
					null == sr ? "reference" : "service id");								
		}
	}

	public Extender<S> get(String serviceInterfaceName) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		return null == sr ? null : get(sr);
	}

	public Collection<Extender<S>> get(String serviceInterfaceName, String filter) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?>[] srs;
		if (null == filter) {
			filter = Extender.EXTENDER_FILTER;
		}
		try {
			srs = context.getServiceReferences(serviceInterfaceName, filter);
		} catch (InvalidSyntaxException | IllegalStateException e) {
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
