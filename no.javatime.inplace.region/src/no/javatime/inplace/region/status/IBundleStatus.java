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

import no.javatime.inplace.region.intface.BundleTransition.Transition;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;

/**
 * Bundle status object containing status codes, exceptions and messages associated with a bundle
 * project.
 */
public interface IBundleStatus extends IStatus {

	/**
	 * Status codes assigned to <code>BundleStatus</code> objects. Used instead of
	 * <code>IStatus</code> status types.
	 */
	public enum StatusCode {
		OK, CANCEL, INFO, WARNING, ERROR, EXCEPTION, BUILD_WARNING, BUILD_ERROR, MODULAR_ERROR, SERVICE_ERROR, JOB_ERROR
	}

	/**
	 * Check the status code in the <code>BundleStatus</code> object against the specified status code
	 * 
	 * @param statusCode the status code to check
	 * @return true if the status code for this <code>BundleStatus</code> object is the same as the
	 * specified status code
	 */
	boolean hasStatus(StatusCode statusCode);

	/**
	 * Set the status code of this bundle status object
	 * 
	 * @param statusCode the status code to set
	 */
	void setStatusCode(StatusCode statusCode);

	/**
	 * Get the status code of this bundle status object
	 * 
	 * @return statusCode of this status object
	 */
	StatusCode getStatusCode();

	/**
	 * Bundle state at time of creation of this bundle status object if not changed by
	 * {@linkplain #setBundleState(int)}
	 * 
	 * @return the bundle state
	 */
	int getBundleState();

	/**
	 * Set the bundle state
	 * 
	 * @param bundleState the bundle state
	 */
	void setBundleState(int bundleState);

	public Transition getBundleTransition();

	public void setBundleTransition(Transition bundleTransition);

	/**
	 * Returns the message describing the outcome. The message is localized to the current locale.
	 * 
	 * @return a localized message
	 */
	@Override
	String getMessage();

	/**
	 * Sets the message. If null is passed, message is set to an empty string.
	 * 
	 * @param message a human-readable message, localized to the current locale
	 */
	void setMessage(String message);

	/**
	 * The project associated with status object if any
	 * 
	 * @return The project associated with the status object or null if no project is registered with
	 * the status object
	 */
	IProject getProject();

	/**
	 * Associate a project with this status object
	 * 
	 * @param projct the project to associate with the status object
	 */
	void setProject(IProject projct);

	/**
	 * The bundle associated with status object if any
	 * 
	 * @return The bundle associated with the status object or null if no bundle is registered with
	 * the status object
	 */
	Bundle getBundle();

	/**
	 * Associate a bundle with this status object
	 * 
	 * @param bundle the bundle to associate with the status object
	 */
	void setBundle(Bundle bundle);

	/**
	 * Sets the severity status
	 * 
	 * @param severity the severity; one of <code>OK</code>, <code>MODULAR_REFRESH_ERROR</code>,
	 * <code>INFO</code>, <code>WARNING</code>, or <code>CANCEL</code>
	 */
	void setSeverity(int severity);

	/**
	 * Sets the exception.
	 * 
	 * @param exception a low-level exception, or <code>null</code> if not applicable
	 */
	void setException(Throwable exception);

	/**
	 * Adds a list of status objects as children to this multi status
	 * 
	 * @param statusList status objects to add as children to this multi status
	 */
	void add(Collection<IBundleStatus> statusList);

	/**
	 * @see org.eclipse.core.runtime.MultiStatus#add(IStatus)
	 */
	void add(IStatus status);

	/**
	 * @see org.eclipse.core.runtime.MultiStatus#addAll(IStatus)
	 */
	void addAll(IStatus status);

	/**
	 * @see org.eclipse.core.runtime.MultiStatus#merge(IStatus)
	 */
	void merge(IStatus status);

	/**
	 * Assign the severity of the underlying {@code IStatus} to this bundle status object.
	 * <p>
	 * The mapping is one-to-one where the name of the enum element status code is the same as the
	 * constant name of the severity
	 * 
	 * @return The converted status code
	 */
	StatusCode converToStatusCode();

	/**
	 * Convert the status code of this bundle status object to a severity of {@code IStatus} and
	 * assigns it to the underlying {@code IStatus} object of this status object. The mapping is:
	 * <ol>
	 * <li> {@code StatusCode.OK} -> {@code IStatus.OK}
	 * <li> {@code StatusCode.WARNING} -> {@code IStatus.WARNING}
	 * <li>{@code StatusCode.INFO} -> {@code IStatus.INFO}
	 * <li> {@code StatusCode.CANCEL} -> {@code IStatus.CANCEL}
	 * <li> {@code StatusCode.ERROR}, {@code StatusCode.EXCEPTION}, {@code StatusCode.BUILD_ERROR}
	 * {@code StatusCode.MODULAR_ERROR}, {@code StatusCode.SERVICE_ERROR},
	 * {@code StatusCode.JOB_ERROR} -> {@code IStatus.ERROR}
	 * </ol>
	 * 
	 * @return The converted severity
	 */
	int convertSeverity();

	/**
	 * If this status object has children, assign the status code from the children with the highest
	 * ranking where the ranking from lowest to highest is:
	 * <ol>
	 * <li> {@code StatusCode.OK}, {@code StatusCode.CANCEL}, {@code StatusCode.INFO}
	 * <li> {@code StatusCode.WARNING}
	 * <li> {@code StatusCode.ERROR}, {@code StatusCode.EXCEPTION}, {@code StatusCode.BUILD_ERROR}
	 * {@code StatusCode.MODULAR_ERROR}, {@code StatusCode.SERVICE_ERROR}
	 * </ol>
	 * 
	 * @return The highest ranked status code
	 */
	public StatusCode setHighestStatusCode();
}
