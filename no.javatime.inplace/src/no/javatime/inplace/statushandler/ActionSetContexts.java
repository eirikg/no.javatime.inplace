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

import java.util.HashMap;
import java.util.Map;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.commands.contexts.ContextManagerEvent;
import org.eclipse.core.commands.contexts.IContextManagerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.internal.registry.ActionSetDescriptor;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Defines action sets contexts for plug-ins being resolved dynamically. This is a workaround for [Bug 295662]
 * and Bug[279332].
 * <p>
 * <b>Problem description:</b>
 * <p>
 * Contexts are defined for action sets when loaded at startup and stays defined for the whole session as long
 * as dynamic bundles using the command framework are not involved.
 * <p>
 * When a bundle is activated dynamically during a session, all existing contexts are being undefined, and the
 * new ones, except for action sets, are being defined in
 * {@code org.eclipse.ui.internal.context.ContextPersistence}. In general contexts for action sets, static and
 * dynamic, are being undefined and not defined again in this dynamic resolving process. The result is
 * undefined context exceptions for all action sets.
 * <p>
 * <b>Proposed solution:</b>
 * <p>
 * A solution to this problem is to read and define the contexts for action sets, static and dynamic, in
 * {@code org.eclipse.ui.internal.context.ContextPersistence#readContextsFromRegistry}.
 * <p>
 * Suppressed warnings: Use the internal ActionSetDescriptor class, IActionSetDescriptor and
 * IWorkbenchRegistryConstants interfaces
 * <p>
 */
@SuppressWarnings("restriction")
public class ActionSetContexts implements IContextManagerListener, IExtensionChangeHandler {

	private IContextService contextService;
	/**
	 * All action sets for resolved plug-ins added at session startup and dynamically during a session. Actions
	 * sets are added and removed dynamically when plug-ins are resolved and unresolved respectively
	 */
	private Map<String, IActionSetDescriptor> actionSets = new HashMap<String, IActionSetDescriptor>();

	public Boolean init() {
		contextService = (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);
		if (null == contextService) {
			return false;
		}
		contextService.addContextManagerListener(this);
		// Track additions and removals of action set extensions
		PlatformUI
				.getWorkbench()
				.getExtensionTracker()
				.registerHandler(
						this,
						ExtensionTracker
								.createExtensionPointFilter(new IExtensionPoint[] { getActionSetExtensionPoint() }));
		readInitialStaticActionSets();
		return true;
	}

	/**
	 * Creates action set descriptors for activated plug-ins declaring action sets at session startup. Should be
	 * read before any new bundles are dynamically resolved after startup
	 */
	private void readInitialStaticActionSets() {
		IExtension[] extensions = getActionSetExtensionPoint().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			addActionSetDescriptor(extensions[i]);
		}
	}

	public void dispose() {
		contextService.removeContextManagerListener(this);
	}

	/**
	 * Define a context for a possible undefined context. Consider to use context listener instead to limit the
	 * number of events, despite the number of listeners
	 */
	@Override
	public void contextManagerChanged(ContextManagerEvent contextManagerEvent) {
		String contextId = contextManagerEvent.getContextId();
		// Only consider contexts for action sets
		if (actionSets.containsKey(contextId)) {
			defineContext(contextId);
		}
	}

	/**
	 * Defines the context for the specified context id based on a registered action set descriptor. A warning
	 * is sent to the bundle CONSOLE if the context id does not belong to an action set extension
	 * 
	 * @param contextId to define a context for. Only context id's for action sets are considered
	 * @return true if the context already was defined or has been defined. False if action set is not
	 *         registered
	 */
	public Boolean defineContext(String contextId) {
		try {
			contextService.deferUpdates(true);
			if (null != contextId) {
				Context context = contextService.getContext(contextId);
				if (!context.isDefined()) {
					IActionSetDescriptor actionSetDescriptor = actionSets.get(contextId);
					if (null != actionSetDescriptor) {
						context.define(actionSetDescriptor.getLabel(), actionSetDescriptor.getDescription(),
								"org.eclipse.ui.contexts.actionSet");
						if (Category.DEBUG && Category.getState(Category.contexts))
							TraceMessage.getInstance().getString("define_context", contextId);
					} else {
						String msg = WarnMessage.getInstance().formatString("no_action_set", contextId);
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
								StatusManager.LOG);
						return false;
					}
				}
			}
		} finally {
			contextService.deferUpdates(false);
		}
		return true;
	}

	/**
	 * Registers a new an action set descriptor for this extension. Called by the framework whenever a plug-in
	 * declaring an action set is resolved.
	 */
	@Override
	public void addExtension(IExtensionTracker tracker, IExtension extension) {
		IActionSetDescriptor actionSetDescriptor = addActionSetDescriptor(extension);
		if (null != actionSetDescriptor) {
			if (Category.DEBUG && Category.getState(Category.contexts))
				TraceMessage.getInstance().getString("add_dynamic_context", actionSetDescriptor.getId());
		}
	}

	/**
	 * Remove the registered action set descriptor for this extension. Called by the framework whenever a
	 * plug-in declaring an action set is unresolved.
	 */
	@Override
	public void removeExtension(IExtension extension, Object[] objects) {
		IActionSetDescriptor actionSetDescriptor = removeActionSetDescriptor(extension);
		if (null != actionSetDescriptor) {
			if (Category.DEBUG && Category.getState(Category.contexts))
				TraceMessage.getInstance().getString("remove_dynamic_context", actionSetDescriptor.getId());
		}
	}

	/**
	 * Creates an action set descriptor from the action set extension, and registers it
	 * 
	 * @param extension must be an action set extension
	 * @return the action set descriptor for the specified extension or null if creation fails
	 */
	private IActionSetDescriptor addActionSetDescriptor(IExtension extension) {
		IActionSetDescriptor actionSetDescriptor = null;
		try {
			IConfigurationElement[] configurationElements = extension.getConfigurationElements();
			for (int j = 0; j < configurationElements.length; j++) {
				IConfigurationElement configurationElement = configurationElements[j];
				if (configurationElement.isValid()) {
					try {
						actionSetDescriptor = new ActionSetDescriptor(configurationElement);
						String extensionId = actionSetDescriptor.getId();
						// Add action set for a resolved bundle
						if (null != actionSets.put(extensionId, actionSetDescriptor)) {
							String msg = WarnMessage.getInstance().formatString("duplicate_action_set", extensionId);
							StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
									StatusManager.LOG);
						}
						if (Category.DEBUG && Category.getState(Category.contexts)) {
							Context context = contextService.getContext(extensionId);
							TraceMessage.getInstance().getString("get_action_set_descriptor", extensionId,
									context.isDefined());
						}
					} catch (CoreException e) {
						String msg = ExceptionMessage.getInstance().formatString("create_action_set_descriptor",
								extension.getLabel());
						StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
								StatusManager.LOG);
					}
				}
			}
		} catch (InvalidRegistryObjectException e) {
			String msg = ExceptionMessage.getInstance().formatString("get_action_set_descriptor",
					extension.getLabel());
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
		return actionSetDescriptor;
	}

	/**
	 * Removes a registered action set descriptor for the action set extension.
	 * 
	 * @param extension must be an action set extension
	 * @return the removed action set descriptor for the specified extension or null if no such descriptor
	 */
	private IActionSetDescriptor removeActionSetDescriptor(IExtension extension) {
		IActionSetDescriptor actionSetDescriptor = null;
		try {
			IConfigurationElement[] configurationElements = extension.getConfigurationElements();
			for (int i = 0; i < configurationElements.length; i++) {
				IConfigurationElement configurationElement = configurationElements[i];
				if (configurationElement.isValid()) {
					String extensionId = configurationElement.getAttribute(IWorkbenchRegistryConstants.ATT_ID);
					if (Category.DEBUG && Category.getState(Category.contexts))
						TraceMessage.getInstance().getString("remove_dynamic_context", extensionId);
					// Remove action set for an unresolved bundle
					actionSetDescriptor = actionSets.remove(extensionId);
					if (null == actionSetDescriptor) {
						String msg = WarnMessage.getInstance().formatString("delete_action_set", extensionId);
						StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
								StatusManager.LOG);
					}
				}
			}
		} catch (InvalidRegistryObjectException e) {
			String msg = ExceptionMessage.getInstance().formatString("remove_action_set_descriptor",
					extension.getLabel());
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);

		}
		return actionSetDescriptor;
	}

	/**
	 * Return the action set extension point for the workbench.
	 * 
	 * @return the action set extension point
	 */
	private IExtensionPoint getActionSetExtensionPoint() {
		return Platform.getExtensionRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID,
				IWorkbenchRegistryConstants.PL_ACTION_SETS);
	}

	/**
	 * Return the action set part association extension point.
	 * 
	 * @return the action set part association extension point
	 */
	@SuppressWarnings({ "unused" })
	private IExtensionPoint getActionSetPartAssociationExtensionPoint() {
		return Platform.getExtensionRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID,
				IWorkbenchRegistryConstants.PL_ACTION_SET_PART_ASSOCIATIONS);
	}
}
