package no.javatime.inplace.extender.intface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

/**
 * Use factory methods to register, unregister and find extenders and create extensions
 * <p>
 * <p>
 * Registered extenders registers its underlying service into the Framework and is also accessible
 * by e.g. {@link org.osgi.framework.BundleContext#getServiceReference(String)}
 * <p>
 * Prior to OSGi R6 a service scope is implicit when registering a service. The scopes in R6 are
 * singleton (shared service), bundle ({@code ServiceFactory} - one service per bundle) and
 * prototype (OSGi R6 only) ({@code PrototypeServiceFactory} - a new service for each call to
 * {@link #getService()} and {@link #getService(Bundle)}).
 * 
 * @see ExtenderBundleTracker
 * @see Extender
 * @see Extension
 */
public class Extenders {

	private Extenders() {
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class names as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name and where the {@code registrar} is the bundle registering the
	 * extender.
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle hosting the specified service and the service interface name
	 * @param registrar The bundle registering the extender
	 * @param serviceInterfaceNames Interface names under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 * @see org.osgi.framework.BundleContext#registerService(String[], Object, Dictionary)
	 */
	public static <S> Extender<S> register(Bundle owner, Bundle registrar,
			String[] serviceInterfaceNames, Object service, Dictionary<String, ?> properties)
			throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(owner, registrar, serviceInterfaceNames, service,
				properties);
		return extender;
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class name as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name and where the {@code registrar} is the bundle registering the
	 * extender.
	 * <p>
	 * This method is otherwise identical to
	 * {@link #register(Bundle, Bundle, String[], Object, Dictionary)} and is provided as a
	 * convenience when the {@code service} will only be registered under a single interface class
	 * name.
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle hosting the specified service and the service interface name
	 * @param registrar The bundle registering the extender
	 * @param serviceInterfaceName The class or interface name under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(Bundle owner, Bundle registrar,
			String serviceInterfaceName, Object service, Dictionary<String, ?> properties)
			throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(owner, registrar, serviceInterfaceName, service,
				properties);
		return extender;
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class name as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name and where the {@code registrar} is the bundle registering the
	 * extender.
	 * <p>
	 * This method is otherwise identical to
	 * {@link #register(Bundle, Bundle, String, Object, Dictionary)} and is provided to specify a type
	 * safe service.
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle hosting the specified service and the service interface name
	 * @param registrar The bundle registering the extender
	 * @param serviceInterfaceName The class or interface name under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(Bundle owner, Bundle registrar, Class<S> serviceInterface,
			S service, Dictionary<String, ?> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(owner, registrar, serviceInterface.getName(),
				service, properties);
		return extender;
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class name as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name and where the {@code registrar} is the bundle registering the
	 * extender.
	 * <p>
	 * This method is otherwise identical to
	 * {@link #register(Bundle, Bundle, String, Object, Dictionary)} and is provided as a convenience
	 * when the bundle registering the service also is the bundle hosting - or owing - the specified
	 * service
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle owing the the service registered by the returned extender
	 * @param serviceInterfaceName The class or interface name under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public static <S> Extender<S> register(Bundle owner, String serviceInterfaceName, Object service,
			Dictionary<String, ?> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(owner, owner, serviceInterfaceName, service,
				properties);
		return extender;
	}

	/**
	 * Unregister this extender and the service held by this extender. After unregistering the
	 * extender, the service is removed from the framework and any references to it is removed from
	 * the extender.
	 * <p>
	 * After unregistering the service is not accessible. Thus it is possible to access all fields
	 * associated with the service through the public access methods of the specified extender
	 * 
	 * @param the extender to unregister
	 * @see Extender#unregisterService()
	 */
	public static <S> void unregister(Extender<S> extender) {
		extender.unregister();
	}

	/**
	 * Release the service object held by this extender. The context bundle's use count for the
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
	 * Create an extension of the extender registered with the specified interface name
	 * <p>
	 * 
	 * @param interfaceName service interface name
	 * @user The bundle using the extension
	 * @return the extension interface or null if there is no registered service with the specified
	 * interface name
	 * @throws ExtenderException if fail to get the registered extension
	 */
	public static final <S> Extension<S> getExtension(String interfaceName, Bundle user)
			throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(interfaceName);
		Activator.ungetServiceMap();
		return null == extender ? null : extender.getExtension(user);
	}

	/**
	 * Create an extension of the extender registered with the specified interface name
	 * <p>
	 * This method is otherwise identical to {@link #getExtension(String, Bundle)} and is provided as
	 * a convenience when the {@code user} bundle is the same as the bundle that registered the
	 * extender for the specified interface name.
	 * 
	 * @param interfaceName service interface name
	 * @return the extension interface or null if there is no registered service with the specified
	 * interface name
	 * @throws ExtenderException if fail to get the registered extension
	 */
	public static final <S> Extension<S> getExtension(String interfaceName) throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(interfaceName);
		Activator.ungetServiceMap();
		return null == extender ? null : extender.getExtension();
	}

	/**
	 * Get the extender registered with the specified interface name
	 * 
	 * @param serviceInterfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the service is not tracked under the specified
	 * interface name.
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static final <S> Extender<S> getExtender(String serviceInterfaceName)
			throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceInterfaceName);
		Activator.ungetServiceMap();
		return extender;
	}

	/**
	 * Returns a list of extenders. The returned list contains extenders that were registered under
	 * the specified service interface name, match the specified filter expression, and the packages
	 * for the class names under which the services registered by the extender match the context
	 * bundle's packages as defined in {@link ServiceReference#isAssignableTo(Bundle, String)}.
	 * 
	 * The specified filter expression is used to select the registered extenders and their services
	 * whose service properties contain keys and values which satisfy the filter expression. See
	 * {@link Filter} for a description of the filter syntax. If the specified filter is null, all
	 * registered extenders are considered to match the filter. If the specified filter expression
	 * cannot be parsed, an ExtenderException will be thrown with a human readable message where the
	 * filter became unparsable.
	 * 
	 * @param serviceInterfaceName The class name with which the service was registered or
	 * {@code null} for all extenders.
	 * @param filter The filter expression or {@code null} for all extenders.
	 * @return a list of extenders matching the specified class service name and the specified filter
	 * or {@code null} if no extenders are registered which satisfy the search.
	 * @throws ExtenderException if this BundleContext of the extender bundle is no longer valid, a
	 * missing security permission or the the filter contains syntax errors
	 */
	public static final <S> Collection<Extender<S>> getExtenders(String serviceInterfaceName,
			String filter) throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Collection<Extender<S>> extenders = extServiceMap.get(serviceInterfaceName, filter);
		Activator.ungetServiceMap();
		return extenders;
	}
	
	/**
	 * Get an extender registered with the specified interface class
	 * <p>
	 * This extender access method has an additional check to verify if the interface class is loaded or
	 * not. A failure first indicates that the extender is not registered or the bundle
	 * hosting the class is not started yet and/or not loaded by or on behalf of the hosting bundle
	 * 
	 * @param interfaceClass service interface class
	 * @return the extender service
	 * @throws ExtenderException if fail to get the registered extender due to no
	 * registered service with the specified interface name
	 */
	public static <E> Extender<E> getExtender(Class<E> interfaceClass) throws ExtenderException {

		Extender<E> ext = null;
		try {
			Class<?> c = Class.forName(interfaceClass.getName());
			ext = getExtender(c.getName());
			if (null == ext) {
				throw new ExtenderException("Null extender for the using bundle");
			}
		} catch (ClassNotFoundException e) {
			// Bundle hosting the class is not started yet and/or not loaded by or on behalf of the hosting
			// bundle
			throw new ExtenderException(e, "Interface class for extender not found for the using bundle");
		}
		return ext;
	}

	/**
	 * Get all extenders tracked by the specified bundle tracker
	 * 
	 * @param bundleTracker the bundle tracker tracking extenders
	 * @return a collection of extenders tracked by the specified bundle tracker or an empty
	 * collection if no extenders are tracked by the specified bundle tracker. If the specified bundle
	 * tracker is null an empty collection is returned.
	 */
	public static final Collection<Extender<?>> getTrackedExtenders(
			BundleTracker<Collection<Extender<?>>> bundleTracker) {

		Collection<Extender<?>> trackedExtenders = new ArrayList<>();
		if (null != bundleTracker) {
			Map<Bundle, Collection<Extender<?>>> tracked = bundleTracker.getTracked();
			Iterator<Entry<Bundle, Collection<Extender<?>>>> it = tracked.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Collection<Extender<?>>> entry = it.next();
				for (Extender<?> e : entry.getValue()) {
					trackedExtenders.add(e);
				}
			}
		}
		return trackedExtenders;
	}

	/**
	 * Get all extenders tracked by the specified bundle using a bundle tracker
	 * <p>
	 * If the specified bundle has registered any extenders using a bundle tracker they are returned.
	 * 
	 * @param registrar the bundle registering any extenders by using a bundle tracker
	 * @return a collection of extenders registered by the specified bundle using a bundle tracker or
	 * an empty collection if no extenders have been registered with a bundle tracker by the specified
	 * bundle. If the specified bundle is null an empty collection is returned.
	 */
	public static final Collection<Extender<?>> getTrackedExtenders(Bundle registrar) {

		Collection<Extender<?>> trackedExtenders = new ArrayList<>();

		Collection<Extender<Object>> extenders = Extenders.getExtenders(null, null);
		for (Extender<?> extender : extenders) {
			Bundle registrarBundle = extender.getRegistrar();
			if (null != registrar && registrarBundle.equals(registrar)) {
				Collection<Extender<?>> tracked = extender.getTrackedExtenders();
				if (null != tracked) {
					trackedExtenders.addAll(tracked);
				}
			}
		}
		return trackedExtenders;
	}

	/**
	 * Get an extender based on its service id. When an extender is registered the unique service id
	 * generated by the framework is automatically used to store and later access an extender
	 * <p>
	 * If the extender could not be found it may have been unregistered or never registered by the
	 * specified key
	 * 
	 * @param serviceId the unique key to locate the extender
	 * @return the extender instance or null if the service is not tracked under the specified service
	 * id.
	 * @throws ExtenderException if the bundle context used to get the extender is not valid or a
	 * security permission is missing
	 */
	public static final <S> Extender<S> getExtender(Long serviceId) throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceId);
		Activator.ungetServiceMap();
		return extender;
	}

	/**
	 * Get an extender based on its service reference. The service reference is first available after
	 * the service has been registered and before unregistered
	 * <p>
	 * If the extender could not be found it may have been unregistered or not registered
	 * 
	 * @param serviceReference the unique service reference to locate the extender
	 * @return the extender instance or null if the service is not tracked under the specified service
	 * id.
	 * @throws ExtenderException if the bundle context used to get the extender is not valid or a
	 * security permission is missing
	 */
	public static final <S> Extender<S> getExtender(ServiceReference<S> serviceReference)
			throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceReference);
		Activator.ungetServiceMap();
		return extender;
	}
}
