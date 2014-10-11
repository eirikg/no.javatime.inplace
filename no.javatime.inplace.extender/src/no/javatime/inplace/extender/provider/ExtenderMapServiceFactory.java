package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory creating service object storing extender services
 *
 * @param <S> type of service
 */
public class ExtenderMapServiceFactory<S> implements ServiceFactory<ExtenderMapService<Long, ExtenderImpl<S>>> {

	@Override
	public ExtenderMapService<Long, ExtenderImpl<S>> getService(Bundle bundle,
			ServiceRegistration<ExtenderMapService<Long, ExtenderImpl<S>>> registration) {
		ExtenderMapService<Long, ExtenderImpl<S>> extenderMap = new ExtenderMapServiceImpl<>();
		return extenderMap;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtenderMapService<Long, ExtenderImpl<S>>> registration,
			ExtenderMapService<Long, ExtenderImpl<S>> service) {
	}  

}