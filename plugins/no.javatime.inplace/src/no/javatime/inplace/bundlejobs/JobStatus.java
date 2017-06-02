package no.javatime.inplace.bundlejobs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
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
	protected BundleCommand bundleCommand;
	protected BundleTransition bundleTransition;
	protected BundleRegion bundleRegion;

	protected BundleProjectCandidates bundleProjectCandidates;
	protected BundleProjectMeta bundleProjectMeta;
	protected MessageOptions messageOptions;
	protected CommandOptions commandOptions;
	protected SaveOptions saveOptions;
	protected long startTime;

	// List of error status objects
	private List<IBundleStatus> errStatusList;

	// List of historic status objects
	private List<IBundleStatus> logStatusList;

	/**
	 * Construct a job with the name of the job to run
	 * 
	 * @param name the name of the job to run
	 */
	public JobStatus(String name) {
		super(name);
		init();
	}

	private void init() {
		errStatusList = new ArrayList<>();
		logStatusList = new ArrayList<>();
		saveOptions = null;
	}

	public SaveOptions getSaveOptions() throws ExtenderException {

		if (null == saveOptions) {
			return saveOptions = Activator.getSaveOptionsService();
		}
		return saveOptions;
	}

	public void setSaveFiles(boolean saveFiles) {

		getSaveOptions().disableSaveFiles(saveFiles);
	}

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
	 * @throws ExtenderException If failing to obtain any of the bundle command, transition region,
	 * candidates services or the project meta and message options service
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException,
			ExtenderException {

		initServices();
		startTime = System.currentTimeMillis();
		// Save files before executing this bundle operation
		// This may trigger a build and an update to be scheduled after this job
		SaveOptions saveOptions = getSaveOptions();
		IBundleStatus jobStatus = saveOptions.saveFiles();
		if (jobStatus.getStatusCode() != StatusCode.OK) {
			addError(saveOptions.getErrorStatusList());
		}
		for (IBundleStatus status : saveOptions.getLogStatusList()) {
			addLogStatus(status);
		}
		return getJobSatus();
	}

	/**
	 * The initialization of services are delayed until bundle jobs are scheduled. Exceptions are when
	 * member methods in job interface services are accessed prior to scheduling of a bundle job
	 * <p>
	 * The initialization is conditional to reduce the number of get service calls for the different
	 * services
	 * 
	 * @throws ExtenderException failing to get any of the services used by bundle jobs
	 */
	protected void initServices() throws ExtenderException {

		if (null == bundleCommand) {
			bundleCommand = Activator.getBundleCommandService();
		}
		if (null == bundleTransition) {
			bundleTransition = Activator.getBundleTransitionService();
		}
		if (null == bundleRegion) {
			bundleRegion = Activator.getBundleRegionService();
		}
		if (null == bundleProjectCandidates) {
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		}
		if (null == bundleProjectMeta) {
			bundleProjectMeta = Activator.getbundlePrrojectMetaService();
		}
		if (null == messageOptions) {
			messageOptions = Activator.getMessageOptionsService();
		}
		if (null == commandOptions) {
			commandOptions = Activator.getCommandOptionsService();
		}
	}

	public long getStartedTime() {
		return startTime;
	}

	public IBundleStatus getJobSatus() {

		StatusCode statusCode = hasErrorStatus() ? StatusCode.JOB_ERROR : StatusCode.OK;
		return new BundleStatus(statusCode, Activator.PLUGIN_ID, getName());
	}

	/**
	 * Add log status messages to this job according to transition type
	 */
	@Override
	public void bundleTransitionChanged(BundleTransitionEvent event) {

		if (!messageOptions.isBundleOperations()) {
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
					addLogStatus(Msg.ON_DEMAND_BUNDLE_START_OP_TRACE, new Object[] { bundle }, bundle);
				} else {
					addLogStatus(
							Msg.START_BUNDLE_OP_TRACE,
							new Object[] { bundle, new DecimalFormat().format(bundleCommand.getExecutionTime()) },
							bundle);
				}
				break;
			case STOP:
				addLogStatus(Msg.STOP_BUNDLE_OP_TRACE,
						new Object[] { bundle, new DecimalFormat().format(bundleCommand.getExecutionTime()) },
						bundle);
				break;
			case UNINSTALL:
				String locUninstMsg = NLS.bind(Msg.BUNDLE_LOCATION_TRACE, bundle.getLocation());
				IBundleStatus uninstStatus = new BundleStatus(StatusCode.INFO, bundle, project,
						locUninstMsg, null);
				String uninstMsg = NLS.bind(Msg.UNINSTALL_BUNDLE_OP_TRACE,
						new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
				IBundleStatus multiUninstStatus = new BundleStatus(StatusCode.OK, bundle, project,
						uninstMsg, null);
				multiUninstStatus.add(uninstStatus);
				addLogStatus(multiUninstStatus);
				break;
			case INSTALL:
				// If null, the bundle project probably failed to install
				if (null != bundle) {
					String locInstMsg = NLS.bind(Msg.BUNDLE_LOCATION_TRACE, bundle.getLocation());
					IBundleStatus instStatus = new BundleStatus(StatusCode.INFO, bundle, project, locInstMsg,
							null);
					String instMsg = NLS.bind(Msg.INSTALL_BUNDLE_OP_TRACE,
							new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
					IBundleStatus multiInstStatus = new BundleStatus(StatusCode.OK, bundle, project, instMsg,
							null);
					multiInstStatus.add(instStatus);
					addLogStatus(multiInstStatus);
				}
				break;
			case LAZY_ACTIVATE:
				String msg = NLS.bind(Msg.LAZY_ACTIVATE_BUNDLE_OP_TRACE,
						new Object[] { bundle, bundleCommand.getStateName(bundle) });
				addInfo(msg, project);
				break;
			case UPDATE_CLASSPATH:
				bundleClassPath = bundleProjectMeta.getBundleClassPath(project);
				msg = NLS.bind(Msg.UPDATE_BUNDLE_CLASSPATH_TRACE, new Object[] { bundleClassPath,
						bundleProjectMeta.getDefaultOutputFolder(project), project.getName() });
				addInfo(msg, project);
				break;
			case REMOVE_CLASSPATH:
				bundleClassPath = bundleProjectMeta.getBundleClassPath(project);
				if (null == bundleClassPath) {
					msg = NLS
							.bind(
									Msg.REMOVE_BUNDLE_CLASSPATH_TRACE,
									new Object[] { bundleProjectMeta.getDefaultOutputFolder(project),
											project.getName() });
					addInfo(msg, project);
				} else {
					msg = NLS.bind(Msg.REMOVE_BUNDLE_CLASSPATH_ENTRY_TRACE, new Object[] { bundleClassPath,
							bundleProjectMeta.getDefaultOutputFolder(project), project.getName() });
					addInfo(msg, project);
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
				msg = NLS.bind(Msg.FRAMEWORK_BUNDLE_OP_TRACE, new Object[] { bundle.getSymbolicName(),
						bundleCommand.getStateName(bundle) });
				addInfo(msg, project);
				break;
			case DELETE_PROJECT:
				// Do not test for nature. Project files are not accessible at this point
				addLogStatus(Msg.DELETE_PROJECT_OP_TRACE, new Object[] { project.getName() }, project);
				break;
			case CLOSE_PROJECT:
				// Do not test for nature. Project files are not accessible at this point
				addLogStatus(Msg.CLOSE_PROJECT_OP_TRACE, new Object[] { project.getName() }, project);
				break;
			case NEW_PROJECT: {
				ActivateProject activate = new ActivateProjectJob();
				String addActivated = activate.isProjectActivated(project) ? "activated" : "deactivated";
				addLogStatus(Msg.ADD_PROJECT_OP_TRACE, new Object[] { addActivated, project.getName() },
						project);
				break;
			}
			default:
				break;
			}
		} catch (InPlaceException e) {
			addLogStatus(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project,
					Msg.LOG_TRACE_EXP, e));
		} catch (NullPointerException e) {
			addLogStatus(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project,
					Msg.LOG_TRACE_EXP, e));
		}
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
	protected IBundleStatus addLogStatus(String message, Bundle bundle, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.OK, bundle, project, message, null);
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Creates a bundle log status object and stores it in a log status list
	 * 
	 * @param message the message part of the created log status object
	 * @return the bundle log status object added to the log status list
	 * @see #addLogStatus(String, Object[], Object)
	 * @see #getLogStatusList()
	 */
	protected IBundleStatus addLogStatus(String message) {
		IBundleStatus status = new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, message);
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds a status object to the log status list
	 * 
	 * @param status the status object to add to the list
	 * @return the added status object
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
	 * @param bundleProject a {@code Bundle} or an {@code IProject}. Must not be null
	 * @see #addLogStatus(String, Bundle, IProject)
	 * @see #getLogStatusList()
	 */
	protected IBundleStatus addLogStatus(String key, Object[] substitutions, Object bundleProject) {
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
	 * @param project the project the error belongs to
	 * @return the newly created error status object with an exception, a message and a project
	 */
	protected IBundleStatus addError(Throwable e, String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, project,
				message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param message an error message
	 * @param bundle the bundle object the error belongs to
	 * @return the newly created error status object with an exception, a message and a bundle object
	 */
	protected IBundleStatus addError(Throwable e, String message, Bundle bundle) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, bundle, message,
				e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds an error to the status list
	 * 
	 * @param e the exception belonging to the error
	 * @param project the project the error belongs to
	 * @return the newly created error status object with an exception and its project
	 */
	protected IBundleStatus addError(Throwable e, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, project, null, e);
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
	protected IBundleStatus addError(Throwable e, String message) {
		IBundleStatus status = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds an information status object to the status list
	 * 
	 * @param message an information message
	 * @return the newly created information status object with a message
	 */
	protected IBundleStatus addInfo(String message) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, message, null);
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds an information status object to the status list
	 * 
	 * @param message an information message
	 * @param project the project the message belongs to
	 * @return the newly created information status object with a message
	 */
	protected IBundleStatus addInfo(String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, project, message,
				null);
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds an cancel status object to the status list
	 * 
	 * @param e the cancel exception belonging to the cancel message
	 * @param message an canceling message
	 * @return the newly created canceling status object with a message
	 */
	protected IBundleStatus addCancel(OperationCanceledException e, String message) {
		IBundleStatus status = new BundleStatus(StatusCode.CANCEL, Activator.PLUGIN_ID, message, e);
		this.logStatusList.add(status);
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
	protected IBundleStatus addWarning(Throwable e, String message, IProject project) {
		IBundleStatus status = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project,
				message, e);
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds a status object to the error status list
	 * 
	 * @param status the status object to add to the list
	 * @return the added status object
	 */
	public IBundleStatus addInfo(IBundleStatus status) {
		this.logStatusList.add(status);
		return status;
	}

	/**
	 * Adds a status object to the error status list
	 * 
	 * @param status the status object to add to the list
	 * @return the added status object
	 */
	public IBundleStatus addError(IBundleStatus status) {
		this.errStatusList.add(status);
		return status;
	}

	/**
	 * Adds a list of status objects to the status list
	 * 
	 * @param statusList the list of status objects to add to the list
	 */
	protected void addError(Collection<IBundleStatus> statusList) {
		this.errStatusList.addAll(statusList);
	}

	/**
	 * Adds all previous error status objects to the specified status object and removes the added
	 * status objects from the internal list of error status objects before adding the specified
	 * status object to the internal error list
	 * 
	 * @param multiStatus The status object to add all previous added error status objects to
	 * @return The specified status object
	 */
	public IBundleStatus createMultiStatus(IBundleStatus multiStatus) {
		for (IBundleStatus status : getErrorStatusList()) {
			multiStatus.add(status);
		}
		clearErrorStatusList();
		return addError(multiStatus);
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
	protected IBundleStatus createMultiStatus(IBundleStatus parent, IBundleStatus child) {
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
		return addError(parent);
	}

	/**
	 * Creates a new status object with {@code StatusCode.OK}. The created status object is not added
	 * to the status list
	 * 
	 * @return the new status object
	 */
	protected IBundleStatus createStatus() {
		return new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, "");
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
	protected int errorStatusList() {
		return errStatusList.size();
	}

	/**
	 * Get the last bundle status object added by this job
	 * 
	 * @return The last bundle status added by this job or a status object with {@code StatusCode} =
	 * OK if no error status object have been added
	 */
	protected IBundleStatus getLastErrorStatus() {

		return hasErrorStatus() ? errStatusList.get(errStatusList.size() - 1) : createStatus();
	}

	/**
	 * Check if any bundle status objects have been added to the error list
	 * 
	 * @return true if bundle error status objects exists in the error status list, otherwise false
	 */
	public boolean hasErrorStatus() {
		return (errStatusList.size() > 0 ? true : false);
	}

	/**
	 * Removes all status objects from the status list
	 */
	private void clearErrorStatusList() {
		this.errStatusList.clear();
	}

	/**
	 * Compares bundle projects for the same symbolic name and version. Each specified project is
	 * compared to all other valid workspace projects as specified by
	 * {@link BundleProjectCandidates#getInstallable()}.
	 * <p>
	 * When duplicates are detected the providing project - if any - in a set of duplicates is treated
	 * as the one to be installed or updated while the rest of the duplicates in the set are those
	 * left uninstalled or not updated. This becomes indirectly evident from the formulation of the
	 * error messages sent to the log view.
	 * <p>
	 * Any errors that occurs while retrieving the symbolic name and version of bundles are added to
	 * the job status list
	 * 
	 * @param duplicateProject duplicate project
	 * @param duplicateException the duplicate exception object associated with the specified
	 * duplicate project
	 * @param message Extra info status message. Can be null
	 * @return a list of duplicate tuples. Returns an empty list if no duplicates are found.
	 * @throws CircularReferenceException if cycles are detected in the project graph
	 * @see #getErrorStatusList()
	 */
	@SuppressWarnings("unused")
	private Collection<IProject> handleDuplicateException(IProject duplicateProject,
			WorkspaceDuplicateException duplicateException, String message)
			throws CircularReferenceException {

		// List of detected duplicate tuples
		Collection<IProject> duplicates = new LinkedHashSet<IProject>();
		String duplicateCandidateKey = bundleRegion.getSymbolicKey(null, duplicateProject);
		if (null == duplicateCandidateKey || duplicateCandidateKey.length() == 0) {
			String msg = ErrorMessage.getInstance().formatString("project_symbolic_identifier",
					duplicateProject.getName());
			addError(null, msg, duplicateProject);
			return null;
		}
		ProjectSorter ps = new ProjectSorter();
		ps.setAllowCycles(true);
		Collection<IProject> installableProjects = bundleProjectCandidates.getInstallable();
		installableProjects.remove(duplicateProject);
		for (IProject duplicateProjectCandidate : installableProjects) {
			IBundleStatus startStatus = null;
			try {
				String symbolicKey = bundleRegion.getSymbolicKey(null, duplicateProjectCandidate);
				if (null == symbolicKey || symbolicKey.length() == 0) {
					String msg = ErrorMessage.getInstance().formatString("project_symbolic_identifier",
							duplicateProjectCandidate.getName());
					addError(null, msg, duplicateProjectCandidate);
					continue;
				}
				if (symbolicKey.equals(duplicateCandidateKey)) {
					throw new WorkspaceDuplicateException("duplicate_bundle_project", symbolicKey,
							duplicateProject.getName(), duplicateProjectCandidate.getName());
				}
			} catch (WorkspaceDuplicateException e) {
				// Build the multi status error log message
				String msg = null;
				try {
					msg = ErrorMessage.getInstance().formatString("duplicate_error",
							bundleProjectMeta.getSymbolicName(duplicateProject),
							bundleProjectMeta.getBundleVersion(duplicateProject));
					startStatus = addError(e, msg, duplicateProject);
					addError(null, e.getLocalizedMessage());
					addError(duplicateException, duplicateException.getLocalizedMessage(), duplicateProject);

					// Inform about the requiring projects of the duplicate project
					Collection<IProject> duplicateClosureSet = ps.sortRequiringProjects(Collections
							.<IProject> singleton(duplicateProject));
					duplicateClosureSet.remove(duplicateProject);
					if (duplicateClosureSet.size() > 0) {
						String affectedBundlesMsg = ErrorMessage.getInstance().formatString(
								"duplicate_affected_bundles", duplicateProject.getName(),
								bundleProjectCandidates.formatProjectList(duplicateClosureSet));
						addInfo(affectedBundlesMsg);
					}
					if (null != message) {
						addInfo(message);
					}
					String rootMsg = ExceptionMessage.getInstance().formatString("root_duplicate_exception");
					createMultiStatus(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, rootMsg),
							startStatus);
				} catch (InPlaceException e1) {
					addError(e1, e1.getLocalizedMessage());
				} finally {
					duplicates.add(duplicateProjectCandidate);
				}
			} catch (ProjectLocationException e) {
				addError(e, e.getLocalizedMessage(), duplicateProject);
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), duplicateProject);
			}
		}
		return duplicates;
	}

	@SuppressWarnings("unused")
	private IBundleStatus formateBundleStatus(Collection<IBundleStatus> statusList, String rootMessage) {
		ProjectSorter bs = new ProjectSorter();
		Collection<IProject> duplicateClosureSet = null;
		IBundleStatus rootStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
				rootMessage);
		for (IBundleStatus bundleStatus : statusList) {
			IProject project = bundleStatus.getProject();
			Throwable e = bundleStatus.getException();
			if (null != e && e instanceof WorkspaceDuplicateException) {
				if (null != project) {
					duplicateClosureSet = bs.sortRequiringProjects(Collections.singleton(project));
					duplicateClosureSet.remove(project);
					if (duplicateClosureSet.size() > 0) {
						String msg = ErrorMessage.getInstance().formatString("duplicate_affected_bundles",
								project.getName(), bundleProjectCandidates.formatProjectList(duplicateClosureSet));
						rootStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
					}
				}
			}
			rootStatus.add(bundleStatus);
		}
		return rootStatus;
	}
}
