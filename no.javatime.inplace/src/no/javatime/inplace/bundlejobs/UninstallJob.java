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
import no.javatime.inplace.StatePersistParticipant;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.closure.CircularReferenceException;
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

public class UninstallJob extends NatureJob implements Uninstall {

	// Remove the bundle project from the workspace region
	private boolean unregisterBundleProject;
	private boolean includeRequiring;

	/**
	 * Default constructor wit a default job name
	 */
	public UninstallJob() {
		super(Msg.UNINSTALL_JOB);
		init();
	}

	/**
	 * Construct an uninstall job with a given name
	 * 
	 * @param name job name
	 */
	public UninstallJob(String name) {
		super(name);
		init();
	}

	/**
	 * Construct a job with a given name and bundle projects to uninstall
	 * 
	 * @param name job name
	 * @param projects bundle projects to uninstall
	 */
	public UninstallJob(String name, Collection<IProject> projects) {
		super(name, projects);
		init();
	}

	/**
	 * Constructs an uninstall job with a given name and a bundle project to uninstall
	 * 
	 * @param name job name
	 * @param project bundle project to uninstall
	 */
	public UninstallJob(String name, IProject project) {
		super(name, project);
		init();
	}

	
	private void init() {
		unregisterBundleProject = false;
		includeRequiring = true;
	}

	@Override
	public void end() {		
		super.end();
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.bundlejobs.Uninstall#isBundleProjectUnregistered()
	 */
	@Override
	public boolean isUnregister() {
		return unregisterBundleProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.bundlejobs.Uninstall#unregisterBundleProject(boolean)
	 */
	@Override
	public void setUnregister(boolean unregister) {
		this.unregisterBundleProject = unregister;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.bundlejobs.Uninstall#isIncludeRequiring()
	 */
	@Override
	public boolean isAddRequiring() {
		return includeRequiring;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.bundlejobs.Uninstall#setIncludeRequiring(boolean)
	 */
	@Override
	public void setAddRequiring(boolean includeRequiring) {
		this.includeRequiring = includeRequiring;
	}

	/**
	 * Runs the bundle(s) uninstall operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			super.runInWorkspace(monitor);
			monitor.beginTask(Msg.UNINSTALL_TASK_JOB, getTicks());
			BundleTransitionListener.addBundleTransitionListener(this);
			uninstall(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			addError(e, msg);
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
	
	/**
	 * Stops, uninstalls and refreshes a set of pending bundle projects and all bundle projects
	 * requiring capabilities from the pending bundle projects.
	 * 
	 * @param monitor the progress monitor to use for reporting progress and job cancellation.
	 * @return status object describing the result of uninstalling with {@code StatusCode.OK} if no
	 * failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 * of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InterruptedException if the deactivate process is interrupted internally or from an
	 * external source. Deactivate is also interrupted if a task running the stop method is terminated
	 * abnormally
	 * @throws OperationCanceledException after stop and uninstall
	 * @throws BackingStoreException Failure to access the preference store for bundle states
	 * @throws IllegalStateException if the current backing store node (or an ancestor) has been
	 * removed when accessing bundle state information
	 */
	private IBundleStatus uninstall(IProgressMonitor monitor) throws InterruptedException,
			OperationCanceledException, BackingStoreException, IllegalStateException {

		Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
		if (pendingBundles.size() == 0) {
			return getLastErrorStatus();
		}
		Collection<Bundle> bundlesToUninstall = null;
		if (includeRequiring) {
			BundleSorter bs = new BundleSorter();
			bs.setAllowCycles(Boolean.TRUE);
			bundlesToUninstall = bs.sortDeclaredRequiringBundles(pendingBundles,
					bundleRegion.getBundles());
		} else {
			bundlesToUninstall = pendingBundles;
		}
		boolean isUninstallWorkspace = pendingBundles.containsAll(bundleRegion.getActivatedBundles());
		if (isUninstallWorkspace) {
			// StatePersistParticipant.saveSessionState(false);
			StatePersistParticipant.saveActivationLevel(StatePersistParticipant.getSessionPreferences(), false);
		}
		stop(bundlesToUninstall, null, new SubProgressMonitor(monitor, 1));
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		uninstall(bundlesToUninstall, new SubProgressMonitor(monitor, 1), true, unregisterBundleProject);
		if (isUninstallWorkspace && !unregisterBundleProject) {
			StatePersistParticipant.saveTransitionState(StatePersistParticipant.getSessionPreferences(), true);
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		return getLastErrorStatus();
	}

	/**
	 * Number of ticks used by this job.
	 * 
	 * @return number of ticks used by progress monitors
	 */
	public static int getTicks() {
		return 3; // Stop, Uninstall and Refresh
	}

}
