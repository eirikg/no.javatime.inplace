package no.javatime.inplace.extender.intface;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Creates and return a service object with a default constructor based on the name of the service
 * class.
 * <p>
 * Use this service factory when registering a service to obtain a new service object for each
 * bundle (bundle service SCOPE) using the registered service
 * <p>
 * The class name must be the name of a class that resides in the bundle specified as a parameter in the
 * callback method {@code #getService(Bundle, ServiceRegistration)}
 * 
 * @param <S> type of service
 */
public class BundleScopeServiceFactory<S> implements ServiceFactory<S> {

	private String serviceClassName;

	@SuppressWarnings("unused")
	private BundleScopeServiceFactory() {
	}

	/**
	 * The name of the service class to create a service object from
	 * 
	 * @param serviceClassName a fully qualified service class name
	 */
	public BundleScopeServiceFactory(String serviceClassName) {
		this.serviceClassName = serviceClassName;
	}

	@Override
	public S getService(Bundle bundle, ServiceRegistration<S> registration) {
		try {
			
			ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
			ServiceReference<S> sr = registration.getReference();
			Extender<S> extender = extServiceMap.get(sr);
			if (null != extender) {
				Bundle ownerBundle = extender.getOwnerBundle();
				Class<S> serviceClass = Introspector.loadClass(ownerBundle, serviceClassName);
				return Introspector.createObject(serviceClass);
			}
		} catch (ExtenderException e) {
			// delegate to framework
		}
		return null;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<S> registration, S service) {
	}
}
