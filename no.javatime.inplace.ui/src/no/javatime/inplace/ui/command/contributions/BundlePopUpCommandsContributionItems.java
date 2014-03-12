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

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ManifestUtil;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.handlers.BundleMenuActivationHandler;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.views.BundleConsole;
import no.javatime.util.messages.views.BundleConsoleFactory;
import no.javatime.util.messages.views.MessageView;


/**
 * Build the context sensitive pop-up menu 
 */
public class BundlePopUpCommandsContributionItems extends BundleCommandsContributionItems {

	public static String dynamicPopUpCommandId = "no.javatime.inplace.command.dynamicitems.popup";
	public static String menuId = "no.javatime.inplace.command.contributions.dynamicitems.popup";

	private static String addClassPathLabel = Message.getInstance().formatString("add_classpath_label_popup"); //$NON-NLS-1$
	private static String removeClassPathLabel = Message.getInstance().formatString("remove_classpath_label_popup"); //$NON-NLS-1$

	// Activation policy per bundle
	public static String policyParamId = Message.getInstance().formatString("policy_menu_parameter");
	private static String eagerLabel = Message.getInstance().formatString("eager_activation_label");

	public BundlePopUpCommandsContributionItems() {
		super();
	}

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();
		CommandContributionItem contributor = null;
		IJavaProject javaProject = BundleMenuActivationHandler.getSelectedJavaProject();
		if (null == javaProject) {
			return new IContributionItem[0];
		} 
		// Get project, activation status and the bundle project
		IProject project = javaProject.getProject();
		Boolean activated = ProjectProperties.isProjectActivated(project);
		Bundle bundle = BundleManager.getRegion().get(project);

		// Busy running bundle jobs. Show a limited set of contributors
		if (OpenProjectHandler.getBundlesJobRunState()) {
			contributions.add(addBusy(menuId, dynamicPopUpCommandId));
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contributor = addRefresh(activated, bundle);
			if (null != contributor) {
				contributions.add(contributor);
			}
			contributor = addReset(activated, bundle);
			if (null != contributor) {
				contributions.add(contributor);
			}
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contributor = addShowBundleView();
			if (null != contributor) {
				contributions.add(contributor);
			}
			contributions.add(addToggleConsoleView());
			contributions.add(addToggleMessageView());
			IContributionItem[] contributionArray = contributions
					.toArray(new ContributionItem[contributions.size()]);
			return contributionArray;
		}
		
		if (!activated) {
			contributor = addActivate(activated, project, bundle);
			if (null != contributor) {
				contributions.add(contributor);
			}
		} else {
			contributor = addDeactivate(activated, project, bundle);
			if (null != contributor) {
				contributions.add(contributor);
			}
		}
		contributor = addStart(activated, bundle);
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributor = addStop(activated, bundle);
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributor = addRefresh(activated, bundle);
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributor = addUpdate(activated, project, bundle);
		if (null != contributor) {
			contributions.add(contributor);
		}		
		contributor = addReset(activated, bundle);
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		contributor = addShowBundleView();
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributions.add(addToggleConsoleView());
		contributions.add(addToggleMessageView());
		contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		contributor = addClassPath(project);
		if (null != contributor) {
			contributions.add(contributor);
		}
		contributions.add(addEagerActivation());
		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;

	}

	private CommandContributionItem addActivate(Boolean activated, IProject project, Bundle bundle) {
		String label;
		String stateName = " (";
		if (!activated) {
			stateName += BundleManager.getCommand().getStateName(bundle) + ")";
			label = activateParamId;
			label += " " + stateName;
			return addContribution(menuId, dynamicPopUpCommandId, label, activateParamId,
					CommandContributionItem.STYLE_PUSH, activateImage);
		}
		return null;
	}

	private CommandContributionItem addDeactivate(Boolean activated, IProject project, Bundle bundle) {
		String label;
		String stateName = " (";
		if (activated) {
			stateName += BundleManager.getCommand().getStateName(bundle) + ")";
			label = deactivateParamId;
			label += " " + stateName;
			return addContribution(menuId, dynamicPopUpCommandId, label, deactivateParamId,
					CommandContributionItem.STYLE_PUSH, deactivateImage);
		}
		return null;
	}

	private CommandContributionItem addStart(Boolean activated, Bundle bundle) {
		if (null != bundle && activated) {
			int state = bundle.getState();
			if (!ManifestUtil.isFragment(bundle) && (state & (Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
				return addContribution(menuId, dynamicPopUpCommandId, startParamId, startParamId,
						CommandContributionItem.STYLE_PUSH, startImage);
			}
		}
		return null;
	}

	private CommandContributionItem addStop(Boolean activated, Bundle bundle) {
		if (null != bundle && activated) {
			int state = bundle.getState();
			if ((state & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
				return addContribution(menuId, dynamicPopUpCommandId, stopParamId, stopParamId,
						CommandContributionItem.STYLE_PUSH, stopImage);
			}
		}
		return null;
	}

	private CommandContributionItem addRefresh(Boolean activated, Bundle bundle) {

		// Conditional enabling of refresh
		if (null != bundle && activated && BundleManager.getCommand().getBundleRevisions(bundle).size() > 1) {
			return addContribution(menuId, dynamicPopUpCommandId, refreshParamId, refreshParamId,
					CommandContributionItem.STYLE_PUSH, refreshImage);
		}
		return null;
	}

	private CommandContributionItem addUpdate(Boolean activated, IProject project, Bundle bundle) {
		if (null != bundle && activated && BundleManager.getTransition().containsPending(project, Transition.UPDATE, Boolean.FALSE)) {
			return addContribution(menuId, dynamicPopUpCommandId, updateParamId, updateParamId,
					CommandContributionItem.STYLE_PUSH, updateImage);
		}
		return null;
	}

	private CommandContributionItem addReset(Boolean activated, Bundle bundle) {
		if (null != bundle && activated) {
			return addContribution(menuId, dynamicPopUpCommandId, resetParamId, resetParamId,
					CommandContributionItem.STYLE_PUSH, resetImage);
		}
		return null;
	}

	private CommandContributionItem addClassPath(IProject project) {
		try {
			if (!ProjectProperties.hasManifestBuildErrors(project)) {
				if (!BundleProject.isOutputFolderInBundleClassPath(project)) {
					return addContribution(menuId, dynamicPopUpCommandId, addClassPathLabel, addClassPathParamId,
							CommandContributionItem.STYLE_PUSH, classPathImage);
				} else {
					return addContribution(menuId, dynamicPopUpCommandId, removeClassPathLabel, removeClassPathParamId,
							CommandContributionItem.STYLE_PUSH, classPathImage);				
				}
			}
		} catch (InPlaceException e) {
			return null; // Menu item not displayed
		}
		return null;
	}
	/**
	 * When bundle view is hidden return a contribution item offering to open the view and vice versa if the
	 * view is open and there is no active project. If the view is open and the list page is active with a selected project return a
	 * contribution item offering to show the details page and vice versa if the details page is active.
	 * 
	 * @return contribution item offering to show or hide the bundle view or show the list or the details page
	 *         in an open view with an active project.
	 */
	private CommandContributionItem addShowBundleView() {
		if (!Message.isViewVisible(BundleView.ID)) {
			return addContribution(menuId, dynamicPopUpCommandId, showBundleView, bundleViewParamId,
					CommandContributionItem.STYLE_PUSH, bundleListImage);
		} else {
			// Flip list and details bundle page
			BundleView bundleView = getBundleView();
			if (null != bundleView) {
				if (bundleView.isListPageActive()) {
					return addContribution(menuId, dynamicPopUpCommandId, showBundleDetailsPage,
							bundleViewParamId, CommandContributionItem.STYLE_PUSH, bundleDetailsImage);
				} else {
					return addContribution(menuId, dynamicPopUpCommandId, showBundleListPage, bundleViewParamId,
							CommandContributionItem.STYLE_PUSH, bundleListImage);
				}
			}
		}
		return addContribution(menuId, dynamicPopUpCommandId, hideBundleView, bundleViewParamId,
				CommandContributionItem.STYLE_PUSH, bundleListImage);
	}

	private CommandContributionItem addToggleConsoleView() {
		if (!BundleConsoleFactory.isConsoleViewVisible()) {
			return addContribution(menuId, dynamicPopUpCommandId, showConsolePage, consoleParamId,
					CommandContributionItem.STYLE_PUSH, BundleConsole.consoleViewImage);
		} else {
			return addContribution(menuId, dynamicPopUpCommandId, hideConsolePage, consoleParamId,
					CommandContributionItem.STYLE_PUSH, BundleConsole.consoleViewImage);
		}
	}

	private CommandContributionItem addToggleMessageView() {
		if (!Message.isViewVisible(MessageView.ID)) {
			return addContribution(menuId, dynamicPopUpCommandId, showMessageView, messageViewParamId,
					CommandContributionItem.STYLE_PUSH, MessageView.messageViewImage);
		} else {
			return addContribution(menuId, dynamicPopUpCommandId, hideMessageView, messageViewParamId,
					CommandContributionItem.STYLE_PUSH, MessageView.messageViewImage);
		}
	}

	private CommandContributionItem addEagerActivation() {
		return addContribution(menuId, dynamicPopUpCommandId, eagerLabel, policyParamId,
				CommandContributionItem.STYLE_CHECK, null);
	}
}
