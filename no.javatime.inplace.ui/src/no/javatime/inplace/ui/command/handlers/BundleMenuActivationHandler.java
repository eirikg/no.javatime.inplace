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
import java.util.Collections;

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.InstallJob;
import no.javatime.inplace.bundlejobs.RefreshJob;
import no.javatime.inplace.bundlejobs.ResetJob;
import no.javatime.inplace.bundlejobs.StartJob;
import no.javatime.inplace.bundlejobs.StopJob;
import no.javatime.inplace.bundlejobs.UpdateScheduler;
import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dialogs.OpenProjectHandler;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.dependencies.service.DependencyDialog;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleProperties;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;
import no.javatime.util.messages.views.BundleConsoleFactory;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.osgi.framework.Bundle;

/**
 * Executes bundle menu commands for one or more projects. The bundle commands are
 * common for the bundle main menu and context pop-up menu.
 */
public abstract class BundleMenuActivationHandler extends AbstractHandler {

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
			if (ProjectProperties.getActivatedProjects().size() > 0) {
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
		if (ProjectProperties.getActivatedProjects().size() <= projects.size()) {
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
		if (projects.size() > 0) {
			RefreshJob refreshJob = new RefreshJob(RefreshJob.refreshJobName, projects);
			jobHandler(refreshJob);
		}
	}

	/**
	 * Schedules an update job for the specified projects and an activate job for
	 * deactivated projects providing capabilities to projects to update
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
		BundleJob job = OpenProjectHandler.getRunningBundleJob();
		if (null != job) {
			Thread thread = job.getThread();
			if (null != thread) {
				// Requires that the user code (e.g. in the start method) is aware of interrupts
				thread.interrupt();
			}
		}
	}
	
	/**
	 * Stops the current thread running the start and stop operation 
	 */
	protected void stopOperation() {
		Activator.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (!Activator.getDefault().getCommandOptionsService().isTimeOut()) {
						BundleJob job = OpenProjectHandler.getRunningBundleJob();
						if (null != job && BundleJob.isStateChanging()) {			
							job.stopCurrentBundleOperation(new NullProgressMonitor());
						}
					}
				} catch (IllegalStateException e) {
					// Also caught by the bundle API
				} catch (InPlaceException e) {
					// Ignore
				}
			}
		});
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
			WorkspaceJob togglePolicyJob = new BundleJob(Message.getInstance().formatString("toggle_policy_job_name")) {
				@Override
				public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				
					BundleManager.addBundleTransitionListener(this);
					for (IProject project : projects) {
							try {					
								BundleProject.toggleActivationPolicy(project);
								BundleRegion bundleRegion = BundleJobManager.getRegion();
								// No bundle jobs (which updates the bundle view) are run when the project(s) are deactivated or auto build is off
								Bundle bundle = bundleRegion.get(project);
								if (!ProjectProperties.isAutoBuilding()) {
									if (bundleRegion.isActivated(bundle)) {
										String msg = WarnMessage.getInstance().formatString("policy_updated_auto_build_off", project.getName());	
										addWarning(null, msg, project);
									} 
								}
								try {
									if (Category.getState(Category.infoMessages) && !Activator.getDefault().getCommandOptionsService().isUpdateOnBuild()) {
										if (bundleRegion.isActivated(bundle)) {
											UserMessage.getInstance().getString("autoupdate_off", project.getName());
										}
									}
								} catch (InPlaceException e) {
									addError(e, project);
								}
								if (null != bundle) {
									if ((bundle.getState() & (Bundle.INSTALLED)) != 0) {
										reInstall(Collections.<IProject>singletonList(project), new SubProgressMonitor(monitor, 1));
									} else if ((bundle.getState() & (Bundle.RESOLVED)) != 0) { 
										// Do not start bundle if in state resolve when toggling policy
										BundleJobManager.getTransition().addPending(bundle, Transition.RESOLVE);
									}
								}
							} catch (InPlaceException e) {
								String msg = ExceptionMessage.getInstance().formatString("error_set_policy", project.getName());
								addError(e, msg, project);
							}
						}
					try {
						return super.runInWorkspace(monitor);
					} catch (CoreException e) {
						String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
						return new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, e);
					} finally {
						BundleManager.removeBundleTransitionListener(this);
					}
				}
			};
			BundleJobManager.addBundleJob(togglePolicyJob, 0);
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
		if (null == projects || projects.size() == 0) {
			return;
		}
		if (so.saveModifiedFiles()) {
			OpenProjectHandler.waitOnBuilder();
			final ResetJob resetJob = new ResetJob();
			WorkspaceJob updateBundleClassPathJob = new BundleJob(Msg.UPDATE_BUNDLE_CLASS_PATH_JOB) {
				
				@Override
				public IBundleStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					
					BundleManager.addBundleTransitionListener(this);
					for (IProject project : projects) {
						try {
							if (!ProjectProperties.hasManifestBuildErrors(project)) {
								if (addToPath) {
									if (BundleProject.addOutputLocationToBundleClassPath(project)) {
										resetJob.addPendingProject(project);
									} 
								} else {
									if (BundleProject.removeOutputLocationFromClassPath(project)) {
										resetJob.addPendingProject(project);
									}
								}
							}
						} catch (InPlaceException e) {
							String msg = ErrorMessage.getInstance().formatString("error_set_classpath", project.getName());
							addError(e, msg, project);
						}
					}
					try {
						return super.runInWorkspace(monitor);
					} catch (CoreException e) {
						String msg = ErrorMessage.getInstance().formatString("error_end_job", getName());
						return new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, e);
					} finally {
						BundleManager.removeBundleTransitionListener(this);
					}
				}
			};
			BundleJobManager.addBundleJob(updateBundleClassPathJob, 0);
			if (projects.size() > 0 && !ProjectProperties.isAutoBuilding()) {
				if (Category.getState(Category.infoMessages)) {
					UserMessage.getInstance().getString("atobuild_of_reset");
				}
				resetJob.reset(ResetJob.resetJobName);		
			}
		}
	}

	/**
	 * Displays the closure options dependency dialog
	 */
	protected void dependencyHandler() {

		try {
			Extension<DependencyDialog> ext = new Extension<>(DependencyDialog.class);
			DependencyDialog depService = ext.getService();
			if (null == depService) {
				throw new InPlaceException("failed_to_get_service_for_interface", DependencyDialog.class.getName());
			}
			depService.open();
			// new DependencyDialogExtension().openAsService();
		} catch (InPlaceException e){
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);						
		}		
	}
	
	/**
	 * Shows the bundle view if it is hidden and hides it if it is open and there is no 
	 * selected project (in list page or a details page) in the view. 
	 * If visible, show details page if one project is specified and selected in the list page and 
	 * the list page if multiple projects are specified and the details page is active.
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

	public static void openWizard(String id) { 

		// First see if this is a "new wizard". 
		IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard(id); 
		// If not check if it is an "import wizard". 
		if  (descriptor == null) {   
			descriptor = PlatformUI.getWorkbench().getImportWizardRegistry().findWizard(id); 
		} 
		// Or maybe an export wizard 
		if  (descriptor == null) {   
			descriptor = PlatformUI.getWorkbench().getExportWizardRegistry().findWizard(id); 
		} 
		try  {   
			// Then if we have a wizard, open it.   
			if  (descriptor != null) {     
				IWizard wizard = descriptor.createWizard();     
				WizardDialog wd = new  WizardDialog(Activator.getDisplay().getActiveShell(), wizard);     
				wd.setTitle(wizard.getWindowTitle());     
				wd.open();   
			} 
		} catch  (CoreException e) {   
			String msg = ExceptionMessage.getInstance().formatString("error_open_create_bundle_wizard", e);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
	}

	/**
	 * Updates project status information in the list page in the bundle view for the specified projects
	 * 
	 * @param projects to refresh
	 */
	static public void updateBundleListPage(final Collection<IJavaProject> projects) {

		final Display display = Activator.getDisplay();
		if (null == display || null == projects) {
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
	 * Toggles between showing and hiding the message CONSOLE view
	 */
	protected void consoleHandler(Collection<IProject> projects) {
		if (!BundleConsoleFactory.isConsoleViewVisible()) {
			BundleConsoleFactory.showConsoleView();
		} else {
			BundleConsoleFactory.closeConsoleView();
		}
	}

	/**
	 * Toggles between showing and hiding the message view
	 */
	protected void messageViewHandler() {
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
			selection = page.getSelection(ProjectProperties.PACKAGE_EXPLORER_ID);
		} else if (activePart instanceof CommonNavigator) {
			selection = page.getSelection(ProjectProperties.PROJECT_EXPLORER_ID);
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
					if (!javaProject.getProject().hasNature(ProjectProperties.PLUGIN_NATURE_ID)) {
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
			selection = page.getSelection(ProjectProperties.PACKAGE_EXPLORER_ID);
		} else if (activePart instanceof CommonNavigator) {
			selection = page.getSelection(ProjectProperties.PROJECT_EXPLORER_ID);
		}
		if (null != selection && !selection.isEmpty()) {
			return getSelectedProject(selection);			
		}
		return null;
	}

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
