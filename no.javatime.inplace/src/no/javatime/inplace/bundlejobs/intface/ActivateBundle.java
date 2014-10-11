package no.javatime.inplace.bundlejobs.intface;

public interface ActivateBundle extends Bundles {

	/**
	 * Set preference for activating bundles according to bundle state in preference store
	 * 
	 * @param useStoredState true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public void setUseStoredState(Boolean useStoredState);

	/**
	 * Check if to activate bundles according to bundle state in preference store
	 * 
	 * @return true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public Boolean getUseStoredState();

}