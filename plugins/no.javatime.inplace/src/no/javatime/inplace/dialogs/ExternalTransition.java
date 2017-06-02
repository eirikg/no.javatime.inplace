package no.javatime.inplace.dialogs;

import java.util.Collection;
import java.util.Collections;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.ActivateBundleJob;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Uninstall;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.closure.ProjectSorter;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * If a bundle is uninstalled from an external source in an activated workspace the workspace is
 * automatically deactivated if automatic handling of external commands is switched on. If off, the
 * user has the option to deactivate the workspace or install, resolve and possibly start the
 * uninstalled bundle again.
 */
public class ExternalTransition implements BundleTransitionEventListener {

	@Override
	public void bundleTransitionChanged(BundleTransitionEvent evt) {
		if (evt.getTransition() == Transition.EXTERNAL) {
			Bundle bundle = evt.getBundle();
			IProject project = evt.getProject();
			BundleTransition transition = Activator.getBundleTransitionService();
			if (transition.getBuildTransitionError(bundle) == TransitionError.MODULAR_EXTERNAL_UNINSTALL) {
				transition.clearBuildTransitionError(project);
				externalUninstall(bundle, project);
			}
		}
	}

	/**
	 * Bundle has been uninstalled from an external source in an activated workspace. Either restore
	 * (activate or install) the bundle or deactivate the workspace depending on default actions or
	 * user option/choice.
	 * 
	 * @param project the project to restore or deactivate
	 * @param bundle the bundle to restore or deactivate
	 */
	private void externalUninstall(final Bundle bundle, final IProject project) {

		final String symbolicName = bundle.getSymbolicName();
		final String location = bundle.getLocation();
			// After the fact
			Activator.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						final BundleRegion bundleRegion = Activator.getBundleRegionService();
						final BundleProjectCandidates bundleProjectcandidates = Activator.getBundleProjectCandidatesService();
						IBundleStatus reqStatus = null;
						int autoDependencyAction = 1; // Default auto dependency action
						Boolean dependencies = false;
						Collection<IProject> reqProjects = Collections.<IProject> emptySet();
						if (bundleRegion.isBundleActivated(bundle)) {
							ProjectSorter bs = new ProjectSorter();
							reqProjects = bs.sortRequiringProjects(Collections.<IProject> singletonList(project),
									Boolean.TRUE);
							// Remove initial project from result set
							reqProjects.remove(project);
							dependencies = reqProjects.size() > 0;
							if (dependencies) {
								String msg = NLS.bind(Msg.REQUIRING_BUNDLES_WARN,
										new Object[] { Activator.getBundleProjectCandidatesService().formatProjectList(reqProjects), symbolicName });
								reqStatus = new BundleStatus(StatusCode.WARNING, symbolicName, msg);
							}
						}
						// User choice to deactivate workspace or restore uninstalled bundle
						if (!Activator.getCommandOptionsService().isAutoHandleExternalCommands()) {
							String question = null;
							int index = 0;
							if (dependencies) {
								question = NLS.bind(Msg.DEACTIVATE_QUESTION_REQ_DLG, new Object[] { symbolicName,
										location, bundleProjectcandidates.formatProjectList(reqProjects) });
								index = 1;
							} else {
								question = NLS.bind(Msg.DEACTIVATE_QUESTION_DLG, new Object[] { symbolicName,
										location });
							}
							MessageDialog dialog = new MessageDialog(null, Msg.EXTERNAL_UNINSTALL_WARN, null,
									question, MessageDialog.QUESTION, new String[] { "Yes", "No" }, index);
							autoDependencyAction = dialog.open();
						}
						// bundleRegion.unregisterBundle(bundle);
						BundleExecutor bundleJob = null;
						// Activate the external uninstalled bundle
						if (autoDependencyAction == 0) {
							if (bundleRegion.isBundleActivated(project)) {
								bundleJob = new ActivateBundleJob(Msg.ACTIVATE_BUNDLE_JOB, project);
								if (dependencies) {
									// Bring workspace back to a consistent state before restoring
									Uninstall uninstallJob = new UninstallJob(Msg.UNINSTALL_JOB,
											reqProjects);
									Activator.getBundleExecutorEventService().add(uninstallJob, 0);
									bundleJob.addPendingProjects(reqProjects);
								}
							} else {
								if (!bundleRegion.isRegionActivated()) {
									// External uninstall may have been issued on multiple bundles (uninstall A B)
									bundleJob = new ActivateProjectJob(Msg.ACTIVATE_PROJECT_JOB, project);
								} else {
									// Workspace is activated but bundle is not. Install the bundle and other uninstalled
									// bundles
									bundleJob = new InstallJob(Msg.INSTALL_JOB, project); 
								}
							}
							// Deactivate workspace
						} else if (autoDependencyAction == 1) {
							// Deactivate workspace to obtain a consistent state between all workspace bundles
							if (bundleRegion.isRegionActivated()) {
								bundleJob = new DeactivateJob(Msg.DEACTIVATE_WORKSPACE_JOB);
								bundleJob.addPendingProjects(bundleRegion.getActivatedProjects());
							}
						}
						if (null != bundleJob) {
							IBundleStatus mStatus = new BundleStatus(StatusCode.WARNING, symbolicName,
									Msg.EXTERNAL_UNINSTALL_WARN);
							String msg = NLS.bind(Msg.EXTERNAL_BUNDLE_OP_TRACE,
									new Object[] { symbolicName, location });
							IBundleStatus status = new BundleStatus(StatusCode.WARNING, bundle, project, msg, null);
							mStatus.add(status);
							if (null != reqStatus) {
								mStatus.add(reqStatus);
							}
							StatusManager.getManager().handle(mStatus, StatusManager.LOG);
							Activator.getBundleExecutorEventService().add(bundleJob, 0);
						}
					} catch (ExtenderException e) {
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
								StatusManager.LOG);
					}		
				}
			});
	}
}
