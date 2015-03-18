package no.javatime.inplace.ui;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ExtensionServiceTracker<S> implements ServiceTrackerCustomizer<S, S>{
	private BundleContext context;
	public ExtensionServiceTracker(BundleContext context) {
		this.context = context;
	}
	
	@Override
	public S addingService(ServiceReference<S> reference) {
		return context.getService(reference);
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, S service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference<S> reference, S service) {
		// TODO Auto-generated method stub
		
	}
}
