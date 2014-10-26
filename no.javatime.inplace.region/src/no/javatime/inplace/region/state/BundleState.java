package no.javatime.inplace.region.state;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransitionImpl;
import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;
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
	
	public void unresolve(BundleNode bundleNode) {
		errorState(bundleNode);		
	}

	public void resolve(BundleNode bundleNode) throws InPlaceException {
		errorState(bundleNode);
	}

	public void lazyLoad(BundleNode bundleNode) throws InPlaceException {
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
	 * @param state new state according to the state machine when an external transition has been issued 
	 * @param transition new transition according to the state machine when an external transition has been issued
	 */
	public void external(BundleNode bundleNode, BundleEvent event, BundleState state, Transition transition) {
		
		Bundle bundle = event.getBundle();
		final String location = bundle.getLocation();
		BundleCommandImpl bundleCommand = BundleCommandImpl.INSTANCE;
		BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;
		final String symbolicName = BundleWorkspaceRegionImpl.INSTANCE.getSymbolicKey(bundle, null);
		final String stateName = bundleCommand.getStateName(event);
		if (bundleTransition.getError(bundle) == TransitionError.INCOMPLETE) {
			if (Activator.getDefault().msgOpt().isBundleOperations()) {
				String msg = NLS.bind(Msg.INCOMPLETE_BUNDLE_OP_INFO, new Object[] {symbolicName, stateName,
						location});
				StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg), StatusManager.LOG);				
			}
		} else {
			bundleNode.commit(transition, state);
			if (Activator.getDefault().msgOpt().isBundleOperations()) {
				String msg = NLS.bind(Msg.EXT_BUNDLE_OP_INFO, new Object[] {symbolicName, stateName,
						location});
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, bundleNode.getTransition()));
//				StatusManager.getManager().handle(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, bundle, msg, null), StatusManager.LOG);
			}
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
				BundleNode.getTransitionName(bundleNode.getPrevTransition(), true, true),
				bundleNode.getPrevState().getClass().getSimpleName(), 
				BundleNode.getTransitionName(bundleNode.getTransition(), true, true), 
				bundleNode.getState().getClass().getSimpleName(),
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
