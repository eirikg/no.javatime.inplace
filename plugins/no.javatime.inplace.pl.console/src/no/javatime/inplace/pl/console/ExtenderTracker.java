package no.javatime.inplace.pl.console;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	Extender<MessageOptions> messageOptionsExtender;

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		if (!getFilter(bundle)) {
			return null;
		}

		try {
			String serviceName = bundle.getHeaders().get(MessageOptions.MESSAGE_OPTIONS_SERVICE);
			if (null != serviceName) {
				messageOptionsExtender = trackExtender(bundle, MessageOptions.class.getName(), serviceName, null);
			}
		} catch (ExtenderException | IllegalStateException e) {
			e.printStackTrace();
		}
		return super.addingBundle(bundle, event);
	}	
	
	@Override
	public void unregistering(Extender<?> extender) {

		super.unregistering(extender);
	}
}
