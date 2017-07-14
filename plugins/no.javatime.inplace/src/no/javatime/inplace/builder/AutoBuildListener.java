package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

import no.javatime.inplace.Activator;
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Act on bundle projects states when auto build is switched on.
 * <p>
 * Remove bundle errors in a deactivated workspace
 * <p>
 * Update activated bundle projects and remove pending build transitions for deactivated bundle
 * projects after projects have been built.
 * <p>
 * If automatic build is switched on after projects are modified, the post build listener is not
 * called by the build framework when build kind is auto build and the resource delta is empty.
 * Modifications of meta files (e.g. description and manifest files) leads to empty resource deltas.
 * <p>
 * For activated projects, update should be scheduled if there are pending update transitions (the
 * java time builder has been invoked).
 * 
 * In such cases - where the post builder is not called - an update job is scheduled. In all other
 * cases the post build listener is responsible for scheduling update jobs for activated projects
 * that have been built.
 * 
 * @see PostBuildListener
 * @see PreBuildListener
 */

public class AutoBuildListener implements IExecutionListener {

	final static private String autoBuildCmd = "org.eclipse.ui.project.buildAutomatically";

	/**
	 * Remove any bundle errors from projects to build
	 * 
	 * @see #postExecuteSuccess(String, Object)
	 */
	@Override
	public void preExecute(String commandId, ExecutionEvent event) {

		if (commandId.equals(autoBuildCmd)) {
			// This is the state before toggle. Auto build is switched from off to on
			if (!Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
				BundleRegion bundleRegion = Activator.getBundleRegionService();
				BundleTransition bundleTransition = Activator.getBundleTransitionService();
				// Pending build added by the pre build listener when auto build was off
				final Collection<IProject> pendingProjects = bundleTransition
						.getPendingProjects(bundleRegion.getProjects(), Transition.BUILD);
				// Remove all errors before a build and after auto build is switched on
				// Ok in a deactivated workspace where all bundles are in state uninstalled
				for (IProject project : pendingProjects) {
					bundleTransition.clearBuildTransitionError(project);
					bundleTransition.clearBundleTransitionError(project);
				}
			}
		}
	}

	/**
	 * If files have been modified when auto build is off:
	 * <ol>
	 * <li>The pre build listener is always called and adds a pending build transition
	 * <li>The JavaTime builder is not called
	 * <li>The post build listener is always called and does nothing
	 * </ol>
	 * and then when auto build is switched on:
	 * <p>
	 * <ol>
	 * <li>The pre build listener is never called
	 * <li>The JavaTime builder is always called and adds an update pending transition
	 * <li>The post build listener is called occasionally and when called an update job is scheduled
	 * </ol>
	 * 
	 * In the case where auto build is switched on and the post build listener is not called, an
	 * update job is scheduled on behalf of the not called post build listener. To schedule a build
	 * there must exist bundle projects with pending update transitions (a build has been performed
	 * and the bundles should be updated).
	 * <p>
	 */
	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {

		if (commandId.equals(autoBuildCmd)) {
			try {
				// This is the state after toggle. Auto build is switched from off to on
				if (Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
					try {
						// Remove all saved pending build transitions
						StatePersistParticipant
								.clearPendingBuildTransitions(StatePersistParticipant.getSessionPreferences());
					} catch (IllegalStateException | BackingStoreException e) {
						String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
					BundleRegion bundleRegion = Activator.getBundleRegionService();
					if (bundleRegion.isRegionActivated()) {
						postExecute(bundleRegion);
					}
					BundleTransition bundleTransition = Activator.getBundleTransitionService();
					// Remove pending build transition and check for errors in deactivated bundle projects
					// Pending build transitions are removed for activated bundle projects in the java time
					// builder
					final Collection<IProject> pendingProjects = bundleTransition
							.getPendingProjects(bundleRegion.getProjects(false), Transition.BUILD);
					if (pendingProjects.size() > 0) {
						removeBuildTransitionJob.addPendingProjects(pendingProjects);
						removeBuildTransitionJob.getJob().schedule();
					}
				}
			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}

	/**
	 * Remove any pending build transitions and check for build errors in projects
	 * <p>
	 * Running in a bundle job updates status in bundle view
	 */
	private BundleExecutor removeBuildTransitionJob = new BundleJob(
			"Remove pending build transition") {
		@Override
		public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

			try {
				getSaveOptions().disableSaveFiles(true);
				super.runInWorkspace(monitor);
				monitor.beginTask(getName(), 1);
				for (IProject project : getPendingProjects()) {
					bundleTransition.removePending(project, Transition.BUILD);
					// Make error status available in projects
					BundleProjectBuildError.hasBuildErrors(project, true);
				}
			} catch (ExtenderException e) {
				addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
			} catch (CoreException e) {
				String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
				return new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, e);
			} finally {
				monitor.done();
			}
			return getJobSatus();
		}
	};

	/**
	 * Auto build has been switched on. Update bundles if the post build listener has not been called
	 * after a build.
	 * <p>
	 * Schedule an update job if there are pending bundle projects to update
	 * 
	 * @param bundleRegion The bundle region service used to obtain activated bundle projects
	 */
	private void postExecute(final BundleRegion bundleRegion) {

		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		try {
			scheduledExecutorService.schedule(new Callable<Object>() {
				public Object call() throws Exception {
					try {
						BundleTransition bundleTransition = Activator.getBundleTransitionService();
						// Schedule an update job on behalf of the post build listener
						Collection<IProject> pendingProjects = bundleTransition
								.getPendingProjects(bundleRegion.getActivatedProjects(), Transition.UPDATE);
						if (pendingProjects.size() > 0) {
							Update update = new UpdateJob();
							for (IProject project : pendingProjects) {
								if (!bundleTransition.containsPending(project, Transition.BUILD, false)) {
									UpdateScheduler.addProjectToUpdateJob(project, update);
								}
							}
							if (update.pendingProjects() > 0) {
								JavaTimeBuilder.postBuild();
								Activator.getBundleExecutorEventService().add(update);
							}
						}
					} catch (ExtenderException e) {
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
								StatusManager.LOG);
					}
					return null;
				}
			}, 1, TimeUnit.SECONDS);
		} catch (RejectedExecutionException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			scheduledExecutorService.shutdown();
		}
	}

	@Override
	public void notHandled(String commandId, NotHandledException exception) {
	}

	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
	}
}
