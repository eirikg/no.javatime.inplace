package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;

/**
 * Activates project(s) by adding an internal nature and builder to the project(s). The nature is
 * removed when a bundle is {@link Deactivate deactivated}
 * <p>
 * Project dependency closures are calculated and added as pending projects to this service
 * according to the current dependency option. E.g. not activated projects providing capabilities to
 * an activated project are added to this service for activation.
 * <P>
 * If the option "Update Bundle-ClassPath on Activate/Deactivate" is switched on than the default
 * output folder is updated on the bundle class path if it is missing. If the activation policy
 * option is different from the current setting in the manifest file the Bundle-ActivationPolicy
 * header is updated when the option is set to "lazy" and removed when set to "eager".
 * <p>
 * This service does not alter the state of bundles. When a project is nature enabled the project is
 * per definition activated even if the bundle is not yet.
 * <p>
 * After activation of a project one of the following service operations are triggered on the nature enabled
 * project (this is also true when auto build option is off):
 * <li>If the activated bundle project is in state UNINSTALLED an {@link ActivateBundle} service operation is
 * added for execution.
 * <li>If the activated bundle project is in state INSTALLED an {@link Update} service operation is added for execution.
 */
public interface ActivateProject extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * project operation.
	 */
	public final static String ACTIVATE_PROJECT_SERVICE = "Activate-Project-Service";

	/**
	 * Check if the project is activated
	 * <p>
	 * Activated projects are assigned an internal nature.
	 * 
	 * @param project to check for the activation
	 * @return true if the specified project is activated, and false if the specified project is
	 * closed, non-existing or not activated (nature enabled)
	 * @throws InPlaceException if the specified project is null, open but does not exist or a core
	 * exception is thrown internally (should not be the case for open and existing projects)
	 * @throws ExtenderException if the extender service to access project candidates to activate
	 * could not be obtained
	 */
	public Boolean isProjectActivated(IProject project) throws InPlaceException;

	/**
	 * Check if the workspace is activated.
	 * <p>
	 * The workspace is activated if at least one project is activated. Activated projects are
	 * assigned an internal nature.
	 * 
	 * @return true if one open project in the workspace is activated, and false if no projects are
	 * activated
	 * @throws InPlaceException open projects that does not exist or a core exception when accessing
	 * projects is thrown internally (should not be the case for open and existing projects)
	 * @throws ExtenderException if the extender service to access project candidates to activate
	 * could not be obtained
	 */
	public Boolean isProjectWorkspaceActivated() throws InPlaceException;

	/**
	 * Get all deactivated projects in the workspace region
	 * <p>
	 * This is a convenience method otherwise identical to {@code BundleProjectCandidates.getCandidates()} 
	 * 
	 * @return a list of deactivated projects or an empty set
	 * @throws InPlaceException if the returned project candidate service id null, any open projects
	 * that does not exist or a core exception when accessing projects is thrown internally (should
	 * not be the case for open and existing projects)
	 * @throws ExtenderException if the extender service to access project candidates to activate
	 * could not be obtained
	 */
	public Collection<IProject> getDeactivatedProjects() throws InPlaceException, ExtenderException;

	/**
	 * Get all activated projects in the workspace region
	 * <p>
	 * Activated projects are assigned an internal nature.
	 * 
	 * @return a list of activated projects or an empty set
	 * @throws InPlaceException if the returned project candidate service id null, any open projects
	 * that does not exist or a core exception when accessing projects is thrown internally (should
	 * not be the case for open and existing projects)
	 * @throws ExtenderException if the extender service to access project candidates to activate
	 * could not be obtained
	 */
	public Collection<IProject> getActivatedProjects() throws InPlaceException, ExtenderException;
}