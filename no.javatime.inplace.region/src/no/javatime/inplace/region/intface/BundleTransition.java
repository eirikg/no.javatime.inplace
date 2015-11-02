package no.javatime.inplace.region.intface;

import java.util.Collection;
import java.util.EnumSet;

import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

/**
 * Each time one of the bundle commands are executed on a bundle its corresponding transition is
 * registered with the bundle. Future or planned transitions on a bundle may be registered as
 * pending transitions on a bundle. There is no guarantee when or that a pending transition will be
 * executed on a bundle project, but the rule is to execute the transition, or more correct its
 * corresponding bundle command, as soon as possible. Error conditions or other bundle events may
 * cause the execution to be delayed or in some cases be removed.
 * <p>
 * The possible active or current transitions and any pending transitions are specified in
 * {@linkplain Transition}.
 * 
 */
public interface BundleTransition {

	/**
	 * Manifest header for accessing the default service implementation class name of the bundle
	 * region
	 */
	public final static String BUNDLE_TRANSITION_SERVICE = "Bundle-Transition-Service";

	/**
	 * Semantic tagging of bundle projects specifying that a transition is;
	 * <ol>
	 * <li>Active (or current); - The transition is currently executing; and/or
	 * <li>Pending (or planned); - The transition will either be executed some time in the future or
	 * depending on external events removed or replaced by another pending transition
	 * </ol>
	 * <p>
	 * There are two kind of transitions:
	 * <ol>
	 * <li>Bundle operations initiated from the workbench or triggered by a build. A bundle
	 * transitions changes the state of the bundle.
	 * <li>CRUD operations initiated from the workbench or by bundle jobs modifying project meta data
	 * (.project and manifest files). A CRUD transition change the persistent state of a bundle
	 * project
	 * </ol>
	 * Further a transition may be atomic (in the OSGi sense) or composite. Composite transitions
	 * comprises one or more atomic transitions. E.g. {@code Transition.ACTIVATE_BUNDLE} comprises the
	 * necessary atomic transitions to start a bundle depending on the initial state of the bundle.
	 * <p>
	 * A transaction is comprised of one or more transitions and are always executed in sequence by a
	 * bundle job. The number and order of the transitions may vary depending on user options and the
	 * state of the workspace.
	 * 
	 * @see BundleTransition#containsPending(Bundle, Transition, boolean)
	 * @see BundleTransition#containsPending(IProject, Transition, boolean)
	 * @see BundleTransition#getPendingTransitions(IProject)
	 * @see BundleTransition#getTransition(Bundle)
	 * @see BundleTransition#getTransition(IProject)
	 * @see BundleCommand#isStateChanging(IProject)
	 */
	public static enum Transition {

		/**
		 * Active when a bundle is started
		 * <p>
		 * Activated bundles tagged with a pending {@code Transition.START} transition are added to the
		 * set of bundles to start before the start (providing) closure is calculated
		 */
		START,
		/**
		 * Active when a bundle is stopped
		 */
		STOP,
		/**
		 * Active when a bundle is started with lazy policy
		 */
		LAZY_ACTIVATE,
		/**
		 * Pending when a:
		 * <ol>
		 * <li>Bundle is in state {@code BUNDLE.UNINSTALLED} and activated.
		 * <li>Project is moved to another location
		 * </ol>
		 */
		ACTIVATE_BUNDLE,
		/**
		 * Pending when this bundle project should be activated. This occurs usually when a change in
		 * dependencies between bundles occurs. E.g. when a deactivated bundle project is imported by an
		 * activated bundle project and the activated bundle project is resolved
		 */
		ACTIVATE_PROJECT,
		/**
		 * Pending to instruct the bundle to updated as part of the activation process. This occurs when
		 * a deactivated bundle project in state {@code BUNDLE.INSTALLED} is activated in an active
		 * workspace (a workspace is activated if at least one bundle bundle project is activated).
		 */
		UPDATE_ON_ACTIVATE,
		/**
		 * Pending to instruct a deactivating of a bundle project. This occurs usually when a change in
		 * dependencies between bundles occurs. E.g. when a deactivated bundle project is imported by an
		 * activated bundle project and the deactivated bundle has UI contributions enabled when not
		 * allowed (preference setting).
		 */
		DEACTIVATE,
		/**
		 * Active when a bundle is installed.
		 * <p>
		 * Pending when an activated bundle project is imported or opened in an activated workspace and
		 * does not fulfill the requirements to be activated.
		 */
		INSTALL,
		/**
		 * Active when a bundle is uninstalled.
		 * <p>
		 * Pending when an activated bundle project is moved. Moved activated bundle projects are
		 * uninstalled and then activated again after the move operation
		 */
		UNINSTALL,
		/**
		 * Active when a bundle is updated
		 * <p>
		 * Pending when:
		 * <ol>
		 * <li>an activated bundle project needs to be updated after a build
		 * <li>a deactivated bundle project is activated in an activated workspace
		 * </ol>
		 */
		UPDATE,
		/**
		 * Active when a project is building
		 * <p>
		 * Pending when an activated bundle project needs to be built (e.g. modified)
		 */
		BUILD,
		/**
		 * Active when a project is refreshed
		 * <p>
		 * Pending bundles tagged with a pending {@code Transition.REFRESH} transition are added to the
		 * set of bundles to refresh before the refresh (requiring) closure is calculated
		 */
		REFRESH,
		/**
		 * Active when the OSGi resolve operation is called on a bundle. The framework make decisions on
		 * which bundles should be resolved (e.g. when refreshed or resolve is issued). Bundles to
		 * resolve are adjusted in the resolver hook (e.g. deactivated bundles are not resolved) and are
		 * thus not tagged with the resolve transition.
		 * 
		 */
		RESOLVE,
		/**
		 * Initiated by the framework resolver when a requiring closure is executed (e.g. by unistall
		 * and refresh operations). There is no explicit unresolve command in OSGi. This is usually
		 * controlled explicit by handing the complete requiring closure to the resolver when executing
		 * the closure. If one or more bundles are excluded from the closure at resolve time, the
		 * resolver will unresolve the excluded bundles anyway and initiate unresolve events for the
		 * excluded bundles. It may in some situations be necessary to exclude bundles from the closure.
		 * Bundles excluded from the closure set are tagged with a pending unresolve transition to
		 * inform other parties (e.g. the bundle event handler or the resolver hook) that we are aware
		 * of not having a complete or valid closure.
		 */
		UNRESOLVE,
		/**
		 * Used when a bundle is first uninstalled and refreshed and than installed, resolved and
		 * started again. When resetting a bundle all dependent (the partial graph) bundles are included
		 * */
		RESET,
		/**
		 * Set as active when a transition is initiated by the framework (except lazy loading) or from a
		 * third party tool or bundle
		 */
		EXTERNAL, /**
		 * Used when the bundle classpath is updated with the default output folder
		 */
		UPDATE_CLASSPATH,
		/**
		 * Used when the default output folder is removed from the bundle classpath
		 */
		REMOVE_CLASSPATH,
		/**
		 * Used when the activation policy is toggled
		 */
		UPDATE_ACTIVATION_POLICY,
		/**
		 * Updates the class path of the bundle with symbolic name as a framework property
		 */
		UPDATE_DEV_CLASSPATH,
		/**
		 * Close project
		 */
		CLOSE_PROJECT,
		/**
		 * Delete project
		 */
		DELETE_PROJECT,
		/**
		 * Rename of project with the JavaTime nature enabled
		 */
		RENAME_PROJECT,
		/**
		 * Open, import and create new project
		 */
		NEW_PROJECT,
		/**
		 * No transition defined. This is also the case if a transition is canceled or rejected
		 */
		NO_TRANSITION,
	}

	/**
	 * Build and life cycle time errors. Life cycle time errors are divided into modular (all but
	 * start and stop) and service errors (start and stop).
	 * <p>
	 * Some build time errors detected by the build system are also checked independent (before or
	 * after) of builds. Duplicates are not detected by the build system and duplicates of external
	 * bundles are by default not detected by the modular system.
	 * <ol>
	 * <li>Duplicates are not detected by the build system but may be detected at build and modular
	 * (install and update) time. Note that duplicates are both build and modular type errors.
	 * <li>Errors in manifest files may also be checked independent of the build system
	 * <li>The build state of a project is checked independent of the build system
	 * <li>Validation of the description file can be checked independent of the build system
	 * <p>
	 * Cycles are detected by the build system, but can also be detected independent of the build
	 * system
	 * </ol>
	 */
	public static enum TransitionError {

		/** Last known transition was successful without any errors */
		NOERROR,
		/** Framework error returned to the refresh event listener */
		MODULAR_REFRESH_ERROR,
		/** Modular exceptions thrown by install, uninstall, resolve, update, unresolve and refresh */
		MODULAR_EXCEPTION,
		/** Runtime exception thrown by start and stop transitions */
		SERVICE_EXCEPTION,
		/**
		 * Duplicates of workspace bundle projects. Not detected by the build system. Can be detected
		 * after build and before install and update. This is both a build time and a modular errors
		 */
		BUILD_MODULAR_WORKSPACE_DUPLICATE,
		/**
		 * Duplicates between workspace bundles and jar bundles. Not detected by the build system or the
		 * OSGi. Can now be detected after build and before install and update. This is both a build
		 * time and a modular errors
		 */
		BUILD_MODULAR_EXTERNAL_DUPLICATE,
		/**
		 * Circular references are detected by the build system, and should always be detected at build
		 * time
		 */
		BUILD_CYCLE,
		/** This is all build time errors detected by teh build system */
		BUILD,
		/**
		 * If there are build errors in the manifest file. Also detected by
		 * {@code TransitionError.BUILD}
		 */
		BUILD_MANIFEST,
		/**
		 * If the description file is invalid or missing. Also detected by {@code TransitionError.BUILD}
		 */
		BUILD_DESCRIPTION_FILE,
		/**
		 * If build state is missing for a project. Usually occurs if file refresh is needed and for
		 * bundles involved in a cycle
		 */
		BUILD_STATE,
		/**
		 * OSGi state change exception. If trying to execute a bundle command while another is currently
		 * executing
		 */
		SERVICE_STATECHANGE,
		/**
		 * A state indicating that a bundle operation did not complete or did complete, but possibly in
		 * an inconsistent manner. Stopping a bundle operation manually (e.g. endless loop) will raise
		 * an incomplete transition exception
		 */
		SERVICE_INCOMPLETE_TRANSITION,
		/**
		 * If a bundle is unistalled from an external source (e.g. Host OSGi Console)
		 */
		MODULAR_EXTERNAL_UNINSTALL
	}

	/**
	 * Get most recent transition executed on the specified bundle project. The transition may be
	 * executing when the transition is retrieved.
	 * 
	 * @param project the bundle project to get the transition for
	 * @return the transition of the bundle project or {@code Transition.NO_TRANSITION} if no
	 * transition is defined for the specified project
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @see BundleCommand#isStateChanging(IProject)
	 */
	Transition getTransition(IProject project) throws ProjectLocationException;

	/**
	 * Get the textual representation of the transition on the specified bundle project
	 * 
	 * @param project the bundle project to get the transition name for
	 * @return the transition name of the bundle project or the string "NO TRANSITON" if no transition
	 * is defined for the specified project or the location of the bundle project could not be
	 * obtained
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	String getTransitionName(IProject project) throws ProjectLocationException;

	/**
	 * Get the textual representation of the specified transition
	 * 
	 * @param transition the transition to get the name for
	 * @param format Convert name to lower case and replace any underscore characters with blanks
	 * @param caption Convert first character in name to upper case
	 * 
	 * @return the name of the transition or "NO TRANSITION" if the specified transition is null
	 * @see #getTransition(String)
	 */
	String getTransitionName(Transition transition, boolean format, boolean caption);

	/**
	 * Get a transition based on its textual name
	 * 
	 * @return The transition mapping from a textual transition name
	 * @see #getTransitionName(Transition, boolean, boolean)
	 */
	Transition getTransition(String transitionName);

	/**
	 * Place a transition as the current or last transition executed on the specified project. Any
	 * transition error on the project is cleared.
	 * 
	 * @param project bundle project to set the current transition on
	 * @param transition the current transition to set on the bundle
	 * @return the previous transition or null if there is no previous transition or the project does
	 * not exist
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	Transition setTransition(IProject project, Transition transition) throws ProjectLocationException;

	/**
	 * Place a transition as the current or last transition executed on the specified bundle. Any
	 * transition error on the bundle is cleared.
	 * 
	 * @param bundle bundle to set the current transition on
	 * @param transition the current transition to set on the bundle
	 * @return the previous transition or null if there is no previous transition or the bundle does
	 * not exist
	 */
	Transition setTransition(Bundle bundle, Transition transition);

	/**
	 * Get the current or the most recent transition for the specified bundle
	 * 
	 * @param bundle the bundle to obtain the current transition from
	 * @return the current or most recent transition. If no transition is defined return null.
	 * @see BundleCommand#isStateChanging(IProject)
	 */
	Transition getTransition(Bundle bundle);

	public IBundleStatus getTransitionStatus(IProject project);

	public TransitionError getTransitionError(IProject project);

	public void setBuildStatus(IProject project, TransitionError transitionError, IBundleStatus status);

	public void setBundleStatus(IProject project, TransitionError transitionError,
			IBundleStatus status);

	/**
	 * Mark the current transition of the specified bundle project with
	 * {@code TransitionError#MODULAR_REFRESH_ERROR}. Convenience method for
	 * {@code setTransitionError(project, TransitionError.MODULAR_REFRESH_ERROR)}.
	 * 
	 * @param project bundle project with a transition to be marked as erroneous.
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @see #setBuildTransitionError(IProject, TransitionError)
	 */

	/**
	 * Mark the current transition of the specified bundle project with the specified transition error
	 * type.
	 * 
	 * @param project bundle project with a transition to be marked as erroneous.
	 * @param error one of the {@code TransitionError} types
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */

	/**
	 * Mark the current transition of the specified bundle project with
	 * {@code TransitionError#MODULAR_REFRESH_ERROR}. Convenience method for
	 * {@code setTransitionError(bundle, TransitionError.MODULAR_REFRESH_ERROR)}.
	 * 
	 * @param bundle bundle project with a transition to be marked as erroneous.
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 * @see #setBuildTransitionError(Bundle, TransitionError)
	 */

	/**
	 * Mark the current transition of the specified bundle project with the specified transition error
	 * type.
	 * 
	 * @param bundle bundle project with a transition to be marked as erroneous.
	 * @param error one of the {@code TransitionError} types
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 */

	/**
	 * Check if the current transition of the specified bundle project is erroneous.
	 * 
	 * @param project the bundle project containing the transition to check for error
	 * @return true if the current transition of the specified bundle project is erroneous or false if
	 * not.
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */

	/**
	 * Check if the current transition of the specified bundle project is erroneous.
	 * 
	 * @param bundle the bundle project containing the transition to check for error
	 * @return true if the current transition of the specified bundle project is erroneous or false if
	 * not.
	 */

	/**
	 * Check if the specified transition error exist among at least one of the workspace bundle
	 * projects
	 * 
	 * @param transitionError the transition error to check against
	 * @return true if the specified transition error exist among at least one of the bundle projects
	 * in the workspace
	 */
	boolean hasBuildTransitionError(TransitionError transitionError);

	/**
	 * Get the error associated with bundle project or {@code TransitionError#NOERROR} if no error
	 * 
	 * @param project the bundle project containing the transition error
	 * @return one of the {@code TransitionError} types or {@code TransitionError#NOERROR}
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	TransitionError getBuildTransitionError(IProject project) throws ProjectLocationException;

	/**
	 * Get the bundle error associated with bundle project or {@code TransitionError#NOERROR} if no
	 * error
	 * 
	 * @param project the bundle project containing the transition error
	 * @return one of the {@code TransitionError} types or {@code TransitionError#NOERROR}
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	TransitionError getBundleTransitionError(IProject project) throws ProjectLocationException;

	/**
	 * Get the error associated with bundle project or {@code TransitionError#NOERROR} if no error
	 * 
	 * @param bundle the bundle project containing the transition error
	 * @return one of the {@code TransitionError} types or {@code TransitionError#NOERROR}
	 */
	TransitionError getBuildTransitionError(Bundle bundle);

	/**
	 * Clear the error flag of the current transition of the specified bundle project.
	 * 
	 * @param project the bundle project containing the transition to clear the error flag from
	 * @return true if the error flag is cleared in current transition of the specified bundle.
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	boolean clearBuildTransitionError(IProject project) throws ProjectLocationException;

	/**
	 * Clear the error flag of the current transition of the specified bundle project.
	 * 
	 * @param project the bundle project containing the transition to clear the error flag from
	 * @return true if the error flag is cleared in current transition of the specified bundle.
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	boolean clearBundleTransitionError(IProject project) throws ProjectLocationException;
	/**
	 * Get all projects among the specified projects that contains the specified pending transition
	 * 
	 * @param projects bundle projects to check for the specified transition
	 * @param transition transition to check for in the specified projects
	 * @return all projects among the specified projects containing the specified transition or an
	 * empty collection
	 */
	public Collection<IProject> getPendingProjects(Collection<IProject> projects,
			Transition transition);

	/**
	 * Get all bundles among the specified bundles that contains the specified transition
	 * 
	 * @param bundles bundle projects to check for the specified transition
	 * @param transition transition to check for in the specified projects
	 * @return all bundles among the specified bundles containing the specified transition or an empty
	 * collection
	 */
	public Collection<Bundle> getPendingBundles(Collection<Bundle> bundles,
			BundleTransition.Transition command);

	/**
	 * Get all pending transition for a bundle project
	 * 
	 * @param project bundle project to get pending transitions for
	 * @return all pending transitions for this bundle project or null if no pending transitions
	 * exists
	 */
	public EnumSet<Transition> getPendingTransitions(IProject project);

	/**
	 * Check if the bundle project has the specified pending transition attached to it.This is the
	 * same as using {@linkplain #containsPending(Bundle, Transition, boolean)}
	 * 
	 * @param project bundle project to check for the specified pending transition
	 * @param transition associated with this bundle project
	 * @param remove clear the transition from the bundle project if true
	 * @return true if this transition is attached to this bundle project as a pending transition or
	 * false if not.
	 */
	public boolean containsPending(IProject project, Transition transition, boolean remove);

	/**
	 * Check if one if the specified bundle projects have the specified pending transition attached to
	 * it
	 * 
	 * @param bundles bundle projects to check for the specified pending transition
	 * @param transition associated with this bundle project
	 * @param remove clear the transition from the bundle project if true
	 * @return true if this transition is attached to one or more bundle project as a pending
	 * transition or false if not.
	 */
	public boolean containsPending(Collection<Bundle> bundles, Transition operation, boolean remove);

	/**
	 * Check if the bundle project has the specified pending transition attached to it. This is same
	 * as as using {@linkplain #containsPending(IProject, Transition, boolean)}
	 * 
	 * @param bundle bundle project to check for the specified pending transition
	 * @param transition associated with this bundle project
	 * @param remove clear the transition from the bundle project if true
	 * @return true if this transition is attached to this bundle project as a pending transition or
	 * false if not. False is also returned if the specified bundle is null.
	 */
	public boolean containsPending(Bundle bundle, Transition transition, boolean remove);

	/**
	 * Check if there are any bundle projects with the specified pending transition
	 * 
	 * @param transition pending transition to check for
	 * @return true if the specified transition is attached to any bundle project as a pending
	 * transition or false if not
	 */
	public boolean containsPending(Transition transition);

	/**
	 * Add a pending transition to the bundle project. This is the same as using
	 * {@linkplain #addPending(Bundle, Transition)}
	 * 
	 * @param project bundle project to add the pending transition to
	 * @param transition to register with this bundle project
	 */
	public void addPending(IProject project, Transition transition);

	/**
	 * Add a pending bundle operation to the specified bundle projects
	 * 
	 * @param project bundle projects to add the pending operation to
	 * @param operation to register with the bundle projects
	 */
	void addPendingCommand(Collection<IProject> projects, Transition operation);

	/**
	 * Add a pending transition to the bundle project. This is the same as using
	 * {@linkplain #addPending(IProject, Transition)}
	 * 
	 * @param bundle bundle project to add the pending transition to
	 * @param transition to register with this bundle project
	 */
	public void addPending(Bundle bundle, Transition transition);

	/**
	 * Remove the specified pending transition from this bundle project. This is the same as using
	 * {@linkplain #removePending(Bundle, Transition)}
	 * 
	 * @param project bundle project to remove this transition from
	 * @param transition to remove from this bundle project
	 * @return false if remove fails for the specified project
	 */
	public boolean removePending(IProject project, Transition transition);

	/**
	 * Remove the specified pending transition from this bundle project. This is the same as using
	 * {@linkplain #removePending(IProject, Transition)}
	 * 
	 * @param bundle bundle project to remove this transition from
	 * @param transition to remove from this bundle project
	 * @return false if remove fails for the specified bundle
	 */
	public boolean removePending(Bundle bundle, Transition transition);

	/**
	 * Remove the specified pending transition from the collection of bundle projects.
	 * 
	 * @param projects bundle projects to remove this transition from
	 * @param operation
	 * @return false if one of the removals fails among the specified projects
	 */
	public boolean removePending(Collection<IProject> projects, Transition operation);
}