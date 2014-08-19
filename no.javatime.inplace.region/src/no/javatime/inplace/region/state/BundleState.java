package no.javatime.inplace.region.state;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public abstract class BundleState {

	public BundleState() {
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
	
	/**
	 * A transition was started but did not finish. The most typical cases are
	 * when an exception is thrown during a start and stop transition
	 *  
	 * @param bundleNode the current bundle node
	 * @param event information about the external command
	 */
	public void incomplete(BundleNode bundleNode, BundleEvent event, TransitionError error) {
		bundleNode.setTransitionError(error);
	}

	/**
	 * An external bundle command has been executed on a workspace bundle
	 * @param bundleNode the current bundle node
	 * @param event information about the external command
	 */
	public void external(BundleNode bundleNode, BundleEvent event) {
		Bundle bundle = event.getBundle();
		final String location = bundle.getLocation();
		BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
		BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;
		final String symbolicName = BundleManager.getRegion().getSymbolicKey(bundle, null);
		final String stateName = bundleCommand.getStateName(event);
		if (bundleTransition.getError(bundle) == TransitionError.INCOMPLETE) {
			String msg = NLS.bind(Msg.INCOMPLETE_BUNDLE_OP_INFO, new Object[] {symbolicName, stateName,
					location});
			StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg), StatusManager.LOG);				
		} else {
			TransitionError error = bundleTransition.getError(bundle);
			bundleTransition.setTransition(bundle, Transition.EXTERNAL);
			bundleTransition.setTransitionError(bundle, error);
			BundleManager.addBundleTransition(new TransitionEvent(bundle, Transition.EXTERNAL));
			String msg = NLS.bind(Msg.EXT_BUNDLE_OP_INFO, new Object[] {symbolicName, stateName,
					location});
			StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg), StatusManager.LOG);
		}
	}

	/**
	 * This method is invoked when the FSM is invalidated by an illegal transition 
	 * Log internal state error to error view and bundle log view
	 * @param bundleNode the node containing the illegal state and transition
	 * @return the logged message
	 */
	public String errorState(BundleNode bundleNode) {
		Bundle bundle = bundleNode.getBundle();
		String bundleProjectName = null;
		if (null != bundle) {
			bundleProjectName = bundle.getSymbolicName();
		} else {
			bundleProjectName = bundleNode.getProject().getName();
		}
		String msg = NLS.bind(Msg.INTERNAL_STATE_WARN, new Object[] { 
				bundleNode.getPrevState().getClass().getSimpleName(), bundleNode.getState().getClass().getSimpleName(),
				BundleNode.getTransitionName(bundleNode.getTransition(), true, true), 
				BundleNode.getTransitionName(bundleNode.getPrevTransition(), true, true), 
				bundleProjectName });
		if (null != bundle) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, bundle, msg, null),
					StatusManager.LOG);
		} else {
			StatusManager.getManager()
			.handle(
					new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, bundleNode.getProject(), msg,
							null), StatusManager.LOG);
		}
		return msg;
	}


	public void commit(BundleNode bundleNode) {
		bundleNode.commit();
	}

	public void rollBack(BundleNode bundleNode) {
		bundleNode.rollBack();
	}

	public Bundle getBundle(BundleNode bundleNode) {
		if (null != bundleNode) {
			Long bundleId = bundleNode.getBundleId();
			if (null != bundleId) {
				return Activator.getContext().getBundle(bundleId);
			}
		}
		return null;
	}

	public BundleState get(Integer stateConstant) {
		return StateFactory.INSTANCE.get(stateConstant);
	}

	public int getBundleStateConstant(BundleNode bundleNode) {
	
		if (null == bundleNode) {
			return Bundle.UNINSTALLED;
		}
		Bundle bundle = Activator.getContext().getBundle(bundleNode.getBundleId());
		if (null == bundle) {
			return Bundle.UNINSTALLED;
		}
		return bundle.getState();
	}
}
