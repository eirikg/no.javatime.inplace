package no.javatime.inplace.region.project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleProject;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.ui.statushandlers.StatusManager;

public class BundleProjectStateImpl {

	public static Boolean isFragment(IProject project) throws InPlaceException {
	
		IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(project);
		IHostDescription host = bundleProjDesc.getHost();
		return null != host ? true : false;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#getProjects()
	 */
	public Collection<IProject> getProjects() {
		return Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#getJavaProject(java.lang.String)
	 */
	public IJavaProject getJavaProject(String projectName) throws InPlaceException{
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (null == project) {
			return null;
		}
		return getJavaProject(project);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#getJavaProject(org.eclipse.core.resources.IProject)
	 */
	public IJavaProject getJavaProject(IProject project) throws InPlaceException {
		IJavaProject javaProject = null;
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				javaProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
			throw new InPlaceException("project_not_accessible", e);
		}
		return javaProject;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#formatProjectList(java.util.Collection)
	 */
	public String formatProjectList(Collection<IProject> projects) {
		StringBuffer sb = new StringBuffer();
		if (null != projects && projects.size() >= 1) {
			for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
				IProject project = iterator.next();
				sb.append(project.getName());
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#getProjectFromLocation(java.lang.String, java.lang.String)
	 */
	public IProject getProjectFromLocation(String location, String locationScheme) {
	
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
	
		for (IProject project : root.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID) && project.hasNature(BundleProject.PLUGIN_NATURE_ID) && project.isOpen()) {
					URL pLoc = new URL(WorkspaceRegionImpl.INSTANCE.getProjectLocationIdentifier(project, locationScheme));
					URL bLoc = new URL(location);
					if (Category.DEBUG) {
						TraceMessage.getInstance().getString("display", "Project location: 	" + pLoc.getPath());
						TraceMessage.getInstance().getString("display", "Bundle  location: 	" + bLoc.getPath());
					}
					if (pLoc.getPath().equalsIgnoreCase(bLoc.getPath())) {
						return project;
					}
				}
			} catch (ProjectLocationException e) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e),
						StatusManager.LOG);
			} catch (CoreException e) {
				// Ignore closed or non-existing project
				String msg = NLS.bind(Msg.PROJECT_MISSING_AT_LOC_WARN, location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			} catch (MalformedURLException e) {
				String msg = ExceptionMessage.getInstance()
						.formatString("project_location_malformed_error", location);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg, e),
						StatusManager.LOG);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#toProjects(java.util.Collection)
	 */
	public Collection<IProject> toProjects(Collection<IJavaProject> javaProjects) {
	
		Collection<IProject> projects = new ArrayList<IProject>();
		for (IJavaProject javaProject : javaProjects) {
			IProject project = javaProject.getProject();
			if (null != project) {
				projects.add(project);
			}
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#toJavaProjects(java.util.Collection)
	 */
	public Collection<IJavaProject> toJavaProjects(Collection<IProject> projects) {
	
		Collection<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		for (IProject project : projects) {
			IJavaProject javaProject = JavaCore.create(project);
			if (null != javaProject) {
				javaProjects.add(javaProject);
			}
		}
		return javaProjects;
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.region.project.BundleProjectState#getProject(java.lang.String)
	 */
	public IProject getProject(String name) {
		if (null == name) {
			throw new InPlaceException("project_null");
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject p = root.getProject(name);
		return p;
	}
}
