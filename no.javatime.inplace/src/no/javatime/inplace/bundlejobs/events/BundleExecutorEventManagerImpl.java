package no.javatime.inplace.bundlejobs.events;

import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventListener;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

public class BundleExecutorEventManagerImpl implements BundleExecutorEventManager {
	
	/**
	 * Management of bundle jobs listeners.
	 */
	private static BundleExecutorNotifier bundleJobNotifier = new BundleExecutorNotifier();

	public BundleExecutorEventManagerImpl() {
	}
		
	@Override
	public void add(BundleExecutor bundleExecutor) {
		add(bundleExecutor, 0);
	}

	@Override
	public synchronized void add(BundleExecutor bundleExecutor, long delay) {

		if (bundleJobNotifier.jobListeners() == 1) {
			// Fallback. If no other listeners are installed
			// the default is to schedule the job.
			bundleExecutor.getJob().schedule(delay);
			return;
		}
		bundleJobNotifier.addBundleJob(bundleExecutor, delay);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job",
					bundleExecutor.getName());					
	}

	@Override
	public synchronized void addListener(BundleExecutorEventListener listener) {

		bundleJobNotifier.addBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("added_job_listener",
					listener.getClass().getName());					
	}

	@Override
	public synchronized void removeListener(BundleExecutorEventListener listener) {
		
		bundleJobNotifier.removeBundleJobListener(listener);
		if (Category.DEBUG && Category.getState(Category.listeners))
			TraceMessage.getInstance().getString("removed_job_listener",
					listener.getClass().getName());					
	}
}
