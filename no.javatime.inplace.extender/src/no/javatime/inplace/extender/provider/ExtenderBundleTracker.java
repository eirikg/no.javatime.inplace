package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;


/**
 * Registers extensions
 */
public class ExtenderBundleTracker implements BundleTrackerCustomizer<Extender<?>> {

	
	@Override
	public Extender<?> addingBundle(Bundle bundle, BundleEvent event) {
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Extender<?> object) {
		object.closeServiceTracker();
	}

}