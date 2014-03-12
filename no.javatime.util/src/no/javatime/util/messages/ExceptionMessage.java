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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * This is a specialization for accessing messages used by exception handlers.
 * By default all accessed strings are forwarded to the
 * {@link no.javatime.util.messages.log.MessageLog}. The more general class for
 * access of key/value pairs can be found in {@link Message}.
 * <p>
 * There are other classes for different specialized or categorized messages.
 * </p>
 */
public class ExceptionMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = ExceptionMessage.class.getName();

	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String EXCEPTION_PROPERTIES_FILE_NAME = "no.javatime.util.messages.exceptionmessages"; //$NON-NLS-1$

	// Assignment of the exceptionmessages.properties property file as a resource
	// bundle
	private static ResourceBundle exceptionBundle = null;

	private static ExceptionMessage instance = null;

	/**
	 * Prevent outside not inherited classes from instantiation.
	 */
	protected ExceptionMessage() {
		try {
			exceptionBundle = ResourceBundle.getBundle(EXCEPTION_PROPERTIES_FILE_NAME);
			initDevice(exceptionBundle);
			initPrefix(exceptionBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + EXCEPTION_PROPERTIES_FILE_NAME + 
			". It may have been deleted or moved.";
			getLogger().log(getLogger().getLevel(), msg, e);
			outputView(null, msg);
		}
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>ExceptionMessage</code>
	 */
	public synchronized static ExceptionMessage getInstance() {
		if (instance == null) {
			instance = new ExceptionMessage();
		}
		return instance;
	}
	
	/**
	 * Formats exception and forwards it to output device(s) defined for
	 * {@code ExceptionMessage} by calling {@code #getString(String, Object...)}
	 * <p>
	 * If output is defined as log, the log service listener determines the output devices
	 * @param msg exception message .
	 */
	public void handleMessage(String msg) {
		handleMessage(null, msg);
	}

	/**
	 * Formats exception and forwards it to output device(s) defined for
	 * {@code ExceptionMessage} by calling {@code #getString(String, Object...)}
	 * <p>
	 * If output is defined as log, the log service listener determines the output device(s)
	 * @param t the exception. May be null.
	 * @param msg error message added to the exception. May be null
	 */
	public void handleMessage(Throwable t, String msg) {

		if (null == msg) {
			msg = "";
		}
		if (Category.DEBUG) {
			StackTraceElement frame = getCallerMetaInfo();
			getString("log_stack_frame", frame.getClassName(), frame.getMethodName());
			// getString("extended_log", frame.getClassName(), frame.getMethodName(), msg);
		}
		if (!msg.isEmpty()) {
			getString("log_message", msg);
		}
		if (null != t) {
			for (String exceptionMessage : getChaindedExceptionMessages(t)) {
				getString("log_message", exceptionMessage);				
			}
		}
	}

	/**
	 * Forwards the exception to the log service with error log level
	 * @param t the exception. May be null.
	 * @param msg error message added to the exception. May be null
	 */
	// TODO See Logmessage
//	public void logServiceException(Throwable t, String msg) {
//		ExtendedLogService logService = getLogService();
//		if (null != logService) {
//			logService.log(LogService.LOG_ERROR, msg, t);
//		} else {
//			msg += getChainedException(t);
//			outputLog(null, msg);
//		}
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getString(final String key) {
		return getString(key, exceptionBundle, EXCEPTION_PROPERTIES_FILE_NAME );
	}
		
	/**
	 * Traverse all chained exceptions and retrieve their messages
	 * @param e the messages in the chained exception to return
	 * @return the messages in the chained exception concatenated in a string.
	 */
	public String getChainedException(Throwable e) {
		StringBuffer msgBuf = new StringBuffer();
		List<String> eList = getChaindedExceptionMessages(e);
		for (Iterator<String> iterator = eList.iterator(); iterator.hasNext();) {
			String msg = iterator.next();
			msgBuf.append(msg);
			if (!msg.endsWith(".")) {
				msgBuf.append(". ");
			}
		}
		return msgBuf.toString();
	}

	/** Adds all nested messages in a chained exception to a list.
	 * 
	 * @param e The topmost exception in a chain
	 * @return a list of all messages in a chained exception or an empty list
	 */
	public List<String> getChaindedExceptionMessages(Throwable e) {	
		List<String> tMsgs = new ArrayList<String>();
		if (null != e && null != e.getLocalizedMessage()) {
			tMsgs.add(e.getLocalizedMessage());
  		Throwable t = e.getCause();
  		while (null != t) {
  			if (null != t.getLocalizedMessage()) {
  				tMsgs.add(t.getLocalizedMessage());
  			}
				t = t.getCause();
  		}
		}
		return tMsgs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(exceptionBundle, extendedPrefix);
	}

	@Override
	protected Color getFontColor(Display display) {
		return display.getSystemColor(SWT.COLOR_RED);
	}
	
	@Override
	public String outputLog(String exdendedPrefix, String msg) {
		return outputLog(exdendedPrefix, Level.SEVERE, msg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, exceptionBundle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputView(String key, String msg) {
		return outputView(key, msg, exceptionBundle);
	}
}
