package no.javatime.inplace.extender.intface;

import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

/**
 * Manage services by using an extender that allows for registration of services on behalf of other
 * bundles. An extender abstracts the registration and usage of OSGi services and can be used
 * interchangeably with service API.
 * <p>
 * Register an extender by using the register methods in {@link Extenders} and
 * {@link ExtenderBundleTracker}.
 * <p>
 * To register an extender, the bundle hosting - or owing - and the bundle registering the extender
 * is required. If the owner and registrar bundle is the same, only the owner bundle is required
 * when registering the extender. The bundle registering the extender also becomes the default using
 * bundle when accessing the service registered by the extender. For other bundles using a
 * registered extender service {@link #getService(Bundle)} or the {@link #getExtension(Bundle)} may
 * be used.
 * <p>
 * To register an extender, the same requirements as registering a service with the Framework
 * service API apply.
 * <p>
 * Unregistering a service with the service API automatically unregisters the extender that
 * registered that service.
 * <p>
 * Updating properties using the service API or the extender interface after registration is
 * reflected in both the extender and the Framework.
 * 
 * @param <S> Type of Service
 * @see ExtenderBundleTracker
 * @see Extenders
 * @see Extension
 * 
 */
public interface Extender<S> {

	public static final String SCOPE = "scope";
	public static final String SINGLETON = "singleton";
	public static final String BUNDLE = "bundle";
	public static final String PROTOTYPE = "prototype";

	/**
	 * All underlying services registered by extenders are assigned this service property. If the
	 * service property value is set to {@code false} or the property is removed the extender is
	 * unregistered, but not the underlying service registered by the extender.
	 * <p>
	 * Using this mechanism - detaching the extender from the service - makes it possible to only use
	 * the service Framework after a service has been registered by an extender.
	 */
	public static final String EXTENDER_FILTER = "(" + Extender.class.getSimpleName().toLowerCase()
			+ "=true)";

	/**
	 * Create a new extension from this extender. Use the extension to access this extender and the
	 * service registered by this extender.
	 * <p>
	 * The specified service interface name must be one of the names this extender was registered
	 * with. Specifically {@code getServiceInterfaceNames().contains(serviceInterfaceName)} must
	 * return {@code true}
	 * <p>
	 * The using bundle may be any bundle wishing to use this extender.
	 * <p>
	 * If the specified user bundle creating this extension is different from the bundle that
	 * registered the extender, and this is the first time an extension is created for this user
	 * bundle a new service object will be created when {@link Extension#getService()} is invoked and
	 * the service scope is bundle; otherwise the service scope will be singleton or prototype
	 * (prototype is only for OSGi R6. See {@link Extenders}).
	 * 
	 * @param <S> Type of Service
	 * @param serviceInterfaceName a service interface name that is equal to one of the service
	 * interface names specified when this extender was registered
	 * @param user this is the bundle using the new extension
	 * @return a new extension object
	 * @throws ExtenderException if the specified service interface name is not equal to one of the
	 * service interface names specified when this extender was registered
	 * @see #getExtension()
	 */
	public Extension<S> getExtension(String serviceInterfaceName, Bundle user)
			throws ExtenderException;

	/**
	 * Create a new extension from this extender. Use the extension to access this extender and the
	 * service registered by this extender.
	 * <p>
	 * If this extender was registered with multiple interface service names, the first registered
	 * service interface name is used.
	 * <p>
	 * This method is otherwise identical to {@code getExtension(getServiceInterfaceName(), user)} and
	 * is provided as a convenience when the service interface name is the only or the first service
	 * interface name specified when this extender was registered
	 * <p>
	 * 
	 * @param <S> Type of Service
	 * @param user this is the bundle using the new extension
	 * @return a new extension object
	 * @see #getExtension(String, Bundle)
	 */
	public Extension<S> getExtension(Bundle user);

	/**
	 * Create a new extension from this extender. Use the extension to access this extender and the
	 * service registered when this extender was registered.
	 * <p>
	 * This method is otherwise identical to
	 * {@code getExtension(getServiceInterfaceName(), getRegistrar())} and is provided as a
	 * convenience when the service interface name is the only or the first service interface name
	 * specified when this extender was registered and {@code user} bundle is the same as the bundle
	 * that registered this extender.
	 * 
	 * @param <S> Type of Service
	 * @return a new extension object
	 * @see #getExtension(String, Bundle)
	 */
	public Extension<S> getExtension();

	/**
	 * Get the service object for this extender where the specified {@code user} bundle is the using
	 * bundle of the returned service.
	 * <p>
	 * If a factory object was specified when the extender was registered each {@code user} bundle is
	 * exposed to one instance of the service object (bundle scope). If instead a service object or a
	 * service class name was specified the service object is shared among all bundles (singleton
	 * scope).
	 * <p>
	 * Note: For OSGi R6 service scope may be used to specify service creation. The scopes are
	 * singleton (shared service), bundle (one service per bundle) and prototype (a new service
	 * instance for each call to {@link #getService()} and {@link #getService(Bundle)}
	 * <p>
	 * The behavior of this extender service is the same as the
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference) OSGi
	 * getService} method.
	 * 
	 * @param <S> Type of Service
	 * @param user the user bundle requesting the service
	 * @return the service object for the interface specified at construction of this extender object
	 * @throws ExtenderException if the service is null, bundle context of the owner bundle is not
	 * valid, the bundle is in an illegal state (uninstalled, installed or resolved), the service was
	 * not created by the same framework instance as the BundleContext of the specified bundle or if
	 * the caller does not have the appropriate AdminPermission[this,CLASS], and the Java Runtime
	 * Environment supports permissions.
	 */
	public S getService(Bundle user) throws ExtenderException;

	/**
	 * Get the service object for this extender where the bundle who registered this extender is the
	 * using bundle of the returned service.
	 * <p>
	 * <p>
	 * This method is otherwise identical to {@link #getService(Bundle)} and is provided as a
	 * convenience when the {@code user} bundle is the same as the bundle that registered this
	 * extender.
	 * 
	 * @param <S> Type of Service
	 * @return the service object for for the interface specified at construction of this extension
	 * object
	 * @throws ExtenderException if the service is null, bundle context of the owner bundle is not
	 * valid, the bundle is in an illegal state (uninstalled, installed or resolved), the service was
	 * not created by the same framework instance as the BundleContext of the specified bundle or if
	 * the caller does not have the appropriate AdminPermission[this,CLASS], and the Java Runtime
	 * Environment supports permissions.
	 */
	public S getService() throws ExtenderException;

	/**
	 * The service class object of this extender. The class object is loaded using the service object
	 * or the service class name specified when this extender was registered.
	 * <p>
	 * The class object is loaded by the class loader of the {@code owner} bundle specified at
	 * registering time.
	 * 
	 * @param <S> Type of Service
	 * @return the service class object of this extender
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid or it is a
	 * fragment bundle, class is not found in the owner bundle, the bundle is in an illegal state
	 * (uninstalled, installed or resolved) or if the caller does not have the appropriate
	 * AdminPermission[this,CLASS], and the Java Runtime Environment supports permissions.
	 */
	public Class<S> getServiceClass() throws ExtenderException;

	/**
	 * Get the service factory object of this extender. This is the service factory object specified
	 * when this extender was registered.
	 * 
	 * @return If the service was registered with a service factory object the factory object is
	 * returned, otherwise null is returned.
	 */
	public ServiceFactory<S> getServiceFactoryObject();

	/**
	 * Get the service interface class name of this extender.
	 * <p>
	 * This is the same as the interface class name specified when this extender was registered.
	 * <p>
	 * If more than one interface name was specified when this extender was registered the first
	 * interface name specified is returned.
	 * 
	 * @return the service interface class name of this extender or the first interface class name if
	 * more than one interface service class name was specified when the service was registered.
	 * @see #getServiceInterfaceNames()
	 */
	public String getServiceInterfaceName();

	/**
	 * Get the service interface class name(s) of this extender.
	 * <p>
	 * This is the same as the interface class names specified when this service extender was
	 * registered.
	 * <p>
	 * If only one interface name was specified when this extender was registered,
	 * {@link #getServiceInterfaceName()} may be used
	 * 
	 * @return the service interface class names of this extender
	 */
	public Collection<String> getServiceInterfaceNames();

	/**
	 * Get the service interface class object of this extender.
	 * <p>
	 * This is the interface class object specified when this service extender was registered.
	 * 
	 * @param <S> Type of Service
	 * @return the service interface class object of this extender
	 */
	public Class<S> getServiceInterfaceClass() throws ExtenderException;

/**
	 * Get the service reference for this extender
	 * 
	 * This is the same as {@link BundleContext#getServiceReference(Class)} and
	 * {@link BundleContext#getServiceReference(String)
	 * 
	 * @return A serviceReference object
	 * @throws ExtenderException If the service has been unregistered.
	 */
	public ServiceReference<S> getServiceReference() throws ExtenderException;

	/**
	 * Get the service id for this extender.
	 * <p>
	 * The service id is accessible after the extender is unregistered.
	 * 
	 * @return the service id
	 * @see Extenders#getExtender(Long)
	 * @see org.osgi.framework.Constants#SERVICE_ID
	 * @see org.osgi.framework.BundleContext#getProperty(String)
	 */
	public Long getServiceId();

	/**
	 * Get the ranking of this extender. This is the same as the service ranking.
	 * 
	 * @return the ranking of this service
	 */
	public Integer getServiceRanking();

	/**
	 * Check if the extender is registered
	 * <p>
	 * If registered, the underlying service registered by this extender is also registered. If
	 * unregistered the service is also unregistered, but with one exception. That is when the
	 * {@link Extender#EXTENDER_FILTER} property is removed or the value of the property is set to
	 * {@code false}
	 * 
	 * @return true if the extender is registered; false if unregistered
	 * @see #EXTENDER_FILTER
	 */
	public Boolean isRegistered();

	/**
	 * Unregister this extender along with the service registered by this extender. After
	 * unregistering the extender, the service is unregistered from the framework.
	 * 
	 * @throws ExtenderException If the service registered with this extender already has been
	 * unregistered
	 * @see Extender#unregisterService()
	 */
	public void unregister() throws ExtenderException;

	/**
	 * Release the service object held by this extender. The specified context bundle's use count for
	 * the service is decremented by one.
	 * 
	 * @param bundle the user bundle of the service
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise.
	 * @throws ExtenderException if the context of the using bundle is no longer valid or the service
	 * was created by another instance of the framework
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService()
	 */
	public Boolean ungetService(Bundle bundle) throws ExtenderException;

	/**
	 * Release the service object held by this extender. The context bundle's use count for the
	 * service is decremented by one.
	 * <p>
	 * <p>
	 * This method is otherwise identical to {@link #ungetService(Bundle)} and is provided as a
	 * convenience when the {@code user} bundle is the same as the bundle that registered this
	 * extender.
	 * 
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise
	 * @throws ExtenderException if the context of the using bundle is no longer valid or the service
	 * was created by another instance of the framework
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService(Bundle)
	 */
	public Boolean ungetService() throws ExtenderException;

	/**
	 * Get the bundle tracker tracking this extender
	 * <p>
	 * An extender can be registered and tracked by a bundle tracker by using
	 * {@link ExtenderBundleTracker#register(Bundle, String[], Object, Dictionary)} or
	 * {@link ExtenderBundleTracker#register(Bundle, String, Object, Dictionary)}
	 * 
	 * @return the bundle tracker or null if the extender was not registered and tracked with a bundle
	 * tracker
	 */
	public BundleTracker<Collection<Extender<?>>> getBundleTracker();

	/**
	 * If this extender was registered with a bundle tracker, this and all other extenders registered
	 * with the bundle tracker are returned.
	 * <p>
	 * 
	 * @return the extenders registered with the same bundle tracker as this extender. The returned
	 * extenders will always contain this extender or an empty collection if this extender was not
	 * registered with a bundle tracker.
	 * @see #getBundleTracker()
	 */
	public Collection<Extender<?>> getTrackedExtenders();

	/**
	 * Get all registered properties.
	 * <p>
	 * Properties are accessible after the extender is unregistered.
	 * 
	 * @return The registered properties
	 */
	public Dictionary<String, Object> getProperties();

	/**
	 * Get all registered property keys.
	 * <p>
	 * Properties are accessible after the extender is unregistered.
	 * 
	 * @return The registered property keys
	 */
	public String[] getPropertyKeys();

	/**
	 * Returns the property value to which the specified property key is mapped. Property keys are
	 * case-insensitive.
	 * <p>
	 * Properties are accessible after the extender is unregistered.
	 * 
	 * @param key The property key
	 * @return The property value to which the key is mapped; null if there is no property named after
	 * the key
	 */
	public Object getProperty(String key);

	/**
	 * Updates the properties with the specified key and its associated value
	 * 
	 * @param key Property key
	 * @param value Property value
	 * @throws ExtenderException If the extender has been unregistered or the property is a case
	 * variant of the same key name
	 */
	public void setProperty(String key, Object value) throws ExtenderException;

	/**
	 * Replace the properties with the provided properties
	 * 
	 * @param dictionary The set of properties to replace with the existing set of properties
	 */
	public void setProperties(Dictionary<String, ?> dictionary);

	/**
	 * Return the owner bundle specified when this extender was registered.
	 * <p>
	 * The owner bundle is the bundle containing or hosting the service and interface classes of the
	 * service registered with this extender.
	 * <p>
	 * It is the owner bundle class loader of the service class that is used when the name of the
	 * service class is specified at registration time.
	 * 
	 * @return the bundle object owing - or hosting - the service registered by this extender
	 * @throws ExtenderException If the context of the owner bundle is invalid
	 */
	public Bundle getOwner() throws ExtenderException;

	/**
	 * Return bundle which registered this extender. The bundle registering this extender was
	 * specified when this extender was created.
	 * <p>
	 * By default this bundle is also the user bundle when getting the service or an extension
	 * belonging to this extender. It is also possible to supply a different user bundle - when other
	 * than the registrar is using the extender - as a parameter when getting the service from this
	 * extender or from an extension belonging to this extender.
	 * 
	 * @return the bundle object that registered this extender
	 * @throws ExtenderException If the context of the registrar bundle is invalid
	 * @see #getService()
	 * @see #getService(Bundle)
	 * @see Extension#getService(Bundle)
	 */
	public Bundle getRegistrar() throws ExtenderException;

	/**
	 * Returns the bundles that are using the service registered by this extender. Specifically, this
	 * method returns the bundles whose usage count for that service is greater than zero.
	 * 
	 * @return A collection of bundles whose usage count of the service registered by this extender is
	 * greater than zero; an empty collection if no bundles are currently using the service registered
	 * by this extender.
	 */
	public Collection<Bundle> getUsingBundles();
}
