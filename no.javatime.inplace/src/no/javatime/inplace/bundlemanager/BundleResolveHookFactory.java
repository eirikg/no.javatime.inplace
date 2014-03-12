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
package no.javatime.inplace.bundlemanager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;



/**
 * A service to be registered to intercept resolve operations. This service is used to return an instance of
 * the singleton resolver handler for each resolve operation.
 * <p>
 * Maybe used by clients to detect and remove Plug-ins that are singletons. This is for instance the case if
 * the plug-in contributes to the UI using extensions. When resolving bundles that are singletons a collision
 * may occur with earlier resolved bundles with the same symbolic name. In these cases the duplicate (the
 * earlier resolved bundle) can be removed if they supplied to the resolver hook. This is the method used
 * internally by the update job.
 */

public class BundleResolveHookFactory implements ResolverHookFactory {

	private BundleResolveHandler handler;

	public BundleResolveHookFactory() {
	}

	/**
	 * Creates a new bundle resolve handler
	 * 
	 * @return a new instance of the bundle resolve handler
	 */
	private BundleResolveHandler init() {
		BundleResolveHandler handler = new BundleResolveHandler();
		return handler;
	}

	@Override
	public synchronized ResolverHook begin(Collection<BundleRevision> triggers) {
		if (null == this.handler) {
			this.handler = init();
		}
		return handler;
	}

	/**
	 * Groups of duplicate singletons
	 * 
	 * @return the singletons groups or null
	 */
	public synchronized Map<Bundle, Set<Bundle>> getGroups() {
		if (null == this.handler) {
			this.handler = init();
		}
		return handler.getGroups();
	}

	/**
	 * Groups of duplicate singletons
	 * 
	 * @param groups the singletons groups to set
	 */
	public synchronized void setGroups(Map<Bundle, Set<Bundle>> groups) {
		if (null == this.handler) {
			this.handler = init();
		}
		handler.setGroups(groups);
	}
}
