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
 * Bundle status object containing status codes, exceptions and messages associated with a bundle project.
 */
public interface IBundleStatus extends IStatus {

	/**
	 * Status codes assigned to <code>BundleStatus</code> objects. Used instead of <code>IStatus</code> status types.
	 */
	public enum StatusCode {
		OK, CANCEL, INFO, WARNING, ERROR, EXCEPTION, BUILDERROR, JOBERROR
	}

	/**
	 * Check the status code in the <code>BundleStatus</code> object against the specified status code
	 * 
	 * @param statusCode the status code to check
	 * @return true if the status code for this <code>BundleStatus</code> object is the same as the specified status code
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
	 * Bundle state at time of creation of this bundle status object
	 * if not changed by {@linkplain #setBundleState(int)}
	 * @return the bundle state
	 */
	int getBundleState();
	
	/**
	 * Set the bundle state
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
	 * @return The project associated with the status object or null if no project is registered with the status object
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
	 * @return The bundle associated with the status object or null if no bundle is registered with the status object
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
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, <code>INFO</code>, <code>WARNING</code>,
	 *          or <code>CANCEL</code>
	 */
	void setSeverity(int severity);
	
	
	/**
	 * Sets the exception.
	 *
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable 
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
	 * If this status object has children, assign the status code from the children with
	 * the highest ranking where the ranking in increasing order is {@code StatusCode#WARNING},
	 * {@code StatusCode#BUILDERROR}, {@code StatusCode#ERROR}
	 */
	public void setStatusCode();
}
