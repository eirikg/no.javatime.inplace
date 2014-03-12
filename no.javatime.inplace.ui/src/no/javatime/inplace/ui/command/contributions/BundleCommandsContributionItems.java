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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.services.IServiceLocator;

import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundleproject.OpenProjectHandler;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.views.BundleProperties;
import no.javatime.inplace.ui.views.BundleView;
import no.javatime.util.messages.Message;

/**
 * Common parameters, images, labels and a utility for adding contribution elements to the main menu and the
 * context sensitive pop-up menu
 */
public abstract class BundleCommandsContributionItems extends CompoundContributionItem implements IWorkbenchContribution {  
	
	// Main dynamic menu id
	public static String dynamicMainCommandId = "no.javatime.inplace.command.dynamicitems.main";

	// Menu parameters
	public static String menuIdPar = "no.javatime.inplace.commandParameter.menuid"; //$NON-NLS-1$

	// Menu commands as parameters (some of them are also used as labels)
	public static String busyParamId = "Busy"; //$NON-NLS-1$

	public static String activateParamId = "Activate"; //$NON-NLS-1$
	public static String installParamId = "Install"; //$NON-NLS-1$
	public static String deactivateParamId = "Deactivate"; //$NON-NLS-1$
	public static String refreshParamId = "Refresh"; //$NON-NLS-1$
	public static String refreshPendingParamId = "Refresh Pending"; //$NON-NLS-1$
	public static String updateParamId = "Update"; //$NON-NLS-1$
	public static String startParamId = "Start"; //$NON-NLS-1$
	public static String stopParamId = "Stop"; //$NON-NLS-1$
	public static String resetParamId = "Reset"; //$NON-NLS-1$
	public static String bundleViewParamId = "bundleView"; //$NON-NLS-1$
	public static String consoleParamId = "consolePage"; //$NON-NLS-1$
	public static String messageViewParamId = "messageView"; //$NON-NLS-1$
	public static String addClassPathParamId = Message.getInstance().formatString("add_classpath_menu_parameter"); //$NON-NLS-1$
	public static String removeClassPathParamId = Message.getInstance().formatString("remove_classpath_menu_parameter"); //$NON-NLS-1$

	// Menu icons
	public static ImageDescriptor activateImage = Activator.getImageDescriptor("icons/activate.gif"); //$NON-NLS-1$
	public static ImageDescriptor deactivateImage = Activator.getImageDescriptor("icons/deactivate.gif"); //$NON-NLS-1$
	public static ImageDescriptor refreshImage = Activator.getImageDescriptor("icons/refresh.gif"); //$NON-NLS-1$
	public static ImageDescriptor updateImage = Activator.getImageDescriptor("icons/update.gif"); //$NON-NLS-1$
	public static ImageDescriptor startImage = Activator.getImageDescriptor("icons/running.gif"); //$NON-NLS-1$
	public static ImageDescriptor stopImage = Activator.getImageDescriptor("icons/stop.gif"); //$NON-NLS-1$
	public static ImageDescriptor resetImage = Activator.getImageDescriptor("icons/reset.gif"); //$NON-NLS-1$
	public static ImageDescriptor bundleDetailsImage = Activator
			.getImageDescriptor("icons/gear_details_title.png"); //$NON-NLS-1$
	public static ImageDescriptor bundleListImage = Activator.getImageDescriptor("icons/gear_list_title.png"); //$NON-NLS-1$

	public static ImageDescriptor classPathImage = Activator.getImageDescriptor("icons/classpath.gif"); //$NON-NLS-1$

	// Menu Labels
	protected static String showBundleView = "Show Bundle View"; //$NON-NLS-1$
	protected static String hideBundleView = "Hide Bundle View"; //$NON-NLS-1$
	protected static String showBundleListPage = "Show Bundle List Page"; //$NON-NLS-1$
	final protected String showBundleDetailsPage = "Show Bundle Details Page"; 
	// Message.getInstance().formatString("flip_details_general_text"); //$NON-NLS-1$
	protected static String showConsolePage = "Show Console Page"; //$NON-NLS-1$
	protected static String hideConsolePage = "Hide Console Page"; //$NON-NLS-1$
	protected static String showMessageView = "Show Message View"; //$NON-NLS-1$
	protected static String hideMessageView = "Hide Message View"; //$NON-NLS-1$
	
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

	protected CommandContributionItem addContribution(String menuId, String commandId, String label,
			String paramId, int style, ImageDescriptor icon) {

		Map<String, String> params = new HashMap<String, String>();

		params.put(menuIdPar, paramId);
		CommandContributionItemParameter cmdPar = new CommandContributionItemParameter(
				serviceLocator, //Activator.getDefault().getActiveWorkbenchWindow(), 
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

	protected CommandContributionItem addBusy(String menuId, String commandId) {
		BundleJob job = OpenProjectHandler.getRunningBundleJob();
		String menuLabel;
		if (null == job) {
			menuLabel = "About to finish job ...";
		} else {
			menuLabel = "Cancel job " + job.getName();
		}
		return addContribution(menuId, commandId, menuLabel, busyParamId,
				CommandContributionItem.STYLE_PUSH, null);
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
	 * @return the bundle view object or null if not present
	 */
	public static BundleView getBundleView() {
		return (BundleView) Message.getView(BundleView.ID);
	}
	
	/**
	 * Get and return the selected project in the bundle view
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
			// Bundles view model properties
			if (element instanceof BundleProperties) {
				BundleProperties bp = (BundleProperties) element;
				return bp.getJavaProject();
			}
		}
		return null;
	}

}
