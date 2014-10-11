package no.javatime.inplace;

import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * In an activated workspace install all projects and set activated projects to the same state as
 * they had at the last shutdown. If the workspace is deactivated set deactivated projects to
 * {@code Transition#UNINSTALL}. If a project has never been activated the default state for the
 * transition will be {@code Transition#NOTRANSITION}
 * 
 */
public class StartUp implements IStartup {

	/**
	 * Restore bundle projects to the same state as they had at shutdown.
	 */
	@Override
	public void earlyStartup() {
		if (InPlace.get().getMsgOpt().isBundleOperations()) {
			String osgiDev = BundleProjectSettings.inDevelopmentMode();
			if (null != osgiDev) {
				String msg = NLS.bind(Msg.CLASS_PATH_DEV_PARAM_INFO, osgiDev);
				InPlace.get().log(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
			}
		}
		try {
			Collection<IProject> activatedProjects = BundleProjectState.getNatureEnabledProjects();
			if (activatedProjects.size() > 0) {
				Collection<IProject> deactivatedProjects = deactivateBuildErrorClosures(activatedProjects);
				if (deactivatedProjects.size() > 0) {
					initDeactivatedWorkspace();
				} else {
					// Restore bundles to state from previous session
					bundleStartUpActivation(activatedProjects);
				}
			} else {
				// Register all projects as bundle projects and set their initial transition
				initDeactivatedWorkspace();
			}
		} finally {
			// Add resource listeners as soon as possible after scheduling bundle start up activation
			// This prevent other bundle jobs from being scheduled before the start up activation job
			InPlace.get().addResourceListeners();
			InPlace.get().processLastSavedState(true);
		}
	}

	/**
	 * Install all projects and set activated projects to the same state as they had at shutdown
	 * 
	 * @param activatedProjects activated bundle projects to same state as in previous session
	 */
	private void bundleStartUpActivation(Collection<IProject> activatedProjects) {

		ActivateBundleJob activateJob = new ActivateBundleJob(ActivateBundleJob.activateStartupJobName,
				activatedProjects);
		activateJob.setUseStoredState(true);
		BundleJobManager.addBundleJob(activateJob, 0);
	}

	/**
	 * Deactivate workspace if there are any build error closures. Build error closures are
	 * deactivated at shutdown so a build error closure at this point indicates that there
	 * has been an abnormal shutdown. The initial state of both deactivated and activated
	 * bundles are initially uninstalled at startup. Closures to deactivate are:
	 * <p>
	 * <ol>
	 * <li><b>Providing resolve closures</b>
	 * <p>
	 * <br>
	 * <b>Deactivated providing closure.</b> Resolve is rejected when deactivated bundles with build
	 * errors provides capabilities to projects to resolve (and start). This closure require the
	 * providing bundles to be activated when the requiring bundles are resolved. This is usually an
	 * impossible position. Activating and updating does not allow a requiring bundle to activate
	 * without activating the providing bundle.
	 * <p>
	 * <br>
	 * <b>Activated providing closure.</b> It is illegal to resolve an activated project when there
	 * are activated bundles with build errors that provides capabilities to the project to resolve.
	 * The requiring bundles to resolve will force the providing bundles with build errors to resolve.
	 * </ol>
	 * 
	 * @param activatedProjects all activated bundle projects
	 * @return projects that are deactivated or an empty set
	 */
	private Collection<IProject> deactivateBuildErrorClosures(Collection<IProject> activatedProjects) {

		DeactivateJob deactivateErrorClosureJob = new DeactivateJob("Deactivate on startup");
		try {
			// Deactivated and activated providing closure. Deactivated and activated projects with build
			// errors providing capabilities to project to resolve (and start) at startup
			BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.DEACTIVATE,
					Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
			if (be.hasBuildErrors()) {
				deactivateErrorClosureJob.addPendingProjects(activatedProjects);
				deactivateErrorClosureJob.setUser(false);
				deactivateErrorClosureJob.schedule();
				return activatedProjects;
			}
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg,
					null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		}
		return Collections.<IProject> emptySet();
	}

	/**
	 * Register all bundle projects and set the transition for all deactivated projects to the last
	 * transition before shut down. If a project has never been activated the default state for the
	 * transition will be {@code Transition#NOTRANSITION}
	 */
	private void initDeactivatedWorkspace() {

		WorkspaceJob initWorkspaceJob = new BundleJob(Msg.INIT_DEACTIVATED_WORKSPACE_JOB) {
			@Override
			public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IBundleStatus status = null;
				try {
					final IEclipsePreferences store = InPlace.getEclipsePreferenceStore();
					if (null == store) {
						addStatus(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
								Msg.INIT_WORKSPACE_STORE_WARN, null));
						return super.runInWorkspace(monitor);
					}
					BundleTransition bundleTransition = BundleManager.getTransition();
					BundleCommand bundleCommand = BundleManager.getCommand();
					BundleRegion bundleRegion = BundleManager.getRegion();
					for (IProject project : ProjectProperties.getPlugInProjects()) {
						if (null != store) {
							try {
								String symbolicKey = bundleRegion.getSymbolicKey(null, project);
								int state = store.getInt(symbolicKey, Transition.INSTALL.ordinal());
								// Don't register the project if there is no transition history
								if (state == Transition.UNINSTALL.ordinal()) {
									bundleCommand.registerBundleProject(project, null, false);
									bundleTransition.setTransition(project, Transition.UNINSTALL);
								} else if (state == Transition.REFRESH.ordinal()) {
									bundleCommand.registerBundleProject(project, null, false);
									bundleTransition.setTransition(project, Transition.REFRESH);
								}
							} catch (ProjectLocationException e) {
								if (null == status) {
									String msg = ExceptionMessage.getInstance().formatString("project_init_location");
									status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, null);
								}
								status.add(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project,
										project.getName(), e));
								addStatus(status);
							}
						}
					}
					return super.runInWorkspace(monitor);
				} catch (CircularReferenceException e) {
					String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
					BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
					multiStatus.add(e.getStatusList());
					addStatus(multiStatus);
				} catch (InPlaceException e) {
					String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
							getName());
					addError(e, msg);
				} catch (CoreException e) {
					String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
					return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
				}

				try {
					return super.runInWorkspace(monitor);
				} catch (CoreException e) {
					String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
					return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
				} finally {
				}
			}
		};
		BundleJobManager.addBundleJob(initWorkspaceJob, 0);
	}
}
