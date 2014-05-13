package no.javatime.inplace.ui.extender;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.service.DependencyDialog;

/**
 * Registers extensions
 */
public class ExtenderBundleTracker implements BundleTrackerCustomizer<Extender<?>> {

	
	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {

		System.out.println("[TT] Add Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));	
		String extensionClassName = bundle.getHeaders().get(DependencyDialog.DEPENDENCY_DIALOG_HEADER);
		if (null != extensionClassName) {
			try { 
				return new Extender<DependencyDialog>(bundle.getBundleId(), DependencyDialog.class, extensionClassName);
			} catch(InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);						
			}
		}
		String commandOptinsClassName = bundle.getHeaders().get(CommandOptions.COMMAND_OPTIONS_HEADER);
		if (null != commandOptinsClassName) {
			try { 
				return new Extender<CommandOptions>(bundle.getBundleId(), CommandOptions.class, commandOptinsClassName);
			} catch(InPlaceException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);						
			}
		}
		// Only track bundles of interest
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[TT] Mod Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		System.out.println("[TT] Rem Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		object.closeTracker();
	}

}