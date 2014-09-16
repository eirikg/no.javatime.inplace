package no.javatime.inplace.pl.console;

import java.util.Dictionary;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.pl.console.impl.BundleConsoleFactoryImpl;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.console.view.BundleConsole;
import no.javatime.inplace.region.manager.InPlaceException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "no.javatime.inplace.pl.console"; //$NON-NLS-1$

	/**
	 * Console of the current IDE
	 */
	public static String CONSOLE_NAME = "Bundle Console";

	// The shared instance
	private static Activator plugin;
	private static BundleContext context;
	private BundleConsole bundleConsole = null;
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;
	private Extension<MessageOptions> messageOptions;

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
		Activator.context = context;
		Bundle bundle = context.getBundle();
		Dictionary<String, String> dictionary = bundle.getHeaders();
		String consoleFactoryName = dictionary.get(BundleConsoleFactory.BUNDLE_CONSOLE_HEADER);
		Extender.register(bundle, BundleConsoleFactory.class, consoleFactoryName);

		messageOptions = new Extension<>(MessageOptions.class);
		bundleConsole = BundleConsoleFactoryImpl.findConsole(CONSOLE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public BundleConsole getBundleConsole() {
		return bundleConsole;
	}
	
	public MessageOptions getMsgOpt() throws InPlaceException {

		MessageOptions msgOpt = messageOptions.getService();
		if (null == msgOpt) {
			throw new InPlaceException("invalid_service", MessageOptions.class.getName());
		}
		return msgOpt;
	}

	/**
	 * Return the bundle context
	 * 
	 * @return the shared bundle context
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
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window or null if not found
	 */
	private IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null) {
			return null;
		}
		final IWorkbench workBench = plugin.getWorkbench();
		if (workBench == null) {
			return null;
		}
		workBenchWindow = null;
		getDisplay().syncExec(new Runnable() {
			public void run() {
				workBenchWindow = workBench.getActiveWorkbenchWindow();
			}
		});
		return workBenchWindow;
	}

	/**
	 * Returns the active page
	 * 
	 * @return the active page or null if not found
	 */
	public IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		return activeWorkbenchWindow.getActivePage();
	}


	/**
	 * Returns the active or the default display if the active display is null
	 * 
	 * @return the active or default display or null if the display is disposed
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null) {
			display = Display.getDefault();
		}
		if (display.isDisposed()) {
			return null;
		}
		return display;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

}
