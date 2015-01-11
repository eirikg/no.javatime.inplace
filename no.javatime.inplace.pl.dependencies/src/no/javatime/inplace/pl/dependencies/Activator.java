package no.javatime.inplace.pl.dependencies;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.pl.dependencies.msg.Msg;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "no.javatime.inplace.pl.dependencies"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	// Get the workbench window from UI thread
	private IWorkbenchWindow workBenchWindow = null;
	
	private Extension<DependencyOptions> dependencyOptions;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		dependencyOptions = Extenders.getExtension(DependencyOptions.class.getName());

		// The service (extender) should be registered by the bundle using this extension
		/*
		Bundle bundle = context.getBundle();
		Dictionary<String, String> dictionary = bundle.getHeaders();		
		String depDlgClassName = dictionary.get(DependencyDialog.DEPENDENCY_DIALOG_IMPL);
		ExtenderImpl.register(bundle, DependencyDialog.class, depDlgClassName);
		*/
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public DependencyOptions getDependencyOptionsService() throws InPlaceException {
		
		DependencyOptions dpOpt = dependencyOptions.getService();
		if (null == dpOpt) {
			throw new InPlaceException(NLS.bind(Msg.INVALID_OPTIONS_SERVICE_EXCEPTION, DependencyOptions.class.getName()));			
		}
		return dpOpt;
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
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	public Shell getShell() {
		return Activator.getDefault().getActiveWorkbenchWindow().getShell();
	}
	/**
	 * Returns the active workbench window
	 * @return the active workbench window or null if not found
	 */

	public IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null) {
			return null;
		}
		final IWorkbench workBench = plugin.getWorkbench();
		if (workBench == null) {
			return null;
		}
		workBenchWindow = null;
		getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				workBenchWindow = workBench.getActiveWorkbenchWindow();
			}
		});
		return workBenchWindow;
	}
	/**
	 * Get a valid display
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// May be null if outside the UI thread
		if (display == null) {
			display = Display.getDefault();
		}
		if (display.isDisposed()) {
			return null;
		}
		return display;
	}

}
