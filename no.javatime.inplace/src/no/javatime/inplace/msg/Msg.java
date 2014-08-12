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
	public static String REMOVE_BUNDLE_CLASSPATH_ENTRY_TRACE;
	public static String REMOVE_BUNDLE_CLASSPATH_TRACE;
	public static String TOGGLE_ACTIVATION_POLICY_TRACE;
	
	// Bundle trace operations
	public static String START_BUNDLE_OPERATION_TRACE;
	public static String STOP_BUNDLE_OPERATION_TRACE;
	public static String REFRESH_BUNDLE_OPERATION_TRACE;
	public static String RESOLVE_BUNDLE_OPERATION_TRACE;
	public static String UNINSTALL_BUNDLE_OPERATION_TRACE;
	public static String INSTALL_BUNDLE_OPERATION_TRACE;
	public static String UPDATE_BUNDLE_OPERATION_TRACE;
	public static String EXTERNAL_BUNDLE_OPERATION_TRACE;
	public static String DISABLE_NATURE_TRACE;
	public static String ENABLE_NATURE_TRACE;
	public static String UNDEFINED_CONTEXT_ERROR_TRACE;
	public static String JOB_NAME_TRACE;

	// Build trace operations
	public static String BUILD_HEADER_TRACE;
	public static String NO_RESOURCE_DELTA_BUILD_TRACE;
	public static String NO_RESOURCE_DELTA_BUILD_AVAILABLE_TRACE;
	public static String INCREMENTAL_BUILD_TRACE;
	public static String FULL_BUILD_TRACE;
	public static String BUILD_ERROR_UPDATE_TRACE;
	public static String BUILD_ERROR_TRACE;
	public static String USING_CURRENT_REVISION_TRACE;
	
	// Bundle job names
	public static String ACTIVATE_PROJECTS_JOB;
	public static String ACTIVATE_WORKSPACE_JOB;
	public static String INIT_DEACTIVATED_WORKSPACE_JOB;

	// Errors
	public static String BEGIN_SHUTDOWN_ERROR;
	public static String END_SHUTDOWN_ERROR;
	public static String INSTALL_ERROR;

	// Warnings
	public static String EXTERNAL_UNINSTALL_WARN;
	public static String REQUIRING_BUNDLES_WARN;
	public static String DELAYED_BUILD_WARN;
	public static String RENAME_PROJECT_WARN;
	public static String INIT_WORKSPACE_STORE_WARN;
	
	// Dialog and View messages
	public static String DEACTIVATE_QUESTION_DLG;
	public static String DEACTIVATE_QUESTION_REQ_DLG;
	
	// Info messages
	public static String DEACTIVATE_BUILD_ERROR_INFO;
	public static String UPDATE_BUILD_ERROR_INFO;
	public static String ACTIVATED_BUNDLES_INFO;
	public static String IMPLICIT_ACTIVATION_INFO;
	public static String DELAYED_RESOLVE_INFO;
	public static String DELAYED_UPDATE_INFO;
	public static String SYSTEM_EXIT_INFO;
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.msg.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}

}
