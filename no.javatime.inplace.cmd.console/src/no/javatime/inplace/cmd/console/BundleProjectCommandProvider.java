package no.javatime.inplace.cmd.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import no.javatime.inplace.bundlejobs.ActivateProjectJob;
import no.javatime.inplace.bundlejobs.BundleJob;
import no.javatime.inplace.bundlejobs.DeactivateJob;
import no.javatime.inplace.bundlejobs.NatureJob;
import no.javatime.inplace.bundlejobs.RefreshJob;
import no.javatime.inplace.bundlejobs.ResetJob;
import no.javatime.inplace.bundlejobs.StartJob;
import no.javatime.inplace.bundlejobs.StopJob;
import no.javatime.inplace.bundlejobs.UpdateJob;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.osgi.framework.Bundle;

// Referenced in component.xml
public class BundleProjectCommandProvider implements CommandProvider {

	private Collection<String> cmds = new HashSet<String>(Arrays.asList(
			"activate", "a", "deactivate", "d", "update", "u",
			"start", "sta", "stop", "sto", "refresh", "ref", 
			"reset", "res", "?", "help", "h", "check", "c"));

	public void _ws(CommandInterpreter ci) {
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
				cmd(cmd, ci, new ActivateProjectJob(
						ActivateProjectJob.activateProjectsJobName), "activate");
				break;
			case "deactivate":
			case "d":
				cmd(cmd, ci, new DeactivateJob(DeactivateJob.deactivateJobName),
						"deactivate");
				break;
			case "update":
			case "u":
				cmd(cmd, ci, new UpdateJob(UpdateJob.updateJobName), "update");
				break;
			case "refresh":
			case "ref":
				cmd(cmd, ci, new RefreshJob(RefreshJob.refreshJobName), "refresh");
				break;
			case "reset":
			case "res":
				cmd(cmd, ci, getResetJob(cmd), "reset");		
				break;
			case "start":
			case "sta":
				cmd(cmd, ci, new StartJob(StartJob.startJobName), "start");
				break;
			case "stop":
			case "sto":
				cmd(cmd, ci, new StopJob(StopJob.stopJobName), "stop");
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
		}
	}

	public void cmd(String cmd, CommandInterpreter ci, BundleJob job,
			String fullCmdname) {

		boolean activationMode = cmd.startsWith("a") ? true : false; 
		try {
			Collection<IProject> projects = getProjects(cmd, ci);
			Collection<IProject> discaredProjects = null;
			if (projects.size() > 0) {
				for (IProject project : projects) {
					boolean activated = NatureJob.isNatureEnabled(project);
					if (!activationMode && !activated
							|| activationMode && activated) {
						if (null == discaredProjects) {
							discaredProjects = new LinkedHashSet<>();
						}
						discaredProjects.add(project);
						ci.println(cmd + ": Project " + project.getName()
								+ " already " + (activationMode ? "activated" : "deactivated"));
					}
				}
				if (null != discaredProjects) {
					projects.removeAll(discaredProjects);
				}
				job.addPendingProjects(projects);
				ci.println("Running " + job.getName());
				job.schedule();
				// A command may trigger multiple bundle jobs. Report from all jobs
//				BundleJob waitingJob = job;
//				do {
//					waitingJob = waitAndReportErrors(ci, waitingJob);
//					while (null != waitingJob && waitingJob.getState() == Job.WAITING) {
//						waitingJob = getBundleJob();
//					}
//				} while (null != waitingJob);
			}
		} catch (InPlaceException | IllegalStateException e) {
			ci.println(cmd + ": failed to " + fullCmdname);
			ci.printStackTrace(e);
		}
	}

	private BundleJob waitAndReportErrors(CommandInterpreter ci, BundleJob job) throws InPlaceException, IllegalStateException {

		try {
			IJobManager jobManager = Job.getJobManager();
			// Wait for build and bundle jobs
			jobManager.join(BundleJob.FAMILY_BUNDLE_LIFECYCLE, null);
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
			Collection<IBundleStatus> statusList = job.getErrorStatusList();
			if (statusList.size() > 0) {
				final IBundleStatus multiStatus = job.createMultiStatus(new BundleStatus(
						StatusCode.ERROR, Activator.getContext().getBundle().getSymbolicName(), job.getName() + " terminated with issues" ));
				printStatus(ci, multiStatus);
				ci.println("See the Bundle and/or the Error Log View for further details");
			} else  {
				statusList = job.getLogStatusList();
				if (statusList.size() > 0) {
					final IBundleStatus multiStatus = new BundleStatus(
							StatusCode.INFO, Activator.getContext().getBundle().getSymbolicName(), job.getName() + " log:" );
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
	
	private BundleJob getBundleJob() {

		IJobManager jobMan = Job.getJobManager();
		Job[] jobs = jobMan.find(BundleJob.FAMILY_BUNDLE_LIFECYCLE);
		for (int i = 0; i < jobs.length; i++) {
			Job job = jobs[i];
			if (job instanceof BundleJob) {
				BundleJob bj = (BundleJob) job;
				return bj;
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
					if (!cmdOpt.isAllowUIContributions()
							&& candidates.isUIPlugin(project)) {
						ci.println(cmd
								+ ": "
								+ project.getName()
								+ " contributes to the UI and UI contributions are set as not allowed (this is an option) ");
					} else {
						ci.println(cmd + ": " + project.getName()
								+ " is not a bundle project");
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
			BundleProjectCandidates candidates = Activator
					.getCandidatesService();
			Collection<IProject> projects = candidates.getBundleProjects();
			for (IProject project : projects) {
				IBundleProjectDescription desc = meta
						.getBundleProjectDescription(project);
				String projSymbolicName = desc.getSymbolicName();
				if (null != projSymbolicName
						&& symbolicName.equals(projSymbolicName)) {
					return project;
				}
			}
		} catch (ExtenderException | InPlaceException e) {
			ci.printStackTrace(e);
		}
		return null;
	}

	private BundleJob getResetJob(String cmd) {
		
		return new BundleJob(cmd) {
			@Override
			public IBundleStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					ResetJob job = new ResetJob(getPendingProjects());
					job.reset(ResetJob.resetJobName);						
					return super.runInWorkspace(monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				return getLastErrorStatus();					
			}
		};
	}
	

	public String getHelp() {
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("---InPlace Bundle Activator---\n");
		buffer.append("----Activate and deactivate workspace plug-ins and bundle projects\n");
		buffer.append("----Output from activated and started bundles are directed to standard out if not overridden elsewhere\n");		
		buffer.append("----Mesages from workspace (ws) commands are directed to the Bundle Log View\n");		
		buffer.append("\tws activate | a (<project name> | <symbolic name> | <bundle id>)+ | '*' - activate project(s)\n");
		buffer.append("\tws deactivate | d (<project name> | <symbolic name> | <bundle id>)+ | '*' - deactivate project(s)\n");
		buffer.append("\tws update | u (<project name> | <symbolic name> | <bundle id>)+ | '*' - update project(s)\n");
		buffer.append("\tws refresh | ref (<project name> | <symbolic name> | <bundle id>)+ | '*' - refresh project(s)\n");
		buffer.append("\tws reset | res (<project name> | <symbolic name> | <bundle id>)+ | '*' - reset project(s)\n");
		buffer.append("\tws start | sta (<project name> | <symbolic name> | <bundle id>)+ | '*' - start project(s)\n");
		buffer.append("\tws stop | sto (<project name> | <symbolic name> | <bundle id>)+ | '*' - stop project(s)\n");
		buffer.append("\tws check | c <command> - check if <command> is a legal command\n");
		buffer.append("\te.g.:\n");
		buffer.append("\tws a * - activate all deactivated bundle projects in workspace\n");
		buffer.append("\tws deactivate A - deactivate bundle project with project name and/or symolic name A\n");
		buffer.append("\tws refresh A B 485 - refresh bundle projects with project name and/or symolic name A and B and bundle with bundle id 485\n");
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