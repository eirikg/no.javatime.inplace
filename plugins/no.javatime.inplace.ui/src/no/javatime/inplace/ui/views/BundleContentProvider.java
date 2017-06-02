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

import java.util.Collection;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.ErrorMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Input to the content provider is a single bundle project or a collection of bundle projects. The collection
 * only accept plug-ins and OSGi bundle projects, while a single project may be any kind of project. When input is a plug-in
 * project or an OSGi bundle project, bundle property values are available.
 * <p>
 * Input is converted to one {@link BundleProperties} model object for each java project in the collection.
 * <p>
 * For a single project one {@link BundleProperties} model object is created for each property of the bundle project.
 * Each property name and property value pair in a single project is accessed by using {@link BundleProperties#getName()}
 * and {@link BundleProperties#getValue()} respectively. The bundle properties are bundle name, project name, bundle id,
 * activation status, state, providing capabilities, requiring capabilities, services in use, number of revisions,
 * activation policy, build status, UI-extension enabled status, location and last installed or updated.
 * <p>
 * The properties objects are returned by the {@link #getElements(Object)} method where {@code Object} parameter is a collection
 * of projects or a single project.
 * @see BundleProperties
 */
class BundleContentProvider implements IStructuredContentProvider {
	
	private IJavaProject javaProject = null;
	private IProject project = null;

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	@Override
	public void dispose() {
	}
	
	/**
	 * Input element <code>parent<code\> must either be an <code>IProject<code\> or a 
	 * <code>Collection<code\> of type <code>IJavaProject<code\>
	 */
	@Override
	public Object[] getElements(Object parent) {
		javaProject = null;
		project = null;
		try {
			if (parent instanceof IProject) {
				project = (IProject) parent;
				if (project.isNatureEnabled(JavaCore.NATURE_ID)) {				
					javaProject = JavaCore.create(project);
					return getBundleProperties(javaProject);
				}
			} else if (parent instanceof IJavaProject) {
				javaProject = (IJavaProject) parent;
				project = javaProject.getProject();
				return getBundleProperties(javaProject);
			} else if (isCollectionOfJavaProject(parent)) {
				@SuppressWarnings("unchecked")
				Collection<IJavaProject> projects = (Collection<IJavaProject>) parent;
				return getBundleProperties(projects);
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, null, e),
					StatusManager.LOG);
		} catch (CoreException e) {					
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, null, e),
					StatusManager.LOG);
		}
		return new BundleProperties[0];
	}
	
	/**
	 * Check if the specified parameter is a <code>Collection<code\> of type <code>IJavaProject<code\>
	 * @param objectType to check for the specified type
	 * @return true if the specified parameter is a <code>Collection<code\> of type <code>IJavaProject<code\> 
	 */
	private Boolean isCollectionOfJavaProject(Object objectType) {
		if (objectType instanceof Collection<?>) {
			for (Object o : (Collection<?>) objectType) {
				if (o instanceof IJavaProject) {
					return true;
				}
			}	
		}
		return false;
	}
	
	/**
	 * Create a set of bundle properties. One for each java project in the specified collection
	 * 
	 * @param javaProjects a collection of java projects
	 * @return a collection of bundle properties objects or an empty collection.
	 */
	private BundleProperties[] getBundleProperties(Collection<IJavaProject> javaProjects) {
		try {
			BundleProperties[] bundleProperties = new BundleProperties[javaProjects.size()];
			IJavaProject[] javaProject = javaProjects.toArray(new IJavaProject[javaProjects.size()]);
			for (int i = 0; i < javaProject.length; i++) {
				bundleProperties[i] = new BundleProperties(javaProject[i]);			
			}
			return bundleProperties;
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.PROPERTY_PAGE_NOT_UPDATED_EXCEPTION,e), StatusManager.LOG);
		}	
		return new BundleProperties[0];
	}
	
	/**
	 * Create a set of properties from the specified java project.
	 *  
	 * @param javaProject a project with the java nature
	 * @return a set of properties for the specified java project
	 */
	private BundleProperties[] getBundleProperties(IJavaProject javaProject) {

		String locationIdentifier = null;
		BundleProperties bundleProps = null;
		BundleProjectCandidates bundleProjectCandidates = null;
		Boolean uiExtensions = false;
		try {
			try {
				bundleProps = new BundleProperties(javaProject);
				bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
				IProject project = bundleProps.getProject();
				try {
					uiExtensions = bundleProjectCandidates.isUIPlugin(project);
				} catch (InPlaceException e) {
				}
				locationIdentifier = bundleProps.getBundleLocationIdentifier();
			} catch (ProjectLocationException e) {
				String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());			
				IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,e);
				msg  = NLS.bind(Msg.REFRESH_HINT_INFO, project.getName()); 
				status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
				StatusManager.getManager().handle(status, StatusManager.LOG);
				locationIdentifier = e.getMessage();
			} catch (SecurityException e) {
				String msg = ErrorMessage.getInstance().formatString("project_location", project.getName());			
				StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
						StatusManager.LOG);
				locationIdentifier = e.getMessage();
			}
			Boolean installeable = bundleProjectCandidates.isInstallable(project);
			if (!installeable && !uiExtensions) {  // Not a plug-in project
				return new BundleProperties[] {
						new BundleProperties(project, BundleProperties.projectLabelName, bundleProps.getProjectName()),
						new BundleProperties(project, BundleProperties.bundleStatusLabelName, bundleProps.getBundleStatus()),
						new BundleProperties(project, BundleProperties.locationLabelName, locationIdentifier),
						new BundleProperties(project, BundleProperties.lastModifiedLabelName, bundleProps.getModifiedDate())					
				};				
			}
			return new BundleProperties[] {

					new BundleProperties(project, BundleProperties.bundleIdLabelName, bundleProps.getBundleId()),
					new BundleProperties(project, BundleProperties.bundleSymbolicNameLabelName, bundleProps.getSymbolicName()),
					new BundleProperties(project, BundleProperties.bundleVersionLabelName, bundleProps.getBundleVersion()),
					new BundleProperties(project, BundleProperties.projectLabelName, bundleProps.getProjectName()),
					new BundleProperties(project, BundleProperties.locationLabelName, locationIdentifier),
					new BundleProperties(project, BundleProperties.activationStatusLabelName, bundleProps.getActivationMode()),
					new BundleProperties(project, BundleProperties.bundleStateLabelName, bundleProps.getBundleState()),
					new BundleProperties(project, BundleProperties.lastTransitionLabelName, bundleProps.getLastTransition()),
					new BundleProperties(project, BundleProperties.bundleStatusLabelName, bundleProps.getBundleStatus()),
					new BundleProperties(project, bundleProps.getAllRequiringLabelName(), bundleProps.getDeclaredRequiringBundleProjects()),
					new BundleProperties(project, bundleProps.getAllProvidingLabelName(), bundleProps.getAllProvidingBundleProjects()),
					new BundleProperties(project, bundleProps.getResolvedRequiringLabelName(), bundleProps.getResolvedRequiringBundleProjects()),
					new BundleProperties(project, bundleProps.getResolvedProvidingLabelName(), bundleProps.getResolvedProvidingBundleProjects()),
					new BundleProperties(project, BundleProperties.servicesInUseLabelName, bundleProps.getServicesInUse()),
					new BundleProperties(project, BundleProperties.numberOfRevisionsLabelName, bundleProps.getBundleRevisions()),
					new BundleProperties(project, BundleProperties.activationPolicyLabelName, bundleProps.getActivationPolicy()),					
					new BundleProperties(project, BundleProperties.UIExtensionsLabelName, bundleProps.getUIExtension()),
					new BundleProperties(project, BundleProperties. lastInstalledOrUpdatedLabelName, bundleProps.getLastInstalledOrUpdated()),					
			};		
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.PROPERTY_PAGE_NOT_UPDATED_EXCEPTION,e), StatusManager.LOG);
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getLocalizedMessage(),e),
					StatusManager.LOG);
		}
		return new BundleProperties[0];
	}

	/**
	 * @return a project with the java nature
	 */
	public final IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * @return a general project
	 */
	public final IProject getProject() {
		return project;
	}
	
}
