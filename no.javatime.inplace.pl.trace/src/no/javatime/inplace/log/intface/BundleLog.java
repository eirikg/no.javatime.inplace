package no.javatime.inplace.log.intface;

import no.javatime.inplace.log.status.IBundleStatus;



public interface BundleLog {

	public final static String BUNDLE_LOG_HEADER = "BundleLog-Container";

//	public enum MessageType {
//		MESSAGE,
//		TRACE,
//		USER,
//		EXCEPTION,
//		ERROR,
//		WARNING,		
//	}
//	
//	public enum Device {
//		/** Do not use any output device */
//		NIL,
//		/** Output to standard CONSOLE */
//		CONSOLE,
//		/**
//		 * Forward retrieved messages to
//		 * {@link no.javatime.util.messages.log.Logger}
//		 */
//		LOG,
//		/** Output to MesageView */
//		VIEW,
//		/** Output to JavaTime view and CONSOLE */
//		VIEW_AND_CONSOLE,
//		/** Output to JavaTime view and LOG */
//		VIEW_AND_LOG,
//		/** Output to standard CONSOLE and LOG */
//		CONSOLE_AND_LOG,
//		/** Output to JavaTime view, LOG and CONSOLE */
//		VIEW_AND_LOG_AND_CONSOLE
//	}
//
//	public void setOut(MessageType messageType, Device device);

	/**
	 * Displays the content in the specified message in the
	 * view defined by the {@link BundleLogView} service interface
	 * 
	 * @see BundleLogView
	 */
	public String trace(IBundleStatus status);
	
	public String message(IBundleStatus status);

	public String user(IBundleStatus status);
	
	public String exception(IBundleStatus status);

	public String warning(IBundleStatus status);

	public String error(IBundleStatus status);
}
