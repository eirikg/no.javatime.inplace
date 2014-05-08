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
package no.javatime.inplace.bundlemanager;


public class BundleStateChangeException extends ExtenderException {

	private static final long serialVersionUID = 4698034779808044676L;

	
	public BundleStateChangeException () {
		super();	
	}
	public BundleStateChangeException(Throwable tex, String key, Object ... substitutions) {
		super(tex, key, substitutions);
	}
	
	public BundleStateChangeException(String key, Object ... substitutions) {
		super(key, substitutions);
	}
	
	
}
