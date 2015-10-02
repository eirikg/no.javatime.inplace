/**
 * 
 */
package no.javatime.inplace.region.closure;

import java.util.Collection;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;

/**
 * A bundle error closure is a subset of a build error closure. The subset is determined the type of
 * build errors in projects
 * <p>
 * Bundle errors include projects without a build state, build errors in manifest files or 
 * bundle project duplicates (same symbolic name end version)
 * @see ProjectBuildErrorClosure
 * @see BundleProjectBuildError
 */
public class BundleBuildErrorClosure extends ProjectBuildErrorClosure {

	public BundleBuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure) {
		super(initialProjects, transition, closure);
	}

	public BundleBuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure, int activationLevel, ActivationScope scope) {
		super(initialProjects, transition, closure, activationLevel, scope);
	}

	@Override
	public Collection<IProject> getBuildErrors() throws CircularReferenceException, InPlaceException {
		if (Activator.getCommandOptionsService().isActivateOnCompileError()) {
			if (null == errorProjects) {
				errorProjects = BundleProjectBuildError.getBundleErrors(getProjectClosures());
			}
			return errorProjects;
		} else {
			return super.getBuildErrors();
		}
	}

//	public static IBundleStatus getErrorsStatus(Collection<IProject> projects) throws
//	ExtenderException, InPlaceException, CircularReferenceException {
//
//		BundleProjectCandidates bundleProjectCandidates =
//				Activator.getBundleProjectCandidatesService(Activator.getContext().getBundle());
//		Collection<IProject> errorProjects =
//				BundleProjectBuildError.getBuildErrors((bundleProjectCandidates.getBundleProjects())); 
//		IBundleStatus	multiStatus = new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID,
//				Msg.FATAL_ACTIVATE_ERROR); 
//		for (IProject project : errorProjects) { 
//			if (!BundleProjectBuildError.hasBuildState(project)) { 
//				multiStatus.add(new BundleStatus(StatusCode.WARNING, Activator.PLUGIN_ID, project, NLS .bind(Msg.BUILD_STATE_ERROR,
//						project.getName()), null)); 
//			} else if (BundleProjectBuildError.hasManifestBuildErrors(project)) {
//				multiStatus.add(new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, project, NLS.bind(
//						Msg.MANIFEST_BUILD_ERROR, project.getName()), null)); 
//			} 
//		} 
//		if (multiStatus.getChildren().length > 0) {
//			return multiStatus; 
//		} 
//		return new BundleStatus(StatusCode.OK, Activator.PLUGIN_ID, ""); 
//	}
}
