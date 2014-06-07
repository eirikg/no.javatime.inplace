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
package no.javatime.inplace.bundleproject;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.UserMessage;

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

/**
 * Adaption of the save dialog before launching in Eclipse.
 *
 */
@SuppressWarnings("restriction")
public class OpenProjectHandler extends SaveScopeResourcesHandler {
	
	private Collection<IProject> dirtyProjects = new LinkedHashSet<IProject>();

	public Collection<IProject> getDirtyProjects() {
		areResourcesDirty();
		return dirtyProjects;
	}

	public Collection<IProject> getDirtyProjects(Collection<IProject> projects) {
		Collection<IProject> dirtyProjects = new LinkedHashSet<IProject>(projects);
		dirtyProjects.retainAll(this.dirtyProjects);
		return dirtyProjects;
	}

	public Boolean hasDirtyProjects() {
		return (dirtyProjects.size() > 0) ? Boolean.TRUE : Boolean.FALSE; 
	}
	
	/**
	 * Shows a file save dialog of all changed files in the workspace
	 * @return true if files are saved or no files are modified. False if modified files are not saved 
	 * 	 
	 * */
	public Boolean saveModifiedFiles() {
		//PlatformUI.getWorkbench().saveAllEditors(true);
		Boolean isDirty = areResourcesDirty();
		if (!isDirty) {
			return Boolean.TRUE;
		}	
		Collection<IProject> projects = ProjectProperties.getProjects();
    IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
    String save = store.getString(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH);
    int ret = showSaveDialog(projects.toArray(new IProject[projects.size()]), !save.equals(MessageDialogWithToggle.NEVER), save.equals(MessageDialogWithToggle.PROMPT));
    if(ret == IDialogConstants.OK_ID) {
    	doSave();
    	return Boolean.TRUE;
    }
    return Boolean.FALSE;
	}
	
	/**
	 * Wait for automatic build to finish before returning,
	 */
	static public void waitOnBuilder() {

		IJobManager jobMan = Job.getJobManager();
		Job[] build = jobMan.find(ResourcesPlugin.FAMILY_AUTO_BUILD); 
		if (build.length >= 1) {
			try {
				if (Category.getState(Category.infoMessages)) {
					UserMessage.getInstance().getString("start_waiting_on_builder",  build[0].getName());
				}
				build[0].join();
				waitOnBuilder();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Wait for a bundle job to finish before returning,
	 */
	static public void waitOnBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] bundleJobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE); 
		if (bundleJobs.length >= 1) {
			try {
				if (Category.getState(Category.infoMessages)) {
					UserMessage.getInstance().getString("start_waiting_on_builder",  bundleJobs[0].getName());
				}
				bundleJobs[0].join();
				waitOnBundleJob();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Check if there is a job belonging to the {@code BundleJob.FAMILY_BUNDLE_LIFECYCLE} and
	 * is either running, waiting or sleeping
	 * @return true if a bundle job is running, waiting or sleeping, otherwise false.
	 */
	static public Boolean getBundlesJobRunState() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE); 
		if (jobs.length > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/** Find the running bundle job
	 * @return bundle job running or null if no bundle is in state running
	 */
	static public BundleJob getRunningBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE); 
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.getState() == Job.RUNNING && job instanceof BundleJob) {
				return (BundleJob) job;
			}
		}
		return null;
	}

	/**
	 * Check if a job with the specified job name is running.
	 * @param jobName is the name returned by {@code Job#getName()}
	 * @return true if the job with the specified job is running.
	 */
	static public Boolean getBundleJobRunState(String jobName) {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE); 
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			String name = job.getName();
			if (job.getState() == Job.RUNNING && name.equals(jobName)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * Check if any files in workspace are modified and unsaved
	 * @return true if there are unsaved modified files in workspace 
	 */
	public Boolean areResourcesDirty () {
		
		Collection<IProject> projects = ProjectProperties.getProjects();	
		IResource[] resources = getScopedDirtyResources(projects.toArray(new IProject[projects.size()]));
		if (resources.length > 0) {
			this.dirtyProjects.clear();
			for (int i = 0; i < resources.length; i++) {
				IProject project = resources[i].getProject();
				if (null != project) {
					this.dirtyProjects.add(project);
				}
			}
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
