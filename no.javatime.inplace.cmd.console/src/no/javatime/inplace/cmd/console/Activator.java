package no.javatime.inplace.cmd.console;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	// Register (extend) services for use facilitated by other bundles
	private static ExtenderTracker extenderBundleTracker;

	private static Extension<BundleProjectCandidates> candidatesExtension;
	private static BundleProjectCandidates candidates;

	private static Extension<CommandOptions> commandOptionsExtension;
	private static CommandOptions commandOptions;

	private static Extension<BundleRegion> regionExtension;
	private static BundleRegion region;

	private static Extension<BundleProjectMeta> projectMetaExtension;
	private static BundleProjectMeta projectMeta;

	private static Extension<BundleTransition> transitionExtension;
	private static BundleTransition transition;

	private static BundleContext context;

	public void start(BundleContext context) throws Exception {

		Activator.context = context;

		extenderBundleTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
		extenderBundleTracker.open();

		candidatesExtension = Extenders.getExtension(BundleProjectCandidates.class.getName());
		candidates = candidatesExtension.getTrackedService();

		commandOptionsExtension = Extenders.getExtension(CommandOptions.class.getName());
		commandOptions = commandOptionsExtension.getTrackedService();

		regionExtension = Extenders.getExtension(BundleRegion.class.getName());
		region = regionExtension.getTrackedService();

		projectMetaExtension = Extenders.getExtension(BundleProjectMeta.class.getName());
		projectMeta = projectMetaExtension.getTrackedService();

		transitionExtension = Extenders.getExtension(BundleTransition.class.getName());
		transition = transitionExtension.getTrackedService();
		
		// Not using DS. component.xml not referenced from manifest
		Extenders.register(context.getBundle(), CommandProvider.class.getName(),
				new BundleProjectCommandProvider(), null);
	}

	public void stop(BundleContext context) throws Exception {

		candidatesExtension.closeTrackedService();
		commandOptionsExtension.closeTrackedService();
		regionExtension.closeTrackedService();
		projectMetaExtension.closeTrackedService();
		transitionExtension.closeTrackedService();
		extenderBundleTracker.close();
		Activator.context = null;
	}

	public static BundleProjectCandidates getCandidatesService() throws ExtenderException {
		return candidates;
	}

	public static CommandOptions getCmdOptionsService() throws ExtenderException {
		return commandOptions;
	}

	public static BundleRegion getRegionService() throws ExtenderException {
		return region;
	}

	public static BundleProjectMeta getMetaService() throws ExtenderException {
		return projectMeta;
	}

	public static BundleTransition getTransitionService() throws ExtenderException {
		return transition;
	}

	/**
	 * The context for interacting with the FrameWork
	 * 
	 * @return the bundle execution context within the FrameWork
	 */
	public static BundleContext getContext() {
		return context;
	}
}
