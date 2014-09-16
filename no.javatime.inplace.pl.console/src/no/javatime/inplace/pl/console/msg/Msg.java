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
package no.javatime.inplace.pl.console.msg;

import org.eclipse.osgi.util.NLS;

public class Msg extends NLS {

	public static String SYSTEM_OUT_BUTTON_TXT;
	public static String SYSTEM_OUT_ACTION_TOOLTIP;
	public static String SYSTEM_EXIT_BUTTON_TXT;
	public static String SYSTEM_EXIT_ACTION_TOOLTIP;
	
	private static final String BUNDLE_NAME = "no.javatime.inplace.pl.console.msg.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}

}
