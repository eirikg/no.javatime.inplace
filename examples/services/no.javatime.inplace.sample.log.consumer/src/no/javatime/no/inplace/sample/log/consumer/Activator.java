package no.javatime.no.inplace.sample.log.consumer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import no.javatime.inplace.sample.log.service.SimpleLogService;

public class Activator implements BundleActivator {
	
	private ServiceTracker<SimpleLogService, SimpleLogService> simpleLogServiceTracker;

	public void start(BundleContext context) throws Exception {
		simpleLogServiceTracker =  new ServiceTracker<SimpleLogService, SimpleLogService>
			(context, SimpleLogService.class, null);
		simpleLogServiceTracker.open();
		log("Hello World!! You have changed");
	}
	
	public void stop(BundleContext context) throws Exception {
		log("Goodbye World!! I'll be back again");
		simpleLogServiceTracker.close();
		simpleLogServiceTracker = null;		
	}
	
	public void log(String msg) {
		SimpleLogService logService = getLogService();		
		logService.log(msg);
	}

	public SimpleLogService getLogService() {	
		SimpleLogService logService = simpleLogServiceTracker.getService();
		return null != logService ? logService : new AlternativeLog();
	}

	private class AlternativeLog implements SimpleLogService {
		
		@Override
		public void log(String msg) {
			System.out.println("Alternative log: " + msg);
		}
	}
}

