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

import java.util.EnumSet;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * There is an unconditional bidirectional relationship between a project and a bundle. The term
 * describing this relationship and the involved entities is the bundle project. A bundle project is
 * characterized by a project that has the java and plug-in nature enabled, the relation between
 * them, the bundle and a common source code combined.The shared keys in this relation are:
 * <ol>
 * <li>The composite key; - symbolic name and version.
 * <li>The location identifier
 * </ol>
 * In addition to the shared keys and the object identifiers of both, the project name and the
 * bundle id is also a primary key of a project and a bundle respectively. The bundle id does not
 * come into existence before the bundle is installed for the first time.
 * <p>
 * Both keys; - The project object (identifier); and - The bundle object is stored in this bundle
 * node. All other foreign and primary keys are accessible through both of these keys. The
 * activation mode (activated or deactivated), the internal state, transition, transition errors and
 * a set of pending bundle transitions are stored along with the two keys in the bundle node.
 * <p>
 * A bundle node stores build and bundle errors:
 * <ol>
 * <li>Build errors are cleared before build and marked as build errors after a build of a project.
 * A build is a kind of transition for projects
 * <li>Bundle errors are cleared before a bundle transition is executed and set if the bundle
 * transition fails. Bundle nodes objects are backed by and holds the state of a FSM where bundle
 * errors represent the state of an error transition
 * </ol>
 * <p>
 * The set of bundle errors is a subset of build errors and thus a bundle error is also a build
 * error. The build error contains the last bundle error reported in a chain of bundle transitions
 * and first cleared before the next build of the project even if the bundle error is cleared before
 * any bundle transition. With some exceptions build errors prevents a bundle transition to be
 * initiated while a bundle error is the consequence of executing a bundle transition.
 */
public class BundleNode {

	// The primary key of the project
	private IProject project;
	// The primary key of the bundle.
	private Bundle bundle;
	// Explicit set, indicating whether the bundle is activated or deactivated
	private Boolean activated;
	// Current bundle state. Terminal state of the current transition
	private BundleState state = StateFactory.INSTANCE.stateLess;
	// Current (is executing) or last executed transition
	private Transition transition = Transition.NO_TRANSITION;
	// True while a transition is executing (current transition)
	private boolean isStateChanging;
	// Build time transition errors. Errors are cleared during build
	private TransitionError buildTransitionError = TransitionError.NOERROR;
	// Bundle life cycle transition errors. Bundle errors are cleared before a bundle operation
	private TransitionError bundleTransitionError = TransitionError.NOERROR;
	// Previous bundle state.
	// Initial state of the current transition and terminal state of the previous transition
	private BundleState prevState = StateFactory.INSTANCE.stateLess;
	// Previous transition
	private Transition prevTransition = Transition.NO_TRANSITION;
	// A set of pending transitions in random order waiting to be executed
	private EnumSet<Transition> pendingTranitions = EnumSet.noneOf(Transition.class);

	private IBundleStatus buildStatus;
	private IBundleStatus bundleStatus;

	/**
	 * Creates a bundle node with a one-to-one relationship between a project and a bundle, called a
	 * bundle project.Initially the bundle node has no state and initialized to
	 * {@link no.javatime.inplace.region.state.StateLess}
	 * 
	 * @param bundle may be null
	 * @param project must not be null
	 * @param activate true if the bundle is to be activated, otherwise false
	 */
	public BundleNode(Bundle bundle, IProject project, Boolean activate) {
		this.project = project;
		this.activated = activate;
		this.bundle = bundle;
	}

	public TransitionError getTransitionError() {
		if (hasBundleTransitionError()) {
			return bundleTransitionError;
		} else if (hasBuildTransitionError()) {
			return buildTransitionError;
		}
		return TransitionError.NOERROR;
	}

	public IBundleStatus getTransitionStatus() {
		if (null != bundleStatus) {
			return bundleStatus;
		} else if (null != buildStatus) {
			return buildStatus;
		}
		return null;
	}

	public IBundleStatus getBuildStatus() {
		return buildStatus;
	}

	public void setBuildStatus(TransitionError transitionError, IBundleStatus status) {
		setBuildTransitionError(transitionError);
		this.buildStatus = status;
	}

	public void setBuildStatus(IBundleStatus status) {
		this.buildStatus = status;
	}
	public boolean clearBuildTransitionError() {
		this.buildTransitionError = TransitionError.NOERROR;
		buildStatus = null;
		return true;
	}

	public IBundleStatus getBundleStatus() {
		return bundleStatus;
	}

	public void setBundleStatus(TransitionError transitionError, IBundleStatus status) {
		// Duplicates are both build and modular errors
		if (transitionError.equals(TransitionError.BUILD_MODULAR_EXTERNAL_DUPLICATE)
				|| transitionError.equals(TransitionError.BUILD_MODULAR_WORKSPACE_DUPLICATE)) {
			setBuildStatus(transitionError, status);
		}
		setBundleTransitionError(transitionError);
		this.bundleStatus = status;
	}

	public void setBundleStatus(IBundleStatus status) {
		this.bundleStatus = status;
	}

	public boolean clearBundleTransitionError() {
		this.bundleTransitionError = TransitionError.NOERROR;
		bundleStatus = null;
		return true;
	}
	

	public Transition getTransition() {
		return transition;
	}

	public Transition setTransition(Transition transition) {
		Transition tmp = this.transition;
		this.transition = transition;
		return tmp;
	}

	public TransitionError getBuildTransitionError() {
		return buildTransitionError;
	}

	public void setBuildTransitionError(TransitionError buildTransitionError) {
		this.buildTransitionError = buildTransitionError;
	}

	public TransitionError getBundleTransitionError() {
		return bundleTransitionError;
	}

	public void setBundleTransitionError(TransitionError bundleTransitionError) {
		this.bundleTransitionError = bundleTransitionError;
	}

	public boolean hasBuildTransitionError() {
		return buildTransitionError == TransitionError.NOERROR ? false : true;
	}

	public boolean hasBundleTransitionError() {
		return bundleTransitionError == TransitionError.NOERROR ? false : true;
	}

	/**
	 * Get the current {@code State} of this bundle node.
	 * 
	 * @return the current state of this bundle node or null if no state has been assigned yet.
	 */
	public BundleState getState() {
		return state;
	}

	/**
	 * Check if the specified state is the same as the current state for this bundle node
	 * 
	 * @param state the state to compare for equality with the current state of this bundle node
	 * @return true if the specified state is equal to the current state of this bundle node,
	 * otherwise false
	 */
	public boolean isState(Class<? extends BundleState> stateClass) {
		return (stateClass.isAssignableFrom(state.getClass()));
	}

	/**
	 * Check if the specified transition and state is the same as the current transition and state
	 * combination for this bundle node
	 * 
	 * @param transition the transition to compare for equality with the current transition of this
	 * bundle node
	 * @param state the state to compare for equality with the current state of this bundle node
	 * @return true if the specified transition and state is equal to the current transition and state
	 * of this bundle node, otherwise false
	 */
	public boolean isState(Transition transition, Class<? extends BundleState> stateClass) {
		if (this.transition == transition) {
			return (stateClass.isAssignableFrom(state.getClass()));
		}
		return false;
	}

	/**
	 * Check if the specified transition is the same as the current transition for this bundle node
	 * 
	 * @param transition the transition to compare for equality with the current transition of this
	 * bundle node
	 * @return true if the specified transition is equal to the current transition of this bundle
	 * node, otherwise false
	 */
	public boolean isTransition(Transition transition) {
		return this.transition == transition;
	}

	/**
	 * Assigns a state treated as the current state of this bundle node. The external attribute of the
	 * state is not altered.
	 * 
	 * @param state the current state of this bundle as any valid sub class of type {@code State}
	 * @see BundleState
	 */
	public void setCurrentState(BundleState currentState) {
		if (Category.DEBUG && Category.getState(Category.fsm)) {
			if (null != bundle) {
				TraceMessage.getInstance().getString("state_change", bundle,
						this.state.getClass().getSimpleName(), currentState.getClass().getSimpleName());
			} else {
				TraceMessage.getInstance().getString("state_change", project.getName(),
						this.state.getClass().getSimpleName(), currentState.getClass().getSimpleName());
			}
		}
		this.state = currentState;
	}

	/**
	 * The symbolic key of the bundle is the concatenation of the symbolic name and the version
	 * 
	 * @return the key of the bundle as a concatenation of the symbolic name and the bundle version or
	 * null if the bundle is missing
	 * @throws InPlaceException if there exists a bundle id and the bundle could not be retrieved
	 */
	public final String getSymbolicKey() throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException("illegal_bundle_id", project.getName());
		}
		return bundle.getSymbolicName() + bundle.getVersion();
	}

	/**
	 * Formats the key as symbolic name_version If bundle is not null the cached version is fetched,
	 * otherwise the referenced version from manifest is read. At least one of the specified
	 * parameters must not be null
	 * 
	 * @param bundle to format the key from
	 * @param project to format the key from
	 * @return the formatted key or an empty string
	 */
	public static String formatSymbolicKey(Bundle bundle, IProject project) {
		StringBuffer key = new StringBuffer();
		if (null != bundle) {
			key.append(bundle.getSymbolicName());
			key.append('_');
			key.append(bundle.getVersion());
		} else if (null != project) {
			String symbolicName = null;
			String version = null;
			try {
				symbolicName = BundleProjectMetaImpl.INSTANCE.getSymbolicName(project);
				version = BundleProjectMetaImpl.INSTANCE.getBundleVersion(project);
			} catch (InPlaceException e) {
			}
			if (null != symbolicName) {
				key.append(symbolicName);
				if (null != version) {
					key.append('_');
					key.append(version);
				}
			}
		}
		return key.toString();
	}

	/**
	 * The unique bundle id of the bundle
	 * 
	 * @return the bundle Id or null
	 */
	public final Long getBundleId() {

		return null != bundle ? bundle.getBundleId() : null;
	}

	/**
	 * Get the bundle object from the specified bundle id
	 * 
	 * @param bundle the id used to retrieve the bundle object
	 * @return the bundle object or null
	 */
	public final Bundle getBundle(Long bundleId) {
		if (null != bundle && bundleId.longValue() == bundle.getBundleId()) {
			return bundle;
		}
		return Activator.getContext().getBundle(bundleId);
	}

	/**
	 * Get the bundle object from the registered bundle
	 * 
	 * @return the bundle object or null
	 */
	public final Bundle getBundle() {
		return bundle;
	}

	/**
	 * The bundle object
	 * 
	 * @param bundle the bundle to set
	 */
	public final void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * The project associated with the bundle
	 * 
	 * @return the project registered with this bundle
	 */
	public final IProject getProject() {
		return project;
	}

	/**
	 * The project associated with the bundle
	 * 
	 * @param project the project to register with this bundle
	 */
	public final void setProject(IProject project) {
		this.project = project;
	}

	/**
	 * A bundle is activated if it has the JavaTime nature, at least installed and the activated flag
	 * set to {@code true}.
	 * 
	 * @return true if this bundle node is activated
	 */
	public final Boolean isActivated() {
		return activated;
	}

	/**
	 * Set the bundle as activated or deactivated
	 * 
	 * @param activate true if activated in-place otherwise false
	 */
	public final void setActivated(Boolean activate) {
		this.activated = activate;
	}

	/**
	 * All pending operations of this bundle
	 * 
	 * @return pending operations registered with this bundle
	 */
	public EnumSet<BundleTransition.Transition> getPendingCommands() {
		return pendingTranitions;
	}

	/**
	 * Register a set of pending bundle operations
	 * 
	 * @param operations to register with this bundle
	 */
	public void setPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingTranitions = operations;
	}

	/**
	 * Add a pending bundle operation to the bundle.
	 * 
	 * @param operation pending operation to add to the bundle
	 * @return true if the operation was added and false if it already exist
	 */
	public boolean addPendingCommand(BundleTransition.Transition operation) {
		return pendingTranitions.add(operation);
	}

	/**
	 * Add all specified operations
	 * 
	 * @param operations to add to the bundle
	 */
	public void addPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingTranitions.addAll(operations);
	}

	/**
	 * Check if the node contains one of the specified operations
	 * 
	 * @param operations a collection of operations to check against
	 * @return true if one of the specified operations is associated with this bundle node. Otherwise
	 * false.
	 */
	public boolean containsPendingCommand(EnumSet<BundleTransition.Transition> operations) {
		for (BundleTransition.Transition op : operations) {
			if (this.pendingTranitions.contains(op)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if an operation is associated with this bundle node
	 * 
	 * @param operation associated with this bundle node
	 * @param remove the operation from the bundle node before returning
	 * @return true if this operation is associated with this bundle node
	 */
	public boolean containsPendingCommand(BundleTransition.Transition operation, boolean remove) {

		if (remove) {
			return pendingTranitions.remove(operation);
		}
		return pendingTranitions.contains(operation);
	}

	/**
	 * Check if the node contains all of the specified operations
	 * 
	 * @param operations a collection of operations to check against
	 * @return true if all the specified operations is associated with this bundle node. Otherwise
	 * false.
	 */
	public boolean containsPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		return this.pendingTranitions.containsAll(operations);
	}

	/**
	 * Remove a pending operation and the reason associated with the operation from this bundle node
	 * 
	 * @param operation to remove from this bundle node
	 */
	public Boolean removePendingCommand(BundleTransition.Transition operation) {
		return this.pendingTranitions.remove(operation);
	}

	/**
	 * Remove all specified operations
	 * 
	 * @param operations to remove
	 */
	public void removePendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingTranitions.removeAll(operations);
	}

	/**
	 * Called when a bundle is starting a transition. The specified transition and state becomes
	 * current and the bundle is set to a state changing state (currently running a transition).
	 * <p>
	 * The specified state should be a valid terminal state and the current state of the bundle should
	 * be a valid source state of the specified the transition. This is validated by the internal
	 * state machine when the transition is executed.
	 * 
	 * @param transition the new current transition
	 * @param state the new current state
	 */
	public void begin(Transition transition, BundleState state) {
		// Start a new bundle command with no bundle errors
		clearBundleTransitionError();
		this.prevTransition = this.transition;
		this.prevState = this.state;
		this.transition = transition;
		this.state = state;
		isStateChanging = true;
	}

	/**
	 * Committing a transition initiated by {@code #begin(Transition, BundleState)}. The bundle is no
	 * longer in a state changing state.
	 */
	public void commit() {
		isStateChanging = false;
	}

	/**
	 * Committing a transition with the specified bundle transition and state as the new state of the
	 * bundle. Transition initiated with {@code #begin(Transition, BundleState)} causes its state and
	 * transition parameters to be overwritten. The bundle is no longer in a state changing state.
	 */
	public void commit(Transition transition, BundleState state) {
		prevTransition = this.transition;
		prevState = this.state;
		this.transition = transition;
		this.state = state;
		isStateChanging = false;
	}

	/**
	 * Undo a transition by setting the transition and state to the values before the transition was
	 * initiated with {@code #begin(Transition, BundleState)}. The bundle is no longer in a state
	 * changing state.
	 */
	public void rollBack() {
		this.transition = this.prevTransition;
		this.state = this.prevState;
		isStateChanging = false;
	}

	/**
	 * Check if the bundle is currently running a transition
	 * 
	 * @return true if the bundle is currently executing a transition, otherwise false
	 */
	public boolean isStateChanging() {
		return isStateChanging;
	}

	public BundleState getPrevState() {
		return prevState;
	}

	public Transition getPrevTransition() {
		return prevTransition;
	}

	/**
	 * Textual representation of the transition for the bundle at the specified location. The location
	 * is the same as used when the bundle was installed.
	 * 
	 * @param transition the transition format
	 * @param format if true format the transition name. Underscore is replaced by blank
	 * @param caption First letter is upper case and the rest is lower case
	 * @param location of the bundle
	 * 
	 * @return the name of the transition or an empty string if no transition is found at the
	 * specified location
	 * @see #getTransition(String)
	 */
	static public String getTransitionName(Transition transition, boolean format, boolean caption) {

		String typeName = "NO_TRANSITION";

		if (null != transition) {
			switch (transition) {
			case INSTALL:
				typeName = "INSTALL";
				break;
			case STOP:
				typeName = "STOP";
				break;
			case UNINSTALL:
				typeName = "MODULAR_EXTERNAL_UNINSTALL";
				break;
			case RESOLVE:
				typeName = "RESOLVE";
				break;
			case UNRESOLVE:
				typeName = "UNRESOLVE";
				break;
			case LAZY_ACTIVATE:
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
			case ACTIVATE_BUNDLE:
				typeName = "ACTIVATE_BUNDLE";
				break;
			case ACTIVATE_PROJECT:
				typeName = "ACTIVATE_PROJECT";
				break;
			case BUILD:
				typeName = "BUILD";
				break;
			case UPDATE_CLASSPATH:
				typeName = "UPDATE_CLASSPATH";
				break;
			case REMOVE_CLASSPATH:
				typeName = "REMOVE_CLASSPATH";
				break;
			case UPDATE_ACTIVATION_POLICY:
				typeName = "UPDATE_ACTIVATION_POLICY";
				break;
			case CLOSE_PROJECT:
				typeName = "CLOSE_PROJECT";
				break;
			case DELETE_PROJECT:
				typeName = "DELETE_PROJECT";
				break;
			case RENAME_PROJECT:
				typeName = "RENAME_PROJECT";
				break;
			case NEW_PROJECT:
				typeName = "NEW_PROJECT";
				break;
			case NO_TRANSITION:
			default:
			}
		}
		if (format) {
			typeName = typeName.replace('_', ' ');
			typeName = typeName.toLowerCase();
			if (caption) {
				typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
			}
		}
		return typeName;
	}

	/**
	 * Get a transition based on its textual name
	 * 
	 * @return The transition mapping from a textual transition name
	 * @see #getTransitionName(Transition, boolean, boolean)
	 */
	static public Transition getTransition(String transitionName) {

		Transition transition = Transition.NO_TRANSITION;

		if (null != transitionName) {
			switch (transitionName) {
			case "INSTALL":
				transition = Transition.INSTALL;
				break;
			case "STOP":
				transition = Transition.STOP;
				break;
			case "MODULAR_EXTERNAL_UNINSTALL":
				transition = Transition.UNINSTALL;
				break;
			case "RESOLVE":
				transition = Transition.RESOLVE;
				break;
			case "UNRESOLVE":
				transition = Transition.UNRESOLVE;
				break;
			case "LAZY_ACTIVATE":
				transition = Transition.LAZY_ACTIVATE;
				break;
			case "START":
				transition = Transition.START;
				break;
			case "UPDATE_ON_ACTIVATE":
				transition = Transition.UPDATE_ON_ACTIVATE;
				break;
			case "DEACTIVATE":
				transition = Transition.DEACTIVATE;
				break;
			case "RESET":
				transition = Transition.RESET;
				break;
			case "REFRESH":
				transition = Transition.REFRESH;
				break;
			case "EXTERNAL":
				transition = Transition.EXTERNAL;
				break;
			case "UPDATE":
				transition = Transition.UPDATE;
				break;
			case "ACTIVATE_BUNDLE":
				transition = Transition.ACTIVATE_BUNDLE;
				break;
			case "ACTIVATE_PROJECT":
				transition = Transition.ACTIVATE_PROJECT;
				break;
			case "BUILD":
				transition = Transition.BUILD;
				break;
			case "UPDATE_CLASSPATH":
				transition = Transition.UPDATE_CLASSPATH;
				break;
			case "REMOVE_CLASSPATH":
				transition = Transition.REMOVE_CLASSPATH;
				break;
			case "UPDATE_ACTIVATION_POLICY":
				transition = Transition.UPDATE_ACTIVATION_POLICY;
				break;
			case "CLOSE_PROJECT":
				transition = Transition.CLOSE_PROJECT;
				break;
			case "DELETE_PROJECT":
				transition = Transition.DELETE_PROJECT;
				break;
			case "RENAME_PROJECT":
				transition = Transition.RENAME_PROJECT;
				break;
			case "NEW_PROJECT":
				transition = Transition.NEW_PROJECT;
				break;
			case "NO_TRANSITION":
			default:
			}
		}
		return transition;
	}

}
