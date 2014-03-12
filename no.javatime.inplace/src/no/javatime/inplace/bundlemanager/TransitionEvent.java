package no.javatime.inplace.bundlemanager;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

import no.javatime.inplace.bundlemanager.BundleTransition.Transition;

public class TransitionEvent {

	Transition transition;
	Bundle bundle;
	
	public TransitionEvent (Bundle bundle, Transition transition) {
		this.bundle = bundle;
		this.transition = transition;
	}
	
	public Bundle getBundle() {
		return bundle;
	}

	public IProject getProject() {
		return BundleManager.getRegion().getProject(bundle);
	}
	
	public Transition getTransition() {
		return transition;
	}
}
