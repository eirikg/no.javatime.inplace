package no.javatime.inplace.pl.preferences.service;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.ManifestOptions;
import no.javatime.inplace.dl.preferences.intface.PreferencesStore;

public class OptionsService {

	private static CommandOptions commandOptionsStoreService;
	private static ManifestOptions outputFolderStoreService;
	private static PreferencesStore preferencesStoreService;

	/**
	 * Get the interface for command options
	 * 
	 * @return the interface for command options
	 */
	public static CommandOptions getCommandOptions() {
		return commandOptionsStoreService;
	}

	/**
	 * Get the interface for default output folder options
	 * 
	 * @return the interface for default output folder options
	 */
	public static ManifestOptions getOutputFolderOptions() {
		return outputFolderStoreService;
	}

	/**
	 * Get the interface for the native preference store
	 * 
	 * @return the interface for the preference store
	 */
	public static PreferencesStore getPreferenceStore() {
		return preferencesStoreService;
	}
	/**
	 * Set/bind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void setCommandOptions(CommandOptions service) {
		commandOptionsStoreService = service;
	}

	/**
	 * Set/bind of object implementing the specified interface
	 * 
	 * @param service object implementing the specified interface
	 */
	public synchronized void setOutputFolderOptions(ManifestOptions service) {
		outputFolderStoreService = service;
	}

	/**
	 * Set/bind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void setPreferencesStore(PreferencesStore service) {
		preferencesStoreService = service;
	}

	/**
	 * Unset/unbind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void unsetCommandOptions(CommandOptions service) {
		// May be the same - if same imp. class is used - as other interfaces
		// (e.g. output folder options)
		if (getCommandOptions() == service) {
			commandOptionsStoreService = null;
		}
	}

	/**
	 * Unset/unbind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void unsetOutputFolderOptions(ManifestOptions service) {
		// May be the same - if same imp. class is used - as other interfaces
		// (e.g. command options)
		if (getOutputFolderOptions() == service) {
			outputFolderStoreService = null;
		}
	}

	/**
	 * Unset/unbind of object implementing the specified interface
	 * 
	 * @param service object implementing this interface
	 */
	public synchronized void unsetPreferencesStore(PreferencesStore service) {
		if (getPreferenceStore() == service) {
			preferencesStoreService = null;
		}
	}
}
