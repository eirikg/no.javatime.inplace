package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Starts pending bundle projects with an initial state of INSTALLED, RESOLVED and STOPPING.
 * <p>
 * Calculate closure of bundles and add them as pending bundle projects to this bundle executor
 * before the bundles are started according to the current dependency option. Providing bundles to
 * pending bundle projects are always added as pending bundle projects before bundles are started.
 * 
 * @see Stop
 */
public interface Start extends BundleExecutor {

	/**
	 * Manifest header for accessing the default service implementation class name of the activate
	 * project operation
	 */
	public final static String START_BUNDLE_SERVICE = "Start-Bundle-Service";

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