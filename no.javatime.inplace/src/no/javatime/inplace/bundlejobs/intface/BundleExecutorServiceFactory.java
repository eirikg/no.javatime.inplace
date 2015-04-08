package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Introspector;
import no.javatime.inplace.extender.intface.PrototypeServiceScopeFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Default prototype scope service factory for bundle service operations.
 * <p>
 * Creates a bundle service executor object based on its fully qualified name and optionally a name
 * of the service operation executor.
 * <p>
 * The service class name must be the name of a class that resides in the bundle specified as the
 * owner bundle when the extender was registered.
 */
public class BundleExecutorServiceFactory extends PrototypeServiceScopeFactory<BundleExecutor> {

	private String bundleExecutorName;

	/**
	 * Creates a service based on the name of the service class to create a service object from
	 * <p>
	 * The created bundle service operation executor is assigned a default name
	 * 
	 * @param serviceClassName a fully qualified service class name
	 * @throws ExstenderException if the specified service class name is null
	 */
	public BundleExecutorServiceFactory(String serviceClassName) throws ExtenderException {
		super(serviceClassName);
	}

	/**
	 * Creates a service based on the name of the executor and of the service class to create a
	 * service object from
	 * 
	 * @param executorName name of the service operation executor.
	 * @param serviceClassName a fully qualified service class name
	 * @throws ExstenderException if the specified service class name is null
	 */
	public BundleExecutorServiceFactory(String executorName, String serviceClassName)
			throws ExtenderException {
		super(serviceClassName);
		this.bundleExecutorName = executorName;
	}

	@Override
	public BundleExecutor getService(Bundle bundle, ServiceRegistration<BundleExecutor> registration) {

		try {
			ServiceReference<?> sr = registration.getReference();
			Extender<?> extender = Extenders.getExtender(sr);
			if (null != extender) {
				Bundle ownerBundle = extender.getOwner();
				Class<?> serviceClass = Introspector.loadClass(ownerBundle, getServiceClassName());
				if (null == bundleExecutorName) {
					return (BundleExecutor) Introspector.createObject(serviceClass);
				} else {
					return (BundleExecutor) Introspector.createObject(serviceClass, bundleExecutorName);
				}
			}
		} catch (ExtenderException e) {
			// delegate to framework
		}
		return null;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<BundleExecutor> registration,
			BundleExecutor service) {
	}
}
