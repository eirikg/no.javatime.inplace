/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace;

import java.util.Collection;

import no.javatime.inplace.bundlejobs.UninstallJob;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.osgi.service.prefs.BackingStoreException;

/**
 * In an activated workspace activate all bundle projects where the activation level (bundle state)
 * is the same as at the last shutdown. Transition states are calculated for both activated and
 * deactivated bundle projects. Any pending transitions from the previous session are added to both
 * deactivated and activated bundle projects
 * <p>
 * In a deactivated workspace the activation level for all bundle projects are
 * {@code Bundle.UNINSTALLED}. The transition state is set to the same as at shutdown and any
 * pending transition from the previous session are added. After first installation of the InPlace
 * Activator the activation level is {@code Bundle.UNINSTALLED} and the transition state is
 * {@code Transition.NO_TRANSITION}
 * <p>
 * If activated bundle projects had build errors at shut down or the "deactivate on exit" preference
 * option was on at shutdown (or manually changed to on after shutdown), the workspace will be
 * deactivated and the activation level, transition state and any pending transitions will be the
 * same as in a deactivated workspace.
 */
class ShutDownJob extends UninstallJob {

	/**
	 * Construct a startup job with a given name
	 * 
	 * @param name job name
	 */
	public ShutDownJob(String name) {
		super(name);
	}

	/**
	 * Construct a job with a given name and projects to start
	 * 
	 * @param name job name
	 * @param projects projects to toggle
	 */
	public ShutDownJob(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Construct a startup job with a given name and a bundle project to toggle
	 * 
	 * @param name job name
	 * @param project bundle project to toggle
	 */
	public ShutDownJob(String name, IProject project) {
		super(name, project);
	}

	/**
	 * Runs the bundle project(s) shutdown operation
	 * 
	 * @return a {@code BundleStatus} object with {@code BundleStatusCode.OK} if the job terminated
	 * normally or {@code BundleStatusCode.JOBINFO} if any status objects have been added to the job
	 * status list.
	 */
	@Override
	public IBundleStatus runInWorkspace(IProgressMonitor monitor) {

		try {
			bundleRegion = Activator.getBundleRegionService();
			if (bundleRegion.isRegionActivated()) {
				super.runInWorkspace(monitor);					
				if (getErrorStatusList().size() > 0) {
					final IBundleStatus multiStatus = createMultiStatus(new BundleStatus(
							StatusCode.ERROR, Activator.PLUGIN_ID, getName()));
					// Send output to standard console when shutting down
					try {
						Activator.getBundleConsoleService().setSystemOutToIDEDefault();
					} catch (ExtenderException | NullPointerException e) {
						// Ignore and send to current setting
					}
					System.err.println(Msg.BEGIN_SHUTDOWN_ERROR);
					printStatus(multiStatus);
					System.err.println(Msg.END_SHUTDOWN_ERROR);
				}
				// Not allowed to deactivate (modify project meta files) workspace at shutdown
				// Save transition state after uninstall if workspace is going to be deactivated at startup
				if (SessionJobsInitiator.isDeactivateOnExit(bundleRegion.getActivatedProjects())) {
					// Need to restore transition state after the workspace is deactivated at startup
					StatePersistParticipant.saveTransitionState(StatePersistParticipant.getSessionPreferences(), true);
				} 
				for (IProject project : getPendingProjects()) {
					bundleRegion.unregisterBundleProject(project);
				}
			} else {
				StatePersistParticipant.saveSessionState(false);
			}
			// Indicate a normal shut down
			StatePersistParticipant.setWorkspaceSession(false);
		} catch (IllegalStateException e) {
			String msg = WarnMessage.getInstance().formatString("node_removed_preference_store");
			addError(e, msg);
		} catch (BackingStoreException e) {
			String msg = WarnMessage.getInstance().formatString("failed_getting_preference_store");
			addError(e, msg);
		} catch (InPlaceException | ExtenderException e) {
			addError(e, Msg.INIT_BUNDLE_STATE_ERROR);
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		}
		return getJobSatus();
	}

	/**
	 * Print status and sub status objects to system err
	 * 
	 * @param status status object to print to system err
	 */
	private void printStatus(IStatus status) {
		Throwable t = status.getException();
		if (null != t) {
			t.printStackTrace();
		} else {
			System.err.println(status.getMessage());
		}
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			printStatus(children[i]);
		}
	}
}
