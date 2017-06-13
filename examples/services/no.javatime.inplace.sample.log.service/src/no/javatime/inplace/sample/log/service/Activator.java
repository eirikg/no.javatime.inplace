package no.javatime.inplace.sample.log.service;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import no.javatime.inplace.sample.log.service.impl.SimpleLogServiceImpl;

class Activator implements BundleActivator {

	private ServiceTracker<SimpleLogService, SimpleLogService> simpleLogServiceTracker;
	private SimpleLogService simpleLogService;
	private ServiceRegistration<?> sr;
	
	public void start(BundleContext context) throws Exception {
		// register the simple log service as a service
		sr = context.registerService(SimpleLogService.class.getName(), 
				new SimpleLogServiceImpl(),
				new Hashtable<String, Object>());
		// Use your own registered service to log messages
		// create a tracker to track the registered simple log service
		simpleLogServiceTracker = new ServiceTracker<SimpleLogService, SimpleLogService>(context,
				SimpleLogService.class.getName(), null);
		simpleLogServiceTracker.open();
		// grab the service
		simpleLogService = simpleLogServiceTracker.getService();
		if (simpleLogService != null)
			simpleLogService.log("Registered simple Log service");
	}

	public void stop(BundleContext context) throws Exception {
		if (simpleLogService != null)
			simpleLogService.log("Unregistered simple Log service");
		// close the service tracker and unregister the simple log service
		simpleLogServiceTracker.close();
		simpleLogServiceTracker = null;
		sr.unregister();
		simpleLogService = null;
	}

}
