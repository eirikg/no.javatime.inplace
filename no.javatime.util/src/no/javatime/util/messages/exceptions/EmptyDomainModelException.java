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
public class EmptyDomainModelException extends LogException {
	
	public static String ID = EmptyDomainModelException.class.getName();
	private static final long serialVersionUID = -6178404473996166703L;

	public EmptyDomainModelException() {
		super();
	}
	public EmptyDomainModelException(Object ... substitutions) {
		super("empty_class_model", substitutions);
	}	
}
