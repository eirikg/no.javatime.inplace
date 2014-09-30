package no.javatime.inplace.extender.intface;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.ExtenderException;

import org.osgi.framework.Bundle;

public interface IExtension<T> {

	/**
	 * Get the service object for this extension.
	 * <p>
	 * Uses the bundle context of the bundle who registered the service. The service object is shared
	 * among all clients. If null is returned this usually means that the bundle context of the bundle
	 * providing the service is no longer valid.
	 * 
	 * @return the service object for for the interface specified at construction of this extension
	 * object or null if no service is being tracked.
	 */
	public T getService();

	/**
	 * Get the service object for this extension.
	 * <p>
	 * Uses the bundle context of the specified bundle when getting the service object If null is
	 * returned this usually means that the specified bundle context of the bundle providing the
	 * service is no longer valid.
	 * 
	 * @return the service object for for the interface specified at construction of this extension
	 * object or null if no service is being tracked.
	 */
	public T getService(Bundle bundle);

	/**
	 * Get the extender which this extension is part of
	 * 
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of this extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public Extender<T> getInstance() throws ExtenderException;
}
