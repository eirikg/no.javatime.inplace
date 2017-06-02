/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 209474, 207344
 *******************************************************************************/
package no.javatime.inplace.log.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import no.javatime.inplace.log.dl.AbstractEntry;
import no.javatime.inplace.log.dl.LogSession;
import no.javatime.inplace.log.msg.Messages;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Represents a given entry in the Error view
 */
public class BundleLogEntryImpl extends AbstractEntry implements BundleLogEntry {

	public static final String SPACE = " "; //$NON-NLS-1$
	public static final String F_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; //$NON-NLS-1$
	private static final DateFormat GREGORIAN_SDF = new SimpleDateFormat(F_DATE_FORMAT,
			Locale.ENGLISH);
	private static final DateFormat LOCAL_SDF = new SimpleDateFormat(F_DATE_FORMAT);

	private String pluginId;
	private int severity;
	private int code;
	private String fDateString;
	private Date fDate;
	private String message;
	private String stack;
	private Throwable throwable;
	private LogSession session;
	private int bundleState;
	private Transition bundleTransition;

	/**
	 * Constructor
	 */
	public BundleLogEntryImpl() {
		// do nothing
	}

	/**
	 * Constructor - creates a new entry from the given status
	 * 
	 * @param status an existing status to create a new entry from
	 */
	public BundleLogEntryImpl(IBundleStatus status) {
		this(status, null);
	}

	/**
	 * Constructor - creates a new entry from the given status
	 * 
	 * @param status an existing status to create a new entry from
	 */
	public BundleLogEntryImpl(IBundleStatus status, LogSession session) {
		processStatus(status, session);
	}

	/**
	 * Returns the {@link LogSession} for this entry or the parent {@link LogSession} iff:
	 * <ul>
	 * <li>The session is <code>null</code> for this entry</li>
	 * <li>The parent of this entry is not <code>null</code> and is a {@link BundleLogEntryImpl}</li>
	 * </ul>
	 * 
	 * @return the {@link LogSession} for this entry
	 */
	public LogSession getSession() {
		if ((session == null) && (parent != null) && (parent instanceof BundleLogEntryImpl)) {
			return ((BundleLogEntryImpl) parent).getSession();
		}
		return session;
	}

	/**
	 * Sets the {@link LogSession} for this entry. No validation is done on the new session.
	 * 
	 * @param session the session to set.
	 */
	public void setSession(LogSession session) {
		this.session = session;
	}

	/**
	 * Returns the severity of this entry.
	 * 
	 * @return the severity
	 * @see IStatus#OK
	 * @see IStatus#WARNING
	 * @see IStatus#INFO
	 * @see IStatus#ERROR
	 */
	public int getSeverity() {
		return severity;
	}

	/**
	 * Returns if the severity of this entry is {@link IStatus#OK}
	 * 
	 * @return if the entry is OK or not
	 */
	public boolean isOK() {
		return severity == IStatus.OK;
	}

	/**
	 * Returns the code for this entry
	 * 
	 * @return the code for this entry
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the id of the plugin that generated this entry
	 * 
	 * @return the plugin id of this entry
	 */
	public String getPluginId() {
		return pluginId;
	}

	/**
	 * Returns the message for this entry or <code>null</code> if there is no message
	 * 
	 * @return the message or <code>null</code>
	 */
	public String getMessage() {
		return message;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Returns the stack trace for this entry or <code>null</code> if there is no stack trace
	 * 
	 * @return the stack trace or <code>null</code>
	 */
	public String getStack() {
		return stack;
	}

	/**
	 * Returns a pretty-print formatting for the date for this entry
	 * 
	 * @return the formatted date for this entry
	 */
	public String getFormattedDate() {
		if (fDateString == null) {
			fDateString = LOCAL_SDF.format(getDate());
		}
		return fDateString;
	}

	/**
	 * Returns the date for this entry or the epoch if the current date value is <code>null</code>
	 * 
	 * @return the entry date or the epoch if there is no date entry
	 */
	public Date getDate() {
		if (fDate == null) {
			fDate = new Date(0); // unknown date - return epoch
		}
		return fDate;
	}

	/**
	 * Returns the human-readable text representation of the integer severity value or '<code>?</code>
	 * ' if the severity is unknown.
	 * 
	 * @return the text representation of the severity
	 */
	public String getSeverityText() {
		switch (severity) {
		case IStatus.ERROR: {
			return Messages.LogView_severity_error;
		}
		case IStatus.WARNING: {
			return Messages.LogView_severity_warning;
		}
		case IStatus.INFO: {
			return Messages.LogView_severity_info;
		}
		case IStatus.OK: {
			return Messages.LogView_severity_ok;
		}
		}
		return "?"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getSeverityText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.views.log.AbstractEntry#getLabel(java.lang.Object)
	 */
	public String getLabel(Object obj) {
		return getSeverityText();
	}

	/**
	 * Processes a given line from the log file
	 * 
	 * @param line
	 * @throws ParseException
	 */
	public void processEntry(String line) throws ParseException {
		// !ENTRY <pluginID> <severity> <code> <bundleState> <date>
		// !ENTRY <pluginID> <date> if logged by the framework!!!
		StringTokenizer stok = new StringTokenizer(line, SPACE);
		severity = 0;
		code = 0;
		StringBuffer dateBuffer = new StringBuffer();
		int tokens = stok.countTokens();
		String token = null;
		for (int i = 0; i < tokens; i++) {
			token = stok.nextToken();
			switch (i) {
			case 0: {
				break;
			}
			case 1: {
				pluginId = token;
				break;
			}
			case 2: {
				try {
					severity = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			case 3: {
				try {
					code = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			case 4: {
				try {
					bundleState = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			default: {
				appendToken(dateBuffer, token);
			}
			}
		}
		Date date = GREGORIAN_SDF.parse(dateBuffer.toString());
		if (date != null) {
			fDate = date;
			fDateString = LOCAL_SDF.format(fDate);
		}
	}

	/**
	 * Processes the given sub-entry from the log
	 * 
	 * @param line
	 * @return the depth of the sub-entry
	 * @throws ParseException
	 */
	public int processSubEntry(String line) throws ParseException {
		// !SUBENTRY <depth> <pluginID> <severity> <code> <bundleState> <date>
		// !SUBENTRY <depth> <pluginID> <date>if logged by the framework!!!
		StringTokenizer stok = new StringTokenizer(line, SPACE);
		StringBuffer dateBuffer = new StringBuffer();
		int depth = 0;
		String token = null;
		int tokens = stok.countTokens();
		for (int i = 0; i < tokens; i++) {
			token = stok.nextToken();
			switch (i) {
			case 0: {
				break;
			}
			case 1: {
				depth = Integer.parseInt(token);
				break;
			}
			case 2: {
				pluginId = token;
				break;
			}
			case 3: {
				try {
					severity = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			case 4: {
				try {
					code = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			case 5: {
				try {
					bundleState = Integer.parseInt(token);
				} catch (NumberFormatException nfe) {
					appendToken(dateBuffer, token);
				}
				break;
			}
			default: {
				appendToken(dateBuffer, token);
			}
			}
		}
		Date date = GREGORIAN_SDF.parse(dateBuffer.toString());
		if (date != null) {
			fDate = date;
			fDateString = LOCAL_SDF.format(fDate);
		}
		return depth;
	}

	/**
	 * Adds the given token to the given buffer, adding a space as needed
	 * 
	 * @param buffer
	 * @param token
	 * 
	 * @since 3.6
	 */
	void appendToken(StringBuffer buffer, String token) {
		if (buffer.length() > 0) {
			buffer.append(SPACE);
		}
		buffer.append(token);
	}

	/**
	 * Sets the stack to the given stack value. No validation is performed on the new value.
	 * 
	 * @param stack
	 */
	public void setStack(String stack) {
		this.stack = stack;
	}

	/**
	 * Sets the message to the given message value. No validation is performed on the new value
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public int getBundleStateId() {
		return bundleState;
	}

	public String getBundleState() {
		return getStateName(bundleState);
	}

	// If we want to save the state as a string in the log file
	@SuppressWarnings("unused")
	private int getStateId(String state) {

		int stateId = 0;
		switch (state) {
		case "INSTALLED":
			stateId = Bundle.INSTALLED;
			break;
		case "STARTING":
			stateId = Bundle.STARTING;
			break;
		case "STOPPING":
			stateId = Bundle.STOPPING;
			break;
		case "UNINSTALLED":
			stateId = Bundle.UNINSTALLED;
			break;
		case "RESOLVED":
			stateId = Bundle.RESOLVED;
			break;
		case "ACTIVE":
			stateId = Bundle.ACTIVE;
			break;
		default:
			stateId = 0;
			break;
		}
		return stateId;
	}

	private String getStateName(int state) {

		String typeName = null;
		switch (state) {
		case Bundle.INSTALLED:
			typeName = "INSTALLED";
			break;
		case Bundle.STARTING:
			// typeName = "<<LAZY>>";
			typeName = "STARTING";
			break;
		case Bundle.STOPPING:
			typeName = "STOPPING";
			break;
		case Bundle.UNINSTALLED:
			typeName = "UNINSTALLED";
			break;
		case Bundle.RESOLVED:
			typeName = "RESOLVED";
			break;
		case Bundle.ACTIVE:
			typeName = "ACTIVE";
			break;
		default:
			typeName = "";
			break;
		}
		return typeName;
	}

	/**
	 * Process the given status and sub-statuses to fill this entry
	 * 
	 * @param status
	 */
	private void processStatus(IBundleStatus status, LogSession session) {
		pluginId = status.getPlugin();
		if (status instanceof BundleStatus) {
			bundleState = status.getBundleState();
			bundleTransition = status.getBundleTransition();
		}
		severity = status.getSeverity();
		code = status.getCode();
		fDate = new Date();
		fDateString = LOCAL_SDF.format(fDate);
		message = status.getMessage();
		this.session = session;
		throwable = status.getException();
		if (throwable != null) {
			StringWriter swriter = new StringWriter();
			PrintWriter pwriter = new PrintWriter(swriter);
			throwable.printStackTrace(pwriter);
			pwriter.flush();
			pwriter.close();
			stack = swriter.toString();
		}
		IStatus[] schildren = status.getChildren();
		if (schildren.length > 0) {
			for (int i = 0; i < schildren.length; i++) {
				if (schildren[i] instanceof IBundleStatus) {
					addChild(new BundleLogEntryImpl((IBundleStatus) schildren[i], session));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.views.log.AbstractEntry#write(java.io.PrintWriter)
	 */
	public void write(PrintWriter writer) {
		if (session != null) {
			writer.println(session.getSessionData());
		}
		writer.println(pluginId);
		writer.println(getSeverityText());
		if (fDate != null) {
			writer.println(getDate());
		}
		if (message != null) {
			writer.println(getMessage());
		}
		if (stack != null) {
			writer.println();
			writer.println(stack);
		}
	}
}
