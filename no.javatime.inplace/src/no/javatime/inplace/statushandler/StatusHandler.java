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

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.statushandlers.StatusManager.INotificationTypes;
import org.eclipse.ui.statushandlers.WorkbenchErrorHandler;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.extender.status.BundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Overrides logging of status of type {@link IBundleStatus}, set interrupt and cancel status on the running
 * bundle job when fatal errors occurs and send customized messages if undefined contexts for action sets
 * related to dynamic bundles occur. Undefined contexts are handled in {@link ActionSetContexts} as a
 * workaround for known context bugs. A safety mechanism is to output any missed undefined context here.
 * <p>
 * The command for toggling line break points is never defined when bundles are resolved and is suppressed in
 * the status handler. This undefined context is not known to have any influence on the behavior of the toggle
 * line break point command. It uses the same context definition as toggle break point.
 * 
 */
public class StatusHandler extends WorkbenchErrorHandler {

	// This command is never defined, and the logging is in ExternalActionManager#isActive(String commandId)
	static private String undefinedToggleLineBrakPoint = "The command (\"org.eclipse.debug.ui.commands.ToggleLineBreakpoint\") is undefined";
	static private String internalError = "An internal error has occurred."; // java.lang.OutOfMemoryError: java
																																						// heap space
	static private Set<String> contextErrors = new HashSet<String>();
	static {
		// Failed to execute runnable (org.eclipse.core.runtime.InvalidRegistryObjectException: Invalid registry
		// object)
		// Cannot get the parent identifier from an undefined context. org.eclipse.jdt.ui.CodingActionSet
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

	@Override
	public void handle(StatusAdapter statusAdapter, int style) {

		StatusAdapter[] adapters = new StatusAdapter[] { statusAdapter };
		StatusManager.getManager().fireNotification(INotificationTypes.HANDLED, adapters);
		IStatus status = statusAdapter.getStatus();
		StatusManager.getManager().addLoggedStatus(status);
		if (isContextError(status, style)) {
			return;
		}
		if (!status.isOK()) {
			Throwable t = status.getException();
			if (null != t && t instanceof VirtualMachineError) {
				// Fatal error from other sources in the workbench. 
				// Interrupt job an send additional messages to message view
				interruptBundleJob();
				if (!(status instanceof BundleStatus)) {
					ErrorMessage.getInstance().handleMessage(status.getMessage());
					ErrorMessage.getInstance().handleMessage(t.getMessage());
					String info = "You can use the System.exit button in the Bundle Console";
					ErrorMessage.getInstance().handleMessage(info);
				}
			}
			if (status instanceof BundleStatus) {
				// Get the bundle status from a bundle job
				BundleStatus bundleStatus = (BundleStatus) status;
				// Send a message to the message view
				if ((style & (StatusManager.LOG)) != 0) {
					if (bundleStatus.hasStatus(StatusCode.ERROR)) {
						ErrorMessage.getInstance().handleMessage(status.getMessage());
					} else if (bundleStatus.hasStatus(StatusCode.WARNING)
							|| bundleStatus.hasStatus(StatusCode.BUILDERROR)) {
						WarnMessage.getInstance().handleMessage(status.getMessage());
					} else if (bundleStatus.hasStatus(StatusCode.CANCEL)) {
						UserMessage.getInstance().handleMessage(status.getMessage());
					} else if (bundleStatus.hasStatus(StatusCode.INFO)) {
						UserMessage.getInstance().handleMessage(status.getMessage());
					}
				}
				InPlace.get().getLog().log(status);
			} else {
				super.handle(statusAdapter, style);
			}
		} else {
			InPlace.get().getLog().log(status);
		}
	}

	/**
	 * Set the interrupt flag and send cancel to the current bundle job running
	 * 
	 * @return true if interrupt flag is set and cancel message is sent to the current job. Otherwise false.
	 */
	private boolean interruptBundleJob() {

		BundleJob job = OpenProjectHandler.getRunningBundleJob();
		if (null != job) {
			job.cancel();
			Thread thread = job.getThread();
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

	private boolean isContextError(IStatus status, int style) {
		String msg = status.getMessage();
		Throwable exception = status.getException();
		if (null != exception && (style & StatusManager.LOG) == StatusManager.LOG) {
			if (exception instanceof NotDefinedException) {
				if (contextErrors.contains(msg)) {
					if (Category.getState(Category.bindingMessages)) {
						// Also related to Bug 279332
						String currentPrefix = ExceptionMessage.getInstance().setPrefixMsg("[Bug 295662 - Contexts] ");
						InPlace.get().getLog().log(
								new Status(IStatus.ERROR, InPlace.PLUGIN_ID, msg, exception));
						ExceptionMessage.getInstance().setPrefixMsg(currentPrefix);
					}
					return true;
				}
			} else if (exception instanceof Exception) {
				if (contextErrors.contains(msg)) {
					if (Category.getState(Category.bindingMessages)) {
						String currentprefix = ExceptionMessage.getInstance().setPrefixMsg("[Bug 295662 - Contexts] ");
						InPlace.get().getLog().log(
								new Status(IStatus.ERROR, InPlace.PLUGIN_ID, msg, exception));
						ExceptionMessage.getInstance().setPrefixMsg(currentprefix);
					}
					return true;
				}
			}
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
