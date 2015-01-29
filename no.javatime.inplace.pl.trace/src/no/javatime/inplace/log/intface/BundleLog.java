package no.javatime.inplace.log.intface;

import java.text.MessageFormat;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.osgi.framework.Bundle;

/**
 * Save status objects to log file. Logged status objects are always displayed in the view defined
 * by the {@link BundleLogView} service interface.
 * <p>
 * The InPlace Activator uses the {@link MessageOptions} service interface or
 * {@link #enableLogging(boolean)} to determine if a status object should be logged or not. It is
 * possible to toggle this logging option from the user interface in the log view. Alternatively you
 * can use your own option to filter logging to be independent of the logging option used by the
 * InPlace Activator.
 * <p>
 * The service scope should be bundle (e.g. see
 * {@link Extenders#register(Bundle, Bundle, String, Object, java.util.Dictionary)} if {@code log()}
 * is used.
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
	public final static String BUNDLE_LOG_IMPL = "BundleLog-Service";

	/**
	 * Logs the specified status object.
	 * <p>
	 * If the bundle or the project is added to the specified status object the bundle or project name
	 * along with the state of the bundle or the state of the corresponding bundle of the project is
	 * logged.
	 * 
	 * @param status the status object to log
	 * @return the logged message of the specified status object
	 */
	public String log(IBundleStatus status);

	/**
	 * Logs the specified status code, bundle symbolic name, bundle state and message.
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param statusCode the status code to log
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 */
	public String logMsg(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException;

	/**
	 * Logs the specified bundle symbolic name, bundle state, message and exception with
	 * {@link StatusCode#EXCEPTION}.
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param e the exception to log
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 */
	public String logExp(Bundle bundle, Exception e, String pattern, Object... substitutions)
			throws BundleLogException;

	/**
	 * Logs the specified bundle symbolic name, bundle state, and message with
	 * {@link StatusCode#WARNING}.
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 */
	public String logWarn(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException;

	/**
	 * Logs the specified bundle symbolic name, bundle state, and message with
	 * {@link StatusCode#ERROR}.
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 */
	public String logErr(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException;

	/**
	 * Logs the specified bundle symbolic name, bundle state, and message with {@link StatusCode#INFO}
	 * .
	 * <p>
	 * Uses {@link MessageFormat#format(String, Object...)} to format the message
	 * 
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 */
	public String logInfo(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException;

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the added status object
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If an argument in the arguments array is not of the type expected by
	 * the format element(s) that use it.
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #addToRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String add(StatusCode statusCode, Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException;

	public String add(StatusCode statusCode, Bundle bundle, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a parent to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the added status object
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message
	 * @throws BundleLogException If the current status object is an immediate child to the root
	 * status object or if an argument in the arguments array is not of the type expected by the
	 * format element(s) that use it.
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #addToRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String addParent(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException;

	public String addParent(StatusCode statusCode, Bundle bundle, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a sibling to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is the first status object added, a new root status object is added. Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the added status object
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws BundleLogException If the current status object is the root or if the pattern is
	 * invalid, or if an argument in the arguments array is not of the type expected by the format
	 * element(s) that use it.
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #addToRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String addSibling(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException;

	public String addSibling(StatusCode statusCode, Bundle bundle, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a new root to the current
	 * status object. After the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added and the current
	 * status object becomes the child of this new root status object. Added status objects are
	 * removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the added status object
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws LogException If an argument in the arguments array is not of the type expected by the
	 * format element(s) that use it.
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addToRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String addRoot(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException;
	
	public String addRoot(StatusCode statusCode, Bundle bundle, String msg);

	/**
	 * Creates a status object from the specified parameters and adds it as a child to the root. After
	 * the status object is added it becomes the current status object.
	 * <p>
	 * If this is not first status object added, a new root status object is added . Added status
	 * objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status code to log
	 * @param bundle adds the bundle symbolic name and bundle state to the added status object
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @throws LogException If an argument in the arguments array is not of the type expected by the
	 * format element(s) that use it.
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String addToRoot(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException;
	
	public String addToRoot(StatusCode statusCode, Bundle bundle, String msg);

	/**
	 * Clears all added status objects since last {@code #clear()} or {@code #log()}
	 * 
	 * @return true if any status objects were cleared and false if no status objects have been
	 * cleared
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #log()
	 */
	public boolean clear();

	/**
	 * Logs all existing added status objects since last {@code #clear()} or {@code #log()}
	 * <p>
	 * All existing added status objects are cleared after they have been logged
	 * 
	 * @return true if any status objects are logged and false if not log
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #addSibling(StatusCode, Bundle, String, Object...)
	 * @see #addRoot(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 */
	public boolean log();

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
