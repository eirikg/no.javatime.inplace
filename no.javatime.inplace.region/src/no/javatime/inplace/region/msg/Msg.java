package no.javatime.inplace.region.msg;

import org.eclipse.osgi.util.NLS;

public class Msg extends NLS {

	// Build messages 
	
	public static String REQUIRING_BUNDLES_BUILD_ERROR_WARN;
	public static String BUILD_ERROR_WARN;

	public static String CLOSURE_INFO;
	public static String PROVIDING_BUNDLES_INFO;
	public static String REQUIRING_BUNDLES_INFO;
	public static String AWAITING_BUILD_INFO;
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.region.msg.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}

}
