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
package no.javatime.inplace.statushandler;

import java.util.Collection;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.osgi.framework.Bundle;

/**
 * Bundle status object containing status codes, exceptions and messages associated with a bundle project.
 */
public class BundleStatus extends MultiStatus implements IBundleStatus {

	/** Overrules the {@code IStatus} status codes */
	private Enum<StatusCode> statusCode;
	private IProject project;
	private Long bundleId;

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @see #convertToSeverity(no.javatime.inplace.statushandler.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, String message) {
		super(pluginId, IStatus.OK, message, null);
		convertToSeverity(statusCode);
		this.statusCode = statusCode;
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
		this.statusCode = converToStausCode(status.getSeverity());
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
	 * @see #convertToSeverity(no.javatime.inplace.statushandler.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, IProject project, String message,
			Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		convertToSeverity(statusCode);
		this.statusCode = statusCode;
		this.project = project;
	}

	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param bundleId associated with this status object
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertToSeverity(no.javatime.inplace.statushandler.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, Long bundleId, String message, Throwable exception) {
		super(pluginId,IStatus.OK, message, exception);
		convertToSeverity(statusCode);
		this.statusCode = statusCode;
		this.bundleId = bundleId;
	}
		
	/**
	 * Creates a new status object. The underlying <code>Status</code> object is assigned a status 
	 * converted from the specified {@code StatusCode}
	 * 
	 * @param statusCode one of the <code>StatusCode</code> constants
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a verbose message related to the status
	 * @param exception an exception or <code>null</code> if not applicable
	 * @see #convertToSeverity(no.javatime.inplace.statushandler.IBundleStatus.StatusCode)
	 */
	public BundleStatus(StatusCode statusCode, String pluginId, String message, Throwable exception) {
		super(pluginId, IStatus.OK, message, exception);
		convertToSeverity(statusCode);
		this.statusCode = statusCode;
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
	public boolean hasStatus(StatusCode statusCode) {
		return statusCode == this.statusCode;
	}
	
	@Override
	public Enum<StatusCode> getStatusCode() {
		return this.statusCode;
	}
	
	public void setStatusCode(Enum<StatusCode> statusCode) {
		this.statusCode = statusCode;
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
		statusCode = converToStausCode(severity);
		super.setSeverity(severity);
	}

	@Override
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
		super.setSeverity(convertToSeverity(statusCode));
	}

	@Override
	public void add(Collection<IBundleStatus> statusList) {
		for (IBundleStatus status : statusList) {
			add(status);
		}
	}

	@Override
	public final IProject getProject() {
		if (null == project && null != bundleId) {
			project = BundleManager.getRegion().getProject(InPlace.getContext().getBundle(bundleId));
		}
		return project;
	}

	@Override
	public final void setProject(IProject project) {
		this.project = project;
	}

	@Override
	public Bundle getBundle() {
		if (null != bundleId) {
			return InPlace.getContext().getBundle(bundleId);
		}
		return null;
	}

	@Override
	public void setBundle(Long bundleId) {
		this.bundleId = bundleId;
	}
}
