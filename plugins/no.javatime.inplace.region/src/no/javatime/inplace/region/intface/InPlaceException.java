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
package no.javatime.inplace.region.intface;

import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.util.messages.ExceptionMessage;

/**
 * Formats, outputs and logs exception messages.
 * Output is forwarded to {@linkplain no.javatime.util.messages.ExceptionMessage}
 * and sent to devices defined by {@code ExceptionMessage}   
 *
 */
public class InPlaceException extends RuntimeException {

	private static final long serialVersionUID = -6632902141188744336L;
	
	public InPlaceException () {
		super();	
	}

	/**
	 * Forwards the exception
	 * 
	 * @param tex the current thrown exception
	 */
	public InPlaceException (Throwable tex) {
		super(tex);	
	}
	
	/**
	 * Formats the message based on message key
	 * 
	 * @param tex the current thrown exception
	 * @param key to access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved message
	 */
	public InPlaceException(Throwable tex, String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions), tex);
	}
	
	/**
	 * Forwards the specified message
	 * 
	 * @param msg message to log
	 */
	public InPlaceException(String msg) {
		super(msg);
	}

	public InPlaceException(IBundleStatus  status) {
		super(status.getMessage(), status.getException());
	}

	/**
	 * Formats the message based on message key
	 * 
	 * @param key to access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved message
	 */
	public InPlaceException(String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions));
	}
}
