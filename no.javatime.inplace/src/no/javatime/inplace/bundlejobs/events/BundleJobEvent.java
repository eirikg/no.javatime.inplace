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
package no.javatime.inplace.bundlejobs.events;

import java.util.EventObject;

import org.eclipse.core.resources.WorkspaceJob;

public class BundleJobEvent extends EventObject {
	
	private static final long serialVersionUID = 1593783115734613639L;
	private transient WorkspaceJob bundleJob;
	private long delay = 0;
	
	public BundleJobEvent(Object source, WorkspaceJob bundleJob, long delay) {
    super(source);
    this.bundleJob = bundleJob;	
    this.delay = delay;
	}
		public BundleJobEvent(Object source, WorkspaceJob bundleJob) {
    super(source);
    this.bundleJob = bundleJob;
	}
		
  public WorkspaceJob getBundleJob() {
		return bundleJob;
	}

  public long getDelay() {
		return delay;
	}
}
