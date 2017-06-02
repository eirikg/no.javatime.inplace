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

import no.javatime.util.Activator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Access messages used by trace functions. The more general class for access of key/value pairs can
 * be found in {@link Message}.
 */
public class TraceMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = TraceMessage.class.getName();
	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String TRACE_PROPERTIES_FILE_NAME = "no.javatime.util.messages.tracemessages"; //$NON-NLS-1$

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
			String msg = ID + ": Can not find Property file " + TRACE_PROPERTIES_FILE_NAME
					+ ". It may have been deleted or moved.";
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
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
		return getString(key, traceBundle, TRACE_PROPERTIES_FILE_NAME);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, traceBundle);
	}
}
