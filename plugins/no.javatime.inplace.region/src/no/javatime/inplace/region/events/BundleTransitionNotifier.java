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
package no.javatime.inplace.region.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;


public class BundleTransitionNotifier {

	protected Collection<BundleTransitionEventListener> transitionListeners = Collections
			.synchronizedList(new ArrayList<BundleTransitionEventListener>());;

	synchronized public int jobListeners() {
		return transitionListeners.size();
	}

	synchronized public void addBundleTransitionListener(BundleTransitionEventListener listener) {
		transitionListeners.add(listener);
	}

	public synchronized void removeBundleTransitionListener(BundleTransitionEventListener listener) {
		if (!transitionListeners.isEmpty()) {
			transitionListeners.remove(listener);
		}
	}

	protected synchronized void fireTransitionEvent(BundleTransitionEvent evt) {
		for (Iterator<BundleTransitionEventListener> iterator = transitionListeners.iterator(); iterator.hasNext();) {
			iterator.next().bundleTransitionChanged(evt);
		}
	}

	public void addBundleTransitionEvent(TransitionEvent transitionEvent) {
		BundleTransitionEvent event = new BundleTransitionEvent(this, transitionEvent);
		fireTransitionEvent(event);
	}
}
