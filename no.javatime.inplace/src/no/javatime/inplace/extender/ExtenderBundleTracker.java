package no.javatime.inplace.extender;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;

/**
 * Registers extensions
 */
public class ExtenderBundleTracker implements BundleTrackerCustomizer<Extender<?>> {

	
	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		System.out.println("[IP] Add Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));	
		String commandOptinsClassName = bundle.getHeaders().get(CommandOptions.COMMAND_OPTIONS_HEADER);
		if (null != commandOptinsClassName) {
			try { 
				return new Extender<CommandOptions>(InPlace.getDefault().getExtenderBundleTracker(), bundle.getBundleId(), CommandOptions.class, commandOptinsClassName);
			} catch(InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);						
			}
		}
		// Only track bundles of interest
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[IP] Mod Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[IP] Rem Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		object.closeTracker();
	}

}