package no.javatime.inplace.log.intface;

import no.javatime.inplace.region.status.IBundleStatus;



public interface BundleLog {

	public final static String BUNDLE_LOG_HEADER = "BundleLog-Container";

	/**
	 * Displays the content in the specified message in the
	 * view defined by the {@link BundleLogView} service interface
	 * 
	 * @see BundleLogView
	 */
	public String trace(IBundleStatus status);
	
	public String message(IBundleStatus status);

	public String user(IBundleStatus status);
	
	public String exception(IBundleStatus status);

	public String warning(IBundleStatus status);

	public String error(IBundleStatus status);
}
