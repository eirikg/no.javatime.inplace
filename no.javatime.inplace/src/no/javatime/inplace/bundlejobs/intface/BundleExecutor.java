package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Common service interface to schedule bundle operations to run. A bundle operation is scheduled by
 * invoking {@link #execute()} or {@link #execute(long)}.
 * <p>
 * All bundle service operations are scheduled as jobs and compatible with the {@link Job Jobs} API.
 * To manage the job of a bundle service operation to execute, use the jobs API and access the
 * underlying workspace job of a bundle service operation with the {@link #getJob()} method.
 * <p>
 * There is a sub service interface for each bundle operation. Examples are {@link ActivateProject}
 * to activate a project and start the bundle associated with the activated project and
 * {@link ActivateBundle} to install, resolve and start an already activated bundle.
 * <p>
 * Add bundle projects to process and use other member methods, including job related methods, to
 * alter and control the state of a bundle executor before executing an operation and interrogate
 * all status and error information after the executor terminates by joining the bundle job
 * {@link #joinBundleExecutor()} or by using a job listener.
 * <p>
 * All bundle operation services (sub interfaces of this interface) must be registered with
 * prototype service scope. Use the provided default {@link BundlesServiceFactory prototype service
 * factory} or create a customized factory for bundle service operations.
 * <p>
 */
public interface BundleExecutor {

	/**
	 * Schedules this bundle job to be run with a delay.
	 * <p>
	 * This method is otherwise equal to {@code getJob().schedule(long)}
	 * 
	 * @param delay Delay in milliseconds before to run the bundle operation
	 * @see Job#schedule(long)
	 */
	public void execute(long delay);

	/**
	 * Schedules this bundle job to be run.
	 * <p>
	 * This is a convenience method to execute a bundle operation otherwise equal to
	 * {@link #execute(long)} and {@code getJob().schedule()}
	 * 
	 * @see Job#schedule()
	 */
	public void execute();

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
	 */
	public String getName();

	/**
	 * Access the underlying workspace job of this bundle operation service.
	 * <p>
	 * Scheduling a job with {@code getJob().schedule()} from the returned {@link WorkspaceJob} is
	 * otherwise identical to {@link #execute()}.
	 * 
	 * @return the job to be executed for this bundle operation service
	 */
	public WorkspaceJob getJob();

	/**
	 * Add a pending bundle project to execute by this bundle service
	 * 
	 * @param bundle project to execute
	 */
	public void addPendingProject(IProject project);

	/**
	 * Add a collection of pending bundle projects to execute by this bundle service.
	 * <p>
	 * To maintain the order of projects to add use {@link #resetPendingProjects(Collection)}
	 * 
	 * @param projects bundle projects to execute
	 * @see BundleJob#resetPendingProjects(Collection)
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

}