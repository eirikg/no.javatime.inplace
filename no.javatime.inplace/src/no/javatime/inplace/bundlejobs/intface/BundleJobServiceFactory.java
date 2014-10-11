package no.javatime.inplace.bundlejobs.intface;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class BundleJobServiceFactory<S> implements ServiceFactory<S> {
 
	public S getService(Bundle bundle, ServiceRegistration<S> reg) { 

		// each bundle gets a Long with it's own id
    return (S) new Long(bundle.getBundleId());
 }
 
	public void ungetService(Bundle bundle, ServiceRegistration<S> reg, S service) {
    // nothing needed in this case
	}
}
