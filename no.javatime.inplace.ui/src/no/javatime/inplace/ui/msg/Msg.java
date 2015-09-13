package no.javatime.inplace.ui.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
		
	// Exceptions
	public static String PREFERENCE_FLUSH_EXCEPTION;
	public static String SERVICE_UNAVAILABLE_EXCEPTION;
	public static String PROPERTY_PAGE_NOT_UPDATED_EXCEPTION;
	public static String THREAD_INVALID_ACCESS_EXCEPTION; 
	// Errors
	public static String ADD_CONTRIBUTION_ERROR;
	public static String ADD_MENU_EXEC_ERROR;
	public static String CREATE_BUNDLE_VIEW_ERROR;
	public static String MANIFEST_ERROR;
	
	// Warnings
	public static String EXTENDER_NOT_AVAILABLE_WARN;
	
	// Info messages
	public static String REFRESH_HINT_INFO;

	// Windows and Page labels
	public static String BUNDLE_LIST_PAGE_CAPTION_TITLE_LABEL;
	public static String BUNDLE_DETAILS_PAGE_CAPTION_TITLE_LABEL;
	
	// Tool bar, tool tip and pull down, main and popup menu labels
	public static String STOP_BUNDLE_OPERATION_LABEL;
	public static String ABOUT_TO_FINISH_JOB_LABEL;
	public static String INTERRUPT_JOB_LABEL;
	public static String EAGER_ACTIVATION_LABEL;
	public static String ADD_CLASSPATH_MAIN_LABEL;
	public static String REMOVE_CLASSPATH_MAIN_LABEL;
	public static String DEPENDENCY_CLOSURE_MAIN_LABEL;
	public static String ADD_CLASSPATH_POPUP_LABEL;
	public static String REMOVE_CLASSPATH_POPUP_LABEL;
	public static String FLIP_DETAILS_LABEL;
	public static String FLIP_LIST_LABEL;
	public static String ACTIVATE_GENERAL_LABEL;
	public static String ACTIVATE_WORKSPACE_LABEL;
	public static String ACTIVATE_LABEL; 
	public static String DEACTIVATE_POPUP_LABEL; 
	public static String DEACTIVATE_MAIN_LABEL;
	public static String REFRESH_POPUP_LABEL;
	public static String REFRESH_PENDING_LABEL;
	public static String REFRESH_MAIN_LABEL;
	public static String REFRSH_GENERAL_LABEL;
	public static String UPDATE_POPUP_LABEL;
	public static String UPDATE_MAIN_LABEL;
	public static String UPDATE_PENDING_LABEL;
	public static String UPDATE_GENERAL_LABEL;
	public static String RESET_POPUP_LABEL;
	public static String RESET_MAIN_LABEL;
	public static String RESET_GENERAL_LABEL;
	public static String STOP_LABEL;
	public static String START_LABEL;
	public static String START_STOP_GENERAL_LABEL;
	public static String OPEN_IN_EDITOR_LABEL;
	public static String OPEN_IN_EDITOR_GENERAL_LABEL;
	public static String UPDATE_CLASS_PATH_LABEL;
	public static String UPDATE_CLASS_PATH_GENERAL_LABEL;
	public static String LINK_WITH_EXPLORERS_LABEL;
	public static String LINK_WITH_EXPLORERS_GENERAL_LABEL;

	// Property labels
	public static String IDENTIFIERS_PROP_CATEGORY_LABEL;
	public static String ALL_DEPENDENCIES_PROP_CATEGORY_LABEL;
	public static String RESOLVED_DEPENDENCIES_PROP_CATEGORY_LABEL;
	public static String STATUS_PROP_CATEGORY_LABEL;
	public static String BUNDLE_NAME_PROP_LABEL;
	public static String BUNDLE_SYMBOLIC_NAME_PROP_LABEL;
	public static String BUNDLE_VERSION_PROP_LABEL;
	public static String PROJECT_NAME_PROP_LABEL;
	public static String BUNDLE_ID_PROP_LABEL;
	public static String ACTIVATION_MODE_PROP_LABEL;
	public static String BUNDLE_STATE_PROP_LABEL;
	public static String SERVICES_IN_USE_PROP_LABEL;
	public static String NUMBER_OF_REVISIONS_PROP_LABEL;
	public static String ACTIVATION_POLICY_PROP_LABEL;
	public static String BUNDLE_STATUS_PROP_LABEL;
	public static String LOCATION_PROP_LABEL;
	public static String LAST_INSTALLED_OR_UPDATED_PROP_LABEL;
	public static String LAST_MODIFIED_PROP_LABEL;
	public static String REFERENCED_PROJECTS_PROP_LABEL;
	public static String REFERENCING_PROJECTS_PROP_LABEL;
	public static String REQUIRING_DECLARED_BUNDLES_PROP_LABEL;
	public static String PROVIDING_DECLARED_BUNDLES_PROP_LABEL;
	public static String REQUIRING_RESOLVED_BUNDLES_PROP_LABEL;
	public static String PROVIDING_RESOLVED_BUNDLES_PROP_LABEL;
	public static String UI_EXTENSIONS_PROP_LABEL;
	public static String LAST_TRANSITION_PROP_LABEL;
	
	// Job names
	public static String ACTIVATE_WORKSPACE_JOB;
	public static String DEACTIVATE_WORKSPACE_JOB;
	public static String DEACTIVATE_BUNDLES_JOB;

  private static final String BUNDLE_NAME = "no.javatime.inplace.ui.msg.Messages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
