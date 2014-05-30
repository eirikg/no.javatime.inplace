package no.javatime.inplace;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;

import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.ProjectLocationException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.extender.status.BundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.UserMessage;

/**
 * In an activated workspace install all projects and set activated projects to the same state as they had at the last shutdown.
 * If the workspace is deactivated set deactivated projects to {@code Transition#UNINSTALL}. If a project has
 * never been activated the default state for the transition will be {@code Transition#NOTRANSITION}
 *
 */
public class StartUp implements IStartup {

	/**
	 * Restore bundle projects to the same state as they had at shutdown. 
	 */
	@Override
	public void earlyStartup() {
		if (Category.getState(Category.infoMessages)) {
			String osgiDev = BundleProject.inDevelopmentMode();
			if (null != osgiDev) {
				UserMessage.getInstance().getString("class_path_dev_parameter", osgiDev);						
			}
		}
		try {
			Collection<IProject> activatedProjects = ProjectProperties.getActivatedProjects();
			if (activatedProjects.size() > 0) {
				// Restore bundles to state from previous session
				bundleStartUpActivation(activatedProjects);
			} else {
				setTransitionStates();
			}
		} finally {
			// Add resource listeners as as early as possible after scheduling bundle start up activation
			// This prevent other bundle jobs from being scheduled before the start up activation job
			InPlace.get().addResourceListeners();
			InPlace.get().processLastSavedState(true);
		}
	}
	
	/**
	 * Install all projects and set activated projects to the same state as they had at shutdown
	 * @param activatedProjects activated bundle projects to same state as in previous session
	 */
	private void bundleStartUpActivation(Collection<IProject> activatedProjects) {
		ActivateBundleJob activateJob = new ActivateBundleJob(ActivateBundleJob.activateStartupJobName,
				ProjectProperties.getActivatedProjects());
		activateJob.setUseStoredState(true);
		BundleManager.addBundleJob(activateJob, 0);
	}

	/**
	 * Set the transition for all deactivated projects to {@code Transition#UNINSTALL}. If a project has
	 * never been activated the default state for the transition will be {@code Transition#NOTRANSITION} 
	 */
	private void setTransitionStates() {
		IEclipsePreferences store = InPlace.getEclipsePreferenceStore();
		if (null != store) {
			BundleTransition  bundleTransition = BundleManager.getTransition();
			IBundleStatus status = null;
			for (IProject project : ProjectProperties.getProjects()) {
				if (!ProjectProperties.isProjectActivated(project)) {
					String symbolicKey = BundleManager.getRegion().getSymbolicKey(null, project);
					int state = store.getInt(symbolicKey, Transition.INSTALL.ordinal());
					if (state == Transition.UNINSTALL.ordinal()) {
						try {
							bundleTransition.initTransition(project);
						} catch (ProjectLocationException e) {
							if (null == status) {
								String msg = ExceptionMessage.getInstance().formatString("project_init_location");
								status = new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, null);
							}
							status.add(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, project, project.getName(), e));
						}
					}
				}
			}
			if (null != status) {
				StatusManager.getManager().handle(status, StatusManager.LOG);
			}
		}
	}
}
