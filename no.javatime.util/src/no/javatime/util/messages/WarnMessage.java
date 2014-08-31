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
 * Access warning messages. The more general class for access of key/value pairs can be found in
 * {@link Message}.
 */
public class WarnMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = WarnMessage.class.getName();

	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String WARN_PROPERTIES_FILE_NAME = "no.javatime.util.messages.warnmessages"; //$NON-NLS-1$

	/**
	 * Assignment of the "errormessages.properties" property file as a resource bundle
	 */
	private static ResourceBundle warnBundle = null;

	private static WarnMessage instance = null;

	/**
	 * Prevent outside not inherited classes from instantiation.
	 */
	protected WarnMessage() {
		try {
			warnBundle = ResourceBundle.getBundle(WARN_PROPERTIES_FILE_NAME);
			initDevice(warnBundle);
			initPrefix(warnBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + WARN_PROPERTIES_FILE_NAME
					+ ". It may have been deleted or moved.";
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>ErrorMessage</code>
	 */
	public static WarnMessage getInstance() {
		if (instance == null) {
			instance = new WarnMessage();
		}
		return instance;
	}

	/*
	 * public void handleWarningMessage(String msg) { handleWarningMessage(msg);
	 * 
	 * }
	 */
	public void handleMessage(String msg) {

		if (null == msg) {
			msg = "";
		}
		if (Category.DEBUG) {
			StackTraceElement frame = getCallerMetaInfo();
			getString("log_stack_frame", frame.getClassName(), frame.getMethodName());
			// getString("extended_log", frame.getClassName(), frame.getMethodName(), msg);
		}
		getString("log_message", msg);
	}

	@Override
	protected String getString(final String key) {
		return getString(key, warnBundle, WARN_PROPERTIES_FILE_NAME);
	}

	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(warnBundle, extendedPrefix);
	}

	@Override
	protected Color getFontColor(Display display) {
		return display.getSystemColor(SWT.COLOR_DARK_RED);
	}

	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, warnBundle);
	}
}
