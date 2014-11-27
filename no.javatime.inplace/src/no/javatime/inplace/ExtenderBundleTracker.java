package no.javatime.inplace;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */
public class ExtenderBundleTracker extends BundleTracker<Extender<?>> {


	public ExtenderBundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<Extender<?>> customizer) {
		super(context, stateMask, customizer);
	}

	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		try { 
			// Get the service name from the bundle providing the service
			String bundleLogSvcName = bundle.getHeaders().get(BundleLog.BUNDLE_LOG_HEADER);
			// Extend and register the bundle log as a service if not registered by others
			if (null != bundleLogSvcName && null == Extenders.getExtender(BundleLog.class.getName())) {
				return Extenders.register(this, bundle, context.getBundle(), 
						BundleLog.class.getName(), bundleLogSvcName, null);
			}
			String bundleConsoleViewSvcName = bundle.getHeaders().get(BundleConsoleFactory.BUNDLE_CONSOLE_HEADER);
			if (null != bundleConsoleViewSvcName && null == Extenders.getExtender(BundleConsoleFactory.class.getName())) {
				return Extenders.register(this, bundle, context.getBundle(), BundleConsoleFactory.class.getName(),
						bundleConsoleViewSvcName, null);
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