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
import java.util.EnumSet;
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
import no.javatime.inplace.dependencies.BundleSorter;
import no.javatime.inplace.dependencies.CircularReferenceException;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Projects are deactivated by removing the JavaTime nature from the projects and moving them to state
 * INSTALLED in an activated workspace and state UNINSTALLED in a deactivated workspace.
 * <p>
 * If the workspace is deactivated (that is when the last bundle in the workspace is deactivated) all bundles
 * are moved to state UNINSTALLED.
 * <p>
 * Calculate closure of projects and add them as pending projects to this job before the projects are
 * deactivated according to the current dependency option.
 * 
 * @see no.javatime.inplace.bundlejobs.ActivateProjectJob
 * @see no.javatime.inplace.bundlejobs.ActivateBundleJob
 */
public class DeactivateJob extends NatureJob {

	/** Standard name of an deactivate job */
	final public static String deactivateJobName = Message.getInstance().formatString("deactivate_job_name");
	final public static String deactivateWorkspaceJobName = Message.getInstance().formatString("deactivate_workspace_job_name");

	
	/** Can be used at IDE shut down */
	final public static String deactivateOnshutDownJobName = Message.getInstance().formatString("deactivate_on_shutDown_job_name");
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
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally and no
	 *         status objects have been added to this job status list and {@code BundleStatusCode.ERROR} if the
	 *         job fails or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 *         status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			monitor.beginTask(deactivateTask, getTicks());
			deactivate(monitor);
		} catch(InterruptedException e) {
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
		} catch (CoreException e) {
			String msg = ExceptionMessage.getInstance().formatString("core_exception_job", getName());
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
	 * Deactivate projects by removing the JavaTime nature, and either move their bundles to state installed or
	 * uninstalled. If there are other activated projects that requires capabilities from bundles added to this
	 * job they are automatically added as pending projects to the job. Beside this projects are added to this
	 * job according to the current dependency settings.
	 * 
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of deactivating with {@code StatusCode.OK} if no failure,
	 *         otherwise one of the failure codes are returned. If more than one bundle fails, status of the
	 *         last failed bundle is returned. All failures are added to the job status list
	 */
	private IBundleStatus deactivate(IProgressMonitor monitor) throws InPlaceException, InterruptedException, CoreException {

		BundleSorter bs = new BundleSorter();
		bs.setAllowCycles(Boolean.TRUE);
		Collection<Bundle> pendingBundles  = calculateDependencies();

		if (Category.getState(Category.infoMessages)) {
			ProjectSorter ps = new ProjectSorter();
			Collection<IProject> errorProjectClosures = ps
					.getRequiringBuildErrorClosure(getPendingProjects(), true);
			if (errorProjectClosures.size() > 0) {
				String msg = WarnMessage.getInstance().formatString("deactivate_with_build_errors",
						ProjectProperties.formatProjectList(errorProjectClosures));
				addWarning(null, msg, null);
			}
		}
//		deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
		InPlace.getDefault().savePluginSettings(true, true);
		// Keep a consistent set of states among not activated bundles. All not activated bundles are
		// collectively either in state installed or in state uninstalled.
		// If this is the last project(s) to deactivate, move all bundles to state uninstalled
//		if (ProjectProperties.getActivatedProjects().size() == 0) {
		if (ProjectProperties.getActivatedProjects().size() <= pendingProjects()) {
			Collection<Bundle> allBundles = bundleRegion.getBundles();
			// Only sort bundles
			Collection<Bundle> bundles = bs.sortDeclaredRequiringBundles(allBundles, allBundles);
			try {
				stop(pendingBundles, EnumSet.of(Integrity.RESTRICT),
						new SubProgressMonitor(monitor, 1));
				uninstall(bundles, EnumSet.of(Integrity.RESTRICT),
						new SubProgressMonitor(monitor, 1), false);
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
			try {				
				// The resolver always include bundles with the same symbolic name in the resolve process
				// Take control to obtain valid transitions according to state machine 
				Map<IProject, Bundle> duplicates = bundleRegion.getSymbolicNameDuplicates(bundleRegion.getProjects(pendingBundles), bundleRegion.getActivatedBundles(), true);
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
							bundlesToResolve.addAll(getBundlesToResolve(Collections.singletonList(bundle)));
						}
					}
					stop(bundlesToRestart, EnumSet.of(Integrity.REQUIRING), new SubProgressMonitor(monitor, 1));
				}
				
				// Do not refresh bundles that are already in state installed
				Collection<Bundle> installedBundles = bundleRegion.getBundles(Bundle.INSTALLED);
				pendingBundles.removeAll(installedBundles);
				stop(pendingBundles, EnumSet.of(Integrity.RESTRICT),
						new SubProgressMonitor(monitor, 1));
				// Nature has been removed from project and bundle should have a deactivated status
				for (Bundle bundle : pendingBundles) {
					bundleRegion.setActivation(bundle, false);
				}
				// Project is already deactivated due to removal of its nature and bundle
				// will not be resolved (rejected by the resolver hook) during refresh and thus
				// enter state INSTALLED
				if (null != bundlesToResolve) {
					pendingBundles.addAll(bundlesToResolve);
				}
				deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
				refresh(pendingBundles, new SubProgressMonitor(monitor, 2));
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				if (null != bundlesToRestart) {
					start(bundlesToRestart, EnumSet.of(Integrity.PROVIDING), new SubProgressMonitor(monitor, 1));
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
	 * Include bundles to deactivate according to dependency option. Dependency options are: providing and
	 * partialGraph while requiring is mandatory. The set of pending projects is not guaranteed to be sorted in
	 * dependency order.
	 * @param bs topological sorter of bundle projects
	 * 
	 * @return pending bundles and their requiring bundles. Pending projects are updated
	 *         accordingly
	 */
//	private Collection<Bundle> calculateDependencies(BundleSorter bs, Collection<Bundle> bundleScope) {
//
//		Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
//
//		// If all activated bundles are selected, no dependency option is needed
//		if (pendingBundles.size() == bundleScope.size()) {
//			pendingBundles = bs.sortRequiringBundles(pendingBundles, bundleScope);
//		} else if (Category.getState(Category.partialGraphOnDeactivate)) {
//			int count = 0;
//			do {
//				count = pendingBundles.size();
//				pendingBundles = bs.sortProvidingBundles(pendingBundles, bundleScope);
//				pendingBundles = bs.sortRequiringBundles(pendingBundles, bundleScope);
//			} while (pendingBundles.size() > count);
//			// The providing and requiring option
//		} else if (Category.getState(Category.providingOnDeactivate)) {
//			pendingBundles = bs.sortProvidingBundles(pendingBundles, bundleScope);
//			pendingBundles = bs.sortRequiringBundles(pendingBundles, bundleScope);
//		} else {
//			// Sort bundles in dependency order when requiring option is set
//			pendingBundles = bs.sortRequiringBundles(pendingBundles, bundleScope);
//		}
//		if (pendingBundles.size() > pendingProjects()) {
//			Collection<IProject> projects = bundleRegion.getProjects(pendingBundles);
//			addPendingProjects(projects);
//		}
//		return pendingBundles;
//	}
	private Collection<Bundle> calculateDependencies() {

		ProjectSorter ps = new ProjectSorter();
		ps.setAllowCycles(true);
		// If all activated bundles are selected, no dependency option is needed			
	if (pendingProjects() == ProjectProperties.getActivatedProjects().size()) {
			replacePendingProjects(ps.sortRequiringProjects(getPendingProjects(), true));
		} else if (Category.getState(Category.partialGraphOnDeactivate)) {
			int count = 0;
			do {
				count = pendingProjects();
				replacePendingProjects(ps.sortProvidingProjects(getPendingProjects(), true));				
				replacePendingProjects(ps.sortRequiringProjects(getPendingProjects(), true));				
			} while (pendingProjects()> count);
			// The providing and requiring option
		} else if (Category.getState(Category.providingOnDeactivate)) {
			replacePendingProjects(ps.sortProvidingProjects(getPendingProjects(), true));				
			replacePendingProjects(ps.sortRequiringProjects(getPendingProjects(), true));				
		} else {
			// Sort bundles in dependency order when requiring option is set
			replacePendingProjects(ps.sortRequiringProjects(getPendingProjects(), true));				
		}
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
