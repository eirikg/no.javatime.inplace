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
Workarounds for Bug 295662.  Defines undefined contexts when bundles are resolved and unresolved.

In addition a custom status handler (overrides Eclipse status handler) is implemented to report status 
of possible errors related to dynamic loading and unloading of plug ins.

There is also general methods for dynamic adding and removing extensions.
 
 <!-- Put @see and @since tags down here. -->
 */
package no.javatime.inplace.statushandler;


