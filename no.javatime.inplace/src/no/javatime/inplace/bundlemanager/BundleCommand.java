package no.javatime.inplace.bundlemanager;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

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
	 * Installs the workspace bundle project and if the register parameter is true records the installed bundle 
	 * along with the project and activation status as a workspace bundle in the bundle workspace region.
	 * <p>
	 * The activation status of a project can be obtained from
	 * {@link no.javatime.inplace.bundleproject.ProjectProperties#isProjectActivated(IProject)
	 * isProjectActivated(IProject)}.
	 * <p>
	 * The location identifier is obtained from {@link #getBundleLocationIdentifier(IProject)} .
	 * <p>
	 * If already installed return the existing {@code bundle} object.
	 * 
	 * @param project install a workspace bundle finding the bundle location identifier based on this project
	 * @param register true to register the bundle as a workspace region bundle
	 * @return the installed bundle object
	 * @throws ExtenderException if the specified project is null, the location of the specified project could
	 *           not be found or any of the {@link BundleContext#installBundle(String, InputStream)} exceptions
	 *           except duplicate bundles
	 * @throws DuplicateBundleException if a bundle with the same symbolic name and version already exists
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found
	 * @see #install(IProject)
	 * @see #registerBundleNode(IProject, Bundle, Boolean)
	 * @see BundleEventManager#bundleChanged(BundleEvent)
	 */
	public Bundle install(IProject project, Boolean register) throws ExtenderException, DuplicateBundleException;

	/**
	 * Resolves the specified set of bundles. If no bundles are specified, then the Framework will attempt to
	 * resolve all unresolved bundles.
	 * 
	 * @param bundles bundles to resolve.
	 * @return true if all specified bundles are resolved; false otherwise.
	 * @throws ExtenderException if the framework is null, a bundle is created with another framework or a
	 *           security permission is missing. See {@link FrameworkWiring#resolveBundles(Collection)} for
	 *           details.
	 * @see FrameworkWiring#resolveBundles(Collection)
	 */
	public Boolean resolve(Collection<Bundle> bundles) throws ExtenderException;

	/**
	 * Start the specified bundle according to the specified activation policy.
	 * 
	 * @param bundle the bundle object to start
	 * @param startOption One of {@link Bundle#START_ACTIVATION_POLICY} and {@link Bundle#START_TRANSIENT}
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#start(int)} exceptions
	 * @throws IllegalStateException is thrown if the start operation terminates abnormally
	 * @throws BundleStateChangeException failed to complete the requested lifecycle state change
	 * @see Bundle#start(int)
	 */
	public void start(Bundle bundle, int startOption) throws ExtenderException, IllegalStateException, BundleStateChangeException;

	/**
	 * Start the specified bundle according to the specified activation policy. If the start operation timeouts
	 * the task running the start operation is terminated unconditionally.  
	 * 
	 * @param bundle the bundle object to start
	 * @param startOption One of {@link Bundle#START_ACTIVATION_POLICY} and {@link Bundle#START_TRANSIENT}
	 * @param timeOut terminates the start operation after the specified timeout in seconds
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#start(int)} exceptions
	 * @throws InterruptedException if the start operation is interrupted
	 * @throws IllegalStateException is thrown if the start operation timeouts
	 * @throws BundleStateChangeException failed to complete the requested lifecycle state change
	 */
	public void start(Bundle bundle, int startOption, int timeOut) 
			throws ExtenderException, InterruptedException, IllegalStateException, BundleStateChangeException;

	/**
	 * Stops the specified bundle. The bundle is ignored if not in state STARTING or ACTIVE.
	 * 
	 * @param bundle the bundle to stop
	 * @param stopTransient true to stop the bundle transient, otherwise false
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#stop(int)} exceptions
	 */
	public void stop(Bundle bundle, Boolean stopTransient) throws ExtenderException, BundleStateChangeException;
	
	/**
	 * Stops the specified bundle. The bundle is ignored if not in state STARTING or ACTIVE. 
	 * If the stop operation timeouts the task running the stop operation is terminated unconditionally.  
	 * 
	 * @param bundle the bundle to stop
	 * @param stopTransient true to stop the bundle transient, otherwise false
	 * @param timeOut terminates the stop operation after the specified timeout in seconds
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#stop(int)} exceptions
	 */
	public void stop(Bundle bundle, boolean stopTransient, int timeOut) 
			throws ExtenderException, InterruptedException, IllegalStateException, BundleStateChangeException;


	/**
	 * Updates the specified bundle from an input stream based on the bundle location identifier. The location
	 * identifier is the location passed to {@link #install(String)} or the location identifier for workspace
	 * bundles used by {@link #install(IProject, Boolean)} when the specified bundle was installed.
	 * 
	 * @param bundle the bundle object to update
	 * @return the object of the updated bundle
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#update(InputStream)} exceptions
	 * @throws DuplicateBundleException if this bundle is a duplicate - same symbolic name and version - of an
	 *           already installed bundle with a different location identifier.
	 */
	public Bundle update(Bundle bundle) throws ExtenderException, DuplicateBundleException;

	/**
	 * Refresh the specified set of bundles asynchronously.
	 * 
	 * @param bundles to refresh. Must not be null.
	 * @param listener to be notified when refresh has been completed
	 * @throws ExtenderException when the framework wiring object is null, the bundle was created with another
	 *           framework wiring object than the current or if a security permission is missing. See
	 *           {@link FrameworkWiring#refreshBundles(Collection, FrameworkListener...)} for details.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	public void refresh(Collection<Bundle> bundles, FrameworkListener listener) throws ExtenderException;

	/**
	 * Refresh the specified set of bundles synchronously.
	 * 
	 * @param bundles to refresh. Must not be null.
	 * @throws ExtenderException when the framework wiring object is null, the bundle was created with another
	 *           framework wiring object than the current or if a security permission is missing. See
	 *           {@link FrameworkWiring#refreshBundles(Collection, FrameworkListener...)} for details.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	public void refresh(final Collection<Bundle> bundles) throws ExtenderException;

	/**
	 * Uninstalls and ai the unregister parameter is true removes the specified workspace bundle from the bundle workspace 
	 * region if it exists as workspace bundle. The project and activation status associated with the workspace bundle is removed
	 * along with the bundle object.
	 * 
	 * @param bundle the bundle object to uninstall
	 * @param unregister if true the bundle project will be unregistered. The bundle must be registered again when installed 
	 * @return the bundle object of the uninstalled bundle
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#uninstall()} exceptions
	 */
	public IProject uninstall(Bundle bundle, Boolean unregister) throws ExtenderException, ProjectLocationException;

	/**
	 * Uninstalls the bundle. The bundle project is not removed from the workspace region if it exists
	 * as workspace bundle
	 *  
	 * @param bundle the bundle object to uninstall
	 * @return the bundle object of the uninstalled bundle
	 * @throws ExtenderException if bundle is null or any of the {@link Bundle#uninstall()} exceptions
	 */
	public IProject uninstall(Bundle bundle) throws ExtenderException;

	/**
	 * The framework standard dependency walker finding all requiring bundles to the specified initial set of
	 * bundles
	 * 
	 * @param bundles initial set of bundles
	 * @return the the dependency closures of the initial set of bundles specified
	 */
	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles);

	/**
	 * Obtain the resolver hook factory for singletons.
	 * 
	 * @return the resolver hook factory object
	 */
	public BundleResolveHookFactory getResolverHookFactory();

	/**
	 * Access to in use non current wirings after update and uninstall and before refresh.
	 * 
	 * @return bundles with in use non current wirings or an empty collection
	 * @throws ExtenderException if the framework wiring is null
	 */
	public Collection<Bundle> getRemovalPending() throws ExtenderException;

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
	 * The bundle context object.
	 * 
	 * @return the bundle context object for this bundle
	 */
	public BundleContext getContext();

	/**
	 * The framework wiring object.
	 * 
	 * @return the framework wiring object used by this bundle manager
	 */
	public FrameworkWiring getFrameworkWiring();

	/**
	 * Convenience method that returns the current revision of the specified bundle.
	 * 
	 * @param bundle object of an installed bundle
	 * @return the current revision of the bundle
	 * @throws ExtenderException if bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	public BundleRevision getCurrentRevision(Bundle bundle) throws ExtenderException;

	/**
	 * Gets all bundle revisions for the specified bundle.
	 * 
	 * @param bundle the bundle with one or more revisions
	 * @return the set of revisions for the bundle as a list. The list should at least contain the current
	 *         revision of the bundle.
	 * @throws ExtenderException if the bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	public List<BundleRevision> getBundleRevisions(Bundle bundle) throws ExtenderException;
	
	public void registerBundle(IProject project, Bundle bundle, Boolean activateBundle);

	public void unregisterBundle(Bundle bundle);

	/**
	 * Register the specified project as a workspace region project.
	 * @param project to activate
	 */
	public void registerBundleProject(IProject project, Bundle bundle, boolean activateBundle);

	/**
	 * Unregister the specified workspace region project.
	 * @param project to deactivate
	 */
	public void unregisterBundleProject(IProject project);

	/**
	 * Check if the specified project is registered as a workspace bundle project
	 * @param project to check for registration as a workspace bundle project 
	 * @return true if bundle project is registered as a bundle project and false if not
	 */
	public boolean isBundleProjectRegistered(IProject project);
	
	public Bundle getCurrentBundle();
	public void setCurrentBundle(Bundle bundle);
}