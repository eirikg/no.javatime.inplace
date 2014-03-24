package no.javatime.inplace.dl.preferences;

import no.javatime.inplace.dl.preferences.impl.CommandOptionsImpl;
import no.javatime.inplace.dl.preferences.impl.CommandOptionsTrackerImpl;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator is not in use. Activate/Deactivate is used for each declarative service
 * in this bundle. To enable use of the this bundle activator add a reference to it in manifest.
 * <p>
 * Consumes and provide access to the OSGi preference store using the service tracker service
 *
 */
public class PreferencesDlActivator implements BundleActivator {

	// OSGi preference store
	private ServiceTracker<PreferencesService, PreferencesService> prefTracker;
	private PreferencesService prefService;
	private static PreferencesDlActivator thisBundle = null;
	private static BundleContext context;
	CommandOptionsTrackerImpl optionsTracker;
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		thisBundle = this;		
		PreferencesDlActivator.context = context;
//		prefTracker = new ServiceTracker<PreferencesService, PreferencesService>
//				(context, PreferencesService.class, null);
//		prefTracker.open();
		
		CommandOptions cmdOpt = new CommandOptionsImpl();
		context.registerService(CommandOptions.class.getName(), cmdOpt, null);
//		optionsTracker = new CommandOptionsTrackerImpl(context);
//		optionsTracker.open();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
//		prefTracker.close();
//		prefTracker = null;
//		prefService = null;
//		optionsTracker.close();
		PreferencesDlActivator.context = null;
		thisBundle = null;
	}
	
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Get the preference store
	 * @return a reference to the OSGi standard preference store interface
	 * @throws Exception 
	 */
	public Preferences getStore() throws RuntimeException {
		prefService = (PreferencesService) prefTracker.getService();
		if (null == prefService) {
			throw new RuntimeException("Failed to get the OSGi prefrence service");
		}
		Preferences preferences = prefService.getSystemPreferences();
		return preferences;
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
