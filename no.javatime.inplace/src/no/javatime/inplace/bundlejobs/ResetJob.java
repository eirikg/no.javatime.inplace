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

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class ResetJob extends BundleJob implements Reset {

	/**
	 * Default constructor with a default job name
	 */
	public ResetJob() {
		super(Msg.RESET_JOB);
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

	/**
	 * Runs the reset bundle(s) operation
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
		try {
			SaveOptions saveOptions = getSaveOptions();
			saveOptions.disableSaveFiles(true);
			super.runInWorkspace(monitor);
			if (!bundleRegion.isRegionActivated()) {
				return getJobSatus();
			}
			BundleTransitionListener.addBundleTransitionListener(this);
			groupMonitor.beginTask(Msg.RESET_JOB, 3);
			BundleClosures closures = new BundleClosures();
			resetPendingProjects(closures.projectDeactivation(Closure.PROVIDING_AND_REQURING,
					getPendingProjects(), true));
			Collection<IProject> projectsToReset = getPendingProjects();
			Collection<IProject> errorProjects = null;
			if (projectsToReset.size() > 0) {
				errorProjects = getExternalDuplicateClosures(projectsToReset, null);
				if (null != errorProjects) {
					projectsToReset.removeAll(errorProjects);
					String msg = ErrorMessage.getInstance().formatString("bundle_errors_reset",
							bundleProjectCandidates.formatProjectList((errorProjects)));
					addError(null, msg);
				}
			}
			if (projectsToReset.size() == 0 || null != errorProjects) {
				return getJobSatus();
			}
			// Save current state of bundles to be used by the activate job to restore the saved current state
			Uninstall uninstall = new UninstallJob(Msg.RESET_UNINSTALL_JOB, projectsToReset);
			uninstall.getJob().setProgressGroup(groupMonitor, 1);
			uninstall.setAddRequiring(false);
			uninstall.setUnregister(true);
			uninstall.setSaveWorkspaceSnaphot(false);
			ActivateBundle activateBundle = new ActivateBundleJob(Msg.RESET_ACTIVATE_JOB,
					projectsToReset);
			activateBundle.setSaveWorkspaceSnaphot(false);
			activateBundle.getJob().setProgressGroup(groupMonitor, 1);
			BundleExecutorEventManager bundleExecutorService = Activator.getBundleExecutorEventService();
			bundleExecutorService.add(uninstall, 0);			
			bundleExecutorService.add(activateBundle, 0);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
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
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();
	}
}
