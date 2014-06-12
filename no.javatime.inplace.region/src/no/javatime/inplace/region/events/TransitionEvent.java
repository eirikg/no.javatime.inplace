package no.javatime.inplace.region.events;

import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleWorkspaceImpl;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class TransitionEvent {

	Transition transition;
	Bundle bundle;
	IProject project;
	
	public TransitionEvent (Bundle bundle, Transition transition) {
		this.bundle = bundle;
		this.project = BundleWorkspaceImpl.INSTANCE.getBundleProject(bundle); // BundleManager.getRegion().getProject(bundle);
		this.transition = transition;
	}

	public TransitionEvent (IProject project, Transition transition) {
		this.project = project;
		// TODO Use interface and let workspaceimpl have package visibility
		this.bundle =  BundleWorkspaceImpl.INSTANCE.get(project); // BundleManager.getRegion().get(project);
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
