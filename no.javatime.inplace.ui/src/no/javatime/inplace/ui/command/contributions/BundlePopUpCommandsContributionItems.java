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

import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.dialogs.OpenProjectHandler;
import no.javatime.inplace.extender.provider.ExtenderException;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleManager;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.project.ManifestOptions;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.command.handlers.BundleMenuActivationHandler;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Message;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * Build the context sensitive pop-up menu
 */
public class BundlePopUpCommandsContributionItems extends BundleCommandsContributionItems {

	public static String menuId = "no.javatime.inplace.command.contributions.dynamicitems.popup";
	public static String dynamicPopUpCommandId = "no.javatime.inplace.command.dynamicitems.popup";

	private static String addClassPathLabel = Message.getInstance().formatString(
			"add_classpath_label_popup"); //$NON-NLS-1$
	private static String removeClassPathLabel = Message.getInstance().formatString(
			"remove_classpath_label_popup"); //$NON-NLS-1$

	// Activation policy per bundle
	public final static String policyParamId = "Policy";
	private final static String eagerLabel = Message.getInstance().formatString("eager_activation_label");
	
	BundleCommand bundleCommand = BundleManager.getCommand();

		public BundlePopUpCommandsContributionItems() {
		super();
	}

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();
		IJavaProject javaProject = BundleMenuActivationHandler.getSelectedJavaProject();
		if (null == javaProject) {
			return new IContributionItem[0];
		}
		try {
			// Get project, activation status and the bundle project
			IProject project = javaProject.getProject();
			Boolean activated = BundleProjectState.isNatureEnabled(project);
			Bundle bundle = BundleManager.getRegion().get(project);
			// Busy running bundle jobs.
			if (OpenProjectHandler.getBundlesJobRunState()) {
				// Do not add contributions for bundles that are dependent on their current state
				contribute(addStopTaskOperation(menuId, dynamicPopUpCommandId), contributions);
				contribute(addInterrupt(menuId, dynamicPopUpCommandId), contributions);
			} else {
				contribute(addActivate(activated, project, bundle), contributions);
				contribute(addDeactivate(activated, project, bundle), contributions);
				contribute(addStart(activated, bundle), contributions);
				contribute(addStop(activated, bundle), contributions);
				contribute(addUpdate(activated, project, bundle), contributions);
				contribute(addRefresh(activated, bundle), contributions);
				contribute(addReset(activated, bundle), contributions);
			}
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contribute(addToggleBundleView(), contributions);
			contribute(addToggleBundleConsoleView(menuId, dynamicPopUpCommandId), contributions);
			contribute(addToggleBundleLogView(menuId, dynamicPopUpCommandId), contributions);
			contributions.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contribute(addClassPath(project), contributions);
			contribute(addEagerActivation(), contributions);
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
	 * Adds a contribution to activate a bundle project if the specified activate parameter is false.
	 * 
	 * @param activated the activation status of the bundle project
	 * @param project project to activate
	 * @param bundle bundle corresponding to the project
	 * @return a contribution to activate the bundle project or null if the specified activate
	 * parameter is true
	 */
	private CommandContributionItem addActivate(Boolean activated, IProject project, Bundle bundle) {
		String label;
		String stateName = " (";
		if (!activated) {
			stateName += bundleCommand.getStateName(bundle) + ")";
			label = activateProjectParamId;
			label += " " + stateName;
			return createContibution(menuId, dynamicPopUpCommandId, label, activateProjectParamId,
					CommandContributionItem.STYLE_PUSH, activateImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to deactivate a bundle project if the specified activate parameter is true.
	 * 
	 * @param activated the activation status of the bundle project
	 * @param project project to deactivate
	 * @param bundle bundle corresponding to the project
	 * @return a contribution to deactivate the bundle project or null if the specified activate
	 * parameter is false
	 */
	private CommandContributionItem addDeactivate(Boolean activated, IProject project, Bundle bundle) {
		String label;
		String stateName = " (";
		if (activated) {
			stateName += bundleCommand.getStateName(bundle) + ")";
			label = deactivateParamId;
			label += " " + stateName;
			return createContibution(menuId, dynamicPopUpCommandId, label, deactivateParamId,
					CommandContributionItem.STYLE_PUSH, deactivateImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to start the specified bundle if the bundle is activated and not in state
	 * active.
	 * 
	 * @param activated the activation status of the bundle project
	 * @param bundle the bundle to start
	 * @return a contribution to start the bundle or null if it is not activated or already in state
	 * active
	 * @throws InPlaceException if the specified bundle is null or a security violation
	 */
	private CommandContributionItem addStart(Boolean activated, Bundle bundle)
			throws InPlaceException {
		if (activated) {
			if (!ManifestOptions.isFragment(bundle)) {
				if ((bundle.getState() & (Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
					return createContibution(menuId, dynamicPopUpCommandId, startParamId, startParamId,
							CommandContributionItem.STYLE_PUSH, startImage);
				}
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to stop the specified bundle if the bundle is activated and in state
	 * active.
	 * 
	 * @param activated the activation status of the bundle project
	 * @param bundle the bundle to stop
	 * @return a contribution to stop the bundle or null if it is not activated, not in state active
	 * or the bundle is null
	 */
	private CommandContributionItem addStop(Boolean activated, Bundle bundle) {
		if (null != bundle && activated) {
			if ((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
				return createContibution(menuId, dynamicPopUpCommandId, stopParamId, stopParamId,
						CommandContributionItem.STYLE_PUSH, stopImage);
			}
		}
		return null;
	}

	/**
	 * Adds a contribution to refresh the specified bundle if the bundle is activated and has more
	 * than one revision
	 * 
	 * @param activated the activation status of the bundle project
	 * @param bundle the bundle to refresh
	 * @return a contribution to refresh the bundle or null if it is not activated, has only one
	 * revision or the bundle is null
	 * @throws InPlaceException if the bundle is null or a proper adapt permission is missing
	 */
	private CommandContributionItem addRefresh(Boolean activated, Bundle bundle)
			throws InPlaceException {

		// Conditional enabling of refresh
		if (activated && bundleCommand.getBundleRevisions(bundle).size() > 1) {
			return createContibution(menuId, dynamicPopUpCommandId, refreshParamId, refreshParamId,
					CommandContributionItem.STYLE_PUSH, refreshImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to update the specified bundle if the bundle is activated and and has a
	 * pending update transition attached to it
	 * 
	 * @param activated the activation status of the bundle project
	 * @param bundle the bundle to update
	 * @return a contribution to update the bundle or null if it is not activated or no pending update
	 * transition
	 */
	private CommandContributionItem addUpdate(Boolean activated, IProject project, Bundle bundle) {
		if (null != bundle
				&& activated
				&& BundleManager.getTransition().containsPending(project, Transition.UPDATE,
						Boolean.FALSE)) {
			return createContibution(menuId, dynamicPopUpCommandId, updateParamId, updateParamId,
					CommandContributionItem.STYLE_PUSH, updateImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to reset the specified bundle if the bundle is activated
	 * 
	 * @param activated the activation status of the bundle project
	 * @param bundle the bundle to reset
	 * @return a contribution to update the bundle or null if it is not activated
	 */
	private CommandContributionItem addReset(Boolean activated, Bundle bundle) {
		if (null != bundle && activated) {
			return createContibution(menuId, dynamicPopUpCommandId, resetParamId, resetParamId,
					CommandContributionItem.STYLE_PUSH, resetImage);
		}
		return null;
	}

	/**
	 * Adds a contribution to add or remove the default output folder
	 * 
	 * @param project The project that contains or is missing the default output folder
	 * @return a contribution to add or remove the default output folder on the specified project or
	 * null if there are any build errors in the manifest
	 * @throws InPlaceException if a core exception occurs, manifest file is missing or the project
	 * description could not be obtained
	 */
	private CommandContributionItem addClassPath(IProject project) throws InPlaceException {

		if (!BuildErrorClosure.hasManifestBuildErrors(project)) {
			if (!BundleProjectSettings.isOutputFolderInBundleClassPath(project)) {
				return createContibution(menuId, dynamicPopUpCommandId, addClassPathLabel,
						addClassPathParamId, CommandContributionItem.STYLE_PUSH, classPathImage);
			} else {
				return createContibution(menuId, dynamicPopUpCommandId, removeClassPathLabel,
						removeClassPathParamId, CommandContributionItem.STYLE_PUSH, classPathImage);
			}
		}
		return null;
	}

	/**
	 * When bundle view is hidden return a contribution item offering to open the view and vice versa
	 * if the view is open and there is no active project. If the view is open and the list page is
	 * active with a selected project return a contribution item offering to show the details page and
	 * vice versa if the details page is active.
	 * 
	 * @return contribution item offering to show or hide the bundle view or show the list or the
	 * details page in an open view with an active project.
	 */
	private CommandContributionItem addToggleBundleView() {
		if (!ViewUtil.isVisible(BundleView.ID)) {
			return createContibution(menuId, dynamicPopUpCommandId, showBundleView, bundleViewParamId,
					CommandContributionItem.STYLE_PUSH, bundleListImage);
		} else {
			// Flip list and details bundle page
			BundleView bundleView = getBundleView();
			if (null != bundleView) {
				if (bundleView.isListPageActive()) {
					return createContibution(menuId, dynamicPopUpCommandId, showBundleDetailsPage,
							bundleViewParamId, CommandContributionItem.STYLE_PUSH, bundleDetailsImage);
				} else {
					return createContibution(menuId, dynamicPopUpCommandId, showBundleListPage,
							bundleViewParamId, CommandContributionItem.STYLE_PUSH, bundleListImage);
				}
			}
		}
		return createContibution(menuId, dynamicPopUpCommandId, hideBundleView, bundleViewParamId,
				CommandContributionItem.STYLE_PUSH, bundleListImage);
	}

	/**
	 * Creates a contribution with a check style to change the activation policy of the current bundle
	 * 
	 * @return a contribution to change activation policy
	 */
	private CommandContributionItem addEagerActivation() {
		return createContibution(menuId, dynamicPopUpCommandId, eagerLabel, policyParamId,
				CommandContributionItem.STYLE_CHECK, null);
	}
}
