package no.javatime.inplace.pl.preferences;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
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

	Extender<CommandOptions> commandOptionsExtender;

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		if (!getFilter(bundle)) {
			return null;
		}

		try {
			String serviceName = bundle.getHeaders().get(CommandOptions.COMMAND_OPTIONS_SERVICE);
			if (null != serviceName) {
				commandOptionsExtender = trackExtender(bundle, CommandOptions.class.getName(), serviceName);
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
