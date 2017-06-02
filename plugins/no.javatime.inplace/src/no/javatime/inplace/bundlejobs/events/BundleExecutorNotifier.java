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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEvent;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventListener;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.statushandlers.StatusManager;

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
		// execute(evt);
		for (Iterator<BundleExecutorEventListener> iterator = jobListeners.iterator(); iterator.hasNext();) {
			iterator.next().bundleJobEvent(evt);
		}
	}
	
	public void addBundleJob(BundleExecutor bundleExecutor) {
		final BundleExecutorEvent event = new BundleExecutorEventImpl(this, bundleExecutor, 0);
		fireJobEvent(event);
	}

	public void addBundleJob(BundleExecutor bundleExecutor, long delay) {
		BundleExecutorEvent event = new BundleExecutorEventImpl(this, bundleExecutor, delay);
		fireJobEvent(event);
	}
	
	private synchronized void execute(final BundleExecutorEvent event) {

		ExecutorService executor = Executors.newSingleThreadExecutor();
		// Run in a separate thread while waiting for the builder to finish
		try {
			executor.execute(new Runnable() {

				/**
				 * Wait on builder to finish before passing the job to listeners
				 */
				@Override
				public void run() {
					try {
						Activator.getResourceStateService().waitOnBuilder(false);
						for (Iterator<BundleExecutorEventListener> iterator = jobListeners.iterator(); iterator.hasNext();) {
							iterator.next().bundleJobEvent(event);
						}
					} catch (ExtenderException e) {
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
								StatusManager.LOG);
					}
				}
			});
		} catch (RejectedExecutionException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			executor.shutdown();
		}
	}

}
