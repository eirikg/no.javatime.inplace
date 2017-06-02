package no.javatime.inplace.dl.preferences.intface;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Wrapper of {@link org.osgi.service.prefs.Preferences}
 * <p>
 * Not in use. Intended to be used as an interface with the 
 * options service interface
 */
public interface PreferencesStore {

	public void put(String key, String value);

	public String get(String key, String def);

	public void remove(String key);

	public void clear() throws BackingStoreException;

	public void putInt(String key, int value);

	public int getInt(String key, int def);

	public void putLong(String key, long value);

	public long getLong(String key, long def);

	public void putBoolean(String key, boolean value);

	public boolean getBoolean(String key, boolean def);

	public void putFloat(String key, float value);

	public float getFloat(String key, float def);

	public void putDouble(String key, double value);

	public double getDouble(String key, double def);

	public void putByteArray(String key, byte[] value);

	public byte[] getByteArray(String key, byte[] def);

	public String[] keys() throws BackingStoreException;

	public String[] childrenNames() throws BackingStoreException;

	public Preferences parent();

	public Preferences node(String pathName);

	public boolean nodeExists(String pathName)
			throws BackingStoreException;

	public void removeNode() throws BackingStoreException;

	public String name();

	public String absolutePath();

	public void flush() throws BackingStoreException;

	public void sync() throws BackingStoreException;

}