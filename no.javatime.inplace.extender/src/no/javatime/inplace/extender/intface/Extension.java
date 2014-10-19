package no.javatime.inplace.extender.intface;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public interface Extension<S> {

	/**
	 * Get the service object for this extension.
	 * <p>
	 * Uses the bundle context of the specified bundle. If a factory object was specified when the
	 * extender of this extension was registered each bundle is exposed to one instance of the service
	 * object. If instead a service object was specified this service object is shared among all
	 * bundles.
	 * <p>
	 * Note: For OSGi R6 service scope may be used to specify service creation. The scopes are
	 * singleton (shared service), bundle (one service per bundle) and prototype (a new service for
	 * each call to {@link #getService()} and {@link #getService(Bundle)}
	 * <p>
	 * The behavior of this extension service is the same as the
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
	 * Get the service object for this extension.
	 * <p>
	 * Uses the bundle context of the specified bundle. If a factory object was specified when the
	 * extender of this extension was registered each bundle is exposed to one instance of the service
	 * object. If instead a service object was specified this service object is shared among all
	 * bundles.
	 * <p>
	 * Note: For OSGi R6 service scope may be used to specify service creation. The scopes are
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
	 * Release the service object held by this extension. The context bundle's use count for the
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
	 * Release the service object held by this extension. The specified context bundle's use count for
	 * the service is decremented by one.
	 * 
	 * @return false if the context bundle's use count for the service is zero or if the service has
	 * been unregistered; true otherwise.
	 * @see org.osgi.framework.BundleContext#ungetService(ServiceReference) OSGi ungetService
	 * @see #ungetService()
	 */
	public Boolean ungetService(Bundle bundle);

	/**
	 * Get the extender which this extension is part of
	 * 
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of this extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public Extender<S> getExtender() throws ExtenderException;

	/**
	 * Uses the {@link org.osgi.util.tracker.ServiceTracker ServiceTracker} to get the service. This
	 * is the same as using {@link #getService()}.
	 * 
	 * @return the service of this extension type or null if no service is being tracked
	 * @throws ExtenderException if this call force opening the service tracker and the bundle context
	 * used by the service tracker is no longer valid
	 * @see #closeServiceTracker()
	 */
	public S getTrackedService() throws ExtenderException;

	/**
	 * This method should be called when the tracking of the service should end
	 * 
	 * @see #getTrackedService()
	 */
	public void closeServiceTracker();
}
