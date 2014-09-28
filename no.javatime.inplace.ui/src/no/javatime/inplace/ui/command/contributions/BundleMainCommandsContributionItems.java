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
package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;
import java.util.Collection;

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dialogs.OpenProjectHandler;
import no.javatime.inplace.extender.provider.ExtenderException;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.WarnMessage;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Build the main menu
 */
public class BundleMainCommandsContributionItems extends BundleCommandsContributionItems {

	private String menuId = "no.javatime.inplace.command.contributions.dynamicitems.main";
	// Main menu dynamic command definitions id
	public static String dynamicMainCommandId = "no.javatime.inplace.command.dynamicitems.main";

	public BundleMainCommandsContributionItems() {
		super();
	}

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();
		Collection<IProject> candidateProjects = ProjectProperties.getCandidateProjects();
		Collection<IProject> activatedProjects = BundleProjectState.getNatureEnabledProjects();

		try {
			// Busy running bundle jobs.
			// Do not add contributions for bundles that are dependent on their current state
			if (OpenProjectHandler.getBundlesJobRunState()) {
				contribute(addStopTaskOperation(menuId, dynamicMainCommandId), contributions);
				contribute(addInterrupt(menuId, dynamicMainCommandId), contributions);
			} else {
				contribute(addActivate(candidateProjects, activatedProjects), contributions);
				contribute(addDeactivate(activatedProjects), contributions);
				contribute(addStart(activatedProjects), contributions);
				contribute(addStop(activatedProjects), contributions);
				contribute(addUpdate(activatedProjects), contributions);
				contribute(addRefreshPending(activatedProjects), contributions);
				contribute(addRefresh(activatedProjects), contributions);
				contribute(addReset(activatedProjects), contributions);
			}
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contribute(addToggleBundleView(), contributions);
			contribute(addToggleBundleConsoleView(menuId, dynamicMainCommandId), contributions);
			contribute(addToggleBundleLogView(menuId, dynamicMainCommandId), contributions);
		} catch (InPlaceException | ExtenderException e) {
			StatusManager.getManager()
					.handle(
							new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
									Msg.ADD_CONTRIBUTION_ERROR, e), StatusManager.LOG);
		}
		IContributionItem[] contributionArray = contributions
				.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}

	/**
	 * Adds a contribution to activate the specified candidate bundle projects.
	 * <p>
	 * Already activated candidate projects are ignored.
	 * 
	 * @param candidateProjects projects to activate. Must not be null
	 * @param activatedProjects already activated projects. Must not be null
	 * @return a contribution to activate the specified candidate projects or null if no candidate
	 * projects specified
	 */
	private CommandContributionItem addActivate(Collection<IProject> candidateProjects,
			Collection<IProject> activatedProjects) {
		if (candidateProjects.size() > 0) {
			String activateLabel = null;
			if (activatedProjects.isEmpty()) {
				activateLabel = formatLabel(ActivateProjectJob.activateWorkspaceJobName,
						candidateProjects.size(), Boolean.FALSE);
			} else {
				activateLabel = formatLabel(Msg.ACTIVATE_JOB, candidateProjects.size(), Boolean.TRUE);
			}
			return createContibution(menuId, dynamicMainCommandId, activateLabel, activateProjectParamId,
					CommandContributionItem.STYLE_PUSH, activateImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to deactivate the specified activated bundle projects.
	 * <p>
	 * Already deactivated bundle projects are ignored.
	 * 
	 * @param activatedProjects Activated projects to deactivate. Must not be null
	 * @return a contribution to deactivate the specified activated projects or null if no activated
	 * projects specified
	 */
	private CommandContributionItem addDeactivate(Collection<IProject> activatedProjects) {
		if (activatedProjects.size() > 0) {
			String deactivateLabel = formatLabel("Deactivate Workspace", activatedProjects.size(),
					Boolean.FALSE);
			return createContibution(menuId, dynamicMainCommandId, deactivateLabel, deactivateParamId,
					CommandContributionItem.STYLE_PUSH, deactivateImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to start the specified activated bundle projects.
	 * <p>
	 * Already started bundle projects are ignored.
	 * 
	 * @param activatedProjects Activated projects to start. Must not be null
	 * @return a contribution to start the specified activated projects or null if no activated
	 * projects specified
	 */
	private CommandContributionItem addStart(Collection<IProject> activatedProjects) {

		BundleRegion bundleRegion = BundleManager.getRegion();
		BundleCommand bundleCommand = BundleManager.getCommand();

		// Calculate number of projects to start
		if (activatedProjects.size() > 0) {
			int nStart = 0;
			for (IProject project : activatedProjects) {
				Bundle bundle = bundleRegion.get(project);
				// Uninstalled
				if (null == bundle) {
					// TODO Can not start an uninstalled bundle?
					// nStart++;
					continue;
				}
				int state = bundleCommand.getState(bundle);
				if (!ManifestOptions.isFragment(bundle)
						&& (state & (Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
					nStart++;
					continue;
				}
			}
			if (nStart > 0) {
				String startLabel = formatLabel("Start", nStart, Boolean.TRUE);
				return createContibution(menuId, dynamicMainCommandId, startLabel, startParamId,
						CommandContributionItem.STYLE_PUSH, startImage);
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to stop the specified activated bundle projects.
	 * <p>
	 * Already stopped bundle projects are ignored.
	 * 
	 * @param activatedProjects Activated projects to stop. Must not be null
	 * @return a contribution to stop the specified activated projects or null if no activated
	 * projects specified
	 */
	private CommandContributionItem addStop(Collection<IProject> activatedProjects) {

		BundleRegion bundleRegion = BundleManager.getRegion();
		BundleCommand bundleCommand = BundleManager.getCommand();

		// Calculate number of projects to stop
		if (activatedProjects.size() > 0) {
			int nStop = 0;
			for (IProject project : activatedProjects) {
				Bundle bundle = bundleRegion.get(project);
				// Uninstalled
				if (null == bundle) {
					continue;
				}
				int state = bundleCommand.getState(bundle);
				if ((state & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
					nStop++;
					continue;
				}
			}
			if (nStop > 0) {
				String stopLabel = formatLabel("Stop", nStop, Boolean.TRUE);
				return createContibution(menuId, dynamicMainCommandId, stopLabel, stopParamId,
						CommandContributionItem.STYLE_PUSH, stopImage);
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to update the specified activated bundle projects with update as a pending
	 * transition
	 * 
	 * @param activatedProjects Activated projects to update. Must not be null
	 * @return a contribution to update the specified activated projects with update as a pending
	 * transition or null if no activated projects specified or no projects have update pending
	 */
	private CommandContributionItem addUpdate(Collection<IProject> activatedProjects) {

		BundleRegion bundleRegion = BundleManager.getRegion();

		// Calculate number of projects to update
		if (activatedProjects.size() > 0) {
			int nUpdate = 0;
			for (IProject project : activatedProjects) {
				// Uninstalled
				if (null == bundleRegion.get(project)) {
					continue;
				}
				if (BundleManager.getTransition().containsPending(project, Transition.UPDATE,
						Boolean.FALSE)) {
					nUpdate++;
					continue;
				}
			}
			if (nUpdate > 0) {
				String updateLabel = formatLabel("Update", nUpdate, Boolean.TRUE);
				return createContibution(menuId, dynamicMainCommandId, updateLabel, updateParamId,
						CommandContributionItem.STYLE_PUSH, updateImage);
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to refresh the specified activated bundle projects with refresh as pending
	 * <p>
	 * Refresh is pending if a bundle has more than one revision
	 * 
	 * @param activatedProjects Activated projects to refresh. Must not be null
	 * @return a contribution to refresh the specified activated projects having a refresh pending
	 * status or null if no activated projects with refresh pending exists among the projects to
	 * refresh
	 * @throws InPlaceException  if the bundle adapt permission is missing
	 */
	private CommandContributionItem addRefreshPending(Collection<IProject> activatedProjects) throws InPlaceException{

		BundleRegion bundleRegion = BundleManager.getRegion();
		BundleCommand bundleCommand = BundleManager.getCommand();
		
		// Calculate number of projects to refresh
		if (activatedProjects.size() > 0) {
			int nRefresh = 0;
			for (IProject project : activatedProjects) {
				Bundle bundle = bundleRegion.get(project);
				// Uninstalled
				if (null == bundle) {
					continue;
				}
				if (bundleCommand.getBundleRevisions(bundle).size() > 1) {
					nRefresh++;
					continue;
				}
			}
			if (nRefresh > 0) {
				if (nRefresh != bundleCommand.getRemovalPending().size()) {
					String msg = WarnMessage.getInstance().formatString("illegal_number_of_revisions",
							bundleCommand.getRemovalPending().size(), nRefresh);
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg), StatusManager.LOG);
				}
				String refreshLabel = formatLabel("Refresh", nRefresh, Boolean.TRUE);
				return createContibution(menuId, dynamicMainCommandId, refreshLabel, refreshPendingParamId,
						CommandContributionItem.STYLE_PUSH, refreshImage);
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to refresh all activated bundle projects.
	 * 
	 * @param activatedProjects Activated projects to refresh. Must not be null
	 * @return a contribution to refresh the specified activated projects or null if no activated
	 * projects specified
	 */
	private CommandContributionItem addRefresh(Collection<IProject> activatedProjects) {
		if (activatedProjects.size() > 0) {
			String refreshLabel = "Refresh Workspace";
			return createContibution(menuId, dynamicMainCommandId, refreshLabel, refreshParamId,
					CommandContributionItem.STYLE_PUSH, refreshImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to reset all activated bundle projects.
	 * 
	 * @param activatedProjects Activated projects to reset. Must not be null
	 * 
	 * @return a contribution to reset the specified activated projects or null if no activated
	 * projects specified
	 */
	private CommandContributionItem addReset(Collection<IProject> activatedProjects) {
		if (activatedProjects.size() > 0) {
			String resetLabel = "Reset Workspace";
			return createContibution(menuId, dynamicMainCommandId, resetLabel, resetParamId,
					CommandContributionItem.STYLE_PUSH, resetImage);
		}
		return null;
	}

	/**
	 * When bundle view is hidden return a contribution item offering to open it and to close it if
	 * open and there is no selection (active project).
	 * <p>
	 * If the view is open and the list page is active with a selected project return a contribution
	 * item offering to show the details page and a contribution to show the list page if the details
	 * page is active.
	 * 
	 * @return contribution item offering to show or hide the bundle view or show the list or the
	 * details page in an open view with a selected project.
	 */
	private CommandContributionItem addToggleBundleView() {
		if (!ViewUtil.isVisible(BundleView.ID)) {
			return createContibution(menuId, dynamicMainCommandId, showBundleView, bundleViewParamId,
					CommandContributionItem.STYLE_PUSH, bundleListImage);
		} else {
			BundleView bundleView = getBundleView();
			if (null != bundleView && (null != getSelectedJavaProjectFromBundleView())) {
				// Bundle view is open and visible and there is a selection
				if (bundleView.isListPageActive()) {
					return createContibution(menuId, dynamicMainCommandId, showBundleDetailsPage,
							bundleViewParamId, CommandContributionItem.STYLE_PUSH, bundleDetailsImage);
				} else {
					return createContibution(menuId, dynamicMainCommandId, showBundleListPage,
							bundleViewParamId, CommandContributionItem.STYLE_PUSH, bundleListImage);
				}
			} else {
				// Bundle view is open but there is no selection
				return createContibution(menuId, dynamicMainCommandId, hideBundleView, bundleViewParamId,
						CommandContributionItem.STYLE_PUSH, bundleListImage);
			}
		}
	}
}
