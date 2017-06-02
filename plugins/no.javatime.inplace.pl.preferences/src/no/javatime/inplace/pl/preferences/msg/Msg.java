package no.javatime.inplace.pl.preferences.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.pl.preferences.msg.PreferencesMessages"; //$NON-NLS-1$
	
	// Exceptions
	public static String PREFERENCE_FLUSH_EXCEPTION;
	public static String STORE_SERVICE_EXCEPTION;
	public static String STOP_OPERATION_OPTON_SELECTION_EXCEPTION;
	
	// Errors
	public static String INIT_PREF_PAGE_ERROR;
	public static String INIT_DEFAULT_PREF_PAGE_ERROR;
	public static String DEFAULT_PREF_PAGE_ERROR;
	public static String SAVE_PREF_PAGE_ERROR;
	
	// Preference labels
	public static String PAGE_DESCRIPTION_LABEL;
	public static String TIMEOUT_GROUP_LABEL;
	public static String TIMEOUT_RADIOGROUP_NAME_LABEL;
	public static String IS_TIMEOUT_LABEL;
	public static String TIMEOUT_SECONDS_LABEL;
	public static String TERMINATE_ON_TIMEOUT_LABEL;
	public static String IS_MANUAL_TERMINATE_LABEL;
	public static String IS_DEACTIVATE_ON_TERMINATE_LABEL;
	public static String COMMAND_GROUP_LABEL;
	public static String IS_DEACTIVATE_ON_EXIT_LABEL;
	public static String IS_UPDATE_ON_BUILD_LABEL;
	public static String IS_ACTIVATE_ON_COMPILE_ERROR_LABEL;
	public static String IS_REFRESH_ON_UPDATE_LABEL;
	public static String IS_AUTO_HANDLE_EXTERNAL_COMMANDS_LABEL;
	public static String MANIFEST_GROUP_LABEL;
	public static String DEAFULT_OUTPUT_FOLDER_LABEL;
	public static String IS_UPDATE_DEFAULT_OUTPUT_FOLDER_LABEL;
	public static String IS_EAGER_ON_ACTIVATE_LABEL;
	public static String ALLOW_EXTENSIONS_TEXT_LABEL;
	public static String IS_ALLOW_UI_CONTRIBUTIONS_LABEL;
	public static String SAVE_GROUP_LABEL;
	public static String IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION_LABEL;
	public static String IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION_LABEL;
	public static String AVAILABLE_FROM_MAIN_MENU_LABEL;
	

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
