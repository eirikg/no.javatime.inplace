package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object storing extender services
 *
 * @param <S> type of service
 */
public class ExtenderServiceFactory<S> implements ServiceFactory<ExtenderImpl<S>> {

	@Override
	public ExtenderImpl<S> getService(Bundle bundle, ServiceRegistration<ExtenderImpl<S>> registration) {
		// System.out.println("ServiceFactory getService: " + ExtenderImpl.class.getName());					
		ExtenderImpl<S> extender = new ExtenderImpl<S>(bundle, bundle, ExtenderImpl.class.getName(), ExtenderImpl.class.getName(), null); 
		return extender;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtenderImpl<S>> registration,
			ExtenderImpl<S> service) {
		// System.out.println("ServiceFactory ungetService: " + ExtenderImpl.class.getName());					
	}  
}  
