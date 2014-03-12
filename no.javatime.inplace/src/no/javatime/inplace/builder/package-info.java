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
Listen to resource and build change events of projects and schedules bundle life cycle jobs depending on type of change event and
state of bundle.      
<p>
A project activated in-place gets a JavaTime nature.
<p>
There is a 1:1 relation between a project and its bundle. Most resource and build state change in a project triggers a set of actions on its bundle. The resource and
build listeners are responsible for detecting these changes and determine the set of bundle operations needed for each kind of change. The listeners set up 
and schedule jobs to perform batches of bundle operations. See {@link no.javatime.inplace.bundlejobs bundle jobs} for a description of batches of bundle operations.
 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.builder;


