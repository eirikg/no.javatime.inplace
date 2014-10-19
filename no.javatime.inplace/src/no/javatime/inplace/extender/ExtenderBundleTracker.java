package no.javatime.inplace.extender;

import java.util.Dictionary;
import java.util.Hashtable;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services facilitated by other bundles
 */
public class ExtenderBundleTracker extends BundleTracker<Extender<?>> {

	public ExtenderBundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<Extender<?>> customizer) {
		super(context, stateMask, customizer);
	}

	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		try { 
			// Extend and register the bundle log as a service
			String bundleLogImpl = bundle.getHeaders().get(BundleLog.BUNDLE_LOG_HEADER);
			if (null != bundleLogImpl) {
				Dictionary<String, Object> properties = new Hashtable<>();
				properties.put(BundleLog.class.getName(), "logging");
				return Extenders.register(this, bundle, InPlace.getContext().getBundle(), 
						BundleLog.class.getName(), bundleLogImpl, properties);
//				Extension<Extender<?>> extender = new ExtensionImpl<>(Extender.class.getName());
//				Extender<?> ser = extender.getService();
//				return ser.register(InPlace.get().getExtenderBundleTracker(),
//						bundle, InPlace.getContext().getBundle(), BundleLog.class.getName(), bundleLogImpl);
			}

		} catch(InPlaceException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		// Only track bundles of interest
		return null;  
	}	
}