package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.intface.AddBundleProject;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

class AddBundleProjectJob extends NatureJob implements AddBundleProject {

	/**
	 * Default constructor wit a default job name
	 */
	public AddBundleProjectJob() {
		super(Msg.ADD_BUNDLE_PROJECT_JOB);
	}

	public AddBundleProjectJob(String name) {
		super(name);
	}

	/**
	 * Constructs an add job with a given job name and pending bundle projects to install and resolve
	 * 
	 * @param name job name
	 * @param projects pending projects to install and resolve
	 */
	public AddBundleProjectJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs an add job with a given job name and pending a bundle project to install and resolve
	 * 
	 * @param name job name
	 * @param project pending project to install and resolve
	 */
	public AddBundleProjectJob(String name, IProject project) {
		super(name, project);
	}

	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

		try {
			super.runInWorkspace(monitor);
			BundleTransitionListener.addBundleTransitionListener(this);
			Collection<IProject> newProjects = getPendingProjects();
			if (newProjects.size() == 0) {
				return getJobSatus();
			}
			// New (deactivated and activated) projects added to the workspace are already opened
			// New projects should be in state uninstalled in a deactivated workspace
			if (!isProjectWorkspaceActivated()) {
				return getJobSatus();
			}
			// Install all new projects in an activated workspace
			Install install = new InstallJob(Msg.INSTALL_JOB, newProjects);
			Activator.getBundleExecutorEventService().add(install, 0);
			// New and existing deactivated projects that provides capabilities to new and existing
			// projects
			Collection<IProject> deactivatedProviders = new LinkedHashSet<>();
			// New projects that have the JavaTime nature enabled
			Collection<IProject> newActivatedBundleProjects = new LinkedHashSet<>();

			BundleClosures closures = new BundleClosures();
			// Divide new projects in activated projects requiring capabilities from
			// deactivated projects and activated projects independent on deactivated
			// projects. Also collect all deactivated providers
			for (IProject newProject : newProjects) {
				BundleTransitionListener.addBundleTransition(new TransitionEvent(newProject,
						Transition.NEW_PROJECT));
				bundleTransition.removePending(newProject, Transition.NEW_PROJECT);
				// Deactivated projects that are not providers are already scheduled for install
				// Project is not registered (install job is in waiting state) with the workspace yet.
				if (isProjectActivated(newProject)) {
					// Get any deactivated providers to this new project
					Collection<IProject> providers = closures.projectActivation(
							Collections.<IProject> singletonList(newProject), false);
					providers.remove(newProject);
					if (providers.size() > 0) {
						deactivatedProviders.addAll(providers);
					}
					newActivatedBundleProjects.add(newProject);
				}
			}
			// Activate all deactivated provider projects
			if (deactivatedProviders.size() > 0) {
				ActivateProject activateProject = new ActivateProjectJob(
						Msg.ACTIVATE_PROJECT_JOB, deactivatedProviders);
				// Do not add requiring projects. They will be resolved as part of the requiring
				// closure when providers are resolved
				Activator.getBundleExecutorEventService().add(activateProject, 0);
			}
			// Provide information when auto build is turned off
			if (messageOptions.isBundleOperations()
					&& !bundleProjectCandidates.isAutoBuilding()) {
				IBundleStatus status = new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
						Msg.BUILDER_OFF_INFO);
				status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
						Msg.BUILDER_OFF_LIST_INFO,
						bundleProjectCandidates.formatProjectList(getPendingProjects()))));
				addLogStatus(status);
			}
			// Deactivated providers are handled by the activate project job
			newActivatedBundleProjects.removeAll(deactivatedProviders);
			// Activate bundles for all new projects that are activated
			if (newActivatedBundleProjects.size() > 0) {
				ActivateBundle activateBundle = new ActivateBundleJob(
						Msg.ACTIVATE_BUNDLE_JOB, newActivatedBundleProjects);
				Activator.getBundleExecutorEventService().add(activateBundle, 0);
			}
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (InPlaceException | ExtenderException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
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
}
 