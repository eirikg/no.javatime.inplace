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

/**
 * This is a specialization for accessing user messages. The more general class
 * for access of key/value pairs can be found in {@link Message}.
 * <p>
 * There are other classes in this package for different specialized or
 * categorized messages.
 * </p>
 */
public class UserMessage extends Message {

	/**
	 * Unique ID of the class
	 */
	public static String ID = UserMessage.class.getName();

	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String USER_PROPERTIES_FILE_NAME = 
		// FileLocation.DATA_SUBFOLDER + "usermessages"; //$NON-NLS-1$
		"no.javatime.util.messages.usermessages"; //$NON-NLS-1$

	// Assignment of the "usermessages.properties" property file as a resource
	// bundle
	private static ResourceBundle userBundle = null; 

	private static UserMessage instance = null;

	/**
	 * Prevent outside not inherited classes from instantiation.
	 */
	protected UserMessage() {
		setOutput(Output.nil);
		try {
			userBundle = ResourceBundle.getBundle(USER_PROPERTIES_FILE_NAME);
			initDevice(userBundle);
			initPrefix(userBundle);
			ignoreInStackFrame(ID);
		} catch (MissingResourceException e) {
			// Use inline text. Resource bundle may be missing.
			String msg = ID + ": Can not find Property file " + USER_PROPERTIES_FILE_NAME + 
			". It may have been deleted or moved.";
			getLogger().log(getLogger().getLevel(), msg, e);
			outputView(null, msg);
		}
	}

	/**
	 * This access the singleton
	 * @return the instance of the <code>UserMessage</class>
	 */
	public synchronized static UserMessage getInstance() {
		if (instance == null) {
			instance = new UserMessage();
		}
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getString(final String key) {
		return getString(key, userBundle, USER_PROPERTIES_FILE_NAME );
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(userBundle, extendedPrefix);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, userBundle);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputView(String key, String msg) {
		
		return outputView(key, msg, userBundle);
	}
}
