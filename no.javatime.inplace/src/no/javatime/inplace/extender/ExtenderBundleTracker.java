package no.javatime.inplace.extender;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers extensions
 */
public class ExtenderBundleTracker implements BundleTrackerCustomizer<Extender<?>> {

	
	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		try { 
			String traceImpl = bundle.getHeaders().get(BundleLog.BUNDLE_LOG_HEADER);
			if (null != traceImpl) {
				return Extender.<BundleLog>register(InPlace.get().getExtenderBundleTracker(),
						bundle, InPlace.getContext().getBundle(), BundleLog.class, traceImpl);
			}

		} catch(InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		// Only track bundles of interest
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
	}

}