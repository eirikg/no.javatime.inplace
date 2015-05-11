package no.javatime.inplace.extender.provider;

import java.util.Collection;

import no.javatime.inplace.extender.Activator;
import no.javatime.inplace.extender.intface.Extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Unregister all extenders hosted by a bundle that is stopped
 */
public class ExtenderBundleListener implements SynchronousBundleListener {

	@Override
	public void bundleChanged(BundleEvent event) {

		switch (event.getType()) {
		case BundleEvent.STOPPED:
			ExtenderServiceMap<?> eMap = Activator.getExtenderServiceMap();
			if (null != eMap && eMap.size() > 0) {
				Bundle bundle = event.getBundle();
				Collection<?> extenders = eMap.getExtenders(bundle);
				for (Object extender : extenders) {
					// The extender service listener removes the extender service from the extender map
					((Extender<?>) extender).unregister();
				}
			}
		}
	}
}
