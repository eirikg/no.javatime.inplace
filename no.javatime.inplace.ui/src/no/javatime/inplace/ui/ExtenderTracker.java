package no.javatime.inplace.ui;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutorServiceFactory;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.bundlejobs.intface.Refresh;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.Start;
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.bundlejobs.intface.TogglePolicy;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.bundlejobs.intface.UpdateBundleClassPath;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
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
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Register and track extenders
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	Extender<BundleRegion> bundleRegionExtender;
	Extender<BundleCommand> bundleCommandExtender;
	Extender<BundleTransition> bundleTransitionExtender;
	Extender<CommandOptions> commandOptionsExtender;
	Extender<MessageOptions> messageOptionsExtender;
	Extender<ResourceState> resourceStateExtender;
	Extender<BundleProjectCandidates> bundleProjectCandidatesExtender;
	Extender<BundleProjectMeta> bundleProjectMetaExtender;
	Extender<BundleExecutorEventManager> bundleExecManagerExtender;

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}
	
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) {

		if (!getFilter(bundle) && !bundle.getSymbolicName().startsWith("org.eclipse.ui.console")) {
			return null;
		}

		try {
			Dictionary<String, String> headers =  bundle.getHeaders();
			String serviceName = headers.get(ActivateProject.ACTIVATE_PROJECT_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, ActivateProject.class.getName(), new BundleExecutorServiceFactory(serviceName));
				trackExtender(bundle, Install.class.getName(), new BundleExecutorServiceFactory(headers.get(Install.INSTALL_BUNDLE_SERVICE)));
				trackExtender(bundle, Deactivate.class.getName(), new BundleExecutorServiceFactory(headers.get(Deactivate.DEACTIVATE_BUNDLE_SERVICE)));
				trackExtender(bundle, Start.class.getName(), new BundleExecutorServiceFactory(headers.get(Start.START_BUNDLE_SERVICE)));
				trackExtender(bundle, Stop.class.getName(), new BundleExecutorServiceFactory(headers.get(Stop.STOP_BUNDLE_SERVICE)));
				trackExtender(bundle, Refresh.class.getName(), new BundleExecutorServiceFactory(headers.get(Refresh.REFRESH_BUNDLE_SERVICE)));
				trackExtender(bundle, Update.class.getName(), new BundleExecutorServiceFactory(headers.get(Update.UPDATE_BUNDLE_SERVICE)));
				trackExtender(bundle, Reset.class.getName(), new BundleExecutorServiceFactory(headers.get(Reset.RESET_BUNDLE_SERVICE)));
				trackExtender(bundle, TogglePolicy.class.getName(), new BundleExecutorServiceFactory(headers.get(TogglePolicy.TOGGLE_POLICY_SERVICE)));
				trackExtender(bundle, UpdateBundleClassPath.class.getName(), new BundleExecutorServiceFactory(headers.get(UpdateBundleClassPath.UPDATE_BUNDLE_CLASS_PATH_SERVICE)));
				resourceStateExtender = trackExtender(bundle, ResourceState.class.getName(), headers.get(ResourceState.RESOURCE_STATE_SERVICE));
				serviceName = headers.get(BundleExecutorEventManager.BUNDLE_EXECUTOR_EVENT_MANAGER_SERVICE);
				if (null != serviceName) {
					bundleExecManagerExtender = trackExtender(bundle, BundleExecutorEventManager.class.getName(), serviceName);
				}
			}
			serviceName = headers.get(BundleCommand.BUNDLE_COMMAND_SERVICE);
			if (null != serviceName) {
				bundleCommandExtender = trackExtender(bundle, BundleCommand.class.getName(), new BundleCommandServiceFactory());
				bundleRegionExtender = trackExtender(bundle, BundleRegion.class.getName(), new BundleRegionServiceFactory());
				bundleTransitionExtender = trackExtender(bundle, BundleTransition.class.getName(), new BundleTransitionServiceFactory());
				bundleProjectCandidatesExtender = trackExtender(bundle, BundleProjectCandidates.class.getName(), new BundleProjectCandidatesServiceFactory());
				bundleProjectMetaExtender = trackExtender(bundle, BundleProjectMeta.class.getName(), new BundleProjectMetaServiceFactory());
			}
			serviceName = headers.get(CommandOptions.COMMAND_OPTIONS_SERVICE);
			if (null != serviceName) {
				commandOptionsExtender = trackExtender(bundle, CommandOptions.class.getName(), serviceName);
			}
			serviceName = headers.get(MessageOptions.MESSAGE_OPTIONS_SERVICE);
			if (null != serviceName) {
				messageOptionsExtender = trackExtender(bundle, MessageOptions.class.getName(),serviceName);
			}
			serviceName = headers.get(DependencyDialog.DEPENDENCY_DIALOG_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, DependencyDialog.class.getName(), serviceName);
			}
			serviceName = headers.get(BundleLogView.BUNDLE_LOG_VIEW_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, BundleLogView.class.getName(), serviceName);
			}
			serviceName = headers.get(BundleConsoleFactory.BUNDLE_CONSOLE_SERVICE);
			if (null != serviceName) {
				trackExtender(bundle, BundleConsoleFactory.class.getName(), serviceName);
			}
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, Msg.EXTENDER_NOT_AVAILABLE_WARN, e),
					StatusManager.LOG);
		}
		return super.addingBundle(bundle, event);
	}
}
