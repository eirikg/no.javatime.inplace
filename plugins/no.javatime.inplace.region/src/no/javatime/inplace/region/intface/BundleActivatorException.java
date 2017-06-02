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

import no.javatime.inplace.region.manager.BaseException;
import no.javatime.util.messages.ExceptionMessage;

public class BundleActivatorException extends BaseException {
	
	private static final long serialVersionUID = 5769222856646228959L;

	public BundleActivatorException () {
		super();	
	}
	public BundleActivatorException(Throwable tex, String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions), tex);
	}
	
	public BundleActivatorException(String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions));
	}
	
	
}
