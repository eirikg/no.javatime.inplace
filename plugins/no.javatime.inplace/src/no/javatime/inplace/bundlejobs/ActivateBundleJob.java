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
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.ExternalDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

public class ActivateBundleJob extends NatureJob implements ActivateBundle {

	// Bundles to activate
	private Collection<Bundle> activatedBundles;
	private ProjectSorter projectSorter = new ProjectSorter();

	/**
	 * Default constructor with a default job name
	 */
	public ActivateBundleJob() {
		super(Msg.ACTIVATE_BUNDLE_JOB);
		init();
	}

	/**
	 * Construct an activate job with a given job name
	 * 
	 * @param name job name
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name) {
		super(name);
		init();
	}

	/**
	 * Constructs an activation job with a given job name and pending bundle projects to activate
	 * 
	 * @param name job name
	 * @param projects pending projects to activate
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name, Collection<IProject> projects) {
		super(name, projects);
		init();
	}

	/**
	 * Constructs an activation job with a given job name and a pending bundle project to activate
	 * 
	 * @param name job name
	 * @param project pending project to activate
	 * @see Msg#ACTIVATE_BUNDLE_JOB
	 * @see Msg#STARTUP_ACTIVATE_BUNDLE_JOB
	 */
	public ActivateBundleJob(String name, IProject project) {
		super(name, project);
	}

	private void init() {
		
		activatedBundles = null;
	}
	
	/**
	 * Runs the bundle(s) activation operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			monitor.beginTask(Msg.ACTIVATE_BUNDLE_JOB, getTicks());
			activate(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (OperationCanceledException e) {
			addCancel(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addError(multiStatus);
		} catch (ExtenderException e) {
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
		} catch (InPlaceException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
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

	/**
	 * Install, resolve and start pending bundles to activate. A bundle is marked as activated if its
	 * corresponding project is activated (nature enabled). If no projects are activated the activate
	 * bundle job will terminate silently. If the workspace is in a deactivated state and there are
	 * bundles to activate all deactivated bundles are installed.
	 * <p>
	 * Closed and non-existing projects are discarded.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return Status of last added {@code IBundleStatus} object is returned or a
	 * {@code IBundleStatus} status with a {@code StatusCode.OK} if no errors. All failures are added
	 * to the job status list.
	 * @throws OperationCanceledException after install and resolve
	 * @throws InterruptedException Checks for and interrupts right before call to start bundle. Start
	 * is also interrupted if the task running the stop method is terminated abnormally (timeout or
	 * manually)
	 * @throws InPlaceException if encountering closed or non-existing projects after they are
	 * discarded or a bundle to activate becomes null
	 * @throws ExtenderException If failing to get an extender service
	 * @throws BackingStoreException Failure to access the preference store for bundle states 
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 */
	protected IBundleStatus activate(IProgressMonitor monitor) throws OperationCanceledException,
			InterruptedException, InPlaceException, ExtenderException, BackingStoreException, IllegalStateException {

		// At least one project must be activated (nature enabled) for workspace bundles to be activated
		if (isProjectWorkspaceActivated()) {
			// If this is the first set of workspace project(s) that have been activated no bundle(s) have
			// been activated yet and all deactivated bundles should be installed in an activated
			// workspace
			if (!bundleRegion.isRegionActivated()) {
				addPendingProjects(bundleProjectCandidates.getBundleProjects());
			} else {
				Collection<IProject> projects = bundleProjectCandidates.getBundleProjects();
				projects.removeAll(getPendingProjects());
				// If any, add uninstalled bundles to be installed in an activated workspace
				for (IProject project : projects) {
					if (null == bundleRegion.getBundle(project)) {
						addPendingProject(project);
					}
				}
			}
			// Add providing projects and remove projects with build errors, cycles, duplicates and
			// affected dependent projects before installing
			try {
				resetPendingProjects(projectSorter.sortProvidingProjects(getPendingProjects()));
			} catch (CircularReferenceException e) {
				String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
				BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
				multiStatus.add(e.getStatusList());
				addError(multiStatus);
				// Remove all pending projects that participate in the cycle(s)
				if (null != e.getProjects()) {
					removePendingProjects(e.getProjects());
				}
			}
			try {
				activatedBundles = install(getPendingProjects(), monitor);
			} catch (InPlaceException | WorkspaceDuplicateException | ExternalDuplicateException | ProjectLocationException e) {
				bundleTransition.addPendingCommand(getActivatedProjects(), Transition.DEACTIVATE);
				return addError(new BundleStatus(StatusCode.JOB_ERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));
			}
		}
		// No projects are activated or no activated bundle projects have been installed
		if (null == activatedBundles || activatedBundles.size() == 0) {
			return getLastErrorStatus();
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// At this point the workspace is activated and all remaining workspace bundles are free of
		// errors and at least installed. Resolve and start activated bundles
		// Only resolve bundles in state installed
		Collection<Bundle> bundlesToResolve = new LinkedHashSet<Bundle>(activatedBundles.size());
		// Only resolve bundles in state installed
		for (Bundle bundle : activatedBundles) {
			if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
				bundlesToResolve.add(bundle);
			}
		}
		if (bundlesToResolve.size() == 0) {
			if (messageOptions.isBundleOperations())
				addLogStatus(Msg.ACTIVATED_BUNDLES_INFO, new Object[] { bundleRegion.formatBundleList(
						activatedBundles, true) }, Activator.getContext().getBundle());
			return getLastErrorStatus();
		}
		Collection<Bundle> notResolvedBundles = resolve(bundlesToResolve, new SubProgressMonitor(
				monitor, 1));
		if (notResolvedBundles.size() > 0) {
			// This should include dependency closures, so no dependent bundles should be started
			activatedBundles.removeAll(notResolvedBundles);
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// Set the bundle class path on start up in case settings (dev and/or update bundle class path) are
		// changed
		if (getName().equals(Msg.STARTUP_ACTIVATE_BUNDLE_JOB)
				&& (null != bundleProjectMeta.inDevelopmentMode() || commandOptions
						.isUpdateDefaultOutPutFolder())) {
			for (Bundle bundle : activatedBundles) {
				resolveBundleClasspath(bundleRegion.getProject(bundle));
			}
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		StatePersistParticipant.restoreSessionState();
		start(activatedBundles, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
		return getLastErrorStatus();
	}	
	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public static int getTicks() {
		return 3; // install (activate workspace), resolve, start
	}
}
