package no.javatime.inplace.extender.intface;

import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

public interface Extender<S> {

	public static final String SCOPE = "scope";
	public static final String SINGLETON = "singleton";
	public static final String BUNDLE = "bundle";

	/**
	 * Create a new extension from this extender. Use the extension to access the service created when
	 * this extender was registered.
	 * <p>
	 * Note that if the bundle creating this extension is different from the bundle that registered
	 * the extender, and this is the first time an extension is created for this client bundle a new
	 * service object will be created if the service SCOPE is bundle; otherwise the service SCOPE will
	 * be singleton or prototype (only for OSGi R6 @see {@link Extenders})
	 * 
	 * @return a new extension object or null if there is no registered service for this extender.
	 */
	public Extension<S> getExtension();

	/**
	 * Get the service object for this extender.
	 * <p>
	 * Uses the bundle context of the specified bundle. If a factory object was specified when the
	 * extender was registered each bundle is exposed to one instance of the service object. If
	 * instead a service object was specified this service object is shared among all bundles.
	 * <p>
	 * Note: For OSGi R6 service SCOPE may be used to specify service creation. The scopes are
	 * singleton (shared service), bundle (one service per bundle) and prototype (a new service for
	 * each call to {@link #getService()} and {@link #getService(Bundle)}
	 * <p>
	 * The behavior of this extender service is the same as the
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference) OSGi
	 * getService} method.
	 * 
	 * @return the service object for for the interface specified at construction of this extension
	 * object or null if no service is being tracked.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService() throws ExtenderException;

	/**
	 * Get the service object for this extender.
	 * <p>
	 * Uses the bundle context of the specified bundle. If a factory object was specified when the
	 * extender was registered each bundle is exposed to one instance of the service object. If
	 * instead a service object was specified this service object is shared among all bundles.
	 * <p>
	 * Note: For OSGi R6 service SCOPE may be used to specify service creation. The scopes are
	 * singleton (shared service), bundle (one service per bundle) and prototype (a new service for
	 * each call to {@link #getService()} and {@link #getService(Bundle)}
	 * <p>
	 * The behavior of this extender service is the same as the
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference) OSGi
	 * getService} method.
	 * 
	 * @param bundle the user bundle requesting the service
	 * @return the service object for for the interface specified at construction of this extension
	 * object or null if no service is being tracked.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService(Bundle bundle) throws ExtenderException;

	/**
	 * The service class object of this extender service. The class object is loaded using the service
	 * object or the service class name specified when this extender was registered.
	 * <p>
	 * The class object is loaded by the class loader of the owner bundle specified at registering
	 * time.
	 * 
	 * @return the service class object of this extender service
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid or it is a
	 * fragment bundle, class is not found in the owner bundle, the bundle is in an illegal state
	 * (uninstalled, installed or resolved) or if the caller does not have the appropriate
	 * AdminPermission[this,CLASS], and the Java Runtime Environment supports permissions.
	 */
	public Class<S> getServiceClass() throws ExtenderException;

	/**
	 * Get the service object of this extender. This is the the service object or the factory object
	 * specified when this extender was registered.
	 * <p>
	 * If the service instead was registered with a service class name the service object is created
	 * from this name using the owner bundle specified when the extender was registered. Service
	 * objects - including service factory objects - that are created must have a default constructor.
	 * <p>
	 * If SCOPE is Bundle {@link #getService(Bundle)} returns a new service object otherwise a shared
	 * (Singleton SCOPE) service object is returned from both {@link #getService(Bundle)}and
	 * {@link #getService()}
	 * 
	 * @return the service object of this extender
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the registration bundle or if the caller does
	 * not have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public Object getServiceObject() throws ExtenderException;

	/**
	 * Get the service interface class name object of this extender. This is the same as the interface
	 * class or name specified when this service extender was registered.
	 * 
	 * @return the service interface class name of this extender
	 */
	public String getServiceInterfaceName();

	/**
	 * Get the service interface class object of this extender. This is the interface class object
	 * specified when this service extender was registered.
	 * 
	 * @return the service interface class object of this extender
	 */
	public Class<S> getInterfaceServiceClass() throws ExtenderException;

	/**
	 * Get the service reference for this extender
	 * 
	 * @return A serviceReference object, or null if no service is registered or has been unregistered
	 * @throws ExtenderException Missing appropriate AdminPermission[this,CONTEXT], and the Java
	 * Runtime Environment supports permissions.
	 */
	public ServiceReference<S> getServicereReference() throws ExtenderException;

	/**
	 * Get the service id for this extender.
	 * <p>
	 * The service id is the unique key used to locate and get an extender
	 * 
	 * @see Extenders#getExtender(Long)
	 * @see org.osgi.framework.Constants#SERVICE_ID
	 * @see org.osgi.framework.BundleContext#getProperty(String)
	 */
	public Long getServiceId();

	/**
	 * Register a service for this extender. If the service is unregistered this implies that it has
	 * been unregistered earlier, either by {@link #unregisterService()} or by
	 * {@link org.osgi.framework.ServiceRegistration#unregister() OSGi unregister}
	 * 
	 * @return true if the service was registered; false if the service already was registered
	 * @throws ExtenderException if the bundle context of the extension is no longer valid, the
	 * service was not registered by this framework, the implementation class not owned by the bundle
	 * registering the service, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal or if the service object could not be obtained
	 */

	public Boolean registerService() throws ExtenderException;

	/**
	 * Check if the service is registered by the framework
	 * <p>
	 * If the service held by this extender has been unregistered see {@link #registerService()} to
	 * re-register the service for this extender.
	 * 
	 * @return true if the service is registered; false if unregistered
	 */
	public Boolean isServiceRegistered();

	/**
	 * Unregister this extender and the service held by this extender. After unregistering the
	 * extender, the service is removed from the framework and any references to it is removed from
	 * the extender. After unregistering the service can not be accessed.
	 * <p>
	 * After unregistering it is not possible to lookup the extender again using
	 * {@link #get(String)} or {@link #getExtension(String)}, but you can register the
	 * extender an a new service again with the specified extender parameter by using
	 * {@link #registerService(Extender)} or {@link Extender#registerService()}.
	 * @throws ExtenderException If the service registered with this extender already has been unregistered
	 * @see Extender#unregisterService()
	 */
	public void unregisterService() throws ExtenderException;

	/**
	 * Release the service object held by this extender. The context bundle's use count for the
	 * service is decremented by one.
	 * <p>
	 * By default the registrar bundle specified when registering this extender is also the user
	 * bundle. It is also possible to supply the user bundle as a parameter when getting or ungetting
	 * the service from an extender or from an extension belonging to an extender.
	 * 
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise; true otherwise
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService(Bundle)
	 */
	public Boolean ungetService();

	/**
	 * Release the service object held by this extender. The specified context bundle's use count for
	 * the service is decremented by one.
	 * 
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise.
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService()
	 */
	public Boolean ungetService(Bundle bundle);

	/**
	 * Get the bundle tracker used when this extender was registered
	 * 
	 * @return the bundle tracker or null if the extender was not registered with a bundle tracker
	 */
	public BundleTracker<Extender<?>> getBundleTracker();

	public Collection<Extender<?>> getTrackedExtenders();

	public Dictionary<String, ?> getProperties();

	public boolean setProperty(String key, Object property);

	/**
	 * Return the owner bundle specified when this extender was registered.
	 * <p>
	 * The bundle class loader of this bundle is used when the service is specified as a class name at
	 * registration time. This means that the service class object will be loaded with the class
	 * loader of this owner bundle when specified as a class name.
	 * 
	 * @return the bundle object or null if the bundle does not exist
	 */
	public Bundle getOwnerBundle();

	/**
	 * Return the registrar bundle specified when this extender was registered.
	 * <p>
	 * By default this bundle is also the user bundle when getting the service or an extension
	 * belonging to this extender. It is also possible to supply the user bundle as a parameter when
	 * getting the service from this extender or from an extension belonging to this extender.
	 * 
	 * @return the bundle object or null if the bundle does not exist
	 * @see #getService(Bundle)
	 * @see Extension#getService(Bundle)
	 */
	public Bundle getRegistrarBundle();
	
	public Collection<Bundle> getUsingBundles();

}
