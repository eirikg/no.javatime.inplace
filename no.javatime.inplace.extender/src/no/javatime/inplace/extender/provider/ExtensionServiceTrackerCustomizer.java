package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.Extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ExtensionServiceTrackerCustomizer<S> implements ServiceTrackerCustomizer<S, S>{

	private Bundle bundle;
	private Extender<S> extender;
	
	public ExtensionServiceTrackerCustomizer(Extender<S> extender, Bundle bundle) {
		this.bundle = bundle;
		this.extender = extender;
	}
	
	@Override
	public S addingService(ServiceReference<S> reference) {

		return extender.getService(bundle);
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, S service) {
		
	}

	@Override
	public void removedService(ServiceReference<S> reference, S service) {

		extender.ungetService(bundle);
	}
}
