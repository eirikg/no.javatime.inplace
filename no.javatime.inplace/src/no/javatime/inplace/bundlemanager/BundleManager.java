package no.javatime.inplace.bundlemanager;

import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlejobs.events.BundleJobNotifier;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.events.BundleTransitionNotifier;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.BundleWorkspaceImpl;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.WorkspaceJob;

public class BundleManager {
	
	private static boolean isInitialized;

	/**
	 * Management of bundle jobs listeners.
	 */
	private static BundleJobNotifier bundleJobNotifier = new BundleJobNotifier();
	private static BundleTransitionNotifier bundleTransitionNotifier = new BundleTransitionNotifier();

	public BundleManager() {
	}
	
	private static boolean init() {
		
		if (!isInitialized) {
			// BundleCommandImpl.INSTANCE.init();
			isInitialized = true;
		}
		return isInitialized;
	}
	
	private static boolean dispose() {
		
		if (isInitialized) {
			// BundleCommandImpl.INSTANCE.unregisterResolverHook();
			isInitialized = false;
		}
		return isInitialized;
	}
	
	public static BundleRegion getRegion() {
		return BundleWorkspaceImpl.INSTANCE;
	}
	
	public static BundleCommand getCommand() {
		return BundleCommandImpl.INSTANCE;
	}

	public static BundleTransition getTransition() {
		return BundleTransitionImpl.INSTANCE;
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
	
	/**
	 * Adds a bundle job managed by job listeners
	 * @param bundleJob bundle job to add for later scheduling by listeners
	 * @param delay TODO
	 * @see #addBundleTransitionListener(BundleTransitionEventListener)
	 * @see #removeBundleTransitionListener(BundleTransitionEventListener)
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEventListener
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobNotifier
	 */
	public static void addBundleJob(WorkspaceJob bundleJob, long delay) {
		if (bundleJobNotifier.jobListeners() == 1) {
			// Fallback. If no other listeners are installed
			// the default is to schedule the job.
			bundleJob.schedule(delay);
		}
		bundleJobNotifier.addBundleJob(bundleJob, delay);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job",
					bundleJob.getName());					
	}

	/**
	 * Adds a listener for added bundle jobs
	 * @param listener add this listener to listening for bundle jobs
	 * @see #addBundleJob(WorkspaceJob, long)
	 * @see #removeBundleTransitionListener(BundleTransitionEventListener)
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEventListener
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobNotifier
	 */
	public static void addBundleJobListener(BundleJobEventListener listener) {
		if (bundleJobNotifier.jobListeners() == 0) {
			init();
		}
		bundleJobNotifier.addBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job_listener",
					listener.getClass().getName());					
	}

	/**
	 * Removes a listener for added bundle jobs
	 * @param listener remove this listener from listen to bundle jobs 
	 * @see #addBundleJob(WorkspaceJob, long)
	 * @see #addBundleTransitionListener(BundleTransitionEventListener)
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEventListener
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobNotifier
	 */
	public static void removeBundleJobListener(BundleJobEventListener listener) {
		if (bundleJobNotifier.jobListeners() == 1) {
			dispose();
		}
		bundleJobNotifier.removeBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("removed_job_listener",
					listener.getClass().getName());					
	}
}
