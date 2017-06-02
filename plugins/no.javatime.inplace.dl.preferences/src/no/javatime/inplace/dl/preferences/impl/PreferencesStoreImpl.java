package no.javatime.inplace.dl.preferences.impl;

import no.javatime.inplace.dl.preferences.intface.PreferencesStore;
import no.javatime.inplace.dl.preferences.service.PreferencesServiceStore;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Implementation of the preference store service interface
 * <p>
 * Not in use. Intended to be used as an interface with the 
 * options service interface
 * <p>
 * Uses OSGI-INF/options.xml
 */
public class PreferencesStoreImpl implements PreferencesStore {

	private final Preferences wrapper;
	
	public PreferencesStoreImpl() {
		wrapper = PreferencesServiceStore.getPreferences();
	}
	
	protected Preferences getSysPrefs() {
		return PreferencesServiceStore.getPreferences();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#put(java.lang.String, java.lang.String)
	 */
	@Override
	public void put(String key, String value) {
		wrapper.put(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#get(java.lang.String, java.lang.String)
	 */
	@Override
	public String get(String key, String def) {
		return wrapper.get(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#remove(java.lang.String)
	 */
	@Override
	public void remove(String key) {
		wrapper.remove(key);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#clear()
	 */
	@Override
	public void clear() throws BackingStoreException {
		wrapper.clear();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putInt(java.lang.String, int)
	 */
	@Override
	public void putInt(String key, int value) {
		wrapper.putInt(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getInt(java.lang.String, int)
	 */
	@Override
	public int getInt(String key, int def) {
		return wrapper.getInt(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putLong(java.lang.String, long)
	 */
	@Override
	public void putLong(String key, long value) {
		wrapper.putLong(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getLong(java.lang.String, long)
	 */
	@Override
	public long getLong(String key, long def) {
		return wrapper.getLong(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putBoolean(java.lang.String, boolean)
	 */
	@Override
	public void putBoolean(String key, boolean value) {
		wrapper.putBoolean(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getBoolean(java.lang.String, boolean)
	 */
	@Override
	public boolean getBoolean(String key, boolean def) {
		return wrapper.getBoolean(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putFloat(java.lang.String, float)
	 */
	@Override
	public void putFloat(String key, float value) {
		wrapper.putFloat(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getFloat(java.lang.String, float)
	 */
	@Override
	public float getFloat(String key, float def) {
		return wrapper.getFloat(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putDouble(java.lang.String, double)
	 */
	@Override
	public void putDouble(String key, double value) {
		wrapper.putDouble(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getDouble(java.lang.String, double)
	 */
	@Override
	public double getDouble(String key, double def) {
		return wrapper.getDouble(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#putByteArray(java.lang.String, byte[])
	 */
	@Override
	public void putByteArray(String key, byte[] value) {
		wrapper.putByteArray(key, value);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#getByteArray(java.lang.String, byte[])
	 */
	@Override
	public byte[] getByteArray(String key, byte[] def) {
		return wrapper.getByteArray(key, def);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#keys()
	 */
	@Override
	public String[] keys() throws BackingStoreException {
		return wrapper.keys();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#childrenNames()
	 */
	@Override
	public String[] childrenNames() throws BackingStoreException {
		return wrapper.childrenNames();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#parent()
	 */
	@Override
	public Preferences parent() {
		return wrapper.parent();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#node(java.lang.String)
	 */
	@Override
	public Preferences node(String pathName) {
		return wrapper.node(pathName);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#nodeExists(java.lang.String)
	 */
	@Override
	public boolean nodeExists(String pathName) throws BackingStoreException {
		return wrapper.nodeExists(pathName);
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#removeNode()
	 */
	@Override
	public void removeNode() throws BackingStoreException {
		wrapper.removeNode();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#name()
	 */
	@Override
	public String name() {
		return wrapper.name();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#absolutePath()
	 */
	@Override
	public String absolutePath() {
		return wrapper.absolutePath();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#flush()
	 */
	@Override
	public void flush() throws BackingStoreException {
		wrapper.flush();
	}

	/* (non-Javadoc)
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#sync()
	 */
	@Override
	public void sync() throws BackingStoreException {
		wrapper.sync();
	}

}
