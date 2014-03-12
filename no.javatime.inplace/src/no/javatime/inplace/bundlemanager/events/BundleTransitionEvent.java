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
package no.javatime.inplace.bundlemanager.events;

import java.util.EventObject;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.TransitionEvent;

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
}
