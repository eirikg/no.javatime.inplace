/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 202583, 207344
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 218648 
 *******************************************************************************/
package no.javatime.inplace.msg;

import org.eclipse.osgi.util.NLS;

public class Msg extends NLS {

	// Manifest trace operations 
	public static String UPDATE_BUNDLE_CLASSPATH_TRACE;
	public static String BUNDLE_CLASSPATH_EXIST_TRACE;
	public static String REMOVE_BUNDLE_CLASSPATH_ENTRY_TRACE;
	public static String REMOVE_BUNDLE_CLASSPATH_TRACE;
	public static String NO_BUNDLE_CLASSPATH_HEADER_TRACE;
	public static String TOGGLE_ACTIVATION_POLICY_TRACE;
	
	// Bundle trace operations
	public static String START_BUNDLE_OPERATION_TRACE;
	public static String STOP_BUNDLE_OPERATION_TRACE;
	public static String REFRESH_BUNDLE_OPERATION_TRACE;
	public static String RESOLVE_BUNDLE_OPERATION_TRACE;
	public static String UNINSTALL_BUNDLE_OPERATION_TRACE;
	public static String INSTALL_BUNDLE_OPERATION_TRACE;
	public static String UPDATE_BUNDLE_OPERATION_TRACE;
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.msg.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}

}
