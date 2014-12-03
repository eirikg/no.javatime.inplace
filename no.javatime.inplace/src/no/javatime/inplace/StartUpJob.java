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
package no.javatime.inplace;

import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * In an activated workspace install all projects and set activated projects to the same state as
 * they had at the last shutdown. If the workspace is deactivated set deactivated projects to
 * {@code Transition#UNINSTALL}. If a project has never been activated the default state for the
 * transition will be {@code Transition#NOTRANSITION}
 * 
 */
public class StartUpJob extends NatureJob {

	final public static String startupName = Msg.INIT_WORKSPACE_JOB;

	/**
	 * Construct a startup job with a given name
	 * 
	 * @param name job name
	 */
	public StartUpJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to start
	 * 
	 * @param name job name
	 * @param projects projects to toggle
	 */
	public StartUpJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a starupjob with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to toggle
	 */
	public StartUpJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) startup operation
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated
	 * normally and no status objects have been added to this job status list and
	 * {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if any
	 * status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
		try {
				if (InPlace.get().getMsgOpt().isBundleOperations()) {
					String osgiDev = InPlace.getbundlePrrojectMetaService().inDevelopmentMode();
					if (null != osgiDev) {
						String msg = NLS.bind(Msg.CLASS_PATH_DEV_PARAM_INFO, osgiDev);
						//InPlace.get().log(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
						addLogStatus(new BundleStatus(StatusCode.INFO, InPlace.PLUGIN_ID, msg));
					}
				}
				ActivateBundleJob activateJob = new ActivateBundleJob(ActivateBundleJob.activateStartupJobName);
				Collection<IProject> activatedProjects = getNatureEnabled();
				if (activatedProjects.size() > 0) {
					Collection<IProject> deactivatedProjects = deactivateBuildErrorClosures(activatedProjects);
					if (deactivatedProjects.size() > 0) {
						initDeactivatedWorkspace();
					} else {
						// Install all projects and set activated projects to the same state as they had at shutdown
						activateJob.addPendingProjects(activatedProjects);
						activateJob.setUseStoredState(true);
						BundleJobManager.addBundleJob(activateJob, 0);
					}
				} else {
					// Register all projects as bundle projects and set their initial transition
					initDeactivatedWorkspace();
				}
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (InPlaceException | ExtenderException e) {
			addError(e, Msg.INIT_BUNDLE_STATE_ERROR);
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
		}
	}

	/**
	 * Register all bundle projects and set the transition for all deactivated projects to the last
	 * transition before shut down. If a project has never been activated the default state for the
	 * transition will be {@code Transition#NOTRANSITION}
	 */
	private void initDeactivatedWorkspace() {

		IBundleStatus status = null;

		try {
			final IEclipsePreferences store = InPlace.getEclipsePreferenceStore();
			if (null == store) {
				addStatus(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
						Msg.INIT_WORKSPACE_STORE_WARN, null));
				return;
			}
			Collection<IProject> plugins = bundleProjectCandidates.getBundleProjects();
			for (IProject project : plugins) {
				try {
					String symbolicKey = bundleRegion.getSymbolicKey(null, project);
					int state = store.getInt(symbolicKey, Transition.UNINSTALL.ordinal());
					// Register all projects
					if (state == Transition.UNINSTALL.ordinal()) {
						bundleRegion.registerBundleProject(project, null, false);
						bundleTransition.setTransition(project, Transition.NOTRANSITION);
					} else if (state == Transition.REFRESH.ordinal()) {
						bundleRegion.registerBundleProject(project, null, false);
						bundleTransition.setTransition(project, Transition.REFRESH);
					} else {
						bundleRegion.registerBundleProject(project, null, false);
						bundleTransition.setTransition(project, Transition.NOTRANSITION);									
					}
				} catch (ProjectLocationException e) {
					if (null == status) {
						String msg = ExceptionMessage.getInstance().formatString("project_init_location");
						status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, null);
					}
					status.add(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project,
							project.getName(), e));
					addStatus(status);
				}
			}
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		}
	}
	
	/**
	 * Deactivate workspace if there are any build error closures. Build error closures are
	 * deactivated at shutdown so a build error closure at this point indicates that there
	 * has been an abnormal shutdown. The initial state of both deactivated and activated
	 * bundles are initially uninstalled at startup. Closures to deactivate are:
	 * <p>
	 * <ol>
	 * <li><b>Providing resolve closures</b>
	 * <p>
	 * <br>
	 * <b>Deactivated providing closure.</b> Resolve is rejected when deactivated bundles with build
	 * errors provides capabilities to projects to resolve (and start). This closure require the
	 * providing bundles to be activated when the requiring bundles are resolved. This is usually an
	 * impossible position. Activating and updating does not allow a requiring bundle to activate
	 * without activating the providing bundle.
	 * <p>
	 * <br>
	 * <b>Activated providing closure.</b> It is illegal to resolve an activated project when there
	 * are activated bundles with build errors that provides capabilities to the project to resolve.
	 * The requiring bundles to resolve will force the providing bundles with build errors to resolve.
	 * </ol>
	 * 
	 * @param activatedProjects all activated bundle projects
	 * @return projects that are deactivated or an empty set
	 */
	private Collection<IProject> deactivateBuildErrorClosures(Collection<IProject> activatedProjects) {

		DeactivateJob deactivateErrorClosureJob = new DeactivateJob("Deactivate on startup");
		try {
			// Deactivated and activated providing closure. Deactivated and activated projects with build
			// errors providing capabilities to project to resolve (and start) at startup
			BuildErrorClosure be = new BuildErrorClosure(activatedProjects, Transition.DEACTIVATE,
					Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
			if (be.hasBuildErrors()) {
				deactivateErrorClosureJob.addPendingProjects(activatedProjects);
				deactivateErrorClosureJob.setUser(false);
				deactivateErrorClosureJob.schedule();
				return activatedProjects;
			}
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference_termination");
			IBundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg,
					null);
			multiStatus.add(e.getStatusList());
			StatusManager.getManager().handle(multiStatus, StatusManager.LOG);
		}
		return Collections.<IProject> emptySet();
	}
}
