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

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEvent;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;

import org.eclipse.core.resources.WorkspaceJob;

public class BundleExecutorEventImpl extends EventObject implements BundleExecutorEvent {

	private static final long serialVersionUID = 1593783115734613639L;
	private transient BundleExecutor bundleExecutor;
	private long delay = 0;

	public BundleExecutorEventImpl(Object source, BundleExecutor bundleJobExecutor, long delay) {
		super(source);
		this.bundleExecutor = bundleJobExecutor;
		this.delay = delay;		
	}

	@Override
	public WorkspaceJob getJob() {
		return bundleExecutor.getJob();
	}
	
	@Override
	public int getJobState() {
		
		return bundleExecutor.getJob().getState();
	}
	
	@Override
	public BundleExecutor getBundlExecutor() {

		return bundleExecutor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent#getDelay()
	 */
	@Override
	public long getDelay() {
		return delay;
	}
}
