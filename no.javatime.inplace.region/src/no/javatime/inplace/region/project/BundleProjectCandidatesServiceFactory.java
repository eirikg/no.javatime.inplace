package no.javatime.inplace.region.project;

import no.javatime.inplace.region.intface.BundleProjectCandidates;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle project. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <BundleProjectCandidates> service interface for bundle projects
 */
public class BundleProjectCandidatesServiceFactory implements ServiceFactory<BundleProjectCandidates> {

	@Override
	public BundleProjectCandidates getService(Bundle bundle, ServiceRegistration<BundleProjectCandidates> registration) {
		BundleProjectCandidatesImpl bundleProject = BundleProjectCandidatesImpl.INSTANCE;
		return bundleProject;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleProjectCandidates> registration,
			BundleProjectCandidates service) {	
	}

}  
