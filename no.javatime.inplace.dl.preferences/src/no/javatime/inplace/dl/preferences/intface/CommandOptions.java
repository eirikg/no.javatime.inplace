package no.javatime.inplace.dl.preferences.intface;

/**
 * Service interface for access and flushing of commands and manifest (extended interface) options
 */
public interface CommandOptions extends ManifestOptions {

	public final static String COMMAND_OPTIONS_SERVICE = "Command-Options-Service";

	public static final String IS_UPDATE_ON_BUILD = "isUpdateOnBuild";
	public static final String IS_ACTIVATE_ON_COMPILE_ERROR = "isActivateOnError";
	public static final String IS_DEACTIVATE_ON_EXIT = "isDeactivateOnExit";
	public static final String TIMEOUT_SECONDS = "timeoutSeconds";
	public static final String DEFAULT_TIMEOUT_SECONDS = "defaultTimeoutSeconds";
	public static final String IS_TIMEOUT = "isTimeout";
	public static final String IS_MANUAL_TERMINATE = "isManualTerminate";
	public static final String IS_DEACTIVATE_ON_TERMINATE = "isDeactivateOnTerminate";
	public static final String IS_REFRESH_ON_UPDATE = "isRefreshOnUpdate";
	public static final String IS_AUTO_HANDLE_EXTERNAL_COMMANDS = "isAutoHandleExternalCommands";
	public static final String IS_ALLOW_UI_CONTRIBUTIONS = "isAllowUIContributions";
	public static final String IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION = "isSaveFilesBeforeBundleOperation";
	public static final String IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION = "isSaveSnapshotBeforeBundleOperation";

	/**
	 * Get option for allowing UI contributions using extensions
	 * 
	 * @return true if UI contributions is allowed, otherwise false.
	 */
	public boolean isAllowUIContributions();

	/**
	 * Get default option for allowing UI contributions using extensions
	 * 
	 * @return true if UI contributions using extensions should be allowed , otherwise false.
	 */
	public boolean getDefaultIsAllowUIContributions();

	/**
	 * Set if UI contributions using the Eclipse extension mechanism should be allowed
	 * 
	 * @param contributions true to allow UI contributions using UI extensions and false to not
	 */
	public void setIsAllowUIContributions(boolean contributions);

	public boolean isSaveFilesBeforeBundleOperation();

	public boolean getDefaultIsSaveFilesBeforeBundleOperation();

	public void setIsSaveFilesBeforeBundleOperation(boolean save);

	public boolean isSaveSnapshotBeforeBundleOperation();

	public boolean getDefaultIsSaveSnapshotBeforeBundleOperation();

	public void setIsSaveSnapshotBeforeBundleOperation(boolean save);

	/**
	 * Get option for handling external bundle commands
	 * 
	 * @return true if external commands are handled automatically, otherwise false.
	 */
	public boolean isAutoHandleExternalCommands();

	/**
	 * Get default option for handling external bundle commands
	 * 
	 * @return true if activation policy should be set to eager on activation , otherwise false.
	 */
	public boolean getDefaultIsAutoHandleExternalCommands();

	/**
	 * Set if external bundle commands should be handled automatically
	 * 
	 * @param automatic true to handle external commands automatically and false to not
	 */
	public void setIsAutoHandleExternalCommands(boolean automatic);

	/**
	 * Should bundles be refreshed right after they are updated
	 * 
	 * @return true if refresh after update, otherwise false.
	 */
	public boolean isRefreshOnUpdate();

	/**
	 * Get default option for refresh bundle after update
	 * 
	 * @return true if default is refresh after update, otherwise false.
	 */
	public boolean getDefaultIsRefreshOnUpdate();

	/**
	 * Set whether bundles should be refreshed after they are updated
	 * 
	 * @param refresh true to refresh after update and false to not
	 */
	public void setIsRefreshOnUpdate(boolean refresh);

	/**
	 * Should bundle projects be activated or updated when they contains compile time errors
	 * 
	 * @return true if activate or update with compile time errors, otherwise false.
	 */
	public boolean isActivateOnCompileError();

	/**
	 * Get default option for projects to be activated or updated when they contains compile time
	 * errors
	 * 
	 * @return true if default is to activate or update with compile time errors, otherwise false.
	 */
	public boolean getDefaultIsActivateOnCompileError();

	/**
	 * Set whether bundle projects should be activated or updated when bundle projects contains
	 * compile time errors
	 * 
	 * @param compileError true to activate or update with compile time errors, otherwise false.
	 */
	public void setIsActivateOnCompileError(boolean compileError);

	/**
	 * Should bundles be updated right after they are built
	 * 
	 * @return true if update after build, otherwise false.
	 */
	public boolean isUpdateOnBuild();

	/**
	 * Get default option for update bundle after build
	 * 
	 * @return true if default is update after build, otherwise false.
	 */
	public boolean getDefaultIsUpdateOnBuild();

	/**
	 * Set whether bundles should be updated after they are built
	 * 
	 * @param update true to update after build and false to not
	 */
	public void setIsUpdateOnBuild(boolean update);

	/**
	 * Check for enabling/disabling the timeout functionality in Start and Stop methods
	 * 
	 * @return true if the timeout functionality is enabled in Start and Stop methods. False if this
	 * functionality is disabled.
	 */
	public boolean isTimeOut();

	/**
	 * Get the default for enabling/disabling timeout in Start and Stop methods
	 * 
	 * @return true if the default timeout functionality is enabled in Start and Stop methods and
	 * false if not
	 */
	public boolean getDefaultIsTimeOut();

	/**
	 * Set value determining if Start and Stop should be manually stopped
	 * 
	 * @param terminate true to force stopping running start and stop methods. Otherwise false
	 */
	public void setIsManualTerminate(boolean terminate);

	/**
	 * Get value determining if Start and Stop should be manually stopped
	 * 
	 * @return True if force termination of start and stop methods. Otherwise false
	 */
	public boolean isManualTerminate();

	/**
	 * Get default value determining if Start and Stop should be manually stopped
	 * 
	 * @return True if default is to force termination of start and stop methods. Otherwise false
	 */
	public boolean getDefaultIsManualTerminate();

	/**
	 * Enable or disable the timeout functionality in Start and Stop methods
	 * 
	 * @param timeOut true to enable the timeout function i Start and Stop methods. False to disable
	 * this functionality.
	 */
	public void setIsTimeOut(boolean timeOut);

	/**
	 * Get thread timeout value in seconds for Start and Stop methods in bundles.
	 * 
	 * @return the timeout for Start and Stop methods in bundles
	 */
	public int getTimeout();

	/**
	 * Default thread timeout value in seconds for Start and Stop methods in bundles. Default is 5000
	 * ms if not set as a configuration parameter using the "equinox.statechange.timeout"
	 * configuration setting
	 * 
	 * @return the default timeout value for the "equinox.statechange.timeout" setting
	 */
	public int getDeafultTimeout();

	/**
	 * Thread timeout value for activating bundles at Framework startup. Default is 5000 ms. See the
	 * "equinox.statechange.timeout" configuration setting
	 * 
	 * @return the default timeout value for the "equinox.statechange.timeout" setting
	 */
	public int getStateChangeWait();

	/**
	 * Set the time in seconds as the time to wait before returning from the Start and Stop methods in
	 * a bundle.
	 * 
	 * @param seconds time to wait in seconds before returning from the Start and Stop methods in a
	 * bundle
	 */
	public void setTimeOut(int seconds);

	/**
	 * Set the default timeout interval in seconds for this session only. At restart of the Framework
	 * the default equinox state change default timeout is used.
	 * <p>
	 * To permanently change the default timeout setting use "equinox.statechange.timeout"
	 * configuration setting *
	 * 
	 * @param seconds timeout in seconds for start and stop methods
	 */
	public void setDefaultTimeout(int seconds);

	/**
	 * Check whether all bundles is going to be deactivated when the Framework shuts down
	 * 
	 * @return true if deactivate on exit and false if not
	 */
	public boolean isDeactivateOnExit();

	/**
	 * Get default of whether all bundles is going to be deactivated when the Framework shuts down
	 * 
	 * @return true if default is deactivate on exit and false if not
	 */
	public boolean getDefaultIsDeactivateOnExit();

	/**
	 * Set whether bundle should be deactivated when the Framework shuts down
	 * 
	 * @param deactivate true to deactivate and false to not deactivate
	 */
	public void setIsDeactivateOnExit(boolean deactivate);

	/**
	 * Check whether a bundle is going to be deactivated when the bundle task is terminated
	 * 
	 * @return true if deactivate on terminate and false if not
	 */
	public boolean isDeactivateOnTerminate();

	/**
	 * Get default of whether a bundle is going to be deactivated when the bundle task is terminated
	 * 
	 * @return true if default is deactivate on terminate and false if not
	 */
	public boolean getDefaultIsDeactivateOnTerminate();

	/**
	 * Set whether all bundles should be deactivated when the bundle task is terminated
	 * 
	 * @param deactivate true to deactivate and false to not deactivate
	 */
	public void setIsDeactivateOnTerminate(boolean deactivate);
}
