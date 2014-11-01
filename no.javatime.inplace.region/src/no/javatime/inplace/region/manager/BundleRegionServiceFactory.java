package no.javatime.inplace.region.manager;

import no.javatime.inplace.region.intface.BundleRegion;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for the bundle region. The service scope for this factory is restricted
 * to singleton
 *
 * @param <BundleRegion> service interface for a region of bundles
 */
public class BundleRegionServiceFactory implements ServiceFactory<BundleRegion> {

	@Override
	public BundleRegion getService(Bundle bundle, ServiceRegistration<BundleRegion> registration) {
		WorkspaceRegionImpl br = WorkspaceRegionImpl.INSTANCE;
		return br;
	}
	
	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleRegion> registration,
			BundleRegion service) {
	}

}  
