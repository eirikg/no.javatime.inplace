package no.javatime.inplace.log.intface;

import java.text.MessageFormat;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.osgi.framework.Bundle;

/**
 * Log messages and status objects to file and display in the view defined by the
 * {@link BundleLogView} service interface
 * <p>
 * For messages to be logged, logging must be enabled through the {@link MessageOptions} service
 * interface or by using {@link #enableLogging(boolean)}
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
	 * @param statusCode the status object to log
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 */
	public String logMsg(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions);

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
	 */
	public String logExp(Bundle bundle, Exception e, String pattern, Object... substitutions);

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
	 */
	public String logWarn(Bundle bundle, String pattern, Object... substitutions);

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
	 */
	public String logErr(Bundle bundle, String pattern, Object... substitutions);

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
	 */
	public String logInfo(Bundle bundle, String pattern, Object... substitutions);

	/**
	 * Creates the specified status code, bundle symbolic name, bundle state and message as a
	 * status object. If this is the first status object added, a new root status object is added.
	 * If there exist a root status object this status object is added as a child to the root status
	 * object.
	 * <p>
	 * To create a new root status object with existing added status objects as children, see
	 * {@link #addParent(StatusCode, Bundle, String, Object...)}. Existing status objects are
	 * removed after they are logged or cleared.
	 * 
	 * @param statusCode the status object to log
	 * @param bundle adds the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String add(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions);

	/**
	 * Creates the specified status code, bundle symbolic name, bundle state and message as a
	 * status object. If this is the first status object added, a new root status object is added.
	 * If there exist a root status object this status object is added as a parent to the root status
	 * object.
	 * <p>
	 * Existing status objects are removed after they are logged or cleared.
	 * 
	 * @param statusCode the status object to log
	 * @param bundle adds the bundle symbolic name and bundle state
	 * @param pattern creates a message with the given pattern and uses it to format the given
	 * substitutions
	 * @param substitutions used by the pattern parameter to format the resulting message
	 * @return the formatted message or null if the pattern is invalid, or if an argument in the
	 * arguments array is not of the type expected by the format element(s) that use it.
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #clear()
	 * @see #log()
	 */
	public String addParent(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions);

	/**
	 * Clears all added status objects
	 * 
	 * @return true if any status objects were cleared and false if no status objects have been added
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #add(StatusCode, Bundle, String, Object...)
	 * @see #log()
	 */
	public boolean clear();

	/**
	 * Logs all existing added status objects since last {@code #clear()} or {@code #log()}
	 * <p>
	 * All existing added status objects are cleared after they have been logged
	 * 
	 * @return true if any status objects are logged and false if there are no added status objects to
	 * log
	 * @see #addParent(StatusCode, Bundle, String, Object...)
	 * @see #add(StatusCode, Bundle, String, Object...)
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
	 * @param bundle the user bundle object of the returned service
	 * @return the bundle log view service
	 * @throws ExtenderException if the bundle log view extender or the service of the extender could
	 * not be created or accessed
	 */
	public BundleLogView getBundleLogViewService(Bundle bundle) throws ExtenderException;
}
