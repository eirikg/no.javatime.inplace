package no.javatime.inplace.ui.extender;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;

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
	
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		try { 
			// Extend and register the dependency dialog as a service
			String dependencyDialogImpl = bundle.getHeaders().get(DependencyDialog.DEPENDENCY_DIALOG_HEADER);
			if (null != dependencyDialogImpl) {
				Extender<?> extender = Extenders.getExtender();  
				return extender.register(Activator.getDefault().getExtenderBundleTracker(),
						bundle, Activator.getContext().getBundle(), DependencyDialog.class.getName(), dependencyDialogImpl);
			}			
			// Extend and register the bundle log view as a service
			String bundleLogViewImpl = bundle.getHeaders().get(BundleLogView.BUNDLE_LOG_VIEW_HEADER);
			if (null != bundleLogViewImpl) {
				Extender<?> extender = Extenders.getExtender();
				return extender.register(Activator.getDefault().getExtenderBundleTracker(),
						bundle, Activator.getContext().getBundle(), BundleLogView.class.getName(), bundleLogViewImpl);
			}
		} catch(InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		// Only track bundles of interest
		return null;  
	}
}
