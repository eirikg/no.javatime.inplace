package no.javatime.inplace.builder;

import java.util.Collection;
import java.util.HashSet;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.intface.SaveOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

public class SaveOptionsJob extends BundleJob implements SaveOptions {

	private boolean isDisableSaveFiles;

	/**
	 * Default constructor wit a default job name
	 */
	public SaveOptionsJob() {
		super(Msg.SAVE_OPTIONS_JOB);
		init();
	}

	/**
	 * Constructs a save job with a given job name
	 * 
	 * @param name job name
	 */
	public SaveOptionsJob(String name) {
		super(name);
		init();
	}
	
	private void init() {
		isDisableSaveFiles = false;
		setSaveWorkspaceSnaphot(false);
	}
	
	/**
	 * Runs the bundle(s) save operation.
	 * 
	 * @return A bundle status object obtained from {@link #getJobSatus()}
	 */
	@Override
	public IBundleStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {

		try {
			startTime = System.currentTimeMillis();
			saveFiles();
		} catch (OperationCanceledException e) {
			addCancel(e, NLS.bind(Msg.CANCEL_JOB_INFO, getName()));
		} catch (InPlaceException | ExtenderException e) {
			String msg = ExceptionMessage.getInstance().formatString("terminate_job_with_errors",
					getName());
			addError(e, msg);
		} catch (Exception e) {
			String msg = ExceptionMessage.getInstance().formatString("exception_job", getName());
			addError(e, msg);
		} finally {
			monitor.done();
		}
		return getJobSatus();
	}
	
	@Override
	public void resetPendingProjects(Collection<IProject> projects) {

		super.resetPendingProjects(projects);
	}	

	@Override
	public void disableSaveFiles(boolean disable) {
		isDisableSaveFiles = disable;
	}
	
	@Override
	public boolean isSaveFiles() throws ExtenderException {

		if (isDisableSaveFiles) {
			return !isDisableSaveFiles;
		}
		boolean saveFiles = false;
		if (null == commandOptions) {
			commandOptions = Activator.getCommandOptionsService();
		}
		resetPendingProjects(getDirtyProjects());
		// If no files to save, ignore the save bundle operation option
		saveFiles = commandOptions.isSaveFilesBeforeBundleOperation() && hasPendingProjects() ? true : false;
		return saveFiles;
	}

	@Override
	public boolean isTriggerUpdate() throws ExtenderException {

		if (isSaveFiles()) {
			initServices();
			boolean activated = false;
			// Must save activated projects to trigger an update
			for (IProject project : getPendingProjects()) {
				if (bundleRegion.isBundleActivated(project)) {
					activated = true;
					break;
				}
			}	
			if (activated 
					&& bundleProjectCandidates.isAutoBuilding() 
					&& (commandOptions.isUpdateOnBuild()
							|| bundleTransition.containsPending(Transition.UPDATE_ON_ACTIVATE))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public SaveOptions getSaveOptions() throws ExtenderException {
		return this;
	}
	
	@Override
	public IBundleStatus saveFiles() throws ExtenderException {

		IWorkbench workbench = Activator.getDefault().getWorkbench();
		if (null == workbench || workbench.isClosing()) {
			return getJobSatus();			
		}
		if (isSaveFiles()) {
			if (null == messageOptions) {
				messageOptions = Activator.getMessageOptionsService();
			}
			Activator.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {							
						if (!PlatformUI.getWorkbench().saveAllEditors(false)) {
							addError(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID,
									Msg.SAVE_FILES_OPTION_ERROR));
						} else {
							if (messageOptions.isBundleOperations()) {
								for (IProject project : getPendingProjects()) {
									addLogStatus(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, NLS.bind(
											Msg.SAVE_PROJECT_FILES_INFO, project.getName())));
								}
							}
						}
					} finally {
						clearPendingProjects();
					}
				}						
			});
		}
		return getJobSatus();
	}
	
	public static Collection<IProject> getDirtyProjects() throws ExtenderException {
		
		Collection<IProject> projects = Activator.getBundleProjectCandidatesService().getProjects();	
		return getScopedDirtyProjects(projects);
	}
		
	public static Collection<IProject> getScopedDirtyProjects(final Collection<IProject> projects) {

		final HashSet<IProject> dirtyProjects = new HashSet<>();

		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (int l = 0; l < windows.length; l++) {
					IWorkbenchPage[] pages = windows[l].getPages();
					for (int i = 0; i < pages.length; i++) {
						IEditorPart[] eparts = pages[i].getDirtyEditors();
						for (int j = 0; j < eparts.length; j++) {
							IResource resource = (IResource) eparts[j].getEditorInput().getAdapter(IResource.class);
							if (resource != null) {
								IProject project = resource.getProject();
								if (projects.contains(project)) {
									dirtyProjects.add(project);
								}
							}
						}
					}
				}
			}						
		});
		return dirtyProjects;
	}

	public static Collection<IResource> getScopedDirtyMetaFiles(final Collection<IProject> projects, final boolean includeProjectMetaFiles) {

		final HashSet<IResource> dirtyResources = new HashSet<>();
		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (int l = 0; l < windows.length; l++) {
					IWorkbenchPage[] pages = windows[l].getPages();
					for (int i = 0; i < pages.length; i++) {
						IEditorPart[] eparts = pages[i].getDirtyEditors();
						for (int j = 0; j < eparts.length; j++) {
							IResource resource = (IResource) eparts[j].getEditorInput().getAdapter(IResource.class);
							if (resource != null) {
								IProject project = resource.getProject();
								if (projects.contains(project)) {
									IFile manifestFile = project.getFile(BundleProjectMeta.MANIFEST_RELATIVE_PATH
											+ BundleProjectMeta.MANIFEST_FILE_NAME);
									if (manifestFile.exists() && manifestFile.isAccessible()) {
										if (resource.getName().equals(manifestFile.getName())) {
											dirtyResources.add(resource);									
										}
									}
									if (includeProjectMetaFiles) {
										IFile projectFile = project.getFile(BundleProjectMeta.PROJECT_META_FILE_NAME);
										if (projectFile.exists() && projectFile.isAccessible()) {
											if (resource.getName().equals(projectFile.getName())) {
												dirtyResources.add(resource);
											}
										}
									}
								}
							}
						}
					}
				}
			}						
		});
		return dirtyResources;
	}

	/**
	 * Causes all editors to save any modified resources in the provided collection
	 * of projects depending on the user's preference.
	 * 
	 * @param projects The projects in which to save editors, or <code>null</code>
	 * to save editors in all projects.
	 * NOTE Using save all editors from the Workbench API
	 */
	@SuppressWarnings("unused")
	private void saveEditors(Collection<IProject> projects) throws ExtenderException {
		
		if (!Activator.getCommandOptionsService().isSaveFilesBeforeBundleOperation()) {
			return;
		}
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				IWorkbenchPage page = pages[j];
				if (projects == null) {
					page.saveAllEditors(false);
				} else {
					IEditorPart[] editors = page.getDirtyEditors();
					for (int k = 0; k < editors.length; k++) {
						IEditorPart editor = editors[k];
						IFile inputFile = ResourceUtil.getFile(editor.getEditorInput());
						if (inputFile != null) {
							if (projects.contains(inputFile.getProject())) {
								page.saveEditor(editor, false);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void saveWorkspace(IProgressMonitor monitor)  {

		SaveSnapShotOption ss = new SaveSnapShotOption();
		ss.saveWorkspace(monitor);
	}
}
