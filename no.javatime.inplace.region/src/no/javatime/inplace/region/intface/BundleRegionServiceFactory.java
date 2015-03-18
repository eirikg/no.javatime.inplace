package no.javatime.inplace.region.intface;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for the bundle region. The service scope for this factory is restricted
 * to singleton
 *
 * @param <BundleRegion> service interface for a bundle region 
 */
public class BundleRegionServiceFactory implements ServiceFactory<BundleRegion> {

	@Override
	public BundleRegion getService(Bundle bundle, ServiceRegistration<BundleRegion> registration) {
		WorkspaceRegionImpl br = WorkspaceRegionImpl.INSTANCE;
		// Set scope to singleton when returning the same instance each time
		ServiceReference<BundleRegion> sr = registration.getReference();
		Extender<BundleRegion> extender = Extenders.getExtender(sr);
		if (null != extender) {
			extender.setProperty(Extender.SCOPE, Extender.SINGLETON);
		}
		return br;
	}
	
	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleRegion> registration,
			BundleRegion service) {
	}

}  
