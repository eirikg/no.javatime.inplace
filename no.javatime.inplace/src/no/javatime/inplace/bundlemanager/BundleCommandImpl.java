/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.bundlemanager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.FrameworkWiring;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.bundlemanager.BundleTransition.Transition;
import no.javatime.inplace.bundlemanager.BundleTransition.TransitionError;
import no.javatime.inplace.bundlemanager.state.BundleNode;
import no.javatime.inplace.bundlemanager.state.BundleState;
import no.javatime.inplace.bundlemanager.state.BundleStateFactory;
import no.javatime.inplace.bundlemanager.state.StateLess;
import no.javatime.inplace.bundlemanager.state.UninstalledState;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.WarnMessage;

/**
 * Maintains workspace bundles and provides an interface to the BundleManager wiring framework (refresh,
 * resolve, dependency closure), bundle context (install) and the bundle object (start, stop, uninstall,
 * update).
 * <p>
 * Installed workspace bundles are added and uninstalled workspace bundles are removed from the workspace
 * region.
 * 
 * @see BundleWorkspaceImpl
 */
class BundleCommandImpl implements BundleCommand {

	final static BundleCommandImpl INSTANCE = new BundleCommandImpl();

	private BundleWorkspaceImpl bundleRegion = BundleWorkspaceImpl.INSTANCE;
	private BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;

	// Used in wait loop, waiting for the refresh thread to notify that it has finished refreshing bundles
	private boolean refreshed;

	/**
	 * Access to the wiring framework API and used internally to refresh and resolve bundles.
	 */
	private FrameworkWiring frameworkWiring;

	/**
	 * Factory creating resolver hook objects for filtering and detection of duplicate bundle instances
	 */
	protected BundleResolveHookFactory resolverHookFactory = new BundleResolveHookFactory();

	/**
	 * Service registrator for the resolve hook factory.
	 */
	private ServiceRegistration<ResolverHookFactory> resolveHookRegistration;

	/**
	 * Default empty constructor.
	 */
	protected BundleCommandImpl() {
		super();
	}

	/**
	 * Create a wiring package service and a resolve hook factory
	 */
	void init() {
		Bundle systemBundle = getContext().getBundle(0); // Platform.getBundle("org.eclipse.osgi");
		if (null != systemBundle) {
			frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
		} else {
			String msg = WarnMessage.getInstance().formatString("system_bundle_not_started");
			StatusManager.getManager().handle(new BundleStatus(StatusCode.WARNING, InPlace.PLUGIN_ID, msg),
					StatusManager.LOG);
		}
		resolveHookRegistration = getContext().registerService(ResolverHookFactory.class, resolverHookFactory,
				null);
	}

	/**
	 * Unregister the resolver hook
	 */
	void dispose() {
		resolveHookRegistration.unregister();
	}

	/**
	 * Installs a workspace bundle from an input stream based on the location identifier of the specified
	 * {@code project} and records the installed bundle along with the project and activation status as a
	 * workspace bundle in the bundle workspace region.
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
	 * @param activate true if the project is activated (nature enabled) and false if not activated
	 * @return the installed bundle object
	 * @throws InPlaceException if the specified project is null, the location of the specified project could
	 *           not be found or any of the {@link BundleContext#installBundle(String, InputStream)} exceptions
	 *           except duplicate bundles
	 * @throws DuplicateBundleException if a bundle with the same symbolic name and version already exists
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found
	 * @see #install(IProject)
	 * @see #registerBundleNode(IProject, Bundle, Boolean)
	 * @see BundleEventManager#bundleChanged(BundleEvent)
	 */
	@Override
	public Bundle install(IProject project, Boolean activate) throws InPlaceException,
			DuplicateBundleException, ProjectLocationException {

		Bundle bundle = bundleRegion.get(project);
		if (null == bundleRegion.get(project)) {
			registerBundleNode(project, bundle, activate);
		}
		if (null == bundle) {
			bundle = install(project);
		}
		return bundle;
	}

	/**
	 * Installs a bundle from an input stream based on the specified location identifierz. For workspace bundles
	 * the location identifier can be obtained from {@link #getBundleLocationIdentifier(IProject)}.
	 * <p>
	 * If the bundle is installed prior to invoking this method, the already installed bundle is returned.
	 * <p>
	 * The bundle project is registered as a workspace bundle by
	 * {@link BundleEventManager#bundleChanged(BundleEvent)} during install.
	 * @param project TODO
	 * 
	 * @return the installed bundle object
	 * @throws InPlaceException for any of the {@link BundleContext#installBundle(String, InputStream)}
	 *           exceptions except duplicate bundles
	 * @throws DuplicateBundleException if a bundle with the same symbolic name and version already exists
	 * @throws ProjectLocationException if the specified project is null or the location of the specified
	 *           project could not be found
	 * @see BundleContext#installBundle(String, InputStream)
	 * @see #install(IProject, Boolean)
	 */
	protected Bundle install(IProject project) throws InPlaceException, DuplicateBundleException,
			ProjectLocationException {

		String locationIdentifier = bundleRegion.getBundleLocationIdentifier(project);
		Bundle bundle = null;
		InputStream is = null;
		try {
			URL bundleReference = new URL(locationIdentifier);
			is = bundleReference.openStream();
			bundleTransition.setTransition(project, Transition.INSTALL);
			bundle = getContext().installBundle(locationIdentifier, is);
			if (Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("install_bundle", bundle.getSymbolicName(),
						getStateName(bundle), bundle.getLocation());
		} catch (MalformedURLException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_install_malformed_error", locationIdentifier);
		} catch (IOException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_install_error", locationIdentifier);
		} catch (NullPointerException npe) {
			bundleTransition.setTransitionError(project);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("npe_error_install_bundle", locationIdentifier,
						npe.getLocalizedMessage());
			throw new InPlaceException(npe, "bundle_install_npe_error", locationIdentifier);
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(project);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_install_bundle", locationIdentifier,
						e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", locationIdentifier);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(project);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_install_bundle", locationIdentifier,
						e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_security_error", locationIdentifier);
		} catch (BundleException e) {
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_install_bundle", locationIdentifier,
						e.getLocalizedMessage());
			if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
				bundleTransition.setTransitionError(project, TransitionError.DUPLICATE);
				throw new DuplicateBundleException(e, "duplicate_bundle_install_error", locationIdentifier);
			} else {
				bundleTransition.setTransitionError(project);
				throw new InPlaceException(e, "bundle_install_error", locationIdentifier);
			}
		} finally {
			try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				throw new InPlaceException(e, "io_exception_install", locationIdentifier);
			} finally {
				BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
						.getTransition(project)));
			}
		}
		return bundle;
	}
	
	/**
	 * Resolves the specified set of bundles. If no bundles are specified, then the Framework will attempt to
	 * resolve all unresolved bundles.
	 * 
	 * @param bundles bundles to resolve.
	 * @return true if all specified bundles are resolved; false otherwise.
	 * @throws InPlaceException if the framework is null, a bundle is created with another framework or a
	 *           security permission is missing. See {@link FrameworkWiring#resolveBundles(Collection)} for
	 *           details.
	 * @see FrameworkWiring#resolveBundles(Collection)
	 */
	@Override
	public Boolean resolve(Collection<Bundle> bundles) throws InPlaceException {
		if (null == frameworkWiring) {
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("null_admin_bundle");
			throw new InPlaceException("null_framework");
		}
		try {
			for (Bundle bundle : bundles) {
				bundleTransition.setTransition(bundle, Transition.RESOLVE);
				bundleRegion.getActiveState(bundle).resolve(bundleRegion.getBundleNode(bundle));
			}
			boolean resolved = frameworkWiring.resolveBundles(bundles);
			if (Category.getState(Category.bundleOperations)) {
				for (Bundle bundle : bundles) {
					if ((getState(bundle) & (Bundle.RESOLVED | Bundle.STARTING)) != 0) {
						TraceMessage.getInstance().getString("resolve_bundle", bundle, getStateName(bundle));
					}
				}
			}
			return resolved;
			// Resolved and unresolved bundles are traced and reported in the resolver hook
		} catch (SecurityException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
					bundleTransition.setTransitionError(bundle);
					bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.installedState);
				}
			}
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_resolve_bundles",
						bundleRegion.formatBundleList(bundles, true));
			throw new InPlaceException(e, "bundles_security_error", bundleRegion.formatBundleList(bundles, true));
		} catch (IllegalArgumentException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
					bundleTransition.setTransitionError(bundle);
					bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.installedState);
				}
			}
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("argument_error_resolve_bundles",
						bundleRegion.formatBundleList(bundles, true));
			throw new InPlaceException(e, "bundles_argument_resolve_bundle", bundleRegion.formatBundleList(bundles,
					true));
		} finally {
			for (Bundle bundle : bundles) {
				try {
					BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
							.getTransition(bundleRegion.getProject(bundle))));
				} catch (ProjectLocationException e) {
					throw new InPlaceException(e, "bundle_resolve_error", bundle);
				}
			}
		}
	}

	public void refresh(final Collection<Bundle> bundles) throws InPlaceException {

		if (bundles.size() == 0) {
			return;
		}
		try {
			for (Bundle bundle : bundles) {
				if (Category.getState(Category.bundleOperations))
					TraceMessage.getInstance().getString("refresh_bundle", bundle);
				if ((bundle.getState() & (Bundle.UNINSTALLED)) == 0) {
					bundleTransition.setTransition(bundle, Transition.REFRESH);
					bundleRegion.getActiveState(bundle).refresh(bundleRegion.getBundleNode(bundle));
				}
			}
			// Used to notify that refresh has finished in the framework event handler
			final BundleCommandImpl thisBundleCommand = this;
			synchronized (this) {
				this.refreshed = false;
			}
			try {
				frameworkWiring.refreshBundles(bundles, new FrameworkListener() {
					public void frameworkEvent(FrameworkEvent event) {
						synchronized (thisBundleCommand) { // Notify job to proceed
							if (Category.DEBUG && Category.getState(Category.listeners))
								TraceMessage.getInstance().getString("notify_refresh_finished",
										BundleCommandImpl.class.getSimpleName(), bundleRegion.formatBundleList(bundles, true));
							thisBundleCommand.refreshed = true;
							thisBundleCommand.notifyAll();
						}
					}
				});
			} catch (SecurityException e) {
				for (Bundle bundle : bundles) {
					if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
						bundleTransition.setTransitionError(bundle);
						bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.resolvedState);
					}
				}
				if (Category.DEBUG && Category.getState(Category.bundleOperations))
					TraceMessage.getInstance().getString("security_error_refresh_bundles",
							bundleRegion.formatBundleList(bundles, true));
				throw new InPlaceException(e, "framework_bundle_security_error", bundleRegion.formatBundleList(bundles,
						true));
			} catch (IllegalArgumentException e) {
				for (Bundle bundle : bundles) {
					if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
						bundleTransition.setTransitionError(bundle);
						bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.resolvedState);
					}
				}
				if (Category.DEBUG && Category.getState(Category.bundleOperations))
					TraceMessage.getInstance().getString("argument_error_refresh_bundles",
							bundleRegion.formatBundleList(bundles, true));
				throw new InPlaceException(e, "bundles_argument_refresh_bundle", bundleRegion.formatBundleList(bundles,
						true));
			}
			
			synchronized (this) {
				while (!this.refreshed) {					
					if (Category.DEBUG && Category.getState(Category.listeners))
						TraceMessage.getInstance().getString("waiting_on_refresh",
								BundleCommandImpl.class.getSimpleName());
					this.wait();
				}
			}
		} catch (InterruptedException e) {
			String msg = ExceptionMessage.getInstance().formatString("interrupt_exception_refresh",
					BundleCommandImpl.class.getSimpleName());
			throw new InPlaceException(e, msg);
		} finally {
			if (Category.DEBUG && Category.getState(Category.listeners))
				TraceMessage.getInstance().getString("continuing_after_refresh",
						BundleCommandImpl.class.getSimpleName());
			for (Bundle bundle : bundles) {
				try {
					BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
							.getTransition(bundleRegion.getProject(bundle))));
				} catch (ProjectLocationException e) {
					throw new InPlaceException(e, "bundle_refresh_error", bundle);
				}
			}
		}
	}

	/**
	 * Refresh the specified set of bundles. Note that BundleManager refresh spawns a new thread.
	 * 
	 * @param bundles to refresh. Must not be null.
	 * @param listener to be notified when refresh has been completed
	 * @throws InPlaceException when the framework wiring object is null, the bundle was created with another
	 *           framework wiring object than the current or if a security permission is missing. See
	 *           {@link FrameworkWiring#refreshBundles(Collection, FrameworkListener...)} for details.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	@Override
	public void refresh(Collection<Bundle> bundles, FrameworkListener listener) throws InPlaceException {
		if (null == frameworkWiring) {
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("null_admin_bundle");
			throw new InPlaceException("null_framework");
		}
		try {
			for (Bundle bundle : bundles) {
				if (Category.getState(Category.bundleOperations))
					TraceMessage.getInstance().getString("refresh_bundle", bundle);
				if ((bundle.getState() & (Bundle.UNINSTALLED)) == 0) {
					bundleTransition.setTransition(bundle, Transition.REFRESH);
					bundleRegion.getActiveState(bundle).refresh(bundleRegion.getBundleNode(bundle));
				}
			}
			frameworkWiring.refreshBundles(bundles, listener);
		} catch (SecurityException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
					bundleTransition.setTransitionError(bundle);
					bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.resolvedState);
				}
			}
			if (Category.DEBUG && Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_refresh_bundles",
						bundleRegion.formatBundleList(bundles, true));
			throw new InPlaceException(e, "framework_bundle_security_error", bundleRegion.formatBundleList(bundles,
					true));
		} catch (IllegalArgumentException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.INSTALLED)) != 0) {
					bundleTransition.setTransitionError(bundle);
					bundleRegion.setActiveState(bundle, BundleStateFactory.INSTANCE.resolvedState);
				}
			}
			if (Category.DEBUG && Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("argument_error_refresh_bundles",
						bundleRegion.formatBundleList(bundles, true));
			throw new InPlaceException(e, "bundles_argument_refresh_bundle", bundleRegion.formatBundleList(bundles,
					true));
		}
	}
	private volatile Thread t;
	public void start(Bundle bundle, int startOption, int timeOut) 
			throws InPlaceException, InterruptedException, TimeoutException {

		class StartTask implements Callable<String> {
			Bundle bundle;
			int startOption = Bundle.START_TRANSIENT;
			
			public StartTask(Bundle bundle, int startOption) {
				this.bundle = bundle;
				this.startOption = startOption;
			}
			
			@Override
			public String call() throws Exception {
//				try {						
					start(bundle, startOption);
//				} catch (Exception e) {
//					throw e;
//				}
				return null;				
			}
		}
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<String> future = null;
			future = executor.submit(new StartTask(bundle, startOption));
			future.get(timeOut, TimeUnit.SECONDS);
		} catch (CancellationException e) {			
			throw new InPlaceException(e);
		} catch (TimeoutException e) {			
			// TODO Work on a solution to interrupt the running bundle
			// Using BundleHost is for testing/experimenting
//			BundleHost bh = bundle.adapt(BundleHost.class);
//			if (bh != null) {
//				System.out.println("BundleHost ok");
//				t = bh.getStateChanging();
//				t.interrupt();
//			} else
//				System.out.println("BundleHost failed to adapt");
//			new Thread(new Runnable() {
//				public void run(){
//					Thread thisThread = Thread.currentThread();
//					while (t.isAlive()) {
//						t.interrupt();
//					}
//				}
//			}).start();			
//
//			while (t.isAlive()) {
//				try {
//					t.join(1000);
//				} catch (InterruptedException e1) {
//					System.out.println("Correctly interrupted");
//					e1.printStackTrace();
//				}
//			}

			throw e;
		} catch (InterruptedException e) {
			System.out.print("Thread exited");
			throw e;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof InPlaceException) {
				throw (InPlaceException) cause;
			} else if (cause instanceof BundleActivatorException) {
				throw (BundleActivatorException) cause;				
			} else {
				throw new InPlaceException(e);
			}
		} finally {
			try {
				executor.shutdownNow();				
			} catch (Exception ex) {
				throw new InPlaceException(ex, "bundle_security_error", bundle);
			}
		}
	}

	/**
	 * Start the specified bundle according to the specified activation policy.
	 * 
	 * @param bundle the bundle object to start
	 * @param startOption One of {@link Bundle#START_ACTIVATION_POLICY} and {@link Bundle#START_TRANSIENT}
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#start(int)} exceptions
	 * @see Bundle#start(int)
	 */
	@Override
	public void start(Bundle bundle, int startOption) throws InPlaceException, BundleActivatorException {

		if (null == bundle) {
			throw new InPlaceException("null_bundle_start");
		}
		BundleState state = bundleRegion.getActiveState(bundle);
		try {
			bundleTransition.setTransition(bundle, Transition.START);
			long startTime = 0;
			state.start(bundleRegion.getBundleNode(bundle));
			if (Category.getState(Category.bundleOperations))
				startTime = System.currentTimeMillis();
			bundle.start(startOption);
			if (Category.getState(Category.bundleOperations)) {
				 long msec = System.currentTimeMillis() - startTime;
				 TraceMessage.getInstance().getString("start_bundle", bundle, getStateName(bundle), msec); 
			}
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("state_error_start_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_start_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_start_bundle", bundle, e.getLocalizedMessage());
			if (e.getType() == BundleException.ACTIVATOR_ERROR) {
				throw new BundleActivatorException(e, "bundle_activator_error", bundle);
			} else {
				throw new InPlaceException(e, "bundle_start_error", bundle);
			}
		} finally {
			try {
				BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
						.getTransition(bundleRegion.getProject(bundle))));
			} catch (ProjectLocationException e) {
				throw new InPlaceException(e, "bundle_start_error", bundle);
			}
		}
	}

	public void stop(Bundle bundle, boolean stopTransient, int timeOut) 
			throws InPlaceException, InterruptedException, TimeoutException {
		
		class StopTask implements Callable<String> {
			Bundle bundle;
			boolean stopTransient;
			
			public StopTask(Bundle bundle, boolean stopTransient) {
				this.bundle = bundle;
				this.stopTransient = stopTransient;
			}
			
			@Override
			public String call() throws Exception {
				try {
					stop(bundle, stopTransient);
				} catch (Exception e) {
					throw e;
				}
				return null;				
			}
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<String> future = null;
			future = executor.submit(new StopTask(bundle, stopTransient));
			future.get(timeOut, TimeUnit.SECONDS);
		} catch (CancellationException e) {			
			throw new InPlaceException(e);
		} catch (TimeoutException e) {			
			throw e;
		} catch (InterruptedException e) {
			throw e;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof InPlaceException) {
				throw (InPlaceException) cause;
			} else if (cause instanceof BundleActivatorException) {
				throw (BundleActivatorException) cause;				
			} else {
				throw new InPlaceException(e);
			}
		} finally {
			try {
				executor.shutdownNow();				
			} catch (Exception ex) {
				throw new InPlaceException(ex, "bundle_security_error", bundle);
			}
		}
	}

	/**
	 * Stops the specified bundle. The bundle is ignored if not in state STARTING or ACTIVE.
	 * 
	 * @param bundle the bundle to stop
	 * @param stopTransient true to stop the bundle transient, otherwise false
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#stop(int)} exceptions
	 */
	@Override
	public void stop(Bundle bundle, Boolean stopTransient) throws InPlaceException {

		if (null == bundle) {
			throw new InPlaceException("null_bundle_stop");
		}
		BundleState state = bundleRegion.getActiveState(bundle);
		try {
			bundleTransition.setTransition(bundle, Transition.STOP);
			state.stop(bundleRegion.getBundleNode(bundle));
			long startTime = 0;
			if (Category.getState(Category.bundleOperations))
				startTime = System.currentTimeMillis();
			if (!stopTransient) {
				bundle.stop();
			} else {
				bundle.stop(Bundle.STOP_TRANSIENT);
			}
			if (Category.getState(Category.bundleOperations)) {
				long msec = System.currentTimeMillis() - startTime;
				TraceMessage.getInstance().getString("stop_bundle", bundle, getStateName(bundle), msec);
			}
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("state_error_stop_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_stop_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_stop_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_stop_error", bundle);
		} finally {
			try {
				BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
						.getTransition(bundleRegion.getProject(bundle))));
			} catch (ProjectLocationException e) {
				throw new InPlaceException(e, "bundle_stop_error", bundle);
			}
		}
	}

	/**
	 * Updates the specified bundle from an input stream based on the bundle location identifier. The location
	 * identifier is the location passed to {@link #install(IProject)} or the location identifier for workspace
	 * bundles used by {@link #install(IProject, Boolean)} when the specified bundle was installed.
	 * 
	 * @param bundle the bundle object to update
	 * @return the object of the updated bundle
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#update(InputStream)} exceptions
	 * @throws DuplicateBundleException if this bundle is a duplicate - same symbolic name and version - of an
	 *           already installed bundle with a different location identifier.
	 */
	@Override
	public Bundle update(Bundle bundle) throws InPlaceException, DuplicateBundleException {

		InputStream is = null;
		if (bundle == null) {
			throw new InPlaceException("null_bundle_update");
		}
		String location = null;
		BundleState state = bundleRegion.getActiveState(bundle);
		try {
			location = bundle.getLocation();
			URL bundlereference = new URL(location);
			is = bundlereference.openStream();
			bundleTransition.setTransition(bundle, Transition.UPDATE);
			state.update(bundleRegion.getBundleNode(bundle));
			bundle.update(is);
			if (Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("update_bundle", bundle, getStateName(bundle));
		} catch (MalformedURLException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			throw new InPlaceException(e, "bundle_update_malformed_error", location);
		} catch (IOException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			throw new InPlaceException(e, "io_exception_update", bundle, location);
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("state_error_update_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_update_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_update_bundle", bundle.getSymbolicName(),
						e.getLocalizedMessage());
			if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
				bundleTransition.setTransitionError(bundle, TransitionError.DUPLICATE);
				throw new DuplicateBundleException(e, "duplicate_bundle_update_error", location, bundle);
			} else {
				bundleTransition.setTransitionError(bundle);
				throw new InPlaceException(e, "bundle_update_error", bundle);
			}
		} finally {
			try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				throw new InPlaceException(e, "io_exception_update", bundle, location);
			} finally {
				try {
					BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
							.getTransition(bundleRegion.getProject(bundle))));
				} catch (ProjectLocationException e) {
					throw new InPlaceException(e, "bundle_update_error", bundle);
				}
			}
		}
		return bundle;
	}

	@Override
	public IProject uninstall(Bundle bundle, Boolean deactivate) throws InPlaceException, ProjectLocationException {

		IProject project = null;
		try {
			project = uninstall(bundle);
		} finally {
			if (deactivate) {
				unregisterBundleProject(project);
			}
		}
		return project;
	}

	/**
	 * Uninstalls and removes the specified workspace bundle from from the bundle workspace region if it exists
	 * as workspace bundle. The project and activation status associated with the workspace bundle is removed
	 * along with the bundle object.
	 * 
	 * @param bundle the bundle object to uninstall
	 * @return the bundle object of the uninstalled bundle
	 * @throws InPlaceException if bundle is null or any of the {@link Bundle#uninstall()} exceptions
	 */
	@Override
	public IProject uninstall(Bundle bundle) throws InPlaceException {

		if (bundle == null) {
			throw new InPlaceException("null_bundle_uninstall");
		}
		BundleState state = bundleRegion.getActiveState(bundle);
		IProject project = null;
		try {
			project = bundleRegion.getProject(bundle);
			state.uninstall(bundleRegion.getBundleNode(bundle));
			bundleTransition.setTransition(bundle, Transition.UNINSTALL);
			bundle.uninstall();
			if (Category.getState(Category.bundleOperations))
				TraceMessage.getInstance().getString("uninstall_bundle", bundle.getSymbolicName(),
						getStateName(bundle), bundle.getLocation());
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("state_error_uninstall_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("security_error_uninstall_bundle", bundle,
						e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			bundleTransition.setTransitionError(bundle);
			bundleRegion.setActiveState(bundle, state);
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("error_uninstall_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_uninstall_error", bundle);
		} finally {
			try {
				if (null != project) {
					BundleManager.addBundleTransition(new TransitionEvent(bundle, bundleTransition
							.getTransition(project)));
				}
			} catch (ProjectLocationException e) {
				throw new InPlaceException(e, "bundle_uninstall_error", bundle);
			}
		}
		return project;
	}

	public void registerBundle(IProject project, Bundle bundle, Boolean activateBundle) {
		registerBundleNode(project, bundle, activateBundle);
	}
	/**
	 * Register a project and its associated workspace bundle in the workspace region. The bundle must be
	 * registered during or after it is installed but before it is resolved. The bundle is set to state
	 * installed after it is registered. If the bundle is already registered, the method returns.
	 * <p>
	 * 
	 * @param project the project project to register. Must not be null.
	 * @param bundle the bundle to register. Must not be null.
	 * @param activateBundle true if the project is activated (nature enabled) and false if not activated
	 * @return the new or existing bundle node or null if any of the bundle or projects parameters are null
	 */
	public BundleNode registerBundleNode(IProject project, Bundle bundle, Boolean activateBundle) {
		BundleNode node = bundleRegion.getBundleNode(project);
		try {
			// Register the bundle with its project and initialize state to stateless
			node = bundleRegion.put(project, bundle, activateBundle);
			BundleState state = node.getCurrentState();
			// Bundle is state less when registered and state should be installed after install
			if (state instanceof StateLess || state instanceof UninstalledState) {
				state.install(node);
			}
		} catch (InPlaceException e) {
		}
		return node;
	}

	/**
	 * Unregister a workspace bundle from the workspace region. The bundle must be unregistered after it is
	 * uninstalled. The activation status is set to false and the bundle is removed from it associated project
	 * 
	 * @param bundle bundle to unregister form the workspace region.
	 */
	public void unregisterBundle(Bundle bundle) {
		BundleNode node = bundleRegion.getBundleNode(bundle);
		if (null != node) {
			node.setBundleId(null);
			node.setActivated(false);
		}
	}
	
	@Override
	public void registerBundleProject(IProject project, Bundle bundle, boolean activateBundle) {
		registerBundleProjectNode(project, bundle, activateBundle);		
	}
	
	public boolean isBundleProjectRegistered(IProject project) {
		BundleNode node = bundleRegion.getBundleNode(project);
		if (null != node) {
			return true;
		}
		return false;
	}	
	
	public BundleNode registerBundleProjectNode(IProject project, Bundle bundle, Boolean activateBundle) {
		BundleNode node = null;
		// Do not register the bundle more than once
		try {
			// Register the bundle with its project and initialize state to stateless
			node = bundleRegion.put(project, bundle, activateBundle);
		} catch (InPlaceException e) {
		}
		return node;
	}

	public void unregisterBundleProject(IProject project) {
		bundleRegion.remove(project);
	}

	/**
	 * The framework standard dependency walker finding all requiring bundles to the specified initial set of
	 * bundles
	 * 
	 * @param bundles initial set of bundles
	 * @return the the dependency closures of the initial set of bundles specified
	 */
	@Override
	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
		if (null == frameworkWiring) {
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("null_admin_bundle");
			throw new InPlaceException("null_framework");
		}
		try {
			return frameworkWiring.getDependencyClosure(bundles);
		} catch (IllegalArgumentException e) {
		}
		return Collections.emptySet();
	}

	/**
	 * Obtain the resolver hook factory for singletons.
	 * 
	 * @return the resolver hook factory object
	 */
	@Override
	public final BundleResolveHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	/**
	 * Access to in use non current wirings after update and uninstall and before refresh.
	 * 
	 * @return bundles with in use non current wirings or an empty collection
	 * @throws InPlaceException if the framework wiring is null
	 */
	@Override
	public Collection<Bundle> getRemovalPending() throws InPlaceException {
		if (null == frameworkWiring) {
			if (Category.DEBUG && Category.isEnabled(Category.bundleOperations))
				TraceMessage.getInstance().getString("null_admin_bundle");
			throw new InPlaceException("null_framework");
		}
		return frameworkWiring.getRemovalPendingBundles();
	}

	/**
	 * Get state constant of the specified bundle. If {@code Bundle} is {@code null} return
	 * {@code Bundle.UNINSTALLED}.
	 * 
	 * @param bundle of the bundle with a state
	 * @return the state of the bundle
	 */
	@Override
	public int getState(Bundle bundle) {

		if (null == bundle) {
			return Bundle.UNINSTALLED;
		}
		return bundle.getState();
	}

	/**
	 * Get the mnemonic state name of the specified bundle. If {@code Bundle} is {@code null} return
	 * "UNINSTALLED".
	 * 
	 * @param bundle the bundle with a state
	 * @return the state name of the bundle
	 */
	@Override
	public String getStateName(Bundle bundle) {

		if (null == bundle) {
			return "UNINSTALLED";
		}
		String typeName = null;
		switch (bundle.getState()) {
		case Bundle.INSTALLED:
			typeName = "INSTALLED";
			break;
		case Bundle.STARTING:
			// typeName = "<<LAZY>>";
			typeName = "STARTING";
			break;
		case Bundle.STOPPING:
			typeName = "STOPPING";
			break;
		case Bundle.UNINSTALLED:
			typeName = "UNINSTALLED";
			break;
		case Bundle.RESOLVED:
			typeName = "RESOLVED";
			break;
		case Bundle.ACTIVE:
			typeName = "ACTIVE";
			break;
		default:
			typeName = "UNKNOWN_STATE";
			break;
		}
		return typeName;
	}

	/**
	 * Get the mnemonic state name of the bundle for the specified bundle event.
	 * 
	 * @param event the bundle event of a bundle with a state
	 * @return the state name of the bundle with the specified bundle event
	 */
	@Override
	public String getStateName(BundleEvent event) {

		String typeName = null;
		switch (event.getType()) {
		case BundleEvent.INSTALLED:
			typeName = "INSTALLED";
			break;
		case BundleEvent.STARTED:
			typeName = "STARTED";
			break;
		case BundleEvent.STOPPED:
			typeName = "STOPPED";
			break;
		case BundleEvent.UPDATED:
			typeName = "UPDATED";
			break;
		case BundleEvent.UNINSTALLED:
			typeName = "UNINSTALLED";
			break;
		case BundleEvent.RESOLVED:
			typeName = "RESOLVED";
			break;
		case BundleEvent.UNRESOLVED:
			typeName = "UNRESOLVED";
			break;
		case BundleEvent.STARTING:
			typeName = "STARTING";
			break;
		case BundleEvent.STOPPING:
			typeName = "STOPPING";
			break;
		case BundleEvent.LAZY_ACTIVATION:
			typeName = "LAZY_ACTIVATION";
			break;
		default:
			typeName = "UNKNOWN_STATE";
		}
		return typeName;
	}

	/**
	 * Get the mnemonic state name of the specified framework event.
	 * 
	 * @param event the framework event with a state
	 * @return the state name of the framework event
	 */
	@Override
	public String getStateName(FrameworkEvent event) {

		String typeName = null;
		int type = event.getType();
		switch (type) {
		case FrameworkEvent.STARTED:
			typeName = "STARTED";
			break;
		case FrameworkEvent.ERROR:
			typeName = "ERROR";
			break;
		case FrameworkEvent.WARNING:
			typeName = "WARNING";
			break;
		case FrameworkEvent.INFO:
			typeName = "INFO";
			break;
		case FrameworkEvent.PACKAGES_REFRESHED:
			typeName = "PACKAGES_REFRESHED";
			break;
		case FrameworkEvent.STARTLEVEL_CHANGED:
			typeName = "STARTLEVEL_CHANGED";
			break;
		case FrameworkEvent.STOPPED:
			typeName = "STOPPED";
			break;
		case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
			typeName = "STOPPED_BOOTCLASSPATH_MODIFIED";
			break;
		case FrameworkEvent.STOPPED_UPDATE:
			typeName = "STOPPED_UPDATE";
			break;
		case FrameworkEvent.WAIT_TIMEDOUT:
			typeName = "WAIT_TIMEDOUT";
			break;
		default:
			typeName = "UNKNOWN_STATE";
			break;
		}
		return typeName;
	}

	/**
	 * The bundle context object.
	 * 
	 * @return the bundle context object for this bundle
	 */
	@Override
	public BundleContext getContext() {
		return InPlace.getContext();
	}

	/**
	 * The framework wiring object.
	 * 
	 * @return the framework wiring object used by this bundle manager
	 */
	@Override
	public FrameworkWiring getFrameworkWiring() {
		return frameworkWiring;
	}

	/**
	 * Convenience method that returns the current revision of the specified bundle.
	 * 
	 * @param bundle object of an installed bundle
	 * @return the current revision of the bundle
	 * @throws InPlaceException if bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	@Override
	public BundleRevision getCurrentRevision(Bundle bundle) throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException("null_bundle_adapt");
		}
		try {
			return bundle.adapt(BundleRevision.class);
		} catch (SecurityException e) {
			throw new InPlaceException(e, "bundle_security_error", bundle.getLocation());
		}
	}

	/**
	 * Gets all bundle revisions for the specified bundle.
	 * 
	 * @param bundle the bundle with one or more revisions
	 * @return the set of revisions for the bundle as a list. The list should at least contain the current
	 *         revision of the bundle.
	 * @throws InPlaceException if the bundle is null or a proper adapt permission is missing
	 * @see Bundle#adapt(Class)
	 */
	@Override
	public List<BundleRevision> getBundleRevisions(Bundle bundle) throws InPlaceException {
		if (null == bundle) {
			throw new InPlaceException("null_bundle_adapt");
		}
		try {
			BundleRevisions br = bundle.adapt(BundleRevisions.class);
			List<BundleRevision> brs = br.getRevisions();
			return brs;
		} catch (SecurityException e) {
			throw new InPlaceException(e, "bundle_security_error", bundle.getLocation());
		}
	}
}