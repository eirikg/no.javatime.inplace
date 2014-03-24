package no.javatime.inplace.dl.preferences.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;

public class CommandOptionsImpl extends ManifestOptionsImpl implements CommandOptions {
	
	private final static boolean defIsDeactivateOnExit = false;
	private final static boolean defIsUpdateOnBuild = true;
	private final static boolean defIsRefreshOnUpdate = true;
	private final static boolean defIsAutoHandleExternalCommands = true;
	private final static boolean defIsAllowUIContributions = true;
	private final static boolean defIsTimeOut = true;
	private final static int defTimeOut = 5;
	
	private Collection<CommandOptions> options =
			Collections.synchronizedCollection(new ArrayList<CommandOptions>());
	
	public CommandOptionsImpl() {
	}

	protected void bindCommandOptions (CommandOptions commandOptions) {
		options.add(commandOptions);
		System.out.println("Binding options");
	}
	
	protected void unbindCommandOptions (CommandOptions commandOptions) {
		options.remove(commandOptions);
		System.out.println("Unbinding options");
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
			if (prop != null)
				stateChangeWait = Integer.parseInt(prop);
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
}
