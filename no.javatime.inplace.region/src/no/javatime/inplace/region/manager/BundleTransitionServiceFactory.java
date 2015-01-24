package no.javatime.inplace.region.manager;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.intface.BundleTransition;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
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
		BundleTransitionImpl transition = BundleTransitionImpl.INSTANCE;
		// Set scope to singleton when returning the same instance each time
		ServiceReference<BundleTransition> sr = registration.getReference();
		Extender<BundleTransition> extender = Extenders.getExtender(sr);
		if (null != extender) {
			extender.setProperty(Extender.SCOPE, Extender.SINGLETON);
		}
		return transition;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleTransition> registration,
			BundleTransition service) {
	}

}  
