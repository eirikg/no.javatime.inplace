package no.javatime.inplace.extender.provider;

import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.Extension;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * An instance of this extension service tracker customizer is created when a service is tracked by
 * an extension. The extension {@code Extension#getService(Bundle)} and
 * {@code Extension#ungetService()} is called to obtain and close the tracked service.
 * <p>
 * If an optional extender bundle tracker is specified when the extension is created the
 * {@link ExtenderBundleTracker#unregistering(Extension)} method is called when the the service is
 * untracked (closed).
 * 
 * @param <S> Type of service
 */
public class ExtensionServiceTrackerCustomizer<S> implements ServiceTrackerCustomizer<S, S> {

	// The bundle using the associated service tracker
	private Bundle bundle;
	// The extension tracking the service
	private Extension<S> extension;
	// An optional extension bundle tracker to call when the service is untracked
	private ExtenderBundleTracker bt;

	public ExtensionServiceTrackerCustomizer(Extension<S> extension, Bundle bundle) {
		this.bundle = bundle;
		this.extension = extension;
	}

	public ExtensionServiceTrackerCustomizer(Extension<S> extension, Bundle bundle,
			ExtenderBundleTracker bt) {
		this.bundle = bundle;
		this.extension = extension;
		this.bt = bt;
	}

	public ExtenderBundleTracker getBundleTracker() {
		return bt;
	}

	@Override
	public S addingService(ServiceReference<S> reference) {

		return extension.getService(bundle);
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, S service) {

	}

	/**
	 * Calls the optional bundle tracker {@link ExtenderBundleTracker#unregistering(Extension)}
	 * method.
	 */
	@Override
	public void removedService(ServiceReference<S> reference, S service) {
		extension.ungetService(bundle);
		// Serviced unregistered while tracker is open
		// !extension.getExtender().isRegistered()) does not work as expected
		if (null != bt && extension.getTrackingCount() != -1) {
			bt.unregistering(extension);
		}
	}
}
