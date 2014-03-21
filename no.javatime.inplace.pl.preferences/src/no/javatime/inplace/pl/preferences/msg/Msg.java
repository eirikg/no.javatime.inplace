package no.javatime.inplace.pl.preferences.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.pl.preferences.msg.PreferencesMessages"; //$NON-NLS-1$
	
	public static String PREFERENCE_FLUSH_EXCEPTION;
	public static String TIMEOUT_GROUP_LABEL;
	public static String IS_TIMEOUT_LABEL;
	public static String TIMEOUT_SECONDS_LABEL;
	public static String COMMAND_GROUP_LABEL;
	public static String IS_DEACTIVATE_ON_EXIT_LABEL;
	public static String IS_UPDATE_ON_BUILD_LABEL;
	public static String IS_REFRESH_ON_UPDATE_LABEL;
	public static String IS_AUTO_HANDLE_EXTERNAL_COMMANDS_LABEL;
	public static String MANIFEST_GROUP_LABEL;
	public static String IS_UPDATE_DEFAULT_OUTPUT_FOLDER_LABEL;
	public static String IS_EAGER_ON_ACTIVATE_LABEL;
	public static String ALLOW_EXTIONS_TEXT_LABEL;
	public static String IS_ALLOW_UI_CONTRIBUTIONS_LABEL;
	

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
