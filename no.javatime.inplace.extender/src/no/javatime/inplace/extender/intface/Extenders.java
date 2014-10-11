package no.javatime.inplace.extender.intface;

import no.javatime.inplace.extender.provider.ExtenderImpl;


/**
 * Register extenders and access extenders and their extensions
 */
public class Extenders {

	private Extenders() {
	}	

	/**
	 * Get this meta extender as a starting point to register and extend service objects.
	 * <p>
	 * Use one of the register methods in the returned extender to create new extender services. 
	 * The service returned by one of the register methods in 
	 * this meta extender is an extender containing service meta data. 
	 * @return the meta extender or null if the meta extender is not itself registered.
	 * <p>
	 * It is also possible to register extenders from other registered extenders. 
	 */
	public static final <S> Extender<S> getExtender() {
		Extender<S> extender = ExtenderImpl.<S>getExtender(Extender.class.getName());
		return extender;		
	}		

	/**
	 * Get an extension of the extender previously registered with the specified interface name
	 * @param interfaceName service interface name  
	 * @return the extension interface or null if there is no registered service with the specified interface name
	 * @throws ExtenderException if fail to get the registered extension
	 */
	public static final <S>  Extension<S> getExtension(String interfaceName) throws ExtenderException {
		Extender<S> extender = ExtenderImpl.<S>getExtender(interfaceName);
		return extender.getExtension();
	}
	
	public static final <S> Extender<S> getExtender(String interfaceName) throws ExtenderException {
		Extender<S> extender = ExtenderImpl.<S>getExtender(interfaceName);
		return extender;
	}
}
