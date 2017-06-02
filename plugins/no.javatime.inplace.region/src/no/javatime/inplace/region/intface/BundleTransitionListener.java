package no.javatime.inplace.region.intface;

import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.events.BundleTransitionNotifier;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

public class BundleTransitionListener {
	
	/**
	 * Management of bundle transition listeners.
	 */
	private static BundleTransitionNotifier bundleTransitionNotifier = new BundleTransitionNotifier();

	public BundleTransitionListener() {
	}	
	
	public static void addBundleTransition(TransitionEvent transitionEvent) {
		bundleTransitionNotifier.addBundleTransitionEvent(transitionEvent);
	}

	public static void addBundleTransitionListener(BundleTransitionEventListener listener) {
		bundleTransitionNotifier.addBundleTransitionListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job_listener",
					listener.getClass().getName());					
	}

	public static void removeBundleTransitionListener(BundleTransitionEventListener listener) {
		bundleTransitionNotifier.removeBundleTransitionListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("removed_job_listener",
					listener.getClass().getName());					
	}	
}
