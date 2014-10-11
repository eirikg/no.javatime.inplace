package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.Activator;

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
		Bundle ownerBundle = Activator.getContext().getBundle();
		ExtenderImpl<S> extender = new ExtenderImpl<S>(ownerBundle, ownerBundle, ExtenderImpl.class.getName(), ExtenderImpl.class.getName()); 
		return extender;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtenderImpl<S>> registration,
			ExtenderImpl<S> service) {
		// TODO Auto-generated method stub
		
	}  

}