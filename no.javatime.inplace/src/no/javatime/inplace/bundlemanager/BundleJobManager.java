package no.javatime.inplace.bundlemanager;

import no.javatime.inplace.bundlejobs.events.BundleJobEventListener;
import no.javatime.inplace.bundlejobs.events.BundleJobNotifier;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.WorkspaceJob;

public class BundleJobManager {
	
	/**
	 * Management of bundle jobs listeners.
	 */
	private static BundleJobNotifier bundleJobNotifier = new BundleJobNotifier();

	public BundleJobManager() {
	}
	
	
	public static BundleRegion getRegion() {
		return BundleManager.getRegion();
	}
	
	public static BundleCommand getCommand() {
		return BundleManager.getCommand();
	}

	public static BundleTransition getTransition() {
		return BundleManager.getTransition();
	}
	
	/**
	 * Adds a bundle job managed by job listeners
	 * @param bundleJob bundle job to add for later scheduling by listeners
	 * @param delay number of msec before starting job
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
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEventListener
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobNotifier
	 */
	public static void addBundleJobListener(BundleJobEventListener listener) {

		bundleJobNotifier.addBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job_listener",
					listener.getClass().getName());					
	}

	/**
	 * Removes a listener for added bundle jobs
	 * @param listener remove this listener from listen to bundle jobs 
	 * @see #addBundleJob(WorkspaceJob, long)
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEvent
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobEventListener
	 * @see no.javatime.inplace.bundlejobs.events.BundleJobNotifier
	 */
	public static void removeBundleJobListener(BundleJobEventListener listener) {
		
		bundleJobNotifier.removeBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("removed_job_listener",
					listener.getClass().getName());					
	}
}
