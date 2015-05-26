package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;

/**
 * Creates a contribution for the bundle dependency dialog
 */
public class BundleDependenciesCommandContribution extends BundleMainCommandsContributionItems {

	private String menuIdDependencies = "no.javatime.inplace.command.contributions.dynamicitems.main.dependencies";

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<>();
		try {
			if (null != Activator.getTracker().getTrackedExtender(DependencyDialog.class.getName())) {
				contributions.add(createContibution(menuIdDependencies, dynamicMainCommandId,
						Msg.DEPENDENCY_CLOSURE_MAIN_LABEL, dependencyDialogParamId,
						CommandContributionItem.STYLE_PUSH, dependenciesImage));
			}
		} catch (ExtenderException e) {
			// Don't display menu entry when unavailable
		}
		IContributionItem[] contributionArray = contributions
				.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}
}
