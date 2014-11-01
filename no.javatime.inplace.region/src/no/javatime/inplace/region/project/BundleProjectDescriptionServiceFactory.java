package no.javatime.inplace.region.project;

import no.javatime.inplace.region.intface.BundleProjectDescription;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle project description. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <BundleProjectDescription> service interface for bundle project description
 */
public class BundleProjectDescriptionServiceFactory implements ServiceFactory<BundleProjectDescription> {

	@Override
	public BundleProjectDescription getService(Bundle bundle, ServiceRegistration<BundleProjectDescription> registration) {
		BundleProjectDescriptionImpl BundleProjectDescription = BundleProjectDescriptionImpl.INSTANCE;
		return BundleProjectDescription;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleProjectDescription> registration,
			BundleProjectDescription service) {	
	}

}  
