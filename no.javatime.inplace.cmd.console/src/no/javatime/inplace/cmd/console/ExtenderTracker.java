package no.javatime.inplace.cmd.console;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.ExecutorServiceFactory;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.bundlejobs.intface.Refresh;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.Start;
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleCommandServiceFactory;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectCandidatesServiceFactory;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleProjectMetaServiceFactory;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleRegionServiceFactory;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransitionServiceFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	private void track(Bundle bundle, String serviceIntefaceName, Object service) throws ExtenderException {

		Extender<?> extender = Extenders.getExtender(serviceIntefaceName);
		if (null == extender) { 
			register(bundle, serviceIntefaceName, service, null);			
		} else {
			trackExtender(extender);
		}
	}
	
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		try {
			Dictionary<String, String> headers =  bundle.getHeaders();
			String serviceName = headers.get(ActivateProject.ACTIVATE_PROJECT_SERVICE);
			if (null != serviceName) {
				track(bundle, ActivateProject.class.getName(), new ExecutorServiceFactory(serviceName));
				track(bundle, Install.class.getName(), new ExecutorServiceFactory(headers.get(Install.INSTALL_BUNDLE_SERVICE)));
				track(bundle, Deactivate.class.getName(), new ExecutorServiceFactory(headers.get(Deactivate.DEACTIVATE_BUNDLE_SERVICE)));
				track(bundle, Start.class.getName(), new ExecutorServiceFactory(headers.get(Start.START_BUNDLE_SERVICE)));
				track(bundle, Stop.class.getName(), new ExecutorServiceFactory(headers.get(Stop.STOP_BUNDLE_SERVICE)));
				track(bundle, Refresh.class.getName(), new ExecutorServiceFactory(headers.get(Refresh.REFRESH_BUNDLE_SERVICE)));
				track(bundle, Update.class.getName(), new ExecutorServiceFactory(headers.get(Update.UPDATE_BUNDLE_SERVICE)));
				track(bundle, Reset.class.getName(), new ExecutorServiceFactory(headers.get(Reset.RESET_BUNDLE_SERVICE)));
				//track(bundle, TogglePolicy.class.getName(), new ExecutorServiceFactory(headers.get(TogglePolicy.TOGGLE_POLICY_SERVICE)));
				//track(bundle, UpdateBundleClassPath.class.getName(), new ExecutorServiceFactory(headers.get(UpdateBundleClassPath.UPDATE_BUNDLE_CLASS_PATH_SERVICE)));
			}
			serviceName = headers.get(BundleCommand.BUNDLE_COMMAND_SERVICE);
			if (null != serviceName) {
				track(bundle, BundleCommand.class.getName(), new BundleCommandServiceFactory());
				track(bundle, BundleRegion.class.getName(), new BundleRegionServiceFactory());
				track(bundle, BundleTransition.class.getName(), new BundleTransitionServiceFactory());
				track(bundle, BundleProjectCandidates.class.getName(), new BundleProjectCandidatesServiceFactory());
				track(bundle, BundleProjectMeta.class.getName(), new BundleProjectMetaServiceFactory());
			}
			serviceName = headers.get(CommandOptions.COMMAND_OPTIONS_SERVICE);
			if (null != serviceName) {
				track(bundle, CommandOptions.class.getName(), serviceName);
			}
		} catch (ExtenderException | IllegalStateException e) {
			e.printStackTrace();
		}
		return super.addingBundle(bundle, event);
	}
}
