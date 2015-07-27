package no.javatime.inplace;

import no.javatime.inplace.builder.AutoBuildListener;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * In an activated workspace install all projects and set activated projects to the same state as
 * they had at the last shutdown. If the workspace is deactivated set deactivated projects to
 * {@code Transition#UNINSTALL}. If a project has never been activated the default state for the
 * transition will be {@code Transition#NOTRANSITION}
 * <p>
 * The {@code earlyStartup()} method is called after initializing the plug-in in the
 * {@code start(BundleContext)} method. Initializations that depends on the workbench should be done
 * here and not when the plug-in is initialized
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

		final Activator activator = Activator.getInstance();
		activator.addResourceListeners();
		activator.processLastSavedState(true);
		try {
			StartUpJob startUp = new StartUpJob(StartUpJob.startupName);
			startUp.schedule();
			final IWorkbench workbench = PlatformUI.getWorkbench();
			if (null != workbench && !workbench.isStarting()) {
				// Not strictly necessary to run an UI thread
				Activator.getDisplay().asyncExec(new Runnable() {
					public void run() {
						// Adding at this point should ensure that all static contexts are loaded
						activator.addDynamicExtensions();
					}
				});

				// Listen to toggling of auto build
				ICommandService service = (ICommandService) workbench.getService(ICommandService.class);
				if (null == service) {
					Activator.log(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
							Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
					return;
				}
				service.addExecutionListener(new AutoBuildListener());
			} else {
				Activator.log(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
						Msg.DYNAMIC_MONITORING_WARN));
				Activator.log(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID,
						Msg.AUTO_BUILD_LISTENER_NOT_ADDED_WARN));
			}
		} catch (BundleLogException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}
}
