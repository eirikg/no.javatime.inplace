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
package no.javatime.inplace.region.closure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.ExternalDuplicateException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.manager.WorkspaceRegionImpl;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.project.BundleProjectMetaImpl;
import no.javatime.inplace.region.state.BundleNode;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Query for workspace region bundles being duplicates of external jar bundles.
 * <p>
 * Maintains an internal map of all installed external bundles
 * <p>
 * // TODO It is only the "symbolicNameDuplicate" method that is used of the public methods. The
 * others must be tuned and throw an external duplicate exception
 */
public class ExternalDuplicates implements BundleListener {

	private Map<String, List<Bundle>> map = new ConcurrentHashMap<String, List<Bundle>>();

	/**
	 * Default empty constructor.
	 */
	public ExternalDuplicates() {
	}

	/**
	 * Add and remove external bundles being installed and uninstalled
	 */
	@Override
	public void bundleChanged(BundleEvent event) {

		final Bundle bundle = event.getBundle();

		switch (event.getType()) {
		case BundleEvent.INSTALLED: {
			if (null == WorkspaceRegionImpl.INSTANCE.getWorkspaceBundleProject(bundle)) {
				add(bundle);
			}
			break;
		}
		case BundleEvent.UNINSTALLED: {
			if (null == WorkspaceRegionImpl.INSTANCE.getWorkspaceBundleProject(bundle)) {
				remove(bundle);
				break;
			}
			break;
		}
		default: {
		}
		}
	}

	/**
	 * Check if the specified symbolic name is a duplicate of an external bundle
	 * 
	 * @param key Symbolic name
	 * @return true if it is a duplicate and false if it is not a duplicate
	 */
	public boolean isSymbolicNameDuplicate(String key) {

		return map.containsKey(key);
	}

	/**
	 * Check if the specified project has a symbolic name and version that is a duplicate of an
	 * external bundle
	 * 
	 * @param project The project with a symbolic name and version to check against external bundles
	 * @return A map of projects and workspace bundles that match the symbolic name and version of
	 * external bundles or an empty map
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @see #getBySymbolicKey(IProject)
	 */
	public Map<IProject, Bundle> getSymbolicKeyDuplicates(Collection<IProject> projects) {

		Map<IProject, Bundle> duplicateMap = new HashMap<IProject, Bundle>();

		for (IProject project : projects) {
			try {
				Bundle bundle = getBySymbolicKey(project);
				if (null != bundle) {
					duplicateMap.put(project, bundle);
				}
			} catch (InPlaceException e) {
			}
		}
		return duplicateMap;
	}

	/**
	 * Check if the specified project has a symbolic name that is a duplicate of an external bundle
	 * 
	 * @param project The project with a symbolic name to check against external bundles
	 * @return A map of projects and workspace bundles that match the symbolic name of external
	 * bundles or an empty map
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @see #symbolicNameDuplicate(IProject)
	 */
	public Map<IProject, Bundle> getSymbolicNameDuplicates(Collection<IProject> projects)
			throws InPlaceException {

		Map<IProject, Bundle> duplicateMap = new HashMap<IProject, Bundle>();

		for (IProject project : projects) {
			try {
				symbolicNameDuplicate(project);
			} catch (ExternalDuplicateException e) {
				BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE;
				Bundle bundle = bundleRegion.getBundle(project);
				if (null != bundle) {
					duplicateMap.put(project, bundle);
				}
			} catch (InPlaceException e) {
			}
		}
		return duplicateMap;
	}

	/**
	 * Check if the specified project has a symbolic name and version that is a duplicate of an
	 * external bundle
	 * 
	 * @param project The project with a symbolic name and version to check against external bundles
	 * @return The workspace bundle associated wit the specified project if it matches the symbolic
	 * name and version of an external bundle. If no match id found return {@code null}
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @see #symbolicNameDuplicate(IProject)
	 */
	public Bundle getBySymbolicKey(IProject project) {

		String key = BundleProjectMetaImpl.INSTANCE.getSymbolicName(project);
		if (null != key) {
			List<Bundle> bucketList = map.get(key);
			if (null != bucketList && bucketList.size() > 0) {
				String symbolicProjectKey = BundleNode.formatSymbolicKey(null, project);
				if (symbolicProjectKey.length() > 0) {
					for (Bundle bundle : bucketList) {
						String symbolicBundleKey = BundleNode.formatSymbolicKey(bundle, null);
						if (symbolicBundleKey.length() > 0 && symbolicBundleKey.equals(symbolicProjectKey)) {
							return bundle;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Check if the specified project has a symbolic name that is a duplicate of the symbolic name of
	 * an external bundle
	 * <p>
	 * Use {@link #getBySymbolicKey(IProject)} to find an exact match
	 * 
	 * @param project The project with a symbolic name to check against external bundles
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @throws ExternalDuplicateException If the symbolic name of the specified project is a duplicate
	 * of the symbolic name of an external bundle
	 * @see #getBySymbolicKey(IProject)
	 */
	public void symbolicNameDuplicate(IProject project) throws InPlaceException,
			ExternalDuplicateException {

		// if (!Activator.getDefault().isRefreshDuplicateBSNAllowed()) {
		// return null;
		// }
		String key = BundleProjectMetaImpl.INSTANCE.getSymbolicName(project);
		if (null != key) {
			List<Bundle> bucketList = map.get(key);
			if (null != bucketList && bucketList.size() > 0) {
				WorkspaceRegionImpl bundleRegion = WorkspaceRegionImpl.INSTANCE;
				BundleNode bundleNode = bundleRegion.getBundleNode(project);
				Bundle bundle = null;
				String msg = null;
				if (null != bundleNode) {
					bundle = bundleNode.getBundle();
				}
				if (null != bundle) {
					bundle = bundleNode.getBundle();
					msg = NLS.bind(Msg.EXTERNAL_UPDATE_DUPLICATE_EXP, bundle.getSymbolicName(), key);
				} else {
					msg = NLS.bind(Msg.EXTERNAL_INSTALL_DUPLICATE_EXP, key,
							bundleRegion.getBundleLocationIdentifier(project));
				}
				ExternalDuplicateException e = new ExternalDuplicateException(msg);
				if (null != bundleNode) {
					bundleNode.setBundleStatus(TransitionError.BUILD_MODULAR_EXTERNAL_DUPLICATE, new BundleStatus(
							StatusCode.EXCEPTION, Activator.PLUGIN_ID, project, msg, e));
				}
				throw e;
			}
		}
	}

	/**
	 * Check if the specified bundle has a symbolic name that is a duplicate of the symbolic name of
	 * an external bundle
	 * <p>
	 * Use {@link #getBySymbolicKey(IProject)} to find an exact match
	 * 
	 * @param bundle The bundle with a symbolic name to check against external bundles
	 * @return The first external bundle found associated with the specified workspace bundle if it
	 * matches the symbolic name of an external bundle. Return {@code null} if no duplicates are found
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @throws ExternalDuplicateException If the symbolic name of the specified bundle is a duplicate
	 * of the symbolic name of an external bundle
	 * @see #getBySymbolicKey(IProject)
	 */
	public void symbolicNameDuplicate(Bundle bundle) throws InPlaceException,
			ExternalDuplicateException {

		BundleRegion bundleRegion = WorkspaceRegionImpl.INSTANCE;
		IProject project = bundleRegion.getProject(bundle);
		symbolicNameDuplicate(project);
	}

	/**
	 * Check if the specified project has a symbolic name that is a duplicate of the symbolic name of
	 * an external bundle
	 * <p>
	 * Use {@link #getBySymbolicKey(IProject)} to find an exact match
	 * 
	 * @param project The project with a symbolic name to check against external bundles
	 * @return {@code true} if the workspace bundle associated with the specified project matches the
	 * symbolic name of an external bundle Return {@code false} if no duplicates are found
	 * @throws InPlaceException if the manifest has an invalid syntax or if an error occurs while
	 * reading the manifest
	 * @see #symbolicNameDuplicate(IProject)
	 */
	public boolean hasSymbolicName(IProject project) throws InPlaceException {

		String key = BundleProjectMetaImpl.INSTANCE.getSymbolicName(project);

		if (null != key) {
			List<Bundle> bucketList = map.get(key);
			if (null != bucketList && bucketList.size() > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a bundle to the map of external bundles
	 * 
	 * @param bundle The bundle to remove from the map
	 */
	private void remove(Bundle bundle) {
		String key = bundle.getSymbolicName();
		if (null != key) {
			List<Bundle> bucketList = map.get(key);
			if (null != bucketList) {
				bucketList.remove(bundle);
				if (bucketList.size() == 0) {
					map.remove(key);
				}
			}
		}
	}

	/**
	 * Add a bundle to the map of external bundles
	 * 
	 * @param bundle The bundle to add to the map
	 */
	private void add(Bundle bundle) {
		String key = bundle.getSymbolicName();
		if (null != key) {
			List<Bundle> bucketList = map.get(key);
			if (null != bucketList) {
				bucketList.add(bundle);
			} else {
				bucketList = new ArrayList<>();
				bucketList.add(bundle);
				map.put(key, bucketList);
			}
		}
	}

	/**
	 * Adds all installed external bundles to the map of external bundles if the map is empty
	 */
	public void initExternalBundles() {

		if (map.isEmpty()) {
			Collection<Bundle> bundles = WorkspaceRegionImpl.INSTANCE.getJarBundles();
			for (Bundle bundle : bundles) {
				add(bundle);
			}
		}
	}
	/**
	 * Removes pending workspace projects and their requiring projects from the specified projects and
	 * dependency closures of duplicate projects to workspace bundles.
	 * 
	 * @param projects duplicate candidates to workspace bundles
	 * @param bDepClosures existing dependency closure of bundles to the specified candidate projects.
	 * May be null.
	 * @param pDepClosures Dependent closure to duplicates
	 * @param scope Domain for duplicates
	 * @param message information message added to the end of the error sent to the log view if
	 * duplicates are detected. Null is allowed.
	 * @return all duplicates and the requiring dependency closure for each duplicate or null if no
	 * duplicates found.
	 */
	// protected Collection<IProject> removeWorkspaceDuplicates(Collection<IProject> projects,
	// Collection<Bundle> bDepClosures, Collection<IProject> pDepClosures,
	// Collection<IProject> scope, String message) {
	//
	// WorkspaceRegionImpl bundleRegion = WorkspaceRegionImpl.INSTANCE;
	// Map<IProject, IProject> wsDuplicates = bundleRegion.getWorkspaceDuplicates(projects, scope);
	// Collection<IProject> duplicateClosures = null;
	// if (wsDuplicates.size() > 0) {
	// duplicateClosures = new ArrayList<IProject>();
	// ProjectSorter ps = new ProjectSorter();
	// ps.setAllowCycles(true);
	// for (Map.Entry<IProject, IProject> key : wsDuplicates.entrySet()) {
	// IProject duplicateProject = key.getKey();
	// IProject duplicateProject1 = key.getValue();
	// try {
	// // If checked on an uninstalled bundle that is not registered yet
	// if (!bundleRegion.isProjectRegistered(duplicateProject)) {
	// bundleRegion.registerBundleProject(duplicateProject, null, false);
	// }
	// bundleTransition.setTransitionError(duplicateProject, TransitionError.DUPLICATE);
	// DuplicateBundleException duplicateBundleException = new DuplicateBundleException(
	// "duplicate_of_ws_bundle", duplicateProject.getName(),
	// bundleProjectMeta.getSymbolicName(duplicateProject1), duplicateProject1.getLocation());
	// handleDuplicateException(duplicateProject, duplicateBundleException, message);
	// Collection<IProject> requiringProjects = ps.sortRequiringProjects(Collections
	// .<IProject> singletonList(duplicateProject));
	// if (requiringProjects.size() > 0) {
	// for (IProject reqProject : requiringProjects) {
	// bundleTransition.removePending(reqProject, Transition.UPDATE);
	// bundleTransition.removePending(reqProject, Transition.UPDATE_ON_ACTIVATE);
	// }
	// projects.removeAll(requiringProjects);
	// duplicateClosures.addAll(requiringProjects);
	// if (null != bDepClosures) {
	// bDepClosures.removeAll(bundleRegion.getBundles(requiringProjects));
	// }
	// if (null != pDepClosures) {
	// pDepClosures.removeAll(requiringProjects);
	// }
	// } else {
	// projects.remove(duplicateProject);
	// duplicateClosures.add(duplicateProject);
	// }
	// } catch (ProjectLocationException e) {
	// addError(e, e.getLocalizedMessage(), duplicateProject);
	// }
	// }
	// }
	// return duplicateClosures;
	// }

	/**
	 * Removes pending workspace projects and their requiring projects from the specified projects and
	 * dependency closures of duplicate projects to external bundles. -- Detect bundles which are
	 * duplicates of external bundles -- Can not let update detect the duplicate -- OSGi will refresh
	 * all dependent bundles of the jar bundle -- and suspend the refreshPackages and not return --
	 * See private void suspendBundle(AbstractBundle bundle) { -- attempt to suspend the bundle or
	 * obtain the state change lock -- Note that this may fail but we cannot quit the --
	 * refreshPackages operation because of it. (bug 84169)
	 * 
	 * @param projects duplicate candidates to external bundles
	 * @param message information message added to the end of the error sent to the log view if
	 * duplicates are detected. Null is allowed.
	 * @return all duplicates and the requiring dependency closure for each duplicate or null if no
	 * duplicates found.
	 */
	// protected Collection<IProject> getExternalDuplicateClosures(Collection<IProject> projects,
	// String message) {
	//
	// if (!Activator.getInstance().isRefreshDuplicateBSNAllowed()) {
	// return null;
	// }
	// Map<IProject, Bundle> externalDuplicates = bundleRegion.getSymbolicNameDuplicates(projects,
	// bundleRegion.getJarBundles());
	// Collection<IProject> duplicateClosures = null;
	// if (externalDuplicates.size() > 0) {
	// duplicateClosures = new ArrayList<IProject>();
	// ProjectSorter ps = new ProjectSorter();
	// ps.setAllowCycles(true);
	// for (Map.Entry<IProject, Bundle> key : externalDuplicates.entrySet()) {
	// IBundleStatus startStatus = null;
	// IProject duplicate = key.getKey();
	// try {
	// // If checked on an uninstalled bundle that is not registered yet
	// if (!bundleRegion.isProjectRegistered(duplicate)) {
	// bundleRegion.registerBundleProject(duplicate, null, false);
	// }
	// bundleTransition.setTransitionError(duplicate, TransitionError.DUPLICATE);
	// String msg = ErrorMessage.getInstance().formatString("duplicate_of_jar_bundle",
	// duplicate.getName(), key.getValue().getSymbolicName(), key.getValue().getLocation());
	// startStatus = addError(null, msg);
	// Collection<IProject> requiringProjects = ps.sortRequiringProjects(Collections
	// .<IProject> singletonList(duplicate));
	// if (requiringProjects.size() > 0) {
	// String affectedBundlesMsg = ErrorMessage.getInstance().formatString(
	// "duplicate_affected_bundles", duplicate.getName(),
	// bundleProjectCandidates.formatProjectList(requiringProjects));
	// addInfoMessage(affectedBundlesMsg);
	// for (IProject reqProject : requiringProjects) {
	// bundleTransition.removePending(reqProject, Transition.UPDATE);
	// bundleTransition.removePending(reqProject, Transition.UPDATE_ON_ACTIVATE);
	// }
	// duplicateClosures.addAll(requiringProjects);
	// } else {
	// duplicateClosures.add(duplicate);
	// }
	// if (null != message) {
	// addInfoMessage(message);
	// }
	// String rootMsg = ErrorMessage.getInstance().formatString(
	// "detected_duplicate_of_jar_bundle");
	// createMultiStatus(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, rootMsg),
	// startStatus);
	// } catch (ProjectLocationException e) {
	// addError(e, e.getLocalizedMessage(), duplicate);
	// }
	// }
	// }
	// return duplicateClosures;
	// }
}