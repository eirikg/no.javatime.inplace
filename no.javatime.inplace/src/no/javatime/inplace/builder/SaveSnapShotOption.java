package no.javatime.inplace.builder;

import no.javatime.inplace.Activator;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Saves a snapshot of the workspace
 */
public class SaveSnapShotOption {


	private BundleLog bundleLog;
	private long startTime;
	private boolean saved;
	private SaveSnapShotOption thisBundleJob; 


	/**
	 * Callback for the different stages of the save workspace operation 
	 */
	class WorkspaceSaveParticipant implements ISaveParticipant {


		@Override
		public void prepareToSave(ISaveContext context) throws CoreException {

			startTime = System.currentTimeMillis();
		}

		@Override
		public void rollback(ISaveContext context) {
			try {
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (null != workbench && !workbench.isClosing() && context.getKind() == ISaveContext.SNAPSHOT) {
					bundleLog.add(StatusCode.WARNING, Activator.getContext().getBundle(), null, Msg.SAVE_WORKSPACE_SNAPSHOT_ERROR);
				}
			} finally {								
				synchronized (thisBundleJob) {
					thisBundleJob.saved = true;
					thisBundleJob.notifyAll();
				}
			}
		}

		@Override
		public void saving(ISaveContext context) throws CoreException {				
		}	

		@Override
		public void doneSaving(ISaveContext context) {

			try {
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (null != workbench && !workbench.isClosing() && context.getKind() == ISaveContext.SNAPSHOT) {
					MessageOptions messageOptions = Activator.getMessageOptionsService();
					if (messageOptions.isBundleOperations()) {
						String msg = NLS.bind(Msg.SAVE_WORKSPACE_SNAPSHOT_INFO,
								(System.currentTimeMillis() - startTime));
						bundleLog.addRoot(StatusCode.INFO, Activator.getContext().getBundle(), null, msg);
						bundleLog.log();
					}
				}
			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			} finally {								
				synchronized (thisBundleJob) {
					thisBundleJob.saved = true;
					thisBundleJob.notifyAll();
				}
			}
		}
	}

	public boolean isSaveSnapshot() throws ExtenderException {
		
		CommandOptions commandOptions = Activator.getCommandOptionsService();
		return commandOptions.isSaveSnapshotBeforeBundleOperation(); 
	}
	/**
	 * Save a snapshot of the workspace
	 * 
	 * @param monitor Shows progress of the save workspace snapshot operation
	 */
	public void saveWorkspace(IProgressMonitor monitor)  {

		ISaveParticipant saveParticipant = new WorkspaceSaveParticipant();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			bundleLog = Activator.getBundleLogService();
			workspace.addSaveParticipant(Long.toString(Activator.getContext().getBundle().getBundleId()), saveParticipant);
			thisBundleJob = this;
			synchronized (this) {
				this.saved = false;
			}
			IStatus status = workspace.save(false, monitor);
			if (!status.isOK()) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, status.getMessage(), status.getException()),
						StatusManager.LOG);
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} catch (OperationCanceledException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} catch (CoreException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		} finally {
			synchronized (this) {
				try {
					while (!this.saved) {
						this.wait();
					}
				} catch (InterruptedException e) {
					String msg = NLS.bind(Msg.INTERRUPT_EXP, "Save workspace snapshot");
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
							StatusManager.LOG);
				}
			}			
			workspace.removeSaveParticipant(Long.toString(Activator.getContext().getBundle().getBundleId()));
		}
	}
}
