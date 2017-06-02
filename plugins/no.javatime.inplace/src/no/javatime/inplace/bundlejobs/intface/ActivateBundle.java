package no.javatime.inplace.bundlejobs.intface;

/**
 * Activates bundle(s) by installing, resolving and starting the bundle(s).
 * <p>
 * For a bundle to be activated its corresponding project must already have been activated. See
 * {@link ActivateProject} for activating projects. If a project that is not activated is added to
 * and executed by this service it is ignored.
 * <p>
 * The workspace is activated if one or more projects are activated. If no projects have been
 * activated the workspace is said to be deactivated and all bundles are in state UNINSTALLED.
 * Possible states for activated projects in an activated workspace are RESOLVED, ACTIVE and
 * STARTING and INSTALLED for not activated projects.
 * <p>
 * The following principles and conditions determine the states bundles are moved to when activated:
 * <ol>
 * <li>The workspace must be activated before any bundles are activated.
 * <li>Not activated projects are installed (state INSTALLED) and bundles for activated projects are
 * by default installed, resolved and started (state ACTIVE or STARTING).
 * <li>When reactivating the workspace (e.g. at startup or by executing the {@link Reset} service)
 * deactivated projects are installed and activated projects are moved to the same state as at shut
 * down if {@link #setRestoreSessionState(Boolean)} is set to {@code true} and started if not.
 * <li>If bundles to activate are dependent on other providing bundles, the independent providing
 * bundles are added to the this bundle operation. The dependency is transitive.
 * </ol>
 * <p>
 * Bundle dependency closures are calculated and added as pending bundles to this service according
 * to the current dependency option. E.g. deactivated projects providing capabilities to an
 * activated project are added to this service for activation.
 * 
 * It is both a prerequisite and guaranteed by this package that all providing projects to the
 * pending projects of this service are activated (nature enabled) when this operation is executed.
 * Providing projects are either activated when a requiring project is activated in
 * {@link ActivateProject} or scheduled for project activation when a new deactivated project is
 * imported by an activated project. Lastly, if none if this holds a deactivated project providing
 * capabilities to an activated bundle is scheduled for activation in the internal resolver hook and
 * the requiring activated bundles are excluded from the resolve set and then resolved when the
 * deactivated project is activated.
 * <p>
 * This service operation is executed implicit at startup of the IDE if the workspace is activated.
 * 
 * @see ActivateProject
 * @see Deactivate
 * 
 */
public interface ActivateBundle extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * bundle operation
	 */
	public final static String ACTIVATE_BUNDLE_SERVICE = "Activate-Bundle-Service";
}