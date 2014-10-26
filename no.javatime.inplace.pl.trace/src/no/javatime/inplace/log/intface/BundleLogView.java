package no.javatime.inplace.log.intface;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The bundle log view interface. 
 * <p>
 * The view can be extended as service. The name of the service can be obtained from
 * the manifest by using {@code #BUNDLE_LOG_VIEW_HEADER}. 
 * <p>
 * The service is read-only and the service scope can be singleton.
 */
public interface BundleLogView {
	
	/**
	 * Name of extension header in the manifest file. 
	 * This header entry may be used to activate this interface as a service
	 * 
	 * The content of the header is the class implementing this message view interface
	 */
	public final static String BUNDLE_LOG_VIEW_HEADER = "BundleLog-View";

	/**
	 * Get the default image for the log view as an image descriptor
	 * 
	 * @return the default image for the message view
	 */
	public ImageDescriptor getLogViewImage();

	/**
	 * Show the view if it is not visible.
	 */
	public void show();
	
	/**
	 * Hide the view if it is visible
	 */
	public void hide();

	/**
	 * Check for visibility of the view in the current page
	 * 
	 * @return true if the message view is visible and false if not visible
	 */
	public boolean isVisible();

	/**
	 * Get the identification string of this view. This is the same as the class
	 * name of the view class interface name. 
	 * 
	 * @return the id of the message view 
	 */
	public String getViewId();
}
