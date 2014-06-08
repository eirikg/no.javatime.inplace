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

import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dialogs.OpenProjectHandler;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.pl.dependencies.service.DependencyDialog;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleRegion;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.project.ManifestUtil;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;
import no.javatime.util.messages.views.BundleConsole;
import no.javatime.util.messages.views.BundleConsoleFactory;

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
	
	public BundleMainCommandsContributionItems() {
		super();
	}

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();

		Collection<IProject> candidateProjects = ProjectProperties.getCandidateProjects();
		Collection<IProject> activatedProjects = ProjectProperties.getActivatedProjects();
		CommandContributionItem contribution;

		// Busy running bundle jobs. Show a limited set of contributors
		if (OpenProjectHandler.getBundlesJobRunState()) {
			contribution = addStopOperation(menuId, dynamicMainCommandId);
			if (null != contribution) {
				contributions.add(contribution);				
			}
			contributions.add(addInterrupt(menuId, dynamicMainCommandId));
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			if (activatedProjects.size() > 0) {				
				contributions.add(addRefresh());
				contributions.add(addReset());
			}
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contributions.add(addToggleConsoleView());
			contributions.add(addToggleMessageView());
			IContributionItem[] contributionArray = contributions
					.toArray(new ContributionItem[contributions.size()]);
			return contributionArray;
		}

		// No projects to activate
		// contribution = addEmpytWorkspace(candidateProjects, activatedProjects);
		// if (null != contribution) {
		// contributions.add(contribution);
		// }
		contribution = addActivate(candidateProjects, activatedProjects);
		if (null != contribution) {
			contributions.add(contribution);
		}
		if (activatedProjects.size() > 0) {
			contribution = addDeactivate(activatedProjects);
			if (null != contribution) {
				contributions.add(contribution);
			}
			contribution = addStart(activatedProjects);
			if (null != contribution) {
				contributions.add(contribution);
			}
			contribution = addStop(activatedProjects);
			if (null != contribution) {
				contributions.add(contribution);
			}
			contribution = addUpdate(activatedProjects);
			if (null != contribution) {
				contributions.add(contribution);
			}
			contribution = addRefreshPending(activatedProjects);
			if (null != contribution) {
				contributions.add(contribution);
			}
			contributions.add(addRefresh());
			contributions.add(addReset());
		}
		contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		contribution = addShowBundleView();
		if (null != contribution) {
			contributions.add(contribution);
		}
		contributions.add(addToggleConsoleView());
		contributions.add(addToggleMessageView());

		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}

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
			return addContribution(menuId, dynamicMainCommandId, activateLabel, activateParamId,
					CommandContributionItem.STYLE_PUSH, activateImage);
		}
		return null;
	}

	private CommandContributionItem addDeactivate(Collection<IProject> activatedProjects) {
		if (activatedProjects.size() > 0) {
			String deactivateLabel = formatLabel("Deactivate Workspace", activatedProjects.size(), Boolean.FALSE);
			return addContribution(menuId, dynamicMainCommandId, deactivateLabel, deactivateParamId,
					CommandContributionItem.STYLE_PUSH, deactivateImage);
		}
		return null;
	}

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
				if (!ManifestUtil.isFragment(bundle)
						&& (state & (Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
					nStart++;
					continue;
				}
			}
			if (nStart > 0) {
				String startLabel = formatLabel("Start", nStart, Boolean.TRUE);
				return addContribution(menuId, dynamicMainCommandId, startLabel, startParamId,
						CommandContributionItem.STYLE_PUSH, startImage);
			}
		}
		return null;
	}

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
				return addContribution(menuId, dynamicMainCommandId, stopLabel, stopParamId,
						CommandContributionItem.STYLE_PUSH, stopImage);
			}
		}
		return null;
	}

	private CommandContributionItem addRefresh() {
		String refreshLabel = "Refresh Workspace";
		return addContribution(menuId, dynamicMainCommandId, refreshLabel, refreshParamId,
				CommandContributionItem.STYLE_PUSH, refreshImage);
	}

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
				if (BundleManager.getTransition().containsPending(project, Transition.UPDATE, Boolean.FALSE)) {
					nUpdate++;
					continue;
				}
			}
			if (nUpdate > 0) {
				String updateLabel = formatLabel("Update", nUpdate, Boolean.TRUE);
				return addContribution(menuId, dynamicMainCommandId, updateLabel, updateParamId,
						CommandContributionItem.STYLE_PUSH, updateImage);
			}
		}
		return null;
	}

	private CommandContributionItem addRefreshPending(Collection<IProject> activatedProjects) {
		
		BundleRegion bundleRegion = BundleManager.getRegion();

		// Calculate number of projects to update
		if (activatedProjects.size() > 0) {
			int nRefresh = 0;
			for (IProject project : activatedProjects) {
				Bundle bundle = bundleRegion.get(project);
				// Uninstalled
				if (null == bundle) {
					continue;
				}
				if (BundleManager.getCommand().getBundleRevisions(bundle).size() > 1) {
					nRefresh++;
					continue;
				}
			}
			if (nRefresh > 0) {
				BundleCommand bundleCommand = BundleManager.getCommand();
				if (nRefresh != bundleCommand.getRemovalPending().size()) {
					String msg = WarnMessage.getInstance().formatString("illegal_number_of_revisions",
							bundleCommand.getRemovalPending().size(), nRefresh);
					StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, msg),
							StatusManager.LOG);
				}
				String refreshLabel = formatLabel("Refresh", nRefresh, Boolean.TRUE);
				return addContribution(menuId, dynamicMainCommandId, refreshLabel, refreshPendingParamId,
						CommandContributionItem.STYLE_PUSH, refreshImage);
			}
		}
		return null;
	}

	private CommandContributionItem addReset() {
		String resetLabel = "Reset Workspace";
		return addContribution(menuId, dynamicMainCommandId, resetLabel, resetParamId,
				CommandContributionItem.STYLE_PUSH, resetImage);
	}

	/**
	 * When bundle view is hidden return a contribution item offering to open it and to close it if 
	 * open and there is no selection (active project). 
	 * <p>
	 * If the view is open and the list page is active with a selected project return a
	 * contribution item offering to show the details page and a contribution to show the list page if 
	 * the details page is active.
	 * 
	 * @return contribution item offering to show or hide the bundle view or show the list or the details page
	 *         in an open view with a selected project.
	 */
	private CommandContributionItem addShowBundleView() {
		if (!Message.isViewVisible(BundleView.ID)) {
			return addContribution(menuId, dynamicMainCommandId, showBundleView, bundleViewParamId,
					CommandContributionItem.STYLE_PUSH, bundleListImage);
		} else {
			BundleView bundleView = getBundleView();
			if (null != bundleView && (null != getSelectedJavaProjectFromBundleView())) {
				// Bundle view is open and visible and there is a selection
				if (bundleView.isListPageActive()) {
					return addContribution(menuId, dynamicMainCommandId, showBundleDetailsPage, bundleViewParamId,
							CommandContributionItem.STYLE_PUSH, bundleDetailsImage);
				} else {
					return addContribution(menuId, dynamicMainCommandId, showBundleListPage, bundleViewParamId,
							CommandContributionItem.STYLE_PUSH, bundleListImage);
				}
			} else {
				// Bundle view is open but there is no selection
				return addContribution(menuId, dynamicMainCommandId, hideBundleView, bundleViewParamId,
						CommandContributionItem.STYLE_PUSH, bundleListImage);			
			}
		}
	}

	private CommandContributionItem addToggleConsoleView() {
		if (!BundleConsoleFactory.isConsoleViewVisible()) {
			return addContribution(menuId, dynamicMainCommandId, showConsolePage, consoleParamId,
					CommandContributionItem.STYLE_PUSH, BundleConsole.consoleViewImage);
		} else {
			return addContribution(menuId, dynamicMainCommandId, hideConsolePage, consoleParamId,
					CommandContributionItem.STYLE_PUSH, BundleConsole.consoleViewImage);
		}
	}

	private CommandContributionItem addToggleMessageView() {
		Extension<BundleLogView> ext = new Extension<>(BundleLogView.class);
		BundleLogView viewService = ext.getService();
		if (null == viewService) {
			throw new InPlaceException("failed_to_get_service_for_interface", DependencyDialog.class.getName());
		}
		if (!viewService.isVisible()) {
			return addContribution(menuId, dynamicMainCommandId, showMessageView, messageViewParamId,
					CommandContributionItem.STYLE_PUSH, viewService.getMessageViewImage());
		} else {
			return addContribution(menuId, dynamicMainCommandId, hideMessageView, messageViewParamId,
					CommandContributionItem.STYLE_PUSH, viewService.getMessageViewImage());
		}
	}
}
