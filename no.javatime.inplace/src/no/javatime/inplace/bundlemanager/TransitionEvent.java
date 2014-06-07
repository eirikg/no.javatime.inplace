package no.javatime.inplace.bundlemanager;

import no.javatime.inplace.bundlemanager.BundleTransition.Transition;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class TransitionEvent {

	Transition transition;
	Bundle bundle;
	IProject project;
	
	public TransitionEvent (Bundle bundle, Transition transition) {
		this.bundle = bundle;
		this.project = BundleManager.getRegion().getProject(bundle);
		this.transition = transition;
	}

	public TransitionEvent (IProject project, Transition transition) {
		this.project = project;
		this.bundle = BundleManager.getRegion().get(project);
		this.transition = transition;
	}
	
	public Bundle getBundle() {
		return bundle;
	}

	public IProject getProject() {
		return project;
	}
	
	public Transition getTransition() {
		return transition;
	}
}
