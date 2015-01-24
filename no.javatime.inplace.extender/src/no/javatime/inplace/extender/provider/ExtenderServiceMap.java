package no.javatime.inplace.extender.provider;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.ServiceReference;

public interface ExtenderServiceMap<S> extends ConcurrentMap<Long, Extender<S>>{

	public Extender<S> get(String serviceInterfaceName) throws ExtenderException;
	public Collection<Extender<S>> get(String serviceInterfaceName, String filter) throws ExtenderException;

	public Extender<S> get(ServiceReference<?> sr);
	public Extender<S> put(ServiceReference<?> sr, Extender<S> extender) throws ExtenderException;
	public Extender<S> remove(ServiceReference<?> sr);

	public Extender<S> put(Extender<S> extender) throws ExtenderException;
	public Extender<S> remove(Extender<S> extender);


	/**
	 * If there are any extenders in the extender map, print a warning to system err
	 */
	public void validateUnregister();

}
