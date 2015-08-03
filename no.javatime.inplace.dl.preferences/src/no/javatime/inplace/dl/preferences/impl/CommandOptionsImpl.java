package no.javatime.inplace.dl.preferences.impl;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;

/**
 * Service implementation for access and flushing of commands and manifest (extended class) options
 *
 */
public class CommandOptionsImpl extends ManifestOptionsImpl implements CommandOptions {
	
	private final static boolean defIsDeactivateOnExit = false;
	private final static boolean defIsUpdateOnBuild = true;
	private final static boolean defIsRefreshOnUpdate = true;
	private final static boolean defIsAutoHandleExternalCommands = true;
	private final static boolean defIsAllowUIContributions = true;
	private final static boolean defIsSaveFilesBeforeBundleOperation = false;
	private final static boolean defIsSaveSnapshotBeforeBundleOperation = false;
	private final static boolean defIsTimeOut = false;
	private final static boolean defIsManualTerminate = true;
	private final static boolean defIsDeactivateOnTerminate = true;
	private final static int defTimeOut = 5;
	
	public CommandOptionsImpl() {
	}
	
	@Override
	public int getTimeout() {
		return getPrefs().getInt(TIMEOUT_SECONDS, getDeafultTimeout());
	}

	@Override
	public int getDeafultTimeout() {
		return getPrefs().getInt(DEFAULT_TIMEOUT_SECONDS, getStateChangeWait());
	}

	@Override
	public int getStateChangeWait() {
		int stateChangeWait = defTimeOut*1000;
		try {
			// In ms
			String prop = bundleContext.getProperty("equinox.statechange.timeout");			
			if (prop != null) {
				stateChangeWait = Integer.parseInt(prop);
			}
			if (stateChangeWait < 1000 || stateChangeWait > 60000) {
				stateChangeWait = defTimeOut*1000;
			}
		} catch (Throwable t) {
			// use default 5000 ms
			stateChangeWait = defTimeOut*1000;
		}
		stateChangeWait /= 1000;
		return stateChangeWait;
	}

	@Override
	public void setTimeOut(int seconds) {
		getPrefs().putInt(TIMEOUT_SECONDS, seconds);
	}

	@Override
	public void setDefaultTimeout(int seconds) {
		getPrefs().putInt(DEFAULT_TIMEOUT_SECONDS, seconds);
	}

	@Override
	public boolean isTimeOut() {
		return getPrefs().getBoolean(IS_TIMEOUT, getDefaultIsTimeOut());
	}

	@Override
	public boolean getDefaultIsTimeOut() {
		boolean isTimout = defIsTimeOut;
		try {
			// In ms
			String prop = bundleContext.getProperty("inplace.timeout");			
			if (prop != null)
				isTimout = Boolean.parseBoolean(prop);
		} catch (Throwable t) {
			isTimout = defIsTimeOut;
		}
		return isTimout;
	}

	@Override
	public void setIsTimeOut(boolean timeOut) {
		getPrefs().putBoolean(IS_TIMEOUT, timeOut);
	}

	@Override
	public boolean isDeactivateOnExit() {
		return getPrefs().getBoolean(IS_DEACTIVATE_ON_EXIT, getDefaultIsDeactivateOnExit());
	}

	@Override
	public boolean getDefaultIsDeactivateOnExit() {
		return defIsDeactivateOnExit;
	}

	@Override
	public void setIsDeactivateOnExit(boolean deactivate) {
		getPrefs().putBoolean(IS_DEACTIVATE_ON_EXIT, deactivate);
	}

	@Override
	public boolean isUpdateOnBuild() {
		return getPrefs().getBoolean(IS_UPDATE_ON_BUILD, getDefaultIsUpdateOnBuild());
	}

	@Override
	public boolean getDefaultIsUpdateOnBuild() {
		return defIsUpdateOnBuild;
	}

	@Override
	public void setIsUpdateOnBuild(boolean update) {
		getPrefs().putBoolean(IS_UPDATE_ON_BUILD, update);
	}

	@Override
	public boolean isRefreshOnUpdate() {
		return getPrefs().getBoolean(IS_REFRESH_ON_UPDATE, getDefaultIsRefreshOnUpdate());
	}

	@Override
	public boolean getDefaultIsRefreshOnUpdate() {
		return defIsRefreshOnUpdate;
	}

	@Override
	public void setIsRefreshOnUpdate(boolean refresh) {
		getPrefs().putBoolean(IS_REFRESH_ON_UPDATE, refresh);
	}

	@Override
	public boolean isAutoHandleExternalCommands() {
		return getPrefs().getBoolean(IS_AUTO_HANDLE_EXTERNAL_COMMANDS, getDefaultIsAutoHandleExternalCommands());
	}

	@Override
	public boolean getDefaultIsAutoHandleExternalCommands() {
		return defIsAutoHandleExternalCommands;	
	}

	@Override
	public void setIsAutoHandleExternalCommands(boolean automatic) {
		getPrefs().putBoolean(IS_AUTO_HANDLE_EXTERNAL_COMMANDS, automatic);
	}

	@Override
	public boolean isAllowUIContributions() {
		return getPrefs().getBoolean(IS_ALLOW_UI_CONTRIBUTIONS, getDefaultIsAllowUIContributions());
	}

	@Override
	public boolean getDefaultIsAllowUIContributions() {
		return defIsAllowUIContributions;
	}

	@Override
	public void setIsAllowUIContributions(boolean contributions) {
		getPrefs().putBoolean(IS_ALLOW_UI_CONTRIBUTIONS, contributions);
	}

	@Override
	public boolean isSaveFilesBeforeBundleOperation() {
		return getPrefs().getBoolean(IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION, getDefaultIsSaveFilesBeforeBundleOperation());
	}

	@Override
	public boolean getDefaultIsSaveFilesBeforeBundleOperation() {
		return defIsSaveFilesBeforeBundleOperation;
	}

	@Override
	public void setIsSaveFilesBeforeBundleOperation(boolean save) {
		getPrefs().putBoolean(IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION, save);
	}

	@Override
	public boolean isSaveSnapshotBeforeBundleOperation() {
		return getPrefs().getBoolean(IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION, getDefaultIsSaveSnapshotBeforeBundleOperation());
	}

	@Override
	public boolean getDefaultIsSaveSnapshotBeforeBundleOperation() {
		return defIsSaveSnapshotBeforeBundleOperation;
	}

	@Override
	public void setIsSaveSnapshotBeforeBundleOperation(boolean save) {
		getPrefs().putBoolean(IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION, save);
	}

	@Override
	public void setIsManualTerminate(boolean terminate) {
		getPrefs().putBoolean(IS_MANUAL_TERMINATE, terminate);		
	}

	@Override
	public boolean isManualTerminate() {
		return getPrefs().getBoolean(IS_MANUAL_TERMINATE, getDefaultIsManualTerminate());
	}

	@Override
	public boolean getDefaultIsManualTerminate() {
		return defIsManualTerminate;
	}

	@Override
	public boolean isDeactivateOnTerminate() {
		return getPrefs().getBoolean(IS_DEACTIVATE_ON_TERMINATE, getDefaultIsDeactivateOnTerminate());
	}

	@Override
	public boolean getDefaultIsDeactivateOnTerminate() {
		return defIsDeactivateOnTerminate;
	}

	@Override
	public void setIsDeactivateOnTerminate(boolean deactivate) {
		getPrefs().putBoolean(IS_DEACTIVATE_ON_TERMINATE, deactivate);		
	}
}
