package no.javatime.inplace.extender.intface;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Creates and return a service object with a default constructor based on the name of the specified
 * service class.
 * <p>
 * Use this service factory when registering an extender to obtain a new service object for each
 * call to {@link Extender#getService(Bundle)} (prototype service scope) using the registered extender
 * <p>
 * The class name must be the name of a class that resides in the bundle specified as the
 * owner bundle when the extender was registered.  
 * 
 * @param <S> type of service
 * @see ExtenderBundleTracker
 * @see Extenders
 */
public class PrototypeServiceScopeFactory<S> extends BundleServiceScopeFactory<S> implements ServiceFactory<S> {


	@SuppressWarnings("unused")
	private PrototypeServiceScopeFactory() {
		super();
	}

	/**
	 * The name of the service class to create a service object from
	 * 
	 * @param serviceClassName a fully qualified service class name
	 * @throws ExstenderException if the specified service class name is null
	 */
	public PrototypeServiceScopeFactory(String serviceClassName) throws ExtenderException {
		super(serviceClassName);
	}

	/**
	 * Return the service class name used by this service factory. This corresponds to the service 
	 * object returned by {@link #getService(Bundle, ServiceRegistration)
	 * 
	 * @return The service class name
	*/
	public String getServiceClassName() {
		return super.getServiceClassName();
	}

	@Override
	public S getService(Bundle bundle, ServiceRegistration<S> registration) {
		return super.getService(bundle, registration);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<S> registration, S service) {
	}
}
