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
package no.javatime.inplace.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.events.BundleTransitionEvent;
import no.javatime.inplace.region.events.BundleTransitionEventListener;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.contributions.BundleCommandsContributionItems;
import no.javatime.inplace.ui.command.handlers.BundleMenuActivationHandler;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Maintains a bundle details page view and a bundle list page view with bundle properties and
 * status information.
 * <p>
 * Local menus and a tool bar is provided for executing bundle operations, toggle activation policy
 * on a bundle, open manifest editor, flip between list and details page and and show/hide bundle
 * console and message view.
 * <p>
 * Pages are updated with bundle status information received from job, bundle and resource listeners
 */
public class BundleView extends ViewPart implements ISelectionListener, BundleListener,
		BundleTransitionEventListener, IResourceChangeListener, IJobChangeListener, IAdaptable {

	public static String ID = BundleView.class.getName();

	// The page book contains one details and one list page
	private PageBook pagebook;
	private TableViewer bundleDetailsPage;
	private TableViewer bundleListPage;
	// Local pull down menu
	private IMenuManager pullDownMenuManager;
	// Common for both pages
	private BundleContentProvider bundleContentProvider;

	// Local tool bar descriptors
	final private static ImageDescriptor linkedWithImage = Activator
			.getImageDescriptor("icons/link_with.gif"); //$NON-NLS-1$
	final private static ImageDescriptor mfEditorImage = Activator
			.getImageDescriptor("icons/edit_manifest.gif"); //$NON-NLS-1$

	// List and details page caption descriptors and images
	final private static ImageDescriptor detailsTitleImageDesc = Activator
			.getImageDescriptor("icons/gear_details_title.png"); //$NON-NLS-1$
	final private static ImageDescriptor listTitleImageDesc = Activator
			.getImageDescriptor("icons/gear_list_title.png"); //$NON-NLS-1$
	final private Image detailsTitleImage = detailsTitleImageDesc.createImage();
	final private Image listTitleImage = listTitleImageDesc.createImage();

	// Toggle between bundle list and bundle details page
	private Action flipPageAction;
	// Deactivate or activate bundle project
	private Action activateAction;
	// Reset bundle when activated
	private Action resetAction;
	// Update bundle when activated
	private Action updateAction;
	// Refresh bundle when activated
	private Action refreshAction;
	// Stop bundle when bundle in state active/starting and start bundle when in state resolved
	private Action startStopAction;
	// Opens bundle in Plug-in Manifest editor
	private Action editManifestAction;
	// Inserts and removes the Bundle-ClassPath header with default output folder entry in manifest
	private Action updateClassPathAction;
	// Link with explorers
	private Action linkWithAction;
	private boolean linkWithState;
	// Select bundles with this index next time the list page is updated
	private int selectIndex = 0;
	// Renamed, deleted and closed projects are marked as removed to prohibit displaying them in pages
	private IProject removedProject;
	// Dynamically delegates the selection provider role to the currently active page.
	final private SelectionProviderIntermediate selectionProviderIntermediate = new SelectionProviderIntermediate();
	// Refreshes the bundle properties page
	private BundlePropertySheetPage bundlePropertySheetPage;
	// Save/restore session state
	private IMemento memento;
	// Track selections in explorers
	private IViewPart packageExplorer;
	private IViewPart projectExplorer;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	/**
	 * Save session state of selected project if any, the link with explorers state and the flip page
	 * state
	 * 
	 * @param memento interface to persistent state
	 */
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (null == memento) {
			return;
		}
		IMemento selMemento = memento.createChild("Selection"); //$NON-NLS-1$
		IProject project = getSelectedProject();
		if (null != project) {
			selMemento.putString("SelectedProject", project.getName()); //$NON-NLS-1$
		}
		IMemento linkWithMemento = memento.createChild("LinkWith"); //$NON-NLS-1$
		linkWithMemento.putBoolean("Explorers", linkWithState); //$NON-NLS-1$

		IMemento detailsPageMemento = memento.createChild("DetailsPage"); //$NON-NLS-1$
		detailsPageMemento.putBoolean("PageSelection", (isDetailsPageActive() ? true : false)); //$NON-NLS-1$
	}

	/**
	 * Set selection to selected bundle project from previous session, restore the link with explorers
	 * state and the flip page state.
	 * <p>
	 * Input should have been set before restoring the selection.
	 * 
	 * @param memento interface to access persisted state
	 */
	public void restoreState(IMemento memento) {
		String projectName = null;
		if (null == memento) {
			return; // Drop restore and continue silently
		}
		try {
			// Set selection to selected project at shutdown if any
			BundleProjectCandidates bundleProjectCandidates = Activator
					.getBundleProjectCandidatesService();
			IMemento[] selMemento = memento.getChildren("Selection"); //$NON-NLS-1$
			if (null != selMemento && selMemento.length == 1) {
				projectName = selMemento[0].getString("SelectedProject"); //$NON-NLS-1$
				if (null != projectName) {
					selectProject(bundleProjectCandidates.getProject(projectName), true);
				}
			}
			// If link with explores was enabled at shut down enable it
			IMemento[] linkWithMemento = memento.getChildren("LinkWith"); //$NON-NLS-1$
			if (null != linkWithMemento && linkWithMemento.length == 1) {
				linkWithState = linkWithMemento[0].getBoolean("Explorers"); //$NON-NLS-1$
				linkWithAction.setChecked(linkWithState);
			}
			// If the details page was active at shutdown flip to details page on the restored project
			IMemento[] detailsPageMemento = memento.getChildren("DetailsPage"); //$NON-NLS-1$
			if (null != detailsPageMemento && detailsPageMemento.length == 1) {
				boolean detailsPageState = detailsPageMemento[0].getBoolean("PageSelection"); //$NON-NLS-1$
				if (detailsPageState && null != projectName) {
					showProject(bundleProjectCandidates.getProject(projectName));
				}
			}
		} catch (ExtenderException e) {
			// Project not accessible. Ignore set selection
		}
	}

	/**
	 * Create page book with two pages, resource, job and bundle listeners, page selection providers,
	 * actions, command contributors, context menu, tool-bar and pull-down menu
	 */
	@Override
	public void createPartControl(Composite parent) {

		pagebook = new PageBook(parent, SWT.NONE);
		// Shared content provider
		bundleContentProvider = new BundleContentProvider();
		// Page book bundle details page
		bundleDetailsPage = new TableViewer(pagebook, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL);
		BundleDetailsLabelProvider bundleDetailsLabelProvider = new BundleDetailsLabelProvider();
		bundleDetailsPage.setLabelProvider(bundleDetailsLabelProvider);
		bundleDetailsPage.setContentProvider(bundleContentProvider);
		bundleDetailsLabelProvider.createColumns(bundleDetailsPage);
		// Page book bundles list page
		bundleListPage = new TableViewer(pagebook, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL);
		BundleListLabelProvider bundleListLabelProvider = new BundleListLabelProvider();
		bundleListPage.setLabelProvider(bundleListLabelProvider);
		bundleListPage.setContentProvider(bundleContentProvider);
		bundleListLabelProvider.createColumns(bundleListPage);

		IWorkbenchPartSite site = getSite();
		// Selection provider that delegates to page specific selection providers
		site.setSelectionProvider(selectionProviderIntermediate);
		// Do not use post change listener due to missing user selections while bundle jobs are running
		bundleListPage.addSelectionChangedListener(bundleListListener);
		bundleDetailsPage.addSelectionChangedListener(bundleDetailsListener);
		// The bundle view listen to selections in package and project explorer
		site.getWorkbenchWindow().getSelectionService()
				.addSelectionListener(BundleMenuActivationHandler.PACKAGE_EXPLORER_ID, this);
		site.getWorkbenchWindow().getSelectionService()
				.addSelectionListener(BundleMenuActivationHandler.PROJECT_EXPLORER_ID, this);
		// Open manifest editor on double click in both pages
		bundleDetailsPage.addDoubleClickListener(bundleDoubleClickListener);
		bundleListPage.addDoubleClickListener(bundleDoubleClickListener);
		// Bundle state and transition listeners, bundle jobs and resource listeners
		// to update bundle status in pages
		Activator.getContext().addBundleListener(this);
		Job.getJobManager().addJobChangeListener(this);
		BundleTransitionListener.addBundleTransitionListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				this,
				IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE
						| IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		// Local bundle actions, tool bars and pull down menu
		createLocalActions();
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(linkWithAction);
		toolBarManager.add(flipPageAction);
		toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		toolBarManager.add(activateAction);
		toolBarManager.add(startStopAction);
		toolBarManager.add(updateAction);
		toolBarManager.add(refreshAction);
		toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		toolBarManager.add(editManifestAction);
		toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		pullDownMenuManager = actionBars.getMenuManager();
		pullDownMenuListener.menuAboutToShow(pullDownMenuManager);
		pullDownMenuManager.setRemoveAllWhenShown(true);
		pullDownMenuManager.addMenuListener(pullDownMenuListener);

		// Context pop-up menus share command framework with main menu and package/project explorers
		// context menu
		MenuManager listContextMenuManager = new MenuManager() {

			/**
			 * Remove contributions from other plug-ins
			 */
			@Override
			public IContributionItem[] getItems() {
				return filterItems(super.getItems());
			}
		};

		MenuManager detailsContextMenuManager = new MenuManager() {

			/**
			 * Remove contributions from other plug-ins
			 */
			@Override
			public IContributionItem[] getItems() {
				return filterItems(super.getItems());
			}
		};
		// Install and register context menu contributions for both pages from menu extension definition
		Menu detailsContextMenu = detailsContextMenuManager.createContextMenu(bundleDetailsPage
				.getTable());
		Menu listContextMenu = listContextMenuManager.createContextMenu(bundleListPage.getTable());
		bundleListPage.getTable().setMenu(listContextMenu);
		bundleDetailsPage.getTable().setMenu(detailsContextMenu);
		site.registerContextMenu(detailsContextMenuManager, bundleDetailsPage);
		site.registerContextMenu(listContextMenuManager, bundleListPage);
		loadBundleProjects();
	}

	/**
	 * Load and display bundle projects and restore to last saved display state
	 */
	private void loadBundleProjects() {

		try {
			// Load all bundle projects
			BundleProjectCandidates bundleProjectCandidates = Activator
					.getBundleProjectCandidatesService();
			Collection<IJavaProject> javaProjects = bundleProjectCandidates
					.toJavaProjects(bundleProjectCandidates.getInstallable());
			// Initially set input to list page in view and display loaded bundle projects
			showProjects(javaProjects, true);
			// Restore view state (selection, details or list page and link status with explorers)
			restoreState(memento);
			// Update the content description display field if workspace is deactivated
			Extension<ActivateProject> activateProjectExtension = Activator.getTracker().getExtension(
					ActivateProject.class.getName());
			ActivateProject activateProject = activateProjectExtension.getTrackedService();
			if (!activateProject.isProjectWorkspaceActivated()) {
				pagebook.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						setContentDescription("Workspace Deactivated"); //$NON-NLS-1$
					}
				});
			}
			activateProjectExtension.closeTrackedService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.CREATE_BUNDLE_VIEW_ERROR,
							e), StatusManager.LOG);
		}
	}

	/**
	 * Dispose images and listeners for bundle, resource, job, menu, double click and selections
	 */
	@Override
	public void dispose() {
		pullDownMenuManager.removeMenuListener(pullDownMenuListener);
		detailsTitleImage.dispose();
		listTitleImage.dispose();
		getSite().getWorkbenchWindow().getSelectionService()
				.removeSelectionListener(BundleMenuActivationHandler.PACKAGE_EXPLORER_ID, this);
		getSite().getWorkbenchWindow().getSelectionService()
				.removeSelectionListener(BundleMenuActivationHandler.PROJECT_EXPLORER_ID, this);
		bundleListPage.removeSelectionChangedListener(bundleListListener);
		bundleListPage.removeDoubleClickListener(bundleDoubleClickListener);
		bundleDetailsPage.removeSelectionChangedListener(bundleDetailsListener);
		bundleDetailsPage.removeDoubleClickListener(bundleDoubleClickListener);
		Activator.getContext().removeBundleListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		Job.getJobManager().removeJobChangeListener(this);
		BundleTransitionListener.removeBundleTransitionListener(this);
		super.dispose();
	}

	IMenuListener pullDownMenuListener = new IMenuListener() {

		/**
		 * Add all menu commands to the menu. Commands enable state is determined before invocation of
		 * this method.
		 * 
		 * @param manager menu manager for pull down menu
		 */
		@Override
		public void menuAboutToShow(IMenuManager manager) {

			try {
				pullDownMenuManager.add(activateAction);
				pullDownMenuManager.add(startStopAction);
				pullDownMenuManager.add(updateAction);
				pullDownMenuManager.add(refreshAction);
				pullDownMenuManager.add(resetAction);
				pullDownMenuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				pullDownMenuManager.add(flipPageAction);
				pullDownMenuManager.add(editManifestAction);
				pullDownMenuManager.add(linkWithAction);
				pullDownMenuManager.add(updateClassPathAction);
				pullDownMenuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			} catch (InPlaceException e) {
				// Menu item not displayed
			}
		}
	};

	IDoubleClickListener bundleDoubleClickListener = new IDoubleClickListener() {

		/**
		 * Open Plug-in Manifest editor
		 */
		@Override
		public void doubleClick(DoubleClickEvent event) {
			editManifestAction.run();
		}
	};

	ISelectionChangedListener bundleListListener = new ISelectionChangedListener() {

		/**
		 * Update tool bar and local pull down menu in list page
		 * 
		 * @param event selection event in list page
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			// Set list page as the current selection provider
			selectionProviderIntermediate.setSelectionProviderDelegate(bundleListPage);
			// Ignore selections from other sources
			if (event.getSource() == bundleListPage) {
				if (!event.getSelection().isEmpty()) {
					updateExplorerSelection(getSelectedProject());
					setSelectedItemIndex(bundleListPage.getTable().getSelectionIndex());
				}
			}
			setEnablement();
		}
	};

	ISelectionChangedListener bundleDetailsListener = new ISelectionChangedListener() {

		/**
		 * Update tool bar and local pull down menu in details page
		 * 
		 * @param event selection event in details page
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			// Set details page as the current selection provider
			selectionProviderIntermediate.setSelectionProviderDelegate(bundleDetailsPage);
			if (!event.getSelection().isEmpty()) {
				setSelectedItemIndex(bundleListPage.getTable().getSelectionIndex());
			}
			setEnablement();
		}
	};

	/**
	 * Listen to selected java projects from other sources. If a project is selected in the package or
	 * project explorer, the project in the current page view is selected
	 * 
	 * @param sourcepart workbench part
	 * @param selection current selection
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part != BundleView.this && linkWithState) {
			if (null != selection && !selection.isEmpty()) {
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					Object object = ss.getFirstElement();
					IProject project = (IProject) Platform.getAdapterManager().getAdapter(object,
							IProject.class);
					if (null != project && project.isAccessible()) {
						// Set selection in list page view to the same project as in part
						try {
							if (Activator.getBundleProjectCandidatesService().isInstallable(project)) {
								selectProject(project, true);
							} else {
								selectProject(project, false);
							}
						} catch (ExtenderException e) {
							StatusManager.getManager().handle(
									new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
									StatusManager.LOG);
							selectProject(project, false);
						}
						if (isDetailsPageActive()) {
							showProject(project);
						}
						// Selection from active part
						setEnablement();
					}
				}
			}
		}
	}

	/**
	 * Update bundle state in bundle page view on external workspace bundles operations and when
	 * classes in bundles with a lazy activation policy are loaded on demand.
	 * <p>
	 * The job event handler updates the bundle view page for internal bundle operations and the
	 * resource change listener for project CRUD operations.
	 * 
	 * @see #done(IJobChangeEvent)
	 * @see #resourceChanged(IResourceChangeEvent)
	 */
	@Override
	public void bundleChanged(BundleEvent event) {

		final Bundle bundle = event.getBundle();
		try {
			BundleRegion bundleRegion = Activator.getBundleRegionService();
			IProject project = bundleRegion.getProject(bundle);
			if (null == project) {
				return; // not a workspace project (jar bundle)
			}
			BundleTransition bundleTransition = Activator.getBundleTransitionService();
			Transition transition = bundleTransition.getTransition(project);
			// Only consider external commands and on demand loading of bundles
			if (transition == Transition.EXTERNAL || (transition == Transition.LAZY_ACTIVATE)) {
				showProjectInfo();
			}
		} catch (ProjectLocationException | ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
		}
	}

	/**
	 * Update detail and list page view for project CRUD operations. Mark projects as removed in
	 * before state when renamed, moved and deleted. This determines which page view to use in the
	 * after state of renamed, moved and deleted projects.
	 * <p>
	 * For all post events update project information in bundle detail and list page view.
	 * 
	 * @see #bundleChanged(BundleEvent)
	 * @see #done(IJobChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		int eventType = event.getType();
		// Before Close, Delete and Rename
		if ((eventType & (IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE)) != 0) {
			final IResource resource = event.getResource();
			if (resource.getType() == IResource.PROJECT) {
				IProject removedProject = resource.getProject();
				try {
					if (Activator.getBundleProjectCandidatesService().isInstallable(removedProject)) {
						markProjectRemoved(removedProject);
					}
				} catch (ExtenderException e) {
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
							StatusManager.LOG);
				}
			}
		} else if ((eventType & (IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD)) != 0) {
			try {
				IWorkbench workbench = Activator.getDefault().getWorkbench();
				if (null != workbench && !workbench.isClosing()) {
					showProjectInfo(0);
				}
			} catch (SWTException e) {
				// Ignore not updating bundle view
			}
		}
	}

	/**
	 * Update content description when building projects
	 */
	@Override
	public void bundleTransitionChanged(BundleTransitionEvent event) {
		final Transition transition = event.getTransition();
		// This transition is only received when the workspace is activated
		if (transition == Transition.BUILD) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("Building bundle project(s) ..."); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * Updates bundle view pages after a bundle job has finished and when bundle projects are built
	 * but not updated (the update on build switch is off). Does not alter the existing selection or
	 * the current page view shown.
	 * 
	 * @see #bundleChanged(BundleEvent)
	 * @see #resourceChanged(IResourceChangeEvent)
	 */
	@Override
	public void done(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD)) {
			// Don't update pages while Eclipse shuts down
			Activator activator = Activator.getDefault();
			if (null == activator) {
				return;
			}
			IWorkbench workbench = activator.getWorkbench();
			if (null == workbench || workbench.isClosing()) {
				return;
			}
			try {
				// Update page when building
				if (!Activator.getCommandOptionsService().isUpdateOnBuild()
						&& Activator.getBundleTransitionService().containsPending(Transition.UPDATE)) {
					showProjectInfo(0);
				}
			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
						StatusManager.LOG);
			}
		} else if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			// Don't update pages while Eclipse shuts down
			IWorkbench workbench = Activator.getDefault().getWorkbench();
			if (null != workbench && workbench.isClosing()) {
				return;
			}
			// Delay update of view for bundle jobs that triggers other bundle jobs
			// to avoid early enablement of the activate/deactivate and reset
			if (!(job instanceof ActivateProject) && !(job instanceof Reset)) {
				showProjectInfo(0);
			}
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("idle"); //$NON-NLS-1$
				}
			});
		}
	}

	@Override
	public void aboutToRun(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setEnablement();
				}
			});
		}
	}

	/**
	 * Display running job name in content bar
	 * 
	 * @param event only act upon bundle jobs
	 */
	@Override
	public void running(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("Running " + job.getName()); //$NON-NLS-1$
				}
			});
		}
	}

	@Override
	public void awake(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("Rescheduled " + job.getName()); //$NON-NLS-1$
				}
			});
		}
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("Scheduled " + job.getName()); //$NON-NLS-1$
				}
			});
		}
	}

	@Override
	public void sleeping(IJobChangeEvent event) {
		final Job job = event.getJob();
		if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setContentDescription("Waiting " + job.getName()); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * Display idle in content bar when no jobs are running
	 * 
	 * @param busy state
	 */
	@Override
	public void showBusy(boolean busy) {
		super.showBusy(busy);
		try {
			if (!Activator.getBundleRegionService().isRegionActivated()) {
				pagebook.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						setContentDescription("Workspace Deactivated"); //$NON-NLS-1$
					}
				});
			} else {
				IJobManager jobMan = Job.getJobManager();
				final Job[] bundleJob = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
				if (bundleJob.length == 0 && !getContentDescription().equals("idle")) { //$NON-NLS-1$
					pagebook.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							setContentDescription("idle"); //$NON-NLS-1$
						}
					});
				}
			}
		} catch (ExtenderException e) {
			// Ignore update state when service is unavailable
		}
	}

	/**
	 * Create local actions used by tool bar buttons and pull down menu
	 * 
	 * @see BundleMenuActivationHandler
	 */
	private void createLocalActions() {

		linkWithAction = new Action(Msg.LINK_WITH_EXPLORERS_GENERAL_LABEL, Action.AS_CHECK_BOX) {
			/**
			 * Enable/Disable linking with package and project explorer
			 */
			@Override
			public void run() {
				linkWithState = isChecked();
				if (linkWithState) {
					updateExplorerSelection(getSelectedProject());
				}
			}
		};

		updateClassPathAction = new Action(Msg.UPDATE_CLASS_PATH_GENERAL_LABEL) {
			/**
			 * Update bin class path if missing
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				if (null != project) {
					try {
						if (!Activator.getBundleProjectMetaService().isDefaultOutputFolder(project)) {
							BundleMenuActivationHandler.updateClassPathHandler(
									Collections.<IProject> singletonList(project), true);
						} else {
							BundleMenuActivationHandler.updateClassPathHandler(
									Collections.<IProject> singletonList(project), false);
						}
					} catch (InPlaceException | ExtenderException e) {
						String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.UPDATE_CLASS_PATH_LABEL);
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e),
								StatusManager.LOG);
					}
				}
			}
		};

		activateAction = new Action(Msg.ACTIVATE_GENERAL_LABEL) {
			/**
			 * Activate or deactivate bundle.
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				if (null != project) {
					try {
						Collection<IProject> projects = Collections.<IProject> singletonList(project);
						if (Activator.getBundleRegionService().isBundleActivated(project)) {
							BundleRegion bundleRegion = Activator.getBundleRegionService();
							Bundle bundle = bundleRegion.getBundle(project);
							if (null != bundle && (bundle.getState() & 
									(Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE)) != 0) {
								BundleMenuActivationHandler.deactivateHandler(projects);
							}
						} else {
							BundleMenuActivationHandler.activateProjectHandler(projects);
						}
					} catch (ExtenderException e) {
						String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.ACTIVATE_GENERAL_LABEL);
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
				}
			}
		};

		resetAction = new Action(Msg.RESET_GENERAL_LABEL) {
			/**
			 * Reset activated bundle
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				if (null != project) {
					try {
						if (Activator.getBundleRegionService().isBundleActivated(project)) {
							BundleMenuActivationHandler.resetHandler(Collections
									.<IProject> singletonList(project));
						}
					} catch (ExtenderException e) {
						String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.RESET_POPUP_LABEL);
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
				}
			}
		};

		updateAction = new Action(Msg.UPDATE_GENERAL_LABEL) {
			/**
			 * Update activated bundle if pending
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				try {
					if (null != project) {
						if (Activator.getBundleRegionService().isBundleActivated(project)) {
							BundleMenuActivationHandler.updateHandler(Collections
									.<IProject> singletonList(project));
						}
					}
				} catch (ExtenderException e) {
					String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.UPDATE_POPUP_LABEL);
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
							StatusManager.LOG);
				}
			}
		};

		refreshAction = new Action(Msg.REFRSH_GENERAL_LABEL) {
			/**
			 * Refresh an activated bundle when number of bundle revisions is greater than one.
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				if (null != project) {
					try {
						BundleRegion bundleRegion = Activator.getBundleRegionService();
						Bundle bundle = bundleRegion.getBundle(project);
						if (bundleRegion.isBundleActivated(project)) {
							if (null != bundle) {
								BundleMenuActivationHandler.refreshHandler(Collections
										.<IProject> singletonList(project));
							}
						}
					} catch (ExtenderException e) {
						String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.REFRESH_POPUP_LABEL);
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e),
								StatusManager.LOG);
					}
				}
			}
		};

		startStopAction = new Action(Msg.START_STOP_GENERAL_LABEL) {
			/**
			 * Start and stop activated bundle
			 */
			@Override
			public void run() {
				IProject project = getSelectedProject();
				if (null != project) {
					try {
						BundleRegion bundleRegion = Activator.getBundleRegionService();
						if (bundleRegion.isBundleActivated(project)) {
							Bundle bundle = bundleRegion.getBundle(project);
							if (bundle != null) {
								Collection<IProject> projects = Collections.<IProject> singletonList(project);
								if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
									BundleMenuActivationHandler.stopHandler(projects);
								} else {
									BundleMenuActivationHandler.startHandler(projects);
								}
							}
						}
					} catch (ExtenderException e) {
						String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.START_STOP_GENERAL_LABEL);
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e),
								StatusManager.LOG);
					}
				}
			}
		};

		flipPageAction = new Action(Msg.FLIP_DETAILS_LABEL) {
			/**
			 * Toggle details and list page
			 */
			@Override
			public void run() {
				IProject project = null;
				try {
					if (isListPageActive()) {
						project = getSelectedProject();
						if (null != project) {
							showProject(project);
						}
					} else {
						BundleProjectCandidates bundleProjectCandidates = Activator
								.getBundleProjectCandidatesService();
						showProjects(
								bundleProjectCandidates.toJavaProjects(bundleProjectCandidates.getInstallable()),
								true);
					}
				} catch (ExtenderException e) {
					String msg = NLS.bind(Msg.ADD_MENU_EXEC_ERROR, Msg.FLIP_LIST_LABEL);
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e),
							StatusManager.LOG);
				}
			}
		};

		editManifestAction = new Action(Msg.OPEN_IN_EDITOR_LABEL) {
			/**
			 * Opens the Plug-in Manifest editor on the selected bundle
			 */
			@Override
			public void run() {

				IProject project = getSelectedProject();
				if (null != project && project.exists() && project.isOpen()) {
					IFile manifestFile = project.getFile(BundleProjectMeta.MANIFEST_RELATIVE_PATH
							+ BundleProjectMeta.MANIFEST_FILE_NAME);
					if (manifestFile.exists() && manifestFile.isAccessible()) {
						// Open in plug-in Manifest editor
						IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry()
								.findEditor("org.eclipse.pde.ui.manifestEditor"); //$NON-NLS-1$
						// Open the manifest file in a text editor
						if (null == editorDesc) {
							editorDesc = PlatformUI.getWorkbench().getEditorRegistry()
									.getDefaultEditor(manifestFile.getName());
						}
						IWorkbenchPage page = getViewSite().getPage();
						if (null != page && null != editorDesc) {
							IEditorInput editorInput = new FileEditorInput(manifestFile);
							try {
								page.openEditor(editorInput, editorDesc.getId());
							} catch (PartInitException e) {
								StatusManager.getManager().handle(
										new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, project,
												Msg.ADD_MENU_EXEC_ERROR, e), StatusManager.LOG);
							}
						}
					} else {
						String msg = NLS.bind(Msg.MANIFEST_ERROR, project.getName());
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, project, msg, null),
								StatusManager.LOG);
					}
				}
			}
		};
	}

	/**
	 * Enable/disable, set label text and tool tip text in local menus and tool-bar for a selected
	 * project. If there is a current selection there is a project to operate on and the UI state is
	 * dependent the state of the selected bundle project.
	 */
	private void setEnablement() {

		Bundle bundle = null;
		BundleProjectCandidates bundleProjectCandidates = null;
		BundleRegion bundleRegion = null;
		IProject project = getSelectedProject();
		try {
			bundleRegion = Activator.getBundleRegionService();
			bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);
			// Disable bundle commands
			project = null;
		}
		// Disable all buttons and menu entries and return when no active project is selected,
		// not a plug-in or not a bundle (should not exist in bundle list) and when a bundle job is
		// running
		if (null == project || isBundleJobRunning() || !bundleProjectCandidates.isInstallable(project)) {
			setUIElement(resetAction, false, Msg.RESET_GENERAL_LABEL, Msg.RESET_GENERAL_LABEL,
					BundleCommandsContributionItems.resetImage);
			setUIElement(startStopAction, false, Msg.START_STOP_GENERAL_LABEL,
					Msg.START_STOP_GENERAL_LABEL, BundleCommandsContributionItems.startImage);
			setUIElement(activateAction, false, Msg.ACTIVATE_GENERAL_LABEL, Msg.ACTIVATE_GENERAL_LABEL,
					BundleCommandsContributionItems.activateImage);
			setUpdateRefresh(false, null);
			if (isBundleJobRunning()) {
				setNonBundleCommandsAction(true, true, true, Msg.FLIP_DETAILS_LABEL,
						Msg.FLIP_DETAILS_LABEL, BundleCommandsContributionItems.bundleListImage);
			} else {
				setNonBundleCommandsAction(false, false, false, Msg.FLIP_DETAILS_LABEL,
						Msg.FLIP_DETAILS_LABEL, BundleCommandsContributionItems.bundleListImage);
			}
			setUIElement(updateClassPathAction, false, Msg.UPDATE_CLASS_PATH_LABEL,
					Msg.UPDATE_CLASS_PATH_LABEL, BundleCommandsContributionItems.classPathImage);
			return;
		}
		// Enable all non bundle commands. Flip page is dependent on the active page
		if (isListPageActive()) {
			setNonBundleCommandsAction(true, true, true, Msg.FLIP_DETAILS_LABEL, Msg.FLIP_DETAILS_LABEL,
					BundleCommandsContributionItems.bundleDetailsImage);
		} else if (isDetailsPageActive()) {
			setNonBundleCommandsAction(true, true, true, Msg.FLIP_LIST_LABEL, Msg.FLIP_LIST_LABEL,
					BundleCommandsContributionItems.bundleListImage);
		}
		// Always possible to activate or deactivate a project
		activateAction.setEnabled(true);
		// Always allow to update the class path for deactivated and activated bundle projects
		setUpdateClassPathAction(project);
		if (bundleRegion.isBundleActivated(project)) {
			// Bundle should be in state installed (if resolve errors), resolved or active/starting
			bundle = Activator.getBundleRegionService().getBundle(project);
			if (null != bundle) {
				// Start and Stop is dependent on the bundle state of the activated bundle
				if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
					setUIElement(startStopAction, true, Msg.STOP_LABEL, Msg.STOP_LABEL,
							BundleCommandsContributionItems.stopImage);
				} else { // bundle in state installed or resolved
					try {
						if (Activator.getBundleProjectMetaService().isFragment(bundle)) {
							setUIElement(startStopAction, false, Msg.START_LABEL, Msg.START_LABEL,
									BundleCommandsContributionItems.startImage);
						} else {
							setUIElement(startStopAction, true, Msg.START_LABEL, Msg.START_LABEL,
									BundleCommandsContributionItems.startImage);
						}
					} catch (ExtenderException e) {
						StatusManager.getManager().handle(
								new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
								StatusManager.LOG);
						setUIElement(startStopAction, false, Msg.START_LABEL, Msg.START_LABEL,
								BundleCommandsContributionItems.startImage);
					}
				}
				// Conditional enabling of update and refresh
				setUpdateRefresh(true, bundle);
			} else {
				// Project is activated but bundle is not yet. Meaning that the bundle has not been
				// installed and resolved/started yet.
				setUIElement(startStopAction, false, Msg.START_LABEL, Msg.START_LABEL,
						BundleCommandsContributionItems.startImage);
				// Conditional enabling of update and refresh
				setUpdateRefresh(false, null);
			}
			// Reset always possible on an activated project
			setUIElement(resetAction, true, Msg.RESET_POPUP_LABEL, Msg.RESET_POPUP_LABEL,
					BundleCommandsContributionItems.resetImage);
			// Always possible to deactivate an activated project
			setUIElement(activateAction, activateAction.isEnabled(), Msg.DEACTIVATE_POPUP_LABEL,
					Msg.DEACTIVATE_POPUP_LABEL, BundleCommandsContributionItems.deactivateImage);
		} else {
			// Enable activate and disable start/stop, refresh, reset and update when project is
			// deactivated
			setUIElement(resetAction, false, Msg.RESET_GENERAL_LABEL, Msg.RESET_GENERAL_LABEL,
					BundleCommandsContributionItems.resetImage);
			setUIElement(startStopAction, false, Msg.START_STOP_GENERAL_LABEL,
					Msg.START_STOP_GENERAL_LABEL, BundleCommandsContributionItems.startImage);
			setUIElement(activateAction, activateAction.isEnabled(), Msg.ACTIVATE_LABEL,
					Msg.ACTIVATE_LABEL, BundleCommandsContributionItems.activateImage);
			setUpdateRefresh(false, null);
		}
	}

	private void setUpdateRefresh(boolean enabled, Bundle bundle) {

		if (enabled && null != bundle) {
			try {
				String updateLabel = Msg.UPDATE_POPUP_LABEL;
				if (Activator.getBundleTransitionService().containsPending(bundle, Transition.UPDATE,
						Boolean.FALSE)) {
					updateLabel = Msg.UPDATE_PENDING_LABEL;
				}
				setUIElement(updateAction, true, updateLabel, updateLabel,
						BundleCommandsContributionItems.updateImage);
			} catch (InPlaceException | ExtenderException e) {
				setUIElement(updateAction, true, Msg.UPDATE_GENERAL_LABEL, Msg.UPDATE_GENERAL_LABEL,
						BundleCommandsContributionItems.updateImage);
			}
			try {
				String refreshLabel = Msg.REFRESH_POPUP_LABEL;
				if (Activator.getBundleCommandService().getBundleRevisions(bundle).size() > 1) {
					refreshLabel = Msg.REFRESH_PENDING_LABEL;
				}
				setUIElement(refreshAction, true, refreshLabel, refreshLabel,
						BundleCommandsContributionItems.refreshImage);
			} catch (InPlaceException | ExtenderException e) {
				setUIElement(refreshAction, false, Msg.REFRSH_GENERAL_LABEL, Msg.REFRSH_GENERAL_LABEL,
						BundleCommandsContributionItems.refreshImage);
			}
		} else {
			setUIElement(updateAction, false, Msg.UPDATE_GENERAL_LABEL, Msg.UPDATE_GENERAL_LABEL,
					BundleCommandsContributionItems.updateImage);
			setUIElement(refreshAction, false, Msg.REFRSH_GENERAL_LABEL, Msg.REFRSH_GENERAL_LABEL,
					BundleCommandsContributionItems.refreshImage);
		}
	}

	private void setNonBundleCommandsAction(boolean editManifestState, boolean linkWithState,
			boolean flipPageState, String flipPageToolTipText, String flipPageText,
			ImageDescriptor flipPageImage) {

		setUIElement(editManifestAction, editManifestState, Msg.OPEN_IN_EDITOR_LABEL,
				Msg.OPEN_IN_EDITOR_GENERAL_LABEL, mfEditorImage);
		setUIElement(linkWithAction, linkWithState, Msg.LINK_WITH_EXPLORERS_LABEL,
				Msg.LINK_WITH_EXPLORERS_GENERAL_LABEL, linkedWithImage);
		setUIElement(flipPageAction, flipPageState, flipPageText, flipPageToolTipText, flipPageImage);
	}

	/**
	 * Enable the update class path action if default output folder is missing in Bundle-ClassPath.
	 * Otherwise disable the action.
	 * 
	 * @param project with the default output location set or unset in Bundle-ClassPath
	 */
	private void setUpdateClassPathAction(IProject project) {
		try {
			updateClassPathAction.setEnabled(true);
			if (!Activator.getBundleProjectMetaService().isDefaultOutputFolder(project)) {
				setUIElement(updateClassPathAction, updateClassPathAction.isEnabled(),
						Msg.ADD_CLASSPATH_POPUP_LABEL, Msg.ADD_CLASSPATH_POPUP_LABEL,
						BundleCommandsContributionItems.classPathImage);
			} else {
				setUIElement(updateClassPathAction, updateClassPathAction.isEnabled(),
						Msg.REMOVE_CLASSPATH_POPUP_LABEL, Msg.REMOVE_CLASSPATH_POPUP_LABEL,
						BundleCommandsContributionItems.classPathImage);
			}
		} catch (InPlaceException | ExtenderException e) {
			updateClassPathAction.setEnabled(false);
		}
	}

	/**
	 * Set the menu text, state, tooltip text and image on the specified action.
	 * 
	 * @param action assign the specified attributes to this action
	 * @param enable state of action
	 * @param menu menu text to display for action
	 * @param toolTip for the action
	 * @param imageDescriptor image used by the action
	 */
	private void setUIElement(Action action, Boolean enable, String menu, String toolTip,
			ImageDescriptor imageDescriptor) {

		action.setEnabled(enable);
		action.setText(menu);
		action.setToolTipText(toolTip);
		action.setImageDescriptor(imageDescriptor);
	}

	/** 
	 * Show project information in list and details page view after the specified delay.
	 * 
	 * @param delay Delay in msec before displaying project info
	 * @see #showProjectInfo()
	 */
	private void showProjectInfo(int delay) {

		if (delay != 0) {
			Job job = new Job("") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {

					showProjectInfo();
					return new Status(Status.OK, Activator.PLUGIN_ID, "");
				}
			};
			job.setUser(false);
			job.schedule(delay);
		} else {
			showProjectInfo();
		}			
	 }

	/**
	 * Show project information in list and details page view. For renamed, deleted and moved projects
	 * switch to list page view if the project as a resource has changed as a consequence of one of
	 * these operations.
	 * <p>
	 * Otherwise update the project information in the current active page view.
	 * <p>
	 * Runs in the user interface thread.
	 * 
	 * @see IResource#equals(Object)
	 */
	private void showProjectInfo() {

		if (null != pagebook) {
			pagebook.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					// Current bundle page view
					boolean showListPage = (isDetailsPageActive()) ? false : true;
					boolean selection = true;
					// Has project been removed, closed or renamed
					IProject removedProject = getRemovedProject();
					if (null != removedProject) {
						IProject activeProject = getSelectedProject();
						// If the project is shown in the detail page, switch to list page view
						// Moved projects are unequal (different path) and renamed and deleted projects are
						// equal
						if (null != activeProject && activeProject.equals(removedProject)) {
							showListPage = true;
							selection = false;
						}
					}
					if (!showListPage && isDetailsPageActive()) {
						final IProject project = getDetailsProject();
						if (null != project && !project.isAccessible()) {
							markProjectRemoved(project);
						}
						showProject(project);
					} else { // Update list page
						try {
							BundleProjectCandidates bundleProjectCandidates = Activator
									.getBundleProjectCandidatesService();
							Collection<IJavaProject> projects = bundleProjectCandidates
									.toJavaProjects(bundleProjectCandidates.getInstallable());
							showProjects(projects, selection);
						} catch (ExtenderException e) {
							StatusManager.getManager().handle(
									new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
									StatusManager.LOG);
							selection = false;
						}
						// Deselect
						if (!selection) {
							selectProject(getSelectedProject(), selection);
						}
					}
				}
			});
		}
	}

	/**
	 * Show projects in the list page and set selection to current project in details page if any.
	 * 
	 * @param javaProjects projects to show
	 * @param setSelection true if the current project selection should be retained, at a possible new
	 * row
	 */
	public void showProjects(Collection<IJavaProject> javaProjects, Boolean setSelection) {

		if (null == javaProjects) {
			return;
		}
		if (listTitleImage == null || listTitleImage.isDisposed()) {
			return;
		}
		setPartName(Msg.BUNDLE_LIST_PAGE_CAPTION_TITLE_LABEL);
		setTitleImage(listTitleImage);
		IProject removedProject = getRemovedProject();
		if (null != removedProject) {
			markProjectRemoved(null);
			if (removedProject.isAccessible()) {
				try {
					if (removedProject.isNatureEnabled(JavaCore.NATURE_ID)) {
						IJavaProject javaProject = JavaCore.create(removedProject);
						javaProjects.remove(javaProject);
					}
				} catch (CoreException e) {
					markProjectRemoved(null);
				}
			}
		}
		IProject selectedProject = null;
		if (setSelection) {
			selectedProject = getSelectedProject();
		}
		bundleListPage.setInput(javaProjects);
		if (setSelection && null != selectedProject) {
			selectProject(selectedProject, true);
			setSelectedItemIndex(bundleListPage.getTable().getSelectionIndex());
		}
		setEnablement();
		pagebook.showPage(bundleListPage.getControl());
		updatePropertyPage();
	}

	/**
	 * Show project in details page
	 * 
	 * @param project to show
	 */
	public void showProject(IProject project) {

		if (null != project) {
			setPartName(Msg.BUNDLE_DETAILS_PAGE_CAPTION_TITLE_LABEL);
			setTitleImage(detailsTitleImage);
			IProject removedProject = getRemovedProject();
			if (null != removedProject && removedProject.equals(project)) {
				return;
			}
			if (project.isAccessible()) {
				bundleDetailsPage.setInput(project);
				pagebook.showPage(bundleDetailsPage.getControl());
				setEnablement();
			}
		} else {
			pagebook.showPage(bundleDetailsPage.getControl());
			setEnablement();
		}
		updatePropertyPage();
	}

	/**
	 * Check if the bundle list page is active. A list page is active if the details page is not
	 * active and there is a selection.
	 * 
	 * @return true if active otherwise false
	 */
	public Boolean isListPageActive() {
		if (null == getDetailsProject() && null != getSelectedProject()) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the bundle details page is active. A page is active if the input on the page is set.
	 * 
	 * @return true if active otherwise false
	 */
	public Boolean isDetailsPageActive() {
		if (null != getDetailsProject()) {
			return true;
		}
		return false;
	}

	/**
	 * The item to select next time the list page is shown
	 * 
	 * @return the selected item in list page
	 */
	@SuppressWarnings("unused")
	private int getSelectedItemIndex() {
		// TODO Why am I not calling this method?
		return selectIndex;
	}

	/**
	 * This index will be used as the selection index the next time the bundle view is shown.
	 * 
	 * @param selectedItem in list page
	 * @see #selectProject(IProject, Boolean)
	 * @see #getSelectedItemIndex()
	 */
	private void setSelectedItemIndex(int index) {
		this.selectIndex = index;
	}

	/**
	 * Sets the specified project in the list page view as the active selection.
	 * 
	 * @param project to be set as the active selection
	 * @param select if true set selection to the specified project if false deselect the project or
	 * if the project is not found, does not exist or is not accessible deselect all rows
	 * @return true if the project existed in the list page view
	 */
	private Boolean selectProject(IProject project, Boolean select) {
		boolean found = false;

		if (null == project || !project.isAccessible()) {
			if (!select) {
				bundleListPage.getTable().deselectAll();
				setSelectedItemIndex(-1);
			}
			return found;
		}
		TableItem[] ti = bundleListPage.getTable().getItems();
		for (int i = 0; i < ti.length && !found; i++) {
			BundleProperties bp = (BundleProperties) ti[i].getData();
			if (bp.getProject().equals(project)) {
				if (select) {
					bundleListPage.getTable().setSelection(ti[i]);
					setSelectedItemIndex(i);
					bundleListPage.getTable().showSelection();
				} else {
					bundleListPage.getTable().deselect(i);
					setSelectedItemIndex(-1);
				}
				found = true;
			}
		}
		// Not found
		if (!found && !select) {
			bundleListPage.getTable().deselectAll();
		}
		return found;
	}

	/**
	 * Get the current project in the details page.
	 * 
	 * @return project if the details page is active or null if the details page is inactive
	 */
	private IProject getDetailsProject() {
		return bundleContentProvider.getProject();
	}

	/**
	 * Get the project of the active selection.
	 * <p>
	 * Note that if the details page is the active page, the project displayed in the details page is
	 * at the same time the selected project in the list page.
	 * 
	 * @return project of the active selection or null if no selection
	 * @see #getDetailsProject()
	 * @see BundleView#getGlobalSelectedProject(ISelection)
	 */
	private IProject getSelectedProject() {
		try {
			if (!bundleListPage.getTable().isDisposed()) {
				int index = bundleListPage.getTable().getSelectionIndex();
				if (index >= 0) {
					TableItem[] ti = bundleListPage.getTable().getItems();
					BundleProperties bp = (BundleProperties) ti[index].getData();
					return bp.getProject();
				}
			}
		} catch (SWTException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
							Msg.THREAD_INVALID_ACCESS_EXCEPTION, e), StatusManager.LOG);
		}
		return null;
	}

	/**
	 * Finds the project belonging to a selection in bundle view, package explorer or project
	 * explorer. The specified selection is based on the project and bundle properties model objects
	 * and may be null
	 * 
	 * @param selection a selected row
	 * @return selected project or null if no selection
	 */
	@SuppressWarnings("unused")
	private IProject getGlobalSelectedProject(ISelection selection) {
		IProject project = null;
		if (null != selection && !selection.isEmpty()) {
			project = BundleMenuActivationHandler.getSelectedProject(selection);
		}
		if (null == project) {
			project = BundleMenuActivationHandler.getSelectedProject();
		}
		return project;
	}

	@Override
	public void setFocus() {
		pagebook.setFocus();
	}

	/**
	 * Get a project that has been marked as removed.
	 * 
	 * @return an invalid java project or null if no projects have been marked as invalid
	 */
	private IProject getRemovedProject() {
		return removedProject;
	}

	/**
	 * Mark a java project as removed. A project is typically marked as invalid in it's before state
	 * (pre_close, pre_delete) when it is deleted, closed, renamed or moved and cleared in its after
	 * state (post_change, post_build).
	 * 
	 * @param removedProject java project to mark as invalid or null to clear any invalid project
	 */
	private void markProjectRemoved(IProject removedProject) {
		this.removedProject = removedProject;
	}

	/**
	 * Updates selection in project and package explorer to the specified project
	 * 
	 * @param project to select in explorer
	 */
	private void updateExplorerSelection(IProject project) {
		// TODO Package and project explorer does not adapt. Am I missing something?
		if (null != project && linkWithState) {
			IWorkbenchPartSite workbenchPartSite = null;
			ISelectionProvider selProvider = null;
			try {
				packageExplorer = ViewUtil.get(BundleMenuActivationHandler.PACKAGE_EXPLORER_ID);
				if (null != packageExplorer) {
					BundleProjectCandidates bundleProjectCandidates = Activator
							.getBundleProjectCandidatesService();
					IJavaProject jp = bundleProjectCandidates.getJavaProject(project);
					IStructuredSelection jpSelection = new StructuredSelection(jp);
					workbenchPartSite = packageExplorer.getSite();
					if (null != workbenchPartSite) {
						selProvider = workbenchPartSite.getSelectionProvider();
						if (null != selProvider) {
							selProvider.setSelection(jpSelection);
						}
					}
				}
				projectExplorer = ViewUtil.get(BundleMenuActivationHandler.PROJECT_EXPLORER_ID);
				if (null != projectExplorer) {
					IStructuredSelection pSelection = new StructuredSelection(project);
					workbenchPartSite = projectExplorer.getSite();
					if (null != workbenchPartSite)
						selProvider = workbenchPartSite.getSelectionProvider();
					if (null != selProvider) {
						selProvider.setSelection(pSelection);
					}
				}
			} catch (InPlaceException | ExtenderException e) {
				// Explorer not updated
			}
		}
	}

	/**
	 * If called from UI thread, refreshes property page from the {@link BundlePropertiesSource} model
	 * setting the selection to the current selected project.
	 */
	public void updatePropertyPage() {

		IProject project = getSelectedProject();
		if (null != project && null != bundlePropertySheetPage) {
			try {
				BundlePropertiesSource bundlePropertiesSource = (BundlePropertiesSource) Platform
						.getAdapterManager().getAdapter(new BundleProperties(project), IPropertySource.class);
				bundlePropertySheetPage.selectionChanged(this, new StructuredSelection(
						bundlePropertiesSource));
			} catch (ExtenderException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
								Msg.PROPERTY_PAGE_NOT_UPDATED_EXCEPTION, e), StatusManager.LOG);
			}
		}
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {

		if (key.equals(IPropertySheetPage.class)) {
			return getPropertySheetPage();
		}
		return super.getAdapter(key);
	}

	/**
	 * This accesses a cached version of the property sheet.
	 */
	public IPropertySheetPage getPropertySheetPage() {

		if (bundlePropertySheetPage == null) {
			bundlePropertySheetPage = new BundlePropertySheetPage();
		}
		return bundlePropertySheetPage;
	}

	private class BundlePropertySheetPage extends PropertySheetPage {

		/*
		 * Preserve the initial order of properties
		 */
		public class BundlePropertySheetSorter extends PropertySheetSorter {

			@Override
			public int compareCategories(String categoryA, String categoryB) {
				// return super.compareCategories(categoryA, categoryB);
				return 0;
			}

			@Override
			public int compare(IPropertySheetEntry entryA, IPropertySheetEntry entryB) {
				// return super.compare(entryA, entryB);
				return 0;
			}

			@Override
			public void sort(IPropertySheetEntry[] entries) {
			}
		}

		public BundlePropertySheetPage() {
			setSorter(new BundlePropertySheetSorter());
		}

		@Override
		protected void setSorter(PropertySheetSorter sorter) {
			if (!(sorter instanceof BundlePropertySheetSorter)) {
				sorter = new BundlePropertySheetSorter();
			}
			super.setSorter(sorter);
		}
	}

	/**
	 * Filter out contributions from other plug-ins
	 * 
	 * @param items list of contributions to filter
	 * @return list of contributions from Activator plug-ins
	 */
	private IContributionItem[] filterItems(IContributionItem[] items) {
		List<IContributionItem> filteredItems = new ArrayList<IContributionItem>();
		for (IContributionItem item : items) {
			if (item != null && item.getId() != null && item.getId().startsWith("no.javatime.inplace")) {
				filteredItems.add(item);
			}
		}
		items = new IContributionItem[filteredItems.size()];
		return filteredItems.toArray(items);
	}

	/**
	 * Inspect the state of a bundle job
	 * 
	 * @return true if there is a bundle job running, sleeping or waiting. Otherwise false
	 */
	private Boolean isBundleJobRunning() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
		if (jobs.length > 0) {
			return true;
		} else {
			return false;
		}
	}
}
