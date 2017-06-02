/*******************************************************************************
 * Copyright (c) 2014 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.log.intface;


/**
 * Exception thrown when logging status objects to the bundle log.
 */
public class BundleLogException extends RuntimeException {

	private static final long serialVersionUID = -2997898089574174020L;

	/**
	 * Default log exception constructor
	 */
	public BundleLogException() {
		super();
	}

	/**
	 * Creates a log exception accepting a current exception
	 * 
	 * @param e the current thrown exception
	 */
	public BundleLogException(Throwable e) {
		super(e);
	}

	/**
	 * Creates a log exception accepting a current exception message
	 * 
	 * @param msg exception message
	 */
	public BundleLogException(String msg) {
		super(msg);
	}

	/**
	 * Creates a log exception accepting a current exception and a pattern to format as an exception
	 * message
	 * 
	 * @param e the current thrown exception
	 * @param msg creates a message with the given pattern and uses it to format the given
	 * substitutions
	 */
	public BundleLogException(Throwable e, String msg) {
		super(msg, e);
	}
}
