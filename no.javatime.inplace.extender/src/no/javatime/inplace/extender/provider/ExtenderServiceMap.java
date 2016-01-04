package no.javatime.inplace.extender.provider;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public interface ExtenderServiceMap<S> extends ConcurrentMap<Long, Extender<S>> {

	// public Extender<S> put(Long sid, Extender<S> extender) throws ExtenderException;

	/**
	 * Get the extender which registered the service under the specified service name
	 * <p>
	 * Assumes that the bundle context used to obtain the service is valid.
	 * <p>  
	 * @param serviceInterfaceName Name of the registered service
	 * @return The extender holding the specified service interface name or null if there is no
	 * service registered wit the specified service interface name
	 * @throws ExtenderException if the service id is <code>null</code> for the registered service
	 * with the specified service interface name
	 */
	public Extender<S> get(String serviceInterfaceName) throws ExtenderException;

	public Collection<Extender<S>> get(String serviceInterfaceName, String filter)
			throws ExtenderException;

	public Extender<S> get(ServiceReference<?> sr);

	/**
	 * Stores an extender in the map based on its service reference
	 * 
	 * @param sr the service reference associated with the specified extender
	 * @param extender the extender to add or update the map with
	 * @return the previous extender or null if there where no previous extender
	 * @throws ExtenderException if the specified extender or service reference is null or invalid
	 * (not referencing a valid service)
	 */
	public Extender<S> put(ServiceReference<?> sr, Extender<S> extender) throws ExtenderException;

	/**
	 * Removes an extender from the map based on its service reference
	 * 
	 * @param sr the service reference associated with the specified extender
	 * @return the previous extender or null if there where no previous extender
	 * @throws ExtenderException if the specified extender or service reference is null or invalid
	 * (not referencing a valid service)
	 */
	public Extender<S> remove(ServiceReference<?> sr);

	public Extender<S> put(Extender<S> extender) throws ExtenderException;

	/**
	 * Removes an extender from the map
	 * 
	 * @param extender the extender to remove from the map
	 * @throws ExtenderException if the specified extender or service reference is null or invalid
	 * (not referencing a valid service)
	 */
	public void remove(Extender<S> extender);

	/**
	 * Get all extenders hosted or owned by the specified owner bundle
	 * 
	 * @param owner The bundle hosting the extenders
	 * @return all extenders hosted by the specified owner bundle or an empty collection
	 */
	public Collection<Extender<S>> getExtenders(Bundle owner);

	/**
	 * If there are any extenders in the extender map, print a warning to system err
	 */
	public void validateUnregister();

}
