package no.javatime.inplace.dl.preferences;

import no.javatime.inplace.dl.preferences.impl.CommandOptionsImpl;
import no.javatime.inplace.dl.preferences.impl.DependencyOptionsImpl;
import no.javatime.inplace.dl.preferences.impl.PreferencesStoreImpl;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.PreferencesStore;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Service interface for loading and storing commands, manifest and dependency options
 * <p>
 * The bundle uses the OSGi preference store for storage and access of the options.
 * <p>
 * How to use DS instead of explicit service registration of the option services:
 * <p>
 * The provided service interfaces for command (including manifest options) and dependency options along with the
 * preference store are also implemented using DS, which is not in use. To enable DS remove the Register/UnRegister of
 * services (commandOptions, preferenceStore and dependencyOptions) in the start/stop method and add the
 * OSGI-INFO/optins.xml file to the Service-Component header in the META-INF/manifest.mf
 */
public class PreferencesDlActivator implements BundleActivator {

	private static PreferencesDlActivator thisBundle = null;
	private static BundleContext context;
	private ServiceRegistration<?> commandOptionsRegister;
	private ServiceRegistration<?> preferenceStoreRegister;
	private ServiceRegistration<?> dependencyOptionsRegister;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		thisBundle = this;
		PreferencesDlActivator.context = context;
		CommandOptionsImpl commandOptImpl = new CommandOptionsImpl();
		commandOptionsRegister = context.registerService(CommandOptions.class.getName(), commandOptImpl, null);
		PreferencesStoreImpl preferencesStoreImpl = new PreferencesStoreImpl();
		preferenceStoreRegister = context.registerService(PreferencesStore.class.getName(), preferencesStoreImpl,
				null);
		DependencyOptionsImpl dependencyOptionsImpl = new DependencyOptionsImpl();
		dependencyOptionsRegister = context.registerService(DependencyOptions.class.getName(),
				dependencyOptionsImpl, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		commandOptionsRegister.unregister();
		preferenceStoreRegister.unregister();
		dependencyOptionsRegister.unregister();
		PreferencesDlActivator.context = null;
		thisBundle = null;
	}
	
	/**
	 * Get the bundle context
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
	public static PreferencesDlActivator getThisBundle() {
		return thisBundle;
	}
}
