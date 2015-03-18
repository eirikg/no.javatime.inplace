package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Stops pending bundle projects with an initial state of ACTIVE and STARTING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this bundle executor
 * before the bundles are stopped according to the current dependency option. Requiring bundles to
 * pending bundle projects are always added as pending bundle projects before bundles are stopped.
 * 
 * @see Start
 */
public interface Stop extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the stop bundle
	 * operation.
	 */
	public final static String STOP_BUNDLE_SERVICE = "Stop-Bundle-Service";

	/**
	 * Stop the current start or stop bundle operation
	 * 
	 * @throws InPlaceException if failing to get the command options service
	 */
	// TODO Change to extender exception
	public void stopCurrentBundleOperation(IProgressMonitor monitor) throws InPlaceException;

	/**
	 * A bundle about to change its state while a bundle command is executing.
	 * 
	 * @return true if a bundle command is executing and a bundle is state changing. False if not.
	 */
	public boolean isStateChanging();

}