package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object storing extender services
 *
 * @param <S> type of service
 */
public class ExtenderServiceMapFactory<S> implements ServiceFactory<ExtenderServiceMap<S>> {
  
	@Override
	public ExtenderServiceMap<S> getService(Bundle bundle,
			ServiceRegistration<ExtenderServiceMap<S>> registration) {

		ExtenderServiceMap<S> extenderMap = new ExtenderServiceMapImpl<>();
		return extenderMap;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtenderServiceMap<S>> registration,
			ExtenderServiceMap<S> service) {
	}  
}