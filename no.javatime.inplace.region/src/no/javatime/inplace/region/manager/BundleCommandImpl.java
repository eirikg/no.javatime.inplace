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
package no.javatime.inplace.region.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import no.javatime.inplace.region.Activator;
import no.javatime.inplace.region.events.TransitionEvent;
import no.javatime.inplace.region.intface.BundleActivatorException;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleStateChangeException;
import no.javatime.inplace.region.intface.BundleThread;
import no.javatime.inplace.region.intface.BundleTransitionListener;
import no.javatime.inplace.region.intface.DuplicateBundleException;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.msg.Msg;
import no.javatime.inplace.region.resolver.BundleResolveHookFactory;
import no.javatime.inplace.region.state.BundleNode;
import no.javatime.inplace.region.state.BundleState;
import no.javatime.inplace.region.state.BundleStateEvents;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.TraceMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Maintains workspace bundles and provides an interface to the BundleTransitionListener wiring framework (refresh,
 * resolve, dependency closure), bundle context (install) and the bundle object (start, stop, uninstall,
 * update).
 * <p>
 * Installed workspace bundles are added and uninstalled workspace bundles are removed from the workspace
 * region.
 * 
 * @see BundleWorkspaceRegionImpl
 */
public class BundleCommandImpl implements BundleCommand {

	public final static BundleCommandImpl INSTANCE = new BundleCommandImpl();

	private BundleWorkspaceRegionImpl bundleRegion = BundleWorkspaceRegionImpl.INSTANCE;
	private BundleTransitionImpl bundleTransition = BundleTransitionImpl.INSTANCE;

	// Used in wait loop, waiting for the refresh thread to notify that it has finished refreshing bundles
	private boolean refreshed;

	// The bundle currently in a transition (state changing)
	private static Bundle currentBundle;

	/**
	 * Access to the wiring framework API and used internally to refresh and resolve bundles.
	 */
	private FrameworkWiring frameworkWiring;

	/**
	 * Execution time of start and stop
	 */
	private long msec;

	/**
	 * Default empty constructor.
	 */
	protected BundleCommandImpl() {
		super();
	}

	/**
	 * Create the wiring package service.
	 */
	public void initFrameworkWiring() {
		if (null == frameworkWiring) {
			Bundle systemBundle = getContext().getBundle(0); // Platform.getBundle("org.eclipse.osgi");
			if (null != systemBundle) {
				frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
			} else {
				StatusManager.getManager().handle(new Status(Status.WARNING, Activator.PLUGIN_ID, 
						Msg.SYSTEM_BUNDLE_ERROR), StatusManager.LOG);
			}
		}
	}

	@Override
	public final BundleResolveHookFactory getResolverHookFactory() {
		return Activator.getDefault().getResolverHookFactory();
	}

	@Override
	public boolean isBundleProjectRegistered(IProject project) {
		BundleNode node = bundleRegion.getBundleNode(project);
		if (null != node) {
			return true;
		}
		return false;
	}

	/**
	 * Register a project and its associated workspace bundle in the workspace region. The bundle must be
	 * registered during or after it is installed but before it is resolved. 
	 * <p>
	 * If the bundle does not exists it is initialized with state {@code StateLess} and {@code Transition.NOTRANSITION}
	 * <p>
	 * @param project the project project to register. Must not be null.
	 * @param bundle the bundle to register. May be null.
	 * @param activateBundle true if the project is activated (nature enabled) and false if not activated
	 * @return the new updated bundle node
	 */
	public BundleNode registerBundleNode(IProject project, Bundle bundle, Boolean activateBundle) {
	
		BundleNode node = bundleRegion.getBundleNode(project);
		try {
			node = bundleRegion.put(project, bundle, activateBundle);
		} catch (InPlaceException e) {
			// Assume project not null
		}
		return node;
	}

	@Override
	public void registerBundleProject(IProject project, Bundle bundle, boolean activateBundle) {
		registerBundleNode(project, bundle, activateBundle);		
	}

	@Override
	public Bundle install(IProject project, Boolean activate) throws InPlaceException,
			DuplicateBundleException, ProjectLocationException {
		
		// Register or update the bundle project. The bundle can not be registered before it is installed
		BundleNode bundleNode = registerBundleNode(project, null, activate);
		// The bundle will be registered and associated with the project when 
		// the bundle becomes available in the bundle listener			
		Bundle bundle = install(project);
		// If the bundle listener for some reason did not register the bundle
		if (null == bundleNode.getBundleId()) {
			bundleNode = registerBundleNode(project, bundle, activate);			
		}
		return bundle;
	}

	/**
	 * Installs a bundle from an input stream based on the specified project. For workspace bundles
	 * the location identifier can be obtained from {@link #getBundleLocationIdentifier(IProject)}.
	 * <p>
	 * If the bundle is installed prior to invoking this method, the already installed bundle is returned.
	 * <p>
	 * The bundle project is registered as a workspace bundle by
	 * {@link BundleStateEvents#bundleChanged(BundleEvent)} during install.
	 * @param project installs the associated bundle of the project
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

		Bundle bundle = null;
		InputStream is = null;
		String locationIdentifier = null;
		final BundleNode bundleNode = bundleRegion.getBundleNode(project);
		
		try {
			final BundleState state = bundleNode.getState();
			state.install(bundleNode);	
			locationIdentifier = bundleRegion.getBundleLocationIdentifier(project);
			URL bundleReference = new URL(locationIdentifier);
			is = bundleReference.openStream();
			bundle = Activator.getContext().installBundle(locationIdentifier, is);
			state.commit(bundleNode);
		} catch (MalformedURLException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_install_malformed_error", locationIdentifier);
		} catch (IOException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_install_error", locationIdentifier);
		} catch (NullPointerException npe) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(npe, "bundle_install_npe_error", locationIdentifier);
		} catch (IllegalStateException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_state_error", locationIdentifier);
		} catch (SecurityException e) {
			bundleTransition.setTransitionError(project);
			throw new InPlaceException(e, "bundle_security_error", locationIdentifier);
		} catch (BundleException e) {
			if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
				bundleTransition.setTransitionError(project, TransitionError.DUPLICATE);
				throw new DuplicateBundleException(e, "duplicate_bundle_install_error", locationIdentifier);
			} else {
				bundleTransition.setTransitionError(project);
				throw new InPlaceException(e, "bundle_install_error", locationIdentifier);
			}
		} catch (ProjectLocationException e) {
			bundleTransition.setTransitionError(project);
			throw e;
		}
		finally {
			try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				throw new InPlaceException(e, "io_exception_install", locationIdentifier);
			} finally {
				if (null != bundle) {
					BundleTransitionListener.addBundleTransition(new TransitionEvent(project, bundleNode.getTransition()));
				}
				if (bundleNode.hasTransitionError()) {
					bundleNode.rollBack();
				}
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
		boolean resolved = true;
		if (null == frameworkWiring) {
			if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
				TraceMessage.getInstance().getString("null_admin_bundle");
			throw new InPlaceException("null_framework");
		}
		try {
			for (Bundle bundle : bundles) {
				BundleNode node = bundleRegion.getBundleNode(bundle);
				node.getState().resolve(node);
			}
			resolved = frameworkWiring.resolveBundles(bundles);
			if (!resolved) {
				for (Bundle bundle : bundles) {
					BundleNode node = bundleRegion.getBundleNode(bundle);
					int state = getState(bundle);
					if ((state & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
						node.setTransitionError(TransitionError.ERROR);
					}
				}
			}
			return resolved;
			// Resolved and unresolved bundles are traced and reported in the resolver hook
		} catch (SecurityException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
					BundleNode node = bundleRegion.getBundleNode(bundle);
					node.setTransitionError(TransitionError.EXCEPTION);
				}
			}
			throw new InPlaceException(e, "bundles_security_error", bundleRegion.formatBundleList(bundles, true));
		} catch (IllegalArgumentException e) {
			for (Bundle bundle : bundles) {
				if ((getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0) {
					BundleNode node = bundleRegion.getBundleNode(bundle);
					node.setTransitionError(TransitionError.EXCEPTION);
				}
			}
			throw new InPlaceException(e, "bundles_argument_resolve_bundle", bundleRegion.formatBundleList(bundles,
					true));
		} finally {
			// The last step (after unresolve) in resolve is resolve. Force a resolve trace
			for (Bundle bundle : bundles) {
				BundleNode node = bundleRegion.getBundleNode(bundle);
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, Transition.RESOLVE));
				if (node.hasTransitionError()) {
					node.rollBack();
				} else {
					node.getState().commit(node);
				}
			}
		}
	}

	/**
	 * Refresh the specified set of bundles synchronously.
	 * 
	 * @param bundles to refresh. Must not be null.
	 * @throws InPlaceException when the framework wiring object is null, the bundle was created with another
	 *           framework wiring object than the current or if a security permission is missing. See
	 *           {@link FrameworkWiring#refreshBundles(Collection, FrameworkListener...)} for details.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	@Override
	public void refresh(final Collection<Bundle> bundles) throws InPlaceException {

		if (null == bundles || bundles.size() == 0) { 
			return; // Ok to return when no bundles to refresh
		}
		try { // wait block
			for (Bundle bundle : bundles) {
				if (BundleWorkspaceRegionImpl.INSTANCE.exist(bundle)) {
					BundleNode node = bundleRegion.getBundleNode(bundle);
					node.getState().refresh(node);
				}
			}
			// Used to notify that refresh has finished in the framework event handler
			final BundleCommandImpl thisBundleCommand = this;
			synchronized (this) {
				this.refreshed = false;
			}
			try { // refresh block
				frameworkWiring.refreshBundles(bundles, new FrameworkListener() {
					@Override
					public void frameworkEvent(FrameworkEvent event) {

						if (Category.getState(Category.bundleEvents)) {
							TraceMessage.getInstance().getString("framework_event", BundleCommandImpl.INSTANCE.getStateName(event),
									event.getBundle().getSymbolicName());
						}
						
						if ((event.getType() & (FrameworkEvent.ERROR)) != 0) {
							for (Bundle bundle : bundles) {
								BundleNode node = bundleRegion.getBundleNode(bundle);
								node.setTransitionError(TransitionError.ERROR);
							}
							StatusManager.getManager().handle(
									new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, event.getBundle().getBundleId(), null,
											event.getThrowable()), StatusManager.LOG);
						}
						
						synchronized (thisBundleCommand) { // Notify to proceed
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
					BundleNode node = bundleRegion.getBundleNode(bundle);
					node.setTransitionError(TransitionError.EXCEPTION);
				}
				throw new InPlaceException(e, "framework_bundle_security_error", bundleRegion.formatBundleList(bundles,
						true));
			} catch (IllegalArgumentException e) {
				for (Bundle bundle : bundles) {
					BundleNode node = bundleRegion.getBundleNode(bundle);
					node.setTransitionError(TransitionError.EXCEPTION);
				}
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
			throw new InPlaceException(e, "interrupt_exception_refresh", BundleCommandImpl.class.getSimpleName());
		} finally {
			if (Category.DEBUG && Category.getState(Category.listeners))
				TraceMessage.getInstance().getString("continuing_after_refresh",
						BundleCommandImpl.class.getSimpleName());
			for (Bundle bundle : bundles) {
				BundleNode node = bundleRegion.getBundleNode(bundle);
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, Transition.REFRESH));
				if (node.hasTransitionError()) {
					node.rollBack();
				} else {
					node.getState().commit(node);
				}
			}
		}
	}

	@Override
	public void start(Bundle bundle, int startOption, int timeOut) 
			throws InPlaceException, InterruptedException, IllegalStateException {

		class StartTask implements Callable<String> {
			Bundle bundle;
			int startOption = Bundle.START_TRANSIENT;
	
			public StartTask(Bundle bundle, int startOption) {
				this.bundle = bundle;
				this.startOption = startOption;
			}
			
			@Override
			public String call() throws Exception {
					start(bundle, startOption);
				return null;				
			}
		}
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<String> future = null;
			future = executor.submit(new StartTask(bundle, startOption));
			future.get(timeOut, TimeUnit.MILLISECONDS);
		} catch (CancellationException e) {			
			throw new InPlaceException(e);
		} catch (TimeoutException e) {			
			bundleTransition.setTransitionError(bundle, TransitionError.STATECHANGE);
			throw new BundleStateChangeException(e, "bundle_task_start_terminate", bundle);
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
	
	
	public long getExecutionTime() {
		return msec;
	}
	
	
	@Override
	public void start(Bundle bundle, int startOption) 
			throws InPlaceException, BundleActivatorException, IllegalStateException, BundleStateChangeException {

		if (null == bundle) {
			throw new InPlaceException("null_bundle_start");
		}
		BundleNode node = bundleRegion.getBundleNode(bundle);
		long startTime = System.currentTimeMillis();
		try {
			currentBundle = bundle;
			node.getState().start(node);
			bundle.start(startOption);
		} catch (IllegalStateException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {		
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			if (null != e.getCause() && (e.getCause() instanceof ThreadDeath)) {
				node.setTransitionError(TransitionError.INCOMPLETE);				
				String msg = ExceptionMessage.getInstance().formatString("bundle_task_start_terminate", bundle);
				throw new IllegalStateException(msg, e);
			}
			node.setTransitionError(TransitionError.EXCEPTION);				
			if (e.getType() == BundleException.ACTIVATOR_ERROR) {
				throw new BundleActivatorException(e, "bundle_activator_error", bundle);
			} else if (e.getType() == BundleException.STATECHANGE_ERROR)  {
				node.setTransitionError(TransitionError.STATECHANGE);				
				throw new BundleStateChangeException(e, "bundle_statechange_error", bundle);							
			}	else {
				throw new InPlaceException(e, "bundle_start_error", bundle);
			}
		} finally {
			msec = System.currentTimeMillis() - startTime;
			BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition())); 
			if (!(node.getTransitionError() == TransitionError.STATECHANGE)) {
				currentBundle = null;
			}
			// The framework moves the bundle to state resolve for incomplete (exceptions) start commands   
			if (node.hasTransitionError()) {
				node.getState().rollBack(node);
			} else {
				node.getState().commit(node);
			}
			try {
			} catch (ProjectLocationException e) {
				throw new InPlaceException(e, "bundle_start_error", bundle);
			}
		}
	}

	@Override
	public void stop(Bundle bundle, boolean stopTransient, int timeOut) 
			throws InPlaceException, InterruptedException, IllegalStateException {
		
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
			future.get(timeOut, TimeUnit.MILLISECONDS);
		} catch (CancellationException e) {			
			throw new InPlaceException(e);
		} catch (TimeoutException e) {			
			throw new BundleStateChangeException(e, "bundle_task_stop_terminate", bundle);
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

	@Override
	public void stop(Bundle bundle, Boolean stopTransient) throws InPlaceException, BundleStateChangeException {

		if (null == bundle) {
			throw new InPlaceException("null_bundle_stop");
		}

		BundleNode node = bundleRegion.getBundleNode(bundle);
		long startTime = System.currentTimeMillis();
		try {
			currentBundle = bundle;
			node.getState().stop(node);
			if (!stopTransient) {
				bundle.stop();
			} else {
				bundle.stop(Bundle.STOP_TRANSIENT);
			}
		} catch (IllegalStateException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			if (null != e.getCause() && (e.getCause() instanceof ThreadDeath)) {
				node.setTransitionError(TransitionError.INCOMPLETE);
				String msg = ExceptionMessage.getInstance().formatString("bundle_task_stop_terminate", bundle);
				throw new IllegalStateException(msg, e);
			}
			node.setTransitionError(TransitionError.EXCEPTION);
			if (e.getType() == BundleException.STATECHANGE_ERROR)  {
				throw new BundleStateChangeException(e, "bundle_statechange_error", bundle);							
			}	else {
				throw new InPlaceException(e, "bundle_stop_error", bundle);
			}
		} finally {
			BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition())); 
			// The framework moves the bundle to state resolve for both 
			// successful and incomplete (exceptions) stop commands   
			if (node.hasTransitionError()) {
				node.getState().rollBack(node);
			} else {
				node.getState().commit(node);
			}
			msec = System.currentTimeMillis() - startTime;
			if (!(bundleTransition.getError(bundle) == TransitionError.STATECHANGE)) {
				currentBundle = null;
			}
		}
	}

	@Override
	public Bundle getCurrentBundle() {
		return currentBundle;
	}

	@Override
	public void setCurrentBundle(Bundle bundle) {
		currentBundle = bundle;
		boolean stateChanging = BundleThread.isStateChanging(bundle);
		if (null == bundle && stateChanging) {
			System.out.println("Error finnishing state change for bundle" + bundle);
		}
		if (null != bundle && !stateChanging) {
			System.out.println("Error in state change for bundle" + bundle);
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
		// Contains duplicate candidate bundles to be removed in the resolver hook in case of singleton
		// collisions
		Map<Bundle, Set<Bundle>> duplicateInstanceGroups = new HashMap<Bundle, Set<Bundle>>();
		Set<Bundle> duplicateInstanceCandidates = new LinkedHashSet<Bundle>();

		String location = null;
		BundleNode node = bundleRegion.getBundleNode(bundle);
		BundleState state = node.getState();
		try {
			state.update(node);
			// Set conditions in the resolver hook for removal of duplicates to avoid singleton
			// collisions
			duplicateInstanceCandidates.add(bundle);
			duplicateInstanceGroups.put(bundle, duplicateInstanceCandidates);
			getResolverHookFactory().setGroups(duplicateInstanceGroups);
			location = bundle.getLocation();
			URL bundlereference = new URL(location);
			is = bundlereference.openStream();
			bundle.update(is);
		} catch (MalformedURLException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_update_malformed_error", location);
		} catch (IOException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "io_exception_update", bundle, location);
		} catch (IllegalStateException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
				node.setTransitionError(TransitionError.DUPLICATE);
				throw new DuplicateBundleException(e, "duplicate_bundle_update_error", location, bundle);
			} else {
				node.setTransitionError(TransitionError.EXCEPTION);
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
				BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, node.getTransition())); 
				if (node.hasTransitionError()) {
					node.getState().rollBack(node);
				} else {
					node.getState().commit(node);
				}
				// TODO Check again if resolver hook has been visited during update.
				duplicateInstanceGroups.clear();
				duplicateInstanceCandidates.clear();
			}
		}
		return bundle;
	}

	@Override
	public IProject uninstall(Bundle bundle, Boolean unregister) throws InPlaceException, ProjectLocationException {

		IProject project = null;
		try {
			project = uninstall(bundle);
		} finally {
			if (unregister) {
				unregisterBundleProject(project);
			}
		}
		return project;
	}

	@Override
	public IProject uninstall(Bundle bundle) throws InPlaceException {

		if (bundle == null) {
			throw new InPlaceException("null_bundle_uninstall");
		}
		BundleNode node = bundleRegion.getBundleNode(bundle);
		BundleState state = null;
		IProject project = null;
		try {
			if (null == node) {
				bundle.uninstall();
				throw new InPlaceException("bundle_unregistered", bundle);
			}
			state = node.getState();
			project = node.getProject();
			state.uninstall(node);
			bundle.uninstall();
		} catch (IllegalStateException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
				TraceMessage.getInstance().getString("state_error_uninstall_bundle", bundle, e.getLocalizedMessage());
			throw new InPlaceException(e, "bundle_state_error", bundle);
		} catch (SecurityException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_security_error", bundle);
		} catch (BundleException e) {
			node.setTransitionError(TransitionError.EXCEPTION);
			throw new InPlaceException(e, "bundle_uninstall_error", bundle);
		} finally {
			BundleTransitionListener.addBundleTransition(new TransitionEvent(bundle, Transition.UNINSTALL)); 
			if (null != node) {
				if (node.hasTransitionError()) {
					node.getState().rollBack(node);
				} else {
					node.getState().commit(node);
				}
			}
		}
		return project;
	}

	@Override
	public void unregisterBundle(Bundle bundle) {
		
		BundleNode node = bundleRegion.getBundleNode(bundle);
		if (null != node) {
			IProject project = node.getProject();
			if (!project.isAccessible()) {
				unregisterBundleProject(project);
			} else {
				node.setBundleId(null);
				node.setActivated(false);
			}
		}
	}
	
	@Override
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
			throw new InPlaceException("null_framework");
		}
		try {
			return frameworkWiring.getDependencyClosure(bundles);
		} catch (IllegalArgumentException e) {
			// Assume all bundles was created by this framework instance
		}
		return Collections.<Bundle>emptySet();
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
			if (Category.DEBUG && Activator.getDefault().msgOpt().isBundleOperations())
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
		if (null == event) {
			return "UNKNOWN_STATE";			
		}
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
		if (null == event) {
			return "UNKNOWN_STATE";			
		}
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
		return Activator.getContext();
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
