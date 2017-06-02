package no.javatime.inplace.builder;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEvent;
import no.javatime.inplace.bundlejobs.events.intface.BundleExecutorEventListener;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Listen to bundle executors being added before they are scheduled. If this is the only bundle job
 * executor event listener registered, schedule any bundle executor event
 * <p>
 * The save workspace snapshot option for the bundle executors are checked, and if on, a workspace
 * snapshot is saved before passing the bundle executors on.
 */
public class BundleExecutorInterceptor implements BundleExecutorEventListener {

	private SaveSnapShotOption saveSnapshot;

	
	/**
	 * 
	 * @return An instance of the save workspace snapshot option
	 */
	public SaveSnapShotOption getSaveSnapshot() {
		
		if (null == saveSnapshot) {
			saveSnapshot = new SaveSnapShotOption();
		}
		return saveSnapshot;
	}


	/**
	 * If this is the only bundle job executor event listener schedule any job event
	 * <p>
	 * Save workspace snapshots. The save workspace snapshot option is accessed from the specified
	 * bundle executors
	 */
	@Override
	public void bundleJobEvent(BundleExecutorEvent event) {

		try {
			BundleExecutor bundleExecutor = event.getBundlExecutor();
			// Save a snapshot before running this executor
			if (bundleExecutor.isSaveWorkspaceSnaphot()) {
				getSaveSnapshot().saveWorkspace(new NullProgressMonitor());
			}
			// If no other registered listeners, schedule the job
			if (Activator.getBundleExecutorEventService().listeners() == 1) {
				Job jobExecutor = event.getJob();
				jobExecutor.schedule(event.getDelay());
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}
}
