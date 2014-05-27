package no.javatime.inplace.dl.preferences.intface;

import org.osgi.service.prefs.BackingStoreException;

public interface MessageOptions {
	
	public final static String MESSAGE_OPTIONS_HEADER = "Message-Options";

	public static final String IS_BUNDLE_EVENTS = "isBundleEvents";
	public static final String IS_BUNDLE_OPERATIONS = "isBundleOperations";
	public static final String IS_INFO_MESSAGES = "isInfoMessages";

	/**
	 * Whether to enable tracing of bundle events or not
	 * 
	 * @return true to trace bundle events and false if not
	 */
	public abstract boolean isBundleEvents();

	/**
	 * Default for tracing of bundle events
	 *  
	 * @return true if default is to trace bundle events and false if not
	 */
	public abstract boolean getDefaultBundleEvents();

	/**
	 * Set whether to enable tracing of bundle events or not
	 * 
	 * @param bundleEvents set to true to trace bundle events and false to not
	 */
	public abstract void setIsBundleEvents(boolean bundleEvents);
	
	/**
	 * Whether to enable tracing of bundle operations or not
	 * 
	 * @return true to trace bundle operations and false if not
	 */
	public abstract boolean isBundleOperations();

	/**
	 * Default for tracing of bundle operations
	 *  
	 * @return true if default is to trace bundle operations and false if not
	 */
	public abstract boolean getDefaultBundleOperations();

	/**
	 * Set whether to enable tracing of bundle operations or not
	 * 
	 * @param bundleOperations set to true to trace bundle operations and false to not
	 */
	public abstract void setIsBundleOperations(boolean bundleOperations);

	/**
	 * Whether to enable informational messages or not
	 * 
	 * @return true to enable informational messages and false if not
	 */
	public abstract boolean isInfoMessages();

	/**
	 * Default for enabling of informational messages
	 *  
	 * @return true if default is to enable informational messages and false if not
	 */
	public abstract boolean getDefaultInfoMessages();

	/**
	 * Set whether to enable informational messages or not
	 * 
	 * @param infoMessages set to true to enable informational messages and false to not
	 */
	public abstract void setIsInfoMessages(boolean infoMessages);

	/**
	 * Flush all changes to OSGi preference store
	 * @throws BackingStoreException thrown when the flush operation could not complete
	 */
	public abstract void flush() throws BackingStoreException;

}
