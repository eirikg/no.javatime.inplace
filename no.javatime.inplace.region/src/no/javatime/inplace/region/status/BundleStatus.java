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
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.util.messages.ErrorMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.ui.statushandlers.StatusManager;
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
	private Transition bundleTransition = Transition.NOTRANSITION;;
	

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @see #convertToSeverity(no.javatime.inplace.region.status.IBundleStatus.StatusCode)
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
	 * @see #converToStausCode(int)
	 */
	public BundleStatus(IStatus status) {
		super(status.getPlugin(), status.getSeverity(), status.getMessage(), status.getException());
		if (status instanceof IBundleStatus) {
			IBundleStatus bundleStatus = (BundleStatus) status;
			init(bundleStatus.getStatusCode(), bundleStatus.getBundle(), bundleStatus.getProject());			
		} else {
			init(converToStausCode(status.getSeverity()), null, null);
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
	 * @see #convertToSeverity(no.javatime.inplace.region.status.IBundleStatus.StatusCode)
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
	 * @see #convertToSeverity(no.javatime.inplace.region.status.IBundleStatus.StatusCode)
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
	 * @see #convertToSeverity(no.javatime.inplace.region.status.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, Bundle bundle, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, bundle, null);
	}
		
	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertToSeverity(no.javatime.inplace.region.status.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		init(statusCode, null, null);
	}

	private void init(StatusCode statusCode, Bundle bundle, IProject project) {
		String symbolicName = null;
		// Must have a symbolic name
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
				IBundleProjectDescription pd = Activator.getBundleDescription(project);
				symbolicName = pd.getSymbolicName();
			}
		} else {
			// Worst case. Symbolic name is not always the same as the plug-gin id.
			if (null == symbolicName) {
				try {
					symbolicName = getPlugin();
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IWorkspaceRoot root = workspace.getRoot();
					for (IProject bundleProject : root.getProjects()) {
						IBundleProjectDescription bundleProjDesc = Activator.getBundleDescription(bundleProject);
						String pdSymbolicName = bundleProjDesc.getSymbolicName();
						if (null != pdSymbolicName && pdSymbolicName.equals(symbolicName)) {
							// Drop comparison with version. Use the first one available
							project = bundleProject;
							break;
						}
					}
					if (null != project) {
						bundle = WorkspaceRegionImpl.INSTANCE.getBundle(project);
						if (null != bundle) {
							symbolicName = bundle.getSymbolicName();
							this.bundle = bundle;
							bundleTransition = BundleTransitionImpl.INSTANCE.getTransition(bundle);
						} else {
							IBundleProjectDescription pd = Activator.getBundleDescription(project);
							symbolicName = pd.getSymbolicName();
						}
					}
				} catch (InPlaceException e) {
					String msg = ErrorMessage.getInstance().formatString("manifest_missing");
					// Do not use bundle status due to infinite recursion
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e),
							StatusManager.LOG);
				}
			}
		}
		if (null == symbolicName) {
			setPlugin(Activator.PLUGIN_ID);
		} else {
			setPlugin(symbolicName);
		}
		convertToSeverity(statusCode);
		this.statusCode = statusCode;
		this.project = project;		
	}

	/**
	 * Converts a {@code StatusCode} to an {@code Status} status code.
	 * @param statusCode is the status code to convert to an {@code IStatus} status
	 * IStaus code CANCEL is not sent to the log view. 
	 * @return the converted {@code StatusCode} to an {@code Status} status
	 */
	private int convertToSeverity(StatusCode statusCode) {

		if (statusCode == StatusCode.INFO || statusCode == StatusCode.JOBINFO
				|| statusCode == StatusCode.CANCEL) {
			setSeverity(IStatus.INFO);
		} else if(statusCode == StatusCode.ERROR
				|| statusCode == StatusCode.EXCEPTION) {
			setSeverity(IStatus.ERROR);				
		} else if(statusCode == StatusCode.WARNING
				|| statusCode == StatusCode.BUILDERROR) {
			setSeverity(IStatus.WARNING);
		}  else if(statusCode == StatusCode.OK) {
			setSeverity(IStatus.OK);
		} 
		return getSeverity();
	}

	/**
	 * Converts an {@code IStatus} status to a {@code StatusCode}.
	 * @param statusCode is the status code to convert to an {@code Status} status
	 * @return the converted {@code Status} to a {@code StatusCode} status
	 */
	private StatusCode converToStausCode(int severity) {

		if (severity == IStatus.INFO) {
			return StatusCode.INFO;
		} else if(severity == IStatus.ERROR) {
			return StatusCode.ERROR;
		} else if(severity == IStatus.WARNING) {
			return StatusCode.WARNING;
		} else if(severity == IStatus.CANCEL) {
			return StatusCode.CANCEL;
		}  else if(severity == IStatus.OK) {
			return StatusCode.OK;
		} 
		return StatusCode.OK;
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
		// TODO this one destroys the original StatusCode
		statusCode = converToStausCode(severity);
		super.setSeverity(severity);
	}

	@Override
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
		super.setSeverity(convertToSeverity(statusCode));
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
	
	/**
	 * Set highest severity from children
	 */
	public void setStatusCode() {
		
		StatusCode statusCode = StatusCode.INFO;
		for (IStatus status : getChildren()) {
			if (status instanceof IBundleStatus) {
				IBundleStatus bundleStatus = (IBundleStatus) status;
				if (bundleStatus.getStatusCode() == StatusCode.ERROR) {
					statusCode = StatusCode.ERROR;
					break;
				} else if (bundleStatus.getStatusCode() == StatusCode.BUILDERROR) {
						statusCode = StatusCode.BUILDERROR;
						break;
				} else if (bundleStatus.getStatusCode() == StatusCode.WARNING) {
					statusCode = StatusCode.WARNING;					
				}
			}
		}
		setStatusCode(statusCode);
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
