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
package no.javatime.inplace.bundlemanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.state.BundleNode;
import no.javatime.inplace.bundlemanager.state.BundleState;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.TraceMessage;

/**
 * Region for workspace bundles. Associate projects with workspace bundles, support static (not resolved) and
 * dynamic bidirectional traversal of bundle dependencies and a factory for resolver hook handlers filtering
 * bundles from resolving and enforcement of singletons.
 */
class BundleWorkspaceImpl implements BundleRegion {

	final static BundleWorkspaceImpl INSTANCE = new BundleWorkspaceImpl();

	// Default initial capacity of 16 assume peak on 22 bundles in workspace to avoid rehash
	private static int initialCapacity = Math.round(20 / 0.75f) + 1;
	
	private boolean autoBuild;


	/**
	 * Internal hash of bundle nodes. Viewed as a DAG when used in combination with the OSGI wiring API.
	 * <p>
	 * Bundle id is the key and can't change while a bundle is installed. Insertions and removals are done
	 * indirectly through bundle jobs when bundles are installed and uninstalled. Bundle jobs does not run
	 * concurrently. Reads outweighs structural modifications.
	 */
	private Map<IProject, BundleNode> bundleNodes = new ConcurrentHashMap<IProject, BundleNode>(initialCapacity, 1);

	protected BundleWorkspaceImpl() {
		super();
	}
	
	public boolean isAutoBuild(boolean disable) {
		boolean autoBuild = this.autoBuild;
		if (disable) {
			this.autoBuild = false;
		}
		return autoBuild;
	}

	public void setAutoBuild(boolean autoBuild) {
		this.autoBuild = autoBuild;
	}

	@Override
	public IProject getProject(Bundle bundle) {
		if (null == bundle) {
			return null;
		}
		BundleNode node = getNode(bundle);
		if (null != node) {
			return node.getProject();
		} else {
			// Uninstalled bundles are not registered in the workspace region
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IPath bundlePathLoc = new Path(bundle.getLocation());
			for (IProject bundleProject : workspace.getRoot().getProjects()) {	
				IPath projectPathLoc = new Path(getBundleLocationIdentifier(bundleProject));
				if (bundlePathLoc.equals(projectPathLoc)) {
					return bundleProject;
				}
			}
		}
		return null;
	}

	@Override
	public IProject getProject(String symbolicName, String version) {
		BundleNode node = getNode(symbolicName, version);
		if (null != node) {
			return node.getProject();
		}
		return null;
	}

	/**
	 * Retrieves the bundle location identifier as an absolute platform-dependent file system path of the
	 * specified project prepended with the reference file scheme (reference:file:/).
	 * <p>
	 * If the associated workspace bundle of the specified project is installed {@link Bundle#getLocation()} is
	 * used.
	 * 
	 * @param project which is the base for finding the path
	 * @return the absolute file system path of project prepended with the URI scheme
	 * @throws ProjectLocationException if the specified project is null or the location of the specified project could
	 *           not be found
	 */
	@Override
	public String getBundleLocationIdentifier(IProject project) throws ProjectLocationException, InPlaceException {

		Bundle bundle = get(project);
		if (null != bundle) {
			try {
				return bundle.getLocation();				
			} catch (SecurityException e) {
				throw new InPlaceException(e, "project_security_error", project.getName());
			}
		} else {
			return ProjectProperties.getProjectLocationIdentifier(project, true);
		}
	}

	/**
	 * Check if the workspace is activated. The condition is satisfied if one project is JavaTime nature enabled
	 * and its bundle project is at least installed.
	 * 
	 * @return true if at least one project is JavaTime nature enabled and its bundle project is not
	 *         uninstalled. Otherwise false
	 * @see BundleWorkspaceImpl#isActivated(Bundle)
	 * @see no.javatime.inplace.bundleproject.ProjectProperties#isProjectWorkspaceActivated()
	 */
	@Override
	public Boolean isBundleWorkspaceActivated() {
		
		for (BundleNode node : bundleNodes.values()) {
			if (node.isActivated()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the bundle is activated. The condition is satisfied if the project is JavaTime nature enabled
	 * and its bundle project is at least installed.
	 * 
	 * @param project to check for activation
	 * @return true if the specified project is JavaTime nature enabled and its bundle project is not
	 *         uninstalled. Otherwise false
	 * @see BundleWorkspaceImpl#isActivated(Bundle)
	 * @see no.javatime.inplace.bundleproject.ProjectProperties#isProjectWorkspaceActivated()
	 */
	@Override
	public Boolean isActivated(IProject project) {
		BundleNode node = getNode(project);
		if (null == node) {
			return false;
		} else {
			return node.isActivated();
		}
	}

	@Override
	public Boolean isActivated(Long bundleId) {
		BundleNode node = getNode(bundleId);
		if (null == node) {
			return false;
		} else {
			return node.isActivated();
		}
	}

	@Override
	public Boolean isActivated(Bundle bundle) {
		BundleNode node = getNode(bundle);
		if (null == node) {
			return false;
		} else {
			return node.isActivated();
		}
	}

	@Override
	public Collection<IProject> getProjects(Boolean activated) {
		Collection<IProject> projects = new ArrayList<IProject>();
		for (BundleNode node : bundleNodes.values()) {
			if (node.isActivated()) {
				projects.add(node.getProject());
			}
		}
		return projects;
	}

	@Override
	public Collection<IProject> getProjects() {
		Collection<IProject> projects = new ArrayList<IProject>();
		for (BundleNode node : bundleNodes.values()) {
			projects.add(node.getProject());
		}
		return projects;
	}

	@Override
	public Collection<IProject> getProjects(Collection<Bundle> bundles) {
		Collection<IProject> projects = new ArrayList<IProject>();
		for (Bundle bundle : bundles) {
			projects.add(getProject(bundle));
		}
		return projects;
	}

	@Override
	public Collection<Bundle> getActivatedBundles() {
		Collection<Bundle> bundles = new ArrayList<Bundle>();
		for (BundleNode node : bundleNodes.values()) {
			if (node.isActivated()) {
				Long bundleId = node.getBundleId();
				if (null != bundleId) {
					Bundle bundle = InPlace.getContext().getBundle(bundleId);
					if (null != bundle) {
						bundles.add(bundle);
					}
				}
			}
		}
		return bundles;
	}

	@Override
	public Collection<Bundle> getDeactivatedBundles() {
		Collection<Bundle> bundles = new ArrayList<Bundle>();
		for (BundleNode node : bundleNodes.values()) {
			if (!node.isActivated()) {
				Long bundleId = node.getBundleId();
				if (null != bundleId) {
					Bundle bundle = InPlace.getContext().getBundle(bundleId);
					if (null != bundle) {
						bundles.add(bundle);
					}
				}
			}
		}
		return bundles;
	}

	@Override
	public Collection<Bundle> getBundles(int state) {
		Collection<Bundle> bundles = new ArrayList<Bundle>();
		for (BundleNode node : bundleNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = InPlace.getContext().getBundle(bundleId);
				if (null != bundle && (bundle.getState() & (state)) != 0) {
					bundles.add(bundle);
				}
			}
		}
		return bundles;
	}

	@Override
	public Collection<Bundle> getBundles() {
		Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
		for (BundleNode node : bundleNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = InPlace.getContext().getBundle(bundleId);
				if (null != bundle) {
					bundles.add(bundle);
				}
			}
		}
		return bundles;
	}
	
	@Override
	public Collection<Bundle> getJarBundles() {
		Collection<Bundle> workspaceBundles = getBundles();
		Set<Bundle> allBundles = new LinkedHashSet<Bundle>();
		Collections.addAll(allBundles, InPlace.getContext().getBundles());
		allBundles.removeAll(workspaceBundles);
		return allBundles;
	}

	@Override
	public Collection<Bundle> getBundles(Collection<IProject> projects) {
		Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
		for (IProject project : projects) {
			Bundle bundle = get(project);
			if (null != bundle) {
				bundles.add(bundle);
			} else {
				// Bundle is uninstalled, but node not removed
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("bundle_not_installed", project.getName());
			}
		}
		return bundles;
	}

	@Override
	public Bundle get(IProject project) {
		BundleNode node = getNode(project);
		if (null != node) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				return InPlace.getContext().getBundle(bundleId);
			}
		}
		return null;
	}
	
	public Bundle get(Long bundleId) {
		BundleNode node = getNode(bundleId);
		if (null != node) {
			return node.getBundle(bundleId);
		}
		return null;
	}
	
	public Map<IProject, IProject> getWorkspaceDuplicates(Collection<IProject> candidateProjects, Collection<IProject> scope) {

		Map<IProject, IProject> duplicateMap = new HashMap<IProject, IProject>();
		Map<String, IProject> candidateKeyMap = new HashMap<String, IProject>();
		for (IProject candidateProject : candidateProjects) {
			String symbolicKey = getSymbolicKey(null, candidateProject);
			if (symbolicKey.length() == 0) {
				continue;
			}
			IProject project = candidateKeyMap.put(symbolicKey, candidateProject);
			if (null != project) {
				duplicateMap.put(candidateProject, project);
			}
		}		
		if (candidateKeyMap.size() > 0) {			
			scope.removeAll(candidateKeyMap.values());
			for (IProject project : scope) {
				String symbolicKey = getSymbolicKey(null, project);
				if (symbolicKey.length() == 0) {
					continue;
				}
				if (candidateKeyMap.containsKey(symbolicKey)) {
					IProject duplicateProject = candidateKeyMap.get(symbolicKey);
					duplicateMap.put(duplicateProject, project);
				}
			}
		}
		return duplicateMap;
	}

	public Map<IProject, Bundle> getSymbolicNameDuplicates(Collection<IProject> projects, Collection<Bundle> candidateBundles, boolean disjoint) {

		Map<String, IProject> newSymbolicNameMap= new HashMap<String, IProject>();
		Map<IProject, Bundle> duplicateMap= new HashMap<IProject, Bundle>();

		for (IProject project : projects) {
			try {
				String symbolicName = BundleProject.getSymbolicNameFromManifest(project);
				newSymbolicNameMap.put(symbolicName, project);
			} catch (InPlaceException e) {
			}
		}
		if (disjoint) {
			Collection<Bundle> bundleProjects = getBundles(projects);
			bundleProjects.retainAll(candidateBundles);
			if (bundleProjects.size() > 0) {
				candidateBundles.removeAll(bundleProjects);
			}
		}
		for (Bundle bundle : candidateBundles) {
			String symbolicName = bundle.getSymbolicName();
			if (newSymbolicNameMap.containsKey(symbolicName)) {
				duplicateMap.put(newSymbolicNameMap.get(symbolicName), bundle);
			}
		}
		return duplicateMap;
	}

	@Override
	public Boolean exist(Bundle bundle) {
		return (getNode(bundle) != null) ? true : false;
	}

	@Override
	public String setActivation(Bundle bundle, Boolean status) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			node.setActivated(status);
			return node.getSymbolicKey();
		}
		return null;
	}

	@Override
	public Boolean exist(String symbolicName, String version) {
		BundleNode bn = getNode(symbolicName, version);
		if (null != bn) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	/**
	 * Concatenates symbolic name and bundle version (<symbolic name>_<version>)
	 * 
	 * @param project containing key to format
	 * @return the symbolic key or an empty string
	 */
	@Override
	public String getSymbolicKey(Bundle bundle, IProject project) {
		return BundleNode.formatSymbolicKey(bundle, project);
	}

	@Override
	public String formatBundleList(Collection<Bundle> bundles, boolean includeVersion) {
		StringBuffer sb = new StringBuffer();
		if (null != bundles && bundles.size() >= 1) {
			for (Iterator<Bundle> iterator = bundles.iterator(); iterator.hasNext();) {
				Bundle bundle = iterator.next();
				sb.append(bundle.getSymbolicName());
				if (includeVersion) {
					sb.append('_');
					sb.append(bundle.getVersion().toString());
				}
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Add a pending bundle operation to the bundle project.
	 * 
	 * @param project bundle project to add the pending operation to
	 * @param operation to register with this bundle project
	 */
	void addPendingCommand(IProject project, BundleTransition.Transition operation) {
		BundleNode bn = getNode(project);
		if (null != bn) {
			bn.addPendingCommand(operation);
		}
	}

	/**
	 * Add a pending bundle operation to the bundle.
	 * 
	 * @param bundle bundle project to add the pending operation to
	 * @param operation to register with this bundle project
	 */
	void addPendingCommand(Bundle bundle, BundleTransition.Transition operation) {
		BundleNode bn = getNode(bundle);
		if (null != bn) {
			bn.addPendingCommand(operation);
		}
	}

	/**
	 * Get all projects among the specified projects that contains the specified pending transition
	 * 
	 * @param projects to check for having the specified transition
	 * @param command or transition to check for in the specified projects
	 * @return all projects among the specified projects containing the specified transition or an empty
	 *         collection
	 */
	Collection<IProject> getPendingProjects(Collection<IProject> projects, BundleTransition.Transition command) {
		Collection<IProject> pendingProjects = new LinkedHashSet<IProject>();
		for (IProject project : projects) {
			if (containsPendingCommand(project, command, false)) {
				pendingProjects.add(project);
			}
		}
		return pendingProjects; 
	}

	/**
	 * Get all pending operations for a project bundle
	 * 
	 * @param project bundle project to get pending operations for
	 * @return all pending operations for this bundle project
	 */
	EnumSet<BundleTransition.Transition> getPendingCommands(IProject project) {
		BundleNode bn = getNode(project);
		if (null != bn) {
			return bn.getPendingCommands();
		}
		return null;
	}

	/**
	 * Check if the bundle project has the specified pending command attached to it.
	 * 
	 * @param project bundle project to check for the specified pending command
	 * @param command associated with this bundle project
	 * @param remove clear the command from the bundle project if true
	 * @return true if this command is associated with this bundle project
	 */
	boolean containsPendingCommand(IProject project, BundleTransition.Transition command, boolean remove) {
		BundleNode bn = getNode(project);
		if (null != bn) {
			return bn.containsPendingCommand(command, remove);
		}
		return false;
	}

	/**
	 * Check if the bundle project has the specified pending command attached to it.
	 * 
	 * @param bundle bundle project to check for the specified pending command
	 * @param command associated with this bundle project
	 * @param remove clear the command from the bundle project if true
	 * @return true if this command is associated with this bundle project
	 */
	boolean containsPendingCommand(Bundle bundle, BundleTransition.Transition command, boolean remove) {
		BundleNode bn = getNode(bundle);
		if (null != bn) {
			return bn.containsPendingCommand(command, remove);
		}
		return false;
	}

	/**
	 * Check if there are any bundle projects with the specified pending command
	 * @param command pending command
	 * @return true if the specified command is associated with any bundle project
	 */
	boolean containsPendingCommand(BundleTransition.Transition command) {
		for (BundleNode node : bundleNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = InPlace.getContext().getBundle(bundleId);
				if (null != bundle && containsPendingCommand(bundle, command, false)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove a pending operation from this bundle project
	 * 
	 * @param project bundle project to remove this operation from
	 * @param operation to remove from this bundle project
	 */
	Boolean removePendingCommand(IProject project, BundleTransition.Transition operation) {
		BundleNode bn = getNode(project);
		if (null != bn) {
			return bn.removePendingCommand(operation);
		}
		return false;
	}

	/**
	 * Remove a pending operation and the reason associated with the operation from this bundle
	 * 
	 * @param bundle bundle project to remove this operation from
	 * @param operation to remove from this bundle project
	 */
	Boolean removePendingCommand(Bundle bundle, BundleTransition.Transition operation) {
		BundleNode bn = getNode(bundle);
		if (null != bn) {
			return bn.removePendingCommand(operation);
		}
		return false;
	}


	BundleNode getBundleNode(Bundle bundle) {
		return getNode(bundle);
	}

	BundleNode getBundleNode(IProject project) {
		return getNode(project);
	}

	BundleState getActiveState(Bundle bundle) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			return node.getCurrentState();
		}
		return null;
	}

	void setActiveState(Bundle bundle, BundleState activeState) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			node.setCurrentState(activeState);
		}
	}

	/**
	 * Record the bundle and its associated project. The project may or may not be activated
	 * 
	 * @param project associated with the bundle. If there is a bundle there must be a project.
	 * @param bundle to record. The bundle id. is the key and must not be null.
	 * @param activate true if project is activated and false if not
	 * @return the new or updated bundle node
	 * @throws InPlaceException if the specified project or bundle parameter is null
	 */
	protected BundleNode put(IProject project, Bundle bundle, Boolean activate) throws InPlaceException {
		if (null == project) {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("npe_project_cache");
			throw new InPlaceException("project_null_location");
		}
//		if (null == bundle) {
//			if (Category.DEBUG && Category.getState(Category.dag))
//				TraceMessage.getInstance().getString("npe_project_cache");
//			throw new InPlaceException("bundle_null_location");
//		}
		BundleNode node = getNode(project);
		// Update node based on bundle
		if (null != node) {
			node.setProject(project);
			if (null != bundle) {
				Long id = bundle.getBundleId();
				node.setBundleId(id);
			} else {
				node.setBundleId(null);
			}
			node.setActivated(activate);
			bundleNodes.put(project, node);
			if (Category.DEBUG && Category.getState(Category.dag)) {
				TraceMessage.getInstance().getString("updated_node", bundleNodes.get(project).getProject());
			}
			// Create a new node
		} else {
			node = new BundleNode(bundle, project, activate);
			bundleNodes.put(project, node);
			if (Category.DEBUG && Category.getState(Category.dag)) {
				TraceMessage.getInstance().getString("inserted_node", bundleNodes.get(project).getProject());
			}
		}
		return node;
	}

	protected Long remove(IProject project) {
		BundleNode node = getNode(project);
		if (null != node) {
			BundleNode deletedNode = bundleNodes.remove(project);
			if (null == deletedNode) {
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("failed_remove_node", project.getName());
				return null;
			} else {
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("removed_node", project.getName());
				return deletedNode.getBundleId();
			}
		} else {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("null_remove_node", project.getName());
		}
		return null;
	}

	/**
	 * Remove a bundle
	 * 
	 * @param bundle to remove
	 * @return the bundle id or null if the bundle is not registered
	 */
	protected Long remove(Bundle bundle) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			BundleNode deletedNode = bundleNodes.remove(node.getProject());
			if (null == deletedNode) {
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("failed_remove_node", bundle.toString());
				return null;
			} else {
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("removed_node", bundle.toString());
				return deletedNode.getBundleId();
			}
		} else {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("null_remove_node", bundle.toString());
		}
		return null;
	}

	/**
	 * Get a bundle node from its associated project
	 * 
	 * @param project associated with the workspace bundle
	 * @return the bundle node or null
	 */
	private BundleNode getNode(IProject project) {
		if (null != project) {
			return bundleNodes.get(project);
		}
		return null;
	}
  
	/**
	 * Finds a bundle node based on the bundle object
	 * 
	 * @param bundle used to identify its bundle node
	 * @return a bundle node or null
	 */
	private BundleNode getNode(Bundle bundle) {
		if (null != bundle) {
			for (BundleNode node : bundleNodes.values()) {
				Long bundleId = node.getBundleId();
				if (null != bundleId && bundleId == bundle.getBundleId()) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Get a bundle node based on the symbolic name and bundle version
	 * 
	 * @param symbolicName of a bundle
	 * @param version bundle version
	 * @return the bundle node for this workspace bundle
	 */
	private BundleNode getNode(String symbolicName, String version) {
		if (null != symbolicName && null != version) {
			for (BundleNode node : bundleNodes.values()) {
				String symbolicKey = node.getSymbolicKey();
				if (null != symbolicKey && symbolicKey.equals(symbolicName + version)) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Get a bundle node based on the bundle id of a bundle
	 * 
	 * @param bundleId the bundle id of the bundle
	 * @return the bundle node for this workspace bundle or null if it does not exist
	 */
	private BundleNode getNode(Long bundleId) {
		if (null != bundleId) {
			for (BundleNode node : bundleNodes.values()) {
				Long id = node.getBundleId();
				if (null != id && id == bundleId) {
					return node;
				}
			}
		}
		return null;
	}
}
