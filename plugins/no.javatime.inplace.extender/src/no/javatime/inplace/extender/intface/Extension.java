package no.javatime.inplace.extender.intface;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


	/**
 * Extensions are users of extenders and the underlying services registered by extenders.
 * <p>
 * Create an extension by using {@link Extenders#getExtension(String, Bundle)} or
 * {@link Extender#getExtension(Bundle)}. There are also two convenience methods where the using
 * bundle is the same bundle as the one who registered the extender used by this extension.
 * 
 * @param <S> Type of Service
 * @see Extenders#getExtension(String, Bundle)
 * @see Extender#getExtension(String, Bundle)
 */
public interface Extension<S> {

	/**
	 * The bundle registered with this extension.
	 * 
	 * @return the bundle using this extension
	 */
	public Bundle getUserBundle();

	/**
	 * Get the service object for this extension where the using bundle is the bundle specified when
	 * this extension was created.
	 * <p>
	 * If {@link Extender#getExtension()} was used to create this extension, the using bundle is the
	 * registrar bundle specified when the extender was registered. See
	 * {@link Extender#getRegistrar()}.
	 * <p>
	 * The behavior of this extension service is the same as the
	 * {@link org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference) OSGi
	 * getService} method.
	 * 
	 * @return the service object or null if no service is being tracked.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService() throws ExtenderException;

	/**
	 * Get the service object for this extension. The specified bundle is the using bundle.
	 * <p>
	 * Uses the bundle context of the specified bundle. If a factory object was specified when the
	 * extender of this extension was registered each bundle is exposed to one instance of the service
	 * object. If instead a service object was specified this service object is shared among all
	 * bundles.
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
	 * Creates and opens a new service tracker if not already created.
	 * <p>
	 * There can be at most one tracker object per extension, but it is possible to close the tracker
	 * and then open a new one.
	 * @param customizer A service tracker to track the life cycle of the service. Can be null.
	 * 
	 * @throws ExtenderException
	 */
	public void openServiceTracker(ServiceTrackerCustomizer<S, S> customizer)
			throws ExtenderException;

	/**
	 * Uses the {@link org.osgi.util.tracker.ServiceTracker ServiceTracker} to get the service. This
	 * is the same as using {@link #getService()} without creating any tracker
	 * <p>
	 * If a tracker has not been created yet the tracker will be created and opened using a default
	 * customizer.
	 * <p>
	 * If the tracker is closed, it is opened before getting the tracked service.
	 * 
	 * @return the service of this extension type 
	 * @throws ExtenderException if this call force opening the service tracker and the bundle context
	 * used by the service tracker is no longer valid or the returned service from the framework is null
	 * @see #closeTrackedService()
	 */
	public S getTrackedService() throws ExtenderException;

	/**
	 * 
	 * @return The tracking count for the service tracker of this extension or -1 if the
	 * service tracker is not open
	 */
	public int getTrackingCount();

	/**
	 * This method should be called when the tracking of the service should end
	 * 
	 * @see #getTrackedService()
	 * @see #openServiceTracker(ServiceTrackerCustomizer)
	 */
	public void closeTrackedService();
}
