package no.javatime.inplace.bundlemanager.state;

import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.util.messages.TraceMessage;

public abstract class BundleState {

	public BundleState() {
	}

	public BundleState get(Integer stateConstant) {
		return BundleStateFactory.INSTANCE.get(stateConstant);			
	}

	public int getBundleStateConstant(BundleNode bundleNode) {

		if (null == bundleNode) {
			return Bundle.UNINSTALLED;			
		}
		Bundle bundle = InPlace.getContext().getBundle(bundleNode.getBundleId());
		if (null == bundle) {
			return Bundle.UNINSTALLED;
		}
		return bundle.getState();
		// TODO Test new statements
		// return BundleCommandImpl.INSTANCE.getState(getBundle(bundleNode));
	}
	
	public Bundle getBundle(BundleNode bundleNode) {
		if (null != bundleNode) {
			return InPlace.getContext().getBundle(bundleNode.getBundleId());
		}
		return null;
	}

	public void install(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void uninstall(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void resolve(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void starting(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void start(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void stopping(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void stop(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void update(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void refresh(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public String errorState(BundleNode bundleNode) {
		return error(bundleNode, "Illegal state.", bundleNode.getCurrentState().getClass());
	}

	public String error(BundleNode bundleNode, String msg, Class<? extends BundleState> newState) {
		return TraceMessage.getInstance().getString("state_error",
				newState.getSimpleName(),getBundle(bundleNode));
	}
}
