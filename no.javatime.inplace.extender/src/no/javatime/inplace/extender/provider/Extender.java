package no.javatime.inplace.extender.provider;

import java.util.HashMap;
import java.util.Map;

import no.javatime.inplace.extender.Activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Manage extensions registered by bundle trackers. The extender provides functionality for examination and direct use
 * of the extension as a service or through introspection.
 * <p>
 * When registered, the extension is automatically registered as a service or if a service is available that service is used. 
 */
public class Extender<T> {

	private final String interfaceName; // extension interface definition name from bundle tracker
	private final Long bId; // bundle id of the extension provider
	private final String clsName; // obtained from extension provider by the bundle tracker 
	private Class<?> cls; // implementation class created by extender or obtained from service registry
	private ServiceRegistration<?> serviceRegOwner = null; // the service registration object if the extender crates the service
	private ServiceReference<?> serviceRef; // service reference obtained from the service registration
	private ServiceTracker<T, T> tracker; // service tracker for the extension
	private Class<T> intFace;
	
	/**
	 * Extension of the current extender instance.
	 */
	private Extension<T> extension; 

	/**
	 * List of all trackers registering extensions
	 */
	private static Map<String, BundleTracker<Extender<?>>> trackers = new HashMap<>();

	/**
	 * Creates an extender object with sufficient information about the extension to provide a service for it.
	 * <p>
	 * The extender manages the life cycle and allocation/deallocation of resources used by the extension.
	 * <p>
	 * This class is typically instantiated from the extender bundle tracker when extension bundles are
	 * activated. To maintain an extension through direct instantiation, this extender class should be sub classed.
	 * 
	 * @param bundleId the bundle id of the bundle providing an extension implementation
	 * @param class1 the interface name of the extension
	 * @param className the name of the class implementing the extension interface
	 * @see ExtenderBundleTracker
	 */
	public Extender(BundleTracker<Extender<?>> bt, Long bundleId, Class<T> intFace, String className) {
		// Should be able to resolve interface name class from interface name
		this(bt, bundleId, intFace.getName(), className);
	}

	public Extender(BundleTracker<Extender<?>> bt, Long bundleId, String interfaceName, String className) {
		this.bId = bundleId;
		this.clsName = className;
		this.interfaceName = interfaceName;
		if (!trackers.containsKey(interfaceName)) {
			trackers.put(interfaceName, bt);
		}
	}

	public Extension<T> getExtension() {
		if (null == extension) {
			extension =  new Extension<T>(getExtensionInterface());
		}
		return extension;
	}
	
	/**
	 * Return the extender object specified by the extension interface name. The extender contains
	 * sufficient meta information about the extension to provide detailed information about it, to use it directly as 
	 * a service or to invoke the extension object by introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension bundle is
	 * activated.
	 * <p>
	 * @param interfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no registered extensions
	 * or the registered class does not implement the registered interface
	 * @throws InPlaceException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static <E> Extender<E> getInstance(String interfaceName) throws InPlaceException {
		BundleTracker<Extender<?>> bt = trackers.get(interfaceName);
		if (null != bt) {
			Map<Bundle, Extender<?>> trackedMetaObjects = bt.getTracked();
			for (Map.Entry<Bundle, Extender<?>> entry : trackedMetaObjects.entrySet()) {
				Extender<?> em = entry.getValue();
				String id = em.getExtensionInterfaceName();
				if (null != id && id.equals(interfaceName)) {
					return register(em);
				}
			}
		}
		return null;
	}

	/**
	 * Return the extender object of the extension specified by the interface of the extension. The extender contains
	 * sufficient meta information about the extension to provide detailed information about it, to use it directly as 
	 * a service or to invoke the extension object by introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension bundle is
	 * activated.
	 * <p>
	 * @param intFace one of possible multiple interfaces of the extension bundle.
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no registered extensions
	 * or the registered class does not implement the registered interface
	 * @throws InPlaceException if the bundle context of the extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public static <E, T> Extender<E> getInstance(Class<T> intFace) {

		return getInstance(intFace.getName());
	}
	
	@SuppressWarnings("unchecked")
	private static <E> Extender<E> register(Extender<?> extender) throws InPlaceException {
		// Ensure that this is a valid interface and that the implementation class actually implements
		// the interface
		if (null != extender.getExtensionInterface() ) {
			// Delayed service registration
			extender.registerAsService();
			// The cast should be safe as long as the class implements the interface
			// See declaration of the bundle tracker customizer in the activator
			return (Extender<E>) extender;
		}		
		return null;
	}

	protected void openTracker() throws InPlaceException{		
		if (null == tracker) {
			try {
				// TODO Is it ok that the context from this bundle is used?
			tracker = new ServiceTracker<T, T>(Activator.getContext(), interfaceName, null);			
			tracker.open();
			} catch (IllegalStateException e) {
				tracker = null;
				throw new InPlaceException("failed_to_open_tracker", getExtensionInterfaceName());				
			}
		}
	}
	
	/**
	 * Get the service object for the current extension
	 * @return the service object or null if no service is being tracked. This usually means
	 * that the bundle context of the bundle providing the service is no longer valid.
	 */
	public T getService() {
		try {				
			if (null == tracker) {
				openTracker();
			}
			return tracker.getService();
		} catch (InPlaceException e) {
			// Ignore
		}
		return null;
	}	

	public void closeTracker() {
		if (null != tracker) {
			tracker.close();
			tracker = null;
		}
	}

	/**
	 * Get the interface class of this extension
	 * @return the interface class
	 * @throws InPlaceException if the class object implementing this interface 
	 *  could not be created, the class is not implementing the registered interface 
	 *  or if the bundle context of the extension is no longer valid	 
	 */
	public Class<T> getExtensionInterface() throws InPlaceException {
		if (null == intFace){
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
	 * @throws InPlaceException if the bundle context of the extension is no longer valid
	 * or the class name is invalid
	 */
	public Class<?> getExtensionClass() throws InPlaceException {
		if (null == cls) {
			Bundle bundle = getExtensionBundle();
			if (null == bundle) {
				// Not tracked
				throw new InPlaceException("get_bundle_exception", bId);
			}
			if (null == clsName) {
				throw new InPlaceException("missing_class_name");								
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
	 * @return a object of the class implementing this extension interface. The
	 * object is created if necessary.
	 * @throws InPlaceException If the class object or the object could not be created
	 * @see #getExtensionBundle()
	 * @see #getExtensionClass()
	 */
	protected Object createExtensionObjec() throws InPlaceException {

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

		return Activator.getContext().getBundle(bId);
	}

	public Bundle getExtenderBundle() {

		return Activator.getContext().getBundle();
	}

	/**
	 * Get the service registration for this extension
	 * <p>
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

	public  boolean isServiceOwner() {
		if (null != getServiceRegistration()) {
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @return A ServiceReference object, or null if no services are registered which implement the specified class
	 */
	public  ServiceReference<?> getServicereReference() {
		Bundle bundle = getExtenderBundle();
		return bundle.getBundleContext().getServiceReference(getExtensionInterface());
	}
	
	/**
	 * Register or get an already registered extension service.
	 * <p>
	 * If the service is registered that service registration is used. 
	 * The service may have been registered earlier by this bundle or 
	 * by an other (typically the extension) bundle
	 * <p>
	 * @throws InPlaceException if the bundle context of the extension is no longer valid,
	 * the service was not registered by this framework, the registered implementation 
	 * class does not implement the registered interface, the interface name is illegal or 
	 * if the service object could not be obtained
	 */
	public void registerAsService() throws InPlaceException {
		try {
			serviceRef = getServicereReference();
			if (null == serviceRef) {
				Class<?> cls = getExtensionClass();
				if (null == interfaceName) {
					throw new InPlaceException("missing_interface_name", cls);				
				}
				if (null == Introspector.getInterface(cls, interfaceName)) {
					throw new InPlaceException("missing_interface", interfaceName, cls);
				}
				serviceRegOwner = getExtenderBundle().getBundleContext().
						registerService(interfaceName, createExtensionObjec(), null);
				serviceRef = getServicereReference();
			}
			Object extensionImplObject = getExtenderBundle().getBundleContext().getService(serviceRef);
			printServiceregInfo();
			// We already have the class and interface instance and their names
			if (null == extensionImplObject) {
				throw new InPlaceException("failed_to_get_service", interfaceName, cls);				
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new InPlaceException(e, e.getMessage());
		}
	}

	private void printServiceregInfo() {
		System.out.println("------------   Service Registation info:  --------------------\n"); 
		System.out.println("Requested by: " + trackers.get(getExtensionInterfaceName()).getClass().getPackage().getName()); 
		
		if (isServiceOwner()) {
			System.out.println(
		"Registered by:         " + getExtenderBundle());
		} else {
		System.out.println(
		"Registered by:         " + getExtensionBundle());
		}
		System.out.println( 
		  "Owner bundle:        " + getExtensionBundle() + "\n"
		+ "Interface Class      " + getExtensionInterface() + "\n"
		+ "Impl. Class          " + getExtenderBundle().getBundleContext().getService(serviceRef).getClass().getName() +  "\n"
		+ "Extender Impl class: " + getExtensionClassName()
		); 
		
	}
	/**
	 * Unregister the registered service for the extension
	 */
	public void unregisterService() {
		if (null != serviceRegOwner) {
			serviceRegOwner.unregister();
			serviceRegOwner = null;
		}
	}	
}
