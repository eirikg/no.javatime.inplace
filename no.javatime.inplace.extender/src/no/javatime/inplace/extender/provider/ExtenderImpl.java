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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.extender.intface.Introspector;
import no.javatime.inplace.extender.intface.PrototypeServiceScopeFactory;

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
public final class ExtenderImpl<S> implements Extender<S> {

	// Context of the extender bundle
	private static final BundleContext context = Activator.getContext();
	// Extenders interface definition names
	private final String[] serviceInterfaceNames;
	// Only used when loading the service class object with the class loader from the owing bundle
	private final Bundle ownerBundle;
	// This is also the using bundle if not a using bundle is specified when getting this service
	private final Bundle regBundle;
	// The bundle tracker tracking this extender object(and possibly others) when registered with
	// a bundle tracker
	private final BundleTracker<Collection<Extender<?>>> bundleTracker;

	private final ServiceRegistration<?> serviceRegistration;
	// Bind eager. Must be accessible after service is unregistered
	private final ServiceReference<?> sr;
	// Name of the service class
	private final String serviceName;
	// The service object or a service factory object
	private final Object service;
	
	// Constructors
	// Service interface name and service
	public ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String serviceInterfaceName,
			Object service, Dictionary<String, ?> properties) throws ExtenderException {
		this(null, ownerBundle, regBundle,
				null != serviceInterfaceName ? new String[] { serviceInterfaceName } : null, service,
				properties);
	}

	// Service interface names and service
	public ExtenderImpl(Bundle ownerBundle, Bundle regBundle, String[] serviceInterfaceNames,
			Object service, Dictionary<String, ?> properties) throws ExtenderException {
		this(null, ownerBundle, regBundle, serviceInterfaceNames, service, properties);
	}

	// Bundle tracker, service interface name and service
	public ExtenderImpl(BundleTracker<Collection<Extender<?>>> bt, Bundle ownerBundle,
			Bundle regBundle, String serviceInterfaceName, Object service,
			Dictionary<String, ?> properties) throws ExtenderException {
		this(bt, ownerBundle, regBundle,
				null != serviceInterfaceName ? new String[] { serviceInterfaceName } : null, service,
				properties);
	}

	// Bundle tracker, service interface names and service
	public ExtenderImpl(BundleTracker<Collection<Extender<?>>> bt, Bundle ownerBundle,
			Bundle regBundle, String[] serviceInterfaceNames, Object service,
			Dictionary<String, ?> properties) throws ExtenderException {

		this.bundleTracker = bt;
		try {

			// Owner and registrar bundle
			this.ownerBundle = ownerBundle;
			if (null == ownerBundle) {
				throw new ExtenderException("Missing owner bundle when registering extender");
			}
			
			this.regBundle = regBundle;
			if (null == regBundle) {
				throw new ExtenderException("Missing registrar bundle when registering extender");
			}

			// Service interface names
			int size = null == serviceInterfaceNames ? 0 : serviceInterfaceNames.length;
			if (size == 0) {
				throw new ExtenderException(
						"Missing service interface name(s) when registering extender for bundle {0}",
						ownerBundle);
			}
			List<String> copy = new ArrayList<String>(size);
			for (int i = 0; i < size; i++) {
				String clazz = serviceInterfaceNames[i].intern();
				if (!copy.contains(clazz)) {
					copy.add(clazz);
				}
			}
			this.serviceInterfaceNames = copy.toArray(new String[copy.size()]);

			// Service and service name
			if (null == service) {
				throw new ExtenderException(
						"Missing service object, service name or service factory object when registering extender for {0}",
						getServiceInterfaceName());
			}
			if (service instanceof String) {
				this.serviceName = (String) service;
				this.service = Introspector.createObject(getServiceClass());
			} else {
				this.service = service;
				this.serviceName = service.getClass().getName();
			}

			// Properties
			Dictionary<String, Object> props = new Hashtable<>();
			if (null != properties) {
				for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
					String key = e.nextElement();
					props.put(key, properties.get(key));
				}
			}
			props.put(Extender.class.getSimpleName().toLowerCase(), Boolean.valueOf(true));

			if (service instanceof PrototypeServiceScopeFactory) {
				props.put(SCOPE, PROTOTYPE);
			} else if (service instanceof ServiceFactory) {
				props.put(SCOPE, BUNDLE);
			} else {
				props.put(SCOPE, SINGLETON);
			}
			// props.put(SCOPE, service instanceof ServiceFactory<?> ? BUNDLE : SINGLETON);

			// Register service
			BundleContext regContext = regBundle.getBundleContext();
			if (null == regContext) {
				throw new ExtenderException("Context of bundle {0} is invalid when registering service {1}", regBundle, getServiceInterfaceName());
			}
			serviceRegistration = regContext.registerService(this.serviceInterfaceNames, this.service, props);
			sr = serviceRegistration.getReference();
			if (!sr.isAssignableTo(getRegistrar(), getServiceClass().getName())) {
				throw new ExtenderException(
						"Bundle {0} is not compatible (same source package) as class {1}", getRegistrar(),
						getServiceClass().getName());
			}

			// Register extender
			@SuppressWarnings("unchecked")
			ExtenderServiceMap<S> serviceMap = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
			if (null != serviceMap) {
				serviceMap.put(getServiceId(), this);
			} else {
				throw new ExtenderException(
						"Extender bundle context is invalid while trying to register {0}",
						getServiceInterfaceName());
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException
				| NullPointerException e) {
			throw new ExtenderException(e, "Failed to register the extender for {0}",
					getServiceInterfaceName());
		}
	}
	
	@Override
	public Extension<S> getExtension(String serviceInterfaceName, Bundle user)
			throws ExtenderException {

		try {
			if (!getServiceInterfaceNames().contains(serviceInterfaceName)) {
				throw new ExtenderException(
						"The specified interface name {0} is not registered with this extender. ",
						serviceInterfaceName);
			}
		} catch (NullPointerException e) {
			ExtenderException ex = new ExtenderException(e,
					"Failed to get extension for for the specified service interface name");
			ex.setNullPointer(true);
			throw ex;
		}
		return new ExtensionImpl<S>(serviceInterfaceName, user);
	}

	@Override
	public Extension<S> getExtension() {
		return new ExtensionImpl<S>(this, getRegistrar());
	}

	@Override
	public Extension<S> getExtension(Bundle user) {
			
		return new ExtensionImpl<S>(this, user);
	}

	@Override
	public Extension<S> getExtension(Bundle user, ExtenderBundleTracker bt) {
			
		return new ExtensionImpl<S>(this, user, bt);
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public S getService() throws ExtenderException {

		try {
			BundleContext regContext = regBundle.getBundleContext();
			if (null == regContext) {
				throw new ExtenderException("Context of bundle {0} is invalid when getting service {1}", regBundle, getServiceInterfaceName());
			}
			if (isPrototypeServiceScope(regBundle)) {
				return ((PrototypeServiceScopeFactory<S>) service).getService(regBundle,
						(ServiceRegistration<S>) serviceRegistration);
			} else {
				
				return (S) regContext.getService(sr);
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException
				| NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Failed to get the service for {0}",
					getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public S getService(Bundle user) throws ExtenderException {

		try {
			if (isPrototypeServiceScope(user)) {
				return ((PrototypeServiceScopeFactory<S>) service).getService(user,
						(ServiceRegistration<S>) serviceRegistration);
			} else {
				BundleContext userContext = user.getBundleContext();
				if (null == userContext) {
					throw new ExtenderException("Context of bundle {0} is invalid when getting service {1}", user, getServiceInterfaceName());
				}
				return (S) userContext.getService(sr);
			}
		} catch (IllegalStateException | IllegalArgumentException | SecurityException
				| NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Failed to get the service for {0}",
					getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	/**
	 * This is a temporarily workaround which emulates prototype scope to support it on pre. OSGi 6
	 * (pre Luna). To use it in a standardized way, OSGi 6 clients should use PrototypeServiceFactory
	 * 
	 * @param user using bundle
	 * @return true if the service is of type {@code PrototypeServiceScopeFactory} and it is not the
	 * first time the specified bundle is a using bundle. Otherwise false
	 */
	private boolean isPrototypeServiceScope(Bundle user) {
		if (service instanceof PrototypeServiceScopeFactory) {
			if (getUsingBundles().contains(user)) {
				return true;
			}
		}
		// The first time or after unget service the using bundle calls the getService bundle scope is
		// used and the framework will return a new service object
		return false;

	}

	@Override
	public Boolean ungetService() throws ExtenderException {

		return ungetService(getRegistrar());
	}

	@Override
	public Boolean ungetService(Bundle user) throws ExtenderException {

		try {
			BundleContext userContext = user.getBundleContext();
			if (null == userContext) {
				throw new ExtenderException("Context of bundle {0} is invalid when ungetting service {1}", user, getServiceInterfaceName());
			}
			return userContext.ungetService(sr);
		} catch (IllegalStateException | IllegalArgumentException e) {
			throw new ExtenderException(e, "Failed to unget service for {0}", getServiceInterfaceName());
		}
	}

	@Override
	public Class<S> getServiceInterfaceClass() throws ExtenderException {

		Class<S> serviceInterfaceClass = null;

		Class<S> serviceClass = getServiceClass();
		if (null != serviceClass) {
			if (serviceClass.getName().equals(getServiceInterfaceName())) {
				// The interface service is a class
				serviceInterfaceClass = serviceClass;
			} else {
				// The interface service is an interface
				serviceInterfaceClass = Introspector.getTypeInterface(serviceClass,
						getServiceInterfaceName());
			}
		}
		return serviceInterfaceClass;
	}

	@Override
	public String getServiceInterfaceName() {

		return this.serviceInterfaceNames[0];
	}

	public Collection<String> getServiceInterfaceNames() {

		int size = serviceInterfaceNames.length;
		List<String> copy = new ArrayList<String>(size);
		for (int i = 0; i < size; i++) {
			copy.add(serviceInterfaceNames[i]);
		}
		return copy;
	}

	@Override
	public Class<S> getServiceClass() throws ExtenderException {

		// must use class loader of bundle owing the class
		Bundle bundle = getOwner();
		if (null == bundle) {
			throw new ExtenderException("Failed to get owner bundle for the {0} service interface name", getServiceInterfaceName());
		}
		return Introspector.loadClass(bundle, serviceName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceFactory<S> getServiceFactoryObject() {

		return (ServiceFactory<S>) (service instanceof ServiceFactory ? service : null);
	}

	/**
	 * Return the owner bundle specified when this extender was registered.
	 * 
	 * @return the bundle object or null if the bundle context of the extension is no longer valid
	 */
	@Override
	public Bundle getOwner() throws ExtenderException {
		
		return ownerBundle;
	}

	@Override
	public Bundle getRegistrar() throws ExtenderException {

		return regBundle;
	}

	@Override
	public Long getServiceId() {

		return (Long) sr.getProperty(Constants.SERVICE_ID);
	}

	@Override
	public Integer getServiceRanking() {

		return (Integer) sr.getProperty(Constants.SERVICE_RANKING);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ServiceReference<S> getServiceReference() throws ExtenderException {

		return (ServiceReference<S>) sr;
	}

	@Override
	public Boolean isRegistered() {

//		try {
//			ExtenderServiceMap<S> map = Activator.getExtenderServiceMap();
//			Extender<S> e = map.get(getServiceId());
//			return e != null ? true : false;
//		} catch (NullPointerException | ClassCastException e) {
//		}
		// TODO Does not seems to work as expected?
			return null == sr.getBundle() ? false : true;
	}

	@Override
	public void unregister() throws ExtenderException {

		try {
			// Unregister publish an unregistered event and  
			// the extender is removed from the extender map in the extender service listener
			serviceRegistration.unregister();
		} catch (IllegalStateException | NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Extender for {0} is already unregistered",
					getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	public Collection<Bundle> getUsingBundles() {

		Bundle[] using = sr.getUsingBundles();
		return null != using ? new ArrayList<Bundle>(Arrays.asList(using)) : Collections
				.<Bundle> emptyList();
	}

	@Override
	public BundleTracker<Collection<Extender<?>>> getBundleTracker() {

		return bundleTracker;
	}

	public Collection<Extender<?>> getTrackedExtenders() {

		Collection<Extender<?>> trackedExtenders = new ArrayList<>();
		if (null != bundleTracker) {
			Map<Bundle, Collection<Extender<?>>> tracked = bundleTracker.getTracked();
			Iterator<Entry<Bundle, Collection<Extender<?>>>> it = tracked.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Collection<Extender<?>>> entry = it.next();
				for (Extender<?> e : entry.getValue()) {
					trackedExtenders.add(e);
				}
			}
		}
		return trackedExtenders;
	}

	@Override
	public Dictionary<String, Object> getProperties() {

		String[] propKeys = sr.getPropertyKeys();
		Dictionary<String, Object> dict = new Hashtable<>();
		for (int i = 0; i < propKeys.length; i++) {
			String key = propKeys[i];
			// Assume key not null
			dict.put(key, sr.getProperty(key));
		}
		return dict;
	}

	public String[] getPropertyKeys() {

		String[] propKeys = sr.getPropertyKeys();
		return propKeys;
	}

	@Override
	public Object getProperty(String key) {

		return sr.getProperty(key);
	}

	@Override
	public void setProperty(String key, Object value) throws ExtenderException {

		Dictionary<String, Object> dict = getProperties();
		dict.put(key, value);
		try {
			serviceRegistration.setProperties(dict);
		} catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Failed to update prop for {0}",
					getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	public void setProperties(Dictionary<String, ?> dictionary) {

		try {
			serviceRegistration.setProperties(dictionary);
		} catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
			ExtenderException ex = new ExtenderException(e, "Failed to update prop for {0}",
					getServiceInterfaceName());
			if (e instanceof NullPointerException) {
				ex.setNullPointer(true);
			}
			throw ex;
		}
	}

	@SuppressWarnings("unused")
	private static <S> Collection<Extender<?>> getTrackedExtender(Extender<S> extender,
			String serviceInterfaceName) {

		BundleTracker<Collection<Extender<?>>> bt = extender.getBundleTracker();
		if (null != bt) {
			Map<Bundle, Collection<Extender<?>>> trackedObjects = bt.getTracked();
			Iterator<Entry<Bundle, Collection<Extender<?>>>> it = trackedObjects.entrySet().iterator();
			while (it.hasNext()) {
				ConcurrentMap.Entry<Bundle, Collection<Extender<?>>> entry = it.next();
				Collection<Extender<?>> trackedExtender = entry.getValue();
				for (Extender<?> e : trackedExtender) {
					String id = e.getServiceInterfaceName();
					if (null != id && id.equals(serviceInterfaceName)) {
						return trackedExtender;
					}
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

	@SuppressWarnings("unused")
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
