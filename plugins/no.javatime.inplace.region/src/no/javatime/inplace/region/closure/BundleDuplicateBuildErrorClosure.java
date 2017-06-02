package no.javatime.inplace.region.closure;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;

/**
 * Detects and reports bundle errors for single projects and build error closures based on an
 * initial set of bundle projects.
 * 
 * A bundle build error closure is a subset of a {@link ProjectBuildErrorClosure build errors}
 * closure. The subset is determined by the type of build errors in projects
 * <p>
 * Bundle errors include errors in projects with no build state, build errors in project manifest
 * files, missing project descriptions and workspace bundles that are duplicates of external bundles
 * 
 * @see ProjectBuildErrorClosure
 * @see BundleProjectBuildError
 */
public class BundleDuplicateBuildErrorClosure extends BundleBuildErrorClosure {

	/**
	 * Calculate the bundle error closures for the specified projects where the bundle error closure
	 * is determined by type of closure, activation level and activation scope.
	 * 
	 * @param initialProjects set of projects to calculate the error closures from
	 * @param transition type of transition the error closure is used for. The transition type is only
	 * used when constructing the default warning message of the detected error closures
	 * @param closure type of closure is providing ({@code Closure.PROVIDING}) or requiring (
	 * {@code Closure.REQUIRING}) The providing/requiring closure is interpreted as bundle projects
	 * providing/requiring capabilities to/from an initial project.
	 */
	public BundleDuplicateBuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure) {
		super(initialProjects, transition, closure);
	}

	/**
	 * Calculate the bundle error closures for the specified projects where the bundle error closure
	 * is determined by type of closure, activation level and activation scope
	 * <p>
	 * The closure defaults to requiring ({@code Closure.REQUIRING}) which means that the requiring
	 * projects of the specified projects are included in the closures.
	 * <p>
	 * The activation level defaults to {@code Bundle.INSTALLED} which means that all installed,
	 * resolved and active requiring or providing (depending on the specified closure type) bundles
	 * are included in the calculated closures.
	 * <p>
	 * The activation scope defaults to activated bundles which means that only activated bundles are
	 * considered when calculating the closures.
	 * 
	 * @param initialProjects set of projects to calculate the error closures from
	 * @param transition type of transition the error closure is used for. The transition type is only
	 * used when constructing the default warning message of the detected error closures
	 * @param closure type of closure is providing ({@code Closure.PROVIDING}) or requiring (
	 * {@code Closure.REQUIRING}) The providing/requiring project closure is interpreted as bundle
	 * projects providing/requiring capabilities to/from an initial project.
	 * @param activationLevel specifies that all projects ({@code Bundle#UNINSTALLED}), installed
	 * resolved and activated bundles ({@code Bundle#INSTALLED}) or only resolved and activated (
	 * {@code Bundle#RESOLVED}) bundles should be used when calculating the error closure.
	 * @param activationScope is deactivated ({@code ActivationLevel.DEACTIVATED}), activated (
	 * {@code ActivationLevel.ACTIVATED}) or all ({@code ActivationLevel.ALL}) projects
	 */
	public BundleDuplicateBuildErrorClosure(Collection<IProject> initialProjects, Transition transition,
			Closure closure, int activationLevel, ActivationScope scope) {
		super(initialProjects, transition, closure, activationLevel, scope);
	}

	/**
	 * All projects with bundle errors that are member of the closure constructed for the initial
	 * (starter) set of projects.
	 * <p>
	 * Bundle errors is a subset of build errors, where this override makes it possible to only detect
	 * bundle errors. This is only the case if the "Activate on build errors" option is on. If the
	 * option is off this is the same as detecting all build errors.
	 * 
	 * @return projects with bundle errors among the closures of the initial (starter) set of
	 * projects.
	 * @throws InPlaceException if a project is null, does not exist, is closed
	 * @throws ExtenderException If failing to obtain the command options service
	 * @see #hasBuildErrors(boolean)
	 */
	@Override
	public Collection<IProject> getBuildErrors(boolean includeDuplicates) throws ExtenderException, InPlaceException {

		if (Activator.getCommandOptionsService().isActivateOnCompileError()) {
			try {
				if (null == errorProjects) {
					errorProjects = BundleProjectBuildError.getBundleErrors(getProjectClosures(), false);
				}
			} catch (CircularReferenceException e) {
				if (null == errorProjects) {
					errorProjects = new LinkedHashSet<>();
				}
				Collection<IProject> cycles = e.getProjects();
				errorProjects.addAll(cycles);
			}
			return errorProjects;
		} else {
			return super.getBuildErrors(includeDuplicates);
		}
	}
}
