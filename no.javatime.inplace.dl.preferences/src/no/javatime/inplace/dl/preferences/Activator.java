package no.javatime.inplace.dl.preferences;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Service interface for loading and storing commands, manifest and dependency
 * options
 * <p>
 * The bundle uses the OSGi preference store for storage and access of the
 * options.
 * <p>
 * How to use DS instead of explicit service registration of the option
 * services:
 * <p>
 * The provided service interfaces for command (including manifest options) and
 * dependency options along with the preference store are also implemented using
 * DS, which is not in use. To enable DS remove the Register/UnRegister of
 * services (commandOptions, preferenceStore and dependencyOptions) in the
 * start/stop method and add the OSGI-INFO/optins.xml file to the
 * Service-Component header in the META-INF/manifest.mf
 */
public class Activator implements BundleActivator {

	private static Activator activator = null;
	private static BundleContext context;
	// Register and track extenders and get and unget services provided by this and other bundles
	private static ExtenderTracker extenderTracker;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		activator = this;
		Activator.context = context;
		extenderTracker = new ExtenderTracker(context, Bundle.ACTIVE, null);
		extenderTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		extenderTracker.close();
		extenderTracker = null;
		Activator.context = null;
		activator = null;
	}

	/**
	 * Get the bundle context
	 * 
	 * @return the bundle context
	 */
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the shared bundle object
	 * 
	 * @return the shared bundle object
	 */
	public static Activator getInstance() {
		return activator;
	}
}
