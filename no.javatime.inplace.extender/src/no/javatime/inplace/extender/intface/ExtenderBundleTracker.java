package no.javatime.inplace.extender.intface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.provider.ExtenderImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Sub type this class to register and track bundles and extenders of bundles with a bundle tracker.
 * <p>
 * When registering one or more extenders and returning them from the
 * {@link #addingBundle(Bundle, BundleEvent)} event they are tracked. When an extender hosted by a
 * bundle is tracked a callback is generated when the hosting bundle is stopped and the underlying
 * service of the extender and any extensions created from the extender is unregistered.
 * 
 */
public class ExtenderBundleTracker extends BundleTracker<Collection<Extender<?>>> {

	Collection<Extender<?>> extenders = null;
	Collection<Extension<?>> extensions = null;
	String filter = "no.javatime.inplace";
	
	public ExtenderBundleTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	public void setFilter(String filter) {
		this.filter = filter;
	}
	
	public boolean getFilter(Bundle bundle) {

		if (!bundle.getSymbolicName().startsWith(filter)) {
			return false;
		}
		return true;
		// Do not consider workspace region bundles
		//	if (null != bundleRegionExtender && bundleRegionExtender.getService().exist(bundle)) {
		//		return null;
		//	}
	}
	/**
	 * Registers and tracks a service with the specified properties under the specified
	 * service interface class names as an extender where {@code owner} is the bundle hosting the
	 * service and the service.
	 * <p>
	 * The using bundle is by the default the bundle registering this extender.
	 * <p>
	 * If the specified service is a service factory object the service scope is bundle (
	 * {@code ServiceFactory}) or prototype ({@code PrototypeServiceFactory}), and singleton if the
	 * the specified service is a service object or a service class name.
	 * <p>
	 * Extenders are typically registered in the method overriding this
	 * {@code #addingBundle(Bundle, BundleEvent)} method in the class sub typing this class.
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle hosting the specified service and the service interface name
	 * @param serviceInterfaceNames Interface names under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 * @see org.osgi.framework.BundleContext#registerService(String[], Object, Dictionary)
	 */
	public <S> Extender<S> register(Bundle owner, String[] serviceInterfaceNames, Object service,
			Dictionary<String, ?> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(this, owner, context.getBundle(),
				serviceInterfaceNames, service, properties);
		trackExtender(extender);
		return extender;
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class name as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name.
	 * <p>
	 * The using bundle is by the default the bundle registering this extender.
	 * <p>
	 * This method is otherwise identical to {@link #register(Bundle, String[], Object, Dictionary)}
	 * and is provided as a convenience when the {@code service} will only be registered under a
	 * single interface class name.
	 * <p>
	 * Extenders are typically registered in the method overriding this
	 * {@code #addingBundle(Bundle, BundleEvent)} method in the class sub typing this class.
	 * 
	 * @param <S> Type of Service.
	 * @param owner The bundle hosting the specified service and the service interface name
	 * @param serviceInterfaceName The class or interface name under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extender object of the registered service type.
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	public <S> Extender<S> register(Bundle owner, String serviceInterfaceName, Object service,
			Dictionary<String, ?> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(this, owner, context.getBundle(),
				serviceInterfaceName, service, properties);
		trackExtender(extender);
		return extender;
	}

	/**
	 * Tracks an extender with the specified service interface name, the service and the specified
	 * bundle as the owner of the tracked extender.
	 * <p>
	 * If an extender with the specified service interface name and the specified bundle as the
	 * registrar is not already registered an extender with bundle sub typing this bundle tracker as
	 * the registrar is registered before tracked.
	 * 
	 * @param <S> Type of Service.
	 * @param bundle The bundle hosting the specified service and the service interface name
	 * @param serviceInterfaceName The class or interface name under which the service can be located.
	 * @param service A fully qualified class name where the class representing the class name has a
	 * default or empty constructor, a service object or a {@code ServiceFactory} object
	 * @param properties The properties for this service. Can be null.
	 * @return An extension object of the registered extender tracked by this bundle tracker and this
	 * bundle as the using bundle of the extension
	 * @throws ExtenderException if the bundle context of the specified registrar or owner bundle is
	 * no longer valid, the registered implementation class does not implement the registered
	 * interface, the interface name is illegal, a security violation or if the service object could
	 * not be obtained
	 */
	protected <S> Extension<S> trackExtension(Bundle bundle, String serviceIntefaceName, Object service)
			throws ExtenderException {

		Extender<S> extender = Extenders.getExtender(serviceIntefaceName, bundle);
		if (null == extender) {
			extender = register(bundle, serviceIntefaceName, service, null);
		} else {
			trackExtender(extender);
		}
		if (null == extensions) {
			extensions = new ArrayList<>();
		}
		Extension<S> extension = extender.getExtension(context.getBundle(), this);
		extensions.add(extension);
		return extension;
	}

	protected <S> Extender<S> trackExtender(Bundle bundle, String serviceIntefaceName, Object service)
			throws ExtenderException {

		Extender<S> extender = Extenders.getExtender(serviceIntefaceName, bundle);
		if (null == extender) {
			extender = register(bundle, serviceIntefaceName, service, null);
		} else {
			trackExtender(extender);
		}
		return extender;
	}

	/**
	 * To track the registered extenders with this bundle tracker the registered extenders should be
	 * returned by the {@code #addingBundle(Bundle, BundleEvent)} method overriding this method in the
	 * class sub typing this class. Eg: {@code return super.addingBundle(bundle, event);}
	 */
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		return trackExtenders();
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Collection<Extender<?>> object) {

		// Tracked services are removed by the framework and the extender is removed by the extender
		// bundle
		if (null != extensions) {
			for (Extension<?> extension : extensions) {
				if (extension.getTrackingCount() != -1) {
					extension.closeTrackedService();
				}
			}
			extensions = null;
		}
		super.removedBundle(bundle, event, object);
	}

	/**
	 * Called when the underlying service for the specified extender is unregistered and the extender
	 * was registered by the bundle tracking this extender. An extender belonging to a bundle being
	 * stopped is automatically unregistered by the extender bundle and thus will generate a call back
	 * to this unregistering method as long as the extender belonging to the stopped bundle is tracked
	 * by the bundle who registered the extender
	 * <p>
	 * The service may have been unregistered by {@link Extender#unregister()} or by
	 * {@link ServiceRegistration#unregister()}
	 * 
	 * @param extender An extender registered by this bundle and tracked by this bundle tracker
	 * @see #register(Bundle, String, Object, Dictionary)
	 * @see #register(Bundle, String[], Object, Dictionary)
	 */
	public void unregistering(Extender<?> extender) {
	}

	/**
	 * Called when the underlying service of the specified extension is unregistered and the specified
	 * extension has an open service tracker on the unregistered service. It is a prerequisite that
	 * the specified extension has been created with the tracker sub typing this bundle tracker and
	 * the bundle of the tracker as the using bundle.
	 * <p>
	 * The service may have been unregistered by {@link Extender#unregister()} or by
	 * {@link ServiceRegistration#unregister()}
	 * 
	 * @param extension extension with a service tracker
	 * @see Extender#getExtension(Bundle, ExtenderBundleTracker)
	 * @see Extenders#getExtension(String, Bundle, ExtenderBundleTracker)
	 * @see #trackExtension(Bundle, String, Object)
	 */
	public void unregistering(Extension<?> extension) {
	}

	/**
	 * Utility returning a service for a tracked extender but never null
	 * 
	 * @return the service
	 * @throws ExtenderException if failing to get the extender service
	 */
	public <S> S getService(String serviceInterfaceName) throws ExtenderException {

		Extender<S> extender = getTrackedExtender(serviceInterfaceName);
		if (null == extender) {
			throw new ExtenderException("Null extender in {0}", serviceInterfaceName);
		}
		S s = extender.getService(context.getBundle());
		if (null == s) {
			throw new ExtenderException("Failed to get the {0} service", serviceInterfaceName);
		}
		return s;
	}
	
	public <S> Extension<S> getExtension(String serviceInterfaceName) throws ExtenderException {
		
		Extender<S> extender = getTrackedExtender(serviceInterfaceName);
		if (null == extender) {
			throw new ExtenderException("Null extender in {0}", serviceInterfaceName);
		}
		return extender.getExtension(context.getBundle(), this);
	}

	public <S> Extender<S> getTrackedExtender(String serviceInterfaceName) {

		Collection<Extender<?>> extenders = getTrackedExtenders(serviceInterfaceName);
		Extender<S> rankedExtender = null;
		for (Extender<?> extender : extenders) {
			if (extender.getServiceInterfaceName().equals(serviceInterfaceName)) {
				if (null == rankedExtender) {
					rankedExtender = (Extender<S>) extender;
					continue;
				}
				// The extender with highest ranking and if a tie the lowest service id
				if (extender.getServiceReference().compareTo(rankedExtender.getServiceReference()) > 0) {
					rankedExtender = (Extender<S>) extender;
				}
			}
		}
		return rankedExtender;
	}

	public final Extender<?> getTrackedExtender(Bundle bundle, Extender<?> extender) {

		Map<Bundle, Collection<Extender<?>>> tracked = getTracked();
		Collection<Extender<?>> extenders = tracked.get(bundle);
		if (null != extenders) {
			for (Extender<?> trackedExtender : extenders) {
				if (trackedExtender.equals(extender)) {
					return trackedExtender;
				}
			}
		}
		return null;
	}

	public final Collection<Extender<?>> getTrackedExtenders(String interfaceName) {

		Collection<Extender<?>> trackedExtenders = new ArrayList<>();

		Map<Bundle, Collection<Extender<?>>> tracked = getTracked();
		Iterator<Entry<Bundle, Collection<Extender<?>>>> it = tracked.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Bundle, Collection<Extender<?>>> entry = it.next();
			for (Extender<?> e : entry.getValue()) {
				if (e.getServiceInterfaceName().equals(interfaceName)) {
					//if (null != Extenders.getExtender(e.getServiceId())) {
						trackedExtenders.add(e);
					//}
				}
			}
		}
		return trackedExtenders;
	}

	/**
	 * Return all extenders being tracked by this bundle extender tracker
	 * 
	 * 
	 * @return All extenders being tracked by this bundle extender tracker or an empty collection if
	 * no extenders are being tracked
	 */
	public final Collection<Extender<?>> getTrackedExtenders() {

		Collection<Extender<?>> trackedExtenders = new ArrayList<>();

		Map<Bundle, Collection<Extender<?>>> tracked = getTracked();
		Iterator<Entry<Bundle, Collection<Extender<?>>>> it = tracked.entrySet().iterator();
		while (it.hasNext()) {
			ConcurrentMap.Entry<Bundle, Collection<Extender<?>>> entry = it.next();
			for (Extender<?> e : entry.getValue()) {
				if (null != Extenders.getExtender(e.getServiceId())) {
					trackedExtenders.add(e);
				}
			}
		}
		return trackedExtenders;
	}

	public void trackExtender(Extender<?> extender) {
		if (null == extenders) {
			extenders = new ArrayList<>();
		}
		extenders.add(extender);
	}

	private Collection<Extender<?>> trackExtenders() {

		if (null == extenders) {
			return extenders;
		} else {
			Collection<Extender<?>> copyList = new ArrayList<>(extenders);
			extenders = null;
			return copyList;
		}
	}
}