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
package no.javatime.inplace.region.status;

import java.util.Collection;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.osgi.framework.Bundle;

/**
 * Bundle status object containing status codes, exceptions and messages associated with a bundle project.
 */
public class BundleStatus extends MultiStatus implements IBundleStatus {

	/** Overrules the {@code IStatus} status codes */
	private StatusCode statusCode;
	private IProject project;
	private Bundle bundle;
	private int bundleState = 0;
	private Transition bundleTransition = Transition.NO_TRANSITION;;
	

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @see #convertSeverity()
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, String message) {
		super(pluginId, IStatus.OK, message, null);
		init(statusCode, null, null);
	}
	
	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned to a
	 * {@code BundleStatus} object. The {@IStatus] status code is converted to a {@code StatusCode}
	 * 
	 * @param status object to convert to a {@BundleStatus} object
	 * @see #converToStatusCode()
	 */
	public BundleStatus(IStatus status) {
		super(status.getPlugin(), status.getSeverity(), status.getMessage(), status.getException());
		if (status instanceof IBundleStatus) {
			IBundleStatus bundleStatus = (BundleStatus) status;
			init(bundleStatus.getStatusCode(), bundleStatus.getBundle(), bundleStatus.getProject());			
		} else {
			setSeverity(status.getSeverity());
			init(converToStatusCode(), null, null);
		}
	}

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param project associated with this status object
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertSeverity()
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, IProject project, String message,
			Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, null, project); 
	}
	
	public BundleStatus(StatusCode statusCode, Bundle bundle, IProject project, String message,
			Throwable exception) {
		super(Activator.PLUGIN_ID, IStatus.OK, message, exception);
		init(statusCode, bundle, project);
	}
		
	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param bundle associated with this status object
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertSeverity()
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, Long bundleId, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, Activator.getContext().getBundle(bundleId), null);
	}

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param bundle associated with this status object
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertSeverity()
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, Bundle bundle, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, bundle, null);
	}

	@Override
	public void setException(Throwable exception) {
		super.setException(exception);
	}	
	
	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertSeverity()
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, null, null);
	}

	private void init(StatusCode statusCode, Bundle bundle, IProject project) {
		String symbolicName = null;
		// Must have a symbolic name without throwing any exceptions
		if (null != bundle) {
			symbolicName = bundle.getSymbolicName();
			this.bundle = bundle;
			bundleState = bundle.getState();
			bundleTransition = BundleTransitionImpl.INSTANCE.getTransition(bundle);
		} else if (null != project) {
			bundle = WorkspaceRegionImpl.INSTANCE.getBundle(project);
			if (null != bundle) {
				symbolicName = bundle.getSymbolicName();
				this.bundle = bundle;
				bundleState = bundle.getState();
				bundleTransition = BundleTransitionImpl.INSTANCE.getTransition(bundle);
			} else {
				try {
					BundleProjectMeta bundleProjectMeta = BundleProjectMetaImpl.INSTANCE;
					symbolicName = bundleProjectMeta.getSymbolicName(project);
				} catch (InPlaceException e) {
				}
			}
		} else {
			// Worst case. Symbolic name is not always the same as the plug-in id
			if (null == symbolicName) {
					symbolicName = getPlugin();
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IWorkspaceRoot root = workspace.getRoot();
					for (IProject bundleProject : root.getProjects()) {
						try {
							BundleProjectMeta bundleProjectMeta = BundleProjectMetaImpl.INSTANCE;
							String pdSymbolicName = bundleProjectMeta.getSymbolicName(project);
							if (null != pdSymbolicName && pdSymbolicName.equals(symbolicName)) {
								// Drop comparison with version. Use the first one available
								project = bundleProject;
								break;
							}
						} catch (InPlaceException e) {
						}
					}
					if (null != project) {
						try {
							bundle = WorkspaceRegionImpl.INSTANCE.getBundle(project);
							if (null != bundle) {
								symbolicName = bundle.getSymbolicName();
								this.bundle = bundle;
								bundleTransition = BundleTransitionImpl.INSTANCE.getTransition(bundle);
							} else {
								BundleProjectMeta bundleProjectMeta = BundleProjectMetaImpl.INSTANCE;
								symbolicName = bundleProjectMeta.getSymbolicName(project);
							}
						} catch (InPlaceException e) {
						}
					}
			}
		}
		if (null == symbolicName) {
			setPlugin(Activator.PLUGIN_ID);
		} else {
			setPlugin(symbolicName);
		}
		// convertToSeverity(statusCode);
		this.statusCode = statusCode;
		this.project = project;		
	}

	/**
	 * Converts a {@code StatusCode} to a {@code Status} status code
	 * 
	 * @return the converted {@code StatusCode} to an {@code Status} status
	 */
	public int convertSeverity() {

		switch (statusCode) {
		case OK:
			setSeverity(IStatus.OK);
			break;
		case WARNING:
		case BUILD_WARNING:	
			setSeverity(IStatus.WARNING);
			break;
		case INFO:
			setSeverity(IStatus.INFO);
			break;
		case CANCEL:
			setSeverity(IStatus.CANCEL);
			break;
		case JOB_ERROR:
		case BUILD_ERROR:
		case SERVICE_ERROR:
		case MODULAR_ERROR:
		case ERROR:
		case EXCEPTION:
			setSeverity(IStatus.ERROR);
			break;
		default:	
			setSeverity(IStatus.OK);			
		}
		return getSeverity();
	}

	/**
	 * Converts an {@code IStatus} status to a {@code StatusCode}
	 * @param statusCode is the status code to convert to an {@code Status} status
	 * 
	 * @return the converted {@code Status} to a {@code StatusCode} status
	 */
	public StatusCode converToStatusCode() {

		switch (new Integer(getSeverity())) {
		case OK:
			setStatusCode(StatusCode.OK);
			break;
		case WARNING:
			setStatusCode(StatusCode.WARNING);
			break;
		case INFO:
			setStatusCode(StatusCode.INFO);
			break;
		case CANCEL:
			setStatusCode(StatusCode.CANCEL);
			break;
		case ERROR:
			setStatusCode(StatusCode.ERROR);
			break;
		default:	
			setStatusCode(StatusCode.OK);
		}
		return statusCode;
	}

	/**
	 * Set highest severity from children
	 */
	public StatusCode setHighestStatusCode() {		
		
		return statusCode = setHighestStatusCode(this, statusCode);
		
	}
	
	public StatusCode setHighestStatusCode(IBundleStatus status, StatusCode statusCode) {		

		if (statusCode == StatusCode.ERROR) {
			return statusCode;
		}
		switch (status.getStatusCode()) {
		case WARNING:
		case BUILD_WARNING:	
			statusCode = StatusCode.WARNING;					
			break;
		case BUILD_ERROR:
		case SERVICE_ERROR:
		case MODULAR_ERROR:
		case ERROR:
		case EXCEPTION:
			statusCode = StatusCode.ERROR;
		default:
			break;
		}
		for (IStatus childStatus : status.getChildren()) {
			if (childStatus instanceof IBundleStatus) {
				statusCode = setHighestStatusCode((IBundleStatus) childStatus, statusCode);
			}
		}	
		return statusCode;
	}
	
	@Override
	public int getBundleState() {
		return bundleState;
	}

	public void setBundleState(int bundleState) {
		this.bundleState = bundleState;
	}

	public Transition getBundleTransition() {
		return bundleTransition;
	}

	public void setBundleTransition(Transition bundleTransition) {
		this.bundleTransition = bundleTransition;
	}

	@Override
	public boolean hasStatus(StatusCode statusCode) {
		return statusCode == this.statusCode;
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}
	
	@Override
	public void setMessage(String message) {
		super.setMessage(message);
	}

	@Override
	public void setSeverity(int severity) {
		super.setSeverity(severity);
	}

	@Override
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
		super.setSeverity(convertSeverity());
	}
	
	public StatusCode getStatusCode() {
		return statusCode;
	}

	@Override
	public void add(Collection<IBundleStatus> statusList) {
		for (IBundleStatus status : statusList) {
			add(status);
		}
	}
	
	@Override
	public final IProject getProject() {
		if (null == project && null != bundle) {
			BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE; 
			return bundleRegion.getProject(bundle);		
		}
		return project;
	}

	@Override
	public final void setProject(IProject project) {
		this.project = project;
	}

	@Override
	public Bundle getBundle() {
		if (null == bundle && null != project) {
			BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE; 
			return bundleRegion.getBundle(project);
			
		}
		return bundle;
	}

	@Override
	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}
}
