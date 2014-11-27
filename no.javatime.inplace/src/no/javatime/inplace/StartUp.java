package no.javatime.inplace;

import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.commands.Command;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * In an activated workspace install all projects and set activated projects to the same state as
 * they had at the last shutdown. If the workspace is deactivated set deactivated projects to
 * {@code Transition#UNINSTALL}. If a project has never been activated the default state for the
 * transition will be {@code Transition#NOTRANSITION}
 * <p>
 * The {@code earlyStartup()} method is called after initializing the plug-in in the
 * {@code start(BundleContext)} method. Initializations that depends on the workbench should be done
 * her and not when the plug-in is initialized
 * 
 */
public class StartUp implements IStartup {

	/**
	 * Restore bundle projects to the same state as they had at shutdown.
	 * <p>
	 * Errors are sent to the error log
	 * <p>
	 * The auto build command service must be obtained after the workbench has started
	 */
	@Override
	public void earlyStartup() {

		final InPlace activator = InPlace.get();
		activator.addResourceListeners();
		activator.processLastSavedState(true);
		BundleJobManager.addBundleJob(new StartUpJob(StartUpJob.startupName), 0);
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (null != workbench && !workbench.isStarting()) {
			// Not strictly necessary to run an UI thread
			InPlace.getDisplay().asyncExec(new Runnable() {
				public void run() {
					// Adding at this point should ensure that all static contexts are loaded
					activator.addDynamicExtensions();
					Command autoBuildCommand = null;
					ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
					autoBuildCommand = service.getCommand("org.eclipse.ui.project.buildAutomatically");
					if (null != autoBuildCommand && autoBuildCommand.isDefined()) {
						activator.addAutobuildListener(autoBuildCommand);
					} else {
						InPlace.get().log(
								new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
										Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
					}
				}
			});			
		} else {
			if (null == InPlace.get().getAutoBuildCommand()) {
				InPlace.get().log(
						new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID,
								Msg.DYNAMIC_MONITORING_WARN));
			}
		}
	}
}
