package no.javatime.inplace.ui.extender;

import java.util.Map;

import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.ui.Activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

/**
 * Manage extensions registered by bundle trackers. The extender provides functionality for examination and direct use
 * of the extension as a service or through introspection.
 * <p>
 * When registered, the extension is automatically registered as a service. The service can than be obtained from 
 * the returned extender instance.
 */
public class Extender {

	private final String interfaceName; // extension interface definition name
	private final Long bId; // bundle id of the extension
	private final String clsName; // implementation class name of the extension 
	private Class<?> cls; // implementation class derived from the implementation class name
	private Object obj; // implementation object derived from the implementation class
	private ServiceRegistration<?> sr = null; // the service registration object of the extension

	/**
	 * Creates an extender object with sufficient information about the extension to provide a service for it.
	 * <p>
	 * The extender manages the life cycle and allocation/deallocation of resources used by the extension.
	 * <p>
	 * This class is typically instantiated from the extender bundle tracker when extension bundles are
	 * activated. To maintain an extension through direct instantiation, this extender class should be sub classed
	 * and manage the life cycle of the extension.
	 * 
	 * @param bundleId the bundle id of the bundle providing an extension implementation
	 * @param interfaceName the interface name of the extension
	 * @param className the name of the class implementing the extension interface
	 * @see ExtenderBundleTracker
	 */
	public Extender(Long bundleId, String interfaceName, String className) {
		this.bId = bundleId;
		this.clsName = className;
		this.interfaceName = interfaceName;
		registerAsService(interfaceName);
	}

	/**
	 * Return the extender object of the extension specified by the bundle id of the extension. The extender contains
	 * sufficient meta information about the extension to provide detailed information about it, to use it directly as 
	 * a service or to invoke the extension object by introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension bundle is
	 * activated.
	 * <p>
	 * The bundle - and hence the bundle id - is supplied by the extender bundle tracker when a bundle of
	 * interest is activated and provides one or more registered extensions.
	 * 
	 * @param bundleId bundle id of the extension bundle.
	 * @return the extender instance or null if the bundle context of the extension is no longer valid, 
	 * the bundle of the extension is no longer tracked or the bundle has no registered extensions
	 */
	public static Extender getInstance(Long bundleId) {
		// TODO Catch
		BundleTracker<Extender> bt = Activator.getDefault().getExtenderBundleTracker();
		Bundle b = Activator.getContext().getBundle(bundleId);
		return bt.getObject(b);
	}

	/**
	 * Return the extender object of the extension specified by the interface name of the extension. The extender contains
	 * sufficient meta information about the extension to provide detailed information about it, to use it directly as 
	 * a service or to invoke the extension object by introspection
	 * <p>
	 * An extender instance is typically created by the extender bundle tracker when the extension bundle is
	 * activated.
	 * <p>
	 * @param interfaceName one of possible multiple interface names of the extension bundle.
	 * @return the extender instance or null if the bundle context of the extension is no longer valid, 
	 * the bundle of the extension is no longer tracked or the bundle has no registered extension
	 * for the the specified interface
	 */
	public static Extender getInstance(String interfaceName) {
		// TODO Catch
		BundleTracker<Extender> bt = Activator.getDefault().getExtenderBundleTracker();
		Map<Bundle, Extender> trackedMetaObjects = bt.getTracked();
		for (Map.Entry<Bundle, Extender> entry : trackedMetaObjects.entrySet()) {
			Extender em = entry.getValue();
			String id = em.getInterfaceName();
			if (null != id && id.equals(interfaceName)) {
				return em;
			}
		}
		return null;
	}

	/**
	 * Get the interface class of the extension
	 * @return the interface class
	 * @throws ExtenderException if the registere3d implementation class of the extension does
	 * not implement the registered interface
	 */
	public Class<?> getInterface() throws ExtenderException {
		return Introspector.getInterface(getCls(), getInterfaceName());
	}

	/**
	 * Get the name of the extension interface. 
	 * <p>
	 * Note that the interface name is returned even if it is no longer tracked 
	 * 
	 * @return the interface name
	 */
	public String getInterfaceName() {
		return interfaceName;
	}
	/**
	 * The implementation class of the extension
	 * <p>
	 * Note that the class is returned even if the bundle is no longer tracked 
	 * 
	 * @return the implementation class of this extension
	 * @throws ExtenderException if the bundle context of the extension is no longer valid
	 */
	public Class<?> getCls() throws ExtenderException {
		if (null == cls) {
			Bundle bundle = getBundle();
			if (null == bundle) {
				throw new ExtenderException("get_bundle_exception", bId);
			}
			cls = Introspector.loadExtensionClass(bundle, getClsName());
		}
		return cls;
	}

	/**
	 * Get the name of the class implementing the extension interface. 
	 * <p>
	 * Note that the class name is returned even if the bundle is no longer tracked 
	 * 
	 * @return the interface name
	 */
	public String getClsName() {
		return clsName;
	}

	public Object getObject() throws ExtenderException {

		if (null == obj) {
			obj = Introspector.createExtensionObject(getCls());
		}
		return obj;
	}

	/**
	 * Return the bundle of for this extension
	 * @return the bundle object or null if the bundle context of the extension is no longer valid or 
	 * the bundle is no longer tracked
	 */
	public Bundle getBundle() {
		Bundle b = Activator.getContext().getBundle(bId);
		return b;
	}

	/**
	 * Get the service registration for this extension
	 * <p>
	 * @return a service registration object or null if the extension is not registered
	 */
	public ServiceRegistration<?> getServiceRegistration() {
		return sr;
	}

	/**
	 * Register the extension as a service.
	 * <p>
	 * @return a service registration object
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or
	 * the registered implementation class does not implement the registered interface 
	 */
	public ServiceRegistration<?> registerAsService() throws ExtenderException {
		if (null == interfaceName || null == Introspector.getInterface(getCls(), interfaceName)) {
			throw new ExtenderException("missing_interface_name", getCls());
		}
		return registerAsService(interfaceName);
	}

	/**
	 * Register the extension as a service.
	 * <p>
	 * The provided interface name is validated against the extension class implementing the
	 * interface before registration.
	 * 
	 * @param interfaceName the interface name of the extension to register as a service
	 * @return a service registration object
	 * @throws ExtenderException if the bundle context of the extension is no longer valid or
	 * the already registered implementation class does not implement the specified interface 
	 */
	private ServiceRegistration<?> registerAsService(String interfaceName) throws ExtenderException {
		try {
			if (null == interfaceName || null == Introspector.getInterface(getCls(), interfaceName)) {
				throw new ExtenderException("missing_interface_name", getCls());
			}
			sr = getBundle().getBundleContext().registerService(interfaceName, getObject(), null);
			System.out.println("Extender Dialog Contribution Service registered for: " + getClsName());
			return sr;
		} catch (IllegalStateException e) {
			throw new ExtenderException(e, e.getMessage());
		}
	}
	
	/**
	 * Unregister the registered service for the extension
	 */
	public void unregisterService() {
		if (null != sr) {
			sr.unregister();
			sr = null;
		}
	}	
}
