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
package no.javatime.inplace.bundlemanager.state;

import java.util.EnumSet;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

/**
 * Stores a bundle project defined as a conditional one-to-one relationship between a bundle and a project.
 * The activation status and any pending bundle commands are maintained along with the bundle (bundle id)
 * and the project (project object).
 * <p>
 * Possible keys to identify a bundle node are bundle object, symbolic name and version, bundle identifier,
 * its associated project object or name and lastly the the location identifier of the project or bundle.
 * <p>
 * The bundle identifier is stored internally in the bundle node instead of the bundle object itself.
 */
public class BundleNode {


	// Initialize with no pending pendingCommands
	private EnumSet<BundleTransition.Transition> pendingCommands = EnumSet.noneOf(BundleTransition.Transition.class);
	private IProject project; // The link between the bundle and the project (bundle project)
	private Long bundleId; // Keep the id instead of the bundle object
	private Boolean activated; // True when project is nature enabled and bundle has been installed
	private BundleState currentState; // Based on commands on bundle nodes (workspace region bundles)
	private Transition transition;
	private TransitionError transitionError;
	
	public Transition getTransition() {
		return transition;
	}

	public Transition setTransition(Transition transition) {
		Transition tmp = this.transition;
		this.transition = transition;
		return tmp;
	}
	
	public boolean setTransitionError(TransitionError transitionError) {
		this.transitionError = transitionError;
		return true;
	}
	
	public TransitionError getTransitionError() {
		return transitionError;
	}
	
	public boolean hasTransitionError() {
		if (transitionError == TransitionError.NOERROR) {
			return false;
		} else {
			return true;
		}
	}

	public boolean clearTransitionError() {
		this.transitionError = TransitionError.NOERROR;
		return true;
	}
	
	public boolean removeTransitionError(TransitionError transitionError) {
		if (this.transitionError == transitionError) {
			this.transitionError = TransitionError.NOERROR;
			return true;
		}
		return false;
	}
	/**
	 * Get the current {@code State} of this bundle node.
	 * 
	 * @return the current state of this bundle node or null if no state has been assigned yet.
	 */
	public BundleState getCurrentState() {
		return currentState;
	}

	/**
	 * Creates a bundle node with a one-to-one relationship between a project and a bundle, called a bundle
	 * project. The bundle id is stored instead of the bundle object in the node. Initially the bundle node has
	 * no state and initialized to {@link no.javatime.inplace.bundlemanager.state.StateLess}
	 * 
	 * @param bundle must not be null
	 * @param project must not be null
	 * @param activate should be true if the project is nature enabled (implies that the project is activated)
	 */
	public BundleNode(Bundle bundle, IProject project, Boolean activate) {
		this.project = project;
		this.activated = activate;
		if (null != bundle) {
			this.bundleId = bundle.getBundleId();
		}
		this.currentState = BundleStateFactory.INSTANCE.stateLess;
		transition = Transition.NOTRANSITION;
		transitionError = TransitionError.NOERROR;
	}

	/**
	 * Assigns a state treated as the current state of this bundle node. The external attribute of the state
	 * is not altered.
	 * 
	 * @param currentState the current state of this bundle as any valid sub class of type {@code State}
	 * @see BundleState
	 */
	public void setCurrentState(BundleState currentState) {
		if (Category.DEBUG && Category.getState(Category.fsm)) {
			if (null != bundleId) {
				Bundle bundle = InPlace.getContext().getBundle(bundleId);
				if (null != bundle) {
					TraceMessage.getInstance().getString("state_change", InPlace.getContext().getBundle(bundleId),
							this.currentState.getClass().getSimpleName(), currentState.getClass().getSimpleName());
				} else {
					TraceMessage.getInstance().getString("state_change", project.getName(),
							this.currentState.getClass().getSimpleName(), currentState.getClass().getSimpleName());
				}
			}
		}
		this.currentState = currentState;
	}

	/**
	 * The symbolic key of the bundle is the concatenation of the symbolic name and the version
	 * 
	 * @return the key of the bundle as a concatenation of the symbolic name and the bundle version or null
	 * if the bundle is missing
	 * @throws ExtenderException if there exists a bundle id and the bundle could not be retrieved
	 */
	public final String getSymbolicKey() throws ExtenderException {
		if (null == bundleId) {
			return null;
		}
		Bundle bundle = InPlace.getContext().getBundle(bundleId);
		if (null == bundle) {
			throw new ExtenderException("illegal_bundle_id", project.getName());
		}
		return bundle.getSymbolicName() + bundle.getVersion();
	}

	/**
	 * Formats the key as symbolic name_version If bundle is not null the cached version is fetched, otherwise
	 * the referenced version from manifest is read. At least one of the specified parameters must not be null
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
				symbolicName = BundleProject.getSymbolicNameFromManifest(project);
				version = BundleProject.getBundleVersionFromManifest(project);
			} catch (ExtenderException e) {
			}
			if (null != symbolicName && null != version) {
				key.append(symbolicName);
				key.append('_');
				key.append(version);
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
		return bundleId;
	}
	
	/**
	 * Get the bundle object from the specified bundle id
	 * @param bundleId the id used to retrieve the bundle object
	 * @return the bundle object or null
	 */
	public final Bundle getBundle(Long bundleId) {
		return InPlace.getContext().getBundle(bundleId);
	}
	
	public final void removeBundle() {
		bundleId = null;
	}
	/**
	 * The unique bundle id of the bundle
	 * 
	 * @param bundleId the unique bundleId to set
	 */
	public final void setBundleId(Long bundleId) {
		this.bundleId = bundleId;
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
	 * A bundle is activated if it has the JavaTime nature and at least installed.
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
		return pendingCommands;
	}

	/**
	 * Register a set of pending bundle operations
	 * 
	 * @param operations to register with this bundle
	 */
	public void setPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingCommands = operations;
	}

	/**
	 * Add a pending bundle operation to the bundle.
	 * 
	 * @param operation pending operation to add to the bundle
	 */
	public void addPendingCommand(BundleTransition.Transition operation) {
		pendingCommands.add(operation);
	}

	/**
	 * Add all specified operations
	 * 
	 * @param operations to add to the bundle
	 */
	public void addPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingCommands.addAll(operations);
	}

	/**
	 * Check if the node contains one of the specified operations
	 * 
	 * @param operations a collection of operations to check against
	 * @return true if one of the specified operations is associated with this bundle node. Otherwise false.
	 */
	public boolean containsPendingCommand(EnumSet<BundleTransition.Transition> operations) {
		for (BundleTransition.Transition op : operations) {
			if (this.pendingCommands.contains(op)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * Check if an operation is associated with this bundle node
	 * 
	 * @param operation associated with this bundle node
	 * @param remove the operation from the bundle node before returning
	 * @return true if this operation is associated with this bundle node
	 */
	public boolean containsPendingCommand(BundleTransition.Transition operation, boolean remove) {
		// TODO if (remove) return remove() else return contains() 
		boolean hasOperation = pendingCommands.contains(operation);
		if (remove) {
			pendingCommands.remove(operation);
		}
		return hasOperation;
	}

	/**
	 * Check if the node contains all of the specified operations
	 * 
	 * @param operations a collection of operations to check against
	 * @return true if all the specified operations is associated with this bundle node. Otherwise false.
	 */
	public boolean containsPendingCommands(EnumSet<BundleTransition.Transition> operations) {
		return this.pendingCommands.containsAll(operations);
	}

	/**
	 * Remove a pending operation and the reason associated with the operation from this bundle node
	 * 
	 * @param operation to remove from this bundle node
	 */
	public Boolean removePendingCommand(BundleTransition.Transition operation) {
		return this.pendingCommands.remove(operation);
	}

	/**
	 * Remove all specified operations
	 * 
	 * @param operations to remove
	 */
	public void removePendingCommands(EnumSet<BundleTransition.Transition> operations) {
		this.pendingCommands.removeAll(operations);
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
	static public String getTransitionName(Transition transition) {
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
