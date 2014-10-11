package no.javatime.inplace.pl.preferences;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.pl.preferences.msg.Msg;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class PreferencePlActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "no.javatime.inplace.pl.preferences"; //$NON-NLS-1$

	// The shared instance
	private static PreferencePlActivator plugin;
	private static BundleContext context;

	private Extension<CommandOptions> commandOptions;

	/**
	 * The constructor
	 */
	public PreferencePlActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		PreferencePlActivator.context = context;
		commandOptions = Extenders.getExtension(CommandOptions.class.getName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		PreferencePlActivator.context = null;
	}

	/**
	 * Get the service object for the command and manifest options
	 * @return the command options service object
	 * @throws IllegalStateException if the service object is null
	 */
	public CommandOptions getOptionsService() throws IllegalStateException {

		CommandOptions cmdOpt = commandOptions.getService();
		if (null == cmdOpt) {
			throw new IllegalStateException(NLS.bind(Msg.STORE_SERVICE_EXCEPTION,  CommandOptions.class.getName()));
		}
		return cmdOpt;
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
	public static PreferencePlActivator getDefault() {
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
