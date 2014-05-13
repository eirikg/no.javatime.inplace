package no.javatime.inplace.ui.extender;

import no.javatime.inplace.bundlemanager.InPlaceException;
/**
 * Create an extension for a given interface.
 *
 * @param <T>
 */
public class Extension<T> {

	/**
	 * The interface class of this extension
	 */
	private Class<T> intFace;

	/**
	 * The extender this extension is part of
	 */
	private Extender<T> extender;
	
	public Extension(Class<T> intFace)  throws InPlaceException {
		this.intFace = intFace;
		this.extender = getInstance();
	}
	
	public Extension(String interfaceName)  throws InPlaceException {
		this.extender = Extender.<T>getInstance(interfaceName);
		this.intFace = extender.getExtensionInterface();
	}
	
	/**
	 * Get the service object for this extension.
	 * <p>
	 * If null is returned this usually means that the bundle context 
	 * of the bundle providing the service is no longer valid.
	 * 
	 * @return the service object for for the interface specified
	 * at construction of this extension object or null if no service
	 * is being tracked. 
	 */
	public T getService() {
		return extender.getService();
	}		
	/**
	 * Get the service object for the current extension
	 * @return the service object or null if no service is being tracked. This usually means
	 * that the bundle context of the bundle providing the service is no longer valid.
	 */
	
	/**
	 * Get the extender which this extension is part of
	 * @return the extender instance representing this extension 
	 * @throws InPlaceException
	 */
	public Extender<T> getInstance() throws InPlaceException {
		this.extender = Extender.<T>getInstance(intFace.getName());
		return extender;
	}

}
