package no.javatime.inplace.dl.preferences;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.impl.CommandOptionsImpl;
import no.javatime.inplace.dl.preferences.impl.DependencyOptionsImpl;
import no.javatime.inplace.dl.preferences.impl.MessageOptionsImpl;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers and track own option services
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		if (!getFilter(bundle)) {
			return null;
		}

		if (null != bundle.getHeaders().get(CommandOptions.COMMAND_OPTIONS_SERVICE)) {
			registerAndTrack(bundle, CommandOptions.class.getName(), new CommandOptionsImpl(), null);	
			registerAndTrack(bundle, DependencyOptions.class.getName(), new DependencyOptionsImpl(), null); 
			registerAndTrack(bundle, MessageOptions.class.getName(), new MessageOptionsImpl(), null);
		}
		return super.addingBundle(bundle, event);
	}
}
