package no.javatime.inplace;

import java.util.Collection;

import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Schedules bundle jobs for activating bundles at session start (start up) and uninstalling bundles
 * at session end (shut down). See {@link StartUpJob} and {@link ShutDownJob} for details about the
 * bundle jobs.
 * <p>
 * The {@code earlyStartup()} method is called after initializing the plug-in in the
 * {@code start(BundleContext)} method. Initializations that depends on the workbench should be done
 * here and not when the plug-in is initialized
 * 
 */
public class SessionJobsInitiator implements IStartup {

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
			// Run build first to avoid running an update job after the start up job
			Activator.getResourceStateService().waitOnBuilder(true);
			Activator.getBundleExecutorEventService().add(startUpJob, 0);
		} catch (BundleLogException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Schedule a shut down job for uninstalling bundles and wait for the job to finish.
	 * <p>
	 * Errors are sent to the error log
	 */
	public static void shutDown() {

		ShutDownJob shutDownJob = null;
		try {
			shutDownJob = new ShutDownJob(Msg.SHUT_DOWN_JOB);
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			shutDownJob.addPendingProjects(bundleRegion.getProjects());
			shutDownJob.setUser(false);
			// Full workbench save is performed by the workbench at shutdown
			shutDownJob.setSaveWorkspaceSnaphot(false);
			// Saving is determined by the user when the workbench is closing
			shutDownJob.setSaveFiles(false);
			ResourceState resourceState = Activator.getResourceStateService();
			Activator.getBundleExecutorEventService().add(shutDownJob, 0);
			// Wait on the job listener to finish
			resourceState.waitOnBuilder(false);
			resourceState.waitOnBundleJob();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
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
	public static boolean isDeactivateOnExit(Collection<IProject> activatedProjects)
			throws ExtenderException, CircularReferenceException {

		// Deactivated and activated providing closure. Deactivated and activated projects with build
		// errors providing capabilities to project to resolve (and start) at startup
		BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.DEACTIVATE,
				Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
		if (be.hasBuildErrors() || Activator.getCommandOptionsService().isDeactivateOnExit()) {
			return true;
		}
		return false;
	}

}
