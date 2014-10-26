package no.javatime.inplace.region.manager;

import no.javatime.inplace.region.intface.BundleCommand;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object for bundle commands. The service scope for this factory is restricted
 * to singleton.
 *
 * @param <S> type of service
 */
public class BundleCommandServiceFactory implements ServiceFactory<BundleCommand> {

	@Override
	public BundleCommand getService(Bundle bundle, ServiceRegistration<BundleCommand> registration) {
		BundleCommandImpl bc = BundleCommandImpl.INSTANCE;
		bc.initFrameworkWiring();
		return bc;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleCommand> registration,
			BundleCommand service) {	
	}

}  
