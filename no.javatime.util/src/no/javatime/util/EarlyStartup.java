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
package no.javatime.util;

import org.eclipse.ui.IStartup;

/**
 * The purpose to force a start up right after workspace creation of this
 * plug-in is twofold: 
 * <ul>
 * <li>This bundle who manages other bundles is started as part of the workspace
 * creation.</li>
 * <li>Be able to process changes that occur
 * between the time of workspace creation and the time when this plug-in is
 * activated.</li>
 * </ul>
 */
public final class EarlyStartup implements IStartup {

	public void earlyStartup() {
		// do nothing but ensure this plug-in is started as early as possible
	}
}
