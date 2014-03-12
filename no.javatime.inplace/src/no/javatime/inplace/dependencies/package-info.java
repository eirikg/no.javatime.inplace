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
/**
 * Topological sort and direct and transitive traversal of workspace bundles and projects.
 * <p>
 * Dependencies between bundles and projects (elements) is viewed as a directed acyclic graph (DAG). The sort strategy applied is a depth-first
 * search (DFS), finding a final element by following a path from an initial element until it is not possible
 * to extend the path. The DFS traverse the elements, and stores them as an ordered set of elements in
 * providing or requiring dependency order.
 */
package no.javatime.inplace.dependencies;