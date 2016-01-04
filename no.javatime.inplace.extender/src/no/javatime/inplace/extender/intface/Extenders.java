package no.javatime.inplace.extender.intface;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

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
	 * Check if the extender is registered
	 * <p>
	 * If registered, the underlying service registered by this extender is also registered. If
	 * unregistered the service is also unregistered
	 * 
	 * @return true if the extender is registered; false if unregistered
	 */
	public static <S> Boolean isRegistered(String serviceIntefaceName) {

		Extender<S> extender = getExtender(serviceIntefaceName);
		return null != extender ? extender.isRegistered() : false;
	}

	/**
	 * Create an extension of the extender registered with the specified interface name
	 * <p>
	 * 
	 * @param serviceInterfaceName service interface name
	 * @user The bundle using the extension
	 * @return An extension of the extender with highest priority with the given service interface
	 * name
	 * @throws ExtenderException if fail to get the registered extension or no extender were
	 * registered with the specified interface name
	 */
	public static final <S> Extension<S> getExtension(String serviceInterfaceName, Bundle user)
			throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceInterfaceName);
		Activator.ungetServiceMap();
		if (null == extender) {
			throw new ExtenderException(
					"Found no extender registered with the specified interface name {0}",
					serviceInterfaceName);
		}
		return extender.getExtension(user);
	}

	/**
	 * Create an extension of the extender registered with the specified interface name
	 * <p>
	 * 
	 * @param serviceInterfaceName The service interface name
	 * @param user The bundle using the extension
	 * @param bt The bundle tracker unget service is called when the service tracker used by the
	 * extension is closed
	 * @return The extension based on the extender with the highest priority
	 * @throws ExtenderException if fail to get the registered extension or no extender were
	 * registered with the specified interface name
	 */
	public static final <S> Extension<S> getExtension(String serviceInterfaceName, Bundle user,
			ExtenderBundleTracker bt) throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceInterfaceName);
		Activator.ungetServiceMap();
		if (null == extender) {
			throw new ExtenderException(
					"Found no extender registered with the specified interface name {0}",
					serviceInterfaceName);
		}
		return extender.getExtension(user, bt);
	}

	/**
	 * Create an extension of the extender registered with the specified interface name
	 * <p>
	 * This method is otherwise identical to {@link #getExtension(String, Bundle)} and is provided as
	 * a convenience when the {@code user} bundle is the same as the bundle that registered the
	 * extender for the specified interface name.
	 * 
	 * @param serviceInterfaceName service interface name
	 * @return the extension interface or null if there is no registered service with the specified
	 * interface name
	 * @throws ExtenderException if fail to get the registered extension or no extender were
	 * registered with the specified interface name
	 */
	public static final <S> Extension<S> getExtension(String serviceInterfaceName)
			throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Extender<S> extender = extServiceMap.get(serviceInterfaceName);
		Activator.ungetServiceMap();
		if (null == extender) {
			throw new ExtenderException(
					"Found no extender registered with the specified interface name {0}",
					serviceInterfaceName);
		}
		return extender.getExtension();
	}

	/**
	 * Get the tracked service object registered with the specified service class instance
	 * 
	 * @param service class instance of the service to return
	 * @return the service object of the specified class instance or null if no service is tracked for
	 * the specified class instance (extender is null)
	 * @throws ExtenderException if the service is null, bundle context of the owner bundle is not
	 * valid, the bundle is in an illegal state (uninstalled, installed or resolved), the service was
	 * not created by the same framework instance as the BundleContext of the specified bundle or if
	 * the caller does not have the appropriate AdminPermission[this,CLASS], and the Java Runtime
	 * Environment supports permissions.
	 */
	public static <S> S getService(Class<S> service) throws ExtenderException {

		Extender<S> extender = getExtender(service.getName());
		return null != extender ? extender.getService() : null;
	}

	/**
	 * Get the tracked service object registered with the specified service class instance
	 * 
	 * @param service class instance of the service to return
	 * @param userBundle The bundle using the returned service object
	 * @return the service object of the specified class instance or null if no service is tracked for
	 * the specified class instance (extender is null)
	 * @throws ExtenderException if the service is null, bundle context of the owner bundle is not
	 * valid, the bundle is in an illegal state (uninstalled, installed or resolved), the service was
	 * not created by the same framework instance as the BundleContext of the specified bundle or if
	 * the caller does not have the appropriate AdminPermission[this,CLASS], and the Java Runtime
	 * Environment supports permissions.
	 */
	public static <S> S getService(Class<S> service, Bundle userBundle) throws ExtenderException {

		Extender<S> extender = getExtender(service.getName(),userBundle);
		return null != extender ? extender.getService() : null;
	}

	/**
	 * Get the extender registered with the specified interface name
	 * 
	 * @param serviceInterfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the service is not tracked under the specified
	 * interface name.
	 * @throws ExtenderException If no extender were registered with the specified interface name
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
	 * @throws ExtenderException If no extender were registered with the specified interface names or
	 * the the filter contains syntax errors
	 */
	public static final <S> Collection<Extender<S>> getExtenders(String serviceInterfaceName,
			String filter) throws ExtenderException {

		ExtenderServiceMap<S> extServiceMap = Activator.getExtenderServiceMap();
		Collection<Extender<S>> extenders = extServiceMap.get(serviceInterfaceName, filter);
		Activator.ungetServiceMap();
		return extenders;
	}

	/**
	 * Get an extender with the specified service interface name owned and registered by the specified
	 * source bundle. If there are more than one extender owned and registered by the bundle hosting
	 * the extender service the first one encountered is returned.
	 * <p>
	 * Using an extender registered by the bundle hosting or owning the extender service guarantees
	 * that the using bundle is only dependent on the bundle owing the service registered with the
	 * extender. If there are no such extenders, registering a new extender result in the same
	 * dependency as using an extender where the owner and registrar bundle is the same.
	 * 
	 * @param serviceInterfaceName the service interface owned and registered by the specified bundle
	 * @param sourceBundle An extender owned and registered by this source bundle. Extenders are
	 * always owned by one bundle, but may be registered by multiple bundles
	 * 
	 * @return An extender owned and registered by the specified bundle with the specified service
	 * interface name or null if no extenders have been registered with a bundle with the specified
	 * service interface name.
	 */
	public static <S> Extender<S> getExtender(String serviceInterfaceName, Bundle sourceBundle) {

		Collection<Extender<S>> extenders = Extenders.getExtenders(serviceInterfaceName, null);
		if (null != extenders) {
			for (Extender<S> extender : extenders) {
				Bundle ownerBundle = extender.getOwner();
				Bundle registrBundle = extender.getRegistrar();
				if (ownerBundle.equals(sourceBundle) && registrBundle.equals(sourceBundle)) {
					return extender;
				}
			}
		}
		return null;
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
