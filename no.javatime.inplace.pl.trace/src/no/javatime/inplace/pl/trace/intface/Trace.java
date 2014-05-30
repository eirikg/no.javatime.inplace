package no.javatime.inplace.pl.trace.intface;

import no.javatime.inplace.extender.status.IBundleStatus;



public interface Trace {

	public final static String TRACE_CONTAINER_HEADER = "Trace-Container";

	public enum MessageType {
		MESSAGE,
		TRACE,
		USER,
		EXCEPTION,
		ERROR,
		WARNING,		
	}
	
	public enum Device {
		/** Do not use any output device */
		NIL,
		/** Output to standard CONSOLE */
		CONSOLE,
		/**
		 * Forward retrieved messages to
		 * {@link no.javatime.util.messages.log.Logger}
		 */
		LOG,
		/** Output to MesageView */
		VIEW,
		/** Output to JavaTime view and CONSOLE */
		VIEW_AND_CONSOLE,
		/** Output to JavaTime view and LOG */
		VIEW_AND_LOG,
		/** Output to standard CONSOLE and LOG */
		CONSOLE_AND_LOG,
		/** Output to JavaTime view, LOG and CONSOLE */
		VIEW_AND_LOG_AND_CONSOLE
	}

	public void setOut(MessageType messageType, Device device);

	/**
	 * Displays the content in the specified message in the
	 * view defined by the {@link MessageView} service interface
	 * 
	 * @see MessageView
	 */
	public String trace(IBundleStatus status);
	
	public String message(IBundleStatus status);

	public String user(IBundleStatus status);
	
	public String exception(IBundleStatus status);

	public String warning(IBundleStatus status);

	public String error(IBundleStatus status);
}
