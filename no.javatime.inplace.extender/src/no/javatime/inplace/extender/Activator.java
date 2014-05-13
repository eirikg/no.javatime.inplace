package no.javatime.inplace.extender;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.ExtenderBundleTracker;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Activator implements BundleActivator {

	private static Activator plugin;
	private static BundleContext context;
	// Don't know the interface to extend yet. Can be any interface 
	private BundleTracker<Extender<?>> extenderBundleTracker;
	private BundleTrackerCustomizer<Extender<?>> extenderBundleTrackerCustomizer;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		extenderBundleTrackerCustomizer = new ExtenderBundleTracker();
		int trackStates = Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED | Bundle.UNINSTALLED;
		extenderBundleTracker = new BundleTracker<Extender<?>>(context, trackStates, extenderBundleTrackerCustomizer);
		extenderBundleTracker.open();
}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		extenderBundleTracker.close();
		extenderBundleTracker = null;
		Activator.context = null;
		plugin = null;
	}
	
	public BundleTracker<Extender<?>> getExtenderBundleTracker() {
		return extenderBundleTracker;
	}

	/**
	 * The context for interacting with the FrameWork
	 * 
	 * @return the bundle execution context within the FrameWork
	 */
	public static BundleContext getContext() {
		return context;
	}
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
