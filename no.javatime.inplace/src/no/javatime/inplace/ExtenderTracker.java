package no.javatime.inplace;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.BundleExecutorServiceFactory;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
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

import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Registers services provided by other bundles
 */

class ExtenderTracker extends ExtenderBundleTracker {

	Bundle thisBundle;
	Extender<BundleExecutorEventManager> bundleExecutorEventManagerExtender;
	Extender<ResourceState> resourceStateExtender;
	Extender<BundleCommand> bundleCommandExtender;
	Extender<BundleRegion> bundleRegionExtender;
	Extender<BundleTransition> bundleTransitionExtender;
	Extender<BundleProjectCandidates> bundleProjectCandidatesExtender;
	Extender<BundleProjectMeta> bundleProjectMetaExtender;
	Extender<CommandOptions> commandOptionsExtender;
	Extender<MessageOptions> messageOptionsExtender;
	Extender<DependencyOptions> dependencyOptionsExtender;
	Extender<BundleLog> bundleLogExtender;
	Extender<BundleConsoleFactory> bundleConsoleFactoryExtender;
	
	public ExtenderTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
		thisBundle = context.getBundle();
	}
	
	/**
	 * Register and track extenders hosted by this bundle.
	 * <p>
	 * Tracked extenders are actually tracked after the first invocation of {@code #addingBundle(Bundle, BundleEvent)}
	 */
	public void trackOwn() {

		try {
			Dictionary<String, String> headers =  thisBundle.getHeaders();
//			track(thisBundle, ActivateProject.class.getName(), new BundleExecutorServiceFactory(headers.get(ActivateProject.ACTIVATE_PROJECT_SERVICE)));
//			track(thisBundle, ActivateBundle.class.getName(), new BundleExecutorServiceFactory(headers.get(ActivateBundle.ACTIVATE_BUNDLE_SERVICE)));
//			track(thisBundle, AddBundleProject.class.getName(), new BundleExecutorServiceFactory(headers.get(AddBundleProject.ADD_BUNDLE_PROJECT_SERVICE)));
//			track(thisBundle, RemoveBundleProject.class.getName(), new BundleExecutorServiceFactory(headers.get(RemoveBundleProject.REMOVE_BUNDLE_PROJECT_SERVICE)));
//			track(thisBundle, Install.class.getName(), new BundleExecutorServiceFactory(headers.get(Install.INSTALL_BUNDLE_SERVICE)));
//			track(thisBundle, Uninstall.class.getName(), new BundleExecutorServiceFactory(headers.get(Uninstall.UNINSTALL_BUNDLE_SERVICE)));
//			track(thisBundle, Reinstall.class.getName(), new BundleExecutorServiceFactory(headers.get(Reinstall.REINSTALL_BUNDLE_SERVICE)));
//			track(thisBundle, Deactivate.class.getName(), new BundleExecutorServiceFactory(headers.get(Deactivate.DEACTIVATE_BUNDLE_SERVICE)));
//			track(thisBundle, Start.class.getName(), new BundleExecutorServiceFactory(headers.get(Start.START_BUNDLE_SERVICE)));
//			track(thisBundle, Stop.class.getName(), new BundleExecutorServiceFactory(headers.get(Stop.STOP_BUNDLE_SERVICE)));
//			track(thisBundle, Refresh.class.getName(), new BundleExecutorServiceFactory(headers.get(Refresh.REFRESH_BUNDLE_SERVICE)));
				trackExtender(thisBundle, Update.class.getName(), new BundleExecutorServiceFactory(headers.get(Update.UPDATE_BUNDLE_SERVICE)));
//			track(thisBundle, Reset.class.getName(), new BundleExecutorServiceFactory(headers.get(Reset.RESET_BUNDLE_SERVICE)));
//			track(thisBundle, TogglePolicy.class.getName(), new BundleExecutorServiceFactory(headers.get(TogglePolicy.TOGGLE_POLICY_SERVICE)));
//			track(thisBundle, UpdateBundleClassPath.class.getName(), new BundleExecutorServiceFactory(headers.get(UpdateBundleClassPath.UPDATE_BUNDLE_CLASS_PATH_SERVICE)));
//			track(thisBundle, ResourceState.class.getName(), new BundleServiceScopeFactory<ResourceState>(headers.get(ResourceState.RESOURCE_STATE_SERVICE)));
			String serviceName = headers.get(BundleExecutorEventManager.BUNDLE_EXECUTOR_EVENT_MANAGER_SERVICE);
			if (null != serviceName) {
				bundleExecutorEventManagerExtender = trackExtender(thisBundle, BundleExecutorEventManager.class.getName(), serviceName);
				resourceStateExtender = trackExtender(thisBundle, ResourceState.class.getName(), headers.get(ResourceState.RESOURCE_STATE_SERVICE));
			}
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
	}

	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) throws ExtenderException {

		if (!getFilter(bundle) && !bundle.getSymbolicName().startsWith("org.eclipse.ui.console")) {
			return null;
		}		
		try { 
			Dictionary<String, String> headers =  bundle.getHeaders();
			
			String serviceName = headers.get(BundleCommand.BUNDLE_COMMAND_SERVICE);
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
			serviceName = headers.get(DependencyOptions.DEPENDENCY_OPTIONS_SERVICE);
			if (null != serviceName) {
				dependencyOptionsExtender = trackExtender(bundle, DependencyOptions.class.getName(),serviceName);
			}			
			serviceName = bundle.getHeaders().get(BundleLog.BUNDLE_LOG_SERVICE);
			if (null != serviceName) {
				bundleLogExtender = trackExtender(bundle, BundleLog.class.getName(), new BundleServiceScopeFactory<>(serviceName));
			}
			serviceName = headers.get(BundleConsoleFactory.BUNDLE_CONSOLE_SERVICE);
			if (null != serviceName) {
				bundleConsoleFactoryExtender = trackExtender(bundle, BundleConsoleFactory.class.getName(), serviceName);
			}
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}
		return super.addingBundle(bundle, event);
	}
	
//	@Override
//	public void removedBundle(Bundle bundle, BundleEvent event, Collection<Extender<?>> object) {
//
//		if (bundle.equals(context.getBundle())) {
//			if ((event.getType() & (BundleEvent.STOPPING)) == 0) {
//				for (Extender<?> extender : object) {
//					serviceDown(extender);					
//				}
//			}
//		}	else {		
//			if (getTrackingCount() != -1) {
//				Collection<Extender<?>> tracked = getTrackedExtenders();
//				for (Extender<?> extender : object) {
//					if (tracked.contains(extender)) {
//						StatusManager.getManager().handle(
//								new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, bundle, "Reported by UI plugin in removedBundle " + extender.getServiceInterfaceName(), null), StatusManager.LOG);
//					} 
//				}
//			}
//		}
//		super.removedBundle(bundle, event, object);
//	}
//	
//	@Override
//	public void unregistering(Extender<?> extender) {
//
//		serviceDown(extender);
//		super.unregistering(extender);
//	}
//	
//	@Override
//	public void unregistering(Extension<?> extension) {
//
//		if (extension.getTrackingCount() != -1) {
//			extension.closeTrackedService();
//		}
//		super.unregistering(extension);	
//	}
	
	private void serviceDown (Extender<?> extender) {
		
		// Service should not be unregistered while this bundle tracker is open 
		if (getTrackingCount() != -1) {
			try {
				BundleLog log = bundleLogExtender.getService();
				String depAttr = (String) extender.getProperty("dependency");
				if (null != depAttr && depAttr.compareToIgnoreCase("optional") == 0) {
					log.addRoot(StatusCode.WARNING, extender.getOwner(), null, "Optional service going down: " + extender.getServiceInterfaceName());
				} else {
					log.addRoot(StatusCode.ERROR, extender.getOwner(), null, "Required service going down: " + extender.getServiceInterfaceName());				
				}
				log.log();
				bundleLogExtender.ungetService();
			} catch (Exception e) {	
				// The bundle log is the unregistered service
				// TODO Elaborate
				System.err.println("Service going down: " + extender.getServiceInterfaceName());
			}
		}		
	}
}