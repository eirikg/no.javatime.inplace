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
import no.javatime.inplace.builder.JavaTimeNature;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

/**
 * Enabling and disabling the JavaTime nature of projects. Checks and reports on duplicate bundles.
 */
public abstract class NatureJob extends BundleJob {

	/** Used to name the set of operations needed to enable the nature on a project */
	private static String enableNatureSubTaskName = Message.getInstance().formatString(
			"enable_nature_subtask_name");
	/** Used to name the set of operations needed to disable the nature of a project */
	private static String disableNatureSubTaskName = Message.getInstance().formatString(
			"disable_nature_subtask_name");

	/** Task name for the build operation */
	final protected static String buildTaskName = Message.getInstance().formatString("build_task_name");


	/**
	 * Construct a nature based job with a given job name
	 * 
	 * @param name the name of the job to run
	 */
	public NatureJob(String name) {
		super(name);
	}

	/**
	 * Constructs a nature based job with a name and projects to perform bundle operations on
	 * 
	 * @param name the name of the job to run
	 * @param projects pending projects to perform bundle operations on
	 */
	public NatureJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs a job with a name and a project to perform bundle operations on
	 * 
	 * @param name the name of the job to run
	 * @param project pending project to perform bundle operations on
	 */
	public NatureJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle(s) nature operation.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		return super.runInWorkspace(monitor);
	}

	/**
	 * Deactivates the specified projects by removing the JavaTime nature from the projects. If update
	 * Bundle-ClassPat on Activate/Deactivate is switched on, remove the default output folder from the header.
	 * 
	 * @param projectsToDeactivate are the projects to deactivate by removing the JavaTime nature from the
	 *          projects
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return status object describing the result of deactivating nature with {@code StatusCode.OK} if no
	 *         failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 *         of the last failed bundle is returned. All failures are added to the job status list
	 * @throws InPlaceException failed to remove nature or the default output folder
	 */
	protected IBundleStatus deactivateNature(Collection<IProject> projectsToDeactivate,
			SubProgressMonitor monitor) throws InPlaceException{

		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToDeactivate.size());

		for (IProject project : projectsToDeactivate) {
			try {
				if (Category.getState(Category.progressBar))
					sleep(sleepTime);
				localMonitor.subTask(NatureJob.disableNatureSubTaskName + project.getName());
				if (ProjectProperties.isProjectActivated(project)) {
					toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
					bundleTransition.clearTransitionError(project);
					if (getOptionsService().isUpdateDefaultOutPutFolder()) {
						BundleProject.removeOutputLocationFromClassPath(project);
					}
				}
			} catch (InPlaceException e) {
				addError(e, e.getLocalizedMessage(), project);
				throw e;
			} finally {
				localMonitor.worked(1);
			}
		}
		return getLastStatus();
	}

	/**
	 * Activates the specified projects by adding the JavaTime nature to the projects. Updates output folder and
	 * reinstalls bundles with lazy activation policy when the Set Activation Policy to Eager on activate option
	 * is set in an activated workspace.
	 * 
	 * 
	 * @param projectsToActivate are the projects to activate by assigning the JavaTime nature to them
	 * @param monitor the progress monitor to use for reporting progress.
	 * @return status object describing the result of activating nature with {@code StatusCode.OK} if no
	 *         failure, otherwise one of the failure codes are returned. If more than one bundle fails, status
	 *         of the last failed bundle is returned. All failures are added to the job status list
	 */
	protected IBundleStatus activateNature(Collection<IProject> projectsToActivate, SubProgressMonitor monitor) {

		IBundleStatus result = new BundleStatus(StatusCode.OK, InPlace.PLUGIN_ID, "");
		SubMonitor localMonitor = SubMonitor.convert(monitor, projectsToActivate.size());
		boolean isAutoBuilding = ProjectProperties.isAutoBuilding();

		for (IProject project : projectsToActivate) {
			try {
				localMonitor.subTask(NatureJob.enableNatureSubTaskName + project.getName());
				if (ProjectProperties.isCandidateProject(project) && !ProjectProperties.isProjectActivated(project)) {
					// Set the JavaTime nature
					toggleNatureActivation(project, new SubProgressMonitor(monitor, 1));
					result = resolveBundleClasspath(project);
					Bundle bundle = bundleRegion.get(project);
					try {
						if (getOptionsService().isEagerOnActivate()) {
							Boolean isLazy = BundleProject.getLazyActivationPolicyFromManifest(project.getProject());
							if (isLazy) {
								BundleProject.toggleActivationPolicy(project);
								// Uninstall and install bundles when toggling from lazy to eager activation policy
								if (null != bundle) {
									reInstall(Collections.<IProject>singletonList(project), new SubProgressMonitor(monitor, 1));
									bundle = bundleRegion.get(project);
								}
							}
						}
					} catch (InPlaceException e) {
						result = addError(e, e.getLocalizedMessage(), project);
					}
					boolean isInstalled = null != bundle ? true : false;
					bundleCommand.registerBundleProject(project, bundle, isInstalled);
					// Adopt any external operations on bundle in an active workspace
					bundleTransition.removePending(project, Transition.EXTERNAL);
					if (isInstalled) {
						// If auto build is off or a manual build is performed tag the bundle for update
						if (!isAutoBuilding) {
							bundleTransition.addPending(project, Transition.UPDATE);
						}
						// Tag the bundle to be started after update
						bundleTransition.addPending(project, Transition.START);
					} else {
						if (!isAutoBuilding) {
						// Assure that the bundle is activated when uninstalled 
							bundleTransition.addPending(project, Transition.ACTIVATE);
						}
					}
				}
			} catch (InPlaceException e) {
				result = addError(e, e.getLocalizedMessage(), project);
			} finally {
				localMonitor.worked(1);
			}
		}
		return result;
	}

	/**
	 * Toggles JavaTime nature on a project. If the project has the JavaTime nature, the nature is removed and
	 * if the the project is not nature enabled, the JavaTime nature is added.
	 * 
	 * @param project the project to add or remove JavaTime nature on
	 * @param monitor the progress monitor to use for reporting progress.
	 * @throws InPlaceException Fails to remove or add the nature from/to the project.
	 */
	public void toggleNatureActivation(IProject project, IProgressMonitor monitor) throws InPlaceException {

		if (project.exists() && project.isOpen()) {
			try {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				SubMonitor localMonitor = SubMonitor.convert(monitor, 1);
				for (int i = 0; i < natures.length; ++i) {
					if (JavaTimeNature.JAVATIME_NATURE_ID.equals(natures[i])) {
						// Remove the nature
						localMonitor.subTask(NatureJob.disableNatureSubTaskName + project.getName());
						String[] newNatures = new String[natures.length - 1];
						System.arraycopy(natures, 0, newNatures, 0, i);
						System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
						description.setNatureIds(newNatures);
						project.setDescription(description, null);
						if (InPlace.get().msgOpt().isBundleOperations())
							InPlace.get().trace("projects_nature_disabled", project.getName());
						localMonitor.worked(1);
						return;
					}
				}
				// Add the nature
				localMonitor.subTask(NatureJob.enableNatureSubTaskName + project.getName());
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = JavaTimeNature.JAVATIME_NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				if (InPlace.get().msgOpt().isBundleOperations())
					InPlace.get().trace("projects_nature_enabled", project.getName());
				localMonitor.worked(1);
			} catch (CoreException e) {
				throw new InPlaceException(e, "error_changing_nature", project.getName());
			}
		} else {
			String msg = WarnMessage.getInstance().formatString("add_nature_project_invalid", project.getName());
			addWarning(null, msg, project);
		}
	}
	/**
	 * Builds the collection of projects
	 * 
	 * @param projects to build
	 * @param buildType incremental or full
	 * @param buildName used in progress monitor
	 * @param monitor the progress monitor to use for reporting progress to the user.
	 * @throws CoreException if build fails
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#FULL_BUILD
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#INCREMENTAL_BUILD
	 */
	protected void buildProjects(Collection<IProject> projects, int buildType, String buildName,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor localMonitor = SubMonitor.convert(monitor, buildName, projects.size());
		for (IProject project : projects) {
			try {
				if (localMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				localMonitor.subTask(buildTaskName + project.getName());
				project.build(buildType, localMonitor.newChild(1));
			} finally {
				localMonitor.worked(1);
			}
		}
	}
}
