package no.javatime.inplace.dl.preferences.impl;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.dl.preferences.service.PreferencesServiceStore;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Service implementation for access and flushing manifest options
 */
public class MessageOptionsImpl implements MessageOptions {

	private static final boolean defIsBundleEvents = false;
	private final static boolean defIsBundleOperations = false;
	private final static boolean defIsInfoMesages = false;

	protected BundleContext bundleContext;
	private Preferences wrapper;

	public MessageOptionsImpl() {
		bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	}

	/**
	 * Activate method from Declarative service. DS is disabled. See comments in activator class
	 */
	protected void activate(ComponentContext context) {
		bundleContext = context.getBundleContext();
	}

	/**
	 * Deactivate method from Declarative Service DS is disabled. See comments in activator class
	 */
	protected void deactivate(ComponentContext context) {
		bundleContext = null;
	}

	protected Preferences getPrefs() {
		if (null == wrapper) {
			wrapper = PreferencesServiceStore.getPreferences();
		}
		return wrapper;
	}

	@Override
	public boolean isBundleEvents() {
		return getPrefs().getBoolean(IS_BUNDLE_EVENTS, getDefaultBundleEvents());
	}

	@Override
	public boolean getDefaultBundleEvents() {
		return defIsBundleEvents;
	}

	@Override
	public void setIsBundleEvents(boolean bundleEvents) {
		getPrefs().putBoolean(IS_BUNDLE_EVENTS, bundleEvents);		
	}

	@Override
	public boolean isBundleOperations() {
		return getPrefs().getBoolean(IS_BUNDLE_OPERATIONS, getDefaultBundleOperations());
	}

	@Override
	public boolean getDefaultBundleOperations() {
		return defIsBundleOperations;
	}

	@Override
	public void setIsBundleOperations(boolean bundleOperations) {
		getPrefs().putBoolean(IS_BUNDLE_OPERATIONS, bundleOperations);		
	}

	@Override
	public boolean isInfoMessages() {
		return getPrefs().getBoolean(IS_INFO_MESSAGES, getDefaultInfoMessages());
	}

	@Override
	public boolean getDefaultInfoMessages() {
		return defIsInfoMesages;
	}

	@Override
	public void setIsInfoMessages(boolean infomessages) {
		getPrefs().putBoolean(IS_INFO_MESSAGES, infomessages);		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#flush()
	 */
	@Override
	public void flush() throws BackingStoreException {
		getPrefs().flush();
	}
}
