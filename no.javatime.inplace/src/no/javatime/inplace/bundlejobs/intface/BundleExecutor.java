package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * Common sub service interface to initiate and control execution of bundle life cycle operations. A
 * bundle operation may be scheduled for execution by invoking {@link #run()}, {@link #run(long)} or
 * scheduling the job directly by accessing the underlying job ({@code #getJob()}) of the bundle
 * executor service.
 * <p>
 * All bundle service operations are scheduled as jobs and compatible with the {@link Job Jobs} API.
 * To manage the job of a bundle service operation to run, use the jobs API and access the
 * underlying workspace job of a bundle service operation with the {@link #getJob()} method.
 * <p>
 * There is a sub service interface for each bundle operation. Examples are {@link ActivateProject}
 * to activate a project and start the bundle associated with the activated project and
 * {@link ActivateBundle} to install, resolve and start an already activated project.
 * <p>
 * Add bundle projects to process and use other member methods, including job related methods, to
 * alter and control the state of a bundle executor before executing an operation and interrogate
 * status and error information during execution and after the executor terminates by joining the
 * bundle job {@link #joinBundleExecutor()} or by using a job listener.
 * <p>
 * All bundle operation services (sub interfaces of this interface) must be registered with
 * prototype service scope. Use the provided default {@link BundleExecutorServiceFactory prototype
 * service factory} or create a customized factory for bundle service operations.
 * <p>
 */
public interface BundleExecutor {

	/**
	 * Common family job name for all bundle executor operations
	 */
	public static final String FAMILY_BUNDLE_LIFECYCLE = "BundleFamily";

	/**
	 * Schedules a bundle executor to run with a delay.
	 * <p>
	 * This method is otherwise equal to {@code getJob().schedule(long)}
	 * 
	 * @param delay Delay in milliseconds before to run the bundle operation
	 * @see Job#schedule(long)
	 */
	public void run(long delay);

	/**
	 * Schedules a bundle executor to run.
	 * <p>
	 * This is a convenience method to execute a bundle operation otherwise equal to
	 * {@link #run(long)} and {@code getJob().schedule()}
	 * 
	 * @see Job#schedule()
	 */
	public void run();

	/**
	 * Wait for the scheduled bundle operation to finish by joining it.
	 * <p>
	 * This is a convenience method to join a bundle operation otherwise equal to
	 * {@code getJob().join()}) or
	 * {@link IJobManager#join(Object, org.eclipse.core.runtime.IProgressMonitor)}
	 * 
	 * @exception InterruptedException if the underlying thread of this job is interrupted while
	 * waiting
	 * @see Job#join()
	 */
	public void joinBundleExecutor() throws InterruptedException;

	/**
	 * Get the name of this bundle executor. This is the default name, the name set with
	 * {@code getJob().setName(String)} or the name specified when this service was registered.
	 * <p>
	 * This is a convenience method to obtain the name of a bundle operation and otherwise equal to
	 * {@code getJob().getName()}
	 * 
	 * @return the name of this bundle operation
	 * @see Job#getName()
	 * @see Job#setName(String)
	 */
	public String getName();

	/**
	 * Access the underlying workspace job of this bundle executor service.
	 * <p>
	 * Scheduling a job with {@code getJob().schedule()} from the returned {@link WorkspaceJob} is
	 * otherwise identical to {@link #run()}.
	 * 
	 * @return the job to be scheduled for this bundle operation service
	 */
	public WorkspaceJob getJob();

	/**
	 * Determine if a bundle that is member of the workspace region is currently executing a bundle
	 * operation (state changing).
	 * 
	 * @return the bundle currently executing a bundle operation or null if no operation is running
	 */
	public Bundle isStateChanging();

	/**
	 * Stop the current bundle operation
	 * <p>
	 * Stops a life cycle operation currently execution while moving from one state to the next. If
	 * the operation is aborted the state and transition (operation) is rolled back to the state it
	 * had before the transition was issued.
	 * 
	 * @return If the operation was stopped return true, otherwise false
	 * @throws ExtenderException if failing to get the required bundle service(s)
	 * @see #isStateChanging()
	 */
	public boolean stopCurrentOperation() throws ExtenderException;

	/**
	 * Add a pending bundle project to execute by this bundle service
	 * 
	 * @param project bundle project to execute
	 */
	public void addPendingProject(IProject project);

	/**
	 * Add a collection of pending bundle projects to execute by this bundle service.
	 * <p>
	 * To maintain the order of projects to add use {@link #resetPendingProjects(Collection)}
	 * 
	 * @param projects bundle projects to execute
	 * @see #resetPendingProjects(Collection)
	 */
	public void addPendingProjects(Collection<IProject> projects);

	/**
	 * Replace existing pending projects with the specified collection. The specified collection is
	 * copied and the order of the projects is maintained
	 * 
	 * @param projects bundle projects to replace
	 */
	public void resetPendingProjects(Collection<IProject> projects);

	/**
	 * Removes a pending bundle project from the collection of bundle projects to execute
	 * 
	 * @param project bundle project to remove
	 */
	public void removePendingProject(IProject project);

	/**
	 * Remove the pending bundle projects from the bundle project list to execute
	 * 
	 * @param projects bundle projects to remove
	 */
	public void removePendingProjects(Collection<IProject> projects);

	/**
	 * Clears all pending bundle projects from the bundle project list to execute
	 */
	public void clearPendingProjects();

	/**
	 * Determine if the specified bundle project is a pending bundle project
	 * 
	 * @param project bundle project to check for pending status
	 * @return true if the specified bundle project is a pending bundle project, otherwise false.
	 */
	public Boolean isPendingProject(IProject project);

	/**
	 * Checks if there are any pending bundle projects to execute.
	 * 
	 * @return true if there are any bundle projects to execute, otherwise false
	 */
	public Boolean hasPendingProjects();

	/**
	 * Returns all pending bundle projects to execute.
	 * <p>
	 * The set is unguarded (not returning a copy of the pending projects) 
	 * 
	 * @return the set of pending bundle projects or an empty set
	 */
	public Collection<IProject> getPendingProjects();

	/**
	 * Number of pending bundle projects to execute
	 * 
	 * @return number of pending bundle projects
	 */
	public int pendingProjects();

	/**
	 * Get all logged status objects added by this job
	 * 
	 * @return a list of status objects or an empty list
	 * @see BundleLog#enableLogging(boolean)
	 * @see MessageOptions#setIsBundleOperations(boolean)
	 */
	public Collection<IBundleStatus> getLogStatusList();

	/**
	 * Get all error status information added by this job
	 * <p>
	 * Errors are sent to the error log and the bundle log
	 * 
	 * @return a list of status objects where each status object describes the nature of the status or
	 * an empty list
	 */
	public Collection<IBundleStatus> getErrorStatusList();

	/**
	 * Check if any bundle error status objects have been added to the error status list
	 * 
	 * @return true if bundle error status objects exists in the status list, otherwise false
	 */
	public boolean hasErrorStatus();

	/**
	 * Add a bundle status. The bundle status is logged if the logging option is on, and also sent to
	 * the error log if status is set to {@code StatusCode.ERROR} or {@code StatusCode.EXCEPTION}
	 * <p>
	 * 
	 * @param status the status object added to the log status list should contain at least the bundle
	 * and/or the project related to the status message
	 * @see getLogStatusList()
	 */
	public IBundleStatus addLogStatus(IBundleStatus status);

	/**
	 * Creates a multi status object containing the specified status object as the parent and all
	 * status objects in the status list as children. The status list is then cleared and the new
	 * multi status object is added to the list.
	 * 
	 * @param multiStatus a status object where all existing status objects are added to as children
	 */
	public IBundleStatus createMultiStatus(IBundleStatus multiStatus);

}