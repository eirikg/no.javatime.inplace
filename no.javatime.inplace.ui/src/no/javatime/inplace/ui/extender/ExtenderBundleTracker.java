package no.javatime.inplace.ui.extender;

import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.service.DependencyDialog;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ExtenderBundleTracker implements BundleTrackerCustomizer<Extender> {

	public final static String DEPENDENCY_DIALOG_CLASS_NAME = "Dependency-Dialog";

  @Override
  public Extender addingBundle(Bundle bundle, BundleEvent event) {

  System.out.println("[TT] Add Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));	
  String extensionClassName = bundle.getHeaders().get(DEPENDENCY_DIALOG_CLASS_NAME);
  if (null != extensionClassName) {
  	try { 
  		return new DependencyDialogProxy(bundle.getBundleId(), 
  				DependencyDialog.class.getName(), extensionClassName);
  	} catch(ExtenderException e) {
  		StatusManager.getManager().handle(
  				new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
  				StatusManager.LOG);						
  	}
  }
  // Only track bundles of interest
  return null;  
  }

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender object) {
		System.out.println("[TT] Mod Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender object) {
		System.out.println("[TT] Rem Bundle: " + bundle + " " + BundleManager.getCommand().getStateName(event));
		//object.setId(null);
	}

}