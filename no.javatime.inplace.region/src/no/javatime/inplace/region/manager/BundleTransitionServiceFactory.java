package no.javatime.inplace.region.manager;

import no.javatime.inplace.region.intface.BundleTransition;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for the bundle command. The service scope for this factory is restricted
 * to singleton
 *
 * @param <BundleTransition> service interface for bundle transitions
 */
public class BundleTransitionServiceFactory implements ServiceFactory<BundleTransition> {

	@Override
	public BundleTransition getService(Bundle bundle,
			ServiceRegistration<BundleTransition> registration) {
		return BundleTransitionImpl.INSTANCE;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleTransition> registration,
			BundleTransition service) {
	}

}  
