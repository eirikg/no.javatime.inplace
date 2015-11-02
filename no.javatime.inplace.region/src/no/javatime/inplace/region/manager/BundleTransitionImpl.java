package no.javatime.inplace.region.manager;

import java.util.Collection;
import java.util.EnumSet;

import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.state.BundleNode;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class BundleTransitionImpl implements BundleTransition {

	public final static BundleTransitionImpl INSTANCE = new BundleTransitionImpl();

	private WorkspaceRegionImpl ws = WorkspaceRegionImpl.INSTANCE;

	public BundleTransitionImpl() {
	}

	@Override
	public Transition getTransition(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return Transition.NO_TRANSITION;
		}
		return bn.getTransition();
	}

	@Override
	public String getTransitionName(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return BundleNode.getTransitionName(Transition.NO_TRANSITION, false, false);
		}
		return BundleNode.getTransitionName(bn.getTransition(), false, false);
	}

	@Override
	public String getTransitionName(Transition transition, boolean format, boolean caption) {
		return BundleNode.getTransitionName(transition, format, caption);
	}

	@Override
	public IBundleStatus getTransitionStatus(IProject project) {
		BundleNode node = ws.getBundleNode(project);
		if (null != node) {
			return node.getTransitionStatus();
		}
		return null;
	}
	
	@Override
	public TransitionError getTransitionError(IProject project) {

		BundleNode node = ws.getBundleNode(project);
		if (null != node) {
			return node.getTransitionError();
		}
		return TransitionError.NOERROR;
	}

	@Override
	public void setBuildStatus(IProject project, TransitionError transitionError,
			IBundleStatus status) {
		BundleNode node = ws.getBundleNode(project);
		if (null != node) {
			node.setBuildStatus(transitionError, status);
		}
	}

	@Override
	public void setBundleStatus(IProject project, TransitionError transitionError,
			IBundleStatus status) {
		BundleNode node = ws.getBundleNode(project);
		if (null != node) {
			node.setBundleStatus(transitionError, status);
		}
	}
	
	@Override
	public boolean hasBuildTransitionError(TransitionError transitionError) {
		for (IProject project : ws.getProjects()) {
			BundleNode bn = ws.getBundleNode(project);
			if  (bn.hasBuildTransitionError()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public TransitionError getBuildTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return TransitionError.NOERROR;
		}		
		return bn.getBuildTransitionError();
	}

	@Override
	public TransitionError getBuildTransitionError(Bundle bundle) throws ProjectLocationException {
	
		if (null == bundle) {
			return TransitionError.NOERROR;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		// TODO NB! check why bundle node becomes null. Happens sometimes on uninstall at shutdown
		if (null == bn) {
			return TransitionError.NOERROR;			
		}
		return bn.getBuildTransitionError();
	}

	public TransitionError getBundleTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return TransitionError.NOERROR;
		}		
		return bn.getBundleTransitionError();
	}

	@Override
	public boolean clearBuildTransitionError(IProject project) throws ProjectLocationException {

		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.clearBuildTransitionError();
	}

	public boolean clearBundleTransitionError(IProject project) throws ProjectLocationException  {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.clearBundleTransitionError();		
	}
	@Override
	public Transition getTransition(String transitionName) {
		return BundleNode.getTransition(transitionName);
	}


	@Override
	public Transition setTransition(IProject project, Transition transition) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return null;
		}
		return bn.setTransition(transition);
	}

	@Override
	public Transition setTransition(Bundle bundle, Transition transition) {
		BundleNode bn = ws.getBundleNode(bundle);
		if (null == bn) {
			return null;
		}		
		return bn.setTransition(transition);
	}
	
	@Override
	public Transition getTransition(Bundle bundle) {
		BundleNode bn = ws.getBundleNode(bundle);
		if (null == bn) {
			return null;
		}		
		return bn.getTransition();
	}

	@Override
	public boolean containsPending(IProject project, Transition operation, boolean remove) {
		return ws.containsPendingCommand(project, operation, remove);
	}

	@Override
	public boolean containsPending(Collection<Bundle> bundles, Transition operation, boolean remove) {
		for (Bundle bundle : bundles) {
			if(ws.containsPendingCommand(bundle, operation, remove)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsPending(Bundle bundle, Transition operation, boolean remove) {
		return ws.containsPendingCommand(bundle, operation, remove);
	}
	
	@Override
	public boolean containsPending(Transition transition) {
		return ws.containsPendingCommand(transition);
	}
	
	@Override
	public EnumSet<Transition> getPendingTransitions(IProject project) {
		return ws.getPendingCommands(project);
	}
	
	@Override
	public Collection<IProject> getPendingProjects(Collection<IProject> projects, Transition command) {
		return ws.getPendingProjects(projects, command);
	}

	@Override
	public Collection<Bundle> getPendingBundles(Collection<Bundle> bundles, Transition command) {
		return ws.getPendingBundles(bundles, command);
	}

	@Override
	public void addPending(Bundle bundle, Transition operation) {
		ws.addPendingCommand(bundle, operation);
	}

	@Override
	public void addPending(IProject project, Transition operation) {
		ws.addPendingCommand(project, operation);
	}

	@Override
	public void addPendingCommand(Collection<IProject> projects, Transition operation) {
		ws.addPendingCommand(projects, operation);
	}
	
	@Override
	public boolean removePending(Bundle bundle, Transition operation) {
		return ws.removePendingCommand(bundle, operation);
	}

	@Override
	public boolean removePending(IProject project, Transition operation) {
		return ws.removePendingCommand(project, operation);
	}

	@Override
	public boolean removePending(Collection<IProject> projects, Transition operation) {
		boolean removed = true;
		for (IProject project : projects) {
			if (!ws.removePendingCommand(project, operation)) {
				removed = false;
			}
		}
		return removed;
	}
}
