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
package no.javatime.inplace.region.events;

import java.util.EventObject;

import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.state.BundleNode;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;


public class BundleTransitionEvent extends EventObject {
	
	private static final long serialVersionUID = -2472454348847272417L;
	private transient TransitionEvent transitionEvent;
	
	public BundleTransitionEvent(Object source, TransitionEvent transitionEvent) {
    super(source);
    this.transitionEvent = transitionEvent;
	}
	
	public Bundle getBundle () {
		return transitionEvent.getBundle();
	}
	
	public Transition getTransition() {
		return transitionEvent.getTransition();
	}	
	public IProject getProject() {
		return transitionEvent.getProject();
	}
	
	public boolean isStateChanging() {
		IProject project = transitionEvent.getProject(); 
		if (null != project) {			
			BundleNode bn = BundleWorkspaceRegionImpl.INSTANCE.getBundleNode(project); 
			if (null != bn) {
				return bn.isStateChanging();
			}			
		}
		return false;
	}
}
