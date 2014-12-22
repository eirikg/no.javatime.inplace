package no.javatime.inplace.ui.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
		
	// Exceptions
	public static String PREFERENCE_FLUSH_EXCEPTION;

	// Errors
	public static String ADD_CONTRIBUTION_ERROR;
	public static String ADD_MENU_EXEC_ERROR;
	public static String MANIFEST_ERROR;
	
	// Field and menu labels
	public static String IS_TIMEOUT_LABEL;
	public static String TIMEOUT_SECONDS_LABEL;
	public static String IS_DEACTIVATE_ON_EXIT_LABEL;
	public static String IS_UPDATE_DEFAULT_OUTPUT_FOLDER_LABEL;
	public static String IS_UPDATE_ON_BUILD_LABEL;
	public static String IS_REFRESH_ON_UPDATE_LABEL;
	public static String IS_EAGER_ON_ACTIVATE_LABEL;
	public static String IS_AUTO_HANDLE_EXTERNAL_COMMANDS_LABEL;
	public static String IS_ALLOW_UI_CONTRIBUTIONS_LABEL;
	public static String STOP_BUNDLE_OP_LABEL;
	public static String ABOUT_TO_FINISH_JOB_LABEL;
	public static String INTERRUPT_JOB_LABEL;
	
	public static String ACTIVATE_JOB;
	public static String DEACTIVATE_UI_CONTRIBOTRS_JOB;

	public static String REFRESH_HINT_INFO;

  private static final String BUNDLE_NAME = "no.javatime.inplace.ui.msg.UIMessages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
