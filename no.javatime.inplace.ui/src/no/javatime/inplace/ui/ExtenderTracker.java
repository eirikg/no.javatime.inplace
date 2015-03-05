package no.javatime.inplace.ui;

import java.util.Collection;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
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

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}

	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		try {
			String dependencyDialogSvcName = bundle.getHeaders().get(
					DependencyDialog.DEPENDENCY_DIALOG_IMPL);
			// Extend and register the dependency dialog as a service if not registered by others
			if (null != dependencyDialogSvcName && null == Extenders.getExtender(DependencyDialog.class.getName())) {
				register(bundle, DependencyDialog.class.getName(), dependencyDialogSvcName, null);
			}
			String bundleLogViewSvcName = bundle.getHeaders().get(BundleLogView.BUNDLE_LOG_VIEW_IMPL);
			// Extend and register the bundle log view as a service if not registered by others
			if (null != bundleLogViewSvcName && null == Extenders.getExtender(BundleLogView.class.getName())) {
					register(bundle, BundleLogView.class.getName(), bundleLogViewSvcName, null);
			}
			String bundleConsoleViewSvcname = bundle.getHeaders().get(BundleConsoleFactory.BUNDLE_CONSOLE_IMPL);
			if (null != bundleConsoleViewSvcname && null == Extenders.getExtender(BundleConsoleFactory.class.getName())) {
				register(bundle, BundleConsoleFactory.class.getName(), bundleConsoleViewSvcname, null);
			}
		} catch (ExtenderException |IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		return super.addingBundle(bundle, event);
	}
}
