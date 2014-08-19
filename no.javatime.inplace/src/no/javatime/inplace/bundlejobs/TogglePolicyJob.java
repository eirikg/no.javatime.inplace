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
import java.util.Collections;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Toggles between lazy and eager activation
 */
public class TogglePolicyJob extends BundleJob {

	/** Standard name of a toggle policy job job */
	final public static String policyJobName = Message.getInstance().formatString(
			"toggle_policy_job_name");

	/**
	 * Construct a toggle policy job with a given name
	 * 
	 * @param name job name
	 */
	public TogglePolicyJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to toggle
	 * 
	 * @param name job name
	 * @param projects projects to toggle
	 */
	public TogglePolicyJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a toggle policy job with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to toggle
	 */
	public TogglePolicyJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) toggle policy operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list and
	 * {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if any
	 * status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			BundleManager.addBundleTransitionListener(this);
			for (IProject project : getPendingProjects()) {
				try {
					BundleProjectSettings.toggleActivationPolicy(project);
					// No bundle jobs (which updates the bundle view) are run when the project(s) are
					// deactivated or auto build is off
					Bundle bundle = bundleRegion.get(project);
					if (!ProjectProperties.isAutoBuilding()) {
						if (bundleRegion.isActivated(bundle)) {
							String msg = WarnMessage.getInstance().formatString("policy_updated_auto_build_off",
									project.getName());
							addWarning(null, msg, project);
						}
					}
					try {
						if (InPlace.get().msgOpt().isBundleOperations()
								&& !InPlace.get().getCommandOptionsService().isUpdateOnBuild()) {
							if (bundleRegion.isActivated(bundle)) {
								addInfoMessage(NLS.bind(Msg.AUTOUPDATE_OFF_INFO, project.getName()));
							}
						}
					} catch (InPlaceException e) {
						addError(e, project);
					}
					if (null != bundle) {
						if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
							reInstall(Collections.<IProject> singletonList(project), new SubProgressMonitor(
									monitor, 1));
						} else if ((bundle.getState() & (Bundle.RESOLVED)) != 0) {
							// Do not start bundle if in state resolve when toggling policy
							BundleJobManager.getTransition().addPending(bundle, Transition.RESOLVE);
						}
					}
				} catch (InPlaceException e) {
					String msg = ExceptionMessage.getInstance().formatString("error_set_policy",
							project.getName());
					addError(e, msg, project);
				}
			}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
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
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg);
		} finally {
			monitor.done();
			BundleManager.removeBundleTransitionListener(this);
		}
	}
}
