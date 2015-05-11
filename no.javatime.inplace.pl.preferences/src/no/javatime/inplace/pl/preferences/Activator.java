package no.javatime.inplace.pl.preferences;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "no.javatime.inplace.pl.preferences"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private static BundleContext context;
	private static Bundle bundle;

	// Register (extend) services for use facilitated by other bundles
	private static ExtenderTracker extenderBundleTracker;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		bundle = context.getBundle();
		Activator.context = context;
		extenderBundleTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
		extenderBundleTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		extenderBundleTracker.close();
		super.stop(context);
		plugin = null;
		Activator.context = null;
	}

	/**
	 * Get the service object for the command and manifest options
	 * @return the command options service object
	 * @throws IllegalStateException if the service object is null
	 */
	public CommandOptions getOptionsService() throws IllegalStateException {

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

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
