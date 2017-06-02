package no.javatime.inplace.region;

import no.javatime.inplace.region.closure.ExternalDuplicates;

import org.eclipse.ui.IStartup;

public class EarlyStartUp implements IStartup {

	@Override
	public void earlyStartup() {
		
		ExternalDuplicates duplicateEvents = Activator.getDefault().getDuplicateEvents();
		duplicateEvents.initExternalBundles();
		Activator.getContext().addBundleListener(duplicateEvents);
	}

}
