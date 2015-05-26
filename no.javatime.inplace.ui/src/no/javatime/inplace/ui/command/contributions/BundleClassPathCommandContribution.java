package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;
import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Adds contributions for collectively adding and removing default output folder in a set of bundles
 * 
 */
public class BundleClassPathCommandContribution extends BundleMainCommandsContributionItems {

	private String menuIdClasspath = "no.javatime.inplace.command.contributions.dynamicitems.main.classpath";

	@Override
	protected IContributionItem[] getContributionItems() {

		ArrayList<ContributionItem> contributions = new ArrayList<>();
		try {
			BundleProjectCandidates bundleProjectCandidates = Activator.getBundleProjectCandidatesService();
			ArrayList<ContributionItem> classPathContributions = addClassPath(bundleProjectCandidates
					.getBundleProjects());
			if (null != classPathContributions) {
				contributions.addAll(classPathContributions);
			}
		} catch (ExtenderException e) {
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
	 * Creates one contribution for the number of bundles among the specified projects that are
	 * missing the default output folder and one contribution for the number of bundles containing the
	 * default output folder in their manifest
	 * 
	 * @param projects collection of projects to search for default output folder in manifest
	 * @return one contribution for bundles with default output folder in manifest and one for those
	 * missing the default output folder
	 */
	private ArrayList<ContributionItem> addClassPath(Collection<IProject> projects)
			throws ExtenderException {

		ArrayList<ContributionItem> contributions = new ArrayList<>();

		if (projects.size() > 0) {
			int nAdd = 0;
			int nRemove = 0;
			Collection<IProject> errProjects = null;
			for (IProject project : projects) {
				try {
					if (!BuildErrorClosure.hasManifestBuildErrors(project)) {
						if (!Activator.getBundleProjectMetaService().isDefaultOutputFolder(project)) {
							nAdd++;
						} else {
							nRemove++;
						}
					} else {
						if (null == errProjects) {
							errProjects = new ArrayList<IProject>();
						}
						errProjects.add(project);
					}
				} catch (InPlaceException e) {
				}
			}
			if (null != errProjects) {
				MessageOptions messageOptions = Activator.getMessageOptionsService();
				if (messageOptions.isInfoMessages() || messageOptions.isBundleEvents()
						|| messageOptions.isBundleOperations()) {
					String msg = WarnMessage.getInstance().formatString("error_not_update_classpath",
							Activator.getBundleProjectCandidatesService().formatProjectList(errProjects));
					StatusManager.getManager()
							.handle(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, null),
									StatusManager.LOG);
				}
			}
			if (nAdd > 0) {
				String updateLabel = formatLabel(Msg.ADD_CLASSPATH_MAIN_LABEL, nAdd, Boolean.FALSE);
				contributions.add(createContibution(menuIdClasspath, dynamicMainCommandId, updateLabel,
						addClassPathParamId, CommandContributionItem.STYLE_PUSH, classPathImage));
			}
			if (nRemove > 0) {
				String updateLabel = formatLabel(Msg.REMOVE_CLASSPATH_MAIN_LABEL, nRemove, Boolean.FALSE);
				contributions.add(createContibution(menuIdClasspath, dynamicMainCommandId, updateLabel,
						removeClassPathParamId, CommandContributionItem.STYLE_PUSH, classPathImage));
			}
			return contributions;
		}
		return null;
	}
}
