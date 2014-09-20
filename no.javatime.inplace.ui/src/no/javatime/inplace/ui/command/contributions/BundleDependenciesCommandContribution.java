package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.ExtenderException;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
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
		try {
			if (null != Extender.getInstance(DependencyDialog.class)) {
				contributions.add(createContibution(menuIdDependencies, dynamicMainCommandId, partialDependenciesLabel, dependencyDialogParamId,
						CommandContributionItem.STYLE_PUSH, dependenciesImage));
			}
		} catch (ExtenderException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.ADD_CONTRIBUTION_ERROR, e),
					StatusManager.LOG);
		}
		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}	
}
