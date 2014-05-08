package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;

import no.javatime.inplace.ui.extender.DependencyDialogProxy;
import no.javatime.util.messages.Message;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;

public class BundleDependenciesCommandContribution extends BundleCommandsContributionItems {

	private String menuIddependencies = "no.javatime.inplace.command.contributions.dynamicitems.main.dependencies";
	
	private static String partialDependenciesLabel = Message.getInstance().formatString("partial_dependencies_label_main"); //$NON-NLS-1$

	@Override
	protected IContributionItem[] getContributionItems() {
		
		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();

		if (null != DependencyDialogProxy.getInstance()) {
			contributions.add(addContribution(menuIddependencies, dynamicMainCommandId, partialDependenciesLabel, partialDependenciesParamId,
					CommandContributionItem.STYLE_PUSH, dependenciesImage));
		}
		IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
		return contributionArray;
	}	
}
