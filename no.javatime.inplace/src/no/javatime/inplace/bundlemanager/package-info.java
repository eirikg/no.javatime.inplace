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
Maintains workspace bundles as a region. A workspace of bundles is deactivated if all bundles are in 
state UNINSTALLED. If one or more bundles are activated, the workspace is said to be activated 
and all deactivated bundles are in state INSTALLED while the activated bundle(s) are in 
state RESOLVED, ACTIVE or STARTING. 
 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.bundlemanager;


