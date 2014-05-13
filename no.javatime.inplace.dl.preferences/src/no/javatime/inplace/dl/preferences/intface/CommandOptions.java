package no.javatime.inplace.dl.preferences.intface;

/**
 * Service interface for access and flushing of commands and manifest (extended interface) options
 */
public interface CommandOptions extends ManifestOptions {
	
	public final static String COMMAND_OPTIONS_HEADER = "Command-Options";

	public static final String IS_UPDATE_ON_BUILD = "isUpdateOnBuild";
	public static final String IS_DEACTIVATE_ON_EXIT = "isDeactivateOnExit";
	public static final String TIMEOUT_SECONDS = "timeoutSeconds";
	public static final String DEFAULT_TIMEOUT_SECONDS = "defaultTimeoutSeconds";
	public static final String IS_TIMEOUT = "isTimeout";
	public static final String IS_MANUAL_TERMINATE = "isManualTerminate";
	public static final String IS_DEACTIVATE_ON_TERMINATE = "isDeactivateOnTerminate";
	public static final String IS_REFRESH_ON_UPDATE = "isRefreshOnUpdate";
	public static final String IS_AUTO_HANDLE_EXTERNAL_COMMANDS = "isAutoHandleExternalCommands";
	public static final String IS_ALLOW_UI_CONTRIBUTIONS = "isAllowUIContributions";

	/**
	 * Get option for allowing UI contributions using extensions
	 * 
	 * @return true if UI contributions is allowed, otherwise false.
	 */
	public abstract boolean isAllowUIContributions();

	/**
	 * Get default option for allowing UI contributions using extensions
	 * 
	 * @return true if UI contributions using extensions should be allowed , otherwise false.
	 */
	public abstract boolean getDefaultIsAllowUIContributions();

	/**
	 * Set if UI contributions using extensions should be allowed
	 * 
	 * @param automatic true to allow UI contributions using extensions and false to not
	 */
	public abstract void setIsAllowUIContributions(boolean contributions);

	/**
	 * Get option for handling external bundle commands
	 * 
	 * @return true if external commands are handled automatically, otherwise false.
	 */
	public abstract boolean isAutoHandleExternalCommands();

	/**
	 * Get default option for handling external bundle commands
	 * 
	 * @return true if activation policy should be set to eager on activation , otherwise false.
	 */
	public abstract boolean getDefaultIsAutoHandleExternalCommands();

	/**
	 * Set if external bundle commands should be handled automatically
	 * 
	 * @param automatic true to handle external commands automatically and false to not
	 */
	public abstract void setIsAutoHandleExternalCommands(boolean automatic);

	/**
	 * Should bundles be refreshed right after they are updated
	 * 
	 * @return true if refresh after update, otherwise false.
	 */
	public abstract boolean isRefreshOnUpdate();

	/**
	 * Get default option for refresh bundle after update
	 * 
	 * @return true if default is refresh after update, otherwise false.
	 */
	public abstract boolean getDefaultIsRefreshOnUpdate();

	/**
	 * Set whether bundles should be refreshed after they are updated
	 * 
	 * @param refresh true to refresh after update and false to not
	 */
	public abstract void setIsRefreshOnUpdate(boolean refresh);

	/**
	 * Should bundles be updated right after they are built
	 * 
	 * @return true if update after build, otherwise false.
	 */
	public abstract boolean isUpdateOnBuild();

	/**
	 * Get default option for update bundle after build
	 * 
	 * @return true if default is update after build, otherwise false.
	 */
	public abstract boolean getDefaultIsUpdateOnBuild();

	/**
	 * Set whether bundles should be updated after they are built
	 * 
	 * @param update true to update after build and false to not
	 */
	public abstract void setIsUpdateOnBuild(boolean update);

	/**
	 * Check for enabling/disabling the timeout functionality in Start and Stop methods
	 * 
	 * @return true if the timeout functionality is enabled in Start and Stop methods. False if this functionality is
	 *         disabled.
	 */
	public abstract boolean isTimeOut();

	/**
	 * Get the default for enabling/disabling timeout in Start and Stop methods
	 * 
	 * @return true if the default timeout functionality is enabled in Start and Stop methods and false if not
	 */
	public abstract boolean getDefaultIsTimeOut();
	
	/**
	 * Set value determining if Start and Stop should be manually stopped 
	 * 
	 * @param terminate true to force stopping running start and stop methods. Otherwise false
	 */
	public abstract void setIsManualTerminate(boolean terminate);

	/**
	 * Get value determining if Start and Stop should be manually stopped 
	 * 
	 * @return True if force termination of start and stop methods. Otherwise false
	 */
	public abstract boolean isManualTerminate();

	/**
	 * Get default value determining if Start and Stop should be manually stopped
	 *  
	 * @return True if default is to force termination of start and stop methods. Otherwise false
	 */
	public abstract boolean getDefaultIsManualTerminate();
	
	/**
	 * Enable or disable the timeout functionality in Start and Stop methods
	 * 
	 * @param timeOut true to enable the timeout function i Start and Stop methods. False to disable this functionality.
	 */
	public abstract void setIsTimeOut(boolean timeOut);

	/**
	 * Get thread timeout value in seconds for Start and Stop methods in bundles.
	 * 
	 * @return the timeout for Start and Stop methods in bundles
	 */
	public abstract int getTimeout();

	/**
	 * Default thread timeout value in seconds for Start and Stop methods in bundles. Default is 5000 ms if not set as a
	 * configuration parameter using the "equinox.statechange.timeout" configuration setting
	 * 
	 * @return the default timeout value for the "equinox.statechange.timeout" setting
	 */
	public abstract int getDeafultTimeout();

	/**
	 * Thread timeout value for activating bundles at Framework startup. Default is 5000 ms. See the
	 * "equinox.statechange.timeout" configuration setting
	 * 
	 * @return the default timeout value for the "equinox.statechange.timeout" setting
	 */
	public abstract int getStateChangeWait();

	/**
	 * Set the time in seconds as the time to wait before returning from the Start and Stop methods in a bundle.
	 * 
	 * @param seconds time to wait in seconds before returning from the Start and Stop methods in a bundle
	 */
	public abstract void setTimeOut(int seconds);

	/**
	 * Set the default timeout interval in seconds for this session only. At restart of the Framework the default equinox
	 * state change default timeout is used.
	 * <p>
	 * To permanently change the default timeout setting use "equinox.statechange.timeout" configuration setting *
	 * 
	 * @param seconds timeout in seconds for start and stop methods
	 */
	public abstract void setDefaultTimeout(int seconds);

	/**
	 * Check whether all bundles is going to be deactivated when the Framework shuts down
	 * 
	 * @return true if deactivate on exit and false if not
	 */
	public abstract boolean isDeactivateOnExit();

	/**
	 * Get default of whether all bundles is going to be deactivated when the Framework shuts down
	 * 
	 * @return true if default is deactivate on exit and false if not
	 */
	public abstract boolean getDefaultIsDeactivateOnExit();

	/**
	 * Set whether bundle should be deactivated when the Framework shuts down
	 * 
	 * @param deactivate true to deactivate and false to not deactivate
	 */
	public abstract void setIsDeactivateOnExit(boolean deactivate);

	/**
	 * Check whether a bundle is going to be deactivated when the bundle task is terminated
	 * 
	 * @return true if deactivate on terminate and false if not
	 */
	public abstract boolean isDeactivateOnTerminate();

	/**
	 * Get default of whether a bundle is going to be deactivated when the bundle task is terminated
	 * 
	 * @return true if default is deactivate on terminate and false if not
	 */
	public abstract boolean getDefaultIsDeactivateOnTerminate();

	/**
	 * Set whether all bundles should be deactivated when the bundle task is terminated
	 * 
	 * @param deactivate true to deactivate and false to not deactivate
	 */
	public abstract void setIsDeactivateOnTerminate(boolean deactivate);
}
