package no.javatime.inplace.bundlejobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleCommand;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleRegion;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundlemanager.DuplicateBundleException;
import no.javatime.inplace.bundlemanager.ProjectLocationException;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.extender.status.BundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Container class for bundle status objects added during a bundle job. A status object contains a status code
 * and one or more elements of type exception, message, project and a bundle id.
 * 
 * @see no.javatime.inplace.extender.status.IBundleStatus
 */
public abstract class JobStatus extends WorkspaceJob {

	/**
	 * Convenience reference to the bundle manager
	 */
	final protected BundleCommand bundleCommand = BundleManager.getCommand();
	final protected BundleTransition bundleTransition = BundleManager.getTransition();
	final protected BundleRegion bundleRegion = BundleManager.getRegion();

	/**
	 * Construct a job with the name of the job to run
	 * 
	 * @param name the name of the job to run
	 */
	public JobStatus(String name) {
		super(name);
	}

	// List of status objects
	private List<IBundleStatus> statusList = new ArrayList<IBundleStatus>();
	
	private List<IBundleStatus> traceStatusList = new ArrayList<IBundleStatus>();

	/**
	 * Runs the bundle(s) status operation.
	 * <p>
	 * Note that the internal {@code JobManager} class logs status unconditionally to the {@code LogView} if a
	 * job returns a status object with {@code IStatus.ERROR} or {@code IStatus.WARNING}
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list, or {@code BundleStatusCode.JOBINFO} if
	 *         any status objects have been added to the job status list. The status list may be obtained from this
	 *         job by accessing {@linkplain #getStatusList()}.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		if (getStatusList().size() > 0) {
			return new BundleStatus(StatusCode.JOBINFO, InPlace.PLUGIN_ID, null);
		}
		return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param message an error message
	 * @param project the project the error belongs to
	 * @return the newly created error status object with an exception, a message and a project
	 */
	public IBundleStatus addError(Throwable e, String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, message, e);
		this.statusList.add(status);
		try {
			bundleTransition.setTransitionError(project);
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(project, locEx);
		}
		return status;
	}

	/**
	 * Get all trace status objects added by this job
	 * 
	 * @return a list of status trace objects
	 * 
	 * @see #addTrace(String, Bundle, IProject)
	 * @see #addTrace(String, Object[], Object)
	 */
	public Collection<IBundleStatus> getTraceList() {
		return traceStatusList;
	}
	
	/**
	 * Creates a bundle status trace object and stores it in a trace list
	 *<p>
	 *Either the specified bundle or project may be null, but not both
	 * 
	 * @param message the message part of the created trace object 
	 * @param bundle the bundle part of the created trace object
	 * @param project the project part of the created trace object
	 * @return the bundle status trace object added to the trace list
	 * @see #addTrace(String, Object[], Object)
	 * @see #getTraceList()
	 */
	public IBundleStatus addTrace(String message, Bundle bundle, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, message, null);
		this.traceStatusList.add(status);
		return status;
	}
	/**
	 * Creates a bundle status trace object and adds it to the bundle status trace list
	 * <p>
	 * If the specified bundle project is of type {@code IProject}, its
	 * corresponding bundle will be added to the status trace object if it
	 * exists and if of type {@code Bundle} its project will be added.
	 *  
	 * @param key a {@code NLS} identifier
	 * @param substitutions parameters to the {@code NLS} string
	 * @param bundleProject a {@code Bundle} or an {@code IProject}
	 * @see #addTrace(String, Bundle, IProject)
	 * @see #getTraceList()
	 */
	public void addTrace(String key, Object[] substitutions, Object bundleProject) {
		Bundle bundle = null;
		IProject project = null;;
		if (null != bundleProject) {
			if (bundleProject instanceof IProject) {
				project = (IProject) bundleProject;
				bundle = bundleRegion.get(project);
			} else if (bundleProject instanceof Bundle) {
				bundle = (Bundle) bundleProject;
				project = bundleRegion.getProject(bundle);
			}
		}
		addTrace(NLS.bind(key, substitutions), bundle, project);
	}
	

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param message an error message
	 * @param bundleId the bundle id of the bundle that the error belongs to
	 * @return the newly created error status object with an exception, a message and a bundle id
	 */
	public IBundleStatus addError(Throwable e, String message, Long bundleId) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, bundleId, message, e);
		this.statusList.add(status);
		try {
			bundleTransition.setTransitionError(bundleRegion.getProject(bundleRegion.get(bundleId)));
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(bundleRegion.getProject(bundleRegion.get(bundleId)), locEx);
		}
		return status;
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param bundleId the bundle id of the bundle that the error belongs to
	 * @return the newly created error status object with an exception and a bundle id
	 */
	public IBundleStatus addError(Throwable e, Long bundleId) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, bundleId, null, e);
		this.statusList.add(status);
		try {
			bundleTransition.setTransitionError(bundleRegion.getProject(bundleRegion.get(bundleId)));
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(bundleRegion.getProject(bundleRegion.get(bundleId)), locEx);
		}
		return status;
	}


	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param project the project the error belongs to
	 * @return the newly created error status object with an exception and its project
	 */
	public IBundleStatus addError(Throwable e, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, null, e);
		this.statusList.add(status);
		try {
			bundleTransition.setTransitionError(project);
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(project, locEx);
		}
		return status;
	}
	

	private IBundleStatus errorSettingTransition(IProject project, Exception e) {
		String msg = null;
		if (null == project) {
			msg = ExceptionMessage.getInstance().formatString("project_null_location");
		} else {
			msg = ErrorMessage.getInstance().formatString("project_location", project.getName());			
		}
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param message an error message
	 * @return the newly created error status object with an exception and a message
	 */
	public IBundleStatus addError(Throwable e, String message) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, (IProject) null, message, e);
		this.statusList.add(status);
		return status;
	}

	
	/**
	 * Adds an information status object to the status list
	 * 
	 * @param message an information message
	 * @return the newly created information status object with a message
	 */
	public IBundleStatus addInfoMessage(String message) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, (IProject) null, message,
				null);
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds an cancel status object to the status list
	 * 
	 * @param e the cancel exception belonging to the cancel message
	 * @param message an canceling message
	 * @return the newly created canceling status object with a message
	 */
	public IBundleStatus addCancelMessage(OperationCanceledException e, String message) {
		IBundleStatus status = new BundleStatus(StatusCode.CANCEL, InPlace.PLUGIN_ID, (IProject) null, message, e);
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds a build error to the status list
	 * 
	 * @param msg an error message
	 * @param project the project the build error belongs to
	 * @return the newly created error status object with a build error message and the project the build error
	 *         belongs to.
	 */
	public IBundleStatus addBuildError(String msg, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.BUILDERROR, InPlace.PLUGIN_ID, project, msg, null);
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds a warning to the status list
	 * 
	 * @param e the exception belonging to the warning
	 * @param message a warning message
	 * @param project the project the warning belongs to
	 * @return the newly created warning status object with an exception, a message and a project
	 */
	public IBundleStatus addWarning(Throwable e, String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, project, message, e);
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds a status object to the status list
	 * 
	 * @param status the status object to add to the list
	 * @return the added status object
	 */
	public IBundleStatus addStatus(IBundleStatus status) {
		this.statusList.add(status);
		return status;
	}

	/**
	 * Adds a list of status objects to the status list
	 * 
	 * @param statusList the list of status objects to add to the list
	 */
	public void addStatus(Collection<IBundleStatus> statusList) {
		this.statusList.addAll(statusList);
	}

	/**
	 * Creates a multi status object containing the specified status object as the parent and all status objects
	 * in the status list as children. The status list is then cleared and the new multi status object is added
	 * to the list.
	 * 
	 * @param multiStatus a status object where all existing status objects are added to as children
	 */
	public IBundleStatus createMultiStatus(IBundleStatus multiStatus) {
		for (IBundleStatus status : getStatusList()) {
			multiStatus.add(status);
		}
		clearStatusList();
		return addStatus(multiStatus);
	}
	/**
	 * Creates a multi status object with the specified parent as the root status and all
	 * status objects added to the status list before and including the specified child object. The child
	 * status object must exist in the status list. The created multi status object replace all child
	 * status objects in the status list
	 * @param parent this is the parent status object
	 * @param child this is the first child
	 * @return the newly created multi status object
	 */
	public IBundleStatus createMultiStatus(IBundleStatus parent, IBundleStatus child) {
		int startIndex = 0;
		if (null == child || statusList.size() == 0 ) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_format_multi_status"); 
			addError(null, msg);
		} else {
			startIndex = statusList.indexOf(child);
			if (-1 == startIndex) {
				startIndex = 0;
			}
		}
		for (int i = statusList.size()-1; i >= startIndex; i--) {
			parent.add(statusList.get(i));
		}
		IStatus[] is = parent.getChildren();
		for (int i = 0; i < is.length; i++) {
			statusList.remove(is[i]);
		}
		return addStatus(parent);
	}
	
	/**
	 * Creates a new status object with {@code StatusCode#OK}. The created status
	 * object is not added to the status list
	 * @return the new status object
	 */
	public IBundleStatus createStatus() {
		return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
	}

	/**
	 * Get all status information added by this job
	 * 
	 * @return a list of status objects where each status object describes the nature of the status
	 */
	public Collection<IBundleStatus> getStatusList() {
		return statusList;
	}

	/**
	 * Number of status elements registered
	 * 
	 * @return number of status elements
	 */
	public int statusList() {
		return statusList.size();
	}

	/**
	 * Get the last added bundle status object added by this job
	 * 
	 * @return a the last added bundle status with added by this job or a status object with {@code StatusCode}
	 *         = OK if the list is empty
	 */
	public IBundleStatus getLastStatus() {
		if (hasStatus()) {
			return statusList.get(statusList.size() - 1);
		}
		return createStatus();
	}

	/**
	 * Check if any bundle status objects have been added to the status list
	 * 
	 * @return true if bundle status objects exists in the status list, otherwise false
	 */
	public boolean hasStatus() {
		return (statusList.size() > 0 ? true : false);
	}

	/**
	 * Removes all status objects from the status list
	 */
	public void clearStatusList() {
		this.statusList.clear();
	}
		
	public IBundleStatus formateBundleStatus(Collection<IBundleStatus> statusList, String rootMessage) {
		ProjectSorter bs = new ProjectSorter();
		Collection<IProject> duplicateClosureSet = null;
		IBundleStatus rootStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, rootMessage);
		for (IBundleStatus bundleStatus : statusList) {
			IProject project = bundleStatus.getProject();
			Throwable e = bundleStatus.getException();
			if (null != e && e instanceof DuplicateBundleException) {
				if (null != project) {
					duplicateClosureSet = bs.sortRequiringProjects(Collections.singleton(project));
					duplicateClosureSet.remove(project);
					if (duplicateClosureSet.size() > 0) {
						String msg = ErrorMessage.getInstance().formatString("duplicate_affected_bundles",
								project.getName(), ProjectProperties.formatProjectList(duplicateClosureSet));
						rootStatus.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
					}
				}
			}
			rootStatus.add(bundleStatus);
		}
		return rootStatus;
	}
}
