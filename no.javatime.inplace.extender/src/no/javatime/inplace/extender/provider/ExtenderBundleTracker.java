package no.javatime.inplace.extender.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ExtenderBundleTracker implements BundleTrackerCustomizer<ExtenderImpl<?>> {

	@Override
	public ExtenderImpl<?> addingBundle(Bundle bundle, BundleEvent event) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, ExtenderImpl<?> object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, ExtenderImpl<?> object) {
		// TODO Auto-generated method stub
		
	}

}