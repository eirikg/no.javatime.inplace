package no.javatime.inplace.extender.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
		
	// Exceptions
	public static String GET_SERVICE_EXP;
	public static String GET_EXTENDER_EXP;
	public static String NULL_EXTENDER_EXP;
	public static String NULL_EXTENSION_EXP;
	
	
	// Warnings
	public static String EXTENDER_NOT_AVAILABLE;
	

  private static final String BUNDLE_NAME = "no.javatime.inplace.extender.msg.Messages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
