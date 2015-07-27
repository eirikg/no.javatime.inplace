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
package no.javatime.inplace.dialogs;

import java.util.Collection;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.SaveOptionsJob;
import no.javatime.inplace.builder.SaveSnapShotOption;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 *
 */
public class ResourceStateHandler implements ResourceState {
	
	private SaveOptionsJob saveOptionsJob = null;
	
	public SaveOptions getSaveOptions() {

		return null == saveOptionsJob ? new SaveOptionsJob() : saveOptionsJob;	
	}
	
	@Override
	public Collection<IProject> getDirtyProjects() throws ExtenderException {

		return SaveOptionsJob.getDirtyProjects();
	}

	@Override
	public Collection<IProject> getDirtyProjects(Collection<IProject> projects) {

		return SaveOptionsJob.getScopedDirtyProjects(projects);
	}

	@Override
	public boolean areResourcesDirty () throws ExtenderException {

		return SaveOptionsJob.getDirtyProjects().size() > 0  ? true : false;
	}

	@Override
	public boolean isSaveFiles() {
		SaveOptions saveOptions = getSaveOptions();
		return saveOptions.isSaveFiles();
	}

	@Override
	public boolean isSaveWorkspaceSnapshot() {
		SaveOptions saveOptions = getSaveOptions();
		return saveOptions.isSaveWorkspaceSnaphot();
	}
	
	@Override
	public boolean isTriggerUpdate() {
		
		SaveOptions saveOptions = getSaveOptions();
		return saveOptions.isTriggerUpdate();
	}

	@Override
	public void saveWorkspaceSnapshot() {

		SaveSnapShotOption saveSnapshot = new SaveSnapShotOption();
		saveSnapshot.saveWorkspace(new NullProgressMonitor());
	}

	@Override
	public void saveFiles() throws InPlaceException, ExtenderException {

		SaveOptions saveOptions = getSaveOptions();
		if (saveOptions.isSaveFiles()) {
			Activator.getBundleExecutorEventService().add(saveOptions);
		}
	}
	
	@Override
	public void waitOnBuilder(boolean log) {

		IJobManager jobMan = Job.getJobManager();
		Job[] build = jobMan.find(ResourcesPlugin.FAMILY_AUTO_BUILD); 
		if (build.length >= 1) {
			try {
				Job job = build[0];
				if (log && Activator.getMessageOptionsService().isBundleOperations()) {
					String state;
					switch (job.getState()) {
					case Job.RUNNING:
						state = "Running";
						break;
					case Job.SLEEPING:
						state = "Sleeping";
						break;
					case Job.WAITING:
						state = "Waiting";
						break;
					case Job.NONE:
					default:
						state = "Unknown";
						break;
					}
					Activator.log(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, 
							NLS.bind(Msg.WAITING_ON_JOB_INFO, job.getName(), state)));
				}
				job.join();
				// Only log once
				waitOnBuilder(false);
			} catch (InterruptedException e) {
				Activator.log(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, 
						Msg.BUILDER_INTERRUPT_INFO, e));				
			} catch (ExtenderException | BundleLogException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}

	@Override
	public Boolean hasBundleJobState() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		if (jobs.length > 0) {
			return true;
		} else {
			return false;
		}
	}

	/** 
	 * Find the first waiting bundle job
	 * 
	 * @return bundle job waiting or null if no bundle is in state waiting
	 */
	static public BundleExecutor getWaitingBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.WAITING && job instanceof BundleJob) {
				return (BundleJob) job;
			}
		}
		return null;
	}
	
	/** 
	 * Find the running bundle job
	 * 
	 * @return bundle job running or null if no bundle is in state running
	 */
	 @Override
	public BundleExecutor getRunningBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.RUNNING && job instanceof BundleJob) {
				return (BundleExecutor) job;
			}
		}
		return null;
	}

	 
	 public void waitOnBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] bundleJobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		if (bundleJobs.length >= 1) {
			try {
				bundleJobs[0].join();
				waitOnBundleJob();
			} catch (InterruptedException e) {
				Activator.log(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, 
						Msg.BUILDER_INTERRUPT_INFO, e));				
			} catch (BundleLogException | ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		}
	}

	/**
	 * Check if a job with the specified job name is running.
	 * @param jobName is the name returned by {@code Job#getName()}
	 * @return true if the job with the specified job is running.
	 */
	@SuppressWarnings("unused")
	static private Boolean getBundleJobRunState(String jobName) {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			String name = job.getName();
			if (job.getState() == Job.RUNNING && name.equals(jobName)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}
}
