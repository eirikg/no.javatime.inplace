package no.javatime.inplace.extender.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import no.javatime.inplace.extender.Activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Manage extensions. The extender provides functionality for
 * examination and direct use of the extension as a service or through introspection.
 * <p>
 * When registered, the extension is automatically registered as a service or if a service 
 * for the extension is available that service is used.
 * <p>
 * The extender may also be used directly to access a registered service
 */
public class Extender<T> {

	/* Extension interface definition name */
	final private String interfaceName;
	/* Interface class of the extension */
	private Class<T> intFace;
	/* Bundle id of the bundle providing the extension */
	final private Long ownerBId;
	/* Bundle id of the bundle registering the extension */
	private Long regBId;
	/* Name of the implementation class */
	final private String clsName;
	/* Implementation class loaded by extender or obtained from service registry */
	private Class<?> cls;
	/* Service registration object when the service is created by the extender */
	private ServiceRegistration<?> serviceRegOwner;
	/* Service reference obtained from the service registration object */
	private ServiceReference<?> serviceRef;
	/* Service tracker for use by the extension */
	private ServiceTracker<T, T> tracker;
	/* The bundle tracker holding extender objects registered by a bundle tracker customizer */
	final private BundleTracker<Extender<?>> bundleTracker;

	/**
	 * An extension belonging to the current extender instance.
	 */
	private Extension<T> extension;

	/**
	 * List of all extenders keyed by interface name
	 */
	private static ConcurrentMap<String, Extender<?>> extenders = new ConcurrentHashMap<>();

	/**
	 * List of all bundles registering a service
	 */
	private ConcurrentSkipListSet<Long> requesters = new ConcurrentSkipListSet<>();

	public static <T> Extender<T> register(Bundle ownerBundle, Class<T> intFace, String className) {
		return register(ownerBundle, ownerBundle, intFace.getName(), className);
	}

	public static <T> Extender<T> register(Bundle ownerBundle, String interfaceName, String className) {
		return register(ownerBundle, ownerBundle, interfaceName, className);
	}

	public static <T> Extender<T> register(Bundle ownerBundle, Bundle regBundle, Class<T> intFace, String className) {
		return register(ownerBundle, regBundle, intFace.getName(), className);
	}

	public static <T> Extender<T> register(Bundle ownerBundle, Bundle regBundle, String interfaceName, String className) {
		Extender<?> ex = new Extender<>(ownerBundle, regBundle, interfaceName, className);
		extenders.put(interfaceName, ex);
		return Extender.register(ex);
	}

	// Register with bundle tracker
	public static <T> Extender<T> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle,
			Class<T> intFace, String className) {
		return register(bt, ownerBundle, ownerBundle, intFace.getName(), className);
	}
	
	public static <T> Extender<T> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle,
			String interfaceName, String className) {
		return register(bt, ownerBundle, ownerBundle, interfaceName, className);
	}

	public static <T> Extender<T> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle,
			Bundle regBundle, Class<T> intFace, String className) {
		return register(bt, ownerBundle, regBundle, intFace.getName(), className);
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
	 * <li>Extender.register(getTracker(), bundle.getBundleId(), serviceInterfaceName,
	 * serviceImplClassName);
	 * </ol>
	 * <p>
	 * After the extender service is registered, the service object can be obtained in one of the
	 * following ways:
	 * <p>
	 * <ol>
	 * <li>Directly from the returned registered extender object by calling {@link #getExtension()}
	 * <li>Create an extension object {@link no.javatime.inplace.extender.provider.Extension
	 * Extension} and call the {@link Extension#getService()} member.
	 * <li>Call the static {@linkplain Extender#getInstance(String)} method to obtain an extender
	 * object and than call {@link Extender#getExtension()} on the returned extender object
	 * </ol>
	 * <p>
	 * Example usage:
	 * <ol>
	 * <li>Extension<ServiceInterface> ex = new Extension<>(serviceInterfaceName);
	 * <li>ServiceInterface so = ex.getService();
	 * <li>so.getName(); // A method defined in the service object
	 * </ol>
	 * <P>
	 * @param ownerBundle the bundle id of the bundle providing an extension implementation
	 * @param regBundle TODO
	 * @param interfaceName the interface name of the extension
	 * @param className the name of the class implementing the extension interface
	 * 
	 * @see ExtenderBundleTracker
	 */
	public static <T> Extender<T> register(BundleTracker<Extender<?>> bt, Bundle ownerBundle,
			Bundle regBundle, String interfaceName, String className) {
		Extender<?> ex = new Extender<>(bt, ownerBundle, regBundle, interfaceName, className);
		extenders.put(interfaceName, ex);
		return Extender.register(ex);
	}

	// Constructors

	private Extender(Bundle ownerBundle, Bundle regBundle, String interfaceName, String className) {
		this.ownerBId = ownerBundle.getBundleId();
		this.regBId = regBundle.getBundleId();	
		this.interfaceName = interfaceName;
		this.clsName = className;
		this.bundleTracker = null;
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
	 * @param ownerBundle the bundle id of the bundle providing an extension implementation
	 * @param regBundle TODO
	 * @param interfaceName the interface name of the extension
	 * @param className the name of the class implementing the extension interface
	 * 
	 * @see ExtenderBundleTracker
	 */
	private Extender(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String interfaceName, String className) {
		this.bundleTracker = bt;
		this.ownerBId = ownerBundle.getBundleId();
		this.regBId = regBundle.getBundleId();		
		this.interfaceName = interfaceName;
		this.clsName = className;
	}

	public Extension<T> getExtension() {
		if (null == extension) {
			extension = new Extension<T>(getExtensionInterfaceName());
		}
		return extension;
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
	 * @param interfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the class
	 * object implementing the extension could not be created
	 */
	public static <E> Extender<E> getInstance(String interfaceName) throws ExtenderException {

		Extender<?> extender = extenders.get(interfaceName);
		if (null != extender) {
			Extender<?> trackedExtender = getTrackedExtender(extender, interfaceName);
			if (null != trackedExtender) {
				return register(trackedExtender);
			}
			return register(extender);
		}
		return null;
	}

	private static Extender<?> getTrackedExtender(Extender<?> extender, String interfaceName) {

		BundleTracker<Extender<?>> bt = extender.getBundleTracker();
		if (null != bt) {
			Map<Bundle, Extender<?>> trackedObjects = bt.getTracked();
			Iterator<Entry<Bundle, Extender<?>>> it = trackedObjects.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Extender<?>> entry = it.next();
				Extender<?> trackedExtender = entry.getValue();
				String id = trackedExtender.getExtensionInterfaceName();
				if (null != id && id.equals(interfaceName)) {
					return trackedExtender;
				}
			}
		}
		return null;
	}

//	public static <T> Long getBundle(Class<T> intFace) {
//		Bundle[] bundles = Activator.getContext().getBundles();
//		for (int i = 0; i < bundles.length; i++) {
//			Bundle bundle = bundles[i];
//			//bundle.getBundleContext().getAllServiceReferences(intFace.getName(), null);
//		}
//		return null;
//	}


	/**
	 * Check if this interface is registered or attached to a service by the extender 
	 * <P>
	 * If trying to register an already registered interface the extender attaches
	 * itself to it and returns silently
	 * 
	 * @param interfaceName interface name of a registered service
	 * @return true if the specified interface is registered by the extender and false if not
	 * @see Extender#register(Long, Long, String, String)
	 */
	public static boolean isExtended(String interfaceName) {
		if (null != extenders.get(interfaceName)) {
			return true;
		}
		return false;
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
	 * @param intFace one of possible multiple interfaces of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the class
	 * object implementing the extension could not be created
	 */
	public static <E, T> Extender<E> getInstance(Class<T> intFace) {

		return getInstance(intFace.getName());
	}

	@SuppressWarnings("unchecked")
	private static <E> Extender<E> register(Extender<?> extender) throws ExtenderException {
		// Ensure that this is a valid interface and that the implementation class
		// actually implements the interface
		if (null != extender.getExtensionInterface()) {
			// Delayed service registration
			extender.registerAsService();
			// The cast should be safe as long as the class implements the interface
			// See declaration of the bundle tracker customizer in the activator
			return (Extender<E>) extender;
		}
		return null;
	}

	protected void openServiceTracker() throws ExtenderException {
		if (null == tracker) {
			try {
				// TODO Is it ok that the context from this bundle is used?
				tracker = new ServiceTracker<T, T>(Activator.getContext(), interfaceName, null);
				tracker.open();
			} catch (IllegalStateException e) {
				tracker = null;
				throw new ExtenderException("failed_to_open_tracker", getExtensionInterfaceName());
			}
		}
	}

	/**
	 * Get the service object for the current extension
	 * 
	 * @return the service object or null if no service is being tracked. This usually means that the
	 * bundle context of the bundle providing the service is no longer valid.
	 */
	public T getService() {
		try {
			if (null == tracker) {
				openServiceTracker();
			}
			return tracker.getService();
		} catch (ExtenderException e) {
			// Ignore
		}
		return null;
	}

	public static void close() {

		for (Entry<String, Extender<?>> extender : extenders.entrySet()) {
			Extender<?> eVal = extender.getValue();
			eVal.closeServiceTracker();
			// unregistered by the framework
			// extenderVal.unregisterService();
		}	
	}

	public void closeServiceTracker() {
//		synchronized (tracker) {
			if (null != tracker && tracker.getTrackingCount() != -1) {
				System.out.println("Closing tracker for: " + getExtensionInterfaceName());
				tracker.close();
				tracker = null;
			}
//		}
	}

	/**
	 * Unregister the registered service for the extension
	 */
	public void unregisterService() {
//		synchronized (serviceRegOwner) {
			try {
				if (null != serviceRegOwner) {
					serviceRegOwner.unregister();
					System.out.println("Unregistering service for: " + getExtensionInterfaceName());
					serviceRegOwner = null;
					serviceRef = null;
				}				
			} catch (IllegalStateException  e) {
				System.out.println("Exception Unregistering service for: " + getExtensionInterfaceName());
			}
//		}
	}


	/**
	 * Get the interface class of this extension
	 * 
	 * @return the interface class
	 * @throws ExtenderException if the class object implementing this interface could not be created,
	 * the class is not implementing the registered interface or if the bundle context of the
	 * extension is no longer valid
	 */
	public Class<T> getExtensionInterface() throws ExtenderException {
		if (null == intFace) {
			intFace = Introspector.getTypeInterface(getExtensionClass(), getExtensionInterfaceName());
		}
		return intFace;
	}

	/**
	 * Get the name of the extension interface.
	 * <p>
	 * The interface name is returned even if it is no longer tracked
	 * 
	 * @return the interface name
	 */
	public String getExtensionInterfaceName() {
		return interfaceName;
	}

	/**
	 * The implementation class of the extension
	 * <p>
	 * The class is returned even if the bundle is no longer tracked
	 * 
	 * @return the implementation class of this extension
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or the class
	 * name is invalid
	 */
	public Class<?> getExtensionClass() throws ExtenderException {
		if (null == cls) {
			Bundle bundle = getExtensionBundle();
			if (null == bundle) {
				// Not tracked
				throw new ExtenderException("get_bundle_exception", ownerBId);
			}
			if (null == clsName) {
				throw new ExtenderException("missing_class_name");
			}
			cls = Introspector.loadExtensionClass(bundle, getExtensionClassName());
		}
		return cls;
	}

	/**
	 * Get the name of the class implementing the extension interface.
	 * <p>
	 * The class name is returned even if the bundle is no longer tracked
	 * 
	 * @return the interface name
	 */
	public String getExtensionClassName() {
		return clsName;
	}

	/**
	 * Create an object of the class implementing the extension interface under consideration
	 * <p>
	 * The class from which the object is created is loaded from the extension bundle
	 * 
	 * @return an object of the class implementing this extension interface.
	 * @throws ExtenderException If the class object or the object could not be created
	 * @see #getExtensionBundle()
	 * @see #getExtensionClass()
	 */
	public Object createExtensionObjec() throws ExtenderException {

		return Introspector.createExtensionObject(getExtensionClass());
	}

	/**
	 * Return the bundle of this extension
	 * <p>
	 * The bundle is returned even if it is no longer tracked
	 * 
	 * @return the bundle object or null if the bundle context of the extension is no longer valid
	 */
	public Bundle getExtensionBundle() {

		return Activator.getContext().getBundle(ownerBId);
	}

//	private Bundle getExtenderBundle() {
//
//		return Activator.getContext().getBundle();
//	}

	public Bundle getRegistratorBundle() {

		return Activator.getContext().getBundle(regBId);
	}

	/**
	 * Get the service registration for this extension
	 * <p>
	 * 
	 * @return a service registration object or null if the extension is not registered
	 */
	public ServiceRegistration<?> getServiceRegistration() {
		return serviceRegOwner;
	}

	public boolean isServiceregistered() {
		if (null != getServicereReference()) {
			return true;
		}
		return false;
	}

	public boolean isServiceOwner() {
		if (null != getServiceRegistration()) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return A ServiceReference object, or null if no services are registered which implement the
	 * specified class
	 */
	public ServiceReference<?> getServicereReference() {
		Bundle bundle = getExtensionBundle();
		// Bundle bundle = getExtenderBundle();
		return bundle.getBundleContext().getServiceReference(getExtensionInterface());
	}

	/**
	 * Register or get an already registered extension service.
	 * <p>
	 * If the service is registered that service registration is used. The service may have been
	 * registered earlier by this bundle or by an other (typically the extension) bundle
	 * <p>
	 * 
	 * @throws ExtenderException if the bundle context of the extension is no longer valid, the service
	 * was not registered by this framework, the implementation class not owned by the bundle registering the service,
	 * the registered implementation class does not implement
	 * the registered interface, the interface name is illegal or if the service object could not be
	 * obtained
	 */
	public void registerAsService() throws ExtenderException {
		try {
			synchronized (this) {
				if (requesters.add(regBId)) {
					serviceRef = getServicereReference();
					Class<?> cls = getExtensionClass();
					if (null == interfaceName) {
						throw new ExtenderException("missing_interface_name", cls);
					}
					if (null == Introspector.getInterface(cls, interfaceName)) {
						throw new ExtenderException("missing_interface", interfaceName, cls);
					}
					if (null == serviceRef) {
						serviceRegOwner = getRegistratorBundle().getBundleContext().registerService(interfaceName,
								createExtensionObjec(), null);
						serviceRef = getServicereReference();
						if (null == serviceRef) {
							throw new ExtenderException("failed_to_get_service_for_interface", interfaceName);
						}
					} else {
						regBId = serviceRef.getBundle().getBundleId();
						requesters.add(regBId);
					}
					Object extensionImplObject = getExtensionBundle().getBundleContext().getService(serviceRef);
					printServiceregInfo(serviceRef);
					if (null == extensionImplObject) {
						throw new ExtenderException("failed_to_get_service", interfaceName, cls);
					}
					if (!serviceRef.isAssignableTo(getExtensionBundle(), getExtensionClass().getName())) {
						throw new ExtenderException("illegal_source_package_reference", getExtensionBundle(), getExtensionClass().getName());					
					}
				}
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e, e.getMessage());
		}
	}

	private void printServiceregInfo(ServiceReference<?> ref) {

		System.out.println("------------   Service Registation info:  --------------------\n");
		System.out.println("serviceref                   : " + serviceRef.getBundle().getSymbolicName());
		System.out.println("getextension bundle          : " + getExtensionBundle().getSymbolicName());
		System.out.println("getregistration bundle       : " + getRegistratorBundle().getSymbolicName());
		System.out.println("Requesters set               : " + formatBundleList(getBundles(requesters), false));
		System.out.println("Using bundles                : " + formatBundleList(new ArrayList<Bundle>(Arrays.asList(serviceRef.getUsingBundles())), false));
		System.out.println("Serviceref Impl. Class       : " + getExtensionBundle().getBundleContext().getService(serviceRef).getClass().getName());
		System.out.println("Extender Impl. Class         : " + getExtensionClass().getName());
		System.out.println("Extender Interface Class     : " + getExtensionInterface().getName());
	}

	public BundleTracker<Extender<?>> getBundleTracker() {
		return bundleTracker;
	}

	private Collection<Bundle> getBundles(Collection<Long> bundleIds) {
		Collection<Bundle> bundles = new LinkedList<>();
		StringBuffer sb = new StringBuffer();
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
