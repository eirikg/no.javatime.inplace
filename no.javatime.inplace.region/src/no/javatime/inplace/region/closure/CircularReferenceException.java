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
package no.javatime.inplace.region.closure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * Circular references in projects and in bundles.
 * <p>
 * All elements (projects or bundles) included in cycles and a list of status objects, one for each cycle, is
 * registered and can be obtained from the exception.
 */
public class CircularReferenceException extends RuntimeException {

	private static final long serialVersionUID = -6180458290568463709L;
	private transient Collection<Bundle> bundles;
	private transient Collection<IProject> projects;
	private Collection<IBundleStatus> statuslist = new ArrayList<IBundleStatus>();

	/**
	 * Creates a circular exception containing a default message indicating a termination due to a cycle.
	 */
	public CircularReferenceException() {
		super(ExceptionMessage.getInstance().formatString("circular_reference_termination"));
	}

	/**
	 * Creates a circular exception containing a message describing the cycle
	 * @param message describing the circular exception
	 */
	public CircularReferenceException(String message) {
		super(message);
	}

	/**
	 * List containing one status object for each cycle detected
	 * 
	 * @return list of cycle objects
	 */
	public Collection<IBundleStatus> getStatusList() {
		return statuslist;
	}

	/**
	 * Adds a status object describing a cycle
	 * 
	 * @param circularStatus the cycle as a status object
	 */
	public void addToStatusList(BundleStatus circularStatus) {
		this.statuslist.add(circularStatus);
	}

	/**
	 * Get all bundles included in cycles(s) 
	 *  
	 * @return the set of bundles or null
	 */
	public Collection<Bundle> getBundles() {
		return bundles;
	}

	/**
	 * Add bundles included in any cycle
	 * 
	 * @param bundles included in cycles
	 */
	public void addBundles(Collection<Bundle> bundles) {
		if (null == this.bundles) {
			this.bundles = new LinkedHashSet<Bundle>();
		}
		this.bundles.addAll(bundles);
	}

	/**
	 * Add a bundle included in any cycle
	 * 
	 * @param bundle included in cycles
	 */
	public void addBundle(Bundle bundle) {
		if (null == bundles) {
			bundles = new LinkedHashSet<Bundle>();
		}
		bundles.add(bundle);
	}

	/**
	 * Get all projects included in cycles(s) 
	 *  
	 * @return the set of projects or null
	 */
	public Collection<IProject> getProjects() {
		return projects;
	}

	/**
	 * Add projects included in any cycle
	 * 
	 * @param projects included in cycles
	 */
	public void addProjects(Collection<IProject> projects) {
		if (null == this.projects) {
			this.projects = new LinkedHashSet<IProject>();
		}
		this.projects.addAll(projects);
	}

	/**
	 * Add a project included in any cycle
	 * 
	 * @param project included in cycles
	 */
	public void addProject(IProject project) {
		if (null == projects) {
			projects = new LinkedHashSet<IProject>();
		}
		projects.add(project);
	}
}
