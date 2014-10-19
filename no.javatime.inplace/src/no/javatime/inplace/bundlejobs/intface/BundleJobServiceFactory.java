package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.BundleJob;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class BundleJobServiceFactory implements ServiceFactory<BundleJob> {
 

	@Override
	public BundleJob getService(Bundle bundle, ServiceRegistration<BundleJob> registration) {
		//System.out.println("ServiceFactory getService: " + ActivateBundleJob.class.getName());					
		return  new ActivateBundleJob(ActivateBundleJob.activateStartupJobName);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleJob> registration,
			BundleJob service) {
		//System.out.println("ServiceFactory ungetService: " + ActivateBundleJob.class.getName());					
	}
}
