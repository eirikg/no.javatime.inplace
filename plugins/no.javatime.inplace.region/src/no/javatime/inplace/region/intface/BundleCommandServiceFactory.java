package no.javatime.inplace.region.intface;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.manager.BundleCommandImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle commands. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <BundleCommand> service interface for bundle commands
 */
public class BundleCommandServiceFactory implements ServiceFactory<BundleCommand> {

	@Override
	public BundleCommand getService(Bundle bundle, ServiceRegistration<BundleCommand> registration) {
		BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
		bundleCommand.initFrameworkWiring();
		// Set scope to singleton when returning the same instance each time
		ServiceReference<BundleCommand> sr = registration.getReference();
		Extender<BundleCommand> extender = Extenders.getExtender(sr);
		if (null != extender) {
			extender.setProperty(Extender.SCOPE, Extender.SINGLETON);
		}
		return bundleCommand;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleCommand> registration,
			BundleCommand service) {	
	}

}  
