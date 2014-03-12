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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * This is a specialization for accessing messages used by trace functions. By
 * default all accessed strings are forwarded to the
 * {@link no.javatime.util.messages.log.MessageLog}. The more general class for
 * access of key/value pairs can be found in {@link Message}.
 * <p>
 * There are other classes for different specialized or categorized messages.
 * </p>
 */
public class TraceMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = TraceMessage.class.getName();
	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String TRACE_PROPERTIES_FILE_NAME = 
		"no.javatime.util.messages.tracemessages"; //$NON-NLS-1$

	// Assignment of the tracemessages.properties property file as a resource
	// bundle
	private static ResourceBundle traceBundle = null;

	private static TraceMessage instance = null;
	
	/**
	 * Prevent outside, not inherited classes from instantiation.
	 */
	protected TraceMessage() {
		try {
			traceBundle = ResourceBundle.getBundle(TRACE_PROPERTIES_FILE_NAME);
			initDevice(traceBundle);			
			initPrefix(traceBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + TRACE_PROPERTIES_FILE_NAME + 
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
	public synchronized static TraceMessage getInstance() {
		if (instance == null) {
			instance = new TraceMessage();
		}
		return instance;
	}
		
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getString(final String key) {
		return getString(key, traceBundle, TRACE_PROPERTIES_FILE_NAME );
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(traceBundle, extendedPrefix);
	}

	@Override
	protected Color getFontColor(Display display) {
		return display.getSystemColor(SWT.COLOR_DARK_BLUE);
	}

	public String outputLog(String exdendedPrefix, String msg) {
		return outputLog(exdendedPrefix, Level.INFO, msg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, traceBundle);
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputView(String key, String msg) {
		return outputView(key, msg, traceBundle);
	}
}
