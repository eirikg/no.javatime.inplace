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
package no.javatime.util.messages.exceptions;

import no.javatime.util.messages.ErrorMessage;

/**
 * Exception which displays a message in the message view and logs messages and exceptions.
 * Uses ErrorMessage property file to display user messages in the view, and uses the
 * ExceptionMessage property file to log the exception. 
 */
public class ViewException extends LogException {
	
	/**
	 * Unique ID of this class
	 */
	public static String ID = ViewException.class.getName();

	/**
	 *	Unique ID for serialization of this class 
	 */
	private static final long serialVersionUID = -6389537721990031774L;

	/**
	 * Log exception and log and display message in message view based on message key
	 * @param e the current thrown exception
	 * @param key access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved messages
	 */
	public ViewException(Exception e, String key, Object ... substitutions) {
		super(e, key, substitutions);
		ErrorMessage.getInstance().getString(key, substitutions);
	}

	/**
	 * Log message and display message in message view based on message key
	 * @param key access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved messages
	 */
	public ViewException(String key, Object ... substitutions) {
		super(key, substitutions);
		ErrorMessage.getInstance().getString(key, substitutions);
	}
}
