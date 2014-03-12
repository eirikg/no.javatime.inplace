/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.util.messages;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

import no.javatime.util.Activator;
import no.javatime.util.messages.exceptions.ViewException;
import no.javatime.util.messages.log.MessageLog;
import no.javatime.util.messages.views.MessageView;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A Message object is used to format and access string messages in resource
 * bundles by key/value pairs. Accessed strings may be prepended with a prefix
 * string and are formatted using
 * {@link java.text.MessageFormat#format(String, Object[])} and directed to
 * different output devices.
 * <p>
 * A <code>Message</code> object is obtained by calling the
 * {@link #getInstance()} method
 * <p>
 * Categorized or specialized classes for handling messages may subclass
 * <code>Message</code>
 * </p>
 */
public class Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = Message.class.getName();

	public static String defKey = "display"; //$NON-NLS-1$

	/**
	 * Console of the current IDE
	 */
	public static String CONSOLE_NAME = "InPlace Console"; //$NON-NLS-1$

	/**
	 * Echo messages to an output device.
	 */
	public enum Output {
		/** Do not use any output device */
		nil,
		/** Output to standard console */
		console,
		/**
		 * Forward retrieved messages to
		 * {@link no.javatime.util.messages.log.Logger}
		 */
		log,
		/** Output to JavaTime {@link no.javatime.util.messages.views.MessageView} */
		view,
		/** Output to JavaTime view and console */
		viewAndConsole,
		/** Output to JavaTime view and log */
		viewAndLog,
		/** Output to standard console and log */
		consoleAndLog,
		/** Output to JavaTime view, log and console */
		viewAndLogAndConsole
	}

	private Output device = null;
	private boolean isPrefix = false;
	private String prefixMsg = null;
	private String msgFormat = null;
	private static Set<String> msgClassNames = new HashSet<String>();
	
	private static int flushInterval = 1;
	private static int noOfMessages = 0;
	
	/**
	 * The Java logger sending output to Application specific log defined in 
	 * the log properties file
	 * @see #outputLog(String, String)
	 */
	private static final MessageLog logger = MessageLog.getLogger(ID);
		
	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String MESSAGE_PROPERTIES_FILE_NAME = "no.javatime.util.messages.messages"; //$NON-NLS-1$

	/** Assignment of the messages.properties property file as a resource bundle */
	private static ResourceBundle generalBundle = null;

	/*
	 * Substituting the declaration of instance and the method #getInstance()
	 * with:
	 * 
	 * public final static Message INSTANCE = new Message();
	 * 
	 * gives better performance but is rigorous to changes
	 */
	private static Message instance = null;

	/**
	 * Prevent outside, not inherited classes from instantiation. Initialize with
	 * default output device to use and if prefix should be used.
	 */
	protected Message() {
		setOutput(Output.nil);
		try {
			generalBundle = ResourceBundle.getBundle(MESSAGE_PROPERTIES_FILE_NAME);
			initDevice(generalBundle);
			initPrefix(generalBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + MESSAGE_PROPERTIES_FILE_NAME //$NON-NLS-1$
					+ ". It may have been deleted or moved."; //$NON-NLS-1$
			getLogger().log(getLogger().getLevel(), msg, e);
			outputView(null, msg);
		}
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>Message</code>
	 */
	public synchronized static Message getInstance() {
		if (instance == null) {
			instance = new Message();
		}
		return instance;
	}
	
	/**
	 * The Logger used by this class for logging messages.
	 * 
	 * @return the <code>Logger</code> instance declared in this class
	 */
	public MessageLog getLogger() {
		return logger;
	}

	public void handleMessage(String msg) {

		if (Category.DEBUG) {
  		StackTraceElement frame = getCallerMetaInfo();
  		getString("extended_log", frame.getClassName(), frame.getMethodName(), msg); //$NON-NLS-1$
		} else {
			getString("log_message", msg); //$NON-NLS-1$
		}
	}

	/**
	 * Identify an unformatted string resource associated with a key, and format
	 * it by performing substitutions. A category determines if the message should
	 * be suppressed or not.
	 * 
	 * @param key the key identifying the unformatted string.
	 * @param category identifies a category which determines if this message
	 *          should be suppressed or not
	 * @param substitutions the variable argument message substitutions.
	 * @return an empty string if the category is disabled or the formatted
	 *         message if the category is enabled
	 */
	public synchronized String getString(String key, Category category, Object... substitutions) {
		if (!category.isEnabled()) {
			return ""; //$NON-NLS-1$
		}
		return getString(key, substitutions);
	}


	/**
	 * Identify an unformatted string resource associated with the key, format it
	 * by performing substitutions and output the message to a specific device.
	 * 
	 * @param key the key identifying the unformatted message of the string.
	 * @param device output for this message only
	 * @param substitutions the message substitutions.
	 * @return a string resource or message associated with the key.
	 * @see java.text.MessageFormat#format(String, Object[])
	 */
	public synchronized String getString(String key, Output device, Object... substitutions) {
		Output currentDevice = getOutput();
		setOutput(device);
		String msg = getString(key, substitutions);
		setOutput(currentDevice);
		return msg;
	}

	/**
	 * Identify an unformatted string resource associated with the key, format
	 * without a prefix and perform substitutions and output the message to a
	 * specific device.
	 * 
	 * @param key the key identifying the unformatted message of the string.
	 * @param device output for this message only
	 * @param substitutions the message substitutions.
	 * @return a string resource or message associated with the key.
	 * @see java.text.MessageFormat#format(String, Object[])
	 */
	public synchronized String getRawString(String key, Output device, Object... substitutions) {
  		Output currentDevice = getOutput();
  		Boolean currentPrefix = setPrefix(false);
  		setOutput(device);
  		String msg = getString(key, substitutions);
  		setOutput(currentDevice);
  		setPrefix(currentPrefix);
  		return msg;
	}

	/**
	 * Identify an unformatted string resource associated with the key, and format
	 * it by performing substitutions
	 * 
	 * @param key the key identifying the unformatted message of the string.
	 * @param substitutions the message substitutions.
	 * @return a string resource or message associated with the key.
	 * @see java.text.MessageFormat#format(String, Object[])
	 */
	public synchronized String getString(String key, Object... substitutions) {
		String msg = null;
		try {
			// Retrieve the message for the key from the correct sub class
			// to access the correct resource bundle, and then substitute
			msg = MessageFormat.format(getString(key), substitutions);
		} catch (IllegalArgumentException e) {
			ExceptionMessage.getInstance().getString("illegal_format", key, e.getMessage()); //$NON-NLS-1$
			ErrorMessage.getInstance().getString("illegal_format", key); //$NON-NLS-1$
			return getString("default_msg_for_access_error", Output.nil); //$NON-NLS-1$
		}
		return output(key, msg); // Output to device for class
	}

	public synchronized String formatString(String key, Object... substitutions) {
		try {
			// Retrieve the message for the key from the correct sub class
			// to access the correct resource bundle, and then substitute
			return MessageFormat.format(getString(key), substitutions);
		} catch (IllegalArgumentException e) {
			ExceptionMessage.getInstance().getString("illegal_format", key, e.getMessage()); //$NON-NLS-1$
			ErrorMessage.getInstance().getString("illegal_format", key); //$NON-NLS-1$
			return getString("default_msg_for_access_error", Output.nil); //$NON-NLS-1$
		}
	}

	public static String commaSeparatedList( Collection<String> strings) {
		StringBuffer sb = new StringBuffer();
		if (null != strings && strings.size() >= 1) {
  		for (Iterator<String> iterator = strings.iterator(); iterator.hasNext();) {
				String string =  iterator.next();
  			sb.append(string);
  			if (iterator.hasNext()) {
    			sb.append(", ");  				 //$NON-NLS-1$
  			}
			}
		}
		return sb.toString();
	}	

	/**
	 * Gets a string for the given key from the bundle defined for this class.
	 * This method is typically overridden by sub classes handling categorized
	 * messages.
	 * 
	 * @param key the key for the desired string
	 * @return the string for the given key
	 */
	protected String getString(final String key) {
		return getString(key, generalBundle, MESSAGE_PROPERTIES_FILE_NAME);
	}

	/**
	 * Gets a string for the given key and bundle. This method is typically used
	 * by overridden getString (String key) methods specifying which categorized
	 * resource to use.
	 * <p>
	 * Exceptions related to access of resource bundles are caught in this method
	 * and information about causes will be sent to output devices.
	 * 
	 * @param key property key
	 * @param rb the resource bundle
	 * @param rbName the name of the resource bundle
	 * @return the string found in the resource bundle for the given key
	 */
	protected synchronized String getString(String key, ResourceBundle rb, String rbName) {

		String msg = null;

		try {
			msg = get(key, rb);
		} catch (ViewException e) {
			// This is not critical, and it's to much to handle this at higher
			// levels each time a string is read from a property file
			// Use inline text. Property file may be missing
			outputView(key, "Property file " + rbName + " may have ben deleted or moved or the key " //$NON-NLS-1$ //$NON-NLS-2$
					+ key + " may have been deleted from this file"); //$NON-NLS-1$
			return "Missing string"; // getString("default_msg_for_access_error", //$NON-NLS-1$
															 // Output.nil);
		}
		return msg;
	}

	/**
	 * Gets a string for the given key and resource bundle. Exceptions are logged.
	 * Higher levels should inform users of exceptions
	 * 
	 * @param key the key for the desired string
	 * @param rb the properties file
	 * @return the string for the given key
	 * @throws ViewException if no object for the given key can be found, if key
	 *           is null or if the object found for the given key is not a string
	 */
	protected static String get(final String key, ResourceBundle rb) throws ViewException {

		String msg = null;

		try {
			// Get the message.
			msg = rb.getString(key);
			return msg;
		} catch (MissingResourceException e) {
			throw new ViewException(e, "missing_resource_key", key, e.getMessage()); //$NON-NLS-1$
		} catch (NullPointerException e) {
			throw new ViewException(e, "npe_resource_key", key, e.getMessage()); //$NON-NLS-1$
		} catch (ClassCastException e) {
			throw new ViewException(e, "cast_resource_key", key, e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Initializing of of usage of prefix string for messages. If this method
	 * returns true, the message string for the key "prefix_message" in this
	 * resource bundle is used as prefix for messages.
	 * 
	 * @param rb containing a "should_prefix" key
	 * @return true if key "should_prefix" in resource bundle is true, else false
	 * @see #isPrefix()
	 * @see #setPrefix(boolean)
	 * @see #getPrefixMsg(ResourceBundle, String)
	 */
	protected boolean initPrefix(ResourceBundle rb) {
		if (rb.getString("should_prefix").equalsIgnoreCase("true")) { //$NON-NLS-1$ //$NON-NLS-2$
			setPrefix(true);
		} else {
			setPrefix(false);
		}
		return isPrefix();
	}

	/**
	 * Getter for the current prefix setting
	 * 
	 * @return the isPrefix
	 */
	public boolean isPrefix() {
		return isPrefix;
	}

	/**
	 * Alter the current prefix setting.
	 * 
	 * @param isPrefix the isPrefix to set
	 * @return the current prefix. True if prefix should be prepended to messages
	 */
	public Boolean setPrefix(boolean isPrefix) {
		boolean temp = this.isPrefix;
		this.isPrefix = isPrefix;
		return temp;
	}

	/**
	 * Detects if prefix for displayed messages should be used, and if so return
	 * the prefix. The prefix identifies the message class and property resource
	 * key used for the message.
	 * <p>
	 * The format of the prefix is by default [{&lt;name of message
	 * class&gt;}({&lt;key name&gt;})] The format may be changed in the different
	 * properties files and is identified by the "prefix_msg_format" key.
	 * 
	 * @param extendedPrefix the key to the string to substitute in to the
	 *          message
	 * @return the prefix or null if no prefix should be used.
	 */
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(generalBundle, extendedPrefix);
	}

	/**
	 * Detects if prefix for displayed messages should be used, and if so return
	 * the prefix. The prefix identifies the message class and property resource
	 * key used for the message.
	 * <p>
	 * The format of the prefix is by default [{&lt;name of message
	 * class&gt;}({&lt;key name&gt;})] The format may be changed in the different
	 * properties files and is identified by the "prefix_msg_format" key.
	 * 
	 * @param rb the resource bundle containing the prefix message
	 * @param extendedPrefix extended prefix text added to the prefix 
	 * @return the prefix or null if no prefix should be used.
	 */
	protected String getPrefixMsg(ResourceBundle rb, String extendedPrefix) {
		if (isPrefix()) {
			// TODO This is messy. Consider remove extended prefix
			if (null == prefixMsg || Category.getState(Category.dynamicPrefix)) {
				if (null == msgFormat) {
					msgFormat = rb.getString("prefix_msg_format"); //$NON-NLS-1$
				}
				// Add the extended prefix to the standard prefix if the format contains two substitution parameters
				if (Category.getState(Category.dynamicPrefix) && msgFormat.contains("{1}") && null != extendedPrefix) { //$NON-NLS-1$
					prefixMsg = MessageFormat.format(msgFormat, rb.getString("msg_prefix"), extendedPrefix); //$NON-NLS-1$
				} else {
					prefixMsg = MessageFormat.format(msgFormat, rb.getString("msg_prefix"));					 //$NON-NLS-1$
				}
			}
			return prefixMsg;
		} else {
			return null;
		}
	}
	
	/**
	 * The prefix prepended to messages 
	 * @return the prefix string or null if not defined
	 */
	public String getPrefixMsg() {
		return this.prefixMsg;
	}

	/**
	 * Override the prefix message from resource bundle
	 * @param prefixMsg to set
	 * @return the current prefix string
	 */
	public String setPrefixMsg(String prefixMsg) {
		String tmp = this.prefixMsg;	
		this.prefixMsg = prefixMsg;
		return tmp;
	}

	public synchronized void ignoreInStackFrame(String className) {
		msgClassNames.add(className);
	}

	/**
	 * Used by {@link #getCallerMetaInfo()} to see if on of the registered classes
	 * to exclude from the stack frame match the class name in the stack frame
	 * given as a parameter to this member
	 * 
	 * @param frameName the class name in the stack frame
	 * @return true if one of the registered classes match the name of the class
	 *         in the frame
	 */
	protected synchronized boolean isClassInStackFrame(String frameName) {
		return msgClassNames.contains(frameName);
	}

	/**
	 * Gets the stack frame of the caller. This is by best effort.
	 * 
	 * @return the relevant stack frame or null if no frame was found
	 */
	public StackTraceElement getCallerMetaInfo() {

		// Get the stack trace.
		StackTraceElement stack[] = (new Throwable()).getStackTrace();
		// First, search back to a method in one of the Message classes.
		int ix = 0;
		while (ix < stack.length) {
			StackTraceElement frame = stack[ix];
			String frameName = frame.getClassName();
			if (isClassInStackFrame(frameName)) {
				break;
			}
			ix++;
		}
		// Search for the first frame before any of the Message classes.
		while (ix < stack.length) {
			StackTraceElement frame = stack[ix];
			String frameName = frame.getClassName();
			if (!isClassInStackFrame(frameName)) {
				// Found a relevant frame.
				return frame;
			}
			ix++;
		}
		// Did not find a relevant frame, so just continue. This is
		// permitted as the requirement is "best effort" here.
		return null;
	}

	/* --- Device output functions --- */

	/**
	 * Initializing usage of device for displaying messages. This method returns
	 * the output device according to the message string for the key
	 * "output_device" in the resource bundle used.
	 * 
	 * @param rb containing a "output_device" key
	 * @return the output device corresponding string associated with the
	 *         output_device key.
	 * @see Output
	 */
	protected Output initDevice(ResourceBundle rb) {
		String outputDevice = rb.getString("output_device"); //$NON-NLS-1$
		if (outputDevice.equalsIgnoreCase("view")) { //$NON-NLS-1$
			setOutput(Output.view);
		} else if (outputDevice.equalsIgnoreCase("nil")) { //$NON-NLS-1$
			setOutput(Output.nil);
		} else if (outputDevice.equalsIgnoreCase("log")) { //$NON-NLS-1$
			setOutput(Output.log);
		} else if (outputDevice.equalsIgnoreCase("console")) { //$NON-NLS-1$
			setOutput(Output.console);
		}else if (outputDevice.equalsIgnoreCase("viewAndConsole")) { //$NON-NLS-1$
			setOutput(Output.viewAndConsole); 
		} else if (outputDevice.equalsIgnoreCase("consoleAndLog")) { //$NON-NLS-1$
			setOutput(Output.consoleAndLog);
		} else if (outputDevice.equalsIgnoreCase("viewAndLog")) { //$NON-NLS-1$
			setOutput(Output.viewAndLog);
		} else if (outputDevice.equalsIgnoreCase("viewAndLogAndConsole")) { //$NON-NLS-1$
			setOutput(Output.viewAndLogAndConsole);
		}
		return getOutput();
	}

	/**
	 * Get the default device to echo strings.
	 * 
	 * @return the output device
	 */
	public Output getOutput() {
		return device;
	}

	/**
	 * Set where, by default to echo strings.
	 * 
	 * @param device may be nil (no output), console, log, view or a combination
	 * @see Output
	 */
	public void setOutput(Output device) {
		this.device = device;
	}

	/**
	 * Echo strings to an output device, and optionally prepends a prefix string
	 * to the message. The decision to prepend a prefix string rests on the
	 * current setting of the prefix for this or derived class instances.
	 * 
	 * @param key unique key of the message
	 * @param msg the string to output
	 * @param device the device to output the message
	 * @return same as the input message parameter with or with a prepended prefix
	 *         string if the current setting of prefix for this class instance is
	 *         true
	 * @see #setPrefix(boolean)
	 */
	public String output(String key, String msg, Output device) {
		Output currDevice = getOutput();
		setOutput(device);
		output(key, msg);
		setOutput(currDevice);
		return msg;
	}

	/**
	 * Echo strings to an output device, and optionally prepends a prefix string
	 * to the message. The decision to prepend a prefix string rests on the
	 * current setting of the prefix for this or derived class instances.
	 * 
	 * @param key unique key of the message
	 * @param msg the string to output
	 * @return same as the input message parameter with a prepended prefix string
	 *         if the current setting of prefix for this class instance is true
	 * @see Output
	 * @see #isPrefix
	 * @see #setPrefix(boolean)
	 * @see #initPrefix(ResourceBundle)
	 */
	protected String output(String key, String msg) {

		switch (device) {
		case console: {
			return outputConsole(key, msg);
		}
		case log: {
			return outputLog(key, msg);
		}
		case view: {
			return outputView(key, msg);
		}
		case viewAndConsole: {
			outputConsole(key, msg);
			return outputView(key, msg);
		}
		case viewAndLog: {
			outputLog(key, msg);
			return outputView(key, msg);
		}
		case consoleAndLog: {
			outputConsole(key, msg);
			return outputLog(key, msg);
		}
		case viewAndLogAndConsole: {
			outputLog(key, msg);
			outputConsole(key, msg);
			return outputView(key, msg);
		}
		case nil:
		default:
			String prefix = getPrefixMsg(key);
			prefix = prefix != null ? prefix + msg : msg;
			return prefix;
		}
	}

	/**
	 * Display a message in JavaTime message view
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @return same as the input message parameter with a prepended prefix string
	 *         if the current setting of prefix for this class instance is true
	 * @see #setPrefix(boolean)
	 */
	public String outputView(String key, String msg) {

		return outputView(key, msg, generalBundle);
	}

	/**
	 * Display a message in JavaTime message view. Adds a prefix to the message if the prefix message
	 * is set to true
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @param rb containing the actual prefix string
	 * @return same as the input message parameter with a prepended prefix string if the current
	 *         setting of prefix for this class instance is true
	 * @see #isPrefix
	 * @see #setPrefix(boolean)
	 * @see #initPrefix(ResourceBundle)
	 */
	protected String outputView(String key, String msg, ResourceBundle rb) {
		final MessageContainer mc = MessageContainer.getInstance();
		String prefix = getPrefixMsg(key);
		prefix = prefix != null ? prefix + msg : msg;
		mc.addMessage(key, getPrefixMsg(rb, key), msg);
		noOfMessages++;
		if (noOfMessages >= flushInterval) {
			setInput(mc);
		}
		return prefix;
	}
	
	private boolean setInput(final MessageContainer mc) {

		Display display = Activator.getDisplay();
		if (null == display) {
			return false;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					MessageView mv = null;
					if (isViewVisible(MessageView.ID)) {
						mv = (MessageView) page.findView(MessageView.ID);
					}
					if (null != mv) {
						try {
							mv.setInput(mc);
							noOfMessages = 0;
						} catch (ConcurrentModificationException e) {
							StatusManager.getManager().handle(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e),
									StatusManager.LOG);
							ExceptionMessage.getInstance().handleMessage(e, "ConcurrentModificationException");						 //$NON-NLS-1$
						}
					}
				}
			}
		});
		return true;
	}
	
	/**
	 * Open view if closed or set focus on an already open view
	 * @param viewId part id of view
	 */
	public static void showView(final String viewId) {
		Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					if (!isViewVisible(viewId)) {
						try {
							page.showView(viewId);
						} catch (PartInitException e) {
						}
					} else  {
						IViewPart mv = page.findView(viewId);
						if (null != mv) {
							mv.setFocus();
						}
					}
				}
			}
		});
	}
	
	/**
	 * Hide view if visible or set focus on an already open view
	 * @param viewId part id of view
	 */
	public static void hideView(final String viewId) {
		Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					// Hide even if not visible, may be on another tab
					IViewPart mv = page.findView(viewId);
					if (null != mv) {
						page.hideView(mv);
					}
				}
			}
		});
	}
	
	/**
	 * Logs a message with or without a prefix to the java logger 
	 * @param exdendedPrefix added to the standard prefix of the log message. May be null. 
	 * @param logLevel standard log level
	 * @param msg message to log
	 * @return the logged message with prefix if defined
	 */
	protected String outputLog(String exdendedPrefix, Level logLevel, String msg) {
		
		String prefixMsg = getPrefixMsg(exdendedPrefix);
		prefixMsg = prefixMsg != null ? prefixMsg + msg : msg;

		StackTraceElement frame = getCallerMetaInfo();
		if (null == frame) {
			logger.log(logLevel, prefixMsg);
		}
		else {
			if (Category.DEBUG) {
				logger.logp(logLevel, frame.getClassName(), frame.getMethodName(), prefixMsg);
			} else {
				logger.log(logLevel, prefixMsg);				
			}
		}
		return prefixMsg;
	}
	
	/**
	 * Log a message to the standard JavaTime logger
	 * 
	 * @param exdendedPrefix added to the standard prefix of the log message. May be null 
	 * @param msg the message to log
	 * @return same as the input message parameter with a prepended prefix string
	 *         if the current setting of prefix for this class instance is true
	 */
	public String outputLog(String exdendedPrefix, String msg) {
		return outputLog(exdendedPrefix, Level.INFO, msg);
	}

	/**
	 * Display a message in JavaTime console view
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @return same as the input message parameter with a prepended prefix string
	 *         if the current setting of prefix for this class instance is true
	 * @see #setPrefix(boolean)
	 */
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, generalBundle);
	}
	
	/**
	 * Display a message in JavaTime console view. Adds a prefix to the message if
	 * the prefix message is set to true
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @param rb containing the actual prefix string
	 * @return same as the input message parameter with a prepended prefix string
	 *         if the current setting of prefix for this class instance is true
	 * @see #isPrefix
	 * @see #setPrefix(boolean)
	 * @see #initPrefix(ResourceBundle)
	 */
	protected String outputConsole(String key, String msg, ResourceBundle rb) {
		final String prefixMsg = getPrefixMsg(key);
		final String prefix = prefixMsg != null ? prefixMsg + msg : msg;
		System.out.println(prefix);
		return prefix;
	}

	protected Color getFontColor(Display display) {
		return display.getSystemColor(SWT.COLOR_BLACK);
	}
	
	protected ImageDescriptor getImage(Display display) {
		return Activator.getImageDescriptor("icons/system_in_out.gif");  //$NON-NLS-1$
	}

	public static IViewPart getView(String partId) {
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null != page) {
			return page.findView(partId);
		}
		return null;
	}

	public static Boolean isViewVisible(String partId) {
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null != page) {
			IViewReference viewReference = page.findViewReference(partId);
			if (null != viewReference) {
				final IViewPart view = viewReference.getView(false);
				if (null != view) {
					return page.isPartVisible(view);
				}
			}
		}
		return false;
	}

	/**
	 * Verify that a view is visible on the active page
	 * 
	 * @param part the view to check
	 * @return true if the view is visible and false if not
	 */
	public static Boolean isViewVisible(Class<? extends ViewPart> part) {

		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (page != null) {
			IViewReference[] vRefs = page.getViewReferences();
			for (IViewReference vr : vRefs) {
				IViewPart vp = vr.getView(false);
				if (null != vp && vp.getClass().equals(part)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Outputs all messages in the message container to the view and sets the
	 * flush interval to default (= 1 message)
	 */
	public void flush() {
		final MessageContainer mc = MessageContainer.getInstance();
		setInput(mc);
		setFlushInterval(1);
	}
	/**
	 * Return number of messages before flushing them to view
	 * @return number of messages to wait before flushing
	 */
	public int getFlushInterval() {
		return flushInterval;
	}

	/**
	 * Set number of messages before flushing them to view
	 * @param flushInterval number of messages to wait before flushing
	 */
	public void setFlushInterval(int flushInterval) {
		Message.flushInterval = flushInterval;
	}	
}
