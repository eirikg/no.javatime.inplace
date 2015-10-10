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
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventManager;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class ResetJob extends BundleJob implements Reset {

	/**
	 * Default constructor with a default job name
	 */
	public ResetJob() {
		super(Msg.RESET_JOB);
	}

	/**
	 * Construct an activate job with a given name
	 * 
	 * @param name job name
	 */
	public ResetJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and pending projects to activate
	 * 
	 * @param name job name
	 * @param projects to activate
	 */
	public ResetJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct an activate job with a given name and a pending project to activate
	 * 
	 * @param name job name
	 * @param project to activate
	 */
	public ResetJob(String name, IProject project) {
		super(name, project);
	}

	// Set the progress group to this monitor on both jobs
	final IProgressMonitor groupMonitor = Job.getJobManager().createProgressGroup();

	/**
	 * Runs the reset bundle(s) operation
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
		try {
			SaveOptions saveOptions = getSaveOptions();
			saveOptions.disableSaveFiles(true);
			super.runInWorkspace(monitor);
			if (!bundleRegion.isRegionActivated()) {
				return getJobSatus();
			}
			BundleTransitionListener.addBundleTransitionListener(this);
			groupMonitor.beginTask(Msg.RESET_JOB, 3);
			Collection<IProject> uninstallClosures = getClosures(getPendingProjects());
			resetPendingProjects(uninstallClosures);
			if (uninstallClosures.size() == 0) {
				addInfoMessage("Terminating with no bundles to to reset");
				return getJobSatus();
			}

			Uninstall uninstall = new UninstallJob(Msg.RESET_UNINSTALL_JOB, uninstallClosures);
			uninstall.getJob().setProgressGroup(groupMonitor, 1);
			uninstall.setAddRequiring(false);
			uninstall.setUnregister(true);
			uninstall.setSaveWorkspaceSnaphot(false);
			// Activate will add the providing closure which is already included by the calculated
			// providing and requiring closure used by uninstall
			ActivateBundle activateBundle = new ActivateBundleJob(Msg.RESET_ACTIVATE_JOB,
					uninstallClosures);
			activateBundle.setSaveWorkspaceSnaphot(false);
			activateBundle.getJob().setProgressGroup(groupMonitor, 1);
			BundleExecutorEventManager bundleExecutorService = Activator.getBundleExecutorEventService();
			bundleExecutorService.add(uninstall, 0);
			bundleExecutorService.add(activateBundle, 0);
		} catch (OperationCanceledException e) {
			addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (ExtenderException e) {
			addError(e, NLS.bind(Msg.SERVICE_EXECUTOR_EXP, getName()));
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
		} finally {
			monitor.done();
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();
	}

	private Collection<IProject> getClosures(Collection<IProject> pendingProjects) {

		Collection<IProject> initialProjects = new LinkedHashSet<>(pendingProjects /* -getPendingProjects() */);

		BundleClosures closures = new BundleClosures();
		// Ignore uninstalled bundles
		Collection<Bundle> uninstalledBundles = bundleRegion.getBundles(
				bundleRegion.getBundles(getPendingProjects()), Bundle.UNINSTALLED);
		if (uninstalledBundles.size() > 0) {
			initialProjects.removeAll(uninstalledBundles);
			// -removePendingProjects(bundleRegion.getProjects(uninstalledBundles));
		}
		Collection<IProject> errorStatusProjects = new LinkedHashSet<>();
		// Remove installed bundles with errors. No dependencies here
		Collection<Bundle> installedBundles = bundleRegion.getBundles(
				bundleRegion.getBundles(getPendingProjects()), Bundle.INSTALLED);
		if (installedBundles.size() > 0) {
			for (Bundle bundle : installedBundles) {
				if (BundleProjectBuildError.hasErrors(bundleRegion.getProject(bundle))) {
					IProject errorProject = bundleRegion.getProject(bundle);
					initialProjects.remove(errorProject);
					// -removePendingProject(errorProject);
					errorStatusProjects.add(errorProject);
				}
			}
		}
		// Get closures of projects to uninstall and to activate
		// Use providing and requiring closure instead of requiring closure plus providing closure
		// to include bundles required by refresh (after uninstall) and resolve (after install)
		Collection<IProject> uninstallClosures = closures.projectDeactivation(
				Closure.PROVIDING_AND_REQUIRING, initialProjects, true /* -getPendingProjects() */);
		BundleBuildErrorClosure be = new BundleBuildErrorClosure(uninstallClosures,
				Transition.UNINSTALL, Closure.PROVIDING, Bundle.RESOLVED, ActivationScope.ACTIVATED);
		if (be.hasBuildErrors()) {
			// Remove legal closures that have overlapping bundles with error closures
			Collection<IProject> errorProjects = be.getBuildErrors();
			Collection<IProject> errorClosures = null;
			Collection<IProject> resolvedProjects = new LinkedHashSet<>(initialProjects /* -getPendingProjects() */);
			resolvedProjects.removeAll(bundleRegion.getProjects(installedBundles));
			do {
				errorClosures = closures.projectDeactivation(Closure.PROVIDING_AND_REQUIRING,
						errorProjects, true);
				errorStatusProjects.addAll(errorClosures);
				uninstallClosures = closures.projectDeactivation(Closure.PROVIDING_AND_REQUIRING,
						resolvedProjects, true);
				// Are there bundles who are members in both error and legal closures
				errorClosures.retainAll(uninstallClosures);
				if (errorClosures.size() > 0) {
					uninstallClosures = closures.projectDeactivation(Closure.PROVIDING_AND_REQUIRING,
							errorClosures, true);
					resolvedProjects.removeAll(uninstallClosures);
					errorStatusProjects.addAll(uninstallClosures);
				}
			} while (errorClosures.size() > 0);
		}	
		Collection<IProject> externalDuplicates = getExternalDuplicateClosures(uninstallClosures, null);
		if (null != externalDuplicates) {
			// TODO
		}
		Collection<IProject> wsDuplicates = removeWorkspaceDuplicates(uninstallClosures, null, null,
				bundleProjectCandidates.getInstallable(), Msg.DUPLICATE_WS_BUNDLE_INSTALL_ERROR);
		if (null != externalDuplicates || null != wsDuplicates) {
			// TODO
		}
		
		if (errorStatusProjects.size() > 0) {
			IBundleStatus errorStatus = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, "Error bundle projects and their closures not beeing reset:");
			StatusCode statusCode = StatusCode.OK;
			for (IProject errProject : errorStatusProjects)  {
				statusCode = BundleProjectBuildError.hasErrors(errProject) ? StatusCode.ERROR : StatusCode.OK; 
				errorStatus.add(new BundleStatus(statusCode, Activator.PLUGIN_ID, errProject, errProject.getName(),null));
			}
			addLogStatus(errorStatus);
		}
		installedBundles = bundleRegion.getBundles(bundleRegion.getBundles(initialProjects),
				Bundle.INSTALLED);
		uninstallClosures.addAll(bundleRegion.getProjects(installedBundles));
		return uninstallClosures;
	}
}
