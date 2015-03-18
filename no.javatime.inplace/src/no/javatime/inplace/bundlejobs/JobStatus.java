package no.javatime.inplace.bundlejobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.DuplicateBundleException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
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
import org.osgi.framework.Bundle;

/**
 * Container class for bundle status objects added during a bundle job. A status object contains a
 * status code and one or more elements of type exception, message, project and bundle.
 * 
 * @see no.javatime.inplace.region.status.IBundleStatus
 */
public class JobStatus extends WorkspaceJob implements BundleTransitionEventListener {

	/**
	 * Convenience references to bundle management
	 */
	final protected BundleCommand bundleCommand = InPlace.getBundleCommandService();
	final protected BundleTransition bundleTransition = InPlace.getBundleTransitionService();
	final protected BundleRegion bundleRegion = InPlace.getBundleRegionService();
	final protected BundleProjectCandidates bundleProjectCandidates = InPlace
			.getBundleProjectCandidatesService();
	final protected BundleProjectMeta bundleProjectMeta = InPlace.getbundlePrrojectMetaService();

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
	 * The status list may be obtained from this job by accessing {@linkplain #getErrorStatusList()}.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		if (getErrorStatusList().size() > 0) {
			return new BundleStatus(StatusCode.JOBINFO, InPlace.PLUGIN_ID, null);
		}
		return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
	}

	/**
	 * Add log status messages to this job according to transition type
	 */
	@Override
	public void bundleTransitionChanged(BundleTransitionEvent event) {

		if (!InPlace.get().getMsgOpt().isBundleOperations()) {
			return;
		}
		Bundle bundle = event.getBundle();
		Transition transition = event.getTransition();
		IProject project = event.getProject();
		String bundleClassPath = null;

		try {
			switch (transition) {
			case RESOLVE:
				addLogStatus(Msg.RESOLVE_BUNDLE_OP_TRACE, new Object[] { bundle }, bundle);
				break;
			case UNRESOLVE:
				addLogStatus(Msg.UNRESOLVE_BUNDLE_OP_TRACE, new Object[] { bundle }, bundle);
				break;
			case UPDATE:
				addLogStatus(Msg.UPDATE_BUNDLE_OP_TRACE, new Object[] { bundle }, bundle);
				break;
			case REFRESH:
				addLogStatus(Msg.REFRESH_BUNDLE_OP_TRACE, new Object[] { bundle }, bundle);
				break;
			case START:
				if (bundleProjectMeta.getCachedActivationPolicy(bundle)) {
					addLogStatus(Msg.ON_DEMAND_BUNDLE_START_OP_TRACE,
							new Object[] { bundle }, bundle);
				} else {
					addLogStatus(
							Msg.START_BUNDLE_OP_TRACE,
							new Object[] { bundle,
									Long.toString(bundleCommand.getExecutionTime()) }, bundle);
				}
				break;
			case STOP:
				addLogStatus(
						Msg.STOP_BUNDLE_OP_TRACE,
						new Object[] { bundle,
								Long.toString(bundleCommand.getExecutionTime()) }, bundle);
				break;
			case UNINSTALL:
				String locUninstMsg = NLS.bind(Msg.BUNDLE_LOCATION_TRACE, bundle.getLocation());
				IBundleStatus uninstStatus = new BundleStatus(StatusCode.INFO, bundle, project, locUninstMsg, null);
				String uninstMsg = NLS.bind(Msg.UNINSTALL_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
				IBundleStatus multiUninstStatus = new BundleStatus(StatusCode.INFO, bundle, project, uninstMsg, null);					
				multiUninstStatus.add(uninstStatus);
				addLogStatus(multiUninstStatus);
				break;
			case INSTALL:
				// If null, the bundle project probably failed to install
				if (null != bundle) {
					String locInstMsg = NLS.bind(Msg.BUNDLE_LOCATION_TRACE, bundle.getLocation());
					IBundleStatus instStatus = new BundleStatus(StatusCode.INFO, bundle, project, locInstMsg, null);
					String instMsg = NLS.bind(Msg.INSTALL_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
					IBundleStatus multiInstStatus = new BundleStatus(StatusCode.INFO, bundle, project, instMsg, null);					
					multiInstStatus.add(instStatus);
					addLogStatus(multiInstStatus);
				}
				break;
			case LAZY_ACTIVATE:
				addLogStatus(Msg.LAZY_ACTIVATE_BUNDLE_OP_TRACE, new Object[] { bundle,
						bundleCommand.getStateName(bundle) }, bundle);
				break;
			case UPDATE_CLASSPATH:
				bundleClassPath = bundleProjectMeta.getBundleClassPath(project);
				addLogStatus(Msg.UPDATE_BUNDLE_CLASSPATH_TRACE, new Object[] { bundleClassPath,
						bundleProjectMeta.getDefaultOutputFolder(project), project.getName() }, project);
				break;
			case REMOVE_CLASSPATH:
				bundleClassPath = bundleProjectMeta.getBundleClassPath(project);
				if (null == bundleClassPath) {
					addLogStatus(
							Msg.REMOVE_BUNDLE_CLASSPATH_TRACE,
							new Object[] { bundleProjectMeta.getDefaultOutputFolder(project), project.getName() },
							project);
				} else {
					addLogStatus(Msg.REMOVE_BUNDLE_CLASSPATH_ENTRY_TRACE, new Object[] { bundleClassPath,
							bundleProjectMeta.getDefaultOutputFolder(project), project.getName() }, project);
				}
				break;
			case UPDATE_ACTIVATION_POLICY:
				Boolean policy = bundleProjectMeta.getActivationPolicy(project);
				addLogStatus(
						Msg.TOGGLE_ACTIVATION_POLICY_TRACE,
						// Changing from (old policy) to (new policy)
						new Object[] { (policy) ? "eager" : "lazy", (policy) ? "lazy" : "eager",
								project.getName() }, project);
				break;
			case UPDATE_DEV_CLASSPATH:
				String osgiDev = bundleProjectMeta.inDevelopmentMode();
				String symbolicName = bundleProjectMeta.getSymbolicName(project);
				addLogStatus(Msg.CLASS_PATH_COMMON_INFO, new Object[] { osgiDev, symbolicName }, project);
				break;
			case EXTERNAL:
				addLogStatus(Msg.FRAMEWORK_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(),
						bundleCommand.getStateName(bundle) }, bundle);
				break;
			case REMOVE_PROJECT:
				// Do not test for nature. Project files are not accessible at this point
				String remActivated = bundleRegion.isBundleActivated(project) ? "activated" : "deactivated";
				addLogStatus(Msg.REMOVE_PROJECT_OP_TRACE, new Object[] { remActivated, project.getName() },
						project);
				break;
			case NEW_PROJECT: {
				final Extension<ActivateProject> activateExtension = Extenders
						.getExtension(ActivateProject.class.getName());
				ActivateProject activate = activateExtension.getService();
				String addActivated = activate.isProjectActivated(project) ? "activated" : "deactivated";
				addLogStatus(Msg.ADD_PROJECT_OP_TRACE, new Object[] { addActivated, project.getName() },
						project);
				break;
			}
			default:
				break;
			}
		} catch (InPlaceException e) {
			addLogStatus(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project,
					Msg.LOG_TRACE_EXP, e));
		} catch (NullPointerException e) {
			addLogStatus(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project,
					Msg.LOG_TRACE_EXP, e));
		}
		// TODO Check and add exceptions to catch
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
	 * Get all log status objects added by this job
	 * 
	 * @return a list of status log status objects or an empty list
	 * 
	 * @see #addLogStatus(String, Bundle, IProject)
	 * @see #addLogStatus(String, Object[], Object)
	 */
	public Collection<IBundleStatus> getLogStatusList() {
		return logStatusList;
	}

	/**
	 * Creates a bundle log status object and stores it in a log status list
	 * <p>
	 * Either the specified bundle or project may be null, but not both
	 * 
	 * @param message the message part of the created log status object
	 * @param bundle the bundle part of the created log status object
	 * @param project the project part of the created log status object
	 * @return the bundle log status object added to the log status list
	 * @see #addLogStatus(String, Object[], Object)
	 * @see #getLogStatusList()
	 */
	public IBundleStatus addLogStatus(String message, Bundle bundle, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, message, null);
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds a bundle log status object to the bundle log status list
	 * <p>
	 * 
	 * @param status the status object added to the log status list should contain at least the bundle
	 * and/or the project related to the status message
	 * @see #getLogStatusList()
	 */
	public IBundleStatus addLogStatus(IBundleStatus status) {
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Creates a bundle log status object and adds it to the bundle log status list
	 * <p>
	 * If the specified bundle project is of type {@code IProject}, its corresponding bundle will be
	 * added to the log status object if it exists and if of type {@code Bundle} its project will be
	 * added.
	 * 
	 * @param key a {@code NLS} identifier
	 * @param substitutions parameters to the {@code NLS} string
	 * @param bundleProjectCandidates a {@code Bundle} or an {@code IProject}. Must not be null
	 * @see #addLogStatus(String, Bundle, IProject)
	 * @see #getLogStatusList()
	 */
	public IBundleStatus addLogStatus(String key, Object[] substitutions, Object bundleProject) {
		Bundle bundle = null;
		IProject project = null;
		if (null != bundleProject) {
			if (bundleProject instanceof Bundle) {
				bundle = (Bundle) bundleProject;
				project = bundleRegion.getProject(bundle);
			} else if (bundleProject instanceof IProject) {
				project = (IProject) bundleProject;
				bundle = bundleRegion.getBundle(project);
			}
		}
		return addLogStatus(NLS.bind(key, substitutions), bundle, project);
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param message an error message
	 * @param bundle the bundle object the error belongs to
	 * @return the newly created error status object with an exception, a message and a bundle object
	 */
	public IBundleStatus addError(Throwable e, String message, Bundle bundle) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, bundle, message, e);
		this.errStatusList.add(status);
		try {
			bundleTransition.setTransitionError(bundleRegion.getProject(bundle));
		} catch (ProjectLocationException locEx) {
			errorSettingTransition(bundleRegion.getProject(bundle), locEx);
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

	public IBundleStatus addInfoMessage(String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, project, message,
				null);
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
		for (IBundleStatus status : getErrorStatusList()) {
			multiStatus.add(status);
		}
		clearErrorStatusList();
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
	 * @return a list of status objects where each status object describes the nature of the status or
	 * an empty list
	 */
	public Collection<IBundleStatus> getErrorStatusList() {
		return errStatusList;
	}

	/**
	 * Number of status elements registered
	 * 
	 * @return number of status elements
	 */
	public int errorStatusList() {
		return errStatusList.size();
	}

	/**
	 * Get the last added bundle status object added by this job
	 * 
	 * @return a the last added bundle status with added by this job or a status object with
	 * {@code StatusCode} = OK if the list is empty
	 */
	public IBundleStatus getLastErrorStatus() {
		if (hasErrorStatus()) {
			return errStatusList.get(errStatusList.size() - 1);
		}
		return createStatus();
	}

	/**
	 * Check if any bundle status objects have been added to the status list
	 * 
	 * @return true if bundle status objects exists in the status list, otherwise false
	 */
	public boolean hasErrorStatus() {
		return (errStatusList.size() > 0 ? true : false);
	}

	/**
	 * Removes all status objects from the status list
	 */
	public void clearErrorStatusList() {
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
								project.getName(), bundleProjectCandidates.formatProjectList(duplicateClosureSet));
						rootStatus.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
					}
				}
			}
			rootStatus.add(bundleStatus);
		}
		return rootStatus;
	}
}
