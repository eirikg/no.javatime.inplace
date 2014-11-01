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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleProject;
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

	// Calculate job executing time
	private long startTime;

	/**
	 * Default constructor for jobs
	 */
	public BundleJobListener() {
		super();
	}

	/**
	 * If the option to report on bundle operations is true, record the start time of the job
	 */
	@Override
	public void running(IJobChangeEvent event) {
		Job job = event.getJob();
		if (job instanceof BundleJob) {
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				startTime = System.currentTimeMillis();
			}
		}
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
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				final Collection<IBundleStatus> traceList = bundleJob.getLogStatusList();
				if (traceList.size() > 0) {
					Runnable trace = new Runnable() {
						public void run() {
							IBundleStatus multiStatus = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID,
									NLS.bind(Msg.JOB_NAME_TRACE, bundleJob.getName(),
											Long.toString(System.currentTimeMillis() - startTime)));
							for (IBundleStatus status : traceList) {
								multiStatus.add(status);
							}
							InPlace.get().log(multiStatus);
						}
					};
					trace.run();
				}
			}
			Collection<IBundleStatus> statusList = logCancelStatus(bundleJob);
			if (statusList.size() > 0) {
				IBundleStatus multiStatus = new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, NLS.bind(
						Msg.END_JOB_ROOT_ERROR, bundleJob.getName()));
				for (IBundleStatus status : statusList) {
					multiStatus.add(status);
				}
				// The custom or the standard status handler is not invoked
				// when the workbench is closing. It logs directly to the error log
				StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
			}
			if (Category.DEBUG) {
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					getBundlesJobRunState(bundleJob);
				}
			}
			schedulePendingOperations();
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
	 */
	private void schedulePendingOperations() {

		BundleTransition bundleTransition = InPlace.getBundleTransitionService();
		BundleProject bundleProject = InPlace.getBundleProjectService();
		BundleJob bundleJob = null;

		Collection<IProject> deactivatedProjects = bundleProject.getCandidates();
		// This usually comes from a delayed update when activated bundles to resolve depends on
		// deactivated bundles
		Collection<IProject> projectsToActivate = bundleTransition.getPendingProjects(
				deactivatedProjects, Transition.ACTIVATE_PROJECT);
		if (projectsToActivate.size() > 0) {
			bundleJob = new ActivateProjectJob(ActivateProjectJob.activateProjectsJobName,
					projectsToActivate);
			bundleTransition.removePending(projectsToActivate, Transition.ACTIVATE_PROJECT);
			if (InPlace.get().getMsgOpt().isBundleOperations()) {
				try {
					ProjectSorter projectSorter = new ProjectSorter();
					// Inform about already activated projects that have requirements on deactivated projects
					for (IProject deactivatedProject : projectsToActivate) {
						Collection<IProject> activatedProjects = projectSorter.sortRequiringProjects(
								Collections.<IProject> singletonList(deactivatedProject), true);
						activatedProjects.remove(deactivatedProject);
						if (activatedProjects.size() > 0) {
							String msg = NLS.bind(Msg.IMPLICIT_ACTIVATION_INFO,
									bundleProject.formatProjectList(activatedProjects), deactivatedProject.getName());
							IBundleStatus status = new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg);
							msg = NLS.bind(Msg.DELAYED_RESOLVE_INFO,
									bundleProject.formatProjectList(activatedProjects), deactivatedProject.getName());
							status.add(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
							bundleJob.addLogStatus(status);
						}
					}
				} catch (CircularReferenceException e) {
				}
			}
			BundleJobManager.addBundleJob(bundleJob, 0);
		}

		Collection<IProject> activatedProjects = bundleProject.getNatureEnabled();

		Collection<IProject> projectsToRefresh = bundleTransition.getPendingProjects(activatedProjects,
				Transition.REFRESH);
		if (projectsToRefresh.size() > 0) {
			bundleJob = new RefreshJob(RefreshJob.refreshJobName, projectsToRefresh);
			bundleTransition.removePending(projectsToRefresh, Transition.REFRESH);
			BundleJobManager.addBundleJob(bundleJob, 0);
		}
		Collection<IProject> projectsToDeactivate = bundleTransition.getPendingProjects(
				activatedProjects, Transition.DEACTIVATE);
		if (projectsToDeactivate.size() > 0) {
			bundleJob = new DeactivateJob(DeactivateJob.deactivateJobName, projectsToDeactivate);
			bundleTransition.removePending(projectsToDeactivate, Transition.DEACTIVATE);
			BundleJobManager.addBundleJob(bundleJob, 0);
		}
	}

	/**
	 * Trace that a bundle job is running if the job is in state {@code Job.RUNNING}.
	 * 
	 * @param bundleJob the bundle job to trace
	 */
	private void getBundlesJobRunState(BundleJob bundleJob) {
		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.RUNNING) {
				TraceMessage.getInstance().getString("running_jobs", job.getName(), bundleJob.getName());
			}
		}
	}
}
