package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.ui.Activator;
import no.javatime.util.messages.Message;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;

/**
 * Creates a contribution for the bundle dependency dialog 
 */
public class BundleDependenciesCommandContribution extends BundleMainCommandsContributionItems {

	private String menuIdDependencies = "no.javatime.inplace.command.contributions.dynamicitems.main.dependencies";

	private static String partialDependenciesLabel = Message.getInstance().formatString("partial_dependencies_label_main"); //$NON-NLS-1$

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<>();
		try {
			if (null != Activator.getTracker().getTrackedExtender(DependencyDialog.class.getName())) {
				contributions.add(createContibution(menuIdDependencies, dynamicMainCommandId, partialDependenciesLabel, dependencyDialogParamId,
						CommandContributionItem.STYLE_PUSH, dependenciesImage));
			}
		} catch (ExtenderException e) {
			// Don't display menu entry when unavailable
		}		
		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}	
}
