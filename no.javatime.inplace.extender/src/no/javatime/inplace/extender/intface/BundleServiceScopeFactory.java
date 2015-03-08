package no.javatime.inplace.extender.intface;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Creates and return a service object with a default constructor based on the name of the specified
 * service class.
 * <p>
 * Use this service factory when registering an extender or a service to obtain a new service object for each
 * bundle (bundle service scope) using the registered extender
 * <p>
 * The class name must be the name of a class that resides in the bundle specified as the owner
 * bundle when the extender was registered.
 * 
 * @param <S> type of service
 * @see ExtenderBundleTracker
 * @see Extenders
 */
public class BundleServiceScopeFactory<S> implements ServiceFactory<S> {

	private final String serviceClassName;
	private Class<S> serviceClass;
	/* internal object to use for synchronization */
	private final Object serviceClassLock = new Object();

	@SuppressWarnings("unused")
	protected BundleServiceScopeFactory() {
		serviceClassName = null;
	}

	/**
	 * Creates a service based on the name of the service class to create a service object from
	 * 
	 * @param serviceClassName a fully qualified service class name
	 * @throws ExstenderException if the specified service class name is null
	 */
	public BundleServiceScopeFactory(String serviceClassName) throws ExtenderException {
		if (null == serviceClassName) {
			throw new ExtenderException("Service class name must not be null when creating a service factory object");
		}
		this.serviceClassName = serviceClassName;
	}

/**
	 * Return the service class name used by this service factory. This corresponds to the service 
	 * object returned by {@link #getService(Bundle, ServiceRegistration)
	 * 
	 * @return The service class name
	*/
	public String getServiceClassName() {
		return serviceClassName;
	}

	@Override
	public S getService(Bundle bundle, ServiceRegistration<S> registration) {
		try {

			ExtenderServiceMap<S> extenderServiceMap = Activator.getExtenderServiceMap();
			ServiceReference<S> sr = registration.getReference();
			Extender<S> extender = extenderServiceMap.get(sr);
			if (null != extender) {
				synchronized (serviceClassLock) {
					if (null == serviceClass) {
						Bundle ownerBundle = extender.getOwner();
						serviceClass = Introspector.loadClass(ownerBundle, serviceClassName);
					}
					return Introspector.createObject(serviceClass);
				}
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
