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
package no.javatime.util.messages.log;

import no.javatime.util.messages.Message;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class BundleLogListener implements LogListener {

	@Override
	public void logged(LogEntry entry) {
	   if (entry.getMessage() != null) {
	  	 
       Message.getInstance().getString(Message.defKey, 
      		 "[" + entry.getBundle().getSymbolicName() + "] " +  entry.getMessage());
	   }
	}

}
