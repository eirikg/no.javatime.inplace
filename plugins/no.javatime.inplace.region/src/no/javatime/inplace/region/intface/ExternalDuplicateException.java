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




public class ExternalDuplicateException extends BaseException {

	private static final long serialVersionUID = 8081640599616522968L;

	public ExternalDuplicateException () {
		super();	
	}
	
	public ExternalDuplicateException(String msg) {
		super(msg);
	}

	public ExternalDuplicateException(Throwable tex, String msg) {
		super(msg, tex);
	}

	public ExternalDuplicateException(Throwable tex, String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions), tex);
	}
	
	public ExternalDuplicateException(String key, Object ... substitutions) {
		super(ExceptionMessage.getInstance().formatString(key, substitutions));
	}
	
	
}
