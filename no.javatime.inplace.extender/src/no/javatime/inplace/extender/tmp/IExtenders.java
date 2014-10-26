package no.javatime.inplace.extender.tmp;

import java.util.Dictionary;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.BundleTracker;

public interface IExtenders<S> {

	public Extender<S> register(BundleTracker<Extender<?>> tracker, Bundle owner,
			Bundle registrar, String serviceInterfaceName, String serviceName,
			Dictionary<String, Object> properties) throws ExtenderException;
	
	public Extender<S> getExtender(String interfaceName) throws ExtenderException;


}
