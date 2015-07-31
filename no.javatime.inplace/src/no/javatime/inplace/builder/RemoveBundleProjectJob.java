package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.builder.intface.RemoveBundleProject;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

public class RemoveBundleProjectJob extends NatureJob implements RemoveBundleProject {

	/**
	 * Constructs a removal job with a given job name
	 * 
	 * @param name job name
	 */
	public RemoveBundleProjectJob(String name) {
		super(name);
	}

	/**
	 * Constructs a removal job with a given job name and pending bundle projects to remove
	 * 
	 * @param name job name
	 * @param projects pending projects to remove
	 */
	public RemoveBundleProjectJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs an removal job with a given job name and a pending bundle project to remove
	 * 
	 * @param name job name
	 * @param project pending project to remove
	 */
	public RemoveBundleProjectJob(String name, IProject project) {
		super(name, project);
	}

	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		Collection<Bundle> reqBundleClosure = null;
		try {
			super.runInWorkspace(monitor);
			// Collect all removed projects in one run
			addNotScheduledProjects();
			Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
			if (pendingBundles.size() == 0) {
				return getJobSatus();
			}
			BundleTransitionListener.addBundleTransitionListener(this);
			if (bundleRegion.isRegionActivated()) {
				// Deactivate all requiring projects to projects being closed or deleted
				BundleClosures closure = new BundleClosures();
				Collection<Bundle> activatedBundles = bundleRegion.getActivatedBundles();
				// Ensure requiring closure by overriding the current closure preferences
				reqBundleClosure = closure.bundleDeactivation(Closure.REQUIRING, pendingBundles,
						activatedBundles);
				// Deactivate requiring projects and uninstall closed projects in requiring order
				for (Bundle bundle : reqBundleClosure) {
					IProject project = bundleRegion.getProject(bundle);
					Collection<Bundle> singletonList = Collections.singletonList(bundle);
					if (project.isAccessible()) {
						// Deactivate bundle with requirements on removed project
						stop(singletonList, null, new SubProgressMonitor(monitor, 1));
						bundleTransition.addPending(project, Transition.UNRESOLVE);
						deactivateNature(Collections.singletonList(project), new SubProgressMonitor(monitor, 1));
						refresh(singletonList, new SubProgressMonitor(monitor, 1));
					} else {
						// Stop and uninstall removed project
						stop(singletonList, null, new SubProgressMonitor(monitor, 1));
						bundleRegion.setActivation(project, false);	
						uninstall(singletonList, new SubProgressMonitor(monitor, 1), false, false);
					}
				}
			}
			// If all remaining activated projects are either closed, deleted or deactivated
			if (!bundleRegion.isRegionActivated()) {
				Collection<Bundle> deactivatedBundles = bundleRegion.getDeactivatedBundles();
				// Uninstall & refresh
				uninstall(deactivatedBundles, new SubProgressMonitor(monitor, 1), true, false);
			} else {
				refresh(pendingBundles, new SubProgressMonitor(monitor, 1));
			}
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
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
			for (IProject removedProject : getPendingProjects()) {
				if (bundleTransition.containsPending(removedProject, Transition.CLOSE_PROJECT, false)) {
					// Do not unregister closed projects to preserve pending build transition
					BundleTransitionListener.addBundleTransition(new TransitionEvent(removedProject,
							Transition.CLOSE_PROJECT));
					bundleTransition.removePending(removedProject, Transition.CLOSE_PROJECT);
					
				} else if (bundleTransition.containsPending(removedProject, Transition.DELETE_PROJECT, false)) {
					BundleTransitionListener.addBundleTransition(new TransitionEvent(removedProject,
							Transition.DELETE_PROJECT));
					bundleTransition.removePending(removedProject, Transition.DELETE_PROJECT);
					// Unregister deleted projects from the workspace region
					bundleRegion.unregisterBundleProject(removedProject);					
				}
			}
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		return getJobSatus();
	}

	/**
	 * Add removed (closed or deleted) projects that are not added to this job.
	 * <p>
	 * If there are no removed projects in the region workspace, the existing pending projects are
	 * removed as pending. This means that the removed projects have already been handled by an
	 * earlier run of this job. The rationale is that there is a need to handle all removed projects
	 * in one job.
	 * 
	 * @return removed projects that were not included in this job at startup, but added to the job by
	 * this method. If an empty set is returned there were no removed projects in the workspace.
	 */
	private Collection<IProject> addNotScheduledProjects() {

		Collection<IProject> notScheduledProjects = new LinkedHashSet<>();
		// Pending projects included
		Collection<IProject> removedCandidates = bundleRegion.getProjects();
		for (IProject removedCandidate : removedCandidates) {
			// If the project is not accessible it is closed or deleted
			if (!removedCandidate.isAccessible()) {
				Bundle bundle = bundleRegion.getBundle(removedCandidate);
				// Uninstalled projects means that they have already been handled
				if (null != bundle && (bundle.getState() & (Bundle.UNINSTALLED)) == 0) {
					notScheduledProjects.add(removedCandidate);
				}
			}
		}
		if (notScheduledProjects.size() > 0) {
			addPendingProjects(notScheduledProjects);
		} else {
			clearPendingProjects();
		}
		return notScheduledProjects;
	}
}
