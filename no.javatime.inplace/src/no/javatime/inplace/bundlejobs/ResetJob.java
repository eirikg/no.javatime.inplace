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

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Reset first uninstalls and then activates bundles to the same state as they had before the reset job.
 * <p>
 * Resetting a pending bundle creates a set containing the bundle to reset, and all bundles that have
 * requirements on the bundle to reset and its providing bundles. The set is the closure of bundles containing
 * the pending bundle to reset and all bundles with a direct or indirect declared dependency (requiring and
 * providing) on the pending bundle.
 */
public class ResetJob {

	/** Standard name of a reset job */
	final public static String resetJobName = Message.getInstance().formatString("reset_job_name");
	/* The name of the uninstall phase of the reset job */
	final private static String uninstallResetJobName = Message.getInstance().formatString(
			"uninstall_reset_job_name");
	/* The name of the activate phase of the reset job */
	final private static String activateResetJobName = Message.getInstance().formatString(
			"activate_reset_job_name");

	private Collection<IProject> pendingProjects = new LinkedHashSet<IProject>();

	/**
	 * Default constructor
	 */
	public ResetJob() {
	}

	/**
	 * Construct a reset job with a pending bundle project to reset
	 * 
	 * @param pendingProject pending bundle project to reset
	 */
	public ResetJob(IProject pendingProject) {
		addPendingProject(pendingProject);
	}

	/**
	 * Construct a reset job with pending bundle projects to reset
	 * 
	 * @param pendingProjects pending bundle projects to reset
	 */
	public ResetJob(Collection<IProject> pendingProjects) {
		addPendingProjects(pendingProjects);
	}

	/**
	 * Pending projects added. If accessed after the job is started the pending projects may or may not contain
	 * dependency closures for the reset operation
	 * 
	 * @return the pending projects with dependency closures
	 */
	public Collection<IProject> getPendingProjects() {
		return pendingProjects;
	}

	/**
	 * Adds pending bundle projects to reset
	 * 
	 * @param projects the pending projects to reset
	 */
	public void addPendingProjects(Collection<IProject> projects) {
		this.pendingProjects.addAll(projects);
	}

	/**
	 * Adds a pending bundle project to reset
	 * 
	 * @param project the pending project to reset
	 */
	public void addPendingProject(IProject project) {
		pendingProjects.add(project);
	}
	final public static Object uninstallFamily = new Object();
	final public static Object activateFamily = new Object();

	/**
	 * Schedules a reset job for the added pending projects.
	 * 
	 * @param name of reset job
	 * @see #resetJobName
	 */
	public void reset(String name) {

		// Set the progress group to this monitor on both jobs
		final IProgressMonitor groupMonitor = Job.getJobManager().createProgressGroup();

		// Schedule uninstall and activate jobs and wait for them to finish
		BundleJob resetJob = new BundleJob(name) {

			/**
			 * Runs the bundle(s) reset operation.
			 * 
			 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} or
			 *         {@code BundleStatusCode.ERROR} if the job fails
			 */
			@Override
			public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

				// Can use the standard bundle family, if all jobs are bundle jobs
				// Use a reset dedicated family here if including a build job later
				final Object resetFamily = new Object[] { uninstallFamily, activateFamily };

				/**
				 * Uninstall job that is member of the uninstall and reset family
				 */
				class GroupUninstall extends UninstallJob {

					/**
					 * Construct a job with bundle projects to uninstall
					 * 
					 * @param name job name
					 * @param projects bundle projects to uninstall
					 */
					public GroupUninstall(String name, Collection<IProject> projects) {
						super(name, projects);
					}

					/**
					 * Only want the jobs created by this instance of the reset job to indicate they belong together,
					 * the reset job should not belong to the same family, it is grouped with it’s spawned child jobs
					 * via the progressGroup
					 * 
					 * @param family name
					 * @return true if this job belongs to the specified family
					 */
					@Override
					public boolean belongsTo(Object family) {

						if (null != family) {
							if (family.equals(uninstallFamily)) {
								return true;
							} else if (family.equals(resetFamily)) {
								return true;
							} else if (super.belongsTo(family)) {
								return true;
							}
						}
						return false;
					}

					/**
					 * Runs the bundle(s) uninstall operation.
					 * 
					 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally
					 *         and no status objects have been added to this job status list and
					 *         {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if
					 *         any status objects have been added to the job status list.
					 */
					@Override
					public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
						try {
							BuildErrorClosure be = new BuildErrorClosure(getPendingProjects(), 
									Transition.UNINSTALL, Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.ALL);
							if (be.hasBuildErrors()) {
								Collection<IProject> buildErrClosure = be.getBuildErrorClosures();
								removePendingProjects(buildErrClosure);
								IBundleStatus bundleStatus = be.getErrorClosureStatus();
								if (null != bundleStatus) {
									addStatus(bundleStatus);			
								}
								throw new OperationCanceledException();
							}
							unregisterBundleProject(true);
							// Uninstall the bundles
							super.runInWorkspace(monitor);
						} catch (OperationCanceledException e) {
							addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
							getJobManager().cancel(resetFamily);
						} catch (CircularReferenceException e) {
							String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
							addError(e, msg);
						} finally {
							monitor.done();
						}
						if (getErrorStatusList().size() > 0) {
							return new BundleStatus(StatusCode.JOBINFO, InPlace.PLUGIN_ID, null);
						}
						return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
					}
				}

				/**
				 * Activate bundle job that is member of the activate and reset family
				 */
				class GroupActivate extends ActivateBundleJob {

					/**
					 * Construct a job with projects to activate
					 * 
					 * @param name job name
					 * @param projects to activate
					 */
					public GroupActivate(String name, Collection<IProject> projects) {
						super(name, projects);
					}

					/**
					 * Only want the jobs created by this instance of the reset job to indicate they belong together,
					 * the reset job should not belong to the same family, it is grouped with it’s spawned child jobs
					 * via the progressGroup
					 * 
					 * @param family name
					 * @return true if this job belongs to the specified family
					 */
					@Override
					public boolean belongsTo(Object family) {

						if (null != family) {
							if (family.equals(activateFamily)) {
								return true;
							} else if (family.equals(resetFamily)) {
								return true;
							} else if (super.belongsTo(family)) {
								return true;
							}
						}
						return false;
					}

					/**
					 * Runs the bundle(s) activate operation.
					 * 
					 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if job terminated normally
					 *         and no status objects have been added to this job status list and
					 *         {@code BundleStatusCode.ERROR} if the job fails or {@code BundleStatusCode.JOBINFO} if
					 *         any status objects have been added to the job status list.
					 */
					@Override
					public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

						try {
							// Wait for the uninstall job to finish before activating the bundles
							getJobManager().join(uninstallFamily, null);
							BuildErrorClosure be = new BuildErrorClosure(getPendingProjects(), Transition.ACTIVATE_BUNDLE, 
									Closure.REQUIRING, Bundle.UNINSTALLED, ActivationScope.ALL);
							if (be.hasBuildErrors()) {
								Collection<IProject> buildErrClosure = be.getBuildErrorClosures();
								removePendingProjects(buildErrClosure);
								IBundleStatus bundleStatus = be.getErrorClosureStatus();
								if (null != bundleStatus) {
									addStatus(bundleStatus);			
								}
								throw new OperationCanceledException();
							}
							super.runInWorkspace(monitor);
							for (IProject project : getPendingProjects()) {
								bundleTransition.removePending(project, Transition.UPDATE);
							}
						} catch (OperationCanceledException e) {
							addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
							getJobManager().cancel(resetFamily);
						} catch (InterruptedException e) {
							String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
							addError(e, msg);
						} catch (CircularReferenceException e) {
							String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
							addError(e, msg);
						} finally {
							monitor.done();
						}
						if (getErrorStatusList().size() > 0) {
							return new BundleStatus(StatusCode.JOBINFO, InPlace.PLUGIN_ID, null);
						}
						return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, null);
					}
				}
				// The reset job
				try {
					// Use the preference store to reset to same as current state after activate
					InPlace.get().savePluginSettings(true, false);
					ProjectSorter ps = new ProjectSorter();
					// Reset require that all bundles that the project(s) to reset have requirements on are reset to
					int count = 0;
					addPendingProjects(pendingProjects);
					// TODO Consider if it is necessary to include a partial graph
					do {
						count = pendingProjects();
						resetPendingProjects(ps.sortRequiringProjects(getPendingProjects()));
						resetPendingProjects(ps.sortProvidingProjects(getPendingProjects()));
					} while (pendingProjects() > count);
					Collection<IProject> projectsToReset = getPendingProjects();
					Collection<IProject> errorProjects = null;
					if (projectsToReset.size() > 0) {
						errorProjects = removeExternalDuplicates(projectsToReset, null, null);
						if (null != errorProjects) {
							projectsToReset.removeAll(errorProjects);
							String msg = ErrorMessage.getInstance().formatString("bundle_errors_reset", bundleProjectCandidates.formatProjectList((errorProjects)));
							addError(null, msg);
						}
					}
					if (projectsToReset.size() == 0 || null != errorProjects) {
						try {
							return super.runInWorkspace(monitor);
						} catch (CoreException e) {
							String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
							return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
						}
					}
					// Save current state of bundles to be used by the activate j0b to resore the current state
					InPlace.get().savePluginSettings(true, false);
					GroupUninstall uninstallJob = new GroupUninstall(uninstallResetJobName, projectsToReset);
					uninstallJob.setProgressGroup(groupMonitor, 1);
					BundleJobManager.addBundleJob(uninstallJob, 0);
					GroupActivate activateBundleJob = new GroupActivate(ResetJob.activateResetJobName, projectsToReset);
					activateBundleJob.setProgressGroup(groupMonitor, 1);
					activateBundleJob.setUseStoredState(true);
					BundleJobManager.addBundleJob(activateBundleJob, 0);
					
				} catch (OperationCanceledException e) {
					getJobManager().cancel(resetFamily);
					return addCancelMessage(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
				} catch (CircularReferenceException e) {
					String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
					BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
					multiStatus.add(e.getStatusList());
					return multiStatus;
				} finally {
					groupMonitor.done();
				}
				return new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, (IProject) null, null, null);
			} // End reset operation
		};
		groupMonitor.beginTask(resetJobName, 3);
		resetJob.setProgressGroup(groupMonitor, 1);
		BundleJobManager.addBundleJob(resetJob, 0);
	}
}
