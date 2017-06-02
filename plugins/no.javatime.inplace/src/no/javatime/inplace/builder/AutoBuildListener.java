package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.javatime.inplace.Activator;
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.WarnMessage;

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

/**
 * Update activated bundle projects and remove pending build transitions for deactivated bundle
 * projects after automatic build has been switched on. 
 * <p>
 * If automatic build is switched on after projects are modified, the post build listener is not
 * called by the builder when build kind is auto build and the resource delta is empty.
 * <p>
 * Modifications of meta files (e.g. .project and manifest files) leads to empty resource deltas.
 * 
 * For activated projects an update should be scheduled if there exists a pending update transition
 * (the java time builder has been invoked) by the build system.
 * 
 * This is for instance the case if only meta project files (e.g. manifest) have been changed. In
 * such cases an update job is scheduled by this auto build listener. In all other cases the post
 * build listener is responsible for scheduling update jobs for activated projects that are built.
 * 
 */
public class AutoBuildListener implements IExecutionListener {

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
	 * In the case where auto build is switched on and the post build listener is not called an update
	 * job is scheduled on behalf of the not called post build listener. To schedule a build the must
	 * exist bundle projects with pending update transitions (a build has been performed and the
	 * bundles should be updated).
	 * <p>
	 */
	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {

		if (commandId.equals("org.eclipse.ui.project.buildAutomatically")) {
			try {
				if (Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
					try {
						// Remove all saved pending build transitions
						StatePersistParticipant.clearPendingBuildTransitions(StatePersistParticipant.getSessionPreferences());
					} catch (IllegalStateException | BackingStoreException e) {
						String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
					BundleRegion bundleRegion = Activator.getBundleRegionService();
					if (bundleRegion.isRegionActivated()) {
						execute(bundleRegion);
					} else {
						// Execute waits on builder
						// Let the post build listener remove pending builds 
						Activator.getResourceStateService().waitOnBuilder(false);
					}
					BundleTransition bundleTransition = Activator.getBundleTransitionService();
					// Remove pending build transition for deactivated bundle projects
					// Activated bundle projects are removed by the java time builder
					final Collection<IProject> pendingProjects = bundleTransition.getPendingProjects(
							bundleRegion.getProjects(false), Transition.BUILD);
					if (pendingProjects.size() > 0) {
						removeBuildTransition.addPendingProjects(pendingProjects);
						removeBuildTransition.getJob().schedule();
					}
				}

			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}

	private BundleExecutor removeBuildTransition = new BundleJob("Remove pending build transition") {
		@Override
		public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

			try {
				getSaveOptions().disableSaveFiles(true);
				super.runInWorkspace(monitor);
				monitor.beginTask(getName(), 1);
				for (IProject project : getPendingProjects()) {
					bundleTransition.removePending(project, Transition.BUILD);
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
	 * Auto build has been switched on. Wait for the builder to finish and update bundles if the post
	 * build listener has not been called.
	 * <p>
	 * Schedule an update job if there are pending bundle projects to update
	 * 
	 * @param bundleRegion The bundle region service used to obtain activated bundle projects
	 */
	private void execute(final BundleRegion bundleRegion) {

		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		try {
			scheduledExecutorService.schedule(new Callable<Object>() {
				public Object call() throws Exception {
					try {
						Activator.getResourceStateService().waitOnBuilder(false);
						BundleTransition bundleTransition = Activator.getBundleTransitionService();

						// Schedule an update job on behalf of the post build listener
						Collection<IProject> pendingProjects = bundleTransition.getPendingProjects(
								bundleRegion.getActivatedProjects(), Transition.UPDATE);
						if (pendingProjects.size() > 0) {
							Update update = new UpdateJob();
							for (IProject project : pendingProjects) {
								if (!bundleTransition.containsPending(project, Transition.BUILD, false)) {
									UpdateScheduler.addProjectToUpdateJob(project, update);
								}
							}
							if (update.pendingProjects() > 0 && JavaTimeBuilder.hasBuild()) {
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

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
	}

}
