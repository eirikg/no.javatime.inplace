package no.javatime.inplace.pl.console.intface;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Service showing and hiding the bundle console. When system out and system err is set - from a
 * button in the bundle console - to the bundle console, output is streamed to the console page
 * where the current IDE is running. When disabled output is streamed to the default console of the
 * IDE.
 */
public interface BundleConsoleFactory {

	/**
	 * Manifest header for accessing the implementation class name of the bundle console
	 * May be used when extending this bundle
	 */
	public final static String BUNDLE_CONSOLE_HEADER = "Bundle-Console-Factory";

	/**
	 * Get the default image for the bundle console view as an image descriptor
	 * 
	 * @return the default image for the bundle console view
	 */
	public abstract ImageDescriptor getConsoleViewImage();

	/**
	 * Show the bundle console view
	 */
	public abstract void showConsoleView();

	/**
	 * Close the bundle console.
	 * <p>
	 * Also closes the generic console view when the bundle console id is the last page in the console
	 * view
	 */
	public abstract void closeConsoleView();

	/**
	 * Check if bundle console view is visible
	 * 
	 * @return true if the bundle console is visible, otherwise false
	 */
	public abstract Boolean isConsoleViewVisible();

	/**
	 * Stream system out end system err to the bundle console
	 * <p>
	 * This does not alter the stored preference option for system out and system err
	 * 
	 * @see MessageOptions
	 */
	public abstract void setSystemOutToBundleConsole();

	/**
	 * Stream system out and system err to the default console for the IDE
	 * <p>
	 * This does not alter the stored preference option for system out and system err
	 * 
	 * @see MessageOptions
	 */
	public abstract void setSystemOutToIDEDefault();

}