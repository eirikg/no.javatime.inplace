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



/**
 * Uses the exception messages property file to get the exception message text
 * based on resource bundle keys 
 */
public class ResourceException extends RuntimeException {
	
	/**
	 * Unique ID of this class
	 */
	public static String ID = ResourceException.class.getName();

	/**
	 *	Unique ID for serialization of this class 
	 */
	private static final long serialVersionUID = -6389537721990031774L;

	/**
	 * Get the message text based on the message key
	 * 
	 * @param e the current thrown exception
	 * @param key access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved messages
	 */
	public ResourceException(Exception e, String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions), e);
	}

	/**
	 * Get the message text based on the message key
	 * 
	 * @param key access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved messages
	 */
	public ResourceException(String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions));
	}
}
