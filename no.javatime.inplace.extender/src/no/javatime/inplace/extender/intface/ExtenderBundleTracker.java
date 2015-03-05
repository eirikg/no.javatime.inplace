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
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Sub type this class to register and track extenders with a bundle tracker.
 * <p>
 * When registering one or more extenders and returning them from the
 * {@link #addingBundle(Bundle, BundleEvent)} event they are tracked by the bundle tracker and can
 * be accessed by the bundle tracker at any time after registration.
 * 
 */
public class ExtenderBundleTracker extends BundleTracker<Collection<Extender<?>>> {

	Collection<Extender<?>> extenders = null;

	public ExtenderBundleTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}

	/**
	 * Registers the specified service with the specified properties under the specified service
	 * interface class names as an extender where {@code owner} is the bundle hosting the service and
	 * the service interface class name.
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
		addExtender(extender);
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
		addExtender(extender);
		return extender;
	}

	/**
	 * To track the registered extenders with this bundle tracker the registered extenders should be
	 * returned by the {@code #addingBundle(Bundle, BundleEvent)} method overriding this method in the
	 * class sub typing this class. Eg: {@code return super.addingBundle(bundle, event);}
	 */
	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		return trackExtenders();
	}
	
	public boolean hasTrackedExtenders(Bundle bundle) {
		Collection<Extender<?>> extenders = getTrackedExtenders();
		for (Extender<?> extender : extenders) {
			if (extender.getOwner().equals(bundle)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return all extenders being tracked by this bundle extender tracker
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
				trackedExtenders.add(e);
			}
		}
		return trackedExtenders;
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

	private void addExtender(Extender<?> extender) {
		if (null == extenders) {
			extenders = new ArrayList<>();
		}
		extenders.add(extender);
	}
}