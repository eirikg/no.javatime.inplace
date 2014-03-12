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
 * If an illegal value is returned from a method in a domain
 * class element during the execution of a simulation step.
 */
public class ExogenousReferenceException extends LogException {
	
	public static final String exogenousReference = "exogenous_endogenous_reference";

	public static String ID = ExogenousReferenceException.class.getName();

	private static final long serialVersionUID = -5340320362985494607L;

	public ExogenousReferenceException() {
		super();
	}
	public ExogenousReferenceException(Object ... substitutions) {
		super(exogenousReference, substitutions);
	}	

}
