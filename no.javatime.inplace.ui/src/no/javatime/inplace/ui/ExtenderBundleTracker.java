package no.javatime.inplace.ui;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
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

	public ExtenderBundleTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Extender<?>> customizer) {
		super(context, stateMask, customizer);
	}

	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		try {
			String dependencyDialogSvcName = bundle.getHeaders().get(
					DependencyDialog.DEPENDENCY_DIALOG_HEADER);
			// Extend and register the dependency dialog as a service if not registered by others
			if (null != dependencyDialogSvcName && null == Extenders.getExtender(DependencyDialog.class.getName())) {
				return Extenders.register(this, bundle, context.getBundle(),
						DependencyDialog.class.getName(), dependencyDialogSvcName, null);
			}
			String bundleLogViewSvcName = bundle.getHeaders().get(BundleLogView.BUNDLE_LOG_VIEW_HEADER);
			// Extend and register the bundle log view as a service if not registered by others
			if (null != bundleLogViewSvcName && null == Extenders.getExtender(BundleLogView.class.getName())) {
					return Extenders.register(this, bundle, context.getBundle(),
						BundleLogView.class.getName(), bundleLogViewSvcName, null);
			}
			String bundleConsoleViewSvcname = bundle.getHeaders().get(BundleConsoleFactory.BUNDLE_CONSOLE_HEADER);
			if (null != bundleConsoleViewSvcname && null == Extenders.getExtender(BundleConsoleFactory.class.getName())) {
				return Extenders.register(this, bundle, context.getBundle(), BundleConsoleFactory.class.getName(),
						bundleConsoleViewSvcname, null);
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		// Only track bundles of interest
		return null;
	}
}
