package no.javatime.inplace.pl.console;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.pl.console.impl.BundleConsoleFactoryImpl;
import no.javatime.inplace.pl.console.msg.Msg;
import no.javatime.inplace.pl.console.view.BundleConsole;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements ServiceListener {

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
	private Extension<MessageOptions> messageOptionsExtension;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		messageOptionsExtension = Extenders.getExtension(MessageOptions.class.getName());
		Filter filter=context.createFilter("(" + Constants.OBJECTCLASS + "=" + ConsoleSession.class.getName()+ ")");
		context.addServiceListener(this, filter.toString());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		context.removeServiceListener(this);
		messageOptionsExtension.closeTrackedService();
		plugin = null;
		super.stop(context);
	}

	public BundleConsole getBundleConsole() {
		if (null == bundleConsole) {
			bundleConsole = BundleConsoleFactoryImpl.findConsole(CONSOLE_NAME);
		}
		return bundleConsole;
	}

	public MessageOptions getMessageOptions() throws ExtenderException {

		return messageOptionsExtension.getTrackedService();
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

	/**
	 * Lazy start of the bundle command provider until the OSGi console opens
	 */
	@Override
	public void serviceChanged(ServiceEvent event) {
		final ServiceReference<?> sr = (ServiceReference<?>) event.getServiceReference();

		switch (event.getType()) {
		case ServiceEvent.REGISTERED:
			if (Activator.getContext().getService(sr) instanceof ConsoleSession) {
				Bundle[] bundles = Activator.getContext().getBundles();
				for (int i = 0; i < bundles.length; i++) {
					if ((bundles[i].getState() & (Bundle.RESOLVED)) != 0) {					
						String provider = bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
						if (null != provider && provider.startsWith("Bundle Project")) {
							try {
								bundles[i].start(Bundle.START_TRANSIENT);
							} catch (BundleException | IllegalStateException | SecurityException e) {
								String msg = NLS.bind(Msg.CMD_PROVIDER_NOT_STARTED_WARN, bundles[i].getSymbolicName());
								IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e);
								StatusManager.getManager().handle(status, StatusManager.LOG);
							}					
						}
					}
				}
				// Direct lookup of bundle
//				Bundle bundle = Platform.getBundle("no.javatime.inplace.cmd.console");
//				// String symbolicName = bundle.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
//				if (null != bundle && (bundle.getState() & (Bundle.RESOLVED)) != 0) {
//					try {
//						bundle.start(Bundle.START_TRANSIENT);
//					} catch (BundleException | IllegalStateException | SecurityException e) {
//						String msg = NLS.bind(Msg.CMD_PROVIDER_NOT_STARTED_WARN, bundle.getSymbolicName());
//						IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e);
//					StatusManager.getManager().handle(status, StatusManager.LOG);
//					}
//				}
			}
			break;
		}
	}
}
