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
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import no.javatime.util.Activator;

import org.eclipse.core.runtime.IStatus;
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
 * Format and access string messages in resource bundles by key/value pairs. Accessed strings may be
 * prepended with a prefix string and are formatted using
 * {@link java.text.MessageFormat#format(String, Object[])} and directed to different output
 * devices.
 * <p>
 * A <code>Message</code> object is obtained by calling the {@link #getInstance()} method
 * <p>
 * Categorized or specialized classes for handling other types of messages may subclass
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
		 * Forward retrieved messages to {@link no.javatime.util.messages.log.Logger}
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

	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String MESSAGE_PROPERTIES_FILE_NAME = "no.javatime.util.messages.messages"; //$NON-NLS-1$

	/** Assignment of the messages.properties property file as a resource bundle */
	private static ResourceBundle generalBundle = null;

	/*
	 * Substituting the declaration of instance and the method #getInstance() with:
	 * 
	 * public final static Message INSTANCE = new Message();
	 * 
	 * gives better performance but is rigorous to changes
	 */
	private static Message instance = null;

	/**
	 * Prevent outside, not inherited classes from instantiation. Initialize with default output
	 * device to use and if prefix should be used.
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
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
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

	public void handleMessage(String msg) {

		if (Category.DEBUG) {
			StackTraceElement frame = getCallerMetaInfo();
			getString("extended_log", frame.getClassName(), frame.getMethodName(), msg); //$NON-NLS-1$
		} else {
			getString("log_message", msg); //$NON-NLS-1$
		}
	}

	/**
	 * Identify an unformatted string resource associated with the key, format it by performing
	 * substitutions and output the message to a specific device.
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
	 * Identify an unformatted string resource associated with the key, and format it by performing
	 * substitutions
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

	/**
	 * Gets a string for the given key from the bundle defined for this class. This method is
	 * typically overridden by sub classes handling categorized messages.
	 * 
	 * @param key the key for the desired string
	 * @return the string for the given key
	 */
	protected String getString(final String key) {
		return getString(key, generalBundle, MESSAGE_PROPERTIES_FILE_NAME);
	}

	/**
	 * Gets a string for the given key and bundle. This method is typically used by overridden
	 * getString (String key) methods specifying which categorized resource to use.
	 * <p>
	 * Exceptions related to access of resource bundles are caught in this method and information
	 * about causes will be sent to output devices.
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
		} catch (ResourceException e) {
			// This is not critical, and it's to much to handle this at higher
			// levels each time a string is read from a property file
			// Use inline text. Property file may be missing
			String inlineMsg = "Property file " + rbName + " may have ben deleted or moved or the key "
					+ key + " may have been deleted from this file";
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, inlineMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			return "Missing string";
		}
		return msg;
	}

	/**
	 * Gets a string for the given key and resource bundle. Exceptions are logged. Higher levels
	 * should inform users of exceptions
	 * 
	 * @param key the key for the desired string
	 * @param rb the properties file
	 * @return the string for the given key
	 * @throws ResourceException if no object for the given key can be found, if key is null or if the
	 * object found for the given key is not a string
	 */
	protected static String get(final String key, ResourceBundle rb) throws ResourceException {

		String msg = null;

		try {
			// Get the message.
			msg = rb.getString(key);
			return msg;
		} catch (MissingResourceException e) {
			throw new ResourceException(e, "missing_resource_key", key, e.getMessage()); //$NON-NLS-1$
		} catch (NullPointerException e) {
			throw new ResourceException(e, "npe_resource_key", key, e.getMessage()); //$NON-NLS-1$
		} catch (ClassCastException e) {
			throw new ResourceException(e, "cast_resource_key", key, e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Initializing of of usage of prefix string for messages. If this method returns true, the
	 * message string for the key "prefix_message" in this resource bundle is used as prefix for
	 * messages.
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
	 * Detects if prefix for displayed messages should be used, and if so return the prefix. The
	 * prefix identifies the message class and property resource key used for the message.
	 * <p>
	 * The format of the prefix is by default [{&lt;name of message class&gt;}({&lt;key name&gt;})]
	 * The format may be changed in the different properties files and is identified by the
	 * "prefix_msg_format" key.
	 * 
	 * @param extendedPrefix the key to the string to substitute in to the message
	 * @return the prefix or null if no prefix should be used.
	 */
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(generalBundle, extendedPrefix);
	}

	/**
	 * Detects if prefix for displayed messages should be used, and if so return the prefix. The
	 * prefix identifies the message class and property resource key used for the message.
	 * <p>
	 * The format of the prefix is by default [{&lt;name of message class&gt;}({&lt;key name&gt;})]
	 * The format may be changed in the different properties files and is identified by the
	 * "prefix_msg_format" key.
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
				// Add the extended prefix to the standard prefix if the format contains two substitution
				// parameters
				if (Category.getState(Category.dynamicPrefix)
						&& msgFormat.contains("{1}") && null != extendedPrefix) { //$NON-NLS-1$
					prefixMsg = MessageFormat.format(msgFormat, rb.getString("msg_prefix"), extendedPrefix); //$NON-NLS-1$
				} else {
					prefixMsg = MessageFormat.format(msgFormat, rb.getString("msg_prefix")); //$NON-NLS-1$
				}
			}
			return prefixMsg;
		} else {
			return null;
		}
	}

	/**
	 * The prefix prepended to messages
	 * 
	 * @return the prefix string or null if not defined
	 */
	public String getPrefixMsg() {
		return this.prefixMsg;
	}

	/**
	 * Override the prefix message from resource bundle
	 * 
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
	 * Used by {@link #getCallerMetaInfo()} to see if on of the registered classes to exclude from the
	 * stack frame match the class name in the stack frame given as a parameter to this member
	 * 
	 * @param frameName the class name in the stack frame
	 * @return true if one of the registered classes match the name of the class in the frame
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
	 * Initializing usage of device for displaying messages. This method returns the output device
	 * according to the message string for the key "output_device" in the resource bundle used.
	 * 
	 * @param rb containing a "output_device" key
	 * @return the output device corresponding string associated with the output_device key.
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
		} else if (outputDevice.equalsIgnoreCase("viewAndConsole")) { //$NON-NLS-1$
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
	 * Echo strings to an output device, and optionally prepends a prefix string to the message. The
	 * decision to prepend a prefix string rests on the current setting of the prefix for this or
	 * derived class instances.
	 * 
	 * @param key unique key of the message
	 * @param msg the string to output
	 * @param device the device to output the message
	 * @return same as the input message parameter with or with a prepended prefix string if the
	 * current setting of prefix for this class instance is true
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
	 * Echo strings to an output device, and optionally prepends a prefix string to the message. The
	 * decision to prepend a prefix string rests on the current setting of the prefix for this or
	 * derived class instances.
	 * 
	 * @param key unique key of the message
	 * @param msg the string to output
	 * @return same as the input message parameter with a prepended prefix string if the current
	 * setting of prefix for this class instance is true
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
			outputConsole(key, msg);
		}
		case view: {
			outputConsole(key, msg);
		}
		case viewAndConsole: {
			return outputConsole(key, msg);
		}
		case viewAndLog: {
			return outputConsole(key, msg);
		}
		case consoleAndLog: {
			return outputConsole(key, msg);
		}
		case viewAndLogAndConsole: {
			return outputConsole(key, msg);
		}
		case nil:
		default:
			String prefix = getPrefixMsg(key);
			prefix = prefix != null ? prefix + msg : msg;
			return prefix;
		}
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
	 * Open view if closed or set focus on an already open view
	 * 
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
					} else {
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
	 * 
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
	 * Display a message in JavaTime console view
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @return same as the input message parameter with a prepended prefix string if the current
	 * setting of prefix for this class instance is true
	 * @see #setPrefix(boolean)
	 */
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, generalBundle);
	}

	/**
	 * Display a message in JavaTime console view. Adds a prefix to the message if the prefix message
	 * is set to true
	 * 
	 * @param key of the message retrieved from a property file
	 * @param msg the message to display
	 * @param rb containing the actual prefix string
	 * @return same as the input message parameter with a prepended prefix string if the current
	 * setting of prefix for this class instance is true
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
		return Activator.getImageDescriptor("icons/system_in_out.gif"); //$NON-NLS-1$
	}
}
