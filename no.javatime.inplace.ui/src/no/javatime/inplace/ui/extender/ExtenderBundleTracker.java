package no.javatime.inplace.ui.extender;

import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.pl.dependencies.service.DependencyDialog;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;

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
			String depDlgImpl = bundle.getHeaders().get(DependencyDialog.DEPENDENCY_DIALOG_HEADER);
			if (null != depDlgImpl) {
				System.out.println("[UI] Add Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));	
				return  Extender.<DependencyDialog>register(Activator.getDefault().getExtenderBundleTracker(),
						bundle, Activator.getContext().getBundle(), DependencyDialog.class, depDlgImpl);
			}
		} catch(InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		// Only track bundles of interest
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[UI] Mod Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[UI] Rem Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		object.closeServiceTracker();
	}

}