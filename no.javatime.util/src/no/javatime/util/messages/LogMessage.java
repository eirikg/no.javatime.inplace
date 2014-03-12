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

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

import no.javatime.util.Activator;

import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

/**
 * Forwards messages to the log. Output devices are defined in {@code #LOG_PROPERTIES_FILE_NAME}
 * Supports OSGi log service and java logger.
 */
public class LogMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = LogMessage.class.getName();
	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String LOG_PROPERTIES_FILE_NAME = 
			"no.javatime.util.messages.logmessages"; 

	// Assignment of the tracemessages.properties property file as a resource bundle
	private static ResourceBundle logBundle = null;

	private static LogMessage instance = null;
	
	/**
	 * Prevent outside, not inherited classes from instantiation.
	 */
	protected LogMessage() {
		try {
			logBundle = ResourceBundle.getBundle(LOG_PROPERTIES_FILE_NAME);
			initDevice(logBundle);			
			initPrefix(logBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + LOG_PROPERTIES_FILE_NAME + 
			". It may have been deleted or moved.";
			getLogger().log(getLogger().getLevel(), msg, e);
			outputView(null, msg);
		}
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>TraceMessage</code>
	 */
	public synchronized static LogMessage getInstance() {
		if (instance == null) {
			instance = new LogMessage();
		}
		return instance;
	}
	
	/**
	 * The log service is created on demand on first call
	 * 
	 * @return the log service instance
	 * @see ExceptionMessage#logServiceException(Throwable, String)
	 */
	private static ExtendedLogService getLogService() {
		return Activator.getDefault().getLogService();
	}
	
	/**
	 * Logs a message. Use log service if present. Forwards to java logger if not present.
	 * @param key Not used. May be null
	 * @param logLevel standard log level
	 * @param msg message to log
	 * @return the logged message
	 */
	@SuppressWarnings("unused")
	private String log(String key, int logLevel, String msg) {
		ExtendedLogService logService = getLogService();
		if (null != logService) {
			logService.log(logLevel, msg);
		} else {
			Level level = Level.ALL;
			if (logLevel == LogService.LOG_ERROR) {
				level = Level.SEVERE;
			} else if (logLevel == LogService.LOG_WARNING) {
				level = Level.WARNING;
			} else if (logLevel == LogService.LOG_INFO) {
				level = Level.INFO;
			} 
			outputLog(null, level, msg);
		}
		return msg;
	}
		
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getString(final String key) {
		return getString(key, logBundle, LOG_PROPERTIES_FILE_NAME );
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(logBundle, extendedPrefix);
	}

	@Override
	protected Color getFontColor(Display display) {
		return display.getSystemColor(SWT.COLOR_RED);
	}

	public String outputLog(String exdendedPrefix, String msg) {
		return outputLog(exdendedPrefix, Level.INFO, msg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, logBundle);
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputView(String key, String msg) {
		return outputView(key, msg, logBundle);
	}

	public void addLoglistener(LogListener logListener, LogFilter logFilter) {
		Activator.getDefault().addLoglistener(logListener, logFilter);
	}
	
	public void removeLoglistener(LogListener logListener) {
		Activator.getDefault().removeLoglistener(logListener);
	}
}
