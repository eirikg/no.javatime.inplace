package no.javatime.inplace.bundlemanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

import no.javatime.inplace.bundlemanager.state.BundleNode;
import no.javatime.inplace.dependencies.ProjectSorter;

class BundleTransitionImpl implements BundleTransition {

	public BundleTransitionImpl() {
	}
	
	final static BundleTransitionImpl INSTANCE = new BundleTransitionImpl();

	private BundleWorkspaceImpl ws = BundleWorkspaceImpl.INSTANCE;


	@Override
	public Transition getTransition(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return Transition.NOTRANSITION;
		}
		return bn.getTransition();
//		String location = ws.getBundleLocationIdentifier(project);
//		return activeTransition.get(location);
	}

	@Override
	public String getTransitionName(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return BundleNode.getTransitionName(Transition.NOTRANSITION);
		}
		return BundleNode.getTransitionName(bn.getTransition());
//		String location = ws.getBundleLocationIdentifier(project);
//		return activeTransition.getName(location);
	}

	@Override
	public String getTransitionName(Transition transition) {
		return BundleNode.getTransitionName(transition);
//		return activeTransition.getName(transition);
	}

	@Override
	public boolean setTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.setTransitionError(TransitionError.ERROR);
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.setError(location, TransitionError.ERROR);			
	}

	public boolean setTransitionError(IProject project, TransitionError error) throws ProjectLocationException {

		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.setTransitionError(error);
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.setError(location, error);			
	}
	
	@Override
	public boolean setTransitionError(Bundle bundle) {
		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		return bn.setTransitionError(TransitionError.ERROR);
//		return this.activeTransition.setError(bundle.getLocation(), TransitionError.ERROR);			
	}

	public boolean setTransitionError(Bundle bundle, TransitionError error) {
		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		return bn.setTransitionError(error);
//		return this.activeTransition.setError(bundle.getLocation(), error);			
	}
	
	@Override
	public boolean hasTransitionError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}
		return bn.hasTransitionError();
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.hasError(location);
	}

	@Override
	public boolean hasTransitionError(TransitionError transitionError) {
		for (IProject project : ws.getProjects()) {
			BundleNode bn = ws.getBundleNode(project);
			if  (bn.hasTransitionError()) {
				return true;
			}
		}
		return false;
//		return activeTransition.hasError(transitionError);
	}
	
	@Override
	public TransitionError getError(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return TransitionError.NOERROR;
		}		
		return bn.getTransitionError();
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.getError(location);
	}

	@Override
	public TransitionError getError(Bundle bundle) throws ProjectLocationException {

		if (null == bundle) {
			return TransitionError.NOERROR;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		return bn.getTransitionError();
		//return this.activeTransition.getError(bundle.getLocation());
	}
	
	@Override
	public boolean clearTransitionError(IProject project) throws ProjectLocationException {

		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.clearTransitionError();
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.clearError(location);
	}

	@Override
	public boolean removeTransitionError(IProject project, TransitionError transitionError) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return false;
		}		
		return bn.removeTransitionError(transitionError);
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.removeError(location, transitionError);
	}
	
	@Override
	public void removeTransitionError(TransitionError transitionError) throws ProjectLocationException {
		for (IProject project : ws.getProjects()) {
			BundleNode bn = ws.getBundleNode(project);
			if (null == bn) {
				continue;
			}		
			bn.removeTransitionError(transitionError);			
		}
	}

	@Override
	public boolean removeTransitionError(Bundle bundle, TransitionError transitionError) {

		if (null == bundle) {
			return false;
		}
		BundleNode bn = ws.getBundleNode(bundle);
		return bn.removeTransitionError(transitionError);
//		return this.activeTransition.removeError(bundle.getLocation(), transitionError);
	}
	@Override
	public Collection<Bundle> removeTransitionErrorClosures(Collection<Bundle> initialBundleSet,
			Collection<Bundle> bDepClosures, Collection<IProject> pDepClosures) {

		Collection<Bundle> bErrorDepClosures = null;
		Collection<Bundle> errorBundles = new ArrayList<Bundle>();
		for (Bundle errorBundle : initialBundleSet) {
			IProject errorProject = BundleManager.getRegion().getProject(errorBundle);
			TransitionError transitionError = getError(errorBundle);
			if (null != errorProject && ( transitionError == TransitionError.DUPLICATE 
					|| transitionError == TransitionError.CYCLE)) {
				errorBundles.add(errorBundle);
			}
		}
		if (errorBundles.size() > 0) {
			// Get all projects with errors and their requiring projects from dependency closures
			ProjectSorter ps = new ProjectSorter();
			Collection<IProject> pErrorDepClosures = ps.sortRequiringProjects(BundleManager.getRegion().getProjects(errorBundles));
			if (pErrorDepClosures.size() > 0) {
				if (null != pDepClosures) {
					pDepClosures.removeAll(pErrorDepClosures);
				}
				bErrorDepClosures = BundleManager.getRegion().getBundles(pErrorDepClosures);
				if (bErrorDepClosures.size() > 0) {
					if (null != bDepClosures) {
						bDepClosures.removeAll(bErrorDepClosures);
					}
					initialBundleSet.removeAll(bErrorDepClosures);
				}
			}
		}
		return bErrorDepClosures;
	}

	@Override
	public void initTransition(IProject project) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return;
		}		
		bn.setTransition(Transition.UNINSTALL);
//		String location = ws.getBundleLocationIdentifier(project);
//		setTransition(location, Transition.UNINSTALL);
	}

	@Override
	public Transition setTransition(IProject project, Transition transition) throws ProjectLocationException {
		BundleNode bn = ws.getBundleNode(project);
		if (null == bn) {
			return null;
		}
		bn.clearTransitionError();
		return bn.setTransition(transition);
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.put(location, transition);
	}
	@Override
	public Transition setTransition(Bundle bundle, Transition transition) {
		BundleNode bn = ws.getBundleNode(bundle);
		if (null == bn) {
			return null;
		}		
		bn.clearTransitionError();
		return bn.setTransition(transition);
//		String location = ws.getBundleLocationIdentifier(project);
//		return this.activeTransition.put(location, transition);
	}

	@Override
	public boolean containsPending(IProject project, Transition operation, boolean remove) {
		return ws.containsPendingCommand(project, operation, remove);
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
	public EnumSet<BundleTransition.Transition> getPendingTransitions(IProject project) {
		return ws.getPendingCommands(project);
	}
	
	@Override
	public Collection<IProject> getPendingProjects(Collection<IProject> projects, BundleTransition.Transition command) {
		return ws.getPendingProjects(projects, command);
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
	public boolean removePending(Bundle bundle, Transition operation) {
		return ws.removePendingCommand(bundle, operation);
	}

	@Override
	public boolean removePending(IProject project, Transition operation) {
		return ws.removePendingCommand(project, operation);
	}

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
