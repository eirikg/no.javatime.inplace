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
package no.javatime.inplace.bundlemanager.events;

import java.util.EventListener;

/**
 * Callback interface for clients interested in being notified when a new bundle job is generated.
 * The bundle job contains pending projects and ready for scheduling by clients. 
 *
 */
public interface BundleTransitionEventListener extends EventListener {
	public void bundleTransitionChanged(BundleTransitionEvent evt);
}
