package no.javatime.inplace.extender.tmp;

import java.util.Dictionary;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.provider.ExtenderImpl;
import no.javatime.inplace.extender.provider.ExtenderServiceMap;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.BundleTracker;

public class ExtendersImpl<S> implements IExtenders<S> {

	public Extender<S> register(BundleTracker<Extender<?>> tracker, Bundle owner,
			Bundle registrar, String serviceInterfaceName, String serviceName,
			Dictionary<String, Object> properties) throws ExtenderException {

		Extender<S> extender = new ExtenderImpl<>(tracker, owner, registrar, serviceInterfaceName,
				serviceName, properties);
		extender.registerService();
		return extender;
	}

	public Extender<S> getExtender(String interfaceName) throws ExtenderException {

		@SuppressWarnings("unchecked")
		ExtenderServiceMap<S> extMapService = (ExtenderServiceMap<S>) Activator.getExtenderServiceMap();
		Extender<S> extender = extMapService.getExtender(interfaceName);
		Activator.ungetServiceMap();
		return extender;
	}
	
}
