package no.javatime.inplace.cmd.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import no.javatime.inplace.bundlejobs.intface.ActivateProject;
import no.javatime.inplace.bundlejobs.intface.BundleExecutor;
import no.javatime.inplace.bundlejobs.intface.Deactivate;
import no.javatime.inplace.bundlejobs.intface.Refresh;
import no.javatime.inplace.bundlejobs.intface.Reset;
import no.javatime.inplace.bundlejobs.intface.Start;
import no.javatime.inplace.bundlejobs.intface.Stop;
import no.javatime.inplace.bundlejobs.intface.Update;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.region.intface.BundleProjectCandidates;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.BundleRegion;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.osgi.framework.Bundle;

public class BundleProjectCommandProvider implements CommandProvider {

	private Collection<String> cmds = new HashSet<String>(Arrays.asList("activate", "a",
			"deactivate", "d", "update", "u", "start", "sta", "stop", "sto", "refresh", "ref", "reset",
			"res", "?", "help", "h", "check", "c"));

	/**
	 * Get an extension returned based on ranking order
	 * 
	 * @param serviceInterfaceName the interface service name used to locate the extension
	 * @return an extension located by the specified service interface name
	 * @throws ExtenderException if failed to get the extender or the extension is null
	 */
	public static <S> Extension<S> getExtension(String serviceInterfaceName) throws ExtenderException {

		Extension<S> extension = Extenders.getExtension(serviceInterfaceName, Activator.getContext().getBundle());
		if (null == extension) {
			throw new ExtenderException(NLS.bind("Null extender service in bundle {0}",  Activator.getContext().getBundle()));
		}
		return extension;
	}

	public void _ws(CommandInterpreter ci) {

		Extension<?> extension = null;

		try {
			String cmd = ci.nextArgument();
			if (cmd == null) {
				ci.println("Missing command");
				return;
			}
			if (!cmds.contains(cmd)) {
				ci.println(cmd + ": unknown command");
				return;
			}
			switch (cmd) {
			case "activate":
			case "a":
				extension = getExtension(ActivateProject.class.getName());
				cmd(cmd, ci, (ActivateProject) extension.getTrackedService(), "activate");
				break;
			case "deactivate":
			case "d":
				extension = getExtension(Deactivate.class.getName());
				cmd(cmd, ci, (Deactivate) extension.getTrackedService(), "deactivate");
				break;
			case "update":
			case "u":
				extension = getExtension(Update.class.getName());
				cmd(cmd, ci, (Update) extension.getTrackedService(), "update");
				break;
			case "refresh":
			case "ref":
				extension = getExtension(Refresh.class.getName());
				cmd(cmd, ci, (Refresh) extension.getTrackedService(), "refresh");
				break;
			case "reset":
			case "res":
				extension = getExtension(Reset.class.getName());
				cmd(cmd, ci, (Reset) extension.getTrackedService(), "reset");
				break;
			case "start":
			case "sta":
				extension = getExtension(Start.class.getName());
				cmd(cmd, ci, (Start) extension.getTrackedService(), "start");
				break;
			case "stop":
			case "sto":
				extension = getExtension(Stop.class.getName());
				cmd(cmd, ci, (Stop) extension.getTrackedService(), "stop");
				break;
			case "check":
			case "c":
				checkCommand(ci);
				break;
			case "?":
			case "help":
			default:
				ci.println(getHelp());
				break;
			}
		} finally {
			if (null != extension) {
				extension.closeTrackedService();
			}
		}
	}

	public void cmd(String cmd, CommandInterpreter ci, BundleExecutor executor, String fullCmdname) {

		boolean activationMode = cmd.startsWith("a") ? true : false;
		Extension<ActivateProject> activateExtension = null;
		try {
			Collection<IProject> projects = getProjects(cmd, ci);
			Collection<IProject> discaredProjects = null;
			if (projects.size() > 0) {
				activateExtension = Extenders.getExtension(
						ActivateProject.class.getName(), Activator.getContext().getBundle());
				ActivateProject activate = activateExtension.getTrackedService();
				for (IProject project : projects) {
					boolean activated = activate.isProjectActivated(project);
					if (!activationMode && !activated || activationMode && activated) {
						if (null == discaredProjects) {
							discaredProjects = new LinkedHashSet<>();
						}
						discaredProjects.add(project);
						ci.println(cmd + ": Project " + project.getName() + " already "
								+ (activationMode ? "activated" : "deactivated"));
					}
				}
				if (null != discaredProjects) {
					projects.removeAll(discaredProjects);
				}
				executor.addPendingProjects(projects);
				ci.println("Running " + executor.getName());
				executor.run();
				// A command may trigger multiple bundle jobs. Report from all jobs
				// BundleJob waitingJob = job;
				// do {
				// waitingJob = waitAndReportErrors(ci, waitingJob);
				// while (null != waitingJob && waitingJob.getState() == Job.WAITING) {
				// waitingJob = getBundleJob();
				// }
				// } while (null != waitingJob);
			}
		} catch (InPlaceException | IllegalStateException e) {
			ci.println(cmd + ": failed to " + fullCmdname);
			ci.printStackTrace(e);
		} finally {
			if (null != activateExtension) {
				activateExtension.closeTrackedService();
			}
		}
	}

	@SuppressWarnings("unused")
	private BundleExecutor waitAndReportErrors(CommandInterpreter ci, BundleExecutor job)
			throws InPlaceException, IllegalStateException {

		try {
			IJobManager jobManager = Job.getJobManager();
			// Wait for build and bundle jobs
			jobManager.join(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE, null);
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
			Collection<IBundleStatus> statusList = job.getErrorStatusList();
			if (statusList.size() > 0) {
				IBundleStatus multiStatus = new BundleStatus(StatusCode.ERROR,
						Activator.getContext().getBundle().getSymbolicName(), job.getName()
						+ " terminated with issues");
				for (IBundleStatus status : job.getErrorStatusList()) {
					multiStatus.add(status);
				}
				printStatus(ci, multiStatus);
				ci.println("See the Bundle and/or the Error Log View for further details");
			} else {
				statusList = job.getLogStatusList();
				if (statusList.size() > 0) {
					final IBundleStatus multiStatus = new BundleStatus(StatusCode.INFO, Activator
							.getContext().getBundle().getSymbolicName(), job.getName() + " log:");
					multiStatus.add(statusList);
					printStatus(ci, multiStatus);
				}
			}
		} catch (Exception e) {
			ci.printStackTrace(e);
		}
		return getBundleJob();
	}

	/**
	 * Print status and sub status objects to OSGi console
	 * 
	 * @param status status object to print
	 */
	private void printStatus(CommandInterpreter ci, IStatus status) {
		String msg = status.getMessage();
		if (null != msg) {
			ci.println(status.getMessage());
		}
		Collection<String> msgList = getChaindedExceptionMessages(status.getException());
		for (String causeMsg : msgList) {
			if (!msg.equals(causeMsg)) {
				ci.println(causeMsg);
			}
		}
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			printStatus(ci, children[i]);
		}
	}

	public List<String> getChaindedExceptionMessages(Throwable e) {
		List<String> tMsgs = new ArrayList<String>();
		if (null != e && null != e.getLocalizedMessage()) {
			tMsgs.add(e.getLocalizedMessage());
			Throwable t = e.getCause();
			while (null != t) {
				if (null != t.getLocalizedMessage()) {
					tMsgs.add(t.getLocalizedMessage());
				}
				t = t.getCause();
			}
		}
		return tMsgs;
	}

	private BundleExecutor getBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job.belongsTo(BundleExecutor.FAMILY_BUNDLE_LIFECYCLE)) {
				return (BundleExecutor) job;
			}
		}
		return null;
	}

	public Collection<IProject> getProjects(String cmd, CommandInterpreter ci) {

		Collection<IProject> projects = new LinkedHashSet<>();
		String bpArg = ci.nextArgument();
		if (null == bpArg) {
			ci.println(cmd + ": missing project name, symbolic name or bundle id");
			return projects;
		}
		BundleProjectCandidates candidates = Activator.getCandidatesService();
		BundleRegion bundleRegion = Activator.getRegionService();
		if (bpArg.equals("*")) {
			if (cmd.startsWith("a")) {
				return candidates.getCandidates();
			} else {
				return bundleRegion.getActivatedProjects();
			}
		}
		while (bpArg != null) {
			try {
				IProject project = null;
				// Bundle id
				try {
					Long bundleId = Long.parseLong(bpArg);
					Bundle bundle = bundleRegion.getBundle(bundleId);
					project = bundleRegion.getProject(bundle);
				} catch (NumberFormatException e) {
				}
				// Symbolic name
				if (null == project) {
					project = getProject(ci, bpArg);
				}
				// Project name
				if (null == project) {
					project = candidates.getProject(bpArg);
				}
				IJavaProject javaProject = JavaCore.create(project);
				if (null == javaProject) {
					ci.println(cmd + ": failed to access project " + bpArg);
					bpArg = ci.nextArgument();
					continue;
				}
				if (!candidates.isInstallable(project)) {
					CommandOptions cmdOpt = Activator.getCmdOptionsService();
					// If the "Allow UI Contributions" option is on
					if (!cmdOpt.isAllowUIContributions() && candidates.isUIPlugin(project)) {
						ci.println(cmd
								+ ": "
								+ project.getName()
								+ " contributes to the UI and UI contributions are set as not allowed (this is an option) ");
					} else {
						ci.println(cmd + ": " + project.getName() + " is not a bundle project");
					}
				} else {
					if (cmd.startsWith("u")) {
						// Force an update even if the "Update on build" option is off
						BundleTransition transition = Activator.getTransitionService();
						transition.addPending(project, Transition.UPDATE);
					}
					projects.add(project);
				}
			} catch (ExtenderException | InPlaceException e) {
				ci.printStackTrace(e);
			}
			bpArg = ci.nextArgument();
		}
		return projects;
	}

	public IProject getProject(CommandInterpreter ci, String symbolicName) {
		if (null == symbolicName) {
			return null;
		}
		try {
			BundleProjectMeta meta = Activator.getMetaService();
			BundleProjectCandidates candidates = Activator.getCandidatesService();
			Collection<IProject> projects = candidates.getBundleProjects();
			for (IProject project : projects) {
				IBundleProjectDescription desc = meta.getBundleProjectDescription(project);
				String projSymbolicName = desc.getSymbolicName();
				if (null != projSymbolicName && symbolicName.equals(projSymbolicName)) {
					return project;
				}
			}
		} catch (ExtenderException | InPlaceException e) {
			ci.printStackTrace(e);
		}
		return null;
	}

	public String getHelp() {

		StringBuffer buffer = new StringBuffer();
		buffer.append("---InPlace Bundle Activator---\n");
		buffer.append("----Activate and deactivate workspace plug-ins and bundle projects\n");
		buffer
				.append("----Output from activated and started bundles are directed to standard out if not overridden elsewhere\n");
		buffer.append("----Mesages from workspace (ws) commands are directed to the Bundle Log View\n");
		buffer
				.append("\tws activate | a (<project name> | <symbolic name> | <bundle id>)+ | '*' - activate project(s)\n");
		buffer
				.append("\tws deactivate | d (<project name> | <symbolic name> | <bundle id>)+ | '*' - deactivate project(s)\n");
		buffer
				.append("\tws update | u (<project name> | <symbolic name> | <bundle id>)+ | '*' - update project(s)\n");
		buffer
				.append("\tws refresh | ref (<project name> | <symbolic name> | <bundle id>)+ | '*' - refresh project(s)\n");
		buffer
				.append("\tws reset | res (<project name> | <symbolic name> | <bundle id>)+ | '*' - reset project(s)\n");
		buffer
				.append("\tws start | sta (<project name> | <symbolic name> | <bundle id>)+ | '*' - start project(s)\n");
		buffer
				.append("\tws stop | sto (<project name> | <symbolic name> | <bundle id>)+ | '*' - stop project(s)\n");
		buffer.append("\tws check | c <command> - check if <command> is a legal command\n");
		buffer.append("\te.g.:\n");
		buffer.append("\tws a * - activate all deactivated bundle projects in workspace\n");
		buffer
				.append("\tws deactivate A - deactivate bundle project with project name and/or symolic name A\n");
		buffer
				.append("\tws refresh A B 485 - refresh bundle projects with project name and/or symolic name A and B and bundle with bundle id 485\n");
		return buffer.toString();
	}

	public boolean checkCommand(CommandInterpreter ci) {

		String command = ci.nextArgument();
		if (cmds.contains(command)) {
			ci.println(command + " is a legal command");
			return true;
		} else {
			ci.println(command + " is not a legal command");
			return false;
		}
	}
}