package no.javatime.inplace.region.project;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.intface.BundleProjectMeta;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle project description. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <BundleProjectMeta> service interface for bundle project description
 */
public class BundleProjectMetaServiceFactory implements ServiceFactory<BundleProjectMeta> {

	@Override
	public BundleProjectMeta getService(Bundle bundle, ServiceRegistration<BundleProjectMeta> registration) {
		BundleProjectMetaImpl bundlePrrojectMeta = BundleProjectMetaImpl.INSTANCE;
		// Set scope to singleton when returning the same instance each time
		ServiceReference<BundleProjectMeta> sr = registration.getReference();
		Extender<BundleProjectMeta> extender = Extenders.getExtender(sr);
		if (null != extender) {
			extender.setProperty(Extender.SCOPE, Extender.SINGLETON);
		}
		return bundlePrrojectMeta;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleProjectMeta> registration,
			BundleProjectMeta service) {	
	}

}  
