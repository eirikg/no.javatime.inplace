package no.javatime.inplace.pl.trace.intface;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.util.messages.MessageContainer;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Message view displaying messages added to {@link MessageContainer message} and
 * {@link Trace trace} containers.
 * <p>
 * To register and use the message view as a service see {@link Extender} and {@link Extension}
 * <p>
 * Use the {@link MessageContainer} or {@link Trace} service to provide content to the viewer
 *  
 * @see MessageContainer 
 * @see Trace
 */
public interface MessageView {
	
	/**
	 * Name of extension header in the manifest file. 
	 * This header entry may be used to activate this interface as a service
	 * 
	 * The content of the header is the class implementing this message view interface
	 */
	public final static String MESSAGE_VIEW_HEADER = "Trace-View";

	/**
	 * Get the default image for the message as an image descriptor
	 * @return the default image for the message view
	 */
	public ImageDescriptor getMessageViewImage();

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
	 * @return true if the message view is visible and fale if not visible
	 */
	public boolean isVisible();

	/**
	 * Get the identification string of this view. This is the same as the class
	 * name of the view class. Note this is not the same as the class name of
	 * the class implementing the interface of this service.
	 * 
	 * @return the id of the message view 
	 */
	public String getViewId();

	/**
	 * Displays the content in the specified message container in the
	 * message view.
	 * <p>
	 * All messages added to the message container will be displayed in 
	 * the message view.
	 *  
	 * @param containerService the message store used by the message view
	 * @return true if the input was set, and false if not
	 */
}
