package no.javatime.inplace.region.manager;

import java.util.Collection;
import java.util.EnumSet;

import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.state.BundleNode;

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
	public Transition getTransition(String transitionName) {
		return BundleNode.getTransition(transitionName);
	}

	@Override
	public boolean setBuildTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		bn.setBuildTransitionError(TransitionError.ERROR);
		return true;
	}

	@Override
	public boolean setBuildTransitionError(IProject project, TransitionError error) throws ProjectLocationException {

		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		bn.setBuildTransitionError(error);
		return true;
	}
	
	@Override
	public boolean setBuildTransitionError(Bundle bundle) {
		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		bn.setBuildTransitionError(TransitionError.ERROR);
		return true;
	}

	@Override
	public boolean setBuildTransitionError(Bundle bundle, TransitionError error) {
		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		bn.setBuildTransitionError(error);
		return true;
	}
	
	@Override
	public boolean hasBuildTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}
		return bn.hasBuildTransitionError();
	}

	@Override
	public boolean hasBuildTransitionError(Bundle bundle) {
		BundleNode bn = ws.getBundleNode(bundle);
		if (null == bn) {
			return false;
		}
		return bn.hasBuildTransitionError();
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
	public TransitionError getBuildError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return TransitionError.NOERROR;
		}		
		return bn.getBuildTransitionError();
	}

	public TransitionError getBundleError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return TransitionError.NOERROR;
		}		
		return bn.getBundleTransitionError();
	}


	@Override
	public TransitionError getBuildError(Bundle bundle) throws ProjectLocationException {

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
	
	@Override
	public boolean clearBuildTransitionError(IProject project) throws ProjectLocationException {

		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.clearBuildTransitionError();
	}

	/**
	 * Remove the specified transition from bundle projects
	 * 
	 * @param transitionError to remove from all bundle projects containing the error
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	@SuppressWarnings("unused")
	private boolean removeBuildTransitionError(IProject project, TransitionError transitionError) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.removeBuildTransitionError(transitionError);
	}
	
	/**
	 * Get all projects among the specified projects that contains the specified pending transition
	 * 
	 * @param projects bundle projects to check for the specified transition
	 * @param transition transition to check for in the specified projects
	 * @return all projects among the specified projects containing the specified transition or an
	 * empty collection
	 */
	@SuppressWarnings("unused")
	private void removeBuildTransitionError(TransitionError transitionError) throws ProjectLocationException {
		for (IProject project : ws.getProjects()) {
			BundleNode bn = ws.getBundleNode(project);
			if (null == bn) {
				continue;
			}		
			bn.removeBuildTransitionError(transitionError);			
		}
	}

	/**
	 * Get all bundles among the specified bundles that contains the specified transition
	 * 
	 * @param bundles bundle projects to check for the specified transition
	 * @param transition transition to check for in the specified projects
	 * @return all bundles among the specified bundles containing the specified transition or an empty
	 * collection
	 */
	@SuppressWarnings("unused")
	private boolean removeBuildTransitionError(Bundle bundle, TransitionError transitionError) {

		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		return bn.removeBuildTransitionError(transitionError);
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
