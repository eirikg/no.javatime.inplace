package no.javatime.inplace.region.msg;

import org.eclipse.osgi.util.NLS;

public class Msg extends NLS {

	// Error messages
	public static String SYSTEM_BUNDLE_ERROR;
	public static String SYMBOLIC_NAME_ERROR;
	public static String STATE_CHANGE_ERROR;
	
	// Warning messages 	
	public static String REQUIRING_BUNDLES_BUILD_ERROR_WARN;
	public static String BUILD_ERROR_WARN;
	public static String ADAPT_TO_REVISION_WARN;
	public static String BUILD_ERROR_ON_DEACTIVATED_DEP_WARN;
	public static String PROJECT_MISSING_AT_LOC_WARN;
	public static String INTERNAL_STATE_WARN;
	
	// Information messages
	public static String CLOSURE_INFO;
	public static String PROVIDING_BUNDLES_INFO;
	public static String REQUIRING_BUNDLES_INFO;
	public static String AWAITING_BUILD_ERROR_INFO;
	public static String NO_BUILD_ERROR_INFO;
	public static String EXT_BUNDLE_OP_ORIGIN_INFO;
	public static String EXT_BUNDLE_OP_INFO;
	public static String INCOMPLETE_BUNDLE_OP_INFO;
	public static String NOT_RESOLVING_INFO;

	// Exceptions
	public static String PROJECT_OPEN_NOT_EXIST_EXP;
	public static String PROJECT_NATURE_NULL_EXP;
	public static String PROJECT_NATURE_CORE_EXP;
	public static String GET_SERVICE_EXP;

	// Location references
	public static String BUNDLE_ID_REF_SCHEME_REF;
	public static String BUNDLE_ID_FILE_SCHEME_REF;
	public static String MANIFEST_FILE_RELATIVE_PATH_REF;
	public static String MANIFEST_FILE_NAME_REF;
	public static String PLUGIN_ID_NATURE_ID;
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.region.msg.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
