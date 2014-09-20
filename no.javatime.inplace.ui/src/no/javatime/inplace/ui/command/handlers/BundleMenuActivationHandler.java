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

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.RefreshJob;
import no.javatime.inplace.bundlejobs.ResetJob;
import no.javatime.inplace.bundlejobs.StartJob;
import no.javatime.inplace.bundlejobs.StopJob;
import no.javatime.inplace.bundlejobs.TogglePolicyJob;
import no.javatime.inplace.bundlejobs.UpdateBundleClassPathJob;
import no.javatime.inplace.bundlejobs.UpdateScheduler;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dialogs.OpenProjectHandler;
import no.javatime.inplace.extender.provider.ExtenderException;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.views.BundleProperties;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Executes bundle menu commands for one or more projects. The bundle commands are
 * common for the bundle main menu and bundle pop-up menus.
 */
public abstract class BundleMenuActivationHandler extends AbstractHandler {

	final public static String PACKAGE_EXPLORER_ID = org.eclipse.jdt.ui.JavaUI.ID_PACKAGES;
	final public static String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";

	/**
	 * Schedules an installation job for the specified projects
	 * 
	 * @param projects to install
	 */
	protected void installHandler(Collection<IProject> projects) {

		InstallJob installJob = new InstallJob(InstallJob.installJobName, projects);
		jobHandler(installJob);
	}

	/**
	 * Schedule an activate project job for the specified projects. An activate project job enables the JavaTime
	 * nature for the specified projects
	 * 
	 * @param projects to activate
	 */
	static public void activateProjectHandler(Collection<IProject> projects) {

		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();
			ActivateProjectJob activateJob = null;
			if (BundleProjectState.getNatureEnabledProjects().size() > 0) {
				activateJob = new ActivateProjectJob(ActivateProjectJob.activateProjectsJobName, projects);
			} else {
				activateJob = new ActivateProjectJob(ActivateProjectJob.activateWorkspaceJobName, projects);
			}
			jobHandler(activateJob);
		}
	}

	/**
	 * Schedules a deactivate job for the specified bundle projects
	 * 
	 * @param projects to deactivate
	 */
	static public void deactivateHandler(Collection<IProject> projects) {

		DeactivateJob deactivateJob = null;
		if (BundleProjectState.getNatureEnabledProjects().size() <= projects.size()) {
			deactivateJob = new DeactivateJob(DeactivateJob.deactivateWorkspaceJobName, projects);			
		} else {
			deactivateJob = new DeactivateJob(DeactivateJob.deactivateJobName, projects);
		}
		jobHandler(deactivateJob);
	}

	/**
	 * Schedules a start job for the specified bundle projects.
	 * 
	 * @param projects of bundle projects to start
	 */
	static public void startHandler(Collection<IProject> projects) {

		StartJob startJob = new StartJob(StartJob.startJobName, projects);
		jobHandler(startJob);
	}

	/**
	 * Schedules a stop job for the specified projects
	 * 
	 * @param projects of bundle projects to stop
	 */
	static public void stopHandler(Collection<IProject> projects) {

		StopJob stopJob = new StopJob(StopJob.stopJobName, projects);
		jobHandler(stopJob);
	}
		
	/**
	 * Schedules a refresh job for the specified projects
	 * 
	 * @param projects of bundle projects to refresh
	 */
	static public void refreshHandler(Collection<IProject> projects) {

		RefreshJob refreshJob = new RefreshJob(RefreshJob.refreshJobName, projects);
		jobHandler(refreshJob);
	}

	/**
	 * Schedules an update job for the specified projects. Projects members in any build error
	 * closure will not updated.
	 * 
	 * @param projects bundle projects to update
	 */
	static public void updateHandler(Collection<IProject> projects) {

		UpdateScheduler.scheduleUpdateJob(projects, 0);
	}
		
	/**
	 * Schedules a reset job for the specified projects by running an uninstall
	 * job and an activate bundle job in sequence.
	 * @param projects to reset
	 */
	static public void resetHandler(final Collection<IProject> projects) {

		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();
			ResetJob resetJob = new ResetJob(projects);
			resetJob.reset(ResetJob.resetJobName);		
		}
	}
	
	/**
	 * Interrupts the current running bundle job
	 */
	protected  void interruptHandler() {
		
		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			BundleJob job = OpenProjectHandler.getRunningBundleJob();
			if (null != job) {
				Thread thread = job.getThread();
				if (null != thread) {
					// Requires that the user code (e.g. in the start method) is aware of interrupts
					thread.interrupt();
				}
			}
		}
	}
	
	/**
	 * Stops the current thread running the start and stop operation if the
	 * option for terminating endless start and stop operations is set to manual.
	 * <p>
	 * If the option for terminating endless start and stop operations is set to 
	 * time out, the operation will be terminated automatically on time out
	 * @throws InPlaceException if failing to get the command options service
	 */
	protected void stopOperationHandler() throws InPlaceException {
		
		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			Activator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!Activator.getDefault().getCommandOptionsService().isTimeOut()) {
						BundleJob job = OpenProjectHandler.getRunningBundleJob();
						if (null != job && BundleJob.isStateChanging()) {			
							job.stopCurrentBundleOperation(new NullProgressMonitor());
						}
					}
				}
			});
		}
	}
	
	/**
	 * Toggles between lazy and eager activation
	 * 
	 * @param projects of bundles containing the activation policy
	 */
	protected void policyHandler(final Collection<IProject> projects) {

		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();		
			TogglePolicyJob pj = new TogglePolicyJob(TogglePolicyJob.policyJobName, projects);
			BundleJobManager.addBundleJob(pj, 0);
		}
	}

	/**
	 * Removes or inserts the default output folder in Bundle-ClassPath and schedules a reset job for bundle projects that have been 
	 * updated when auto build is off.
	 * 
	 * @param projects to update the  default output folder of
	 * @param addToPath add default output folder if true and remove default output folder if false
	 */
	public static void updateClassPathHandler(final Collection<IProject> projects, final boolean addToPath) {

		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();
			UpdateBundleClassPathJob updBundleClasspath = 
					new UpdateBundleClassPathJob(UpdateBundleClassPathJob.updateBundleClassJobName, projects);
			updBundleClasspath.setAddToPath(addToPath);
			BundleJobManager.addBundleJob(updBundleClasspath, 0);
		}
	}

	/**
	 * Displays the closure options dependency dialog
	 * 
	 * @throws ExtenderException if failing to get the extender for the bundle console view
	 * @throws InPlaceException if failing to get the extension service for the bundle console view
	 */
	protected void dependencyDialogHandler() throws InPlaceException, ExtenderException {

		Extension<DependencyDialog> ext = new Extension<>(DependencyDialog.class);
		DependencyDialog depService = ext.getService();
		if (null == depService) {
			throw new InPlaceException("failed_to_get_service_for_interface", DependencyDialog.class.getName());
		}
		depService.open();
	}
	
	/**
	 * Shows the bundle view if it is hidden and hides it if it is open and there is no 
	 * selected project (in list page or a details page) in the view. 
	 * If visible, show details page if one project is specified and selected in the list page and 
	 * show the list page if multiple projects are specified and the details page is active.
	 * 
	 * @param projects to display in the bundle view
	 */
	@SuppressWarnings("restriction")
	protected void bundleViewHandler(Collection<IProject> projects) {

		if (!Message.isViewVisible(BundleView.ID)) {
			Message.showView(BundleView.ID);
			updateBundleListPage(ProjectProperties.toJavaProjects(ProjectProperties.getInstallableProjects()));
		} else {
			BundleView bv = BundleCommandsContributionItems.getBundleView();
			Collection<IJavaProject> javaProjects = ProjectProperties.toJavaProjects(projects);
			int size = javaProjects.size();
			// Show list page
			if (bv.isDetailsPageActive()) {
				if (size <= 1) {
					bv.showProjects(ProjectProperties.toJavaProjects(ProjectProperties.getInstallableProjects()), true);
				} else {
					bv.showProjects(javaProjects, true);
				}
			} else if (null != getSelectedProject()){
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
				Message.hideView(BundleView.ID);
			}
		} 
		// No projects open, start the new bundle wizard 
		openWizard(org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard.PLUGIN_POINT);
	}
	
	/**
	 * Opens wizard specified by the wizard id parameter
	 * <p>
	 * Candidate wizards are new, import and export wizards
	 * @param id the wizard id of the wizard to open
	 */
	public static void openWizard(String id) { 

		// First: search for "new wizard". 
		IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard(id); 
		// Second: search for "import wizard". 
		if  (descriptor == null) {   
			descriptor = PlatformUI.getWorkbench().getImportWizardRegistry().findWizard(id); 
		} 
		// Third: search for  "export wizard" 
		if  (descriptor == null) {   
			descriptor = PlatformUI.getWorkbench().getExportWizardRegistry().findWizard(id); 
		} 
		try  {   
			// Open the wizard   
			if  (descriptor != null) {     
				IWizard wizard = descriptor.createWizard();     
				WizardDialog wd = new  WizardDialog(Activator.getDisplay().getActiveShell(), wizard);     
				wd.setTitle(wizard.getWindowTitle());     
				wd.open();   
			} 
		} catch  (CoreException | SWTException e) {   
			String msg = ExceptionMessage.getInstance().formatString("error_open_create_bundle_wizard", e);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Updates project status information in the list page in the bundle view for the specified projects
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
				if (Message.isViewVisible(BundleView.ID)) {						
					BundleView bundleView = (BundleView) Message.getView(BundleView.ID);
					if (bundleView != null) {
						bundleView.showProjects(projects, true);
					}
				}
			}
		});
	}

	/**
	 * Toggles between showing and hiding the bundle console view
	 * 
	 * @throws ExtenderException if failing to get the extender for the bundle console view
	 * @throws InPlaceException if failing to get the extension service for the bundle console view
	 */
	protected void bundleConsoleHandler() throws InPlaceException, ExtenderException {

		Extension<BundleConsoleFactory> ext = new Extension<>(BundleConsoleFactory.class);
		BundleConsoleFactory bundleConsoleService = ext.getService();
		if (null == bundleConsoleService) {
			throw new InPlaceException("failed_to_get_service_for_interface", BundleConsoleFactory.class.getName());
		}	
		if (!bundleConsoleService.isConsoleViewVisible()) {
			bundleConsoleService.showConsoleView();
		} else {
			bundleConsoleService.closeConsoleView();
		}
	}

	/**
	 * Toggles between showing and hiding the bundle log view
	 * 
	 * @throws ExtenderException if failing to get the extender for the bundle log view
	 * @throws InPlaceException if failing to get the extension service for the bundle log view
	 */
	protected void bundleLogViewViewHandler() throws InPlaceException, ExtenderException {

		Extension<BundleLogView> ext = new Extension<>(BundleLogView.class);
		BundleLogView viewService = ext.getService();
		if (null == viewService) {
			throw new InPlaceException("failed_to_get_service_for_interface", BundleLogView.class.getName());
		}
		if (viewService.isVisible()) {
			viewService.hide();
		} else {
			viewService.show();
		}
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
	 * Get the selected Java plug-in project from a selection in package explorer,
	 * project explorer or bundle list page.
	 * @param selection to get the selected project from
	 * @return The selected Java project or null if selection is empty
	 */
	public static IJavaProject getSelectedJavaProject(ISelection selection) {

		// Get the java project from the selection
		IJavaProject javaProject = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			// Bundles view
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
					if (!javaProject.getProject().hasNature(BundleProjectState.PLUGIN_NATURE_ID)) {
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
	 * Get the selected plug-in project from a selection in package explorer,
	 * project explorer or bundle list page.
	 * @param selection to get the selected project from
	 * @return The selected project or null if selection is empty
	 */
	public static IProject getSelectedProject(ISelection selection) {

		// Get the java project from the selection
		IProject project = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			// Bundles view
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
	 * Default way to schedule jobs, with no delay, saving files before schedule, 
	 * waiting on builder to finish, no progress dialog, run the job via the bundle view 
	 * if visible showing a half busy cursor and also displaying the job name in the 
	 * content bar of the bundle view
	 * 
	 * @param job to schedule
	 */
	static public void jobHandler(WorkspaceJob job) {

		OpenProjectHandler so = new OpenProjectHandler();
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();
			BundleJobManager.addBundleJob(job, 0);
		}
	}

	/**
	 * Restore state of a checked menu entry, and set the state of the specified
	 * category id to the state of the restored menu entry
	 * 
	 * @param commandId id of the menu contribution
	 * @param categoryId category id corresponding to the command id (and menu id)
	 * @return the command object of the corresponding command id or null if the
	 * command or state of the checked menu entry could not be obtained
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
