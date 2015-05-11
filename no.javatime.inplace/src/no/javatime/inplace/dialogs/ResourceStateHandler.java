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
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.SaveScopeResourcesHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Adaption of the save dialog before launching in Eclipse.
 *
 */
@SuppressWarnings("restriction")
public class ResourceStateHandler extends SaveScopeResourcesHandler implements ResourceState {
	
	private Collection<IProject> dirtyProjects; //= new LinkedHashSet<IProject>();

	public ResourceStateHandler() {
		dirtyProjects = new LinkedHashSet<IProject>();	
	}
	
	/* (non-Javadoc)
	 * @see no.javatime.inplace.dialogs.ResourceState#getDirtyProjects()
	 */
	@Override
	public Collection<IProject> getDirtyProjects() {
		synchronized (dirtyProjects) {
			areResourcesDirty();
			Collection<IProject> dirtyProjects = new LinkedHashSet<IProject>(this.dirtyProjects);
			return dirtyProjects;
		}
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dialogs.ResourceState#getDirtyProjects(java.util.Collection)
	 */
	@Override
	public Collection<IProject> getDirtyProjects(Collection<IProject> projects) {

		synchronized (dirtyProjects) {
			Collection<IProject> dirtyProjects = new LinkedHashSet<IProject>(projects);
			dirtyProjects.retainAll(this.dirtyProjects);
			return dirtyProjects;
		}
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dialogs.ResourceState#hasDirtyProjects()
	 */

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dialogs.ResourceState#areResourcesDirty()
	 */
	@Override
	public Boolean areResourcesDirty () {
		
		Collection<IProject> projects = Activator.getBundleProjectCandidatesService().getProjects();	
		IResource[] resources = getScopedDirtyResources(projects.toArray(new IProject[projects.size()]));
		if (resources.length > 0) {
			synchronized (dirtyProjects) {
				this.dirtyProjects.clear();
				for (int i = 0; i < resources.length; i++) {
					IProject project = resources[i].getProject();
					if (null != project) {
						this.dirtyProjects.add(project);
					}
				}
			}
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	
	@Override
	public Boolean saveModifiedResources() {
		
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return false;
		}
		return true;
	}
	
	@Override
	public Boolean saveModifiedFiles() {

		Boolean isDirty = areResourcesDirty();
		if (!isDirty) {
			return Boolean.TRUE;
		}	
		Collection<IProject> projects = Activator.getBundleProjectCandidatesService().getProjects();
    IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
    String save = store.getString(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH);
    int ret = showSaveDialog(projects.toArray(new IProject[projects.size()]), 
    		!save.equals(MessageDialogWithToggle.NEVER), save.equals(MessageDialogWithToggle.PROMPT));
    if(ret == IDialogConstants.OK_ID) {
    	doSave();
    	return Boolean.TRUE;
    }
    return Boolean.FALSE;
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

	/**
	 * Wait for a bundle job to finish before returning,
	 */
	public void waitOnBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] bundleJobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE); 
		if (bundleJobs.length >= 1) {
			try {
				if (Activator.getMessageOptionsService().isBundleOperations()) {
					Activator.log(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, 
							NLS.bind(Msg.WAITING_ON_JOB_INFO, bundleJobs[0].getName())));
				}
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
