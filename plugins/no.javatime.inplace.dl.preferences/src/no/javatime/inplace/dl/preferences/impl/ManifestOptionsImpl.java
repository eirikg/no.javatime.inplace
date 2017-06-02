package no.javatime.inplace.dl.preferences.impl;

import no.javatime.inplace.dl.preferences.intface.ManifestOptions;
import no.javatime.inplace.dl.preferences.service.PreferencesServiceStore;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Service implementation for access and flushing manifest options
 */
public class ManifestOptionsImpl implements ManifestOptions {

	private static final boolean defIsUpdateDefaultOutputFolder = true;
	private final static boolean defIsEagerOnActivate = true;

	protected BundleContext bundleContext;
	private Preferences wrapper;

	public ManifestOptionsImpl() {
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
	public boolean isUpdateDefaultOutPutFolder() {
		return getPrefs().getBoolean(IS_UPDATE_DEFAULT_OUTPUT_FOLDER, getDefaultUpdateDefaultOutPutFolder());
	}

	@Override
	public boolean getDefaultUpdateDefaultOutPutFolder() {
		return defIsUpdateDefaultOutputFolder;
	}

	@Override
	public void setIsUpdateDefaultOutPutFolder(boolean updateDefaultOutputFolder) {
		getPrefs().putBoolean(IS_UPDATE_DEFAULT_OUTPUT_FOLDER, updateDefaultOutputFolder);
	}

	@Override
	public boolean isEagerOnActivate() {
		return getPrefs().getBoolean(IS_EAGER_ON_ACTIVATE, getDefaultIsEagerOnActivate());
	}

	@Override
	public boolean getDefaultIsEagerOnActivate() {
		return defIsEagerOnActivate;
	}

	@Override
	public void setIsEagerOnActivate(boolean eager) {
		getPrefs().putBoolean(IS_EAGER_ON_ACTIVATE, eager);
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
