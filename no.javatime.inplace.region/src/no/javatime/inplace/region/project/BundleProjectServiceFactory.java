package no.javatime.inplace.region.project;

import no.javatime.inplace.region.intface.BundleProject;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle project. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <BundleProject> service interface for bundle projects
 */
public class BundleProjectServiceFactory implements ServiceFactory<BundleProject> {

	@Override
	public BundleProject getService(Bundle bundle, ServiceRegistration<BundleProject> registration) {
		BundleProjectImpl bundleProject = BundleProjectImpl.INSTANCE;
		return bundleProject;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleProject> registration,
			BundleProject service) {	
	}

}  
