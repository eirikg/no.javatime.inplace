package no.javatime.inplace.ui.command.contributions;

import java.util.ArrayList;
import java.util.Collection;

import no.javatime.inplace.bundleproject.BundleProjectSettings;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.manager.InPlaceException;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.statushandlers.StatusManager;

public class BundleClassPathCommandContribution extends BundleCommandsContributionItems {

	private String menuIdClasspath = "no.javatime.inplace.command.contributions.dynamicitems.main.classpath";
	
	private static String addClassPathLabel = Message.getInstance().formatString("add_classpath_label_main"); //$NON-NLS-1$
	private static String removeClassPathLabel = Message.getInstance().formatString("remove_classpath_label_main"); //$NON-NLS-1$

	@Override
	protected IContributionItem[] getContributionItems() {
		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();

		// Only allow for activated projects
		// ArrayList<ContributionItem> classPathContributions =	addClassPath(activatedProjects);
		// Allow for all plug-in projects		
		ArrayList<ContributionItem> classPathContributions =	addClassPath(ProjectProperties.getPlugInProjects());
		if (null != classPathContributions) {
			contributions.addAll(classPathContributions);
			IContributionItem[] contributionArray = contributions.toArray(new ContributionItem[contributions.size()]);
			return contributionArray;
		}
		return null;
	}
	
	/**
	 * All bundles missing the output folder in classpath
	 * 
	 * @param projects
	 * @return
	 */	
	private ArrayList<ContributionItem> addClassPath(Collection<IProject> projects) {

		ArrayList<ContributionItem> contributions = new ArrayList<ContributionItem>();

		if (projects.size() > 0) {
			int nAdd = 0;
			int nRemove = 0;
			Collection<IProject> errProjects = null;
			for (IProject project : projects) {
				try {
					if (!BuildErrorClosure.hasManifestBuildErrors(project)) {
						if (!BundleProjectSettings.isOutputFolderInBundleClassPath(project)) {
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
				Extension<MessageOptions> msgOpt = new Extension<>(MessageOptions.class);
				MessageOptions optServicet = msgOpt.getService();
				if (null != optServicet && (optServicet.isInfoMessages() || optServicet.isBundleEvents() || optServicet.isBundleOperations())) {
					String msg = WarnMessage.getInstance().formatString("error_not_update_classpath", BundleProjectState.formatProjectList(errProjects));
					StatusManager.getManager().handle(
							new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, msg, null), StatusManager.LOG);						
				}
			}
			if (nAdd > 0) {
				String updateLabel = formatLabel(addClassPathLabel, nAdd, Boolean.FALSE);
				contributions.add(addContribution(menuIdClasspath, dynamicMainCommandId, updateLabel, addClassPathParamId,
						CommandContributionItem.STYLE_PUSH, classPathImage));
			}
			if (nRemove > 0) {
				String updateLabel = formatLabel(removeClassPathLabel, nRemove, Boolean.FALSE);
				contributions.add(addContribution(menuIdClasspath, dynamicMainCommandId, updateLabel, removeClassPathParamId,
						CommandContributionItem.STYLE_PUSH, classPathImage));
			}
			return contributions;
		}
		return null;
	}	
}
