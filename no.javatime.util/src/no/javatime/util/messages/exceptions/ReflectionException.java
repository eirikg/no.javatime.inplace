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

/**
 * Reports exceptions raised by the reflection library
 */
public class ReflectionException extends LogException {
	
	public static String ID = ReflectionException.class.getName();

	private static final long serialVersionUID = 2965688448911432073L;

	public ReflectionException(String key, Object ... substitutions) {
		super(key, substitutions);
	}	
	public ReflectionException(Exception e, String key, Object ... substitutions) {
		super(e, key, substitutions);
	}	

}
