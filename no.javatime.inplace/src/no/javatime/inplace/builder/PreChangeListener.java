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
package no.javatime.inplace.builder;

import java.util.Collection;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.region.closure.BundleDependencies;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Listen to projects being closed, deleted and renamed. Schedules a bundle deactivate or uninstall
 * job for projects being closed and deleted and an uninstall job for projects being renamed.
 * <p>
 * When a project is closed or deleted a deactivate job is scheduled if the project has activated
 * requiring projects and an uninstall job if not.
 * <p>
 * When a project is renamed an uninstall job is scheduled causing all requiring bundles to be
 * uninstalled as well.
 * <p>
 * This is the central and only place projects are removed (closed or deleted) from the workspace.
 * Removed projects are always unregistered (removed) from the bundle project workspace region.
 */
public class PreChangeListener implements IResourceChangeListener {

	/**
	 * Handle pre resource events types on projects.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		if (!BundleProjectState.isWorkspaceNatureEnabled()) {
			return;
		}
		final IResource resource = event.getResource();
		if (resource.isAccessible() && (resource.getType() & (IResource.PROJECT)) != 0) {
			final IProject project = resource.getProject();
			try {
				BundleRegion bundleRegion = BundleManager.getRegion();
				Bundle bundle = bundleRegion.get(project);
				// Get any requiring bundles, activated and deactivated
				Collection<Bundle> bundles = BundleDependencies.getRequiringBundles(bundle,
						bundleRegion.getBundles());
				boolean rename = BundleManager.getTransition().containsPending(bundle, Transition.RENAME,
						true);
				// Uninstall the bundle to be closed or deleted when there are no requiring bundles
				// and uninstall the bundle to rename when the rename transition is pending
				// The renamed bundle is installed again with the new name after it has been built in
				// the post build listener
				if (bundles.size() == 0 || rename) {
					// Uninstall projects that are going to be deleted, closed or renamed
					UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName);
					// If this is the last activated project, deactivate workspace when project is
					// being closed or deleted by uninstalling all projects
					if (bundleRegion.isActivated(project) && bundleRegion.getBundleProjects(true).size() == 1
							&& !rename) {
						uninstallJob.addPendingProjects(ProjectProperties.getPlugInProjects());
					} else {
						// A special case is that an activated bundle may be in state uninstall when the
						// activated duplicate project has been imported or opened
						if (null == BundleJobManager.getRegion().get(project)) {
							return;
						}
						uninstallJob.addPendingProject(project);
					}
					BundleJobManager.addBundleJob(uninstallJob, 0);

				} else {
					// First deactivate requiring bundles
					if (bundleRegion.isActivated(bundle)) {
						DeactivateJob daj = new DeactivateJob(DeactivateJob.deactivateJobName, project);
						BundleJobManager.addBundleJob(daj, 0);
					}
					// Second uninstall the bundle to be deleted or closed
					WorkspaceJob uninstall = new BundleJob(UninstallJob.uninstallJobName, project) {
						@Override
						public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
							try {
								BundleManager.addBundleTransitionListener(this);
								Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
								// May happen if the closed or deleted project was uninstalled in
								// the deactivate job (the last activated project)
								if (pendingBundles.size() == 0) {
									try {
										return super.runInWorkspace(monitor);
									} catch (CoreException e) {
										String msg = ErrorMessage.getInstance()
												.formatString("error_end_job", getName());
										return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
									} finally {
										BundleManager.removeBundleTransitionListener(this);
									}
								}
								stop(pendingBundles, null, new SubProgressMonitor(monitor, 1));
								uninstall(pendingBundles, new SubProgressMonitor(monitor, 1), true);
								return super.runInWorkspace(monitor);
							} catch (InterruptedException e) {
								String msg = ExceptionMessage.getInstance()
										.formatString("interrupt_job", getName());
								addError(e, msg);
							} catch (CircularReferenceException e) {
								String msg = ExceptionMessage.getInstance().formatString("circular_reference",
										getName());
								BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION,
										InPlace.PLUGIN_ID, msg);
								multiStatus.add(e.getStatusList());
								addStatus(multiStatus);
							} catch (InPlaceException e) {
								String msg = ExceptionMessage.getInstance().formatString(
										"terminate_job_with_errors", getName());
								addError(e, msg);
							} catch (CoreException e) {
								String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
								return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
							} finally {
								BundleManager.removeBundleTransitionListener(this);
							}

							try {
								return super.runInWorkspace(monitor);
							} catch (CoreException e) {
								String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
								return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
							} finally {
								BundleManager.removeBundleTransitionListener(this);
							}
						}
					};
					BundleJobManager.addBundleJob(uninstall, 0);
				}
			} catch (InPlaceException e) {
				String hint = ExceptionMessage.getInstance().formatString("project_uninstall_error",
						project.getName());
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, hint, e), StatusManager.LOG);
			}
		}
	}
}
