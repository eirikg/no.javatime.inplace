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
package no.javatime.inplace.region.manager;

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

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.WorkspaceDuplicateException;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
import no.javatime.inplace.region.state.BundleNode;
import no.javatime.inplace.region.state.BundleState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Region for workspace bundles. Associate projects with workspace bundles, support static (not
 * resolved) and dynamic bidirectional traversal of bundle dependencies and a factory for resolver
 * hook handlers filtering bundles from resolving and enforcement of singletons.
 */
public class WorkspaceRegionImpl implements BundleRegion {

	public final static WorkspaceRegionImpl INSTANCE = new WorkspaceRegionImpl();

	// Default initial capacity of 16 assume peak on 22 bundles in workspace to avoid rehash
	private static int initialCapacity = Math.round(20 / 0.75f) + 1;

	/**
	 * Internal hash of bundle project nodes. Viewed as a DAG when used in combination with the OSGI
	 * wiring API.
	 * <p>
	 * {@code IProject} is the key and does not change during an IDE session. Nodes are registered
	 * (insertions) when projects are nature enabled and optionally unregistered (removals) when
	 * bundles are uninstalled. Reads outweighs structural modifications.
	 */
	private Map<IProject, BundleNode> projectNodes = new ConcurrentHashMap<IProject, BundleNode>(
			initialCapacity, 1);

	/**
	 * Bundle nodes as bundle projects. Hash for direct access to projects and bundle nodes with the
	 * bundle id as key
	 */
	private Map<Long, IProject> bundleProjects = new ConcurrentHashMap<Long, IProject>(
			initialCapacity, 1);

	protected WorkspaceRegionImpl() {
		super();
	}

	@Override
	public BundleCommand getCommandService(Bundle user) throws ExtenderException {

		return Activator.getBundleCommandService(user);
	}

	@Override
	public BundleTransition getTransitionService(Bundle user) throws ExtenderException {

		return Activator.getBundleTransitionService(user);
	}

	@Override
	public BundleProjectCandidates getCandidatesService(Bundle user) throws ExtenderException {

		return Activator.getBundleProjectCandidatesService(user);
	}

	@Override
	public BundleProjectMeta getMetaService(Bundle user) throws ExtenderException {

		return Activator.getbundlePrrojectMetaService(user);
	}

	@Override
	public IProject getProject(Bundle bundle) {

		BundleNode node = getNode(bundle);
		if (null != node) {
			return node.getProject();
		}
		if (null == bundle || projectNodes.size() == 0) {
			return null;
		}
		IPath bundlePathLoc = new Path(bundle.getLocation());
		for (IProject project : projectNodes.keySet()) {
			IPath projectPathLoc = new Path(getProjectLocationIdentifier(project,
					BundleRegion.BUNDLE_REF_LOC_SCHEME));
			if (bundlePathLoc.equals(projectPathLoc)) {
				return project;
			}
		}
		return null;
	}

	/**
	 * Get the associated project of the specified bundle.
	 * <p>
	 * First search registered bundle projects than search the entire workspace for the project
	 * 
	 * @param bundle the bundle associated with the project to return
	 * @return the associated project of the specified bundle or null if no project is found or
	 * workspace is closed
	 */
	public IProject getWorkspaceBundleProject(Bundle bundle) {
		if (null == bundle) {
			return null;
		}
		try {
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
		} catch (IllegalStateException e) {
			// Workspace closed
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

	@Override
	public String getProjectLocationIdentifier(IProject project, String locationScheme)
			throws ProjectLocationException {
		if (null == project) {
			throw new ProjectLocationException("project_null_location");
		}
		if (null == locationScheme) {
			locationScheme = BundleRegion.BUNDLE_REF_LOC_SCHEME;
		}
		StringBuffer scheme = new StringBuffer(locationScheme);
		IPath path = project.getLocation();
		if (null == path || path.isEmpty()) {
			throw new ProjectLocationException("project_location_find", project.getName());
		}
		String locIdent = path.toOSString();
		return scheme.append(locIdent).toString();
	}

	@Override
	public String getBundleLocationIdentifier(IProject project) throws ProjectLocationException,
			InPlaceException {

		Bundle bundle = getBundle(project);
		if (null != bundle) {
			try {
				return bundle.getLocation();
			} catch (SecurityException e) {
				throw new InPlaceException(e, "project_security_error", project.getName());
			}
		} else {
			return getProjectLocationIdentifier(project, BundleRegion.BUNDLE_REF_LOC_SCHEME);
		}
	}

	@Override
	public Boolean isRegionActivated() {
		for (BundleNode node : projectNodes.values()) {
			if (null != node && node.isActivated()) {
				return true;
			}
		}
		return false;
	}

	public Bundle isRegionStateChanging() {

		for (BundleNode node : projectNodes.values()) {
			if (null != node && node.isStateChanging()) {
				Bundle bundle = node.getBundle();
				// Verify by interrogating the thread
				// if (!BundleThread.isStateChanging(bundle)) {
				// String msg = NLS.bind(Msg.STATE_CHANGE_ERROR, bundle);
				// StatusManager.getManager().handle(
				// new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, bundle, msg, null),
				// StatusManager.LOG);
				// }
				return bundle;
			}
		}
		return null;
	}

	@Override
	public boolean isProjectRegistered(IProject project) {
		BundleNode node = getBundleNode(project);
		if (null != node) {
			return true;
		}
		return false;
	}

	/**
	 * Register a project and its associated workspace bundle in the workspace region. The bundle must
	 * be registered during or after it is installed but before it is resolved.
	 * <p>
	 * If the bundle does not exists it is initialized with state {@code StateLess} and
	 * {@code Transition.NO_TRANSITION}
	 * <p>
	 * 
	 * @param project the project project to register. Must not be null.
	 * @param bundle the bundle to register. May be null.
	 * @param activateBundle true if the project is activated (nature enabled) and false if not
	 * activated
	 * @return the new updated bundle node
	 */
	public BundleNode registerBundleNode(IProject project, Bundle bundle, Boolean activateBundle) {

		BundleNode node = getBundleNode(project);
		try {
			node = put(project, bundle, activateBundle);
		} catch (InPlaceException e) {
			// Assume project not null
		}
		return node;
	}

	@Override
	public void registerBundleProject(IProject project, Bundle bundle, boolean activateBundle) {
		registerBundleNode(project, bundle, activateBundle);
	}

	@Override
	public void unregisterBundleProject(IProject project) {
		remove(project);
	}

	@Override
	public Boolean isBundleActivated(IProject bundleProject) {

		BundleNode node = getNode(bundleProject);
		return null == node ? false : node.isActivated();
	}

	@Override
	public Boolean isBundleActivated(Long bundleId) {
		BundleNode node = getNode(bundleId);
		return null == node ? false : node.isActivated();
	}

	@Override
	public Boolean isBundleActivated(Bundle bundle) {
		BundleNode node = getNode(bundle);
		return null == node ? false : node.isActivated();
	}

	@Override
	public Collection<IProject> getActivatedProjects() {

		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (IProject project : getProjects()) {
			if (isBundleActivated(project)) {
				projects.add(project);
			}
		}
		return projects;
	}

	@Override
	public Collection<IProject> getProjects(Boolean activated) {
		Collection<IProject> projects = new ArrayList<IProject>();
		for (BundleNode node : projectNodes.values()) {
			if (activated && node.isActivated()) {
				projects.add(node.getProject());
			} else if (!activated && !node.isActivated()) {
				projects.add(node.getProject());
			}
		}
		return projects;
	}

	@Override
	public Collection<IProject> getProjects() {
		Collection<IProject> projects = new ArrayList<IProject>();
		for (BundleNode node : projectNodes.values()) {
			projects.add(node.getProject());
		}
		return projects;
	}

	@Override
	public Collection<IProject> getProjects(Collection<Bundle> bundles) {
		Collection<IProject> projects = new LinkedHashSet<IProject>();
		for (Bundle bundle : bundles) {
			projects.add(getProject(bundle));
		}
		return projects;
	}

	@Override
	public Collection<Bundle> getActivatedBundles() {
		Collection<Bundle> bundles = new ArrayList<Bundle>();
		for (BundleNode node : projectNodes.values()) {
			if (node.isActivated()) {
				Long bundleId = node.getBundleId();
				if (null != bundleId) {
					Bundle bundle = Activator.getContext().getBundle(bundleId);
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
		for (BundleNode node : projectNodes.values()) {
			if (!node.isActivated()) {
				Long bundleId = node.getBundleId();
				if (null != bundleId) {
					Bundle bundle = Activator.getContext().getBundle(bundleId);
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
		for (BundleNode node : projectNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = Activator.getContext().getBundle(bundleId);
				if (null != bundle && (bundle.getState() & (state)) != 0) {
					bundles.add(bundle);
				}
			}
		}
		return bundles;
	}

	@Override
	public Collection<Bundle> getBundles(Collection<Bundle> bundles, int state) {

		Collection<Bundle> bundleStates = new LinkedHashSet<>();
		for (Bundle bundle : bundles) {
			if (null != bundle && (bundle.getState() & (state)) != 0) {
				bundleStates.add(bundle);
			}
		}
		return bundleStates;
	}

	@Override
	public Collection<Bundle> getBundles() {

		Collection<Bundle> bundles = new LinkedHashSet<Bundle>();
		for (BundleNode node : projectNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = Activator.getContext().getBundle(bundleId);
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
		Collections.addAll(allBundles, Activator.getContext().getBundles());
		allBundles.removeAll(workspaceBundles);
		return allBundles;
	}

	@Override
	public Collection<Bundle> getBundles(Collection<IProject> projects) {
		Collection<Bundle> bundles = new LinkedHashSet<>();
		for (IProject project : projects) {
			Bundle bundle = getBundle(project);
			if (null != bundle) {
				bundles.add(bundle);
			} else {
				if (Category.DEBUG && Category.getState(Category.dag))
					TraceMessage.getInstance().getString("bundle_not_installed", project.getName());
			}
		}
		return bundles;
	}

	@Override
	public Bundle getBundle(IProject project) {
		BundleNode node = getNode(project);
		if (null != node) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				return Activator.getContext().getBundle(bundleId);
			}
		}
		return null;
	}

	@Override
	public Bundle getBundle(Long bundleId) {
		BundleNode node = getNode(bundleId);
		if (null != node) {
			return node.getBundle(bundleId);
		}
		return null;
	}

	public void workspaceDuplicate(IProject project) throws WorkspaceDuplicateException {

		Map<IProject, IProject> duplicateMap = getWorkspaceDuplicates(
				Collections.<IProject> singletonList(project), null);
		if (duplicateMap.size() > 0) {
			String msg = null;
			for (Map.Entry<IProject, IProject> key : duplicateMap.entrySet()) {
				IProject duplicateProject = key.getKey();
				IProject otherDuplicateProject = key.getValue();
				if (duplicateProject.equals(project)) {
					msg = NLS.bind(
							Msg.WORKSPACE_UPATE_DUPLICATE_ERROR,
							new Object[] { BundleNode.formatSymbolicKey(null, project),
									duplicateProject.getName(), otherDuplicateProject.getName() });
					break;
				}
			}
			BundleNode bundleNode = getBundleNode(project);
			WorkspaceDuplicateException e = new WorkspaceDuplicateException(msg);
			bundleNode.setStatus(TransitionError.WORKSPACE_DUPLICATE, new BundleStatus(
					StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e));
			throw e;
		}
	}

	@Override
	public Map<IProject, IProject> getWorkspaceDuplicates(Collection<IProject> candidateProjects,
			Collection<IProject> candidates) {

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
		// Extend scope of search for additional duplicates of the duplicates already found
		if (candidateKeyMap.size() > 0) {
			Collection<IProject> candidatesCopy = null;
			if (null == candidates) {
				candidatesCopy = getProjects();
			} else {
				candidatesCopy = new LinkedHashSet<>(candidates);
			}
			candidatesCopy.removeAll(candidateKeyMap.values());
			for (IProject project : candidatesCopy) {
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

	@Override
	public Map<IProject, Bundle> getExternalDuplicates(Collection<IProject> projects) {

		return Activator.getDefault().getDuplicateEvents().getSymbolicNameDuplicates(projects);
	}

	@Override
	public Map<IProject, Bundle> getSymbolicNameDuplicates(Collection<IProject> projects,
			Collection<Bundle> candidateBundles) {

		Map<String, IProject> newSymbolicNameMap = new HashMap<String, IProject>();
		Map<IProject, Bundle> duplicateMap = new HashMap<IProject, Bundle>();

		for (IProject project : projects) {
			try {
				String symbolicName = BundleProjectMetaImpl.INSTANCE.getSymbolicName(project);
				if (null != symbolicName) {
					newSymbolicNameMap.put(symbolicName, project);
				}
			} catch (InPlaceException e) {
			}
		}
		Collection<Bundle> bundleProjects = getBundles(projects);
		for (Bundle bundle : candidateBundles) {
			if (!bundleProjects.contains(bundle)) {
				String symbolicName = bundle.getSymbolicName();
				if (newSymbolicNameMap.containsKey(symbolicName)) {
					duplicateMap.put(newSymbolicNameMap.get(symbolicName), bundle);
				}
			}
		}
		return duplicateMap;
	}

	@Override
	public boolean exist(Bundle bundle) {

		return (null != bundleProjects.get(bundle.getBundleId())) ? true : false;
	}

	@Override
	public IBundleStatus getBundleStatus(IProject project) {
		BundleNode node = getNode(project);
		if (null != node) {
			return node.getStatus();
		}
		return null;
	}

	@Override
	public void setBundleStatus(IProject project, IBundleStatus status) {
		BundleNode node = getNode(project);
		if (null != node) {
			node.setStatus(status);
		}
	}

	@Override
	public void setBundleStatus(IProject project, TransitionError transitionError,
			IBundleStatus status) {
		BundleNode node = getNode(project);
		if (null != node) {
			node.setStatus(transitionError, status);
		}
	}

	@Override
	public boolean setActivation(Bundle bundle, Boolean status) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			node.setActivated(status);
			return true;
		}
		return false;
	}

	@Override
	public boolean setActivation(IProject project, Boolean status) {
		BundleNode node = getNode(project);
		if (null != node) {
			node.setActivated(status);
			return true;
		}
		return false;
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
	void addPendingCommand(IProject project, Transition operation) {
		BundleNode bn = getNode(project);
		if (null != bn) {
			bn.addPendingCommand(operation);
		}
	}

	/**
	 * Add a pending bundle operation to the specified bundle projects
	 * 
	 * @param project bundle projects to add the pending operation to
	 * @param operation to register with the bundle projects
	 */
	void addPendingCommand(Collection<IProject> projects, Transition operation) {
		for (IProject project : projects) {
			BundleNode bn = getNode(project);
			if (null != bn) {
				bn.addPendingCommand(operation);
			}
		}
	}

	/**
	 * Add a pending bundle operation to the bundle.
	 * 
	 * @param bundle bundle project to add the pending operation to
	 * @param operation to register with this bundle project
	 */
	void addPendingCommand(Bundle bundle, Transition operation) {
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
	 * @return all projects among the specified projects containing the specified transition or an
	 * empty collection
	 */
	Collection<IProject> getPendingProjects(Collection<IProject> projects, Transition command) {
		Collection<IProject> pendingProjects = new LinkedHashSet<IProject>();
		for (IProject project : projects) {
			if (containsPendingCommand(project, command, false)) {
				pendingProjects.add(project);
			}
		}
		return pendingProjects;
	}

	/**
	 * Get all bundles among the specified bundles that contains the specified pending transition
	 * 
	 * @param bundles to check for having the specified transition
	 * @param command or transition to check for in the specified projects
	 * @return all bundles among the specified bundles containing the specified transition or an empty
	 * collection
	 */
	Collection<Bundle> getPendingBundles(Collection<Bundle> bundles, Transition command) {
		Collection<Bundle> pendingBundles = new LinkedHashSet<Bundle>();
		for (Bundle bundle : bundles) {
			if (containsPendingCommand(bundle, command, false)) {
				pendingBundles.add(bundle);
			}
		}
		return pendingBundles;
	}

	/**
	 * Get all pending operations for a project bundle
	 * 
	 * @param project bundle project to get pending operations for
	 * @return all pending operations for this bundle project
	 */
	EnumSet<Transition> getPendingCommands(IProject project) {
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
	boolean containsPendingCommand(IProject project, Transition command, boolean remove) {
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
	 * @return true if this command is associated with this bundle project. Otherwise false. If the
	 * specified bundle is null false is returned.
	 */
	boolean containsPendingCommand(Bundle bundle, Transition command, boolean remove) {
		BundleNode bn = getNode(bundle);
		if (null != bn) {
			return bn.containsPendingCommand(command, remove);
		}
		return false;
	}

	/**
	 * Check if there are any bundle projects with the specified pending command
	 * 
	 * @param command pending command
	 * @return true if the specified command is associated with any bundle project
	 */
	boolean containsPendingCommand(Transition command) {
		for (BundleNode node : projectNodes.values()) {
			Long bundleId = node.getBundleId();
			if (null != bundleId) {
				Bundle bundle = Activator.getContext().getBundle(bundleId);
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
	Boolean removePendingCommand(IProject project, Transition operation) {
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
	Boolean removePendingCommand(Bundle bundle, Transition operation) {
		BundleNode bn = getNode(bundle);
		if (null != bn) {
			return bn.removePendingCommand(operation);
		}
		return false;
	}

	public BundleNode getBundleNode(Bundle bundle) {
		return getNode(bundle);
	}

	public BundleNode getBundleNode(IProject project) {
		return getNode(project);
	}

	public BundleState getState(Bundle bundle) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			return node.getState();
		}
		return null;
	}

	public void setState(Bundle bundle, BundleState activeState) {
		BundleNode node = getNode(bundle);
		if (null != node) {
			node.setCurrentState(activeState);
		}
	}

	/**
	 * Register the bundle and its associated project. The project may or may not be activated
	 * <p>
	 * Also register bundle project in a separate hash map for direct access
	 * 
	 * @param project project to register or update. Must not be null
	 * @param bundle the bundle to register or update. May be null
	 * @param activate true if bundle is activated and false if not
	 * @return the new or updated bundle node
	 * @throws InPlaceException if the specified project parameter is null
	 */
	protected BundleNode put(IProject project, Bundle bundle, Boolean activate)
			throws InPlaceException {

		if (null == project) {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("npe_project_cache");
			throw new InPlaceException(ExceptionMessage.getInstance().getString("project_null_location"));
		}
		BundleNode node = getNode(project);
		// Update node
		if (null != node) {
			node.setProject(project);
			// Replace the bundle id
			if (null != bundle) {
				Long oldBundleid = node.getBundleId();
				if (null != oldBundleid) {
					bundleProjects.remove(oldBundleid);
				}
				node.setBundle(bundle);
			}
			node.setActivated(activate);
			projectNodes.put(project, node);
			if (Category.DEBUG && Category.getState(Category.dag)) {
				TraceMessage.getInstance()
						.getString("updated_node", projectNodes.get(project).getProject());
			}
			// Create node
		} else {
			node = new BundleNode(bundle, project, activate);
			projectNodes.put(project, node);
			if (Category.DEBUG && Category.getState(Category.dag)) {
				TraceMessage.getInstance().getString("inserted_node",
						projectNodes.get(project).getProject());
			}
		}
		if (null != bundle) {
			bundleProjects.put(bundle.getBundleId(), project);
		}
		return node;
	}

	/**
	 * Use with care.
	 * 
	 * @param bundleId Key for removal of the mapping from bundleId to project
	 * @return The associated project or null
	 */
	@SuppressWarnings("unused")
	private IProject remove(Long bundleId) {

		IProject project = null;
		if (null != bundleId) {
			project = bundleProjects.remove(bundleId);
			if (null != project) {
				BundleNode node = getBundleNode(project);
				if (null != node) {
					node.setActivated(false);
				}
			}
		}
		return project;
	}

	/**
	 * Remove the project and its associated bundle from the bundle project region.
	 * <p>
	 * Also remove the bundle project, if there is a bundle, from a separate hash map for direct
	 * access
	 * 
	 * @param project project to remove. Must not be null
	 * @throws InPlaceException if the specified project parameter is null
	 */
	protected Long remove(IProject project) {

		BundleNode deletedNode = projectNodes.remove(project);
		if (null == deletedNode) {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("null_remove_node", project.getName());
			return null;
		} else {
			if (Category.DEBUG && Category.getState(Category.dag))
				TraceMessage.getInstance().getString("removed_node", project.getName());
			Long bundleId = deletedNode.getBundleId();
			IProject delProject = null;
			if (null != bundleId) {
				delProject = bundleProjects.remove(bundleId);
			}
			if (Category.DEBUG && Category.getState(Category.dag)) {
				if (null == delProject) {
					TraceMessage.getInstance().getString("null_remove_node", project.getName());
				}
			}
			return bundleId;
		}
	}

	/**
	 * Get a bundle node from its associated project
	 * 
	 * @param project associated with the workspace bundle
	 * @return the bundle node or null
	 */
	private BundleNode getNode(IProject project) {

		return null != project ? projectNodes.get(project) : null;
	}

	/**
	 * Finds a bundle node based on the bundle object
	 * 
	 * @param bundle used to identify its bundle node
	 * @return a bundle node or null
	 */
	private BundleNode getNode(Bundle bundle) {
		if (null != bundle) {
			// Bundle id never reused and must return the bundle id in state uninstalled
			IProject project = bundleProjects.get(bundle.getBundleId());
			return null != project ? projectNodes.get(project) : null;
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
			// Bundle id never reused and must return the bundle id in state uninstalled
			IProject project = bundleProjects.get(bundleId);
			return null != project ? projectNodes.get(project) : null;
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
			for (BundleNode node : projectNodes.values()) {
				String symbolicKey = node.getSymbolicKey();
				if (null != symbolicKey && symbolicKey.equals(symbolicName + version)) {
					return node;
				}
			}
		}
		return null;
	}
}
