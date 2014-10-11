package no.javatime.inplace.extender.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.extender.intface.Introspector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Manage extensions. The extender provides functionality for examination and direct use of the
 * extension as a service or through introspection.
 * <p>
 * When registered, the extension is automatically registered as a service or if a service for the
 * extension is available that service is used.
 * <p>
 * The extender may also be used directly to access a registered service
 * 
 * @param <S> The type of the service being registered.
 */
public class ExtenderImpl<S> implements Extender<S> {

	public static final String filter = "(" + ExtenderImpl.class.getSimpleName() + "=true)";
	
	private BundleContext context = Activator.getContext();

	/** Extenders interface definition name */
	final private String serviceInterfaceName;
	/** Interface class of the extension */
	private Class<S> serviceInterfaceClass;
	/** Bundle id of the bundle providing the extension */
	final private Long ownerBId;
	/** Bundle id of the bundle registering the extension */
	private Long regBId;
	/** Name of the implementation class */
	private String serviceName;
	/** Implementation class loaded by extender or obtained from service registry */
	private Class<S> serviceClass;
	/** Service registration object returned when the service is registered */
	private ServiceRegistration<?> serviceRegistration;
	/** Service tracker used by the extension */
	private ServiceTracker<S, S> tracker;
	/** The bundle tracker holding extender objects registered by a bundle tracker */
	private BundleTracker<Extender<?>> bundleTracker;
	/** The service of this extender is a service factory */
	private ServiceFactory<?> serviceFactory;

	public Extender<S> register(Bundle regBundle, Class<S> serviceInterfaceClass, String serviceName) {
		return register(regBundle, regBundle, serviceInterfaceClass.getName(), serviceName);
	}

	public Extender<S> register(Bundle regBundle, String serviceInterfaceName, String serviceName) {
		return register(regBundle, regBundle, serviceInterfaceName, serviceName);
	}

	public Extender<S> register(Bundle ownerBundle, Bundle regBundle, Class<S> serviceInterfaceClass,
			String serviceName) {
		return register(ownerBundle, regBundle, serviceInterfaceClass.getName(), serviceName);
	}

	public static <S> ExtenderImpl<S> register(Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName) {
		ExtenderImpl<S> extender = new ExtenderImpl<>(ownerBundle, regBundle, serviceInterfaceName,
				serviceName);
		return ExtenderImpl.register(extender);
	}

	public Extender<S> register(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			ServiceFactory<?> serviceFactory) {

		ExtenderImpl<S> extender = new ExtenderImpl<>(ownerBundle, regBundle, serviceInterfaceName,
				serviceFactory);
		return ExtenderImpl.register(extender);
	}

	// Register with bundle tracker

	public Extender<S> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			Class<S> serviceInterfaceClass, String serviceName) {
		return register(bt, ownerBundle, regBundle, serviceInterfaceClass.getName(), serviceName);
	}

	/**
	 * Creates an extender object, registers it as a service and creates and opens a service tracker
	 * for access to the service object. If a service already exist the extender attach itself to it
	 * and creates and opens a tracker for that service if one does not exists.
	 * <p>
	 * An extender can be registered from a bundle tracker supplying the tracker to one of the
	 * register members. If the bundle is tracked by the bundle tracker, the extender can get
	 * information about the service life cycle events from the tracker.
	 * <P>
	 * Example registering:
	 * <ol>
	 * <li>ExtenderImpl.register(getTracker(), bundle.getBundleId(), serviceInterfaceName,
	 * serviceImplClassName);
	 * </ol>
	 * <p>
	 * After the extender service is registered, the service object can be obtained in one of the
	 * following ways:
	 * <p>
	 * <ol>
	 * <li>Directly from the returned registered extender object by calling {@link #getExtension()}
	 * <li>Create an extension object {@link no.javatime.inplace.extender.provider.ExtensionImpl
	 * Extenders} and call the {@link Extenders#getService()} member.
	 * <li>Call the static {@linkplain ExtenderImpl#getExtender(String)} method to obtain an extender
	 * object and than call {@link ExtenderImpl#getExtension()} on the returned extender object
	 * </ol>
	 * <p>
	 * Example usage:
	 * <ol>
	 * <li>Extenders<ServiceInterface> ex = new Extenders<>(serviceInterfaceName);
	 * <li>ServiceInterface so = ex.getService();
	 * <li>so.getName(); // A method defined in the service object
	 * </ol>
	 * <P>
	 * 
	 * @param ownerBundle the bundle id of the bundle providing an extension implementation
	 * @param regBundle TODO
	 * @param serviceInterfaceName the interface name of the extension
	 * @param servicename the name of the class implementing the extension interface
	 * 
	 * @see ExtenderBundleTracker
	 */
	public Extender<S> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName) {
		ExtenderImpl<S> extender = new ExtenderImpl<>(bt, ownerBundle, regBundle, serviceInterfaceName,
				serviceName);
		return ExtenderImpl.register(extender);
	}

	@SuppressWarnings("unused")
	private static <S> void setBt(ExtenderImpl<S> extender, BundleTracker<ExtenderImpl<S>> bt) {
		// extender.bundleTracker = bt;
	}

	private static <S> ExtenderImpl<S> register(ExtenderImpl<S> extender) throws ExtenderException {

		// Check if the extender service map is registered and get the extension for this service to
		// register
		// extender.extenderServices = ExtenderImpl.getExtenderMapService();
		if (null != extender.serviceFactory || null != extender.getInterfaceServiceClass()) {
			extender.registerService();
			Long sid = (Long) extender.getServicereReference().getProperty(Constants.SERVICE_ID);
			ExtenderMapService<Long, ExtenderImpl<S>> extMapService = getExtenderMapService();
			extMapService.put(sid, extender);
			// extender.extenderMapService.put(sid, extender);
			Activator
					.getDefault()
					.getExtenderListener()
					.serviceChanged(
							new ServiceEvent(ServiceEvent.REGISTERED, extender.getServicereReference()));
			return extender;
		}
		return null;
	}

	// Constructors
	public ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			String serviceName) {
		this.ownerBId = ownerBundle.getBundleId();
		this.regBId = regBundle.getBundleId();
		this.serviceInterfaceName = serviceInterfaceName;
		this.serviceName = serviceName;
		this.bundleTracker = null;
		// this.extenderMapService = ExtenderImpl.getExtenderMapService();
	}

	private ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			ServiceFactory<?> serviceFactory) {
		this(ownerBundle, regBundle, serviceInterfaceName, (String) null);
		this.serviceFactory = serviceFactory;
	}

	/**
	 * Creates an extender object with sufficient information about the extension to provide a service
	 * for it.
	 * <p>
	 * The extender manages the life cycle and allocation/deallocation of resources used by the
	 * extension.
	 * <p>
	 * This class is typically instantiated from the extender bundle tracker when extension bundles
	 * are activated. To maintain an extension through direct instantiation, this extender class
	 * should be sub classed.
	 * 
	 * @param ownerBundle the bundle id of the bundle providing an extension implementation
	 * @param regBundle TODO
	 * @param serviceInterfaceName the interface name of the extension
	 * @param serviceInterfaceName the name of the class implementing the extension interface
	 * 
	 * @see ExtenderBundleTracker
	 */
	private ExtenderImpl(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName) {
		this(ownerBundle, regBundle, serviceInterfaceName, serviceName);
		this.bundleTracker = bt;
	}

	public Extension<S> getExtension() {

		// if (null == extension) {
		return new ExtensionImpl<S>(getServiceInterfaceName(), getRegistratorBundle());
		// }
		// return extension;
	}

	/**
	 * Return the extender object of the extension specified by the interface of the extension. The
	 * extender contains sufficient meta information about the extension to provide detailed
	 * information about it, to use it directly as a service or to invoke the extension object by
	 * introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension
	 * bundle is activated.
	 * <p>
	 * 
	 * @param serviceInterfaceClass one of possible multiple interfaces of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static <S> Extender<S> getExtender(Class<S> serviceInterface) throws ExtenderException {

		return getExtender(serviceInterface.getName());
	}

	/**
	 * Return the extender object specified by the extension interface name. The extender contains
	 * sufficient meta information about the extension to provide detailed information about it, to
	 * use it directly as a service or to invoke the extension object by introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension
	 * bundle is activated.
	 * <p>
	 * 
	 * @param serviceInterfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static <S> Extender<S> getExtender(String serviceInterfaceName) throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		if (null == sr) {
			return null;
		}
		ExtenderImpl<S> extender = getExtender(sr);
		@SuppressWarnings("unchecked")
		ExtenderImpl<S> trackedExtender = (ExtenderImpl<S>) getTrackedExtender(extender,
				serviceInterfaceName);
		if (null != trackedExtender) {
			return (ExtenderImpl<S>) trackedExtender;
		}
		return extender;
	}

	public static <S> ExtenderImpl<S> getExtender(String serviceInterfaceName, Bundle bundle)
			throws ExtenderException {

		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		// TODO Get all services and return the first matching the user
		ExtenderImpl<S> extender = getExtender(sr);
		@SuppressWarnings("unchecked")
		ExtenderImpl<S> trackedExtender = (ExtenderImpl<S>) getTrackedExtender(extender,
				serviceInterfaceName);
		if (null != trackedExtender) {
			return trackedExtender;
		}
		return extender;
	}

	protected void openServiceTracker(Bundle userBundle) throws ExtenderException {

		if (null == tracker) {
			try {
				if (null == userBundle) {
					userBundle = getRegistratorBundle();
				}
				tracker = new ServiceTracker<S, S>(userBundle.getBundleContext(), getServicereReference(),
						null);
				tracker.open();
			} catch (IllegalStateException e) {
				tracker = null;
				throw new ExtenderException("failed_to_open_tracker", getServiceInterfaceName());
			}
		}
	}

	public S getService(Bundle bundle) throws ExtenderException {

		try {
			return bundle.getBundleContext().getService(getServicereReference());
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e, e.getMessage());
		}
		// try {
		// if (null == tracker) {
		// openServiceTracker(bundle);
		// }
		// return tracker.getService();
		// } catch (ExtenderException e) {
		// // Ignore
		// // throw new ExtenderException("failed_to_open_tracker", getServiceInterfaceName());
		// }
		// return null;
	}

	public S getTrackedService(Bundle bundle) throws ExtenderException {

		try {
			if (null == tracker) {
				openServiceTracker(bundle);
			}
			return tracker.getService();
		} catch (ExtenderException e) {
			// Ignore
			// throw new ExtenderException("failed_to_open_tracker", getServiceInterfaceName());
		}
		return null;
	}

	/**
	 * Get the service object for the current extension
	 * 
	 * @return the service object or null if no service is being tracked. This usually means that the
	 * bundle context of the bundle providing the service is no longer valid.
	 */
	public S getService() throws ExtenderException {
		// try {
		// return (S)
		// getRegistratorBundle().getBundleContext().getService(serviceRegistration.getReference());
		// } catch (IllegalStateException e) {
		// throw new ExtenderException("failed_to_open_tracker", getServiceInterfaceName());
		// }
		try {
			if (null == tracker) {
				openServiceTracker(getRegistratorBundle());
			}
			return tracker.getService();
		} catch (ExtenderException e) {
			// Ignore
		}
		return null;
	}

	public static void close() {

		// for (Entry<String, ExtenderImpl<?>> extender : extenderMap.entrySet()) {
		// ExtenderImpl<?> eVal = extender.getValue();
		// eVal.closeServiceTracker();
		// // unregistered by the framework
		// // extenderVal.unregisterService();
		// }
	}

	private void closeServiceTracker() {
		// synchronized (tracker) {
		if (null != tracker && tracker.getTrackingCount() != -1) {
			// System.out.println("Closing tracker for: " + getExtensionInterfaceName());
			tracker.close();
			tracker = null;
		}
		// }
	}

	/**
	 * Get the interface class of this extension
	 * 
	 * @return the interface class
	 * @throws ExtenderException if the class object implementing this interface could not be created,
	 * the class is not implementing the registered interface or if the bundle context of the
	 * extension is no longer valid
	 */
	public Class<S> getInterfaceServiceClass() throws ExtenderException {
		if (null == serviceInterfaceClass) {
			Class<S> serviceClass = (Class<S>) getServiceClass();
			if (null != serviceClass) {
				if (serviceClass.getName().equals(getServiceInterfaceName())) {
					// The interface service is a class
					return serviceInterfaceClass = serviceClass;
				} else {
					// The interface service is an interface
					serviceInterfaceClass = Introspector.getTypeInterface(serviceClass,
							getServiceInterfaceName());
				}
			}
		}
		return serviceInterfaceClass;
	}

	/**
	 * Get the name of the extension interface.
	 * <p>
	 * The interface name is returned even if it is no longer tracked
	 * 
	 * @return the interface name
	 */
	public String getServiceInterfaceName() {
		return serviceInterfaceName;
	}

	public Class<S> getServiceClass() throws ExtenderException {

		if (null == serviceClass) {
			// must use class loader of bundle owing the class
			Bundle bundle = getOwnerBundle();
			if (null == bundle) {
				throw new ExtenderException("get_bundle_exception", ownerBId);
			}
			if (null == serviceName) {
				throw new ExtenderException("missing_class_name");
			}
			serviceClass = Introspector.loadClass(bundle, serviceName);
		}
		return serviceClass;
	}

	/**
	 * Get the name of the class implementing the extension interface.
	 * <p>
	 * The class name is returned even if the bundle is no longer tracked
	 * 
	 * @return the interface name
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Create an object of the class implementing the extension interface under consideration
	 * <p>
	 * The class from which the object is created is loaded from the extension bundle
	 * 
	 * @return an object of the class implementing this extension interface.
	 * @throws ExtenderException If the class object or the object could not be created
	 * @see #getServiceClass()
	 */
	public Object getServiceObject() throws ExtenderException {

		return Introspector.createObject(getServiceClass());
	}

	/**
	 * Return the bundle of this extension
	 * <p>
	 * The bundle is returned even if it is no longer tracked
	 * 
	 * @return the bundle object or null if the bundle context of the extension is no longer valid
	 */
	public Bundle getOwnerBundle() {

		return Activator.getContext().getBundle(ownerBId);
	}

	// private Bundle getExtenderBundle() {
	//
	// return Activator.getContext().getBundle();
	// }

	public Bundle getRegistratorBundle() {

		return Activator.getContext().getBundle(regBId);
	}

	public boolean isServiceregistered() {

		if (null != getServicereReference()) {
			return true;
		}
		return false;
	}

	/**
	 * @return A ServiceReference object, or null if no extenderMap are registered which implement the
	 * specified class
	 */
	@SuppressWarnings("unchecked")
	public ServiceReference<S> getServicereReference() throws IllegalStateException {
		Bundle bundle = getRegistratorBundle();
		return (ServiceReference<S>) bundle.getBundleContext()
				.getServiceReference(serviceInterfaceName);
	}

	/**
	 * Register or get an already registered extension service.
	 * <p>
	 * If the service is registered that service registration is used. The service may have been
	 * registered earlier by this bundle or by an other (typically the extension) bundle
	 * <p>
	 * 
	 * @throws ExtenderException if the bundle context of the extension is no longer valid, the
	 * service was not registered by this framework, the implementation class not owned by the bundle
	 * registering the service, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal or if the service object could not be obtained
	 */
	@SuppressWarnings("unchecked")
	public void registerService() throws ExtenderException {
		try {
			synchronized (this) {
				if (null == serviceRegistration) {
					if (null != serviceFactory) {
						serviceRegistration = getRegistratorBundle().getBundleContext().registerService(
								serviceInterfaceName, serviceFactory, null);
						S serviceobject = getService(getRegistratorBundle());
						if (null == serviceobject) {
							throw new ExtenderException("failed_to_get_service", serviceInterfaceName,
									serviceFactory.getClass());
						}
						serviceClass = (Class<S>) serviceobject.getClass();
						serviceName = serviceClass.getName();
					}
					Class<S> serviceClass = getServiceClass();
					if (null == serviceInterfaceName) {
						throw new ExtenderException("missing_interface_name", serviceClass);
					}
					if (!serviceName.equals(serviceInterfaceName)) {
						if (null == Introspector.getInterface(serviceClass, serviceInterfaceName)) {
							throw new ExtenderException("missing_interface", serviceInterfaceName, serviceClass);
						}
					}
					if (null == serviceRegistration) {
						final Dictionary<String, Object> properties = new Hashtable<String, Object>();
						properties.put(Constants.SERVICE_DESCRIPTION, "ExtenderImpl service object");
						properties.put(ExtenderImpl.class.getSimpleName(), Boolean.valueOf(true));
						serviceRegistration = getRegistratorBundle().getBundleContext().registerService(
								serviceInterfaceName, getServiceObject(), properties);
					}
					// Validate the service
					ServiceReference<?> serviceReference = getServicereReference();
					if (null == serviceReference) {
						throw new ExtenderException("failed_to_get_service_for_interface", serviceInterfaceName);
					}
					if (!serviceReference.isAssignableTo(getRegistratorBundle(), getServiceClass().getName())) {
						throw new ExtenderException("illegal_source_package_reference", getRegistratorBundle(),
								getServiceClass().getName());
					}
					// printServiceregInfo(serviceReference);
				}
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e, e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private void printServiceregInfo(ServiceReference<?> ref) {

		System.out.println("------------   Service Registation info:  --------------------\n");
		System.out.println("Owner bundle           			 : " + getOwnerBundle().getSymbolicName());
		System.out.println("Bundle registered service    : " + ref.getBundle().getSymbolicName());
		System.out.println("Registration bundle    			 : " + getRegistratorBundle().getSymbolicName());
		Bundle[] using = ref.getUsingBundles();
		if (null != using) {
			System.out.println("Using bundles                : "
					+ formatBundleList(new ArrayList<Bundle>(Arrays.asList(using)), false));
		}
		System.out.println("Serviceref Impl. Class       : "
				+ getOwnerBundle().getBundleContext().getService(ref).getClass().getName());
		System.out.println("ExtenderImpl Impl. Class         : " + getServiceClass().getName());
		System.out
				.println("ExtenderImpl Interface Class     : " + getInterfaceServiceClass().getName());
	}

	public BundleTracker<Extender<?>> getBundleTracker() {
		return bundleTracker;
	}

	/**
	 * Register the extender map and the extender itself as services
	 * 
	 * @param ownerBundle the bundle owing the services
	 * @param regBundle the bundle registering the services
	 * @return the registered extender
	 */
	public static <S> ExtenderImpl<S> init(Bundle ownerBundle, Bundle regBundle) {

		// Register the map of extenders as a service
		registerExtenderMapService();

		// The extender itself is a service
		ExtenderImpl<S> extender = new ExtenderImpl<>(ownerBundle, regBundle, Extender.class.getName(),
				new ExtenderServiceFactory<>());
		ExtenderImpl<S> extenderImpl = ExtenderImpl.register(extender);

		// For now, don't make the extension a service
		// ExtenderImpl<S> extenderForExtension = new ExtenderImpl<S>(ownerBundle, regBundle,
		// Extension.class.getName(), ExtensionImpl.class.getName());
		// ExtenderImpl.register(extenderForExtension);
		return extenderImpl;
	}

	/**
	 * Registers the extender map as a service. The service holds all registered extender services
	 * keyed by their service id.
	 * <p>
	 * If the map service is already registered, return the already registered service map
	 * 
	 * @return The extender map service
	 * @throws ExtenderException if the bundle context is no longer valid, the service was not
	 * registered by this framework, the implementation class is not owned by the bundle registering
	 * the service, the registered implementation class does not implement the registered interface,
	 * the interface name is illegal, the service object could not be obtained or a missing security
	 * permission
	 */
	private static <S> ExtenderMapService<Long, ExtenderImpl<S>> registerExtenderMapService()
			throws ExtenderException {

		try {
			ExtenderMapService<Long, ExtenderImpl<S>> extenderMapService = getExtenderMapService();
			if (null == extenderMapService) {
				// Register the extender map as a service
				BundleContext bc = Activator.getContext();
				ServiceFactory<ExtenderMapService<Long, ExtenderImpl<S>>> factory = new ExtenderMapServiceFactory<>();
				Bundle bundle = bc.getBundle();
				ExtenderImpl<S> extender = new ExtenderImpl<>(bundle, bundle,
						ExtenderMapService.class.getName(), factory);
				extender.registerService();
				// TODO Check for null and throw exception
				extenderMapService = getExtenderMapService();
				// Record the registered extender map service with its own service
				ServiceReference<S> sr = extender.getServicereReference();
				Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
				extenderMapService.put(sid, extender);
				Activator.getDefault().getExtenderListener()
						.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, sr));
			}
			return extenderMapService;
		} catch (IllegalStateException | SecurityException | IllegalArgumentException e) {
			// TODO: put additional info on this exception
			throw new ExtenderException(e);
		}
	}

	/**
	 * Get the service for the extender map containing a map of all registered extender services keyed
	 * by their service id.
	 * 
	 * @return The extender map service or null if the service could not be obtained
	 * @throws ExtenderException if this BundleContext is no longer valid or a missing security
	 * permission
	 */
	@SuppressWarnings("unchecked")
	private static <S> ExtenderMapService<Long, ExtenderImpl<S>> getExtenderMapService()
			throws ExtenderException {
		try {
			BundleContext bc = Activator.getContext();
			ServiceReference<?> sr = bc.getServiceReference(ExtenderMapService.class.getName());
			if (null == sr) {
				return null;
			}
			return (ExtenderMapService<Long, ExtenderImpl<S>>) bc.getService(sr);
		} catch (IllegalStateException | SecurityException | IllegalArgumentException e) {
			// TODO: put additional info on this exception
			throw new ExtenderException(e);
		}
	}

	public static <S> ExtenderImpl<S> getExtender(ServiceReference<?> sr) {
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		ExtenderMapService<Long, ExtenderImpl<S>> extenders = getExtenderMapService();
		return extenders.get(sid);
	}

	public static <S> ExtenderImpl<S> removeExtender(ServiceReference<?> sr) {
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		ExtenderMapService<Long, ExtenderImpl<S>> extenders = getExtenderMapService();
		if (null != extenders) {
			return extenders.remove(sid);
		}
		return null;
	}

	/**
	 * Check if this interface is registered or attached to a service by the extender
	 * <P>
	 * If trying to register an already registered interface the extender attaches itself to it and
	 * returns silently
	 * 
	 * @param serviceInterfaceName interface name of a registered service
	 * @return true if the specified interface is registered by the extender and false if not
	 */
	public static <S> boolean isExtended(String serviceInterfaceName) {
		BundleContext context = Activator.getContext();
		ServiceReference<?> sr = context.getServiceReference(serviceInterfaceName);
		Long sid = (Long) sr.getProperty(Constants.SERVICE_ID);
		ExtenderMapService<Long, ExtenderImpl<S>> extenders = getExtenderMapService();
		ExtenderImpl<?> extender = extenders.get(sid);
		if (null != extender) {
			return true;
		}
		return false;
	}

	private static <S> Extender<?> getTrackedExtender(ExtenderImpl<S> extender,
			String serviceInterfaceName) {

		BundleTracker<Extender<?>> bt = extender.getBundleTracker();
		if (null != bt) {
			Map<Bundle, Extender<?>> trackedObjects = bt.getTracked();
			Iterator<Entry<Bundle, Extender<?>>> it = trackedObjects.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Extender<?>> entry = it.next();
				Extender<?> trackedExtender = entry.getValue();
				String id = trackedExtender.getServiceInterfaceName();
				if (null != id && id.equals(serviceInterfaceName)) {
					return trackedExtender;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private Collection<Bundle> getBundles(Collection<Long> bundleIds) {
		Collection<Bundle> bundles = new LinkedList<>();
		if (null != bundleIds && bundleIds.size() >= 1) {
			for (Long bid : bundleIds) {
				Bundle bundle = Activator.getContext().getBundle(bid);
				if (null != bundle) {
					bundles.add(bundle);
				}
			}
		}
		return bundles;
	}

	/**
	 * If there are any extenders in the extender map, print a warning to system out
	 */
	@SuppressWarnings("unchecked")
	public static <S> void validateUnregister() {

		BundleContext bc = Activator.getContext();
		ServiceReference<?> sr = bc.getServiceReference(ExtenderMapService.class.getName());
		ExtenderMapService<Long, ExtenderImpl<S>> extenderServiceMap = (ExtenderMapService<Long, ExtenderImpl<S>>) bc
				.getService(sr);
		if (extenderServiceMap.size() > 0) {
			Iterator<Entry<Long, ExtenderImpl<S>>> it = extenderServiceMap.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Long, ExtenderImpl<S>> entry = it.next();
				ExtenderImpl<S> ext = entry.getValue();
				ServiceReference<?> serviceRef = bc.getServiceReference(ext.getServiceInterfaceName());
				Long sid = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
				System.out.println();
				System.err.println("This one has not been unregistered SID: " + sid + " "
						+ ext.getServiceInterfaceName() + ". Revise.");
			}
			extenderServiceMap.clear();
		}
	}

	private String formatBundleList(Collection<Bundle> bundles, boolean includeVersion) {
		StringBuffer sb = new StringBuffer();
		if (null != bundles && bundles.size() >= 1) {
			for (Iterator<Bundle> iterator = bundles.iterator(); iterator.hasNext();) {
				Bundle bundle = iterator.next();
				sb.append(bundle.getSymbolicName());
				if (includeVersion) {
					sb.append('_');
					sb.append(bundle.getVersion().toString());
				}
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}
}
