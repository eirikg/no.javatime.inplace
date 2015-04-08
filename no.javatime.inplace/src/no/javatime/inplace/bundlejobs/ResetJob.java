/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.bundlejobs;

import java.util.Collection;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class ResetJob extends BundleJob implements Reset {

	/** Standard name of a reset job */
	final public static String resetJobName = Message.getInstance().formatString("reset_job_name");
	/* The name of the uninstall phase of the reset job */
	final private static String uninstallResetJobName = Message.getInstance().formatString(
			"uninstall_reset_job_name");
	/* The name of the activate phase of the reset job */
	final private static String activateResetJobName = Message.getInstance().formatString(
			"activate_reset_job_name");

	/**
	 * Default constructor
	 */
	public ResetJob() {
		super(resetJobName);
	}

	/**
	 * Construct an activate job with a given name
	 * 
	 * @param name job name
	 */
	public ResetJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and pending projects to activate
	 * 
	 * @param name job name
	 * @param projects to activate
	 */
	public ResetJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct an activate job with a given name and a pending project to activate
	 * 
	 * @param name job name
	 * @param project to activate
	 */
	public ResetJob(String name, IProject project) {
		super(name, project);
	}
	
	// Set the progress group to this monitor on both jobs
	final IProgressMonitor groupMonitor = Job.getJobManager().createProgressGroup();

	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
		try {
			BundleTransitionListener.addBundleTransitionListener(this);
			groupMonitor.beginTask(resetJobName, 3);
			// Use the preference store to reset to same as current state after activate
			InPlace.get().savePluginSettings(true, false);
			ProjectSorter ps = new ProjectSorter();
			// Reset require that all bundles that the project(s) to reset have requirements on are reset to
			int count = 0;
			// TODO Consider if it is necessary to include a partial graph
			do {
				count = pendingProjects();
				resetPendingProjects(ps.sortRequiringProjects(getPendingProjects()));
				resetPendingProjects(ps.sortProvidingProjects(getPendingProjects()));
			} while (pendingProjects() > count);

			Collection<IProject> projectsToReset = getPendingProjects();
			Collection<IProject> errorProjects = null;
			if (projectsToReset.size() > 0) {
				errorProjects = removeExternalDuplicates(projectsToReset, null, null);
				if (null != errorProjects) {
					projectsToReset.removeAll(errorProjects);
					String msg = ErrorMessage.getInstance().formatString("bundle_errors_reset", bundleProjectCandidates.formatProjectList((errorProjects)));
					addError(null, msg);
				}
			}
			if (projectsToReset.size() == 0 || null != errorProjects) {
				try {
					return super.runInWorkspace(monitor);
				} catch (CoreException e) {
					String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
					return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
				}
			}
			// Save current state of bundles to be used by the activate j0b to restore the current state
			InPlace.get().savePluginSettings(true, false);
			UninstallJob uninstallJob = new UninstallJob(uninstallResetJobName, projectsToReset);
			uninstallJob.setProgressGroup(groupMonitor, 1);
			InPlace.getBundleJobEventService().add(uninstallJob, 0);
			ActivateBundle activateBundleJob = new ActivateBundleJob(ResetJob.activateResetJobName, projectsToReset);
			activateBundleJob.setPersistState(true);
			activateBundleJob.getJob().setProgressGroup(groupMonitor, 1);
			InPlace.getBundleJobEventService().add(activateBundleJob, 0);
		} catch (OperationCanceledException e) {			
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (ExtenderException e) {			
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
	}
}
