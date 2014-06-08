/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.region.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import no.javatime.inplace.region.manager.BundleCommandImpl;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.osgi.framework.Bundle;

/**
 * Keeps track of the current active transition performed on a bundle. An active
 * transition is either currently executing or the last executed transition.
 * <p>
 * The key is the location identifier of the bundle and the values are the kind of transition being executed.
 */
public class ActiveTransition {
	
	private class State {

		public Transition transition;
		public TransitionError error;

		public State(Transition transition, TransitionError error) {
			this.transition = transition;
			this.error = error;
		}
		
	}

	private Map<String, State> transit = new ConcurrentHashMap<String, State>();

	/**
	 * Default instance constructor
	 */
	public ActiveTransition() {
	}

	public int getSize() {
		return transit.size();
	}

	/**
	 * Indicates whether a bundle operation is in progress. The bundle may be in a transition (between two
	 * states) not yet reported by OSGI bundle events. Installing or uninstalling are transitions reported by
	 * this method
	 * 
	 * @param location is a unique path to a bundle
	 * @return installing, uninstalling or that no transition is running.
	 */
	public Transition get(String location) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return Transition.NOTRANSITION;
		}
		State t = transit.get(location);
		if (null == t) {
			return Transition.NOTRANSITION;
		} else {
			return t.transition;
		}
	}

	/**
	 * Get the current or last transition executed on a bundle identified
	 * by the bundle location identifier 
	 * 
	 * @param location identifier of the bundle to get the transition for
	 * @return the transition of the bundle or {@code Transition.NOTRANSITION}
	 * if no transition is defined for the specified location identifier
	 */
	public Transition get(Bundle bundle) {
		return get(bundle.getLocation());
	}

	public boolean isTransition(String location, Transition transition) {
		if (transition == get(location)) {
			return true;
		}
		return false;
	}

	public boolean hasTransition(Transition transition) {
		for (State trans : this.transit.values()) {
			if (trans.transition == transition) {
				return true;
			}
		}
		return false;
	}

	public void remove(Transition transition) {
		for (String location : this.transit.keySet()) {
			Transition trans = get(location);
			if (trans == transition) {
				remove(location);
			}
		}
	}

	/**
	 * Place a transition to execute for a bundle. The error state is set to
	 * {@code TransitionError#NOERROR}.
	 * <p>
	 * If the specified transition is {@code null}, the transition is set to
	 * {@code Transition#NOTRANSITION}
	 * 
	 * @param location of the bundle undergoing the transition
	 * @param transition transition being run on the bundle
	 * @return the previous transition or null if there is nor previous transition
	 */
	public Transition put(String location, Transition transition) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return Transition.NOTRANSITION;
		}
		if (Category.DEBUG && Category.getState(Category.fsm)) {
			Transition currentTransition = get(location);
			if (null != currentTransition && Transition.UNINSTALL != currentTransition) {
				TraceMessage.getInstance().getString("existing_and_new_transition", transition, currentTransition,
						location);
			}
		}
		if (null == transition) {
			transition = Transition.NOTRANSITION;
		}
		State retState = transit.put(location, new State(transition, TransitionError.NOERROR));
		if (null != retState) {
			return retState.transition;
		} else {
			return Transition.NOTRANSITION;
		}
	}
	
	public boolean setError(String location, TransitionError transitionError) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return false;
		}
		State state = transit.get(location);
		if (null == state) {
			state = new State(Transition.NOTRANSITION, transitionError);
		}
		state.error = transitionError;
		transit.put(location, state);
		return true;
	}
 
	public boolean hasError(String location) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return false;
		}
		State state = transit.get(location);
		if (null != state) {
			if (state.error == TransitionError.NOERROR) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasError(TransitionError transitionError) {
		for (State state : transit.values()) {
			if (state.error.equals(transitionError)) {
				return true;
			}
		}
		return false;
	}
	
	public TransitionError getError(String location) {

		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return TransitionError.NOERROR;
		}
		State state = transit.get(location);
		if (null != state) {
			return state.error;
		}
		return TransitionError.NOERROR;
	}

	public boolean clearError(String location) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return false;
		}
		State state = transit.get(location);
		if (null != state) {
			state.error = TransitionError.NOERROR;
			transit.put(location, state);
			return true;
		}
		return false;
	}

	public boolean removeError(String location, TransitionError error) {
		if (null == location) {
			if (Category.DEBUG)
				TraceMessage.getInstance().getString("missing_transit_location");
			return false;
		}
		State state = transit.get(location);
		if (null != state && state.error == error) {
			state.error = TransitionError.NOERROR;
			transit.put(location, state);
			return true;
		}
		return false;
	}
	
	/**
	 * Clear all registered transitions
	 */
	public void clear() {
		transit.clear();
	}

	/**
	 * Remove a registered transition for a bundle project
	 * 
	 * @param location identifier of the bundle to remove the transition for
	 * @return the transition of the bundle or null if there is no registered transition
	 */
	public Transition remove(String location) {
		State state =  transit.remove(location);
		if (null != state) {
			return state.transition;
		} else {
			return null;
		}
	}

	/**
	 * Remove the specified transition for a bundle project
	 * 
	 * @param location identifier of the bundle to remove the transition for
	 * @param transition the transition to remove
	 * @return the specified transition of the bundle or {@code Transition.NO_TRANSITION} if the specified
	 *         transition is not registered with the bundle
	 */
	public Transition remove(String location, Transition transition) {
		Transition currentTrans = get(location);
		if (null != currentTrans && currentTrans.equals(transition)) {
			State state =  transit.remove(location);
			if (null != state) {
				return state.transition;
			} else {
				return null;
			}
		} else {
			return Transition.NOTRANSITION;
		}
	}

	public String getName(String location) {

		if (null == location) {
			return getName((Transition) null);
		}
		State state = transit.get(location);
		if (null != state) {
			return getName(state.transition);
		} else {
			return getName((Transition) null);
		}
	}

	/**
	 * Textual representation of the transition for the bundle at the specified location. The location is the
	 * same as used when the bundle was installed.
	 * 
	 * @param location of the bundle
	 * @return the name of the transition or an empty string if no transition is found at the specified location
	 * @see Bundle#getLocation()
	 * @see BundleCommandImpl#getBundleLocationIdentifier(org.eclipse.core.resources.IProject)
	 */
	public String getName(Transition transition) {
		String typeName = "NO TRANSITION";
		if (null == transition) {
			return typeName;
		}
		switch (transition) {
		case INSTALL:
			typeName = "INSTALL";
			break;
		case STOP:
			typeName = "STOP";
			break;
		case UNINSTALL:
			typeName = "UNINSTALL";
			break;
		case RESOLVE:
			typeName = "RESOLVE";
			break;
		case UNRESOLVE:
			typeName = "UNRESOLVE";
			break;
		case LAZY_LOAD:
		case START:
			typeName = "START";
			break;
		case UPDATE_ON_ACTIVATE:
			typeName = "UPDATE_ON_ACTIVATE";
			break;
		case DEACTIVATE:
			typeName = "DEACTIVATE";
			break;
		case RESET:
			typeName = "RESET";
			break;
		case REFRESH:
			typeName = "REFRESH";
			break;
		case EXTERNAL:
			typeName = "EXTERNAL";
			break;
		case UPDATE:
			typeName = "UPDATE";
			break;
		case BUILD:
			typeName = "BUILD";
			break;
		case NOTRANSITION:
		default:
			return typeName;
		}
		return typeName;
	}
}
