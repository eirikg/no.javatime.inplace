package no.javatime.inplace;

import java.util.Collection;

import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */

public class ExtenderTracker extends ExtenderBundleTracker {


	public ExtenderTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}

	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		try { 
			// Get the service name from the bundle providing the service
			String bundleLogSvcName = bundle.getHeaders().get(BundleLog.BUNDLE_LOG_IMPL);
			// Extend and register the bundle log as a service if not registered by others
			if (null != bundleLogSvcName && null == Extenders.getExtender(BundleLog.class.getName())) {
				register(bundle, BundleLog.class.getName(), new BundleServiceScopeFactory<>(bundleLogSvcName), null);
			}
			String bundleConsoleViewSvcName = bundle.getHeaders().get(BundleConsoleFactory.BUNDLE_CONSOLE_IMPL);
			if (null != bundleConsoleViewSvcName && null == Extenders.getExtender(BundleConsoleFactory.class.getName())) {
				register(bundle, BundleConsoleFactory.class.getName(), bundleConsoleViewSvcName, null);
			}
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		return super.addingBundle(bundle, event);
	}
}