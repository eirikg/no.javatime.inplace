package no.javatime.inplace.builder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.msg.Msg;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.osgi.util.NLS;

/**
 * Add {@code Transition.RENAME} as a pending transition when a JavaTime nature enabled
 * project is refactored.
 * <p>
 * Log a warning to the bundle log if illegal project name
 * <p>
 * The purpose is to distinguish renamed projects from closed and deleted projects in
 * the {@link PreChangeListener}
 *
 */
public class ResourceRenameParticipant extends RenameParticipant {
	
	IProject fromProject = null;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor arg0,
			CheckConditionsContext arg1) throws OperationCanceledException {

		ResourceChangeChecker checker = (ResourceChangeChecker) arg1
				.getChecker(ResourceChangeChecker.class);
		IResourceChangeDescriptionFactory deltaFactory = checker
				.getDeltaFactory();
		IResourceDelta[] affectedProjects = deltaFactory.getDelta()
				.getAffectedChildren();
		return verifyProjectName(affectedProjects);
	}
	/**
	 * Lexical check of new project name. Send a warning to the bundle log if misspelled. 
	 * 
	 * @param affectedProjects list of affected projects (resources) and their children
	 * @return always return an ok status
	 */
	private RefactoringStatus verifyProjectName(IResourceDelta[] affectedProjects) {

		for (IResourceDelta projectDelta : affectedProjects) {
			IPath toProjectFromPath = projectDelta.getMovedFromPath();
			if (null != toProjectFromPath) {
				// Compare the old path of the new renamed delta with the current path  
				if (null != fromProject && toProjectFromPath.equals(fromProject.getFullPath())) {
					IResource toResource = projectDelta.getResource();
					if (toResource instanceof IProject) {
						IProject toProject = toResource.getProject();
						String projectName = toProject.getName();
						Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\\-][a-zA-Z0-9_\\-\\.]*$");
						Matcher matcher = pattern.matcher(projectName);
						if (!matcher.find()) {
							InPlace.get().log(new BundleStatus(StatusCode.WARNING, projectName, 
									NLS.bind(Msg.RENAME_PROJECT_WARN, toProject.getName())));
						}
					}
				}
			}
		}		
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {		
		 return null;
	}

	@Override
	public String getName() {
		return null;
	}

	/**
	 * Add project to refactor as a pending transition
	 */
	@Override
	protected boolean initialize(Object arg0) {
		if (arg0 instanceof IProject) {
			fromProject = (IProject) arg0;
			InPlace.getBundleTransitionService().addPending(fromProject, Transition.RENAME);
			return true;
		}
		return false;
	}
}
