package no.javatime.inplace.region.events;

import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class TransitionEvent {

	Transition transition;
	Bundle bundle;
	IProject project;
	
	public TransitionEvent (Bundle bundle, Transition transition) {
		this.bundle = bundle;
		this.project = WorkspaceRegionImpl.INSTANCE.getProject(bundle); 
		this.transition = transition;
	}

	public TransitionEvent (IProject project, Transition transition) {
		this.project = project;
		this.bundle =  WorkspaceRegionImpl.INSTANCE.getBundle(project); 
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
