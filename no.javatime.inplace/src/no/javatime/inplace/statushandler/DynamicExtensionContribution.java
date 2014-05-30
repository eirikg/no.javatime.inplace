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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.extender.status.BundleStatus;
import no.javatime.inplace.extender.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.UserMessage;

/**
 * Supports dynamic addition and removal of extensions.
 * <p>
 * Adds a custom status handler extension with binding to the current Eclipse product id.
 * The eclipse.product entry in config.ini is used to define the custom status handler if present. 
 * The status handler may also be specified as command line parameter to Eclipse: 
 * <p>
 * -statushandler no.javatime.inplace.statushandler 
 * <p>
 * If both the product key and the command line parameter is missing, use the standard status handler.
 * <p>
 * Adds command extension for org.eclipse.debug.ui.commands.ToggleLineBreakpoint. This is a workaround for Bug 295662.
 * <p>
 * This class may also be used for adding and removing extensions dynamically.
 */
@SuppressWarnings("restriction")
public class DynamicExtensionContribution {


	final static public String statusHandlerExtensionPointId = "org.eclipse.ui.statusHandlers";
	// This is also the product id
	final static public String statusHandlerExtensionId = "no.javatime.inplace.statushandler";

	final static public String eclipseUICommandsExtensionPointId = "org.eclipse.ui.commands";
	final static public String eclipseToggleLineBreakPintExtension = "org.eclipse.debug.ui.commands.ToggleLineBreakpoint";
	final static private String defaultProductId = "no.javatime.inplace.product";
	
	public final static DynamicExtensionContribution INSTANCE = new DynamicExtensionContribution();

	private DynamicExtensionContribution() {
	}
	

	/**
	 * Workaround for a bug, when bundles are resolved dynamically. Stack trace message is:
	 * "The command ("org.eclipse.debug.ui.commands.ToggleLineBreakpoint") is undefined"
	 * <p>
	 * Adds a command extension for: org.eclipse.debug.ui.commands.ToggleLineBreakpoint. The reference to the
	 * identifier in the definition of action "org.eclipse.debug.ui.commands.ToggleLineBreakpoint" is missing.
	 * in action set "org.eclipseui.actionSets".
	 * <p>
	 * Warning in "org.eclipse.debug.ui/plugin.xml" line 253 (Ver. 3.8.0): Referenced identifier
	 * 'org.eclipse.debug.ui.commands.ToggleLineBreakpoint' in attribute 'definitionId' cannot be found.
	 * 
	 * @return true if the extension was added, otherwise false
	 */
	public Boolean addToggleLineBreakPointCommand() {

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<?eclipse version=\"3.4\"?>");
		sb.append("<plugin>");
		sb.append("<extension");
		sb.append("	point=\"");
		sb.append(eclipseUICommandsExtensionPointId);
		sb.append("\">");
		sb.append("<command");
		sb.append("	name=\"");
		sb.append("Toggle &amp;Line Breakpoint");
		sb.append("\"");
		sb.append("	description=\"");
		sb.append("Creates or removes a line breakpoint");
		sb.append("\"");
		sb.append("	categoryId=\"");
		sb.append("org.eclipse.debug.ui.category.run");
		sb.append("\"");
		sb.append("	id=\"");
		sb.append(eclipseToggleLineBreakPintExtension);
		sb.append("\">");
		sb.append("</command>");
		sb.append("</extension>");
		sb.append("</plugin>");
		Boolean extAdded = addExtension(sb.toString());
		if (Category.getState(Category.infoMessages)) {
			if (extAdded) {
				UserMessage.getInstance().getString("debug_line_brakpoint_command",
						eclipseToggleLineBreakPintExtension);
			}
		}
		if (!extAdded) {
			Bundle bundle = InPlace.get().getBundle();
			String msg = ErrorMessage.getInstance().formatString("failed_to_add_status_debug_line_breakpoint_command",
					(null == bundle) ? null : bundle.getSymbolicName());
			StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
		return extAdded;
	}

	/**
	 * Adds a custom status handler extension with binding to the current Eclipse product id using this plug-in
	 * and support for the status handler parameter to Eclipse: -statushandler no.javatime.inplace.status. If
	 * not present use the eclipse.product entry in config.ini if present. Otherwise use the standard status
	 * handler.
	 * 
	 * @return true if the extension was added, otherwise false
	 */
	public Boolean addCustomStatusHandler() {

		Boolean extAdded = true;
		// Product binding identifier for the current Eclipse product
		String productId = Platform.getProduct() != null ? Platform.getProduct().getId() : defaultProductId;
		if (extAdded) {
			String statusHandlerId = formatStatusHandlerId();
			String statusHandlerClassName = StatusHandler.class.getName();
			StringBuffer sb = new StringBuffer();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			sb.append("<?eclipse version=\"3.4\"?>");
			sb.append("<plugin>");
			sb.append("<extension");
			sb.append("	id=");
			sb.append(statusHandlerId);
			sb.append("	name=");
			sb.append(statusHandlerId);
			sb.append("	point=\"");
			sb.append(statusHandlerExtensionPointId);
			sb.append("\">");
			sb.append("<statusHandler");
			sb.append("	class=");
			sb.append('\"');
			sb.append(statusHandlerClassName);
			sb.append('\"');
			sb.append("	id=");
			sb.append(statusHandlerId);
			sb.append('>');
			sb.append("</statusHandler>");
			sb.append(formatProductBinding(productId));
			// Add support for the -statushandler command line switch to Eclipse
			// Usage: -statushandler no.javatime.inplace.statushandler
			sb.append(formatProductBinding(statusHandlerExtensionId));
			sb.append(formatProductBinding(defaultProductId));
			sb.append("</extension>");
			sb.append("</plugin>");
			extAdded = addExtension(sb.toString());
			if (!extAdded) {
				// If adding this customized status handler fails, use the standard which will display a dialog
				Bundle bundle = InPlace.get().getBundle();
				String msg = ErrorMessage.getInstance().formatString("failed_to_add_status_handler_contribution",
						(null == bundle) ? null : bundle.getSymbolicName());
				StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		}
		if (Category.getState(Category.infoMessages)) {
			if (extAdded) {
				if (!productId.equals(defaultProductId)) {
					UserMessage.getInstance().getString("customized_status_handler", statusHandlerExtensionId, productId);
				} else if (statusFromCommandline()) {
					UserMessage.getInstance().getString("customized_status_handler_cmd_line", statusHandlerExtensionId, productId);					
				} else {
					UserMessage.getInstance().getString("standard_status_handler");
					UserMessage.getInstance().getString("use_customized_status_handler");
				}
			}
		}
		return extAdded;
	}
	
	private Boolean statusFromCommandline() {
		String[] args = Platform.getCommandLineArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(statusHandlerExtensionId)) {
				return true;
			}
		}
		return false;
	}
	
	private String formatProductBinding(String productId) {
		StringBuffer sb = new StringBuffer();
		sb.append("<statusHandlerProductBinding");
		sb.append("	handlerId=");
		sb.append(formatStatusHandlerId());
		sb.append("	productId=");
		sb.append('\"');
		sb.append(productId);
		sb.append('\"');
		sb.append('>');
		sb.append("</statusHandlerProductBinding>");
		return sb.toString();
	}

	private String formatStatusHandlerId() {
		StringBuffer statusHandlerId = new StringBuffer();
		statusHandlerId.append('\"');
		statusHandlerId.append(statusHandlerExtensionId);
		statusHandlerId.append('\"');
		return statusHandlerId.toString();
	}

	/**
	 * Adds an extension specified in the string parameter. The extension must be a complete containing the xml
	 * and plugin tags.
	 * 
	 * @param xmlsrc a complete xml src string
	 * @return true if the extension was added, otherwise false.
	 */
	public Boolean addExtension(String xmlsrc) {

		Boolean extAdded = true;
		Bundle bundle = InPlace.get().getBundle();
		try {
			IExtensionRegistry reg = RegistryFactory.getRegistry();
			Object key = ((ExtensionRegistry) reg).getTemporaryUserToken();
			IContributor contributor = ContributorFactoryOSGi.createContributor(bundle);
			ByteArrayInputStream is = new ByteArrayInputStream(xmlsrc.getBytes("UTF-8"));
			if (!reg.addContribution(is, contributor, false, null, null, key)) {
				String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension", 
						bundle.getSymbolicName(), xmlsrc);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
				extAdded = false;
			}
		} catch (IllegalArgumentException e) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension",
					bundle.getSymbolicName(), xmlsrc);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			extAdded = false;
		} catch (UnsupportedEncodingException e) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension",
					bundle.getSymbolicName(), xmlsrc);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			extAdded = false;
		}
		return extAdded;
	}

	/**
	 * Adds an extension specified file name parameter. The string path must represent a valid file system path
	 * on the local file system. The extension must be a complete containing the xml and plugin tags.
	 * 
	 * @param xmlsrc xml file name containing the extension to add
	 * @return true if the extension was added, otherwise false.
	 */
	public Boolean addExtensionFromFile(String xmlsrc) {
		// Use Eclipse Dynamic Extension API
		Boolean extAdded = true;
		Bundle bundle = InPlace.get().getBundle();
		try {
			IExtensionRegistry reg = RegistryFactory.getRegistry();
			Object key = ((ExtensionRegistry) reg).getTemporaryUserToken();
			IContributor contributor = ContributorFactoryOSGi.createContributor(bundle);
			InputStream is = FileLocator.openStream(bundle, new Path(xmlsrc), false);
			if (!reg.addContribution(is, contributor, false, null, null, key)) {
				String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension", 
						bundle.getSymbolicName(), xmlsrc);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
				extAdded = false;
			}
		} catch (IllegalArgumentException e) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension",
					bundle.getSymbolicName(), xmlsrc);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			extAdded = false;
		} catch (UnsupportedEncodingException e) {
			String msg = ErrorMessage.getInstance().formatString("failed_to_add_extension",
					bundle.getSymbolicName(), xmlsrc);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			extAdded = false;
		} catch (IOException e) {
			String msg = ErrorMessage.getInstance().formatString("io_exception_status", xmlsrc,
					bundle.getSymbolicName());
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
			extAdded = false;
		}
		return extAdded;
	}

	/**
	 * Removes an extension from an extension point
	 * 
	 * @param extensionPointId id of extension point
	 * @param extensionId id of extension
	 * @return true if removed, otherwise false
	 */
	public Boolean removeExtension(String extensionPointId, String extensionId) {
		Boolean removed = true;
		// use Eclipse Dynamic Extension API
		try {
			IExtensionRegistry reg = RegistryFactory.getRegistry();
			Object token = ((ExtensionRegistry) reg).getTemporaryUserToken();
			IExtension extension = reg.getExtension(extensionPointId, extensionId);
			// Not added or already removed
			if (null == extension) {
				return removed;
			}
			removed = reg.removeExtension(extension, token);
			// It exists but was not removed
			if (!removed) {
				String msg = ErrorMessage.getInstance().formatString("failed_remove_extension", extensionId);
				StatusManager.getManager().handle(new BundleStatus(StatusCode.ERROR, InPlace.PLUGIN_ID, msg),
						StatusManager.LOG);
			}
		} catch (IllegalArgumentException e) {
			String msg = ErrorMessage.getInstance().formatString("failed_remove_extension", extensionId);
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, InPlace.PLUGIN_ID, msg, e),
					StatusManager.LOG);
			removed = false;
		}
		return removed;
	}
}
