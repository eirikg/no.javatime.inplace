package no.javatime.inplace.region;

import java.util.Collection;
import java.util.Dictionary;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.project.BundleProjectCandidatesImpl;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
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

public class ExtenderTracker extends ExtenderBundleTracker {

	Bundle thisBundle;
	Extender<BundleCommand> bundleCommandExtender;
	Extender<BundleRegion> bundleRegionExtender;
	Extender<BundleTransition> bundleTransitionExtender;
	Extender<BundleProjectCandidates> bundleProjectCandidatesExtender;
	Extender<BundleProjectMeta> bundleProjectMetaExtender;

	Extender<CommandOptions> commandOptionsExtender;
	Extender<MessageOptions> messageOptionsExtender;
	Extender<DependencyOptions> dependencyOptionsExtender;

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
			bundleCommandExtender = trackExtender(thisBundle, BundleCommand.class.getName(), BundleCommandImpl.INSTANCE, null);
			bundleRegionExtender = trackExtender(thisBundle, BundleRegion.class.getName(), WorkspaceRegionImpl.INSTANCE, null);
			bundleTransitionExtender = trackExtender(thisBundle, BundleTransition.class.getName(), BundleTransitionImpl.INSTANCE, null);
			bundleProjectCandidatesExtender = trackExtender(thisBundle, BundleProjectCandidates.class.getName(), BundleProjectCandidatesImpl.INSTANCE, null);
			bundleProjectMetaExtender = trackExtender(thisBundle, BundleProjectMeta.class.getName(), BundleProjectMetaImpl.INSTANCE, null);
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event) throws ExtenderException {
		if (((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0)) {
			return null;
		}
		if (!getFilter(bundle)) {
			return null;
		}

		try {
			Dictionary<String, String> headers =  bundle.getHeaders();

			String serviceName = headers.get(CommandOptions.COMMAND_OPTIONS_SERVICE);
			if (null != serviceName) {
				commandOptionsExtender = trackExtender(bundle, CommandOptions.class.getName(), serviceName, null);
			}
			serviceName = headers.get(MessageOptions.MESSAGE_OPTIONS_SERVICE);
			if (null != serviceName) {
				messageOptionsExtender = trackExtender(bundle, MessageOptions.class.getName(),serviceName, null);
			}
			serviceName = headers.get(DependencyOptions.DEPENDENCY_OPTIONS_SERVICE);
			if (null != serviceName) {
				dependencyOptionsExtender = trackExtender(bundle, DependencyOptions.class.getName(),serviceName, null);
			}			
		} catch (ExtenderException | IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
		return super.addingBundle(bundle, event);
	}

	@Override
	public void unregistering(Extender<?> extender) {

		serviceDown(extender);
		super.unregistering(extender);
	}

	@Override
	public void unregistering(Extension<?> extension) {

		if (extension.getTrackingCount() != -1) {
			extension.closeTrackedService();
		}
		super.unregistering(extension);
	}

	private void serviceDown (Extender<?> extender) {

		// Service should not be unregistered while this bundle tracker is open
		if (getTrackingCount() != -1) {
			try {
			} catch (Exception e) {
				// The bundle log is the unregistered service
				// TODO Elaborate
				System.err.println("Service going down: " + extender.getServiceInterfaceName());
			}
		}
	}
}
