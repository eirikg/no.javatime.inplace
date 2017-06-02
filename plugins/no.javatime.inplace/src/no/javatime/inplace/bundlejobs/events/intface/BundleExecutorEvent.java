package no.javatime.inplace.bundlejobs.events.intface;

import no.javatime.inplace.bundlejobs.intface.BundleExecutor;

import org.eclipse.core.resources.WorkspaceJob;

public interface BundleExecutorEvent {

	/**
	 * The workspace job of the added bundle executor
	 * 
	 * @return Job to schedule
	 */
	public WorkspaceJob getJob();

	
	public int getJobState();

	/**
	 * The bundle executor added for execution by
	 * {@link BundleExecutorEventManager#add(BundleExecutor)}
	 * 
	 * @return Bundle executor to execute
	 */
	public BundleExecutor getBundlExecutor();

	/**
	 * Delay in milliseconds before execution
	 * 
	 * @return Milliseconds before execution
	 */
	public long getDelay();

}