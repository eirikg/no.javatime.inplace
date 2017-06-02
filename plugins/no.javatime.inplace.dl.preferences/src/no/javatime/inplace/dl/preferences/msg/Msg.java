package no.javatime.inplace.dl.preferences.msg;

import org.eclipse.osgi.util.NLS;

	
public class Msg extends NLS {
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.dl.preferences.msg.OptionsMessages"; //$NON-NLS-1$
	
	public static String PREFERENCE_FLUSH_EXCEPTION;
	public static String STORE_SERVICE_EXCEPTION;
	public static String ILLEGAL_CLOSURE_EXCEPTION;
	

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
