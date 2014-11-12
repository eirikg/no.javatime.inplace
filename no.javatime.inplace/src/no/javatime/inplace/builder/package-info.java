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
 state of a bundle.
 <p>
 There is a 1:1 relationship between a project and its bundle. The term bundle project refers to a project that has the java and 
 plug-in nature enabled, the relation between them (e.g. the symbolic name and version as the primary key or the location 
 identifier as the primary key) and the bundle combined. The relation is said to be implemented or realized when the  bundle is at 
 least in state installed.
 <p>
 A project is either activated or deactivated. If one or more projects are activated the workspace is said to be activated.  
 A project is activated when it is assigned a JavaTime nature. In an activated workspace all activated bundle projects are at least in state resolved
 and deactivated bundle projects are in state installed. In a deactivated workspace all bundles are in state uninstalled and all resource and build
 change events are ignored.    
 <p>
 Each resource change on activated projects leads to an invocation of the java time builder object which adds pending transitions (e.g. update) 
 to the affected projects. A pending transition is picked up by the post build listener and other parties which schedules bundle jobs based 
 on the type of the transition. The post build listener schedules jobs based on pending transitions for project activate (adding the JavaTime nature), 
 bundle activate(install, resolve/refresh, start), install, uninstall, deactivate (removing the JavaTime nature) and update. 
 <p>
 Jobs are also scheduled directly by the pre change, the post build and the pre build listener for project CRUD operations. 
 Removal (delete and close) of projects are acted on by the pre change listener scheduling an uninstall job for removed projects and deactivation
 of projects requiring capabilities from the removed projects. The post build listener schedules an add project (import, open, create and rename) 
 job where each added project and providing projects are installed or resolved (and started) based on the activation mode of a project.  

 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.builder;

