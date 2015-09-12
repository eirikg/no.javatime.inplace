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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Uninstall and save bundle project workspace state for all bundle projects when shutting down.
 * <p>
 * <p>
 * See {@link StatePersistParticipant} for a specification of saving and restoring the bundle
 * project workspace state.
 * <p>
 * If activated bundle projects have build errors at shut down or the "Deactivate on Exit"
 * preference option is on at shutdown (or manually changed to on after shutdown), the workspace
 * will be deactivated at start up
 * 
 * @see StartUpJob
 * @see StatePersistParticipant
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
			// Signal that we are shutting down
			StatePersistParticipant.setWorkspaceSession(false);
			IEclipsePreferences sessionPrefs = StatePersistParticipant.getSessionPreferences();
			if (bundleRegion.isRegionActivated()) {
				// Indicates a normal shut down
				// Activation levels and transition states are stored in the uninstall job
				super.runInWorkspace(monitor);
				if (getErrorStatusList().size() > 0) {
					final IBundleStatus multiStatus = createMultiStatus(new BundleStatus(StatusCode.ERROR,
							Activator.PLUGIN_ID, getName()));
					try {
						// Send errors to default output console when shutting down
						Activator.getBundleConsoleService().setSystemOutToIDEDefault();
					} catch (ExtenderException | NullPointerException e) {
						// Ignore and send to current setting
					}
					System.err.println(Msg.BEGIN_SHUTDOWN_ERROR);
					printStatus(multiStatus);
					System.err.println(Msg.END_SHUTDOWN_ERROR);
				}
			} else {
				// Activation levels are - always in state uninstalled - not saved in an deactivated
				// workspace. 
				// Save transition state for bundles in a deactivated workspace
				StatePersistParticipant.saveTransitionState(sessionPrefs, true);
			}
			// They should be, but ensure that saved and current pending transitions are in sync
			StatePersistParticipant.savePendingBuildTransitions(sessionPrefs,
					StatePersistParticipant.isWorkspaceSession());
			for (IProject project : getPendingProjects()) {
				bundleRegion.unregisterBundleProject(project);
			}
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
