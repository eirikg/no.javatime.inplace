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

import no.javatime.inplace.Activator;
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Note that the deactivate job should try to deactivate all pending projects independent
 * of any kind of build errors.
 */
public class DeactivateJob extends NatureJob implements Deactivate {

	/** Ignore build errors when deactivating */
	private boolean checkBuildErrors;

	/**
	 * Default constructor wit a default job name
	 */
	public DeactivateJob() {
		super(Msg.DEACTIVATE_BUNDLES_JOB);
		init();
	}

	/**
	 * Construct a deactivate job with a given name
	 * 
	 * @param name job name
	 */
	public DeactivateJob(String name) {
		super(name);
		init();
	}

	/**
	 * Construct a job with a given name and projects and their corresponding bundles to deactivate
	 * 
	 * @param name job name
	 * @param projects bundle projects to deactivate
	 */
	public DeactivateJob(String name, Collection<IProject> projects) {
		super(name, projects);
		init();
	}

	/**
	 * Construct a job with a given name and a project and its corresponding bundle to deactivate
	 * 
	 * @param name job name
	 * @param project bundle projects to deactivate
	 */
	public DeactivateJob(String name, IProject project) {
		super(name, project);
		init();
	}

	private void init() {
		checkBuildErrors = false;
	}

	@Override
	public void end() {
		super.end();
		init();
	}

	/**
	 * Runs the project(s) and bundle(s) deactivate operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.DEACTIVATE_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			deactivate(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (ExtenderException e) {
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			addError(e, msg);
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

	/**
	 * Deactivate added pending bundle projects by removing the JavaTime nature, and move all their
	 * bundles to state installed or uninstalled. If there are other activated projects that requires
	 * capabilities from the set of initial added projects they are automatically added as pending
	 * projects. Beside this, projects are added according to the current dependency settings.
	 * <p>
	 * This deactivate method can be invoked either directly after pending projects are added or
	 * indirectly when scheduled as a bundle job
	 * 
	 * @param monitor monitor the progress monitor to use for reporting progress to the user.
	 * @return status object describing the result of deactivating with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException if one of the projects to deactivate does not exist or is closed
	 * @throws InterruptedException if the deactivate process is interrupted internally or from an
	 * external source. Deactivate is also interrupted if a task running the stop method is terminated
	 * abnormally
	 * @throws CircularReferenceException if cycles are detected among the specified projects
	 * @throws OperationCanceledException cancels at appropriate places on a cancel request from an
	 * external source
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 * @see #addPendingProject(IProject)
	 * @see #getErrorStatusList()
	 */
	public IBundleStatus deactivate(IProgressMonitor monitor) throws InPlaceException,
			InterruptedException, CircularReferenceException, OperationCanceledException,
			BackingStoreException, IllegalStateException {

		// Disable nature of uninstalled projects
		// TODO Optimize. Only get uninstalled bundles
		Collection<IProject> installeableProjects = bundleProjectCandidates.getInstallable();
		for (IProject project : installeableProjects) {
			// If null the bundle is not registered
			if (null == bundleRegion.getBundle(project)) {
				deactivateNature(Collections.<IProject> singletonList(project), new SubProgressMonitor(
						monitor, 1));
				removePendingProject(project);
			}
		}
		BundleClosures closure = new BundleClosures();
		Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
		// All not activated bundles are collectively either in state installed or in state uninstalled.
		Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
		pendingBundles = closure.bundleDeactivation(pendingBundles, activatedBundles);
		resetPendingProjects(bundleRegion.getProjects(pendingBundles));
		saveDirtyMetaFiles(true);
		if (isCheckBuildErrors()) {
			deactivateBuildErrorClosure(getPendingProjects());
		}
		if (activatedBundles.size() <= pendingBundles.size()) {
			// This is the last project(s) to deactivate, move all bundles to state uninstalled
			Collection<Bundle> allBundles = bundleRegion.getBundles();
			allBundles = closure.bundleDeactivation(allBundles, allBundles);
			try {
				stop(pendingBundles, null, new SubProgressMonitor(monitor, 1));
				uninstall(allBundles, new SubProgressMonitor(monitor, 1), true, false);
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
				StatePersistParticipant.saveTransitionState(
						StatePersistParticipant.getSessionPreferences(), true);
			} catch (InPlaceException e) {
				String msg = ExceptionMessage.getInstance().formatString(
						"deactivate_job_uninstalled_state", getName(),
						bundleRegion.formatBundleList(allBundles, true));
				addError(e, msg);
			}
		} else {
			// Deactivate pending and requiring bundle projects
			try {
				// The resolver always include bundles with the same symbolic name in the resolve process
				// TODO let getSymbolicNameDuplicates throw an InPlaceException
				Map<IProject, Bundle> duplicates = bundleRegion.getSymbolicNameDuplicates(
						getPendingProjects(), bundleRegion.getActivatedBundles());
				Collection<Bundle> bundlesToRestart = null;
				Collection<Bundle> bundlesToRefresh = null;
				if (duplicates.size() > 0) {
					for (Bundle bundle : duplicates.values()) {
						if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
							if (null == bundlesToRestart) {
								bundlesToRestart = new LinkedHashSet<Bundle>();
							}
							bundlesToRestart.add(bundle);
						} else if ((bundle.getState() & (Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
							if (null != bundlesToRefresh) {
								bundlesToRefresh = new LinkedHashSet<Bundle>();
							}
							bundlesToRefresh.addAll(getRefreshClosure(Collections.<Bundle> singletonList(bundle),
									activatedBundles));
						}
					}
					stop(bundlesToRestart, Closure.REQUIRING, new SubProgressMonitor(monitor, 1));
				}

				// Do not refresh bundles already in state installed
				Collection<Bundle> installedBundles = bundleRegion.getBundles(Bundle.INSTALLED);
				pendingBundles.removeAll(installedBundles);
				stop(pendingBundles, null, new SubProgressMonitor(monitor, 1));
				deactivateNature(getPendingProjects(), new SubProgressMonitor(monitor, 1));
				// Nature removed from projects, set all bundles to a deactivated status
				if (null != bundlesToRefresh) {
					pendingBundles.addAll(bundlesToRefresh);
				}
				try {
					// Deactivated bundles will not be resolved (rejected by the resolver hook) during refresh
					// and thus enter state INSTALLED
					refresh(pendingBundles, new SubProgressMonitor(monitor, 2));
				} catch (InPlaceException e) {
					handleRefreshException(new SubProgressMonitor(monitor, 1), e, pendingBundles);
					if (null != bundlesToRestart) {
						bundlesToRestart.removeAll(pendingBundles);
					}
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				if (null != bundlesToRestart) {
					start(bundlesToRestart, Closure.PROVIDING, new SubProgressMonitor(monitor, 1));
				}
			} catch (InPlaceException e) {
				String msg = ExceptionMessage.getInstance().formatString("deactivate_job_installed_state",
						getName(), bundleRegion.formatBundleList(pendingBundles, true));
				addError(e, msg);
			}
		}
		return getLastErrorStatus();
	}

	/**
	 * Adds the specified exception to the log and reinstalls bundles that are at least in state
	 * resolved
	 * 
	 * @param monitor Progress monitor for reinstalling bundles
	 * @param throwable The refresh exception
	 * @param bundlesToResolve bundles to refresh/resolve
	 */
	private void handleRefreshException(IProgressMonitor monitor, Throwable throwable,
			Collection<Bundle> bundlesToResolve) {

		SubMonitor progress = SubMonitor.convert(monitor, bundlesToResolve.size());
		try {
			addError(throwable, NLS.bind(Msg.REFRESH_EXP, getName()));
			Collection<Bundle> notInstalledBundles = bundleRegion.getBundles(bundlesToResolve,
					Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING);
			if (notInstalledBundles.size() > 0) {
				reInstall(bundleRegion.getProjects(bundlesToResolve), monitor, false, Bundle.RESOLVED);
			}
		} finally {
			progress.worked(1);
		}
		
		
	}

	private Collection<IProject> deactivateBuildErrorClosure(Collection<IProject> activatedProjects) {

		BundleBuildErrorClosure be = new BundleBuildErrorClosure(activatedProjects,
				Transition.DEACTIVATE, Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
			Collection<IProject> buildErrorClosures = be.getBuildErrorClosures(true);
			if (buildErrorClosures.size() > 0) {
			try {
				String msg = NLS.bind(Msg.DEACTIVATE_BUILD_ERROR_INFO,
						new Object[] { bundleProjectCandidates.formatProjectList(buildErrorClosures),
						bundleProjectCandidates.formatProjectList(be.getBuildErrors(true)) });
				be.setBuildErrorHeaderMessage(msg);
				IBundleStatus bundleStatus = be.getErrorClosureStatus();
				if (bundleStatus.getStatusCode() != StatusCode.OK) {
					addLogStatus(bundleStatus);
				}
			} catch (ExtenderException e) {
				addLogStatus(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e));
			}
			return buildErrorClosures;
		}
		return Collections.<IProject> emptySet();
	}

	@Override
	public boolean isCheckBuildErrors() {
		return checkBuildErrors;
	}

	@Override
	public void setCheckBuildErrors(boolean checkBuildErrors) {
		this.checkBuildErrors = checkBuildErrors;
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
