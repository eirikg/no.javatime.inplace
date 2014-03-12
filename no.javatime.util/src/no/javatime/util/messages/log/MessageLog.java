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
package no.javatime.util.messages.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.LogMessage;
import no.javatime.util.messages.Message;

import org.eclipse.core.runtime.IStatus;

/**
 * Class <code>Logger</code> is a specific subclass of standard java Logger,
 * augmented to fit the needs of the JavaTime project.
 * 
 * <p>
 * The levels in descending order are:
 * <ol>
 * <li>SEVERE (highest value)</li>
 * <li>WARNING</li>
 * <li>INFO</li>
 * <li>CONFIG</li>
 * <li>FINE</li>
 * <li>FINER</li>
 * <li>FINEST (lowest value)</li>
 *</ol>
 * 
 */
public class MessageLog extends java.util.logging.Logger {


	/** Cache this log manager */
	private static LogManager manager;

	protected MessageLog(String name) {
		super(name, null);
	}

	/**
	 * Report (and possibly create) the logger related to the provided class
	 * @param cl the related class
	 * @return the logger
	 */
	public static MessageLog getLogger(Class<?> cl) {
		return getLogger(cl.getName());
	}

	/**
	 * Report (and possibly create) the logger related to the provided name
	 * (usually the full class name)
	 * @param name the logger name
	 * @return the logger found or created
	 */
	public static synchronized MessageLog getLogger(String name) {
		// Lazy initialization if needed
		if (manager == null) {
			manager = LogManager.getLogManager();
			setGlobalParameters();
		}

		MessageLog result = (MessageLog) manager.getLogger(name);

		if (result == null) {
			result = new MessageLog(name);
			manager.addLogger(result);
			result = (MessageLog) manager.getLogger(name);
		}

		return result;
	}

	/**
	 * Report the resulting level for the logger, which may be inherited from
	 * parents higher in the hierarchy
	 * @return The effective logging level for this logger
	 */
	public Level getEffectiveLevel() {
		java.util.logging.Logger logger = this;
		Level level = getLevel();

		while (level == null) {
			logger = logger.getParent();

			if (logger == null) {
				return null;
			}

			level = logger.getLevel();
		}

		return level;
	}

	/**
	 * Check if a Debug (Fine) would actually be logged
	 * @return true if to be logged
	 */
	public boolean isFineEnabled() {
		return isLoggable(Level.FINE);
	}

	/**
	 * Assert the provided condition, and stop the application if the condition is
	 * false, since this is supposed to detect a programming error
	 * @param exp the expression to check
	 * @param msg the related error message
	 */
	public void logAssert(boolean exp, String msg) {
		if (!exp) {
			severe(msg);
		}
	}

	/**
	 * Set the logger level, using a level name
	 * @param levelStr the name of the level (case is irrelevant), such as Fine or
	 *          INFO
	 */
	public void setLevel(String levelStr) {
		setLevel(Level.parse(levelStr.toUpperCase()));
	}

	/**
	 * Log the provided message and stop
	 * @param msg the (severe) message
	 */
	public void severe(String msg) {
		super.severe(msg);
		new Throwable().printStackTrace();
	}

	/**
	 * Log the provided message and exception, then stop the application
	 * @param msg the (severe) message
	 * @param thrown the exception
	 */
	public void severe(String msg, Throwable thrown) {
		super.severe(msg);
		thrown.printStackTrace();
	}

	/**
	 * Log a warning with a related exception, then continue
	 * @param msg the (warning) message
	 * @param thrown the related exception, whose stack trace will be printed only
	 *          if the constant flag 'printStackTraces' is set.
	 */
	public void warning(String msg, Throwable thrown) {
		super.warning(msg + " [" + thrown + "]");
	}

	/**
	 * Log message based on key parameter using the resource bundle associated
	 * with {@link Message}. The calling class and method are also logged. The
	 * method assumes that the Logger class used is instantiated with its
	 * associated class.
	 * @param key message key used to load the message from the bundle
	 */
	public void logMessage(String key) {
		super.logrb(Level.INFO, super.getName(), Thread.currentThread()
				.getStackTrace()[2].getMethodName(),
				Message.MESSAGE_PROPERTIES_FILE_NAME, key);
	}

	/**
	 * Log message based on key parameter using the resource bundle associated
	 * with {@link Message}.
	 * @param key message key used to load the message from the bundle
	 * @param className name of class that issued the logging request
	 * @param methodName name of method that issued the logging request
	 */
	public void logMessage(String key, String className, String methodName) {
		super.logrb(Level.INFO, className, methodName,
				Message.MESSAGE_PROPERTIES_FILE_NAME, key);
	}

	public void log(Level level, IStatus status) {
		if (status != null) {
			log(level, status.toString());
		}
	}

	/**
	 * This is a convenience method to log that a method is terminating by
	 * throwing an exception. The logging is done using the FINER level and
	 * reverted to the original level of the Logger as it was before the
	 * invocation of this member.
	 * <p>
	 * The method assumes that the Logger class used is instantiated with its
	 * associated class.
	 * </p>
	 * @param thrown is the Throwable exception
	 */
	public void throwing(Throwable thrown) {
		Level level = Level.FINER;
		Level oldl = super.getLevel();
		super.setLevel(level);
		super.throwing(super.getName(), Thread.currentThread().getStackTrace()[2]
				.getMethodName(), thrown);
		super.setLevel(oldl);
	}

	/**
	 * This method define configuration parameters in a programmatic way, so that
	 * only specific loggers if any need to be set in a configuration file.
	 * Assigns rotating log files and prohibit output to the console
	 */
	private static void setGlobalParameters() {
		// Retrieve the logger at the top of the hierarchy
		java.util.logging.Logger topLogger;
		topLogger = java.util.logging.Logger.getLogger("");

		Handler filehandler;
		try {

			// To create the log .lck file, the data sub folder must exist 
			File dataDir = new File(FileLocation.DATA_SUBFOLDER);
			if (!dataDir.exists()) {
				if (dataDir.mkdir()) {
					ErrorMessage.getInstance().getString("log_dir_init_error", FileLocation.DATA_SUBFOLDER);					
				}
			}
			String logFileName = LogMessage.getInstance().formatString("log_file_name");
			Integer  noOfLogFiles = new Integer( LogMessage.getInstance().formatString("no_of_rotating_log_files"));
			Integer  logFileSize = 1048576 * new Integer( LogMessage.getInstance().formatString("log_file_size_in_mb"));
			filehandler = new FileHandler(FileLocation.DATA_SUBFOLDER + logFileName, logFileSize , noOfLogFiles, false);
			filehandler.setLevel(Level.ALL);
			topLogger.addHandler(filehandler);
		} catch (SecurityException e) {
			ErrorMessage.getInstance().getString("log_file_init_error", e.getMessage());
		} catch (IOException e) {
			ErrorMessage.getInstance().getString("log_file_init_error", e.getMessage());
		}

		// Handler for console
		Handler consoleHandler = null;
		for (Handler handler : topLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {				
				consoleHandler = handler;
				break;
			}
		}
		// Do not log to console
		if (consoleHandler != null) {
			topLogger.removeHandler(consoleHandler);
		}
// 		Reuse console
//		if (consoleHandler == null) {
//			consoleHandler = new ConsoleHandler();
//			topLogger.addHandler(consoleHandler);
//		}
//		consoleHandler.setFormatter(new SimpleFormatter());
//		// consoleHandler.setFormatter(new LogBasicFormatter());
//		consoleHandler.setLevel(java.util.logging.Level.INFO);

		// Handler for GUI log pane
		// topLogger.addHandler(new LogGuiHandler());

		// Handler for animation of progress in a StepMonitor
		// topLogger.addHandler(new LogStepMonitorHandler());

		// Default level
		// topLogger.setLevel(java.util.logging.Level.INFO);
	}

}
