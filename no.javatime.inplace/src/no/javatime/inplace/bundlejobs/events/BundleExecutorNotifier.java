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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEvent;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventListener;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;

class BundleExecutorNotifier {

	protected Collection<BundleExecutorEventListener> jobListeners = Collections
			.synchronizedList(new ArrayList<BundleExecutorEventListener>());;

	public synchronized int jobListeners() {
		return jobListeners.size();
	}

	public synchronized void addBundleJobListener(BundleExecutorEventListener listener) {
		jobListeners.add(listener);
	}

	public synchronized void removeBundleJobListener(BundleExecutorEventListener listener) {
		if (!jobListeners.isEmpty()) {
			jobListeners.remove(listener);
		}
	}

	protected synchronized void fireJobEvent(BundleExecutorEvent evt) {
		for (Iterator<BundleExecutorEventListener> iterator = jobListeners.iterator(); iterator.hasNext();) {
			iterator.next().bundleJobEvent(evt);
		}
	}
	
	public void addBundleJob(BundleExecutor bundleExecutor) {
		BundleExecutorEvent event = new BundleExecutorEventImpl(this, bundleExecutor, 0);
		fireJobEvent(event);
	}

	public void addBundleJob(BundleExecutor bundleExecutor, long delay) {
		BundleExecutorEvent event = new BundleExecutorEventImpl(this, bundleExecutor, delay);
		fireJobEvent(event);
	}
}
