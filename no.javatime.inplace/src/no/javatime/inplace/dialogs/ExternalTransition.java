package no.javatime.inplace.dialogs;

import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dependencies.ProjectSorter;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

public class ExternalTransition implements BundleTransitionEventListener{

	@Override
	public void bundleTransitionChanged(BundleTransitionEvent evt) {
		if (evt.getTransition() == Transition.EXTERNAL) {
			Bundle bundle = evt.getBundle();
			IProject project = evt.getProject();
			if (BundleJobManager.getTransition().getError(bundle) == TransitionError.UNINSTALL) {  
				externalUninstall(bundle, project);
			} else {
				if (InPlace.get().msgOpt().isBundleOperations()) {
					String msg = NLS.bind(Msg.EXTERNAL_BUNDLE_OPERATION_TRACE, new Object[] 
							{bundle.getSymbolicName(), bundle.getLocation()});
					IBundleStatus status = new BundleStatus(StatusCode.INFO, bundle, project, msg, null);
					InPlace.get().trace(status);
				}
				
			}
		}
	}
	
	/**
	 * Bundle has been uninstalled from an external source in an activated workspace. Either restore (activate
	 * or install) the bundle or deactivate the workspace depending on default actions or user option/choice.
	 * 
	 * @param project the project to restore or deactivate
	 * @param bundle the bundle to restore or deactivate
	 */
	private void externalUninstall(final Bundle bundle, final IProject project) {

		final String symbolicName = bundle.getSymbolicName();
		final String location = bundle.getLocation();
		final BundleCommand bundleCommand = BundleJobManager.getCommand(); 
		// After the fact
		InPlace.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				final BundleRegion bundleRegion = BundleJobManager.getRegion(); 
				IBundleStatus reqStatus = null;
				int autoDependencyAction = 1; // Default auto dependency action
				new OpenProjectHandler().saveModifiedFiles();
				Boolean dependencies = false;
				Collection<IProject> reqProjects = Collections.<IProject>emptySet();
				if (bundleRegion.isActivated(bundle)) {
					ProjectSorter bs = new ProjectSorter();
					reqProjects = bs.sortRequiringProjects(Collections.<IProject>singletonList(project), Boolean.TRUE);
					// Remove initial project from result set
					reqProjects.remove(project);
					dependencies = reqProjects.size() > 0;
					if (dependencies) {
						String msg = NLS.bind(Msg.REQUIRING_BUNDLES_WARN, 
								new Object[] {ProjectProperties.formatProjectList(reqProjects), symbolicName});
						reqStatus = new BundleStatus(StatusCode.WARNING, symbolicName, msg);
					}
				}
				// User choice to deactivate workspace or restore uninstalled bundle
				try {
					if (!InPlace.get().getCommandOptionsService().isAutoHandleExternalCommands()) {
						String question = null;
						int index = 0;
						if (dependencies) {
							question = NLS.bind(Msg.DEACTIVATE_QUESTION_REQ_DLG, new Object[] {symbolicName, 
									location, ProjectProperties.formatProjectList(reqProjects)});
							index = 1;
						} else {
							question = NLS.bind(Msg.DEACTIVATE_QUESTION_DLG, new Object[] {symbolicName, location});
						}
						MessageDialog dialog = new MessageDialog(null, Msg.EXTERNAL_UNINSTALL_WARN, null, question,
								MessageDialog.QUESTION, new String[] { "Yes", "No" }, index);
						autoDependencyAction = dialog.open();
					}
				} catch (InPlaceException e) {
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, e.getMessage(), e),
							StatusManager.LOG);			
				}
				bundleCommand.unregisterBundle(bundle);
				BundleJob bundleJob = null;
				// Reactivate uninstalled bundle
				if (autoDependencyAction == 0) {
					if (ProjectProperties.isProjectActivated(project)) {
						bundleJob = new ActivateBundleJob(ActivateBundleJob.activateJobName, project);
						if (dependencies) {
							// Bring workspace back to a consistent state before restoring
							UninstallJob uninstallJob = new UninstallJob(UninstallJob.uninstallJobName, reqProjects);
							BundleJobManager.addBundleJob(uninstallJob, 0);
							bundleJob.addPendingProjects(reqProjects);
						}
					} else {
						if (!ProjectProperties.isProjectWorkspaceActivated()) {
							// External uninstall may have been issued on multiple bundles (uninstall A B)
							bundleJob = new ActivateProjectJob(ActivateProjectJob.activateProjectsJobName, project);
						} else {
							// Workspace is activated but bundle is not. Install the bundle and other uninstalled bundles
							bundleJob = new InstallJob(InstallJob.installJobName, ProjectProperties.getInstallableProjects());
						}
					}
					// Deactivate workspace
				} else if (autoDependencyAction == 1) {
					// Deactivate workspace to obtain a consistent state between all workspace bundles
					if (ProjectProperties.isProjectWorkspaceActivated()) {
						bundleJob = new DeactivateJob(DeactivateJob.deactivateWorkspaceJobName);
						bundleJob.addPendingProjects(ProjectProperties.getActivatedProjects());
					}
				}
				if (null != bundleJob) {
					IBundleStatus mStatus = new BundleStatus(StatusCode.WARNING, symbolicName, 
							Msg.EXTERNAL_UNINSTALL_WARN);
					String msg = NLS.bind(Msg.EXTERNAL_BUNDLE_OPERATION_TRACE, new Object[] {symbolicName, location});
					IBundleStatus status = new BundleStatus(StatusCode.WARNING, bundle, project, msg, null);
					mStatus.add(status);
					if (null != reqStatus) {
						mStatus.add(reqStatus);
					}
					if (InPlace.get().msgOpt().isBundleOperations()) {
						bundleJob.addTrace(mStatus);
					}
					StatusManager.getManager().handle(mStatus, StatusManager.LOG);
					BundleJobManager.addBundleJob(bundleJob, 0);
				}
			}
		});
	}
}
