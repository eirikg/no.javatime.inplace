package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.intface.AddBundleProject;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.bundlejobs.intface.ActivateBundle;
import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.BundleBuildErrorClosure;
import no.javatime.inplace.region.closure.BundleClosures;
import no.javatime.inplace.region.closure.BundleProjectBuildError;
import no.javatime.inplace.region.closure.CircularReferenceException;
import no.javatime.inplace.region.closure.ProjectBuildErrorClosure.ActivationScope;
import no.javatime.inplace.region.events.TransitionEvent;
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Bundle;

class AddBundleProjectJob extends NatureJob implements AddBundleProject {

	/**
	 * Default constructor wit a default job name
	 */
	public AddBundleProjectJob() {
		super(Msg.ADD_BUNDLE_PROJECT_JOB);
	}

	/**
	 * Constructs a add bundle job with a given job name
	 * 
	 * @param name job name
	 */
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
			// Register project if it is not going to be registered by install
			for (IProject project : getPendingProjects()) {
				bundleRegion.registerBundleProject(project, null, isProjectActivated(project));
			}
			// New (deactivated and activated) projects added to the workspace are already opened
			// New projects should be in state uninstalled in a deactivated workspace
			if (!isProjectWorkspaceActivated()) {
				return getJobSatus();
			}
			// Catch any fatal errors in new projects before install detects them
			if (BundleProjectBuildError.getBundleErrors(newProjects, true).size() > 0) {
				bundleTransition.addPendingCommand(getActivatedProjects(), Transition.DEACTIVATE);
				return addStatus(new BundleStatus(StatusCode.JOBERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));				
			}			
			try {
				// Install all new projects in an activated workspace
				install(newProjects, monitor);
			} catch (InPlaceException | WorkspaceDuplicateException | ExternalDuplicateException | ProjectLocationException e) {
				bundleTransition.addPendingCommand(getActivatedProjects(), Transition.DEACTIVATE);
				return addStatus(new BundleStatus(StatusCode.JOBERROR, Activator.PLUGIN_ID, Msg.INSTALL_ERROR));
			}
			ResourceState resourceState = Activator.getResourceStateService();
			boolean isTriggerUpdate = resourceState.isTriggerUpdate();
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
				activateProject.setSaveWorkspaceSnaphot(false);
				Activator.getBundleExecutorEventService().add(activateProject, 0);
			}
			// Deactivated providers are handled by the activate project job
			newActivatedBundleProjects.removeAll(deactivatedProviders);
			// Activate bundles for all new projects that are activated
			if (newActivatedBundleProjects.size() > 0 && !isTriggerUpdate) {
				ActivateBundle activateBundle = new ActivateBundleJob(
						Msg.ACTIVATE_BUNDLE_JOB, newActivatedBundleProjects);
					activateBundle.setSaveWorkspaceSnaphot(false);
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
	
	/**
	 * Find and return build error closures among the specified deactivated projects to activate.
	 * 
	 * @param projects projects to activate with possible build error closures
	 * @return set of providing build error closures or an empty set
	 */
	private Collection<IProject> buildErrorClosure(Collection<IProject> projects) {

		// Deactivated providing closure. In this case the activation scope is deactivated as long as we
		// are not checking activated providing or requiring bundles with build errors
		// Activated requiring closure is not checked (see method comments)
		// Note that the bundles to activate are not activated yet.
		BundleBuildErrorClosure be = new BundleBuildErrorClosure(projects, Transition.ACTIVATE_PROJECT,
				Closure.PROVIDING, Bundle.UNINSTALLED, ActivationScope.DEACTIVATED);
		if (be.hasBuildErrors(true)) {
			Collection<IProject> errorClosure = be.getBuildErrorClosures(true);
			try {
				if (messageOptions.isBundleOperations()) {
					addLogStatus(be.getErrorClosureStatus());
				}
			} catch (ExtenderException e) {
				addLogStatus(be.getErrorClosureStatus());
			}
			return errorClosure;
		}
		return Collections.<IProject> emptySet();
	}

}
 