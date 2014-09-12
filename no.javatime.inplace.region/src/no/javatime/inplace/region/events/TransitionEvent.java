package no.javatime.inplace.region.events;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleWorkspaceRegionImpl;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class TransitionEvent {

	Transition transition;
	Bundle bundle;
	IProject project;
	
	public TransitionEvent (Bundle bundle, Transition transition) {
		this.bundle = bundle;
		this.project = BundleWorkspaceRegionImpl.INSTANCE.getRegisteredBundleProject(bundle); 
		this.transition = transition;
	}

	public TransitionEvent (IProject project, Transition transition) {
		this.project = project;
		this.bundle =  BundleWorkspaceRegionImpl.INSTANCE.get(project); 
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
