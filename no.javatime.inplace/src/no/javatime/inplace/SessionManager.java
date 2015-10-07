package no.javatime.inplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Initialize and clears the the region workspace at start up and shut down respectively and
 * schedules bundle jobs for activating bundles at session start and deactivating or uninstalling
 * bundles at session end.
 * <p>
 * A session last from the point when bundle projects are activated and to the point when bundles
 * are started to get uninstalled. If bundle projects are deactivated when a session starts it ends
 * at the point when bundles are started to get deactivated.
 * <p>
 * The workspace is deactivated at start up if it should but has not been deactivated at shut down
 * due to an abnormal termination of a session.
 * <p>
 * Examples of sessions are workspace session (startup/shutdown), deactivate/activate session and
 * when the Reset (uninstall/activate) bundle command is executed.
 * <p>
 * The {@code earlyStartup()} method is called after initializing the plug-in in the
 * {@code start(BundleContext)} method. Initializations that depends on the workbench should be done
 * here and not when the plug-in is initialized
 * <p>
 * The {@code preShutdown(IWorkbench, boolean)} method deactivates or uninstalls the workspace and
 * is called before the IDE begin the shut down process.
 * 
 */
public class SessionManager implements IStartup, IWorkbenchListener {

	/**
	 * Schedule a start up job for activating bundles
	 */
	@Override
	public void earlyStartup() {

		StartUpJob startUpJob = new StartUpJob(Msg.INIT_WORKSPACE_JOB);
		try {
			startUpJob.addPendingProjects(startUpJob.getActivatedProjects());
			// Override save preference options settings at startup
			startUpJob.setSaveWorkspaceSnaphot(false);
			startUpJob.setSaveFiles(false);
			// Let build run first to avoid running an update job after the start up job
			Activator.getResourceStateService().waitOnBuilder(false);
			Activator.getBundleExecutorEventService().add(startUpJob, 0);
		} catch (BundleLogException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (null != workbench) {
				workbench.addWorkbenchListener(this);
			}
		}
	}

	/**
	 * In cases where the workspace is deactivated at shut down project meta files are modified and
	 * the deactivation of projects must be done before the IDE starts its own shutdown process
	 */
	@Override
	public boolean preShutdown(IWorkbench workbench, boolean forced) {
		earlyShutDown(workbench);
		return true;
	}

	/**
	 * Not in use. The listener is removed in {@code preShutdown(IWorkbench, boolean)}
	 */
	@Override
	public void postShutdown(IWorkbench workbench) {
	}

	/**
	 * Schedule a shut down job for deactivation or uninstalling bundles and waits for the job to
	 * finish.
	 * <p>
	 * Errors are sent to the error log and to the default console
	 * 
	 * @param workbench The listener for this workbench is removed before returning
	 */
	private void earlyShutDown(IWorkbench workbench) {

		try {
			// Signal that we are shutting down
			StatePersistParticipant.setWorkspaceSession(false);
			IEclipsePreferences sessionPrefs = StatePersistParticipant.getSessionPreferences();
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			Collection<IProject> activatedProjects = bundleRegion.getActivatedProjects();
			Collection<IProject> bundleProjects = bundleRegion.getProjects();
			if (activatedProjects.size() > 0) {
				Collection<IBundleStatus> errorStatusList = new ArrayList<>(2);
				ResourceState resourceState = Activator.getResourceStateService();
				Collection<IProject> projectsToDeactivate = isDeactivateOnExit(bundleProjects,
						activatedProjects);
				if (projectsToDeactivate.size() > 0) {
					DeactivateJob deactivateJob = new DeactivateJob(Msg.DEACTIVATE_ON_SHUTDOWN_JOB,
							projectsToDeactivate);
					if (Activator.getCommandOptionsService().isDeactivateOnExit()) {
						deactivateJob.addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
								Msg.DEACTIVATE_ON_EXIT_INFO));
					} else {
						deactivateJob.addLogStatus(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
								Msg.DEACTIVATE_ON_EXIT_ERROR_WARN));
					}
					deactivateJob.getJob().setUser(false);
					// Full workbench save is performed by the workbench at shutdown
					deactivateJob.setSaveWorkspaceSnaphot(false);
					// Saving is determined by the user when the workbench is closing
					deactivateJob.setSaveFiles(false);
					Activator.getBundleExecutorEventService().add(deactivateJob, 0);
					// Wait for builder and bundle jobs to finish before proceeding
					resourceState.waitOnBuilder(false);
					resourceState.waitOnBundleJob();
					if (deactivateJob.getErrorStatusList().size() > 0) {
						final IBundleStatus multiStatus = deactivateJob.createMultiStatus(new BundleStatus(
								StatusCode.ERROR, Activator.PLUGIN_ID, deactivateJob.getName()));
						errorStatusList.add(multiStatus);
					}
				}
				if (projectsToDeactivate.size() < activatedProjects.size()) {
					// Uninstall in an activated workspace
					Uninstall uninstallJob = new UninstallJob(Msg.SHUT_DOWN_JOB, bundleProjects);
					uninstallJob.getJob().setUser(false);
					// Full workbench save is performed by the workbench at shutdown
					uninstallJob.setSaveWorkspaceSnaphot(false);
					// Saving is determined by the user when the workbench is closing
					uninstallJob.setSaveFiles(false);
					Activator.getBundleExecutorEventService().add(uninstallJob, 0);
					// Wait for builder and bundle jobs to finish before proceeding
					resourceState.waitOnBuilder(false);
					resourceState.waitOnBundleJob();
					if (uninstallJob.getErrorStatusList().size() > 0) {
						final IBundleStatus multiStatus = uninstallJob.createMultiStatus(new BundleStatus(
								StatusCode.ERROR, Activator.PLUGIN_ID, uninstallJob.getName()));
						errorStatusList.add(multiStatus);
					}
				} else {
					// Activation levels are always in state uninstalled and not saved in an deactivated
					// workspace. Save transition state for bundles in a deactivated workspace
					StatePersistParticipant.saveTransitionState(sessionPrefs, true);
				}
				if (errorStatusList.size() > 0) {
					try {
						// Send errors to default output console when shutting down
						Activator.getBundleConsoleService().setSystemOutToIDEDefault();
					} catch (ExtenderException | NullPointerException e) {
						// Ignore and send to current system err setting
					}
					System.err.println(Msg.BEGIN_SHUTDOWN_ERROR);
					for (IBundleStatus status : errorStatusList) {
						printStatus(status);
					}
					System.err.println(Msg.END_SHUTDOWN_ERROR);
				}
			} else {
				// Activation levels are always in state uninstalled and not saved in an deactivated
				// workspace. Save transition state for bundles in a deactivated workspace
				StatePersistParticipant.saveTransitionState(sessionPrefs, true);
			}
			// They should be, but ensure that saved and current pending transitions are in sync
			StatePersistParticipant.savePendingBuildTransitions(sessionPrefs,
					StatePersistParticipant.isWorkspaceSession());
			for (IProject project : bundleRegion.getProjects()) {
				bundleRegion.unregisterBundleProject(project);
			}
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
		} catch (InPlaceException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.INIT_BUNDLE_STATE_ERROR,
							e), StatusManager.LOG);
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					Msg.SHUT_DOWN_JOB);
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e), StatusManager.LOG);
		} finally {
			if (null != workbench) {
				workbench.removeWorkbenchListener(this);
			}
		}
	}

	/**
	 * If there are build errors among the closures of the specified activated bundle projects or the
	 * "Deactivate on Exit" option is on this signals that bundle projects should be deactivated
	 * instead of activated at start up.
	 * <p>
	 * The following closure is checked for build errors:
	 * <ol>
	 * <br>
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
	 * @param activatedProjects all or a scope of activated bundle projects
	 * @return true if there are build errors among the closures of activated bundle projects or the
	 * "Deactivate on Exit" option is on.
	 */
	public static Collection<IProject> isDeactivateOnExit(Collection<IProject> projects,
			Collection<IProject> activatedProjects) throws ExtenderException, CircularReferenceException {

		// Deactivate workspace if some projects are missing build state
		if (BundleProjectBuildError.hasBuildState(projects).size() > 0) {
			return activatedProjects;
		}
		// Deactivated and activated providing closure. Deactivated and activated projects with build
		// errors providing capabilities to project to resolve (and start) at startup
		BundleBuildErrorClosure be = new BundleBuildErrorClosure(activatedProjects,
				Transition.DEACTIVATE, Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
		if (Activator.getCommandOptionsService().isDeactivateOnExit()) {
			return activatedProjects;
		} else if (be.hasBuildErrors()) {
			return be.getBuildErrorClosures();
		}
		return Collections.<IProject> emptySet();
	}

	/**
	 * Print status and sub status objects to system err
	 * 
	 * @param status status object to print to system err
	 */
	private void printStatus(IStatus status) {
		Throwable t = status.getException();
		if (null != t) {
			t.printStackTrace();
		} else {
			System.err.println(status.getMessage());
		}
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			printStatus(children[i]);
		}
	}
}
