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
package no.javatime.util.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import no.javatime.util.Activator;
import no.javatime.util.messages.exceptions.ViewException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A category is an unique string which is defined when this class is
 * instantiated or one of the predefined categories are used. A category may be
 * true or false.
 * <p>
 * A predefined category string is a key in a a predefined entry in the
 * {@link #CATEGORY_PROPERTIES_FILE_NAME property file} of this class which
 * determines if this category is enabled or disabled. The format of the entry
 * in the property file is <category string>= false | true. It should not be
 * necessary to instantiate {@code Category} if a predefined category is used.
 * <p>
 * New categories may be defined at runtime by creating new instances of
 * {@code Category} and accessed in a static manner. If you use one of the
 * static access methods the last set value is returned for the category
 * specified.
 * <p>
 * This class is typically used in conjunction with one of the getString methods
 * in the different message classes in this package to suppress categories of
 * messages.
 * <p>
 * When instantiating the memory cost is a string object (the category) a
 * boolean, and an entry of the string object and its corresponding boolean
 * value in a category map.
 * 
 * @see Message#getString(String, Category, Object...)
 */
public class Category extends Message {

	/**
	 * Name of the properties file. File extension is implicit
	 */
	public static final String CATEGORY_PROPERTIES_FILE_NAME = "no.javatime.util.messages.categories"; //$NON-NLS-1$

	private static ResourceBundle categoryBundle;
	static {
		try {
			categoryBundle = ResourceBundle.getBundle(CATEGORY_PROPERTIES_FILE_NAME);
		} catch (MissingResourceException e) {
			throw new ViewException(e, "resource_exception", Category.class.getName(),
					CATEGORY_PROPERTIES_FILE_NAME);
		}
	}

	/**
	 * Debug flag used stand alone or in combination with the different
	 * categories. Use of the <code>DEBUG</code> directive will result in dead
	 * code eliminated by the flow analyzer.
	 * 
	 * Eg. if (Category.DEBUG)
	 * TraceMessage.getInstance().getString("update_bundle", bundlesCategory,
	 * bundles.getSymbolicName());
	 * 
	 * where bundlesCategory e.g is: private Category bundlesCategory = new
	 * Category(Category.bundles);
	 * 
	 */
	public static final Boolean DEBUG;
	static {
		DEBUG = "true".equalsIgnoreCase(get("debug", categoryBundle));
	}

	// Trace
	public static final String build = "build";
	public static final String dag = "dag";
	public static final String listeners = "listeners";
	public static final String binpath = "binpath";
	public static final String progressBar = "progressBar";
	public static final String logService = "logService";	
	public static final String contexts = "contexts";	
	public static final String fsm = "fsm";	

	// Dependency dialog
	public static final String partialGraphOnActivate = "partialGraphOnActivate";
	public static final String partialGraphOnStart = "partialGraphOnStart";
	public static final String partialGraphOnDeactivate = "partialGraphOnDeactivate";
	public static final String partialGraphOnStop = "partialGraphOnStop";
	public static final String requiringOnActivate = "requiringOnActivate";
	public static final String requiringOnStart = "requiringOnStart";
	public static final String providingOnStart = "providingOnStart";
	public static final String requiringOnStop = "requiringOnStop";
	public static final String providingOnStop = "providingOnStop";
	public static final String providingOnDeactivate = "providingOnDeactivate";
	// Checked menu entries in Bundle main menu
	public static final String updateClassPathOnActivate = "updateClassPathOnActivate";
	public static final String eagerActivation = "eagerActivation";
	public static final String autoDependency = "autoDependency";
	public static final String autoRefresh = "autoRefresh";
	public static final String autoUpdate = "autoUpdate";
	public static final String uiContributors = "uiContributors";
	public static final String deactivateOnExit = "deactivateOnExit";
	// InPlace Console tool bar items
	public static final String bundleEvents = "bundleEvents";
	public static final String bundleOperations = "bundleOperations";
	public static final String bindingMessages = "bindingMessages";
	public static final String infoMessages = "infoMessages";
	public static final String systemOut = "systemOut";
	
	// Extended dynamic prefixes to error, warning, exception, log and user messages
	public static final String dynamicPrefix = "dynamicPrefix";
	
	public static final Boolean enable = true;
	public static final Boolean disable = false;

	private static Map<String, Boolean> categories = new HashMap<String, Boolean>();
	static {
		// Trace builder
		categories.put(build, (get(build, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Trace context bindings
		categories.put(contexts, (get(contexts, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Trace traversal of bundle dependencies
		categories.put(dag, (get(dag, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Trace resource change events
		categories.put(listeners, (get(listeners, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Trace insertion, deletion and updates of bin in the bundle class path header
		categories.put(binpath, (get(binpath, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Trace progress dialog behavior
		categories.put(progressBar, (get(progressBar, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// State machine trace
		categories.put(fsm, (get(fsm, categoryBundle).startsWith(enable.toString()) ? enable : disable));		
		// Use OSGI log service for tracing
		categories.put(logService, (get(logService, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Dependency dialog
		categories.put(partialGraphOnActivate, (get(partialGraphOnActivate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(partialGraphOnStart, (get(partialGraphOnStart, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(partialGraphOnDeactivate, (get(partialGraphOnDeactivate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(partialGraphOnStop, (get(partialGraphOnStop, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(requiringOnActivate, (get(requiringOnActivate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(requiringOnStart, (get(requiringOnStart, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(providingOnStart, (get(providingOnStart, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(requiringOnStop, (get(requiringOnStop, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(providingOnStop, (get(providingOnStop, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(providingOnDeactivate, (get(providingOnDeactivate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// Checked main menu entries
		categories.put(updateClassPathOnActivate, (get(updateClassPathOnActivate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(eagerActivation, (get(eagerActivation, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(autoDependency, (get(autoDependency, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(autoRefresh, (get(autoRefresh, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(autoUpdate, (get(autoUpdate, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(deactivateOnExit, (get(deactivateOnExit, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(uiContributors, (get(uiContributors, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		// InPlace Console context menu tool bar items
		categories.put(bundleEvents, (get(bundleEvents, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(bundleOperations, (get(bundleOperations, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(bindingMessages, (get(bindingMessages, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(infoMessages, (get(infoMessages, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(systemOut, (get(systemOut, categoryBundle).startsWith(enable.toString()) ? enable : disable));
		categories.put(dynamicPrefix, (get(dynamicPrefix, categoryBundle).startsWith(enable.toString()) ? enable : disable));
	}
	
	private String key;
	private Boolean value;

	/*
	 * Substituting the declaration of instance and the method #getInstance()
	 * with:
	 * 
	 * public final static Message INSTANCE = new Message();
	 * 
	 * gives better performance but is rigorous to changes
	 */
	private static Category instance = null;

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>Message</code>
	 */
	public synchronized static Category getInstance() {
		if (instance == null) {
			instance = new Category();
		}
		return instance;
	}


	private Category() {
		initDevice(categoryBundle);
		initPrefix(categoryBundle);
		ignoreInStackFrame(Category.class.getName());
	}

	public Category(String category) {
		initDevice(categoryBundle);
		initPrefix(categoryBundle);
		ignoreInStackFrame(Category.class.getName());
		if (!categories.containsKey(category)) {
			categories.put(category, true);
		}
		value = categories.get(category);
		this.key = category;
	}

	public Category(String category, Boolean enabled) {
		initDevice(categoryBundle);
		initPrefix(categoryBundle);
		ignoreInStackFrame(Category.class.getName());
		categories.put(category, enabled);
		this.value = enabled;
		this.key = category;
	}

	/**
	 * @param category the category to set
	 */
	public synchronized void setCategory(String category) {
		if (!categories.containsKey(category)) {
			categories.put(category, true);
		}
		value = categories.get(category);
		this.key = category;
	}

	/**
	 * @see #isEnabled()
	 */
	public synchronized Boolean getState() {
		return value;
	}

	public synchronized void setState(Boolean state) {
		categories.put(key, state);
		value = state;
	}

	public synchronized Boolean isEnabled() {
		return isEnabled(key);
	}

	public synchronized Boolean containsCategory() {
		return categories.containsKey(key);
	}

	public synchronized Boolean resetState(String category) {
		value = categories.put(category, (getString(category).startsWith(enable.toString()) ? enable : disable));
		if (null == value) {
			// TODO throw exception here and change resretState() method.
			value = false;
		}
		return categories.get(category);
	}

	public synchronized void resetState() {
		for (String category : categories.keySet()) {
			categories.put(category, (getString(category).startsWith(enable.toString()) ? enable
					: disable));
		}
		value = categories.get(key);
	}

	/**
	 * @see #isEnabled(String)
	 */
	public synchronized static Boolean getState(String category) {
		Boolean val = categories.get(category);
		if (null == val) {
			return Boolean.FALSE;
		}
		return val;
	}
	
	/**
	 * Sets the state of a category
	 * @param category to assign the new value to
	 * @param state value to set for the specified category
	 * @return previous value or null if category does not exist
	 */
	public synchronized static Boolean setState(String category, Boolean state) {
		return categories.put(category, state);
	}

	/**
	 * Checks whether a category exists and returns its trace state. A disabled
	 * state means that trace messages of this category should be suppressed.
	 * 
	 * @param category a category name
	 * @return If the category exist and is enabled, true is returned. If the
	 *         category does not exist or it exist and is disabled false is
	 *         returned
	 */
	public synchronized static Boolean isEnabled(String category) {
		Boolean value = categories.get(category);
		if (null == value)
			return disable;
		else
			return value;
	}

	public synchronized static Boolean containsCategory(String category) {
		return categories.containsKey(category);
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getString(final String key) {
		return getString(key, categoryBundle, CATEGORY_PROPERTIES_FILE_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrefixMsg(String extendedPrefix) {
		return getPrefixMsg(categoryBundle, extendedPrefix);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputConsole(String key, String msg) {
		return outputConsole(key, msg, categoryBundle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String outputView(String key, String msg) {
		return outputView(key, msg, categoryBundle);
	}

	/**
	 * Save various setting through the preference service
	 * @param flush true to save settings to storage
	 */
	static public void savePluginSettings(Boolean flush) {
		// Saves plug-in preferences at the workspace level
		IEclipsePreferences prefs =
				//Platform.getPreferencesService().getRootNode().node(Plugin.PLUGIN_PREFEERENCES_SCOPE).node(PLUGIN_ID);
				InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		if (null == prefs) {
			return;
		}
		for (String key : categories.keySet()) {
			Boolean value = Category.getState(key);
			prefs.putBoolean(key, value);
		}
		// Preferences are automatically flushed during "super.stop()".
		try {
			if (flush) {
				prefs.flush();
			}
		} catch(BackingStoreException e) {
			// Ignore
		}
	}

	/**
	 * Restore settings through the preference service
	 * @param sync true to prevent others changing the settings
	 */
	static public void loadPluginSettings(Boolean sync) {

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		if (null == prefs) {
			return;
		}
		// Call prefs.sync() to prevent others changing the settings
		try {
			if (sync) {
				prefs.sync();
			}
		} catch(BackingStoreException e) {
		}
		for (String key : categories.keySet()) {
			Boolean defValue = Category.getState(key);
			setState(key, prefs.getBoolean(key, defValue));
		}
	}

}
