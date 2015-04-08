package no.javatime.inplace.ui;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.BundleExecutorServiceFactory;
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
import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
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
				track(bundle, ActivateProject.class.getName(), new BundleExecutorServiceFactory(serviceName));
				track(bundle, Install.class.getName(), new BundleExecutorServiceFactory(headers.get(Install.INSTALL_BUNDLE_SERVICE)));
				track(bundle, Deactivate.class.getName(), new BundleExecutorServiceFactory(headers.get(Deactivate.DEACTIVATE_BUNDLE_SERVICE)));
				track(bundle, Start.class.getName(), new BundleExecutorServiceFactory(headers.get(Start.START_BUNDLE_SERVICE)));
				track(bundle, Stop.class.getName(), new BundleExecutorServiceFactory(headers.get(Stop.STOP_BUNDLE_SERVICE)));
				track(bundle, Refresh.class.getName(), new BundleExecutorServiceFactory(headers.get(Refresh.REFRESH_BUNDLE_SERVICE)));
				track(bundle, Update.class.getName(), new BundleExecutorServiceFactory(headers.get(Update.UPDATE_BUNDLE_SERVICE)));
				track(bundle, Reset.class.getName(), new BundleExecutorServiceFactory(headers.get(Reset.RESET_BUNDLE_SERVICE)));
				track(bundle, TogglePolicy.class.getName(), new BundleExecutorServiceFactory(headers.get(TogglePolicy.TOGGLE_POLICY_SERVICE)));
				track(bundle, UpdateBundleClassPath.class.getName(), new BundleExecutorServiceFactory(headers.get(UpdateBundleClassPath.UPDATE_BUNDLE_CLASS_PATH_SERVICE)));
				track(bundle, ResourceState.class.getName(), new BundleServiceScopeFactory<ResourceState>(headers.get(ResourceState.RESOURCE_STATE_SERVICE)));
				track(bundle, BundleExecutorEventManager.class.getName(), new BundleServiceScopeFactory<ResourceState>(headers.get(BundleExecutorEventManager.BUNDLE_EXECUTOR_EVENT_MANAGER_SERVICE)));
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
			serviceName = headers.get(MessageOptions.MESSAGE_OPTIONS_SERVICE);
			if (null != serviceName) {
				track(bundle, MessageOptions.class.getName(),serviceName);
			}
			serviceName = headers.get(DependencyDialog.DEPENDENCY_DIALOG_SERVICE);
			if (null != serviceName) {
				track(bundle, DependencyDialog.class.getName(), serviceName);
			}
			serviceName = headers.get(BundleLogView.BUNDLE_LOG_VIEW_SERVICE);
			if (null != serviceName) {
				track(bundle, BundleLogView.class.getName(), serviceName);
			}
			serviceName = headers.get(BundleConsoleFactory.BUNDLE_CONSOLE_SERVICE);
			if (null != serviceName) {
				track(bundle, BundleConsoleFactory.class.getName(), serviceName);
			}
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, Msg.EXTENDER_NOT_AVAILABLE, e),
					StatusManager.LOG);
		}
		return super.addingBundle(bundle, event);
	}
}
