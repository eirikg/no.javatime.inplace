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
import java.util.HashMap;
import java.util.Map;

import no.javatime.inplace.bundlejobs.intface.ResourceState;
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.pl.console.intface.BundleConsoleFactory;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.inplace.ui.views.BundleProperties;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.view.ViewUtil;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Common parameters, images, labels and contribution elements to the main menu
 * and the pop-up menu
 */
public abstract class BundleCommandsContributionItems extends CompoundContributionItem implements
		IWorkbenchContribution {


	// Menu parameters
	public static String menuIdPar = "no.javatime.inplace.commandParameter.menuid"; //$NON-NLS-1$

	// Menu commands as parameters (some of them are also used as labels)

	public final static String activateProjectParamId = "Activate Project"; //$NON-NLS-1$
	public final static String installParamId = "Install"; //$NON-NLS-1$
	public final static String deactivateParamId = "Deactivate"; //$NON-NLS-1$
	public final static String refreshParamId = "Refresh"; //$NON-NLS-1$
	public final static String refreshPendingParamId = "Refresh Pending"; //$NON-NLS-1$
	public final static String updateParamId = "Update"; //$NON-NLS-1$
	public final static String startParamId = "Start"; //$NON-NLS-1$
	public final static String stopParamId = "Stop"; //$NON-NLS-1$
	public final static String resetParamId = "Reset"; //$NON-NLS-1$
	public final static String bundleViewParamId = "BundleView"; //$NON-NLS-1$
	public final static String bundleConsolePageParamId = "Bundle Console Page"; //$NON-NLS-1$
	public final static String bundleLogViewParamId = "Bundle Log View"; //$NON-NLS-1$
	public final static String addClassPathParamId = "Add ClassPath";
	public final static String removeClassPathParamId = "Remove ClassPath";
	public final static String stopOperationParamId = "Stop Bundle Operation"; //$NON-NLS-1$
	public final static String interruptParamId = "Interrupt"; //$NON-NLS-1$
	public final static String dependencyDialogParamId = "Dependency Dialog";

	// Menu icons
	public static ImageDescriptor activateImage = Activator.getImageDescriptor("icons/activate.gif"); //$NON-NLS-1$
	public static ImageDescriptor deactivateImage = Activator
			.getImageDescriptor("icons/deactivate.gif"); //$NON-NLS-1$
	public static ImageDescriptor refreshImage = Activator.getImageDescriptor("icons/refresh.gif"); //$NON-NLS-1$
	public static ImageDescriptor updateImage = Activator.getImageDescriptor("icons/update.gif"); //$NON-NLS-1$
	public static ImageDescriptor startImage = Activator.getImageDescriptor("icons/running.gif"); //$NON-NLS-1$
	public static ImageDescriptor stopImage = Activator.getImageDescriptor("icons/stop.gif"); //$NON-NLS-1$
	public static ImageDescriptor resetImage = Activator.getImageDescriptor("icons/reset.gif"); //$NON-NLS-1$
	public static ImageDescriptor bundleDetailsImage = Activator
			.getImageDescriptor("icons/gear_details_title.png"); //$NON-NLS-1$
	public static ImageDescriptor bundleListImage = Activator
			.getImageDescriptor("icons/gear_list_title.png"); //$NON-NLS-1$
	public static ImageDescriptor classPathImage = Activator
			.getImageDescriptor("icons/classpath.gif"); //$NON-NLS-1$
	public static ImageDescriptor dependenciesImage = Activator
			.getImageDescriptor("icons/dependencies.gif"); //$NON-NLS-1$

	// Menu Labels
	protected static String showBundleView = "Show Bundle View            Alt+Shift+Q U"; //$NON-NLS-1$
	protected static String hideBundleView = "Hide Bundle View"; //$NON-NLS-1$
	protected static String showBundleListPage = "Show Bundle List Page"; //$NON-NLS-1$
	final protected String showBundleDetailsPage = "Show Bundle Details Page";
	protected static String showConsolePage = "Show Console Page         Alt+Shift+Q C"; //$NON-NLS-1$
	protected static String hideConsolePage = "Hide Console Page"; //$NON-NLS-1$
	protected static String showMessageView = "Show Bundle Log"; //$NON-NLS-1$
	protected static String hideMessageView = "Hide Bundle Log"; //$NON-NLS-1$

	private IServiceLocator serviceLocator;

	protected BundleCommandsContributionItems() {
		super();
	}

	@Override
	public void initialize(final IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	@Override
	protected abstract IContributionItem[] getContributionItems();

	/**
	 * Contribution to the specified menu id for terminating OSGi start and stop operations.
	 * 
	 * @param menuId a dynamic menu id. This is usually the bundle main or pop-up menu id
	 * @param commandId The command id for a defined command associated with the specified menu id
	 * @return the command contribution item if a bundle job is running, the operation currently
	 * executing is a start or stop operation and the manual terminate option for endless start/stop
	 * operations is enabled. Null if one of these conditions are false
	 */
	protected CommandContributionItem addStopTaskOperation(String menuId, String commandId) {

		Extension<Stop> stopExt = null;
		try {
			ResourceState resourceState = Activator.getResourceStateService();
			stopExt = Activator.getExtension(Stop.class.getName());
			Stop stop = stopExt.getTrackedService();
			CommandOptions commandOption = Activator.getCommandOptionsService();
			boolean timeOut = commandOption.isTimeOut();
			Job job = resourceState.getRunningBundleJob();
			if (null != job && !timeOut && stop.isStateChanging()) {
				// A job currently executing a start or stop operation and the manual terminate of endless
				// operations option is enabled
				return createContibution(menuId, commandId, Msg.STOP_BUNDLE_OP_LABEL, stopOperationParamId,
						CommandContributionItem.STYLE_PUSH, null);
			}
		} catch (ExtenderException e) {
			StatusManager.getManager()
			.handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID,
							Msg.ADD_CONTRIBUTION_ERROR, e), StatusManager.LOG);
		} finally {
			if (null != stopExt) {
				stopExt.closeTrackedService();				
			}
		}
		return null;
	}

	/**
	 * Contribution to the specified menu id for interrupting a running bundle job or an information
	 * message that the job is about finish if there is no running job
	 * 
	 * @param menuId a dynamic menu id. This is usually the bundle main or pop-up menu id
	 * @param commandId The command id for a defined command associated with the specified menu id
	 * @return the command contribution item with an message to interrupt if a bundle job is running
	 * or a message that the job is about to terminate if there is no job running
	 */
	protected CommandContributionItem addInterrupt(String menuId, String commandId) {

		ResourceState resourceState = Activator.getResourceStateService();
		Job job = resourceState.getRunningBundleJob();
		String menuLabel;
		if (null == job) {
			menuLabel = Msg.ABOUT_TO_FINISH_JOB_LABEL;
		} else {
			menuLabel = NLS.bind(Msg.INTERRUPT_JOB_LABEL, job.getName());
		}
		return createContibution(menuId, commandId, menuLabel, interruptParamId,
				CommandContributionItem.STYLE_PUSH, null);
	}

	/**
	 * Creates a contribution for toggling the bundle console view.  
	 * 
	 * @param menuId a dynamic menu id. This is usually the bundle main or pop-up menu id
	 * @param commandId The command id for a defined command associated with the specified menu id
	 * @return contribution for hiding or showing the bundle console view or null if failed to get bundle console view
	 */
	protected CommandContributionItem addToggleBundleConsoleView(String menuId, String commandId) {
		
		Extension<BundleConsoleFactory> extension = null;
		try {
			extension = Activator.getExtension(BundleConsoleFactory.class);
			BundleConsoleFactory console = extension.getTrackedService();
			ImageDescriptor image = console.getConsoleViewImage();
			if (!console.isConsoleViewVisible()) {
				return createContibution(menuId, commandId, showConsolePage, bundleConsolePageParamId,
						CommandContributionItem.STYLE_PUSH, image);
			} else {
				return createContibution(menuId, commandId, hideConsolePage, bundleConsolePageParamId,
						CommandContributionItem.STYLE_PUSH, image);
			}
		} catch (ExtenderException e) {
			// Don't display menu entry when extension is not available
		} finally {
			if (null != extension) {
				extension.closeTrackedService();
			}
		}
		return null;
	}

	/**
	 * Creates a contribution for toggling the bundle log view.  
	 * 
	 * @param menuId a dynamic menu id. This is usually the bundle main or pop-up menu id
	 * @param commandId The command id for a defined command associated with the specified menu id
	 * @return contribution for hiding or showing the bundle log view or null if failed to get the log view
	 */
	protected CommandContributionItem addToggleBundleLogView(String menuId, String commandId) {
		Extension<BundleLogView> extension = null;;
		try {
			extension = Activator.getExtension(BundleLogView.class);
			BundleLogView logView = extension.getTrackedService();
			ImageDescriptor image = logView.getLogViewImage();
			if (!logView.isVisible()) {
				return createContibution(menuId, commandId, showMessageView, bundleLogViewParamId,
						CommandContributionItem.STYLE_PUSH, image);
			} else {
				return createContibution(menuId, commandId, hideMessageView, bundleLogViewParamId,
						CommandContributionItem.STYLE_PUSH, image);
			}
		} catch (ExtenderException e) {
			// Don't display menu entry when extension is not available
		} finally {
			if (null != extension) {
				extension.closeTrackedService();
			}
		}
		return null;
	}

	/**
	 * Utility to add a contribution to a list of contributions
	 * 
	 * @param contribution the contribution to add to the contribution list. May be null
	 * @param contributions the contribution list to add contributions to. Must not be null
	 */
	protected void contribute(CommandContributionItem contribution,
			ArrayList<ContributionItem> contributions) {
		if (null != contribution) {
			contributions.add(contribution);
		}
	}

	/**
	 * Utility to construct a contribution item that can be used as is with the specified parameters set on the item.
	 * <p>
	 * This only sets a minimum of possible parameters. Additional parameters can be set on the returned item.
	 * The visible parameter is enabled by default, the help context parameter is set to null, the disabled and
	 * hover icon parameters are set to null, the tool tip parameter is set to null and the mnemonic
	 * parameter is set to null.
	 * 
	 * @param menuId A dynamic menu id. This is usually the bundle main or pop-up menu id
	 * @param commandId The command id for a defined command associated with the specified menu id
	 * @param label The label of the menu item
	 * @param paramId Parameter identifying the contribution and to pass on to menu handlers of the
	 * contribution
	 * @param style standard style parameter for a menu item
	 * @param icon The Icon to display along with the menu item.
	 * @return a new contribution item
	 * @see org.eclipse.ui.menus#CommandContributionItemParameter

	 */
	protected CommandContributionItem createContibution(String menuId, String commandId,
			String label, String paramId, int style, ImageDescriptor icon) {

		Map<String, String> params = new HashMap<String, String>();

		params.put(menuIdPar, paramId);
		CommandContributionItemParameter cmdPar = new CommandContributionItemParameter(serviceLocator, 
				menuId, // id
				commandId, // Command id
				params, // Command parameters
				icon, null, null, // Icon, disabled icon, hover icon
				label, // Label
				null, // Mnemonic
				null, // Tool tip
				style, // Style
				null, // Help context id
				true); // Visible enabled

		return new CommandContributionItem(cmdPar);
	}

	protected String formatLabel(String preLabel, int items, Boolean includeBundleName) {
		StringBuffer label = new StringBuffer(preLabel);
		if (includeBundleName) {
			label.append(" Bundle");
			if (items > 1) {
				label.append('s');
			}
		}
		label.append(" ( ");
		label.append(items);
		label.append(" )");
		return label.toString();
	}

	/**
	 * Get the bundle view
	 * 
	 * @return the bundle view object or null if not present
	 */
	public static BundleView getBundleView() {
		return (BundleView) ViewUtil.get(BundleView.ID);
	}

	/**
	 * Get and return the selected project in the bundle view
	 * 
	 * @return the selected project in bundle view or null if no selection
	 */
	protected static IJavaProject getSelectedJavaProjectFromBundleView() {

		// Get the selection from the active page
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null == page) {
			return null;
		}
		IWorkbenchPart activePart = page.getActivePart();
		if (null == activePart) {
			return null;
		}
		ISelection selection = null;
		if (activePart instanceof BundleView) {
			selection = page.getSelection(BundleView.ID);
		}
		if (null != selection && selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			// Bundle view model properties
			if (element instanceof BundleProperties) {
				BundleProperties bp = (BundleProperties) element;
				return bp.getJavaProject();
			}
		}
		return null;
	}

}
