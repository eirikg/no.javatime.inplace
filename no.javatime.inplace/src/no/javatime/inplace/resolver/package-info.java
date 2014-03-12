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
 A factory is used to create resolver hook objects that filters bundles and detects duplicate bundle instances.
 The created resolver hook objects intercepts the resolver and is of type  {@linkplain no.javatime.inplace.bundlemanager.BundleResolveHandler}.  
 <p>
 A resolver hook object created by the factory removes duplicate bundles that are singletons and filter out 
 (exclude from resolve) deactivated bundles, bundle closures with build errors and activated bundles that 
 are dependent on deactivated bundles.
 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.resolver;