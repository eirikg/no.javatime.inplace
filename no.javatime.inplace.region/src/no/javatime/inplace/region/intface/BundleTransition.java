package no.javatime.inplace.region.intface;

import java.util.Collection;
import java.util.EnumSet;

import no.javatime.inplace.region.intface.BundleTransition.Transition;

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
	 * Semantic tagging of bundles specifying that a transition is active (or current) or is pending
	 * (planned) on a bundle. A bundle is tagged right before a simple or composite OSGi command is
	 * executed (active), when changing the characteristics of a bundle (e.g. toggling activation
	 * policy or altering the bundle class path) and when a transition is scheduled for execution some
	 * time in the future (pending transitions).
	 * <p>
	 * Active transitions can be accessed using the {@linkplain BundleCommand#getTransition(IProject)}
	 * . Accessing scheduled transitions can among others be obtained by using the
	 * {@linkplain BundleCommand#containsPending(Bundle, Transition, boolean)} and
	 * {@linkplain BundleCommand#containsPending(IProject, Transition, boolean)}
	 */
	public static enum Transition {
		/**
		 * Active when a starting a bundle
		 */
		START,
		/**
		 * Active when stopping a bundle
		 */
		STOP,
		/**
		 * Set as active when a bundle is started with lazy policy
		 */
		LAZY_ACTIVATE,
		/**
		 * Used when activating (install, resolve and/or refresh and optionally starting) a bundle
		 */
		ACTIVATE_BUNDLE,
		/**
		 * Used when activating (enables a nature) a project
		 */
		ACTIVATE_PROJECT,
		/**
		 * Use together with {@code Transition.ACTIVATE_BUNDLE}. If a deactivated bundle in state
		 * {@code BUNDLE.INSTALLED} is activated in an active workspace (a workspace is activated if at
		 * least one bundle bundle project is activated) the bundle is updated as part of the activation
		 * process. When auto build is switched off this transition force an update of the bundle even
		 * if it has not been built yet.
		 */
		UPDATE_ON_ACTIVATE,
		/**
		 * Used when deactivating a bundle. In an activated workspace the bundle is stopped and
		 * unresolved (refresh with no resolve) with {@code BUNDLE.INSTALLED} as the terminal state. If
		 * the bundle is among the last set of bundles to deactivate the workspace is also deactivated
		 * and the bundle is stopped, uninstalled and refreshed. The nature (see
		 * {@link Transition#ACTIVATE_PROJECT} is always removed when deactivating a bundle.
		 */
		DEACTIVATE,
		/**
		 * Active when installing a bundle
		 */
		INSTALL,
		/**
		 * Active when installing a bundle
		 */
		UNINSTALL,
		/**
		 * Active when updating a bundle
		 */
		UPDATE,
		/**
		 * Active when a project is saved but not built yet
		 */
		BUILD,
		/**
		 * Active when a bundle is refreshed
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
		 * controlled explicit by handing the complete requiring closure to the resolver when
		 * executing the closure. If one or more bundles are excluded from the closure at
		 * resolve time, the resolver will unresolve the excluded bundles anyway and initiate unresolve
		 * events for the excluded bundles. It may in some situations be necessary to exclude bundles
		 * from the closure. Bundles excluded from the closure set are tagged with a pending unresolve
		 * transition to inform other parties (e.g. the bundle event handler or the resolver hook) that
		 * we are aware of not having a complete or valid closure.
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
		 * Rename of project with the JavaTime nature enabled
		 */
		RENAME,
		/**
		 * No transition defined. This is also the case if a transition is canceled or rejected
		 */
		NOTRANSITION,
	}

	public static enum TransitionError {
		NOERROR, ERROR, EXCEPTION, DUPLICATE, CYCLE, BUILD, DEPENDENCY, STATECHANGE,
		/**
		 * A state indicating that a bundle command/operation did not complete or did complete, but
		 * possibly in an inconsistent manner. May for instance happen when executing an infinite loop
		 * in Start/Stop methods Never ending or operations that timeout will have an incomplete
		 * transition error and the state will be the state the bundle had when the previous transition
		 * ended.
		 */
		INCOMPLETE, UNINSTALL
	}

	/**
	 * Get most recent transition executed on the specified bundle project. The transition may be
	 * executing when the transition is retrieved.
	 * 
	 * @param project the bundle project to get the transition for
	 * @return the transition of the bundle project or {@code Transition.NOTRANSITION} if no
	 * transition is defined for the specified project
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
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
	 * @param format TODO
	 * @param caption TODO
	 * @return the name of the transition or "NO TRANSITION" if the specified transition is null
	 */
	String getTransitionName(Transition transition, boolean format, boolean caption);

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
	 */
	Transition getTransition(Bundle bundle);

	/**
	 * Mark the current transition of the specified bundle project with {@code TransitionError#ERROR}.
	 * Convenience method for {@code setTransitionError(project, TransitionError.ERROR)}.
	 * 
	 * @param project bundle project with a transition to be marked as erroneous.
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @see #setTransitionError(IProject, TransitionError)
	 */
	boolean setTransitionError(IProject project) throws ProjectLocationException;

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
	boolean setTransitionError(IProject project, TransitionError error)
			throws ProjectLocationException;

	/**
	 * Mark the current transition of the specified bundle project with {@code TransitionError#ERROR}.
	 * Convenience method for {@code setTransitionError(bundle, TransitionError.ERROR)}.
	 * 
	 * @param bundle bundle project with a transition to be marked as erroneous.
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 * @see #setTransitionError(Bundle, TransitionError)
	 */
	boolean setTransitionError(Bundle bundle);

	/**
	 * Mark the current transition of the specified bundle project with the specified transition error
	 * type.
	 * 
	 * @param bundle bundle project with a transition to be marked as erroneous.
	 * @param error one of the {@code TransitionError} types
	 * @return true if the error was set on transition for the specified bundle project, otherwise
	 * false
	 */
	boolean setTransitionError(Bundle bundle, TransitionError error);

	/**
	 * Check if the current transition of the specified bundle project is erroneous.
	 * 
	 * @param project the bundle project containing the transition to check for error
	 * @return true if the current transition of the specified bundle project is erroneous or false if
	 * not.
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	boolean hasTransitionError(IProject project) throws ProjectLocationException;

	/**
	 * Check if the current transition of the specified bundle project is erroneous.
	 * 
	 * @param bundle the bundle project containing the transition to check for error
	 * @return true if the current transition of the specified bundle project is erroneous or false if
	 * not.
	 */
	boolean hasTransitionError(Bundle bundle);

	/**
	 * Check if the specified transition error exist among at least one of the workspace bundle
	 * projects
	 * 
	 * @param transitionError the transition error to check against
	 * @return true if the specified transition error exist among at least one of the bundle projects
	 * in the workspace
	 */
	boolean hasTransitionError(TransitionError transitionError);

	/**
	 * Get the error associated with bundle project or {@code TransitionError#NOERROR} if no error
	 * 
	 * @param project the bundle project containing the transition error
	 * @return one of the {@code TransitionError} types or {@code TransitionError#NOERROR}
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	TransitionError getError(IProject project) throws ProjectLocationException;

	/**
	 * Get the error associated with bundle project or {@code TransitionError#NOERROR} if no error
	 * 
	 * @param bundle the bundle project containing the transition error
	 * @return one of the {@code TransitionError} types or {@code TransitionError#NOERROR}
	 */
	TransitionError getError(Bundle bundle);

	/**
	 * Clear the error flag of the current transition of the specified bundle project.
	 * 
	 * @param project the bundle project containing the transition to clear the error flag from
	 * @return true if the error flag is cleared in current transition of the specified bundle.
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	boolean clearTransitionError(IProject project) throws ProjectLocationException;

	/**
	 * Remove the specified transition error for a bundle project if the error exists
	 * 
	 * @param project the bundle project to remove the transition error for
	 * @param transitionError the transition error to remove
	 * @return true if the specified transition error was removed. Otherwise false
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	boolean removeTransitionError(IProject project, TransitionError transitionError)
			throws ProjectLocationException;

	/**
	 * Remove the specified transition error for a bundle project if the error exists
	 * 
	 * @param bundle the bundle project to remove the transition error for
	 * @param transitionError the transition error to remove
	 * @return true if the specified transition error was removed. Otherwise false
	 */
	boolean removeTransitionError(Bundle bundle, TransitionError transitionError);

	/**
	 * Remove the specified transition from bundle projects
	 * 
	 * @param transitionError to remove from all bundle projects containing the error
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 */
	void removeTransitionError(TransitionError transitionError) throws ProjectLocationException;

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