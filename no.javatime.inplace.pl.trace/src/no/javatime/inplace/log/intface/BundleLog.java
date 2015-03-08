package no.javatime.inplace.log.intface;

import java.text.MessageFormat;
import java.util.Dictionary;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.osgi.framework.Bundle;

/**
 * Save status objects to the log file. Logged status objects are always displayed in the view
 * defined by the {@link BundleLogView} service interface.
 * <p>
 * The log is based on the {@link IBundleStatus} status object. To be independent of the status
 * object use the methods associated with the {@code log()} method. The methods associated with the
 * {@code log()} method uses the status object internally to build a tree structure of status
 * objects to log while the {@code log(IBundleStatus)} methods accept a single status object, which
 * in itself may be a tree. The rest of the log methods creates and sends a single status object to
 * the log.
 * <p>
 * The InPlace Activator uses the {@link MessageOptions#setIsBundleOperations(boolean)} or
 * {@link #enableLogging(boolean)} option to determine if a status object should be logged or not.
 * It is possible to toggle this logging option from the user interface in the log view.
 * Alternatively you can use your own option to filter logging to be independent of the logging
 * option used by the InPlace Activator.
 * <p>
 * The service scope should be bundle (e.g. see
 * {@link Extenders#register(Bundle, Bundle, String, Object, Dictionary)}) if
 * {@link #log()} is used. The {@code log()} and its associated add methods are shared among all
 * threads in a bundle. To acquire a separate bundle status tree for a thread you can register a
 * separate extender with bundle scope for that thread.
 * 
 * @see BundleLogView
 * @see MessageOptions#isBundleEvents()
 * @see MessageOptions#setIsBundleEvents(boolean)
 */
public interface BundleLog {

	/**
	 * Manifest header for accessing the default implementation class name of the bundle log. May be
	 * used to register this interface as a service or extending this interface as a service.
	 */
	public final static String BUNDLE_LOG_SERVICE = "BundleLog-Service";

	/**
	 * Logs the specified status object.
	 * <p>
	 * If the bundle or the project is added to the specified status object the bundle or project name
	 * along with the state of the bundle or the state of the corresponding bundle of the project is
	 * logged.
	 * 
	 * @param status the status object to log
	 * @return the logged message of the specified status object
	 * @throws BundleLogException If bundle in the specified status object is null and the
	 * {@code #BundleContext} of this bundle is no longer valid
	 */
	public String log(IBundleStatus status) throws BundleLogException;

	/**
	 * Logs the specified status code, bundle symbolic name, bundle state and message.
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param statusCode the status code to log
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException if the pattern is invalid, or if an argument in the arguments array
	 * is not of the type expected by the format element(s) that use it. If the specified bundle
	 * parameter is null and the {@code #BundleContext} of this bundle is no longer valid
	 */
	public String log(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException;

	/**
	 * Logs the specified status code, bundle symbolic name, bundle state and message.
	 * <p>
	 * If the specified bundle is null the bundle object of the {@code BundleLog} bundle is used
	 * 
	 * @param statusCode the status code to log
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @throws BundleLogException If the specified bundle parameter is null and the
	 * {@code #BundleContext} of this bundle is no longer valid
	 */
	public void log(StatusCode statusCode, Bundle bundle, Exception exception, String msg)
			throws BundleLogException;

	/**
	 * Logs all added status objects since last {@code #clear()} or {@code #log()}
	 * <p>
	 * All added status objects are cleared after they have been logged
	 * 
	 * @throws BundleLogException If no root status object has been added or if the bundle in the
	 * added root status object is null and the {@code #BundleContext} of this bundle is no longer
	 * valid
	 * @see #clear()
	 */
	public void log() throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the pattern is invalid, or if an argument in the arguments array
	 * is not of the type expected by the format element(s) that use it. If the specified bundle
	 * parameter is null and the {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String add(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @see #clear()
	 * @see #log()
	 */
	public void add(StatusCode statusCode, Bundle bundle, Exception exception, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a parent to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the current status object is an immediate child to the root.If
	 * the pattern is invalid, or if an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it. If the specified bundle parameter is null and the
	 * {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String addParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a parent to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception TODO
	 * @param msg the message to log
	 * @throws BundleLogException If the current status object is an immediate child to the root
	 * @see #clear()
	 * @see #log()
	 */
	public void addParent(StatusCode statusCode, Bundle bundle, Exception exception, String msg)
			throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the current
	 * status object. The current status object is unchanged.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the current status object is an immediate child to the root.If
	 * the pattern is invalid, or if an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it. If the specified bundle parameter is null and the
	 * {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String addToParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the current
	 * status object. The current status object is unchanged.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception TODO
	 * @param msg the message to log
	 * @throws BundleLogException If the current status object is an immediate child to the root
	 * @see #clear()
	 * @see #log()
	 */
	public void addToParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a sibling to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the current status object is the root or if the pattern is
	 * invalid, or if an argument in the arguments array is not of the type expected by the format
	 * element(s) that use it. If the specified bundle parameter is null and the
	 * {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String addSibling(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a sibling to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @throws BundleLogException If the current status object is the root
	 * @see #clear()
	 * @see #log()
	 */
	public void addSibling(StatusCode statusCode, Bundle bundle, Exception exception, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a new root to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added and the current
	 * status object becomes the child of this new root status object. Added status objects are
	 * removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the pattern is invalid, or if an argument in the arguments array
	 * is not of the type expected by the format element(s) that use it. If the specified bundle
	 * parameter is null and the {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String addRoot(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a new root to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added and the current
	 * status object becomes the child of this new root status object. Added status objects are
	 * removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @see #clear()
	 * @see #log()
	 */
	public void addRoot(StatusCode statusCode, Bundle bundle, Exception exception, String msg);

	/**
	 * Check if the current position in the status tree is root
	 * 
	 * @return true if the root of the status tree is the current status and false if not
	 */
	public boolean isRoot();

	/**
	 * Counts number of status object members in the tree
	 * 
	 * @return number of status object members in the tree. Returns zero if no status objects has been
	 * added to the tree
	 */
	public int size();

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the root. After
	 * the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added . Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param pattern creates a message to log with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the pattern is invalid, or if an argument in the arguments array
	 * is not of the type expected by the format element(s) that use it. If the specified bundle
	 * parameter is null and the {@code #BundleContext} of this bundle is no longer valid
	 * @see #clear()
	 * @see #log()
	 */
	public String addToRoot(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the root. After
	 * the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added . Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the log
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @see #clear()
	 * @see #log()
	 */
	public void addToRoot(StatusCode statusCode, Bundle bundle, Exception exception, String msg);

	/**
	 * Clears all added status objects since last {@code #clear()} or {@code #log()}
	 * 
	 * @return true if any status objects were cleared and false if no status objects have been
	 * cleared
	 * @see #log()
	 */
	public boolean clear();

	/**
	 * Enable/disable log messages. If the specified logging parameter is {@code true} messages are
	 * logged and displayed in the log view. If {@code false} log messages are ignored.
	 * <p>
	 * This is the same as enabling/disabling logging using the
	 * {@link MessageOptions#setIsBundleEvents(boolean)} method
	 * 
	 * @param log enables logging when true and disables logging when false
	 * @return the current logging setting
	 * @throws ExtenderException if failing to get the extender service for the command options
	 */
	public boolean enableLogging(boolean log) throws ExtenderException;

	/**
	 * Get the bundle log view service associated with this bundle log
	 * 
	 * @param bundle the bundle using this service
	 * @return the bundle log view service
	 * @throws ExtenderException if the bundle log view extender or the service of the extender could
	 * not be created or accessed
	 */
	public BundleLogView getBundleLogViewService(Bundle bundle) throws ExtenderException;
}
