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

/**
 * Removes or inserts the default output folder in the Bundle-ClassPath and schedules a reset job
 * for bundle projects that have been updated when auto build is off.
 */
public class UpdateBundleClassPathJob extends BundleJob {

	private boolean addToPath = true;

	/** Standard name of an update bundle class path job */
	final public static String updateBundleClassJobName = Msg.UPDATE_BUNDLE_CLASS_PATH_JOB;

	/**
	 * Construct an update bundle class path job with a given name
	 * 
	 * @param name job name
	 */
	public UpdateBundleClassPathJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to update
	 * 
	 * @param name job name
	 * @param projects projects to update
	 */
	public UpdateBundleClassPathJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct an update bundle class path job with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to update
	 */
	public UpdateBundleClassPathJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) update bundle class path operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list and
	 * {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if any
	 * status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		final ResetJob resetJob = new ResetJob();

		try {
			BundleTransitionListener.addBundleTransitionListener(this);
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
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					addInfoMessage(Msg.ATOBUILD_OFF_RESET_INFO);
				}
				resetJob.reset(ResetJob.resetJobName);
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (InPlaceException | ExtenderException e) {
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
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
	}

	/**
	 * Determines whether the default output folder is added to the Bundle-ClassPath or removed from
	 * the Bundle-ClassPath
	 * 
	 * @return true if the default output folder is added and false if default output folder is
	 * removed
	 */
	public boolean isAddToPath() {
		return addToPath;
	}

	/**
	 * Determines whether to add the default output folder to the Bundle-ClassPath or remove the
	 * default output folder from the Bundle-ClassPath
	 * <p>
	 * Default is to add the default output folder to the Bundle-ClassPath
	 * 
	 * @param addToPath add default output folder if true and remove default output folder if false
	 */
	public void setAddToPath(boolean addToPath) {
		this.addToPath = addToPath;
	}
}
