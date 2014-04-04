package no.javatime.inplace.pl.preferences.page;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.pl.preferences.PreferencePlActivator;
import no.javatime.inplace.pl.preferences.msg.Msg;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
				
		IPreferenceStore prefStore = PreferencePlActivator.getDefault().getPreferenceStore();
		try {
			CommandOptions cmdStore = PreferencePlActivator.getDefault().getOptionsService();
			prefStore.setDefault(Msg.TIMEOUT_RADIOGROUP_NAME, CommandOptions.IS_TIMEOUT);
			prefStore.setDefault(CommandOptions.IS_TIMEOUT, cmdStore.getDefaultIsTimeOut());
//			prefStore.setDefault(CommandOptions.IS_MANUAL_TERMINATE, cmdStore.getDefaultIsManualTerminate());
			prefStore.setDefault(CommandOptions.TIMEOUT_SECONDS, cmdStore.getDeafultTimeout());
			prefStore.setDefault(CommandOptions.IS_DEACTIVATE_ON_EXIT, cmdStore.getDefaultIsDeactivateOnExit());
			prefStore.setDefault(CommandOptions.IS_REFRESH_ON_TERMINATE, cmdStore.getDefaultIsRefreshOnTerminate());
			prefStore.setDefault(CommandOptions.IS_UPDATE_ON_BUILD, cmdStore.getDefaultIsUpdateOnBuild());
			prefStore.setDefault(CommandOptions.IS_REFRESH_ON_UPDATE, cmdStore.getDefaultIsRefreshOnUpdate());
			prefStore.setDefault(CommandOptions.IS_EAGER_ON_ACTIVATE, cmdStore.getDefaultIsEagerOnActivate());
			prefStore.setDefault(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS, cmdStore.getDefaultIsAutoHandleExternalCommands());
			prefStore.setDefault(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS, cmdStore.getDefaultIsAllowUIContributions());
			prefStore.setDefault(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER, cmdStore.getDefaultUpdateDefaultOutPutFolder());

		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, PreferencePlActivator.PLUGIN_ID, Msg.INIT_DEFAULT_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		}
	}

}
