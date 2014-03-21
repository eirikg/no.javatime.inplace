package no.javatime.inplace.dl.preferences.service;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

public class PreferencesServiceStore {

	private PreferencesService preferencesService;
	private static Preferences sysPrefs;
	
	/**
	 * Activate method from Declarative service
	 */
	protected void activate(ComponentContext context) {
		sysPrefs = preferencesService.getSystemPreferences();
	}

	/**
	 * Deactivate method from Declarative Service
	 */
	protected void deactivate(ComponentContext context) {
		sysPrefs = null;
	}

	/**
	 * Get the interface for system preferences
	 * 
	 * @return the interface for system preferences
	 */
	public static Preferences getPreferences() {
		return sysPrefs;
	}

	/**
	 * Set/bind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void setPreferences(PreferencesService service) {
		preferencesService = service;
	}


	/**
	 * Unset/unbind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void unsetPreferences(PreferencesService service) {
		if (preferencesService == service) {
			preferencesService = null;
		}
	}

}
