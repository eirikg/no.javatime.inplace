package no.javatime.inplace.cmd.console;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	// Register (extend) services for use facilitated by other bundles
	private static ExtenderTracker extenderBundleTracker;


	public static ExtenderTracker getExtenderBundleTracker() {

		return extenderBundleTracker;
	}


	private static BundleContext context;
	private static Bundle bundle;

	public void start(BundleContext context) throws Exception {

		Activator.context = context;
		bundle = context.getBundle();
		extenderBundleTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
		extenderBundleTracker.trackOwn();
		extenderBundleTracker.open();

		// Not using DS. component.xml not referenced from manifest
		//		Extenders.register(context.getBundle(), CommandProvider.class.getName(),
		//				new BundleProjectCommandProvider(), null);
	}

	public void stop(BundleContext context) throws Exception {

		extenderBundleTracker.close();
		Activator.context = null;
	}

	public static BundleRegion getRegionService() throws ExtenderException {

		return extenderBundleTracker.bundleRegionExtender.getService(bundle);
	}

	public static BundleTransition getTransitionService() throws ExtenderException {

		return getRegionService().getTransitionService(bundle);
	}

	public static BundleProjectCandidates getCandidatesService() throws ExtenderException {

		return getRegionService().getCanidatesService(bundle);
	}


	public static BundleProjectMeta getMetaService() throws ExtenderException {

		return getRegionService().getMetaService(bundle);
	}

	public static CommandOptions getCommandOptionsService() throws ExtenderException {

		return extenderBundleTracker.commandOptionsExtender.getService(bundle);
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
