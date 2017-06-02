package no.javatime.inplace.pl.dependencies.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.pl.dependencies.msg.OptionsMessages"; //$NON-NLS-1$
	
	public static String INVALID_OPTIONS_SERVICE_EXCEPTION;
	public static String ILLEGAL_STATE_ON_SAVE_EXCEPTION;
	public static String ILLEGAL_STATE_ON_READ_EXCEPTION;
	
	public static String DIALOG_CAPTION_TEXT;
	public static String MAIN_MESSAGE;
	public static String TITLE_MESSAGE;

	public static String BUNDLES_OPERATION_NAME;
	public static String ACTIVATE_OPERATION;
	public static String DEACTIVATE_OPERATION;
	public static String STOP_OPERATION;
	public static String START_OPERATION;
		
	public static String PROVIDING_LABEL;
	public static String REQUIRING_LABEL;
	public static String PROVIDING_AND_REQUIRING_LABEL;
	public static String REQURING_AND_PROVIDING_LABEL;
	public static String PARTIAL_GRAPH_LABEL;
	public static String SINGLE_LABEL;

	public static String ACTIVATE_GROUP_DESC;
	public static String DEACTIVATE_GROUP_DESC;
	public static String REQUIRING_OPERATION_DESC;
	public static String REQUIRING_START_OPERATION_DESC;
	public static String PROVIDING_OPERATION_DESC;
	public static String PROVIDING_STOP_OPERATION_DESC;
	public static String REQUIRING_PROVIDING_OPERATION_DESC;
	public static String PARTIAL_GRAPH_OPERATION_DESC;
	public static String PROVIDING_REQUIRING_OPERATION_DESC;
	public static String SINGLE_START_OPERATION_DESC;
	public static String SINGLE_STOP_OPERATION_DESC;
	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
