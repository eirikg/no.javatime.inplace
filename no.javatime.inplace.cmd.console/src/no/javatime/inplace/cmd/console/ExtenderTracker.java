package no.javatime.inplace.cmd.console;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutorServiceFactory;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
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

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	Extender<BundleRegion> bundleRegionExtender;
	Extender<CommandOptions> commandOptionsExtender;

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}

	public void trackOwn() {
		registerAndTrack(context.getBundle(), CommandProvider.class.getName(),
				new BundleProjectCommandProvider(), null);
	}

	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		if (!getFilter(bundle)) {
			return null;
		}

		try {
			Dictionary<String, String> headers =  bundle.getHeaders();
			String serviceName = headers.get(ActivateProject.ACTIVATE_PROJECT_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, ActivateProject.class.getName(), new BundleExecutorServiceFactory(serviceName), null);
				trackExtender(bundle, Install.class.getName(), new BundleExecutorServiceFactory(headers.get(Install.INSTALL_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Deactivate.class.getName(), new BundleExecutorServiceFactory(headers.get(Deactivate.DEACTIVATE_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Start.class.getName(), new BundleExecutorServiceFactory(headers.get(Start.START_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Stop.class.getName(), new BundleExecutorServiceFactory(headers.get(Stop.STOP_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Refresh.class.getName(), new BundleExecutorServiceFactory(headers.get(Refresh.REFRESH_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Update.class.getName(), new BundleExecutorServiceFactory(headers.get(Update.UPDATE_BUNDLE_SERVICE)), null);
				trackExtender(bundle, Reset.class.getName(), new BundleExecutorServiceFactory(headers.get(Reset.RESET_BUNDLE_SERVICE)), null);
				//track(bundle, TogglePolicy.class.getName(), new BundleExecutorServiceFactory(headers.get(TogglePolicy.TOGGLE_POLICY_SERVICE)));
				//track(bundle, UpdateBundleClassPath.class.getName(), new BundleExecutorServiceFactory(headers.get(UpdateBundleClassPath.UPDATE_BUNDLE_CLASS_PATH_SERVICE)));
			}
			serviceName = headers.get(BundleCommand.BUNDLE_COMMAND_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, BundleCommand.class.getName(), new BundleCommandServiceFactory(), null);
				bundleRegionExtender = trackExtender(bundle, BundleRegion.class.getName(), new BundleRegionServiceFactory(), null);
				trackExtender(bundle, BundleTransition.class.getName(), new BundleTransitionServiceFactory(), null);
				trackExtender(bundle, BundleProjectCandidates.class.getName(), new BundleProjectCandidatesServiceFactory(), null);
				trackExtender(bundle, BundleProjectMeta.class.getName(), new BundleProjectMetaServiceFactory(), null);
			}
			serviceName = headers.get(CommandOptions.COMMAND_OPTIONS_SERVICE);
			if (null != serviceName) {
				commandOptionsExtender = trackExtender(bundle, CommandOptions.class.getName(), serviceName, null);
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
