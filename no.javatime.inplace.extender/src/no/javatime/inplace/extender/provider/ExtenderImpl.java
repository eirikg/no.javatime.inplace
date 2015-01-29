package no.javatime.inplace.extender.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.extender.intface.Introspector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

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

	public static final String extenderFilter = "(" + Extender.class.getSimpleName() + "=true)";

	final private static BundleContext context = Activator.getContext();

	/** Extenders interface definition name */
	final private String serviceInterfaceName;

	/** Interface class of the extension */
	private Class<S> serviceInterfaceClass;

	/**
	 * Context of bundle owing this service. Only used when loading the service class object with the
	 * class loader from the owing bundle
	 */
	final private BundleContext ownerContext;

	/**
	 * Context of bundle registering this service. This is also the using bundle if not a using bundle
	 * is specified when getting this service
	 */
	final private BundleContext regContext;

	/** Name of the service class */
	final private String serviceName;
	/** Service class loaded by the extender or obtained from service registry */
	private Class<S> serviceClass;
	/** Service registration object returned when the service is registered */
	private ServiceRegistration<?> serviceRegistration;
	/** The bundle tracker also holding the extender object when registered with a bundle tracker */
	final private BundleTracker<Extender<?>> bundleTracker;
	/** The service object or a service factory object */
	private Object service;
	final private Dictionary<String, Object> properties;

	/* internal object to use for synchronization */
	private final Object registrationLock = new Object();

	private ExtenderImpl(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName, Object service,
			Dictionary<String, Object> properties) {

		this.bundleTracker = bt;
		this.ownerContext = ownerBundle.getBundleContext();
		this.regContext = regBundle.getBundleContext();
		this.serviceInterfaceName = serviceInterfaceName;
		this.serviceName = null != serviceName ? serviceName : service.getClass().getName();
		this.service = service;
		this.properties = new Hashtable<>();
		synchronized (this.properties) {
			if (null != properties) {
				for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
					String key = e.nextElement();
					this.properties.put(key, properties.get(key));
				}
			}
			this.properties.put(Extender.class.getSimpleName(), Boolean.valueOf(true));
		}
	}

	// Constructors
	public ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			String serviceName, Dictionary<String, Object> properties) {
		this(null, ownerBundle, regBundle, serviceInterfaceName, serviceName, null, properties);
	}

	public ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			Object service, Dictionary<String, Object> properties) {
		this(null, ownerBundle, regBundle, serviceInterfaceName, (String) null, service, properties);
	}

	public ExtenderImpl(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, Object service, Dictionary<String, Object> properties) {
		this(bt, ownerBundle, regBundle, serviceInterfaceName, null, service, properties);
	}

	public ExtenderImpl(BundleTracker<Extender<?>> bt, Bundle ownerBundle, Bundle regBundle,
			String serviceInterfaceName, String serviceName, Dictionary<String, Object> properties) {
		this(bt, ownerBundle, regBundle, serviceInterfaceName, serviceName, null, properties);
	}

	@Override
	public Extension<S> getExtension() {

		return !isServiceRegistered() ? null : new ExtensionImpl<S>(getServiceInterfaceName(),
				getRegistrarBundle());
	}

	@Override
	public S getService(Bundle bundle) throws ExtenderException {

		try {
			ServiceReference<S> sr = getServicereReference();
			return null == sr ? null : bundle.getBundleContext().getService(sr);
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e, e.getMessage());
		}
	}

	@Override
	public S getService() throws ExtenderException {
		try {
			ServiceReference<S> sr = getServicereReference();
			return null == sr ? null : regContext.getService(sr);
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e);
		}
	}

	@Override
	public void unregisterService() throws ExtenderException {
		try {
			synchronized (registrationLock) {
				serviceRegistration.unregister();
			}
		} catch (IllegalStateException e) {
			// TODO: add info
			throw new ExtenderException(e);
		}
	}

	@Override
	public Boolean ungetService() {
		return ungetService(getRegistrarBundle());
	}

	@Override
	public Boolean ungetService(Bundle bundle) {
		try {
			ServiceReference<S> sr = getServicereReference();
			return null == sr ? false : bundle.getBundleContext().ungetService(sr);
		} catch (IllegalStateException | IllegalArgumentException e) {
			// TODO: add info to this exception
			throw new ExtenderException(e);
		}
	}

	@Override
	public Class<S> getInterfaceServiceClass() throws ExtenderException {
		synchronized (registrationLock) {
			if (null == serviceInterfaceClass) {
				Class<S> serviceClass = getServiceClass();
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
	}

	@Override
	public String getServiceInterfaceName() {
		return serviceInterfaceName;
	}

	@Override
	public Class<S> getServiceClass() throws ExtenderException {

		// must use class loader of bundle owing the class
		Bundle bundle = getOwnerBundle();
		if (null == bundle) {
			throw new ExtenderException("Failed to get bundle with identifier {0}", getOwnerBundle()
					.getBundleId());
		}
		synchronized (registrationLock) {
			if (null == serviceClass) {
				if (null == serviceName) {
					throw new ExtenderException("Missing class name implementing extender interface");
				}
				serviceClass = Introspector.loadClass(bundle, serviceName);
			}
			return serviceClass;
		}
	}

	@Override
	public Object getServiceObject() throws ExtenderException {

		// If service is null a service class name must have been specified at registration time
		synchronized (registrationLock) {
			return null == service ? service = Introspector.createObject(getServiceClass()) : service;
		}
	}

	/**
	 * Return the owner bundle specified when this extender was registered.
	 * 
	 * @return the bundle object or null if the bundle context of the extension is no longer valid
	 */
	@Override
	public Bundle getOwnerBundle() {

		return ownerContext.getBundle();
	}

	@Override
	public Bundle getRegistrarBundle() {

		return regContext.getBundle();
	}

	@Override
	public Long getServiceId() {

		ServiceReference<S> sr = getServicereReference();
		return null == sr ? null : (Long) sr.getProperty(Constants.SERVICE_ID);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ServiceReference<S> getServicereReference() throws ExtenderException {
		try {
			synchronized (registrationLock) {
				return null == serviceRegistration ? null : (ServiceReference<S>) serviceRegistration
						.getReference();
			}
		} catch (SecurityException e) {
			// TODO add info
			throw new ExtenderException(e);
		} catch (IllegalStateException e) {
			// Service unregistered
		}
		return null;
	}

	@Override
	public Boolean isServiceRegistered() {

		synchronized (registrationLock) {
			if (null != serviceRegistration) {
				try {
					serviceRegistration.getReference();
					return true;
				} catch (IllegalStateException | SecurityException e) {
					// Service unregistered or security violation
				}
			}
			serviceRegistration = null;
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Boolean registerService() throws ExtenderException {
		try {
			if (!isServiceRegistered()) {
				if (null == serviceInterfaceName) {
					throw new ExtenderException(
							"Missing interface of implementation class {0} when registering service",
							serviceClass);
				}
				synchronized (registrationLock) {
					Activator.getDefault().getExtenderListener().setExtender(this);					
					serviceRegistration = regContext.registerService(serviceInterfaceName,
							getServiceObject(), properties);
				}
				// Validate the service
				ServiceReference<?> serviceReference = getServicereReference();
				if (null == serviceReference) {
					throw new ExtenderException("Failed to get the service for interface {0}",
							serviceInterfaceName);
				}
				if (!serviceReference.isAssignableTo(getRegistrarBundle(), getServiceClass().getName())) {
					throw new ExtenderException(
							"Bundle {0} is not compatible (same source package) as class {1}",
							getRegistrarBundle(), getServiceClass().getName());
				}
				setScope(serviceReference);
				// Add this registered extender to the service map
				ExtenderServiceMap<S> esm = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
				if (null != esm) {
					esm.put(serviceReference, this);
					return true;
				}
				return false;
			} else {
				return false;
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			throw new ExtenderException(e, e.getMessage());
		}
	}

	private boolean setScope(ServiceReference<?> sr) {

		if (null == sr.getProperty(SCOPE)) {
			// This is for OSGi previous to R6 (Prototype SCOPE not supported)
			synchronized (properties) {
				if (service instanceof ServiceFactory) {
					return setProperty(SCOPE, BUNDLE);
				} else {
					return setProperty(SCOPE, SINGLETON);
				}
			}
		}
		return false;
	}

	public Collection<Bundle> getUsingBundles() {
		ServiceReference<?> sr = getServicereReference();
		if (null != sr) {
			Bundle[] using = sr.getUsingBundles();
			if (null != using) {
				return new ArrayList<Bundle>(Arrays.asList(using));
			}
		}
		return Collections.<Bundle>emptyList();
	}
	
	@SuppressWarnings("unused")
	private void printServiceregInfo(ServiceReference<?> ref) {

		System.out.println("------------   Service Registation info:  --------------------\n");
		System.out.println("Owner bundle           			 : " + getOwnerBundle().getSymbolicName());
		System.out.println("Bundle registered service    : " + ref.getBundle().getSymbolicName());
		System.out.println("Registration bundle    			 : " + getRegistrarBundle().getSymbolicName());
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

	@Override
	public BundleTracker<Extender<?>> getBundleTracker() {
		return bundleTracker;
	}
	
	public Collection<Extender<?>> getTrackedExtenders() {
		
		Collection<Extender<?>> trackedExtenders = new ArrayList<>();
		if (null != bundleTracker) {
			Map<Bundle, Extender<?>> tracked = bundleTracker.getTracked();
			Iterator<Entry<Bundle, Extender<?>>> it = tracked.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Extender<?>> entry = it.next();
				trackedExtenders.add(entry.getValue());
			}
		}
		return trackedExtenders;				
	}
	
	@Override
	public Dictionary<String, ?> getProperties() {

		ServiceReference<?> sr = getServicereReference();
		String[] propKeys = sr.getPropertyKeys();
		Dictionary<String, Object> dict = new Hashtable<>();
		for (int i = 0; i < propKeys.length; i++) {
			String key = propKeys[i];
			dict.put(key, sr.getProperty(key));
		}
		return dict;
	}
	
	@Override
	public boolean setProperty(String key, Object property) {

		if (null != serviceRegistration) {
			synchronized (properties) {
				properties.put(key, property);				
				serviceRegistration.setProperties(properties);
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unused")
	private static <S> Extender<?> getTrackedExtender(Extender<S> extender,
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
				Bundle bundle = context.getBundle(bid);
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
