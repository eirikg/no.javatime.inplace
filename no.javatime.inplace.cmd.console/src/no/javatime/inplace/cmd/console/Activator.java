package no.javatime.inplace.cmd.console;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static Extension<BundleProjectCandidates> candidates;
	private static Extension<CommandOptions> cmdOptions;
	private static Extension<BundleRegion> region;
	private static Extension<BundleProjectMeta> meta;
	private static Extension<BundleTransition> transition;
	private static BundleContext context;

	public void start(BundleContext context) throws Exception {

		Activator.context = context;
		candidates = Extenders.getExtension(BundleProjectCandidates.class.getName()); 
		cmdOptions = Extenders.getExtension(CommandOptions.class.getName()); 
		region = Extenders.getExtension(BundleRegion.class.getName()); 
		meta = Extenders.getExtension(BundleProjectMeta.class.getName()); 
		transition = Extenders.getExtension(BundleTransition.class.getName()); 
		Extenders.register(context.getBundle(), "org.eclipse.osgi.framework.console.CommandProvider", new BundleProjectCommandProvider(), null);
	}

	public void stop(BundleContext context) throws Exception {
		Activator.context = null;
	}

	public static BundleProjectCandidates getCandidatesService() throws ExtenderException {
		return candidates.getService();
	}

	public static CommandOptions getCmdOptionsService() throws ExtenderException {
		return cmdOptions.getService();
	}

	public static BundleRegion getRegionService() throws ExtenderException {
		return region.getService();
	}

	public static BundleProjectMeta getMetaService() throws ExtenderException {
		return meta.getService();
	}

	public static BundleTransition getTransitionService() throws ExtenderException {
		return transition.getService();
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
