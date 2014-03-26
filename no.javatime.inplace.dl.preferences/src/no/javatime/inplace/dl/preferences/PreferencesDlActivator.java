package no.javatime.inplace.dl.preferences;

import no.javatime.inplace.dl.preferences.impl.CommandOptionsImpl;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Service interface for access and flushing of commands and manifest options
 * <p>
 * The bundle uses the OSGi preference store for storage and access of the options. 
 * <p>
 * The provided service interface for options is also implemented using DS, which is not in use. 
 * To enable DS remove the service registration/unregistration in the start/stop method and add the 
 * OSGI-INFO/optins.xml file to the Service-Component header in the META-INF/manifest.mf
 */
public class PreferencesDlActivator implements BundleActivator {

	private static PreferencesDlActivator thisBundle = null;
	private static BundleContext context;
	private ServiceRegistration<?> registration;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		thisBundle = this;
		PreferencesDlActivator.context = context;
		CommandOptionsImpl commandOptImpl = new CommandOptionsImpl();
		registration = context.registerService(CommandOptions.class.getName(), commandOptImpl, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		PreferencesDlActivator.context = null;
		thisBundle = null;
	}

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static PreferencesDlActivator getThisBundle() {
		return thisBundle;
	}
}
