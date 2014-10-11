package no.javatime.inplace.extender.intface;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.util.tracker.BundleTracker;

public interface Extender<S> {

	public Extender<S> register(Bundle regBundle, Class<S> serviceInterfaceClass, String serviceName);

	public Extender<S> register(Bundle regBundle, String serviceInterfaceName, String serviceName);

	public Extender<S> register(Bundle ownerBundle, Bundle regBundle, Class<S> serviceInterfaceClass,
			String serviceName);

	public Extender<S> register(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			ServiceFactory<?> serviceFactory);

	public Extender<S> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName);

	public Extender<S> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			Class<S> serviceInterfaceClass, String serviceName);

	public Extension<S> getExtension();

	/**
	 * Get the service object for the extender using the context of the registration bundle specified
	 * when this extender was registered. This is the object created from the class specified as the
	 * service class or the class instantiated by the specified factory object when the extender
	 * service was registered.
	 * <p>
	 * This method uses the BundleContext of the registration bundle specified when this extender was
	 * created
	 * <p>
	 * The behavior of this extender service is the same as the OSGi
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
	 * getService} method.
	 * 
	 * @return the service object or null if no service is being tracked. That is if the bundle has
	 * been unregistered or the use count of the bundle has dropped to zero when a class object or
	 * name was used when registering the bundle. If a factory object was specified when registering
	 * the bundle a new service object is created and returned.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService() throws ExtenderException;

	/**
	 * Get the service object of this extender. This is the object created from the class specified as
	 * the service class or the class instantiated by the specified factory object when the extender
	 * service was registered.
	 * <p>
	 * The behavior of this extender service is the same as the OSGi
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
	 * getService} method.
	 * 
	 * @param bundle use the BundleContext of this bundle when getting the service object
	 * @return the service object or null if no service is being tracked. This usually means that the
	 * bundle context of the bundle providing the service is no longer valid or the use count of the
	 * bundle has dropped to zero.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService(Bundle bundle) throws ExtenderException;

	/**
	 * Get the service object of this extender. This is the object created from the class specified as
	 * the service class or the class instantiated by the specified factory object when the extender
	 * service was registered.
	 * <p>
	 * This method uses the BundleContext of the registration bundle specified when this extender was
	 * created
	 * <p>
	 * This is the same object as returned from {@link #getService(Bundle)} and {@link #getService()}
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
	 * The service class object implementing the interface service class of this extender service.
	 * This is the class specified as the service class or the class instantiated by the specified
	 * factory object when the extender service was registered.
	 * <p>
	 * The class is loaded by the class loader of the owner bundle specified at registering time.
	 * 
	 * @return the service class object of this extender service
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid or it is a
	 * fragment bundle, class is not found in the owner bundle, the bundle is in an illegal state
	 * (uninstalled, installed or resolved) or if the caller does not have the appropriate
	 * AdminPermission[this,CLASS], and the Java Runtime Environment supports permissions.
	 */
	public Class<S> getServiceClass() throws ExtenderException;

}
