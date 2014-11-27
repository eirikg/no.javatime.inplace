package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

/**
 * Uninstall all activated bundle projects that have been removed (closed or deleted) from the
 * workspace. Removed bundle projects are added as pending projects to this job before scheduling
 * the job.
 * <p>
 * If there are removed projects in the workspace that are not added to this job when it starts
 * running the projects are added automatically as pending projects by this job.
 * <p>
 * When removing bundle projects with requiring bundles the requiring closure set becomes
 * incomplete. This inconsistency is solved by deactivating the requiring bundles in the closure
 * before uninstalling the removed projects.
 */
class RemoveBundleProjectJob extends NatureJob {

	final public static String removeBundleProjectName = Msg.REMOVE_BUNDLE_PROJECT_JOB;

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
			// Collect all removed projects in one run
			addNotScheduledProjects();
			Collection<Bundle> pendingBundles = bundleRegion.getBundles(getPendingProjects());
			if (pendingBundles.size() == 0) {
				return super.runInWorkspace(monitor);
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
						// Deactivate bundle with requirements on closed project
						stop(singletonList, null, new SubProgressMonitor(monitor, 1));
						bundleTransition.addPending(project, Transition.UNRESOLVE);
						deactivateNature(Collections.singletonList(project), new SubProgressMonitor(monitor, 1));
						refresh(singletonList, new SubProgressMonitor(monitor, 1));
					} else {
						// Uninstall, refresh and unregister closed project
						BundleTransitionListener.addBundleTransition(new TransitionEvent(project,
								Transition.REMOVE_PROJECT));
						bundleTransition.removePending(project, Transition.REMOVE_PROJECT);
						stop(singletonList, null, new SubProgressMonitor(monitor, 1));
						bundleRegion.setActivation(project, false);
						uninstall(singletonList, new SubProgressMonitor(monitor, 1), false, false);
					}
				}
			}
			// If all remaining activated projects are either closed or deactivated
			if (!bundleRegion.isRegionActivated()) {
				Collection<Bundle> deactivatedBundles = bundleRegion.getDeactivatedBundles();
				// Uninstall & refresh
				uninstall(deactivatedBundles, new SubProgressMonitor(monitor, 1), true, false);
			} else {
				refresh(pendingBundles, new SubProgressMonitor(monitor, 1));
			}
			return super.runInWorkspace(monitor);
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_job", getName());
			addError(e, msg);
		} catch (CircularReferenceException e) {
			String msg = ExceptionMessage.getInstance().formatString("circular_reference", getName());
			BundleStatus multiStatus = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg);
			multiStatus.add(e.getStatusList());
			addStatus(multiStatus);
		} catch (InPlaceException | ExtenderException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			// Unregister the removed projects from the workspace
			for (IProject removedProject : getPendingProjects()) {
				bundleRegion.unregisterBundleProject(removedProject);
			}
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
		try {
			BundleTransitionListener.addBundleTransitionListener(this);
			return super.runInWorkspace(monitor);
		} catch (CoreException e) {
			String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
			return new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg, e);
		} finally {
			BundleTransitionListener.removeBundleTransitionListener(this);
		}
	}

	/**
	 * Add removed projects that are not added to this job.
	 * <p>
	 * If there are no removed projects to add, the existing pending projects are removed as pending.
	 * This means that the removed projects have already been handled by an earlier run of this job.
	 * The rationale is that there is a need to handle all removed projects in one job.
	 * 
	 * @return removed projects that were not included in this job at startup, but added to the job by
	 * this method. If an empty set is returned there were no removed projects in the workspace.
	 */
	private Collection<IProject> addNotScheduledProjects() {

		Collection<IProject> notScheduledProjects = new LinkedHashSet<>();
		for (IProject removedProject : bundleRegion.getProjects()) {
			if (!removedProject.isAccessible()) {
				Bundle bundle = bundleRegion.getBundle(removedProject);
				// Uninstalled projects means that they have already been handled
				if (null != bundle && (bundle.getState() & (Bundle.UNINSTALLED)) == 0) {
					notScheduledProjects.add(removedProject);
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
