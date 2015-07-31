package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Update bundle projects after automatic build has been switched on.
 * <p>
 * If automatic build is off when projects are modified, the post build listener is not always
 * called by the builder building the modified projects when automatic build is switched from off to
 * on. This is for instance the case if only meta project files (e.g. manifest) have been changed.
 * In such cases an update job is scheduled by this auto build listener. In all other cases the post
 * build listener is responsible for scheduling update jobs for activated projects that are built.
 * 
 */
public class AutoBuildListener implements IExecutionListener {

	/**
	 * If files have been modified when auto build is off:
	 * <ol>
	 * <li>The pre build listener is always called and adds a build pending transition
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
	 * job is scheduled on behalf of the not called post build listener. 
	 * To schedule a build the must exist bundle projects with pending update transitions
	 * (a build has been performed and the bundles should be updated).
	 * <p>
	 */
	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {

		if (commandId.equals("org.eclipse.ui.project.buildAutomatically")) {
			try {
				BundleRegion bundleRegion = Activator.getBundleRegionService();
				if (bundleRegion.isRegionActivated()) {
					// Auto build has been switched on
					if (Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
						execute(bundleRegion);
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
	 * Auto build has been switched on. Wait for the scheduled builder and post build listener to
	 * finish, before updating bundles
	 * <p>
	 * Update is scheduled if the are projects with pending bundle projects to update
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
						// Post build listener not called.
						// Schedule an update job on behalf of the post build listener
						Collection<IProject> pendingProjects = bundleTransition.getPendingProjects(
								bundleRegion.getActivatedProjects(), Transition.UPDATE);
						if (pendingProjects.size() > 0) {
							Update update = new UpdateJob("Update after toggling auto build");
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
			}, 2, TimeUnit.SECONDS);
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
