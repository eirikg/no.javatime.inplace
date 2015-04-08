package no.javatime.inplace.bundlejobs.events.intface;

import no.javatime.inplace.bundlejobs.intface.BundleExecutor;

/**
 * Service to execute bundle executors operations and listen to bundle executors being added before they are
 * scheduled.
 * <p>
 * To listen to jobs after they are scheduled use the Jobs API. Use {@link BundleExecutor#getJob()}
 * or {@link BundleExecutorEvent#getJob()} to get the workspace job of a bundle executor
 * <p>
 * 
 * Use this bundle job event manager as a singleton service, implement and add the
 * {@link BundleExecutorEventListener} interface to the event manager and begin receive
 * {@link BundleExecutorEvent] events objects passed as a parameter to the implemented listener.
 */
public interface BundleExecutorEventManager {

	/**
	 * Manifest header for accessing the default implementation class name of the the bundle job event
	 * manager service
	 */
	public final static String BUNDLE_EXECUTOR_EVENT_MANAGER_SERVICE = "Bundle-Executor-Event-Manager-Service";

	/**
	 * This is a convenience method for {@link #add(BundleExecutor, long)} with no delay
	 * 
	 * @param bundleExecutor bundle executor operation to add for later execution by listeners
	 */
	public void add(BundleExecutor bundleExecutor);

	/**
	 * Adds a bundle executor operation managed by job listeners
	 * 
	 * @param bundleExecutor bundle executor operation to add for later execution by listeners
	 * @param delay number of msec before executing bundle operation
	 */
	public void add(BundleExecutor bundleExecutor, long delay);

	/**
	 * Adds a listener for added bundle jobs
	 * 
	 * @param listener add this listener to listening for bundle jobs
	 */
	public void addListener(BundleExecutorEventListener listener);

	/**
	 * Removes a listener for added bundle jobs
	 * 
	 * @param listener remove this listener from listen to bundle jobs
	 */
	public void removeListener(BundleExecutorEventListener listener);
}