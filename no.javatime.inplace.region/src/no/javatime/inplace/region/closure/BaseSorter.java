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

/**
 * This base class is typically used by classes analyzing cycles in workspace bundles and projects.
 * <p>
 * Set whether cycles and self reference among elements are allowed or not.
 * 
 * @see ProjectSorter
 * @see BundleSorter
 */
 class BaseSorter {

	/**
	 * True if cycles are allowed. Default is not allowed
	 */
	private Boolean allowCycles = false;
	/**
	 * True if self reference is allowed. Default is allowed
	 */
	private Boolean allowSelfReference = true;

	protected CircularReferenceException circularException;

	/**
	 * Default constructor
	 */
	public BaseSorter() {
	}

	/**
	 * Determines whether cycles between elements are ignored or not
	 * 
	 * @return true if cycles are allowed and false if not
	 */
	public Boolean getAllowCycles() {
		return allowCycles;
	}

	/**
	 * Specifies whether cycles between elements are ignored or not. Cycles are by default not allowed
	 * 
	 * @param allowCycles true if cycles are allowed and false if not
	 */
	public void setAllowCycles(Boolean allowCycles) {
		this.allowCycles = allowCycles;
	}

	/**
	 * Determines whether self reference between elements are ignored or not
	 * 
	 * @return true if self reference is allowed and false if not
	 */
	public Boolean getAllowSelfReference() {
		return allowSelfReference;
	}

	/**
	 * Specifies whether self reference between elements are ignored or not. Self reference is by default
	 * allowed
	 * 
	 * @param allowSelfReference true if self reference is allowed and false if not
	 */
	public void setAllowSelfReference(Boolean allowSelfReference) {
		this.allowSelfReference = allowSelfReference;
	}

}
