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

import org.eclipse.core.resources.WorkspaceJob;

public class BundleJobNotifier {

	protected Collection<BundleJobEventListener> jobListeners = Collections
			.synchronizedList(new ArrayList<BundleJobEventListener>());;

	synchronized public int jobListeners() {
		return jobListeners.size();
	}

	synchronized public void addBundleJobListener(BundleJobEventListener listener) {
		jobListeners.add(listener);
	}

	public synchronized void removeBundleJobListener(BundleJobEventListener listener) {
		if (!jobListeners.isEmpty()) {
			jobListeners.remove(listener);
		}
	}

	protected synchronized void fireJobEvent(BundleJobEvent evt) {
		for (Iterator<BundleJobEventListener> iterator = jobListeners.iterator(); iterator.hasNext();) {
			iterator.next().bundleJobEvent(evt);
		}
	}
	public void addBundleJob(WorkspaceJob workspaceJob, long delay) {
		BundleJobEvent event = new BundleJobEvent(this, workspaceJob, delay);
		fireJobEvent(event);
	}

	public void addBundleJob(WorkspaceJob workspaceJob) {
		BundleJobEvent event = new BundleJobEvent(this, workspaceJob);
		fireJobEvent(event);
	}
}
