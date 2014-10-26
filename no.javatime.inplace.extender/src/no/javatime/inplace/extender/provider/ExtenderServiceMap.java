package no.javatime.inplace.extender.provider;

import java.util.concurrent.ConcurrentMap;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.ServiceReference;

public interface ExtenderServiceMap<S> extends ConcurrentMap<Long, Extender<S>>{

	public Extender<S> addExtender(ServiceReference<?> sr, Extender<S> extender) throws ExtenderException;

	public Extender<S> getExtender(ServiceReference<?> sr);

	public Extender<S> getExtender(String serviceInterfaceName) throws ExtenderException;

	public Extender<S> removeExtender(ServiceReference<?> sr);
	
	/**
	 * If there are any extenders in the extender map, print a warning to system err
	 */
	public void validateUnregister();

}
