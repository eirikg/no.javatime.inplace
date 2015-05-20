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
package no.javatime.inplace.ui.command.handlers;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.Install;
import no.javatime.inplace.bundlejobs.intface.Refresh;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.Start;
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.bundlejobs.intface.TogglePolicy;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.bundlejobs.intface.UpdateBundleClassPath;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleProperties;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Category;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Executes bundle menu commands for one or more projects. The bundle commands are common for the
 * bundle main menu and bundle pop-up menus.
 */
public abstract class BundleMenuActivationHandler extends AbstractHandler {

	final public static String PACKAGE_EXPLORER_ID = org.eclipse.jdt.ui.JavaUI.ID_PACKAGES;
	final public static String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";

	/**
	 * Schedules an installation job for the specified projects
	 * 
	 * @param projects to install
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	protected void installHandler(Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<Install> installExt = Activator.getTracker().getTrackedExtender(Install.class.getName());
			Install install = installExt.getService();
			install.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(install);
			installExt.ungetService();
		}
	}

	/**
	 * Schedule an activate project job for the specified projects.
	 * 
	 * @param projects to activate
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void activateProjectHandler(Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<ActivateProject> activateProjectExt = Activator.getTracker().getTrackedExtender(
					ActivateProject.class.getName());
			ActivateProject activateproject = activateProjectExt.getService();
			if (Activator.getBundleRegionService().getActivatedProjects().size() == 0) {
				activateproject.getJob().setName(Msg.ACTIVATE_WORKSPACE_JOB);
			}
			activateproject.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(activateproject);
			activateProjectExt.ungetService();
		}
	}

	/**
	 * Schedules a deactivate job for the specified bundle projects
	 * 
	 * @param projects to deactivate
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void deactivateHandler(Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<Deactivate> deactivateExtender = Activator.getTracker().getTrackedExtender(
					Deactivate.class.getName());
			Deactivate deactivate = deactivateExtender.getService();
			if (Activator.getBundleRegionService().getActivatedProjects().size() <= projects.size()) {
				deactivate.getJob().setName(Msg.DEACTIVATE_WORKSPACE_JOB);
			} else {
				deactivate.getJob().setName(Msg.DEACTIVATE_JOB);
			}
			deactivate.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(deactivate);
			deactivateExtender.ungetService();
		}
	}

	/**
	 * Schedules a start job for the specified bundle projects.
	 * 
	 * @param projects of bundle projects to start
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void startHandler(Collection<IProject> projects) throws ExtenderException {

		if (updated()) {
			Extender<Start> startExt = Activator.getTracker().getTrackedExtender(Start.class.getName());
			Start start = startExt.getService();
			start.addPendingProjects(projects);
			// Put update in the job queue before starting projects
			Activator.getBundleExecEventService().add(start);
			startExt.ungetService();
		}
	}

	/**
	 * Schedules a stop job for the specified projects
	 * 
	 * @param projects of bundle projects to stop
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void stopHandler(Collection<IProject> projects) throws ExtenderException {

		if (updated()) {
			Extender<Stop> stopExt = Activator.getTracker().getTrackedExtender(Stop.class.getName());
			Stop stop = stopExt.getService();
			stop.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(stop);
			stopExt.ungetService();
		}
	}

	/**
	 * Schedules a refresh job for the specified projects
	 * 
	 * @param projects of bundle projects to refresh
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void refreshHandler(Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		// Get any dirty projects before trying to update
		Collection<IProject> dirtyProjects = resourceState.getDirtyProjects();
		// Updated dirty projects or no dirty projects to update
		if (updated()) {
			Extender<Refresh> refreshExt = Activator.getTracker().getTrackedExtender(Refresh.class.getName());
			Refresh refresh = refreshExt.getService();
			// Has update saved and updated dirty projects
			if (dirtyProjects.size() > 0) {
				// If the refresh on update option is on, the update job will refresh the bundles
				// that are common (including closure) in dirty projects and projects to refresh
				if (Activator.getCommandOptionsService().isRefreshOnUpdate()) {
					Collection<IProject> copyProjects = new LinkedHashSet<>(projects);
					Extender<Update> updateExtender = Activator.getTracker().getTrackedExtender(
							Update.class.getName());
					Update update = updateExtender.getService();
					update.addPendingProjects(dirtyProjects);
					Collection<IProject> closure = update.getRequiringUpdateClosure();
					copyProjects.removeAll(closure);
					if (copyProjects.size() > 0) {
						refresh.addPendingProjects(copyProjects);
					}
					updateExtender.ungetService();
				} else {
					refresh.addPendingProjects(projects);
				}
				resourceState.waitOnBuilder(false);
				resourceState.waitOnBundleJob();
			} else {
				refresh.addPendingProjects(projects);
			}
			if (refresh.hasPendingProjects()) {
				Activator.getBundleExecEventService().add(refresh);
			}
			refreshExt.ungetService();
		}
	}

	/**
	 * Schedules an update job for the specified projects.
	 * <p>
	 * A pending update transition is added to each of the specified bundle projects.
	 * <p>
	 * If any of the specified bundle projects are members in any build error closure they will not by
	 * updated.
	 * <p>
	 * Changed but not saved (dirty) projects is saved before this update is scheduled, but they are
	 * excluded from update if the "Update on Build" option is on. The saved projects will
	 * automatically be triggered for update by the internal post build listener. As a consequence if
	 * all projects are dirty and "Update on Build" is on no update job is scheduled.
	 * 
	 * @param projects bundle projects to update
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void updateHandler(Collection<IProject> projects)
			throws ExtenderException {

		Extender<Update> updateExt = null;
		if (projects.size() > 0) {
			Collection<IProject> updateProjects = new LinkedHashSet<>(projects);
			ResourceState resourceState = Activator.getResourceStateService();
			Collection<IProject> dirtyProjects = resourceState.getDirtyProjects();
			if (resourceState.saveModifiedResources()) {
				updateExt = Activator.getTracker().getTrackedExtender(Update.class.getName());
				Update update = updateExt.getService();
				if (dirtyProjects.size() > 0 
						&& Activator.getBundleProjectCandidatesService().isAutoBuilding()) {
					if (Activator.getCommandOptionsService().isUpdateOnBuild()) {
						updateProjects.removeAll(dirtyProjects);
					}
					// Update built projects first
					resourceState.waitOnBuilder(false);
				}
				for (IProject updateProject : updateProjects) {
					if (update.canUpdate(updateProject)) {
						update.addPendingProject(updateProject);
						update.addUpdateTransition(updateProject);
					}
				}
				// If files are saved and auto build is on, the builder is scheduled before this update job
				// It is of no importance who runs the update job first, the auto builder calling the post
				// builder scheduling an update or this update. The second update job will return (the
				// update transitions are removed from the updated projects by the first update job) with no
				// common projects to update.
				if (update.hasPendingProjects()) {
					Activator.getBundleExecEventService().add(update);
				}
				updateExt.ungetService();
			}
		}
	}

	/**
	 * Schedules a reset job for the specified projects by running an uninstall job and an activate
	 * bundle job in sequence.
	 * 
	 * @param projects to reset
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	static public void resetHandler(final Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<Reset> resetExt = Activator.getTracker().getTrackedExtender(Reset.class.getName());
			Reset reset = resetExt.getService();
			reset.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(reset);
			resetExt.ungetService();
		}
	}

	/**
	 * Ask for save of any modified projects if auto build is off and calls update if auto build is on
	 * 
	 * @return false if there are any modified files after asking for save or calling update. True if
	 * there modified files after asking for save or calling update
	 */
	private static boolean updated() {

		ResourceState resourceState = Activator.getResourceStateService();
		if (!Activator.getCommandOptionsService().isUpdateOnBuild()) {
			resourceState.saveModifiedResources();
		} else if (resourceState.areResourcesDirty()) {
			updateHandler(resourceState.getDirtyProjects());
		}
		// Resource state after save and update, after save or no save
		return !resourceState.areResourcesDirty();
	}

	/**
	 * Interrupts the current running bundle job
	 */
	protected void interruptHandler() throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			BundleExecutor executor = resourceState.getRunningBundleJob();
			if (null != executor) {
				Thread thread = executor.getJob().getThread();
				if (null != thread) {
					// Requires that the user code (e.g. in the start method) is aware of interrupts
					thread.interrupt();
				}
			}
		}
	}

	/**
	 * Stops the current thread running the start and stop operation if the option for terminating
	 * endless start and stop operations is set to manual.
	 * <p>
	 * If the option for terminating endless start and stop operations is set to time out, the
	 * operation will be terminated automatically on time out
	 * 
	 * @throws ExtenderException if failing to get the command options service
	 * @throws InPlaceException if failing to get the stop operation
	 */
	protected void stopOperationHandler() throws ExtenderException, InPlaceException {

		final ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Activator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					BundleExecutor executor = resourceState.getRunningBundleJob();
					if (null != executor && null != executor.isStateChanging()) {
						executor.stopCurrentOperation();
					}
				}
			});
		}
	}

	/**
	 * Toggles between lazy and eager activation
	 * 
	 * @param projects of bundles containing the activation policy
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	protected void policyHandler(final Collection<IProject> projects) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<TogglePolicy> policyExt = Activator.getTracker().getTrackedExtender(
					TogglePolicy.class.getName());
			TogglePolicy policy = policyExt.getService();
			policy.addPendingProjects(projects);
			Activator.getBundleExecEventService().add(policy);
			policyExt.ungetService();
		}
	}

	/**
	 * Removes or inserts the default output folder in Bundle-ClassPath and schedules a reset job for
	 * bundle projects that have been updated when auto build is off.
	 * 
	 * @param projects to update the default output folder of
	 * @param addToPath add default output folder if true and remove default output folder if false
	 * @throws ExtenderException if failing to get the extender or the service for this bundle
	 * operation
	 */
	public static void updateClassPathHandler(final Collection<IProject> projects,
			final boolean addToPath) throws ExtenderException {

		ResourceState resourceState = Activator.getResourceStateService();
		if (resourceState.saveModifiedResources()) {
			Extender<UpdateBundleClassPath> classPathExt = Activator.getTracker().getTrackedExtender(
					UpdateBundleClassPath.class.getName());
			UpdateBundleClassPath classPath = classPathExt.getService();
			classPath.addPendingProjects(projects);
			classPath.setAddToPath(addToPath);
			Activator.getBundleExecEventService().add(classPath);
			classPathExt.ungetService();
		}
	}

	/**
	 * Shows the bundle view if it is hidden and hides it if it is open and there is no selected
	 * project (in list page or a details page) in the view. If visible, show details page if one
	 * project is specified and selected in the list page and show the list page if multiple projects
	 * are specified and the details page is active.
	 * 
	 * @param projects to display in the bundle view
	 */
	protected void bundleViewHandler(Collection<IProject> projects) {
		BundleProjectCandidates bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		if (!ViewUtil.isVisible(BundleView.ID)) {
			ViewUtil.show(BundleView.ID);
			updateBundleListPage(bundleProjectCandidates.toJavaProjects(bundleProjectCandidates
					.getInstallable()));
		} else {
			BundleView bv = BundleCommandsContributionItems.getBundleView();
			Collection<IJavaProject> javaProjects = bundleProjectCandidates.toJavaProjects(projects);
			int size = javaProjects.size();
			// Show list page
			if (bv.isDetailsPageActive()) {
				if (size <= 1) {
					bv.showProjects(
							bundleProjectCandidates.toJavaProjects(bundleProjectCandidates.getInstallable()),
							true);
				} else {
					bv.showProjects(javaProjects, true);
				}
			} else if (null != getSelectedProject()) {
				// Show details page
				if (size == 1) {
					bv.showProject(projects.toArray(new IProject[projects.size()])[0]);
				} else {
					IJavaProject jp = getSelectedJavaProject();
					if (null != jp) {
						bv.showProject(jp.getProject());
					} else {
						bv.showProject(projects.toArray(new IProject[projects.size()])[0]);
					}
				}
			} else {
				// Bundle view is open but there is no selection or detail page
				ViewUtil.hide(BundleView.ID);
			}
		}
	}

	/**
	 * Updates project status information in the list page in the bundle view for the specified
	 * projects
	 * 
	 * @param projects to refresh. Must not be null.
	 */
	static public void updateBundleListPage(final Collection<IJavaProject> projects) {

		final Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (ViewUtil.isVisible(BundleView.ID)) {
					BundleView bundleView = (BundleView) ViewUtil.get(BundleView.ID);
					if (bundleView != null) {
						bundleView.showProjects(projects, true);
					}
				}
			}
		});
	}

	/**
	 * Displays the closure options dependency dialog
	 * 
	 * @throws ExtenderException if failing to get the dependency dialog extender or service
	 */
	protected void dependencyDialogHandler() throws ExtenderException {

		Extension<DependencyDialog> dependencyExt = Activator.getTracker().getExtension(
				DependencyDialog.class.getName());
		DependencyDialog dependency = dependencyExt.getTrackedService();
		dependency.open();
		dependencyExt.closeTrackedService();
	}

	/**
	 * Toggles between showing and hiding the bundle console view
	 * 
	 * @throws ExtenderException if failing to get the bundle console view extender or service
	 */
	protected void bundleConsoleHandler() throws ExtenderException {

		Extension<BundleConsoleFactory> consoleExt = Activator.getTracker().getExtension(
				BundleConsoleFactory.class.getName());
		BundleConsoleFactory console = consoleExt.getTrackedService();
		if (!console.isConsoleViewVisible()) {
			console.showConsoleView();
		} else {
			console.closeConsoleView();
		}
		consoleExt.closeTrackedService();
	}

	/**
	 * Toggles between showing and hiding the bundle log view
	 * 
	 * @throws ExtenderException if failing to get the log view extender or service
	 */
	protected void bundleLogViewViewHandler() throws ExtenderException {

		Extension<BundleLogView> logViewExt = Activator.getTracker().getExtension(
				BundleLogView.class.getName());
		BundleLogView logView = logViewExt.getTrackedService();
		if (logView.isVisible()) {
			logView.hide();
		} else {
			logView.show();
		}
		logViewExt.closeTrackedService();
	}

	/**
	 * Get the selected Java plug-in project in the currently active part.
	 * 
	 * @return The selected Java project or null if no such selection exist
	 */
	public static IJavaProject getSelectedJavaProject() {

		// Get the selection from the active page
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null == page) {
			return null;
		}
		IWorkbenchPart activePart = page.getActivePart();
		if (null == activePart) {
			return null;
		}
		ISelection selection = null;
		if (activePart instanceof BundleView) {
			selection = page.getSelection(BundleView.ID);
		} else if (activePart instanceof IPackagesViewPart) {
			selection = page.getSelection(PACKAGE_EXPLORER_ID);
		} else if (activePart instanceof CommonNavigator) {
			selection = page.getSelection(PROJECT_EXPLORER_ID);
		}
		if (null != selection && !selection.isEmpty()) {
			return getSelectedJavaProject(selection);
		}
		return null;
	}

	/**
	 * Get the selected Java plug-in project from a selection in package explorer, project explorer or
	 * bundle list page.
	 * 
	 * @param selection to get the selected project from
	 * @return The selected Java project or null if selection is empty
	 */
	public static IJavaProject getSelectedJavaProject(ISelection selection) {

		// Get the java project from the selection
		IJavaProject javaProject = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			// BundleExecutor view
			if (element instanceof BundleProperties) {
				BundleProperties bp = (BundleProperties) element;
				javaProject = bp.getJavaProject();
				// Package explorer
			} else if (element instanceof IJavaProject) {
				javaProject = (IJavaProject) element;
				// Project or Package explorer
			} else if (element instanceof IProject) {
				IProject project = (IProject) element;
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {
						javaProject = JavaCore.create(project);
					}
				} catch (CoreException e) {
					return null;
				}
			}
			// The Java project must also be a plug-in project
			if (null != javaProject) {
				try {
					if (!javaProject.getProject().hasNature(BundleProjectCandidates.PLUGIN_NATURE_ID)) {
						return null;
					}
				} catch (Exception e) {
					return null;
				}
			}
		}
		return javaProject;
	}

	/**
	 * Get the selected Java plug-in project in the currently active part.
	 * 
	 * @return The selected Java project or null if no such selection exist
	 */
	public static IProject getSelectedProject() {

		// Get the selection from the active page
		Activator activator = Activator.getDefault();
		if (null == activator) {
			return null;
		}
		IWorkbenchPage page = activator.getActivePage();
		if (null == page) {
			return null;
		}
		IWorkbenchPart activePart = page.getActivePart();
		if (null == activePart) {
			return null;
		}
		ISelection selection = null;
		if (activePart instanceof BundleView) {
			selection = page.getSelection(BundleView.ID);
		} else if (activePart instanceof IPackagesViewPart) {
			selection = page.getSelection(PACKAGE_EXPLORER_ID);
		} else if (activePart instanceof CommonNavigator) {
			selection = page.getSelection(PROJECT_EXPLORER_ID);
		}
		if (null != selection && !selection.isEmpty()) {
			return getSelectedProject(selection);
		}
		return null;
	}

	/**
	 * Get the selected plug-in project from a selection in package explorer, project explorer or
	 * bundle list page.
	 * 
	 * @param selection to get the selected project from
	 * @return The selected project or null if selection is empty
	 */
	public static IProject getSelectedProject(ISelection selection) {

		// Get the java project from the selection
		IProject project = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			// BundleExecutor view
			if (element instanceof BundleProperties) {
				BundleProperties bp = (BundleProperties) element;
				project = bp.getProject();
				// Package explorer
			} else if (element instanceof IJavaProject) {
				project = ((IJavaProject) element).getProject();
				// Project or Package explorer
			} else if (element instanceof IProject) {
				project = (IProject) element;
			}
		}
		return project;
	}

	/**
	 * Default way to schedule jobs, with no delay, saving files before schedule and waiting on
	 * builder to finish.
	 * 
	 * @param bundleExecutor bundle executor operation to schedule
	 * @throws ExtenderException If failing to get the resource state service or adding he specified
	 * job to job queue
	 */
	// static public void jobHandler(BundleExecutor bundleExecutor) throws ExtenderException {
	//
	// ResourceState resourceState = Activator.getResourceStateService();
	// if (resourceState.saveModifiedResources()) {
	// resourceState.waitOnBuilder(true);
	// Activator.getBundleExecEventService().add(bundleExecutor);
	// }
	// }

	/**
	 * Restore state of a checked menu entry, and set the state of the specified category id to the
	 * state of the restored menu entry
	 * 
	 * @param commandId id of the menu contribution
	 * @param categoryId category id corresponding to the command id (and menu id)
	 * @return the command object of the corresponding command id or null if the command or state of
	 * the checked menu entry could not be obtained
	 */
	static public Command setCheckedMenuEntry(String categoryId, String commandId) {

		Command command = null;
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(
				ICommandService.class);
		if (commandService != null) {
			command = commandService.getCommand(commandId);
			if (command.isDefined()) {
				State state = command.getState(RegistryToggleState.STATE_ID);
				if (state != null) {
					Boolean stateVal = ((Boolean) state.getValue()).booleanValue();
					Category.setState(categoryId, stateVal);
					return command;
				}
			}
		}
		return command;
	}
}
