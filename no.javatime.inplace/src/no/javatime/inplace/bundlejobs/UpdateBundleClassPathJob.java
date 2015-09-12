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
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.UpdateBundleClassPath;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;

public class UpdateBundleClassPathJob extends NatureJob implements UpdateBundleClassPath {

	private boolean addToPath = true;

	/**
	 * Default constructor wit a default job name
	 */
	public UpdateBundleClassPathJob() {
		super(Msg.UPDATE_BUNDLE_CLASS_PATH_JOB);
		init();
	}
	
	/**
	 * Construct an update bundle class path job with a given name
	 * 
	 * @param name job name
	 */
	public UpdateBundleClassPathJob(String name) {
		super(name);
		init();
	}

	/**
	 * Construct a job with a given name and projects to update
	 * 
	 * @param name job name
	 * @param projects projects to update
	 */
	public UpdateBundleClassPathJob(String name, Collection<IProject> projects) {
		super(name, projects);
		init();
	}

	/**
	 * Construct an update bundle class path job with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to update
	 */
	public UpdateBundleClassPathJob(String name, IProject project) {
		super(name, project);
		init();
	}

	private void init() {		
		addToPath = true;
	}
	
	@Override
	public void end() {
		super.end();
		init();
	}

	/**
	 * Runs the bundle project(s) update bundle class path operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()} 
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		final Reset resetJob = new ResetJob();

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(getName(), 1);
			saveDirtyMetaFiles(false);
			for (IProject project : getPendingProjects()) {
				try {
					if (!BuildErrorClosure.hasManifestBuildErrors(project)) {
						if (addToPath) {
							if (bundleProjectMeta.addDefaultOutputFolder(project)) {
								resetJob.addPendingProject(project);
							}
						} else {
							if (bundleProjectMeta.removeDefaultOutputFolder(project)) {
								resetJob.addPendingProject(project);
							}
						}
					}
				} catch (InPlaceException e) {
					String msg = ErrorMessage.getInstance().formatString("error_set_classpath",
							project.getName());
					addError(e, msg, project);
				}
			}
			if (pendingProjects() > 0 && !bundleProjectCandidates.isAutoBuilding()) {
				if (messageOptions.isBundleOperations()) {
					addInfoMessage(Msg.ATOBUILD_OFF_RESET_INFO);
				}
				resetJob.setSaveWorkspaceSnaphot(false);
				Activator.getBundleExecutorEventService().add(resetJob, 0);
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (ExtenderException e) {			
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, e);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();		
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.bundlejobs.UpdateBundleClassPath#isAddToPath()
	 */
	@Override
	public boolean isAddToPath() {
		return addToPath;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.bundlejobs.UpdateBundleClassPath#setAddToPath(boolean)
	 */
	@Override
	public void setAddToPath(boolean addToPath) {
		this.addToPath = addToPath;
	}
}
