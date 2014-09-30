package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.IExtension;

import org.osgi.framework.Bundle;


/**
 * Create an extension for a given interface.
 *
 * @param <T>
 */
public class Extension<T> implements IExtension<T> {

	/**
	 * The interface class of this extension
	 */
	private Class<T> intFace;

	/**
	 * The extender this extension is part of
	 */
	private Extender<T> extender;
	
	public Extension(Class<T> intFace)  throws ExtenderException {
		this.intFace = intFace;
		this.extender = getInstance();
	}

	public Extension(String interfaceName)  throws ExtenderException {
		this.extender = Extender.<T>getInstance(interfaceName);
		this.intFace = extender.getExtensionInterface();
	}

	public T getService(Bundle bundle) {		
		return extender.getService(bundle);
	}		

	public T getService() {
		return extender.getService();
	}		
	
	public Extender<T> getInstance() throws ExtenderException {
		this.extender = Extender.<T>getInstance(intFace.getName());
		return extender;
	}
}
