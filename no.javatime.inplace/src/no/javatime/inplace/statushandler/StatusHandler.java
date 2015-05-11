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
package no.javatime.inplace.statushandler;

import java.util.HashSet;
import java.util.Set;

import no.javatime.inplace.Activator;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.dialogs.ResourceStateHandler;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.ExceptionMessage;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.statushandlers.StatusManager.INotificationTypes;
import org.eclipse.ui.statushandlers.WorkbenchErrorHandler;

/**
 * Overrides logging of status object of type {@link IBundleStatus} and forwards status objects of
 * type {@code IStatus} to the standard status handler. Logs any undefined contexts for action sets
 * related to dynamic bundles to the bundle log. Undefined contexts are handled in
 * {@link ActionSetContexts} as a workaround for known context bugs. A safety mechanism is to output
 * any missed undefined context here.
 * <p>
 * The command for toggling line break points is never defined when bundles are resolved. This
 * undefined context is not known to have any influence on the behavior of the toggle line break
 * point command. It uses the same context definition as toggle break point.
 * 
 */
public class StatusHandler extends WorkbenchErrorHandler {

	// This command is never defined, and the logging is in ExternalActionManager#isActive(String
	// commandId)
	static private String undefinedToggleLineBrakPoint = "The command (\"org.eclipse.debug.ui.commands.ToggleLineBreakpoint\") is undefined";
	static private String internalError = "An internal error has occurred."; // java.lang.OutOfMemoryError:
																																						// java
																																						// heap space
	static private Set<String> contextErrors = new HashSet<String>();
	static {
		// Failed to execute runnable (org.eclipse.core.runtime.InvalidRegistryObjectException: Invalid
		// registry
		// object)
		// Cannot get the parent identifier from an undefined context.
		// org.eclipse.jdt.ui.CodingActionSet
		contextErrors.add("Undefined context while filtering dialog/window contexts");
		contextErrors.add(undefinedToggleLineBrakPoint);
	}

	public StatusHandler() {
	}

	public boolean supportsNotification(int type) {
		if (INotificationTypes.HANDLED == type)
			return true;
		else
			return false;
	}

	/**
	 * Log status objects of type {@code IBundleStatus} to the bundle log and all errors to the error
	 * log. Other errors of type {@code IStatus} are forwarded to the standard status handler
	 * <p>
	 * A special case is undefined context errors, which is sent to the bundle log.
	 */
	@Override
	public void handle(StatusAdapter statusAdapter, int style) {

		IStatus status = statusAdapter.getStatus();
		StatusAdapter[] adapters = new StatusAdapter[] { statusAdapter };
		StatusManager.getManager().fireNotification(INotificationTypes.HANDLED, adapters);
		StatusManager.getManager().addLoggedStatus(status);
		if (isContextError(status, style)) {
			return;
		}
		Throwable exception = status.getException();
		if (null != exception && exception instanceof VirtualMachineError) {
			// Interrupt job an send additional messages to the error log
			interruptBundleJob();
			IBundleStatus criticalErrorStatus = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
					exception.getMessage(), exception);
			criticalErrorStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID,
					Msg.SYSTEM_EXIT_INFO));
			criticalErrorStatus.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, status
					.getMessage()));
			Activator.getInstance().getLog().log(criticalErrorStatus);
			return;
		}
		// Only consider bundle status objects
		if (status instanceof IBundleStatus) {
			IBundleStatus bundleStatus = (IBundleStatus) status;
			if (bundleStatus.isMultiStatus()) {
				bundleStatus.setStatusCode();
			}
			try {
				// Also send the error status objects to the bundle log
				if (Activator.getMessageOptionsService().isBundleOperations()) {
					Activator.log(bundleStatus);
				}
			} catch (ExtenderException | BundleLogException e) {
				super.handle(new StatusAdapter(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(),
						e)), style);
			}
			// Send to error log
			if (bundleStatus.getStatusCode() != StatusCode.INFO) {
				super.handle(statusAdapter, style);
			}
		} else {
			// Forward any other type of status objects to the standard error handler
			super.handle(statusAdapter, style);
		}
	}

	/**
	 * Log undefined context errors to the bundle log
	 * 
	 * @param status the status object to log
	 * @param style the standard status handler style constants
	 * @return true if e context error is logged. Otherwise false
	 */
	private boolean isContextError(IStatus status, int style) {

		try {
			Throwable exception = status.getException();
			if (null != exception && (style & StatusManager.LOG) == StatusManager.LOG) {
				if (exception instanceof NotDefinedException || exception instanceof Exception) {
					String msg = status.getMessage();
					if (contextErrors.contains(msg)) {
						if (Activator.getMessageOptionsService().isBundleOperations()) {
							// Also related to Bug 279332
							String bugInfoMsg = Msg.UNDEFINED_CONTEXT_ERROR_TRACE;
							IBundleStatus undefinedContextStatus = new BundleStatus(StatusCode.INFO,
									Activator.PLUGIN_ID, bugInfoMsg);
							IBundleStatus errorStatus = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg,
									exception);
							undefinedContextStatus.add(errorStatus);
							Activator.log(undefinedContextStatus);
						}
						return true;
					}
				}
			}
		} catch (ExtenderException | BundleLogException e) {
			super.handle(new StatusAdapter(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(),
					e)), style);
		}
		return false;
	}

	/**
	 * Set the interrupt flag and send cancel to the current bundle job running
	 * 
	 * @return true if interrupt flag is set and cancel message is sent to the current job. Otherwise
	 * false.
	 */
	private boolean interruptBundleJob() {

		ResourceStateHandler so = new ResourceStateHandler();
		BundleExecutor bundleExtecutor = so.getRunningBundleJob();
		if (null != bundleExtecutor) {
			bundleExtecutor.getJob().cancel();
			Thread thread = bundleExtecutor.getJob().getThread();
			if (null != thread) {
				// Requires that the user code (e.g. in the start method) is aware of interrupts
				thread.interrupt();
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isInternalError(String msg) {
		if (msg.equals(internalError)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isUndefinedToggleLineBreakpoint(String msg) {
		if (msg.equals(undefinedToggleLineBrakPoint)) {
			return true;
		}
		return false;
	}

	/**
	 * Extracts the extension id from an undefined context exception
	 * 
	 * @param t the undefined context exception
	 * @return the extension id of a not defined context message or null
	 */
	@SuppressWarnings("unused")
	private String extractExtensionId(Throwable t) {
		String start = "Cannot get the parent identifier from an undefined context.";
		for (String exceptionMessage : ExceptionMessage.getInstance().getChaindedExceptionMessages(t)) {
			if (exceptionMessage.startsWith(start)) {
				String extName = exceptionMessage.substring(start.length(), exceptionMessage.length());
				if (extName.length() > 0) {
					return extName.trim();
				}
			}
		}
		return null;
	}
}
