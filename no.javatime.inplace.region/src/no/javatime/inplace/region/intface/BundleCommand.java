package no.javatime.inplace.region.intface;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.resolver.BundleResolveHookFactory;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

public interface BundleCommand {

	/**
	 * Obtain the resolver hook factory for singletons.
	 * 
	 * @return the resolver hook factory object
	 */
	public BundleResolveHookFactory getResolverHookFactory();

	/**
	 * Register and activates (install, resolve and start) the specified bundle project with this
	 * region.
	 * <p>
	 * For the bundle to start on the development platform the default output folder should be added
	 * to the Bundle-ClassPath in the manifest file of the specified project before the bundle project
	 * is activated
	 * <p>
	 * If the project has a lazy activation policy the bundle is started with the
	 * {@code Bundle#START_ACTIVATION_POLICY} otherwise the {@code Bundle#START_TRANSIENT} start
	 * option is used
	 * <p>
	 * The start operation uses the timeout option if it is set to true.
	 * 
	 * @param project the project to register and activate
	 * @return the activated bundle. The returned state
	 * @throws InPlaceException if "dev.mode" is off, an IO or property error occurs updating build
	 * properties file, default output folder is missing or any of the
	 * {@link BundleContext#installBundle(String, InputStream)} exceptions except the duplicate bundle
	 * exception
	 * @throws DuplicateBundleException if a bundle with the same symbolic name and version already
	 * exists
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @throws InterruptedException if the start operation is interrupted
	 * @throws IllegalStateException is thrown if the start operation terminates abnormally
	 * @throws BundleStateChangeException failed to complete the requested life cycle state change
	 * @throws ExtenderException if the command options service is invalid or null
	 * @see BundleProjectMeta#addDefaultOutputFolder(IProject)
	 * @see BundleCommand#install(IProject, Boolean)
	 * @see BundleCommand#resolve(Collection)
	 * @see BundleCommand#start(Bundle, int)
	 * @see BundleCommand#start(Bundle, int, int)
	 */
	public Bundle activate(IProject project) throws InPlaceException, DuplicateBundleException,
			ProjectLocationException, InterruptedException, IllegalStateException, ExtenderException;

	/**
	 * Not implemented yet
	 * 
	 * @param project
	 * @return
	 * @throws InPlaceException
	 */
	public Bundle deactivate(IProject project) throws InPlaceException;

	/**
	 * Installs the workspace bundle project and register the installed bundle along with the project
	 * and activation mode as a workspace bundle with this region.
	 * <p>
	 * An activated bundle is a bundle that is resolvable and a deactivated bundle is not. If the
	 * specified activation parameter is {@code Boolean.FALSE} the bundle is installed and recorded as
	 * a deactivated bundle. A deactivated bundle, as opposed to an activated bundle, will be silently
	 * rejected by the resolver hook when resolved. If the activation parameter is
	 * {@code Boolean.TRUE} the bundle is activated and will be accepted by the resolver hook when
	 * resolved.
	 * <p>
	 * A {@link BundleTransition.Transition#ACTIVATE_BUNDLE Transition.ACTIVATE_BUNDLE} is added as a
	 * pending transition to an activated bundle if it has not been resolved before invoking install
	 * and a {@link BundleTransition.Transition#DEACTIVATE Transition.DEACTIVATE} is added as a
	 * pending transition to a deactivated bundle if it has been resolved before invoking install.
	 * <p>
	 * If the bundle is already installed the installed bundle is returned.
	 * <p>
	 * To activate or deactivate an already installed bundle, use
	 * {@code BundleRegion#setActivation(IProject,
	 * Boolean)} or {@code BundleRegion#setActivation(Bundle, Boolean)}.
	 * <p>
	 * The activation mode of a bundle project can be obtained from
	 * {@code BundleRegion#isProjectActivated(IProject)} .
	 * <p>
	 * The location identifier of an uninstalled bundle can be obtained from
	 * {@link BundleRegion#getBundleLocationIdentifier(IProject)} and {@link Bundle#getLocation()} for
	 * an installed bundle.
	 * 
	 * @param project installs the associated bundle based on the location identifier of the project
	 * @param activate true to register the bundle as an activated workspace region bundle. If false
	 * the bundle is registered as a deactivated bundle
	 * @return the installed bundle object
	 * @throws InPlaceException Any of the {@link BundleContext#installBundle(String, InputStream)}
	 * exceptions except duplicate bundles
	 * @throws DuplicateBundleException if a bundle with the same symbolic name and version already
	 * exists
	 * @throws ProjectLocationException if the specified project is null or the location of the
	 * specified project could not be found
	 * @see BundleContext#installBundle(String)
	 * @see BundleContext#installBundle(String, InputStream)
	 */
	public Bundle install(IProject project, Boolean activate) throws InPlaceException,
			DuplicateBundleException, ProjectLocationException;

	/**
	 * Resolves the specified set of bundles. If no bundles are specified, then the Framework will
	 * attempt to resolve all activated unresolved bundles.
	 * 
	 * @param bundles bundles to resolve.
	 * @return true if all specified bundles are resolved; false otherwise.
	 * @throws InPlaceException if the framework is null, a bundle is created with another framework
	 * or a security permission is missing. See {@link FrameworkWiring#resolveBundles(Collection)} for
	 * details.
	 * @see FrameworkWiring#resolveBundles(Collection)
	 */
	public Boolean resolve(Collection<Bundle> bundles) throws InPlaceException;

	/**
	 * Start the specified bundle according to the specified activation policy.
	 * 
	 * @param bundle the bundle object to start
	 * @param startOption One of {@link Bundle#START_ACTIVATION_POLICY} and
	 * {@link Bundle#START_TRANSIENT}
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#start(int)} exceptions
	 * @throws IllegalStateException is thrown if the start operation terminates abnormally
	 * @throws BundleStateChangeException failed to complete the requested lifecycle state change
	 * @see Bundle#start(int)
	 */
	public void start(Bundle bundle, int startOption) throws InPlaceException, IllegalStateException,
			BundleStateChangeException;

	/**
	 * Start the specified bundle according to the specified activation policy. If the start operation
	 * timeouts the task running the start operation is terminated unconditionally.
	 * 
	 * @param bundle the bundle object to start
	 * @param startOption One of {@link Bundle#START_ACTIVATION_POLICY} and
	 * {@link Bundle#START_TRANSIENT}
	 * @param timeOut terminates the start operation after the specified timeout in seconds
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#start(int)} exceptions
	 * @throws InterruptedException if the start operation is interrupted
	 * @throws IllegalStateException is thrown if the start operation timeouts
	 * @throws BundleStateChangeException failed to complete the requested lifecycle state change
	 */
	public void start(Bundle bundle, int startOption, int timeOut) throws InPlaceException,
			InterruptedException, IllegalStateException, BundleStateChangeException;

	/**
	 * Stops the specified bundle. The bundle is ignored if not in state STARTING or ACTIVE.
	 * 
	 * @param bundle the bundle to stop
	 * @param stopTransient true to stop the bundle transient, otherwise false
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#stop(int)} exceptions
	 * @throws BundleStateChangeException failed to complete the requested lifecycle state change
	 */
	public void stop(Bundle bundle, Boolean stopTransient) throws InPlaceException,
			BundleStateChangeException;

	/**
	 * Stops the specified bundle. The bundle is ignored if not in state STARTING or ACTIVE. If the
	 * stop operation timeouts the task running the stop operation is terminated unconditionally.
	 * 
	 * @param bundle the bundle to stop
	 * @param stopTransient true to stop the bundle transient, otherwise false
	 * @param timeOut terminates the stop operation after the specified timeout in seconds
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#stop(int)} exceptions
	 */
	public void stop(Bundle bundle, boolean stopTransient, int timeOut) throws InPlaceException,
			InterruptedException, IllegalStateException, BundleStateChangeException;

	/**
	 * Updates the specified bundle from an input stream based on the bundle location identifier. The
	 * location identifier is the location passed to {@link #install(String)} or the location
	 * identifier for workspace bundles used by {@link #install(IProject, Boolean)} when the specified
	 * bundle was installed.
	 * 
	 * @param bundle the bundle object to update
	 * @return the object of the updated bundle
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#update(InputStream)}
	 * exceptions
	 * @throws DuplicateBundleException if this bundle is a duplicate - same symbolic name and version
	 * - of an already installed bundle with a different location identifier.
	 */
	public Bundle update(Bundle bundle) throws InPlaceException, DuplicateBundleException;

	/**
	 * Refresh the specified set of bundles synchronously.
	 * 
	 * @param bundles to refresh. Must not be null.
	 * @throws InPlaceException when the framework wiring object is null, the bundle was created with
	 * another framework wiring object than the current or if a security permission is missing. See
	 * {@link FrameworkWiring#refreshBundles(Collection, FrameworkListener...)} for details.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	public void refresh(final Collection<Bundle> bundles) throws InPlaceException;

	/**
	 * Uninstall the specified bundle. If the unregister parameter is set to true, the specified
	 * workspace project and the associated bundle is removed from the workspace region. If set to
	 * false the project and the bundle will exist in the workspace region until unregistered.
	 * <p>
	 * To {@link #refresh(Collection) refresh} the bundle after it has been uninstall, defer
	 * unregistering the bundle until after refresh.
	 * 
	 * @param bundle the bundle object to uninstall
	 * @param unregister if true the project and the bundle will will be unregistered and thus removed
	 * from the workspace region.
	 * @return the project associated with the uninstalled bundle or null if the bundle project could
	 * not be registered
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#uninstall()} exceptions
	 * @see BundleRegion#unregisterBundleProject(IProject)
	 */
	public IProject uninstall(Bundle bundle, Boolean unregister) throws InPlaceException,
			ProjectLocationException;

	/**
	 * Uninstalls the bundle. The bundle is not removed from the workspace region if it exists as
	 * workspace bundle
	 * 
	 * @param bundle the bundle object to uninstall
	 * @return the bundle project of the uninstalled bundle or null if the bundle project is not
	 * registered
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#uninstall()} exceptions
	 */
	public IProject uninstall(Bundle bundle) throws InPlaceException;

	/**
	 * The framework standard dependency walker finding all requiring bundles to the specified initial
	 * set of bundles
	 * 
	 * @param bundles initial set of bundles
	 * @return the the dependency closures of the initial set of bundles specified
	 */
	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles);

	/**
	 * Access to in use non current wirings after update and uninstall and before refresh.
	 * 
	 * @return bundles with in use non current wirings or an empty collection
	 * @throws InPlaceException if the framework wiring is null
	 */
	public Collection<Bundle> getRemovalPending() throws InPlaceException;

	/**
	 * A bundle project is about to change its state while a bundle command is executing.
	 * 
	 * @param project check if the project is in a state changing process
	 * @return true if the project is state changing. False if not.
	 */
	public boolean isStateChanging(IProject project);

	/**
	 * Get state constant of the specified bundle. If {@code Bundle} is {@code null} return
	 * {@code Bundle.UNINSTALLED}.
	 * 
	 * @param bundle of the bundle with a state
	 * @return the state of the bundle
	 */
	public int getState(Bundle bundle);

	/**
	 * Get the mnemonic state name of the specified bundle. If {@code Bundle} is {@code null} return
	 * "UNINSTALLED".
	 * 
	 * @param bundle the bundle with a state
	 * @return the state name of the bundle
	 */
	public String getStateName(Bundle bundle);

	/**
	 * Get the mnemonic state name of the bundle for the specified bundle event.
	 * 
	 * @param event the bundle event of a bundle with a state
	 * @return the state name of the bundle with the specified bundle event
	 */
	public String getStateName(BundleEvent event);

	/**
	 * Get the mnemonic state name of the specified framework event.
	 * 
	 * @param event the framework event with a state
	 * @return the state name of the framework event
	 */
	public String getStateName(FrameworkEvent event);

	/**
	 * Convenience method that returns the current revision of the specified bundle.
	 * 
	 * @param bundle object of an installed bundle
	 * @return the current revision of the bundle
	 * @throws InPlaceException if bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	public BundleRevision getCurrentRevision(Bundle bundle) throws InPlaceException;

	/**
	 * Gets all bundle revisions for the specified bundle.
	 * 
	 * @param bundle the bundle with one or more revisions
	 * @return the set of revisions for the bundle as a list. The list should at least contain the
	 * current revision of the bundle.
	 * @throws InPlaceException if the bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	public List<BundleRevision> getBundleRevisions(Bundle bundle) throws InPlaceException;

	public Bundle getCurrentBundle();

	public void setCurrentBundle(Bundle bundle);

	public long getExecutionTime();

}