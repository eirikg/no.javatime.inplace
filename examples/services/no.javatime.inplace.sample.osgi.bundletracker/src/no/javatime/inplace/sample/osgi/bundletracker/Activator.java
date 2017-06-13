/*******************************************************************************
 * Copyright (c) 2012, EclipseSource Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Elias Volanakis - initial API and implementation
 *******************************************************************************/
package no.javatime.inplace.sample.osgi.bundletracker;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Prints bundle life cycle changes to the console
 * <p>
 * Use the InPlace Bundle Activator Console to view output in the Eclipse instance where you
 * activate the sample bundle tracker. Make the Bundle Console visible from the Bundle main view
 */
public class Activator implements BundleActivator {

	private MyBundleTracker<?> bundleTracker;

	public void start(BundleContext context) throws Exception {
		System.out.println("Start tracking bundles");
		int trackStates = Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED
				| Bundle.UNINSTALLED;
		bundleTracker = new MyBundleTracker<>(context, trackStates, null);
		bundleTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Stop tracking bundles");
		bundleTracker.close();
		bundleTracker = null;
	}

	private final class MyBundleTracker<T> extends BundleTracker<T> {

		public MyBundleTracker(BundleContext context, int stateMask,
				BundleTrackerCustomizer<T> customizer) {
			super(context, stateMask, customizer);
		}

		/**
		 * Track the specified bundle according to the state mask of this bundle tracker
		 */
		public T addingBundle(Bundle bundle, BundleEvent event) {
			// Typically we would inspect bundle, to figure out if we want to
			// track it or not. If we don't want to track return null, otherwise
			// return the bundle object.
			print(bundle, event);
			@SuppressWarnings("unchecked")
			T result = (T) bundle;
			return result;
		}

		/**
		 * Print state change of the specified bundle
		 */
		public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
			print(bundle, event);
		}

		/**
		 * Print state change of the specified bundle
		 */
		public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
			print(bundle, event);
		}
	}

	/**
	 * Prints state and state change of the specified bundle to the console
	 * 
	 * @param bundle the bundle to print state information from
	 * @param event the state change of the specified bundle
	 */
	private void print(Bundle bundle, BundleEvent event) {
		String symbolicName = bundle.getSymbolicName();
		String state = stateAsString(bundle);
		String type = typeAsString(event);
		System.out.println("[BT] " + symbolicName + ", state: " + state + ", event.type: " + type);
	}

	/**
	 * Return the state of the specified bundle to a readable state name
	 * 
	 * @param bundle a bundle with a state
	 * @return the sate as a readable name
	 */
	private static String stateAsString(Bundle bundle) {
		if (bundle == null) {
			return "null";
		}
		int state = bundle.getState();
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		default:
			return "unknown bundle state: " + state;
		}
	}

	/**
	 * Return the state event of the specified bundle to a readable state event name
	 * 
	 * @param bundle the bundle who generated the event
	 * @return the sate event as a readable name
	 */
	private static String typeAsString(BundleEvent event) {
		if (event == null) {
			return "null";
		}
		int type = event.getType();
		switch (type) {
		case BundleEvent.INSTALLED:
			return "INSTALLED";
		case BundleEvent.LAZY_ACTIVATION:
			return "LAZY_ACTIVATION";
		case BundleEvent.RESOLVED:
			return "RESOLVED";
		case BundleEvent.STARTED:
			return "STARTED";
		case BundleEvent.STARTING:
			return "STARTING";
		case BundleEvent.STOPPING:
			return "STOPPING";
		case BundleEvent.STOPPED:
			return "STOPPED";
		case BundleEvent.UNINSTALLED:
			return "UNINSTALLED";
		case BundleEvent.UNRESOLVED:
			return "UNRESOLVED";
		case BundleEvent.UPDATED:
			return "UPDATED";
		default:
			return "unknown event type: " + type;
		}
	}
}
