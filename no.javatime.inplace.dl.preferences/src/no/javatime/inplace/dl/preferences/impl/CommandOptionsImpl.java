package no.javatime.inplace.dl.preferences.impl;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;

public class CommandOptionsImpl extends ManifestOptionsImpl implements CommandOptions {
	
	private final static boolean defIsDeactivateOnExit = false;
	private final static boolean defIsUpdateOnBuild = true;
	private final static boolean defIsRefreshOnUpdate = true;
	private final static boolean defIsAutoHandleExternalCommands = true;
	private final static boolean defIsAllowUIContributions = true;
	private final static boolean defIsTimeOut = true;
	private final static int defTimeOut = 5;
	

	public CommandOptionsImpl() {
	}

	@Override
	public int getTimeout() {
		return wrapper.getInt(TIMEOUT_SECONDS, getDeafultTimeout());
	}

	@Override
	public int getDeafultTimeout() {
		return wrapper.getInt(DEFAULT_TIMEOUT_SECONDS, getStateChangeWait());
	}

	@Override
	public int getStateChangeWait() {
		int stateChangeWait = defTimeOut*1000;
		try {
			// In ms
			String prop = bundleContext.getProperty("equinox.statechange.timeout");			
			if (prop != null)
				stateChangeWait = Integer.parseInt(prop);
		} catch (Throwable t) {
			// use default 5000 ms
			stateChangeWait = defTimeOut*1000;
		}
		stateChangeWait /= 1000;
		return stateChangeWait;
	}

	@Override
	public void setTimeOut(int seconds) {
		wrapper.putInt(TIMEOUT_SECONDS, seconds);
	}

	@Override
	public void setDefaultTimeout(int seconds) {
		wrapper.putInt(DEFAULT_TIMEOUT_SECONDS, seconds);
	}

	@Override
	public boolean isTimeOut() {
		return wrapper.getBoolean(IS_TIMEOUT, getDefaultIsTimeOut());
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
		wrapper.putBoolean(IS_TIMEOUT, timeOut);
	}

	@Override
	public boolean isDeactivateOnExit() {
		return wrapper.getBoolean(IS_DEACTIVATE_ON_EXIT, getDefaultIsDeactivateOnExit());
	}

	@Override
	public boolean getDefaultIsDeactivateOnExit() {
		return defIsDeactivateOnExit;
	}

	@Override
	public void setIsDeactivateOnExit(boolean deactivate) {
		wrapper.putBoolean(IS_DEACTIVATE_ON_EXIT, deactivate);
	}

	@Override
	public boolean isUpdateOnBuild() {
		return wrapper.getBoolean(IS_UPDATE_ON_BUILD, getDefaultIsUpdateOnBuild());
	}

	@Override
	public boolean getDefaultIsUpdateOnBuild() {
		return defIsUpdateOnBuild;
	}

	@Override
	public void setIsUpdateOnBuild(boolean update) {
		wrapper.putBoolean(IS_UPDATE_ON_BUILD, update);
	}

	@Override
	public boolean isRefreshOnUpdate() {
		return wrapper.getBoolean(IS_REFRESH_ON_UPDATE, getDefaultIsRefreshOnUpdate());
	}

	@Override
	public boolean getDefaultIsRefreshOnUpdate() {
		return defIsRefreshOnUpdate;
	}

	@Override
	public void setIsRefreshOnUpdate(boolean refresh) {
		wrapper.putBoolean(IS_REFRESH_ON_UPDATE, refresh);
	}

	@Override
	public boolean isAutoHandleExternalCommands() {
		return wrapper.getBoolean(IS_AUTO_HANDLE_EXTERNAL_COMMANDS, getDefaultIsAutoHandleExternalCommands());
	}

	@Override
	public boolean getDefaultIsAutoHandleExternalCommands() {
		return defIsAutoHandleExternalCommands;	
	}

	@Override
	public void setIsAutoHandleExternalCommands(boolean automatic) {
		wrapper.putBoolean(IS_AUTO_HANDLE_EXTERNAL_COMMANDS, automatic);
	}

	@Override
	public boolean isAllowUIContributions() {
		return wrapper.getBoolean(IS_ALLOW_UI_CONTRIBUTIONS, getDefaultIsAllowUIContributions());
	}

	@Override
	public boolean getDefaultIsAllowUIContributions() {
		return defIsAllowUIContributions;
	}

	@Override
	public void setIsAllowUIContributions(boolean contributions) {
		wrapper.putBoolean(IS_ALLOW_UI_CONTRIBUTIONS, contributions);
	}
}
