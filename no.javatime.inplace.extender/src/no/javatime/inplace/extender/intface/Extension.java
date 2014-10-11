package no.javatime.inplace.extender.intface;


import org.osgi.framework.Bundle;

public interface Extension<S> {

	/**
	 * Get the service object for this extension.
	 * <p>
	 * Uses the bundle context of the bundle who registered the service. The service object is shared
	 * among all clients. If null is returned this usually means that the bundle context of the bundle
	 * providing the service is no longer valid.
	 * 
	 * @return the service object for for the interface specified at construction of this extension
	 * object or null if no service is being tracked.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 * @throws ExtenderException if the bundle context of the owner bundle is not valid, the bundle is
	 * in an illegal state (uninstalled, installed or resolved), the service was not created by the
	 * same framework instance as the BundleContext of the specified bundle or if the caller does not
	 * have the appropriate AdminPermission[this,CLASS], and the Java Runtime Environment supports
	 * permissions.
	 */
	public S getService() throws ExtenderException ;

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
	public S getService(Bundle bundle) throws ExtenderException ;

	/**
	 * Get the extender which this extension is part of
	 * 
	 * @return the extender instance or null if the bundle is no longer tracked, the bundle has no
	 * registered extensions or the registered class does not implement the registered interface
	 * @throws ExtenderException if the bundle context of this extension is no longer valid or the
	 * class object implementing the extension could not be created
	 */
	public Extender<S> getExtender() throws ExtenderException;
}
