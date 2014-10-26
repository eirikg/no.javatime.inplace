package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.Message;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Creates a contribution for the bundle dependency dialog 
 */
public class BundleDependenciesCommandContribution extends BundleMainCommandsContributionItems {

	private String menuIdDependencies = "no.javatime.inplace.command.contributions.dynamicitems.main.dependencies";

	private static String partialDependenciesLabel = Message.getInstance().formatString("partial_dependencies_label_main"); //$NON-NLS-1$

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();
		Extension<DependencyDialog> ext = null;
		try {
			ext = Extenders.getExtension(DependencyDialog.class.getName());
			DependencyDialog depDlgService = ext.getService();
			if (null != depDlgService) {
				contributions.add(createContibution(menuIdDependencies, dynamicMainCommandId, partialDependenciesLabel, dependencyDialogParamId,
						CommandContributionItem.STYLE_PUSH, dependenciesImage));
			} else {
				InPlaceException e = new InPlaceException("failed_to_get_service_for_interface", DependencyDialog.class.getName());
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.ADD_CONTRIBUTION_ERROR, e),
						StatusManager.LOG);
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.ADD_CONTRIBUTION_ERROR, e),
					StatusManager.LOG);
		}
		if (null != ext) {
			ext.ungetService();
		}
		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}	
}
