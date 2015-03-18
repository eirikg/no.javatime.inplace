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
package no.javatime.inplace.extender.intface;

import java.text.MessageFormat;


/**
 * Formats, outputs and logs exception messages.
 * Output is forwarded to {@linkplain no.javatime.util.messages.ExceptionMessage}
 * and sent to devices defined by {@code ExceptionMessage}   
 *
 */
public class ExtenderException extends RuntimeException {

	private static final long serialVersionUID = -6632902141188744336L;
	private boolean isNullPointer;
	
	public ExtenderException () {
		super();	
	}

	/**
	 * Outputs and logs exception
	 * 
	 * @param tex the current thrown exception
	 */
	public ExtenderException (Throwable tex) {
		super(tex);	
	}
	
	public ExtenderException(String msg) {
		super(msg);
	}

	/**
	 * Outputs and logs exception and message based on message key
	 * 
	 * @param tex the current thrown exception
	 * @param pattern to access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved message
	 */
	public ExtenderException(Throwable tex, String pattern, Object ... substitutions) {
		super(format(pattern, substitutions), tex);
	}
	
	/**
	 * Outputs and logs a message based on message key
	 * 
	 * @param pattern to access message from resource bundle
	 * @param substitutions message strings to insert into the retrieved message
	 */
	public ExtenderException(String pattern, Object ... substitutions) {
		super(format(pattern, substitutions));
	}

	public boolean isNullPointer() {
		return isNullPointer;
	}

	public void setNullPointer(boolean isNullPointer) {
		this.isNullPointer = isNullPointer;
	}


	/**
	 * Formats a message based on a pattern to format using a list of substitutions
	 * <p>
	 * If a formatting error occurs, substitute the resulting message with an error message.
	 * 
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or if a formating error occurs, a verbose description of the
	 * formatting error
	 */
	static private String format(String pattern, Object ... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
		} catch (IllegalArgumentException e) {
			if (null != pattern) {
				msg = MessageFormat.format("Failed to format: {0}", pattern);
			} else {
				msg = "Failed to format message with null pattern";
			}
		}
		return msg;
	}
}
