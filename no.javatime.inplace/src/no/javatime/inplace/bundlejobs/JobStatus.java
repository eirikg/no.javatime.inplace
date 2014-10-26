package no.javatime.inplace.bundlejobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.DuplicateBundleException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Container class for bundle status objects added during a bundle job. A status object contains a
 * status code and one or more elements of type exception, message, project and a bundle id.
 * 
 * @see no.javatime.inplace.region.status.IBundleStatus
 */
public abstract class JobStatus extends WorkspaceJob implements BundleTransitionEventListener {

	/**
	 * Convenience reference to the bundle manager
	 */
	final protected BundleCommand bundleCommand = InPlace.getBundleCommandService();
	final protected BundleTransition bundleTransition = InPlace.getBundleTransitionService();
	final protected BundleRegion bundleRegion = InPlace.getBundleRegionService();

	/**
	 * Construct a job with the name of the job to run
	 * 
	 * @param name the name of the job to run
	 */
	public JobStatus(String name) {
		super(name);
	}

	// List of error status objects
	private List<IBundleStatus> errStatusList = new ArrayList<IBundleStatus>();

	// List of historic status objects
	private List<IBundleStatus> logStatusList = new ArrayList<IBundleStatus>();

	/**
	 * Runs the bundle(s) status operation.
	 * <p>
	 * Note that the internal {@code JobManager} class logs status unconditionally to the
	 * {@code LogView} if a job returns a status object with {@code IStatus.ERROR} or
	 * {@code IStatus.WARNING}
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list, or
	 * {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job status list.
	 * The status list may be obtained from this job by accessing {@linkplain #getStatusList()}.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		if (getStatusList().size() > 0) {
			return new BundleStatus(StatusCode.JOBINFO, InPlace.PLUGIN_ID, null);
		}
		return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
	}

	/**
	 * Adds trace messages to this job according to transition type
	 */
	@Override
	public void bundleTransitionChanged(BundleTransitionEvent event) {
		if (!InPlace.get().getMsgOpt().isBundleOperations()) {
			return;
		}
		Bundle bundle = event.getBundle();
		Transition transition = event.getTransition();
		IProject project = event.getProject();
		IBundleProjectDescription bundleProjDesc = null;
		try {

			switch (transition) {
			case RESOLVE:
				addTrace(Msg.RESOLVE_BUNDLE_OP_TRACE,
						new Object[] {bundle.getSymbolicName()}, bundle);
				break;
			case UNRESOLVE:
				addTrace(Msg.UNRESOLVE_BUNDLE_OP_TRACE,
						new Object[] {bundle.getSymbolicName()}, bundle);
				break;
			case UPDATE:
				addTrace(Msg.UPDATE_BUNDLE_OP_TRACE,
						new Object[] { bundle.getSymbolicName()}, bundle);
				break;
			case REFRESH:
				addTrace(Msg.REFRESH_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName() }, bundle);
				break;
			case START:
				if (ManifestOptions.getlazyActivationPolicy(bundle)) {
					addTrace(Msg.ON_DEMAND_BUNDLE_START_OP_TRACE, new Object[] { bundle.getSymbolicName() },
							bundle);
				} else {
					addTrace(
							Msg.START_BUNDLE_OP_TRACE,
							new Object[] { bundle.getSymbolicName(),
									Long.toString(bundleCommand.getExecutionTime()) }, bundle);
				}
				break;
			case STOP:
				addTrace(
						Msg.STOP_BUNDLE_OP_TRACE,
						new Object[] { bundle.getSymbolicName(),
								Long.toString(bundleCommand.getExecutionTime()) }, bundle);
				break;
			case UNINSTALL:
				addTrace(Msg.UNINSTALL_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(),
						bundle.getLocation() }, bundle);
				break;
			case INSTALL:
				// If null, the bundle project probably failed to install
				if (null != bundle) {
					addTrace(Msg.INSTALL_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(),
							bundle.getLocation() }, bundle);
				}
				break;
			case LAZY_ACTIVATE:
				addTrace(Msg.LAZY_ACTIVATE_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(),
						bundleCommand.getStateName(bundle) }, bundle);
				break;
			case UPDATE_CLASSPATH:
				bundleProjDesc = InPlace.get().getBundleDescription(project);
				addTrace(Msg.UPDATE_BUNDLE_CLASSPATH_TRACE,
						new Object[] { bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH),
						BundleProjectSettings.getDefaultOutputLocation(project), project.getName() }, project);
				break;
			case REMOVE_CLASSPATH:
				bundleProjDesc = InPlace.get().getBundleDescription(project);
				String classPath = bundleProjDesc.getHeader(Constants.BUNDLE_CLASSPATH);
				if (null == classPath) {
					addTrace(
							Msg.REMOVE_BUNDLE_CLASSPATH_TRACE,
							new Object[] { BundleProjectSettings.getDefaultOutputLocation(project),
									project.getName() }, project);
				} else {
					addTrace(Msg.REMOVE_BUNDLE_CLASSPATH_ENTRY_TRACE, new Object[] { classPath,
							BundleProjectSettings.getDefaultOutputLocation(project), project.getName() }, project);
				}
				break;
			case UPDATE_ACTIVATION_POLICY:
				bundleProjDesc = InPlace.get().getBundleDescription(project);
				String policy = bundleProjDesc.getActivationPolicy();
				addTrace(Msg.TOGGLE_ACTIVATION_POLICY_TRACE,
						new Object[] { (null == policy) ? "lazy" : "eager", (null == policy) ? "eager" : "lazy",
								project.getName() }, project);
				break;
			case EXTERNAL:
				addTrace(Msg.FRAMEWORK_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(), bundleCommand.getStateName(bundle)}, bundle);
			default:
				break;
			}
		} catch (InPlaceException e) {
			addTrace(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project, "Trace suppressed", e));
		} catch (NullPointerException e) {
			addTrace(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project, "Trace suppressed", e));
		}
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
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, message,
				e);
		this.errStatusList.add(status);
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
	 * @return a list of status trace objects or an empty list
	 * 
	 * @see #addTrace(String, Bundle, IProject)
	 * @see #addTrace(String, Object[], Object)
	 */
	public Collection<IBundleStatus> getTraceList() {
		return logStatusList;
	}

	/**
	 * Creates a bundle status trace object and stores it in a trace list
	 * <p>
	 * Either the specified bundle or project may be null, but not both
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
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds a bundle status trace object to the bundle status trace list
	 * <p>
	 * 
	 * @param status the status object added to the trace list should contain at least the bundle
	 * and/or the project related to the status message
	 * @see #getTraceList()
	 */
	public IBundleStatus addTrace(IBundleStatus status) {
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Creates a bundle status trace object and adds it to the bundle status trace list
	 * <p>
	 * If the specified bundle project is of type {@code IProject}, its corresponding bundle will be
	 * added to the status trace object if it exists and if of type {@code Bundle} its project will be
	 * added.
	 * 
	 * @param key a {@code NLS} identifier
	 * @param substitutions parameters to the {@code NLS} string
	 * @param bundleProject a {@code Bundle} or an {@code IProject}. Must not be null
	 * @see #addTrace(String, Bundle, IProject)
	 * @see #getTraceList()
	 */
	public IBundleStatus addTrace(String key, Object[] substitutions, Object bundleProject) {
		Bundle bundle = null;
		IProject project = null;
		;
		if (null != bundleProject) {
			if (bundleProject instanceof Bundle) {
				bundle = (Bundle) bundleProject;
				project = bundleRegion.getBundleProject(bundle);
			} else if (bundleProject instanceof IProject) {
				project = (IProject) bundleProject;
				bundle = bundleRegion.get(project);
			}
		}
		return addTrace(NLS.bind(key, substitutions), bundle, project);
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
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, bundleId, message,
				e);
		this.errStatusList.add(status);
		try {
			bundleTransition
					.setTransitionError(bundleRegion.getBundleProject(bundleRegion.get(bundleId)));
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(bundleRegion.getBundleProject(bundleRegion.get(bundleId)), locEx);
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
		this.errStatusList.add(status);
		try {
			bundleTransition
					.setTransitionError(bundleRegion.getBundleProject(bundleRegion.get(bundleId)));
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(bundleRegion.getBundleProject(bundleRegion.get(bundleId)), locEx);
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
		this.errStatusList.add(status);
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
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, project, msg, e);
		this.errStatusList.add(status);
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
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds an information status object to the status list
	 * 
	 * @param message an information message
	 * @return the newly created information status object with a message
	 */
	public IBundleStatus addInfoMessage(String message) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, message, null);
		this.errStatusList.add(status);
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
		IBundleStatus status = new BundleStatus(StatusCode.CANCEL, InPlace.PLUGIN_ID, message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds a build error to the status list
	 * 
	 * @param msg an error message
	 * @param project the project the build error belongs to
	 * @return the newly created error status object with a build error message and the project the
	 * build error belongs to.
	 */
	public IBundleStatus addBuildError(String msg, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.BUILDERROR, InPlace.PLUGIN_ID, project, msg,
				null);
		this.errStatusList.add(status);
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
		IBundleStatus status = new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, project,
				message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds a status object to the status list
	 * 
	 * @param status the status object to add to the list
	 * @return the added status object
	 */
	public IBundleStatus addStatus(IBundleStatus status) {
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds a list of status objects to the status list
	 * 
	 * @param statusList the list of status objects to add to the list
	 */
	public void addStatus(Collection<IBundleStatus> statusList) {
		this.errStatusList.addAll(statusList);
	}

	/**
	 * Creates a multi status object containing the specified status object as the parent and all
	 * status objects in the status list as children. The status list is then cleared and the new
	 * multi status object is added to the list.
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
	 * Creates a multi status object with the specified parent as the root status and all status
	 * objects added to the status list before and including the specified child object. The child
	 * status object must exist in the status list. The created multi status object replace all child
	 * status objects in the status list
	 * 
	 * @param parent this is the parent status object
	 * @param child this is the first child
	 * @return the newly created multi status object
	 */
	public IBundleStatus createMultiStatus(IBundleStatus parent, IBundleStatus child) {
		int startIndex = 0;
		if (null == child || errStatusList.size() == 0) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_format_multi_status");
			addError(null, msg);
		} else {
			startIndex = errStatusList.indexOf(child);
			if (-1 == startIndex) {
				startIndex = 0;
			}
		}
		for (int i = errStatusList.size() - 1; i >= startIndex; i--) {
			parent.add(errStatusList.get(i));
		}
		IStatus[] is = parent.getChildren();
		for (int i = 0; i < is.length; i++) {
			errStatusList.remove(is[i]);
		}
		return addStatus(parent);
	}

	/**
	 * Creates a new status object with {@code StatusCode#OK}. The created status object is not added
	 * to the status list
	 * 
	 * @return the new status object
	 */
	public IBundleStatus createStatus() {
		return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
	}

	/**
	 * Get all status information added by this job
	 * 
	 * @return a list of status objects where each status object describes the nature of the status or an empty list
	 */
	public Collection<IBundleStatus> getStatusList() {
		return errStatusList;
	}

	/**
	 * Number of status elements registered
	 * 
	 * @return number of status elements
	 */
	public int statusList() {
		return errStatusList.size();
	}

	/**
	 * Get the last added bundle status object added by this job
	 * 
	 * @return a the last added bundle status with added by this job or a status object with
	 * {@code StatusCode} = OK if the list is empty
	 */
	public IBundleStatus getLastStatus() {
		if (hasStatus()) {
			return errStatusList.get(errStatusList.size() - 1);
		}
		return createStatus();
	}

	/**
	 * Check if any bundle status objects have been added to the status list
	 * 
	 * @return true if bundle status objects exists in the status list, otherwise false
	 */
	public boolean hasStatus() {
		return (errStatusList.size() > 0 ? true : false);
	}

	/**
	 * Removes all status objects from the status list
	 */
	public void clearStatusList() {
		this.errStatusList.clear();
	}

	public IBundleStatus formateBundleStatus(Collection<IBundleStatus> statusList, String rootMessage) {
		ProjectSorter bs = new ProjectSorter();
		Collection<IProject> duplicateClosureSet = null;
		IBundleStatus rootStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID,
				rootMessage);
		for (IBundleStatus bundleStatus : statusList) {
			IProject project = bundleStatus.getProject();
			Throwable e = bundleStatus.getException();
			if (null != e && e instanceof DuplicateBundleException) {
				if (null != project) {
					duplicateClosureSet = bs.sortRequiringProjects(Collections.singleton(project));
					duplicateClosureSet.remove(project);
					if (duplicateClosureSet.size() > 0) {
						String msg = ErrorMessage.getInstance().formatString("duplicate_affected_bundles",
								project.getName(), BundleProjectState.formatProjectList(duplicateClosureSet));
						rootStatus.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
					}
				}
			}
			rootStatus.add(bundleStatus);
		}
		return rootStatus;
	}
}
