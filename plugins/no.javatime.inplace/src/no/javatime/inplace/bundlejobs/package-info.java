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
 * Set of bundle jobs implementing composite (Activate, Deactivate, Reinstall 
 * and Reset) and explicit (Start, Stop, Install, Uninstall, Resolve, Update and Refresh) BundleExecutorEventManagerImpl commands.
 * <p>
 * A project is activated when the JavaTime nature is applied to the project and a bundle
 * is activated when the project is activated and the bundle is in state RESOLVED, ACTIVE or STARTING. Activated projects with errors may be in any state.
 * <p>
 * The scheduling rule for a bundle job is the same as for the incremental builder, 
 * assuring that different bundle jobs and the builder never runs in parallel.
 * <p>
 * Bundle projects are added and removed automatically to and from jobs according to three kinds of dependency rules and to a set of dependency options.
 * There are three kinds of dependency closures:
 * <ol>
 * <li>The requiring dependency closure includes all bundles that directly and indirectly require capabilities from an initial set of bundles.
 * <li>The providing dependency closure includes all bundles that directly and indirectly provide capabilities to an initial set of bundles.
 * <li>For update and refresh the error dependency closure is the requiring dependency closure from an initial set of bundles with build errors and 
 * the providing dependency closure with the calculated requiring dependency closure as the initial set. 
 * <li>For activate and deactivate the error dependency closure is calculated from an initial set of bundles with build errors and their requiring dependency closure. 
 * </ol>
 * The dependency rules calculates the providing dependency closure on bundles being activated and the requiring dependency closure on bundles being
 * deactivated. The calculated dependency closures are added to the activate and deactivate job.
 * The error dependency closure is calculated to remove bundles from scheduled bundle jobs.
 * <p>
 * Dependency options can be expressed as a sequence of the requiring and providing dependency closures and are calculated for inclusion of additional bundles in a
 * start, stop, activate or deactivate job.
 * 
 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.bundlejobs;

