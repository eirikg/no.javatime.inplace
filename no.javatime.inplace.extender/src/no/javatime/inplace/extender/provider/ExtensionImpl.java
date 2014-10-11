package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;

import org.osgi.framework.Bundle;


/**
 * Create an extension for a given interface.
 *
 * @param <T>
 */
public class ExtensionImpl<T> implements Extension<T> {

	/**
	 * The interface class of this extension
	 */
	private Class<T> intFace;

	/**
	 * The extender this extension is part of
	 */
	private Extender<T> extender;
	private Bundle userBundle;
	
	public ExtensionImpl()  throws ExtenderException {
	}

	public ExtensionImpl(Class<T> intFace, Bundle userBundle)  throws ExtenderException {

		this.intFace = intFace;
		this.userBundle = userBundle;
		this.extender = getExtender();
	}

	public ExtensionImpl(String interfaceName, Bundle userBundle)  throws ExtenderException {
		this.extender = ExtenderImpl.<T>getExtender(interfaceName);
		this.intFace = extender.getInterfaceServiceClass();
		this.userBundle = userBundle;
	}

	public ExtensionImpl(Class<T> intFace)  throws ExtenderException {
		this.intFace = intFace;
		this.extender = getExtender();
	}

	public ExtensionImpl(String interfaceName)  throws ExtenderException {
		this.extender = ExtenderImpl.<T>getExtender(interfaceName);
		this.intFace = extender.getInterfaceServiceClass();
	}

	public T getService(Bundle bundle) throws ExtenderException {		
		extender = getExtender();
		return extender.getService(bundle);
	}		

	public T getService() throws ExtenderException {
		extender = getExtender();
		if (null == userBundle) {
			return extender.getService();			
		} else {
			return extender.getService(userBundle);						
		}
	}		
	
	public Extender<T> getExtender() throws ExtenderException {
		this.extender = ExtenderImpl.<T>getExtender(intFace.getName());
		return extender;
	}
}
