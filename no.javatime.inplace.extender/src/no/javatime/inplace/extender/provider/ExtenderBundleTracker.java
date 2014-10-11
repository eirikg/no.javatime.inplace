package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;


public class ExtenderBundleTracker extends BundleTracker<ExtenderImpl<?>> {

	public ExtenderBundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<ExtenderImpl<?>> customizer) {
		super(context, stateMask, customizer);
	}
	
	@Override
	public ExtenderImpl<?> addingBundle(Bundle bundle, BundleEvent event) {
		return null;  
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, ExtenderImpl<?> object) {		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, ExtenderImpl<?> object) {
	}
}