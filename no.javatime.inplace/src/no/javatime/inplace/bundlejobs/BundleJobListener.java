/*******************************************************************************
 * Copyright (c) 2011 - 2014 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.bundlejobs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Report status and log information about bundle jobs
 * 
 * @see no.javatime.inplace.region.status.IBundleStatus
 * @see no.javatime.inplace.bundlejobs.JobStatus
 */
public class BundleJobListener extends JobChangeAdapter {

	/**
	 * Default constructor for jobs
	 */
	public BundleJobListener() {
		super();
	}

	/**
	 * Log all log (trace) status objects from a bundle job to the bundle log. The error and warning
	 * status object list is forwarded to the custom status handler.
	 * <p>
	 * If the option to log bundle operations is off and the job contains bundle status information
	 * they will not be displayed in the bundle log. To force status information to be displayed
	 * either turn on the log bundle operations switch or send status information directly to the
	 * bundle log
	 */
	@Override
	public void done(IJobChangeEvent event) {
		
		final Job job = event.getJob();
		if (job instanceof BundleJob) {
			final BundleJob bundleJob = (BundleJob) job;
			try {
				MessageOptions messageOptions = Activator.getMessageOptionsService();
				// Send the log list to the bundle log
				if (messageOptions.isBundleOperations()) {
					// final String execTime = Long.toString(System.currentTimeMillis() - startTime);
					final Collection<IBundleStatus> logList = bundleJob.getLogStatusList();
					if (logList.size() > 0) {
//						ExecutorService executor = Executors.newSingleThreadExecutor();
//						executor.execute(new Runnable() {
//							@Override
//							public void run() {
								IBundleStatus multiStatus = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
										NLS.bind(Msg.JOB_NAME_TRACE, bundleJob.getName(), 
												new DecimalFormat().format(System.currentTimeMillis() - bundleJob.getStartedTime())));
								for (IBundleStatus status : logList) {
									multiStatus.add(status);
								}
								multiStatus.setStatusCode();
								Activator.log(multiStatus);
//							}
//						});
//						executor.shutdown();
					}
				}
				// Send the error list to the error log
				Collection<IBundleStatus> errorList = logCancelStatus(bundleJob);
				if (errorList.size() > 0) {
					IBundleStatus multiStatus = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, NLS.bind(
							Msg.END_JOB_ROOT_ERROR, bundleJob.getName()));
					for (IBundleStatus status : errorList) {
						multiStatus.add(status);
					}
					// The custom or the standard status handler is not invoked
					// when the workbench is closing. It logs directly to the error log
					StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
				}
				if (Category.DEBUG) {
					if (messageOptions.isBundleOperations()) {
						getBundlesJobRunState(bundleJob);
					}
				}
				schedulePendingOperations();
			} catch (BundleLogException | ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}
		
	/**
	 * If the bundle job has been cancelled, log it
	 * 
	 * @param bundleJob the job that may contain a cancel status
	 * 
	 * @return a copy of the error status list with the cancel status removed from the status list or
	 * a copy of the status list if it does not contain any cancel status
	 */
	private Collection<IBundleStatus> logCancelStatus(BundleJob bundleJob) {

		Collection<IBundleStatus> statusList = bundleJob.getErrorStatusList();
		for (IBundleStatus status : statusList) {
			if (status.hasStatus(StatusCode.CANCEL)) {
				StatusManager.getManager().handle(status, StatusManager.LOG);
				Collection<IBundleStatus> modifiedStatusList = new ArrayList<IBundleStatus>(statusList);
				modifiedStatusList.remove(status);
				return modifiedStatusList;
			}
		}
		return statusList;
	}

	/**
	 * Schedule bundle jobs if there are any pending bundle operations of type activate, deactivate or
	 * refresh among all bundle projects.
	 * @throws ExtenderException if failing to get any of the transition, candidate or region services
	 */
	private void schedulePendingOperations() throws ExtenderException {

		BundleTransition bundleTransition = Activator.getBundleTransitionService();
		BundleProjectCandidates bundleProjectcandidates = Activator.getBundleProjectCandidatesService();
		BundleRegion bundleRegion = Activator.getBundleRegionService();
		BundleExecutor bundleJob = null;

		Collection<IProject> deactivatedProjects = bundleProjectcandidates.getCandidates();
		
		// This usually comes from a delayed update when activated bundles to resolve depends on
		// deactivated bundles
		Collection<IProject> projectsToActivate = bundleTransition.getPendingProjects(
				deactivatedProjects, Transition.ACTIVATE_PROJECT);
		if (projectsToActivate.size() > 0) {
			bundleJob = new ActivateProjectJob(ActivateProjectJob.activateProjectJobName,
					projectsToActivate);
			bundleTransition.removePending(projectsToActivate, Transition.ACTIVATE_PROJECT);
			if (Activator.getMessageOptionsService().isBundleOperations()) {
				try {
					ProjectSorter projectSorter = new ProjectSorter();
					// Inform about already activated projects that have requirements on deactivated projects
					for (IProject deactivatedProject : projectsToActivate) {
						Collection<IProject> activatedProjects = projectSorter.sortRequiringProjects(
								Collections.<IProject> singletonList(deactivatedProject), true);
						activatedProjects.remove(deactivatedProject);
						if (activatedProjects.size() > 0) {
							String msg = NLS.bind(Msg.IMPLICIT_ACTIVATION_INFO,
									bundleProjectcandidates.formatProjectList(activatedProjects), deactivatedProject.getName());
							IBundleStatus status = new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg);
							msg = NLS.bind(Msg.DELAYED_RESOLVE_INFO,
									bundleProjectcandidates.formatProjectList(activatedProjects), deactivatedProject.getName());
							status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
							bundleJob.addLogStatus(status);
						}
					}
				} catch (CircularReferenceException e) {
				}
			}
			Activator.getBundleExecutorEventService().add(bundleJob, 0);
		}

		Collection<IProject> activatedProjects = bundleRegion.getActivatedProjects();

		Collection<IProject> projectsToRefresh = bundleTransition.getPendingProjects(activatedProjects,
				Transition.REFRESH);
		if (projectsToRefresh.size() > 0) {
			bundleJob = new RefreshJob(RefreshJob.refreshJobName, projectsToRefresh);
			bundleTransition.removePending(projectsToRefresh, Transition.REFRESH);
			Activator.getBundleExecutorEventService().add(bundleJob);
		}
		Collection<IProject> projectsToDeactivate = bundleTransition.getPendingProjects(
				activatedProjects, Transition.DEACTIVATE);
		if (projectsToDeactivate.size() > 0) {
			bundleJob = new DeactivateJob(DeactivateJob.deactivateJobName, projectsToDeactivate);
			bundleTransition.removePending(projectsToDeactivate, Transition.DEACTIVATE);
			Activator.getBundleExecutorEventService().add(bundleJob);
		}
	}

	/**
	 * Trace that a bundle job is running if the job is in state {@code Job.RUNNING}.
	 * 
	 * @param bundleJob the bundle job to trace
	 */
	private void getBundlesJobRunState(BundleJob bundleJob) {
		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.RUNNING) {
				TraceMessage.getInstance().getString("running_jobs", job.getName(), bundleJob.getName());
			}
		}
	}
}
