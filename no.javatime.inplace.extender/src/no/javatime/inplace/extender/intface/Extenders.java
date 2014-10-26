package no.javatime.inplace.extender.intface;

import java.util.Dictionary;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

/**
 * Register, unregister, re-register and access extenders and create extensions
 * <p>
 * Prior to OSGi R6 a service scope is implicit when registering a service. The scopes are singleton
 * (shared service), bundle (one service per bundle) and prototype (OSGi R6 only) (a new service for
 * each call to {@link #getService()} and {@link #getService(Bundle)}.
 * <p>
 * The specified registrar bundle specified when registering a service is also the user bundle of
 * the service when {@link #getService()} is used. If {@link #getService(Bundle)} is used, the
 * specified bundle is the user bundle.
 */
public class Extenders {

	private Extenders() {
	}

	/**
	 * Registers the specified service object using a service name with a bundle tracker and the
	 * specified properties under the specified interface class name with the Framework. The extender
	 * is typically registered from a (registrar) bundle not containing (owing) the service to
	 * register and is registered from a bundle tracker when the bundle owing the service class is
	 * activated.
	 * <p>
	 * The service scope is singleton when specifying a service name. Use
	 * {@link BundleScopeServiceFactory} or your own customized service factory to to use bundle
	 * scope. For prototype scope use the prototype factory supplied by the Framework. Note that it
	 * is not possible to use the prototype service scope prior to OSGi R6 (pre. Luna)
	 * 
	 * @param tracker The tracker that the extender was registered by. Can be null.
	 * @param owner The bundle owing the class of the specified service name. Can be same as the
	 * specified registrar bundle.
	 * @param registrar The bundle registering the service with the Framework.
	 * @param serviceInterfaceName The class name under which the service can be located.
	 * @param serviceName A fully qualified class name of the service.
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type. If the service was registered before
	 * this registration the already registered extender object is returned.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(BundleTracker<Extender<?>> tracker, Bundle owner,
			Bundle registrar, String serviceInterfaceName, String serviceName,
			Dictionary<String, Object> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(tracker, owner, registrar, serviceInterfaceName,
				serviceName, properties);
		extender.registerService();
		return extender;
	}

	/**
	 * Registers the specified service object with a bundle tracker and the specified properties under
	 * the specified interface class name with the Framework. The extender is typically registered
	 * from a (registrar) bundle not containing (owing) the service to register and is registered from
	 * a bundle tracker when the bundle owing the service class is activated.
	 * <p>
	 * If the specified service is a service factory object the service scope is bundle, and singleton
	 * if the the specified service object is a service. For prototype scope use the prototype
	 * factory supplied by the Framework. Note that it is not possible to use the prototype service
	 * scope prior to OSGi R6 (pre. Luna)
	 * 
	 * @param tracker The tracker that the extender was registered by. Can be null.
	 * @param owner The bundle owing the class of the specified service name. Can be same as the
	 * specified registrar bundle.
	 * @param registrar The bundle registering the service with the Framework.
	 * @param serviceInterfaceName The class name under which the service can be located.
	 * @param service The service object or a service factory object.
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type. If the service was registered before
	 * this registration the already registered extender object is returned.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(BundleTracker<Extender<?>> tracker, Bundle owner,
			Bundle registrar, String serviceInterfaceName, Object service,
			Dictionary<String, Object> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(tracker, owner, registrar, serviceInterfaceName,
				service, properties);
		extender.registerService();
		return extender;
	}

	/**
	 * Registers the specified service object using a service name with the specified properties under
	 * the specified interface class name with the Framework. The service scope is singleton. The
	 * extender is typically registered from a (registrar) bundle not containing (owing) the service
	 * to register.
	 * 
	 * @param owner The bundle owing the class of the specified service name. Can be same as the
	 * specified registrar bundle.
	 * @param registrar The bundle registering the service with the Framework.
	 * @param serviceInterfaceName The class name under which the service can be located.
	 * @param serviceName A fully qualified class name of the service.
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type. If the service was registered before
	 * this registration the already registered extender object is returned.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName, Dictionary<String, Object> properties)
			throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(ownerBundle, regBundle, serviceInterfaceName,
				serviceName, properties);
		extender.registerService();
		return extender;
	}

	/**
	 * Registers the specified service object with the specified properties under the specified
	 * interface class name with the Framework. The service scope is singleton. The extender is
	 * typically registered from a (registrar) bundle not containing (owing) the service to register.
	 * <p>
	 * If the specified service is a service factory object the service scope is bundle, and singleton
	 * if the the specified service object is a service.
	 * 
	 * @param owner The bundle owing the class of the specified service name. Can be same as the
	 * specified registrar bundle.
	 * @param registrar The bundle registering the service with the Framework.
	 * @param interfaceService The class name under which the service can be located.
	 * @param service The service object or a service factory object.
	 * @param properties The properties for this service. Can be null.
	 * 
	 * @return An extender object of the registered service type. If the service was registered before
	 * this registration the already registered extender object is returned.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, Object service, Dictionary<String, Object> properties)
			throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(ownerBundle, regBundle, serviceInterfaceName,
				service, properties);
		extender.registerService();
		return extender;
	}

	/**
	 * Registers the specified service object with the specified properties under the specified
	 * interface class name with the Framework.
	 * <p>
	 * If the specified service is a service factory object the service scope is bundle, and singleton
	 * if the the specified service object is a service.
	 * 
	 * @param registrar The bundle registering the service with the Framework
	 * @param serviceInterfaceName The class name under which the service can be located
	 * @param service The service object or a service factory object.
	 * @param properties The properties for this service.
	 * @return An extender object of the registered service type. If the service was registered before
	 * this registration the already registered extender object is returned.
	 * @throws ExtenderException if the bundle context of the specified registrar bundle is no longer
	 * valid, the registered implementation class does not implement the registered interface, the
	 * interface name is illegal, a security violation or if the service object could not be obtained
	 */
	public static <S> Extender<S> register(Bundle registrar, String serviceInterfaceName,
			Object service, Dictionary<String, Object> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(registrar, registrar, serviceInterfaceName, service,
				null);
		extender.registerService();
		return extender;
	}

	/**
	 * Registers a service object with the properties, interface name and possible bundle tracker held
	 * by the specified extender.
	 * <p>
	 * If the service is a service factory object the service scope is bundle, and singleton if the
	 * the specified service object is a service or a service name.
	 * <p>
	 * An extender service may be unregistered and registered multiple times using the same extender
	 * object. If the extender is registered with a service when invoking this method the extender is
	 * returned without doing any service registration.
	 * 
	 * @param extender An unregistered extender
	 * @return True if a service object was registered and false if the specified extender is null or
	 * a registered service object already exists.
	 * @throws ExtenderException if the bundle context of the specified registrar bundle is no longer
	 * valid, the registered implementation class does not implement the registered interface, the
	 * interface name is illegal, a security violation or if the service object could not be obtained
	 * @see #unregisterService(Extender)
	 */
	public static <S> Boolean registerService(Extender<S> extender) throws ExtenderException {

		return null == extender ? false : extender.registerService();
	}

	/**
	 * Unregister this extender and the service held by this extender. After unregistering the
	 * extender, the service is removed from the framework and any references to it is removed from
	 * the extender. After unregistering the service can not be accessed.
	 * <p>
	 * After unregistering it is not possible to lookup the extender again using
	 * {@link #getExtender(String)} or {@link #getExtension(String)}, but you can register the
	 * extender and a new service again with the specified extender parameter by using
	 * {@link #registerService(Extender)} or {@link Extender#registerService()}.
	 * 
	 * @param the extender to unregister
	 * @see Extender#unregisterService()
	 * @see #registerService(Extender)
	 */
	public static <S> void unregisterService(Extender<S> extender) {
		extender.unregisterService();
	}

	/**
	 * Release the service object held by this extension. The context bundle's use count for the
	 * service is decremented by one.
	 * 
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise;
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService(Bundle)
	 */
	public static <S> Boolean ungetService(Extender<S> extender) {
		return extender.ungetService();
	}

	/**
	 * Get an extension of the extender previously registered with the specified interface name
	 * 
	 * @param interfaceName service interface name
	 * @return the extension interface or null if there is no registered service with the specified
	 * interface name
	 * @throws ExtenderException if fail to get the registered extension
	 */
	public static final <S> Extension<S> getExtension(String interfaceName) throws ExtenderException {

		@SuppressWarnings("unchecked")
		ExtenderServiceMap<S> extMapService = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
		Extender<S> extender = extMapService.getExtender(interfaceName);
		Activator.ungetServiceMap();
		return extender.getExtension();
	}

	/**
	 * Get an extender to access its service, query its meta data and get an extension for the
	 * extender
	 * 
	 * @param serviceInterfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the service is not tracked under the specified interface name.
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static final <S> Extender<S> getExtender(String interfaceName) throws ExtenderException {

		@SuppressWarnings("unchecked")
		ExtenderServiceMap<S> extMapService = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
		Extender<S> extender = extMapService.getExtender(interfaceName);
		Activator.ungetServiceMap();
		return extender;
	}

	/**
	 * Get an extender based on its service id. When an extender is registered the unique service id
	 * generated by the framework is automatically used to store and later access an extender
	 * <p>
	 * If the extender could not be found it may have been unregistered or never registered by the
	 * specified key
	 * 
	 * @param serviceId the unique key to locate the extender
	 * @return the extender instance or null if the service is not tracked under the specified service id.
	 * @throws ExtenderException if the bundle context used to get the extender is not valid or a
	 * security permission is missing
	 */
	public static final <S> Extender<S> getExtender(Long serviceId) throws ExtenderException {

		@SuppressWarnings("unchecked")
		ExtenderServiceMap<S> extMapService = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
		Extender<S> extender = extMapService.get(serviceId);
		Activator.ungetServiceMap();
		return extender;
	}
}
