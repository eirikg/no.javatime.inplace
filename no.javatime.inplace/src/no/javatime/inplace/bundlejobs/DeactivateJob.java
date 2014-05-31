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
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.BundleClosures;
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.bundle.log.intface.BundleLogView;
import no.javatime.inplace.bundle.log.intface.BundleLog;
import no.javatime.inplace.bundle.log.intface.BundleLog.Device;
import no.javatime.inplace.bundle.log.intface.BundleLog.MessageType;
import no.javatime.inplace.bundle.log.status.BundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus;
import no.javatime.inplace.bundle.log.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Projects are deactivated by removing the JavaTime nature from the projects and moving them to state INSTALLED in an
 * activated workspace and state UNINSTALLED in a deactivated workspace.
 * <p>
 * If the workspace is deactivated (that is when the last bundle in the workspace is deactivated) all bundles are moved
 * to state UNINSTALLED.
 * <p>
 * Calculate closure of projects and add them as pending projects to this job before the projects are deactivated
 * according to the current dependency option.
 * 
 * @see no.javatime.inplace.bundlejobs.ActivateProjectJob
 * @see no.javatime.inplace.bundlejobs.ActivateBundleJob
 */
public class DeactivateJob extends NatureJob {

	/** Standard name of an deactivate job */
	final public static String deactivateJobName = Message.getInstance().formatString("deactivate_job_name");
	final public static String deactivateWorkspaceJobName = Message.getInstance().formatString(
			"deactivate_workspace_job_name");

	/** Can be used at IDE shut down */
	final public static String deactivateOnshutDownJobName = Message.getInstance().formatString(
			"deactivate_on_shutDown_job_name");
	/** Used to name the set of operations needed to deactivate a bundle */
	final private static String deactivateTask = Message.getInstance().formatString("deactivate_task_name");

	/**
	 * Construct a deactivate job with a given name
	 * 
	 * @param name job name
	 */
	public DeactivateJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects and their corresponding bundles to deactivate
	 * 
	 * @param name job name
	 * @param projects bundle projects to deactivate
	 */
	public DeactivateJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a job with a given name and a project and its corresponding bundle to deactivate
	 * 
	 * @param name job name
	 * @param project bundle projects to deactivate
	 */
	public DeactivateJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the project(s) and bundle(s) deactivate operation.
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no status
	 *         objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the job fails or
	 *         {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(deactivateTask, getTicks());
			deactivate(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (OperationCanceledException e) {
			String msg = UserMessage.getInstance().formatString("cancel_job", getName());
			addCancelMessage(e, msg);
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors", getName());
			addError(e, msg);
		} catch (NullPointerException e) {
			String msg = ExceptionMessage.getInstance().formatString("npe_job", getName());
			addError(e, msg);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			monitor.done();
		}
		try {
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		}
	}

	/**
	 * Deactivate added pending bundle projects by removing the JavaTime nature, and move all their bundles to state
	 * installed or uninstalled. If there are other activated projects that requires capabilities from the set of initial
	 * added projects they are automatically added as pending projects. Beside this, projects are added according to the
	 * current dependency settings.
	 * <p>
	 * This deactivate method can be invoked either directly after pending projects are added or indirectly when scheduled
	 * as a bundle job
	 * 
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of deactivating with {@code StatusCode.OK} if no failure, otherwise one
	 *         of the failure codes are returned. If more than one bundle fails, status of the last failed bundle is
	 *         returned. All failures are added to the job status list
	 * @throws InPlaceException if one of the projects to deactivate does not exist or is closed
	 * @throws InterruptedException if the deactivate process is interrupted internally or from an external source.
	 *           Deactivate is also interrupted if a task running the stop method is terminated abnormally
	 * @throws CircularReferenceException if cycles are detected among the specified projects
	 * @throws OperationCanceledException cancels at appropriate places on a cancel request from an external source
	 * @see #addPendingProject(IProject)
	 * @see #getStatusList()
	 */
	public IBundleStatus deactivate(IProgressMonitor monitor) throws InPlaceException, InterruptedException,
			CircularReferenceException, OperationCanceledException {

		BundleSorter bundleSorter = new BundleSorter();
		bundleSorter.setAllowCycles(Boolean.TRUE);

		BundleClosures pd = new BundleClosures();
		Collection<IProject> projects = pd.projectDeactivation(getPendingProjects(), true);
		resetPendingProjects(projects);
		Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());

		// Inform about build errors in projects to deactivate
		if (Category.getState(Category.infoMessages)) {
			ProjectSorter projectSorter = new ProjectSorter();
			Collection<IProject> errorProjectClosures = projectSorter.getRequiringBuildErrorClosure(
					getPendingProjects(), true);
			if (errorProjectClosures.size() > 0) {
				String msg = WarnMessage.getInstance().formatString("deactivate_with_build_errors",
						ProjectProperties.formatProjectList(errorProjectClosures));
				addWarning(null, msg, null);
			}
		}
		InPlace.get().savePluginSettings(true, true);
		// All not activated bundles are collectively either in state installed or in state uninstalled.
		if (ProjectProperties.getActivatedProjects().size() <= pendingProjects()) {
			// This is the last project(s) to deactivate, move all bundles to state uninstalled
			Collection<Bundle> allWorkspaceBundles = bundleRegion.getBundles();
			// Sort to stop and uninstall in dependency order
			Collection<Bundle> bundles = bundleSorter.sortDeclaredRequiringBundles(allWorkspaceBundles,
					allWorkspaceBundles);
			try {
				stop(pendingBundles, null, new SubProgressMonitor(monitor, 1));
				uninstall(bundles, new SubProgressMonitor(monitor, 1), false);
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
				refresh(bundles, new SubProgressMonitor(monitor, 1));
			} catch (InPlaceException e) {
				String msg = ExceptionMessage.getInstance().formatString("deactivate_job_uninstalled_state",
						getName(), bundleRegion.formatBundleList(bundles, true));
				addError(e, msg);
			}
		} else {
			// Deactivate pending and requiring bundle projects
			try {
				// The resolver always include bundles with the same symbolic name in the resolve process
				// TODO let getSymbolicNameDuplicates throw an InPlaceException
				Map<IProject, Bundle> duplicates = bundleRegion.getSymbolicNameDuplicates(
						bundleRegion.getProjects(pendingBundles), bundleRegion.getActivatedBundles(), true);
				Collection<Bundle> bundlesToRestart = null;
				Collection<Bundle> bundlesToResolve = null;
				if (duplicates.size() > 0) {
					for (Bundle bundle : duplicates.values()) {
						if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
							if (null == bundlesToRestart) {
								bundlesToRestart = new LinkedHashSet<Bundle>();
							}
							bundlesToRestart.add(bundle);
						} else if ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
							if (null != bundlesToResolve) {
								bundlesToResolve = new LinkedHashSet<Bundle>();
							}
							bundlesToResolve.addAll(getBundlesToResolve(Collections.<Bundle>singletonList(bundle)));
						}
					}
					stop(bundlesToRestart,Closure.REQUIRING, new SubProgressMonitor(monitor, 1));
				}

				// Do not refresh bundles already in state installed
				Collection<Bundle> installedBundles = bundleRegion.getBundles(Bundle.INSTALLED);
				pendingBundles.removeAll(installedBundles);
				stop(pendingBundles, null, new SubProgressMonitor(monitor, 1));
				deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
				// Nature removed from projects, set all bundles to a deactivated status
				for (Bundle bundle : pendingBundles) {
					bundleRegion.setActivation(bundle, false);
				}
				if (null != bundlesToResolve) {
					pendingBundles.addAll(bundlesToResolve);
				}
				// Deactivated bundles will not be resolved (rejected by the resolver hook) during refresh and thus
				// enter state INSTALLED
				refresh(pendingBundles, new SubProgressMonitor(monitor, 2));
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				if (null != bundlesToRestart) {
					start(bundlesToRestart, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
				}
			} catch (InPlaceException e) {
				String msg = ExceptionMessage.getInstance().formatString("deactivate_job_installed_state", getName(),
						bundleRegion.formatBundleList(pendingBundles, true));
				addError(e, msg);
			}
		}
		return getLastStatus();
	}

	/**
	 * Include bundles to deactivate according to dependency option. Dependency options are providing and partialGraph
	 * while requiring is mandatory. The set of pending projects are not guaranteed to be sorted in dependency order.
	 * 
	 * @param bs topological sorter of bundle projects
	 * 
	 * @return pending bundles and their requiring bundles. Pending projects are updated accordingly
	 * @throws CircularReferenceException if cycles between projects to sort are detected
	 */
	@SuppressWarnings("unused")
	private Collection<Bundle> calculateDependencies() throws CircularReferenceException {

		BundleClosures pd = new BundleClosures();
		Collection<IProject> projects = pd.projectDeactivation(getPendingProjects(), true);
		resetPendingProjects(projects);
		return bundleRegion.getBundles(getPendingProjects());
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public int getTicks() {
		return 4;
	}
}
