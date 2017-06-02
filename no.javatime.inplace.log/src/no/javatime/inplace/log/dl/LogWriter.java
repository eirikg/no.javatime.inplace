package no.javatime.inplace.log.dl;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.util.Calendar;
import java.util.Date;

import no.javatime.inplace.log.Activator;
import no.javatime.inplace.log.impl.BundleLogEntryImpl;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

public class LogWriter implements SynchronousLogListener, LogFilter {

	static final String PERF_LOGGER_NAME = "org.eclipse.performance.logger"; //$NON-NLS-1$
	private static final String PROP_LOG_ENABLED = "eclipse.log.enabled"; //$NON-NLS-1$

	private static final String PASSWORD = "-password"; //$NON-NLS-1$	
	/** The session tag */
	private static final String SESSION = "!SESSION"; //$NON-NLS-1$
	/** The entry tag */
	private static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
	/** The sub-entry tag */
	private static final String SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	/** The message tag */
	private static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	/** The bundle state tag */
	private static final String STATE = "!STATE"; //$NON-NLS-1$
	/** The stacktrace tag */
	private static final String STACK = "!STACK"; //$NON-NLS-1$

	/** The line separator used in the log output */
	private static final String LINE_SEPARATOR;
	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	//Constants for rotating log file
	/** The default size a log file can grow before it is rotated */
	private static final int DEFAULT_LOG_SIZE = 128;
	/** The default number of backup log files */
	private static final int DEFAULT_LOG_FILES = 10;
	/** The minimum size limit for log rotation */
	private static final int LOG_SIZE_MIN = 10;

	/** The system property used to specify the log level */
	private static final String PROP_LOG_LEVEL = "eclipse.log.level"; //$NON-NLS-1$
	/** The system property used to specify size a log file can grow before it is rotated */
	private static final String PROP_LOG_SIZE_MAX = "eclipse.log.size.max"; //$NON-NLS-1$
	/** The system property used to specify the maximum number of backup log files to use */
	private static final String PROP_LOG_FILE_MAX = "eclipse.log.backup.max"; //$NON-NLS-1$
	/** The extension used for log files */
	private static final String LOG_EXT = ".bundle.log"; //$NON-NLS-1$
	/** The extension markup to use for backup log files*/
	private static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$

	/** The system property used to specify command line args should be omitted from the log */
	private static final String PROP_LOG_INCLUDE_COMMAND_LINE = "eclipse.log.include.commandline"; //$NON-NLS-1$
	private static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	/** Indicates if the console messages should be printed to the console (System.out) */
	private boolean consoleLog = false;
	/** Indicates if the next log message is part of a new session */
	private boolean newSession = true;
	/**
	 * The File object to store messages.  This value may be null.
	 */
	private File outFile;

	/**
	 * The Writer to log messages to.
	 */
	private Writer writer;

	private final String loggerName;
	private final boolean enabled;
	private final EnvironmentInfo environmentInfo;

	int maxLogSize = DEFAULT_LOG_SIZE; // The value is in KB.
	int maxLogFiles = DEFAULT_LOG_FILES;
	int backupIdx = 0;

	private int logLevel = FrameworkLogEntry.OK;
	private boolean includeCommandLine = true;


	public LogWriter(File outFile, String loggerName) {
		this.outFile = outFile;
		this.writer = null;
		this.loggerName = loggerName;
		environmentInfo = Activator.getDefault().getEnvironmentService();
		this.enabled = true;
		// TODO UI to switch on(of logging
		// this.enabled = "true".equals(environmentInfo.getProperty(PROP_LOG_ENABLED));
		readLogProperties();
	}

	public LogWriter(Writer writer, String loggerName) {
		if (writer == null)
			// log to System.err by default
			this.writer = logForStream(System.err);
		else
			this.writer = writer;
		this.loggerName = loggerName;
		environmentInfo = Activator.getDefault().getEnvironmentService();
		this.enabled = "true".equals(environmentInfo.getProperty(PROP_LOG_ENABLED));
	}

	/**
	 * Constructs an EclipseLog which uses the specified File to log messages to
	 * @param outFile a file to log messages to
	 */
	public LogWriter(File outFile, String loggerName, boolean enabled, EnvironmentInfo environmentInfo) {
		this.outFile = outFile;
		this.writer = null;
		this.loggerName = loggerName;
		this.enabled = enabled;
		this.environmentInfo = environmentInfo;
		readLogProperties();
	}

	/**
	 * Constructs an EclipseLog which uses the specified Writer to log messages to
	 * @param writer a writer to log messages to
	 */
	public LogWriter(Writer writer, String loggerName, boolean enabled, EnvironmentInfo environmentInfo) {
		if (writer == null)
			// log to System.err by default
			this.writer = logForStream(System.err);
		else
			this.writer = writer;
		this.loggerName = loggerName;
		this.enabled = enabled;
		this.environmentInfo = environmentInfo;
	}

	private Throwable getRoot(Throwable t) {
		Throwable root = null;
		if (t instanceof BundleException)
			root = ((BundleException) t).getNestedException();
		if (t instanceof InvocationTargetException)
			root = ((InvocationTargetException) t).getTargetException();
		// skip inner InvocationTargetExceptions and BundleExceptions
		if (root instanceof InvocationTargetException || root instanceof BundleException) {
			Throwable deeplyNested = getRoot(root);
			if (deeplyNested != null)
				// if we have something more specific, use it, otherwise keep what we have
				root = deeplyNested;
		}
		return root;
	}

	/**
	 * Helper method for writing out argument arrays.
	 * @param header the header
	 * @param args the list of arguments
	 */
	private void writeArgs(String header, String[] args) throws IOException {
		if (args == null || args.length == 0)
			return;
		write(header);
		for (int i = 0; i < args.length; i++) {
			//mask out the password argument for security
			if (i > 0 && PASSWORD.equals(args[i - 1]))
				write(" (omitted)"); //$NON-NLS-1$
			else
				write(" " + args[i]); //$NON-NLS-1$
		}
		writeln();
	}

	/**
	 * Returns the session timestamp.  This is the time the platform was started
	 * @return the session timestamp
	 */
	private String getSessionTimestamp() {
		// Main should have set the session start-up timestamp so return that. 
		// Return the "now" time if not available.
		String ts = environmentInfo.getProperty("eclipse.startTime");
		if (ts != null) {
			try {
				return getDate(new Date(Long.parseLong(ts)));
			} catch (NumberFormatException e) {
				// fall through and use the timestamp from right now
			}
		}
		return getDate(new Date());
	}

	/**
	 * Writes the session
	 * @throws IOException if an error occurs writing to the log
	 */
	private void writeSession() throws IOException {
		write(SESSION);
		writeSpace();
		String date = getSessionTimestamp();
		write(date);
		writeSpace();
		for (int i = SESSION.length() + date.length(); i < 78; i++) {
			write("-"); //$NON-NLS-1$
		}
		writeln();
		// Write out certain values found in System.getProperties()
		try {
			String key = "eclipse.buildId"; //$NON-NLS-1$
			String value = environmentInfo.getProperty(key);
			writeln(key + "=" + value); //$NON-NLS-1$
			key = "java.fullversion"; //$NON-NLS-1$
			value = System.getProperty(key);
			if (value == null) {
				key = "java.version"; //$NON-NLS-1$
				value = System.getProperty(key);
				writeln(key + "=" + value); //$NON-NLS-1$
			} else {
				writeln(key + "=" + value); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// If we're not allowed to get the values of these properties
			// then just skip over them.
		}
		String osgiVer = Activator.getContext().getProperty("org.osgi.framework.version");
		if (null != osgiVer) {
			writeln ("org.osgi.framework.version=" + osgiVer);
		}
	}

	public void close() {
		try {
			if (writer != null) {
				Writer tmpWriter = writer;
				writer = null;
				tmpWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * If a File is used to log messages to then the File opened and a Writer is created
	 * to log messages to.
	 */
	private void openFile() {
		if (writer == null) {
			if (outFile != null) {
				try {
					writer = logForStream(secureAction.getFileOutputStream(outFile, true));
				} catch (IOException e) {
					writer = logForStream(System.err);
				}
			} else {
				writer = logForStream(System.err);
			}
		}
	}

	/**
	 * If a File is used to log messages to then the writer is closed.
	 */
	private void closeFile() {
		if (outFile != null) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// we cannot log here; just print the stacktrace.
					e.printStackTrace();
				}
				writer = null;
			}
		}
	}
// --- Begin writing TraceLogEntry --
	
	/**
	 * Bypass the listener and use this to log directly to the log 
	 * 
	 * @param traceLogEntry log entry for bundles
	 */
	public synchronized void log(BundleLogEntryImpl traceLogEntry) {
		if (traceLogEntry == null)
			return;
		if (!isLoggable(traceLogEntry.getSeverity()))
			return;
		try {
			checkLogFileSize();
			openFile();
			if (newSession) {
				writeSession();
				newSession = false;
			}
			writeLog(0, traceLogEntry);
			writer.flush();
		} catch (Exception e) {
			// any exceptions during logging should be caught 
			System.err.println("An exception occurred while writing to the bundle log:");//$NON-NLS-1$
			e.printStackTrace(System.err);
			System.err.println("Logging to the console instead.");//$NON-NLS-1$
			//we failed to write, so dump log entry to console instead
			try {
				writer = logForStream(System.err);
				writeLog(0, traceLogEntry);
				writer.flush();
			} catch (Exception e2) {
				System.err.println("An exception occurred while logging to the console:");//$NON-NLS-1$
				e2.printStackTrace(System.err);
			}
		} finally {
			closeFile();
		}
	}

	/**
 * Writes the log entry to the log using the specified depth.  A depth value of 0
 * indicates that the log entry is the root entry.  Any value greater than 0 indicates
 * a sub-entry.
 * @param depth the depth of the entry
 * @param entry the entry to log
 * @throws IOException if any error occurs writing to the log
 */
	private void writeLog(int depth, BundleLogEntryImpl entry) throws IOException {

		writeEntry(depth, entry);
		writeMessage(entry);
		// Moved to Entry writeState(entry);
		writeStack(entry);
		Object[] children =  entry.getChildren(entry);		
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				Object logEntry = children[i];
				if (logEntry instanceof BundleLogEntryImpl) {
					writeLog(depth + 1, (BundleLogEntryImpl) logEntry);
				}
			}
		}
	}

	/**
	 * Writes the ENTRY or SUBENTRY header for an entry.  A depth value of 0
	 * indicates that the log entry is the root entry.  Any value greater than 0 indicates
	 * a sub-entry.
	 * @param depth the depth of th entry
	 * @param entry the entry to write the header for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeEntry(int depth, BundleLogEntryImpl entry) throws IOException {
		if (depth == 0) {
			writeln(); // write a blank line before all !ENTRY tags bug #64406
			write(ENTRY);
		} else {
			write(SUBENTRY);
			writeSpace();
			write(Integer.toString(depth));
		}
		writeSpace();
		write(entry.getPluginId());
		writeSpace();
		write(Integer.toString(entry.getSeverity()));
		writeSpace();
		write(Integer.toString(entry.getCode()));
		writeSpace();
		write(Integer.toString(entry.getBundleStateId()));
		writeSpace();
		write(getDate(new Date()));
		writeln();
	}
	
	/**
	 * Writes the MESSAGE header to the log for the given entry.
	 * @param entry the entry to write the message for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeMessage(BundleLogEntryImpl entry) throws IOException {
		write(MESSAGE);
		writeSpace();
		writeln(entry.getMessage());
	}

	/**
	 * Writes the STATE header to the log for the given entry.
	 * @param entry the entry to write the bundle state for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeState(BundleLogEntryImpl entry) throws IOException {
		if (entry.getBundleState().length() > 0) {
			write(STATE);
			writeSpace();
			writeln(entry.getBundleState());
		}
	}

	/**
	 * Writes the STACK header to the log for the given entry.
	 * @param entry the entry to write the stacktrace for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeStack(BundleLogEntryImpl entry) throws IOException {
		Throwable t = entry.getThrowable();
		if (t != null) {
			String stack = getStackTrace(t);
			write(STACK);
			writeSpace();
			write(Integer.toString(0 /*entry.getStackCode() */));
			writeln();
			write(stack);
		}
	}

	


	public synchronized void setWriter(Writer newWriter, boolean append) {
		setOutput(null, newWriter, append);
	}

	/**
	 * @throws IOException  
	 */
	public synchronized void setFile(File newFile, boolean append) throws IOException {
		if (newFile != null && !newFile.equals(this.outFile)) {
			// If it's a new file, then reset.
			readLogProperties();
			backupIdx = 0;
		}
		setOutput(newFile, null, append);
		Activator.getDefault().setLogFile(newFile);
		// environmentInfo.setConfiguration(EclipseStarter.PROP_LOGFILE, newFile == null ? "" : newFile.getAbsolutePath()); //$NON-NLS-1$
	}

	public synchronized File getFile() {
		return outFile;
	}

	public void setConsoleLog(boolean consoleLog) {
		this.consoleLog = consoleLog;
	}

	private void setOutput(File newOutFile, Writer newWriter, boolean append) {
		if (newOutFile == null || !newOutFile.equals(this.outFile)) {
			if (this.writer != null) {
				try {
					this.writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.writer = null;
			}
			// Append old outFile to newWriter. We only attempt to do this
			// if the current Writer is backed by a File and this is not
			// a new session.
			File oldOutFile = this.outFile;
			this.outFile = newOutFile;
			this.writer = newWriter;
			boolean copyFailed = false;
			if (append && oldOutFile != null && oldOutFile.isFile()) {
				Reader fileIn = null;
				try {
					openFile();
					fileIn = new InputStreamReader(secureAction.getFileInputStream(oldOutFile), "UTF-8"); //$NON-NLS-1$
					copyReader(fileIn, this.writer);
				} catch (IOException e) {
					copyFailed = true;
					e.printStackTrace();
				} finally {
					if (fileIn != null) {
						try {
							fileIn.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						// delete the old file if copying didn't fail
						if (!copyFailed)
							oldOutFile.delete();
					}
					closeFile();
				}
			}
		}
	}

	private void copyReader(Reader reader, Writer aWriter) throws IOException {
		char buffer[] = new char[1024];
		int count;
		while ((count = reader.read(buffer, 0, buffer.length)) > 0) {
			aWriter.write(buffer, 0, count);
		}
	}

	/**
	 * Returns a date string using the correct format for the log.
	 * @param date the Date to format
	 * @return a date string.
	 */
	private String getDate(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		StringBuffer sb = new StringBuffer();
		appendPaddedInt(c.get(Calendar.YEAR), 4, sb).append('-');
		appendPaddedInt(c.get(Calendar.MONTH) + 1, 2, sb).append('-');
		appendPaddedInt(c.get(Calendar.DAY_OF_MONTH), 2, sb).append(' ');
		appendPaddedInt(c.get(Calendar.HOUR_OF_DAY), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.MINUTE), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.SECOND), 2, sb).append('.');
		appendPaddedInt(c.get(Calendar.MILLISECOND), 3, sb);
		return sb.toString();
	}

	private StringBuffer appendPaddedInt(int value, int pad, StringBuffer buffer) {
		pad = pad - 1;
		if (pad == 0)
			return buffer.append(Integer.toString(value));
		int padding = (int) Math.pow(10, pad);
		if (value >= padding)
			return buffer.append(Integer.toString(value));
		while (padding > value && padding > 1) {
			buffer.append('0');
			padding = padding / 10;
		}
		buffer.append(value);
		return buffer;
	}

	/**
	 * Returns a stacktrace string using the correct format for the log
	 * @param t the Throwable to get the stacktrace for
	 * @return a stacktrace string
	 */
	private String getStackTrace(Throwable t) {
		if (t == null)
			return null;

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		t.printStackTrace(pw);
		// ensure the root exception is fully logged
		Throwable root = getRoot(t);
		if (root != null) {
			pw.println("Root exception:"); //$NON-NLS-1$
			root.printStackTrace(pw);
		}
		return sw.toString();
	}

	/**
	 * Returns a Writer for the given OutputStream
	 * @param output an OutputStream to use for the Writer
	 * @return a Writer for the given OutputStream
	 */
	private Writer logForStream(OutputStream output) {
		try {
			return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new BufferedWriter(new OutputStreamWriter(output));
		}
	}

	private void writeState(FrameworkLogEntry entry) throws IOException {
		write(STATE);
		writeSpace();
		//writeln(Integer.toString(entry.);
	}

	/**
	 * Writes the given message to the log.
	 * @param message the message
	 * @throws IOException if any error occurs writing to the log
	 */
	private void write(String message) throws IOException {
		if (message != null) {
			writer.write(message);
			if (consoleLog)
				System.out.print(message);
		}
	}

	/**
	 * Writes the given message to the log and a newline.
	 * @param s the message
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeln(String s) throws IOException {
		write(s);
		writeln();
	}

	/**
	 * Writes a newline log.
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeln() throws IOException {
		write(LINE_SEPARATOR);
	}

	/**
	 * Writes a space to the log.
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeSpace() throws IOException {
		write(" "); //$NON-NLS-1$
	}

	/**
	 * Checks the log file size.  If the log file size reaches the limit then the log 
	 * is rotated
	 * @return false if an error occurred trying to rotate the log
	 */
	private boolean checkLogFileSize() {
		if (maxLogSize == 0)
			return true; // no size limitation.

		boolean isBackupOK = true;
		if (outFile != null) {
			if ((secureAction.length(outFile) >> 10) > maxLogSize) { // Use KB as file size unit.
				String logFilename = outFile.getAbsolutePath();

				// Delete old backup file that will be replaced.
				String backupFilename = ""; //$NON-NLS-1$
				if (logFilename.toLowerCase().endsWith(LOG_EXT)) {
					backupFilename = logFilename.substring(0, logFilename.length() - LOG_EXT.length()) + BACKUP_MARK + backupIdx + LOG_EXT;
				} else {
					backupFilename = logFilename + BACKUP_MARK + backupIdx;
				}
				File backupFile = new File(backupFilename);
				if (backupFile.exists()) {
					if (!backupFile.delete()) {
						System.err.println("Error when trying to delete old log file: " + backupFile.getName());//$NON-NLS-1$ 
						if (backupFile.renameTo(new File(backupFile.getAbsolutePath() + System.currentTimeMillis()))) {
							System.err.println("So we rename it to filename: " + backupFile.getName()); //$NON-NLS-1$
						} else {
							System.err.println("And we also cannot rename it!"); //$NON-NLS-1$
							isBackupOK = false;
						}
					}
				}

				// Rename current log file to backup one.
				boolean isRenameOK = outFile.renameTo(backupFile);
				if (!isRenameOK) {
					System.err.println("Error when trying to rename log file to backup one."); //$NON-NLS-1$
					isBackupOK = false;
				}
				File newFile = new File(logFilename);
				setOutput(newFile, null, false);

				// Write a new SESSION header to new log file.
				openFile();
				try {
					writeSession();
					writeln();
					writeln("This is a continuation of log file " + backupFile.getAbsolutePath());//$NON-NLS-1$
					writeln("Created Time: " + getDate(new Date(System.currentTimeMillis()))); //$NON-NLS-1$
					writer.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace(System.err);
				}
				closeFile();
				backupIdx = (++backupIdx) % maxLogFiles;
			}
		}
		return isBackupOK;
	}

	/**
	 * Reads the PROP_LOG_SIZE_MAX and PROP_LOG_FILE_MAX properties.
	 */
	private void readLogProperties() {
		String newMaxLogSize = environmentInfo.getProperty(PROP_LOG_SIZE_MAX);
		if (newMaxLogSize != null) {
			maxLogSize = Integer.parseInt(newMaxLogSize);
			if (maxLogSize != 0 && maxLogSize < LOG_SIZE_MIN) {
				// If the value is '0', then it means no size limitation.
				// Also, make sure no inappropriate(too small) assigned value.
				maxLogSize = LOG_SIZE_MIN;
			}
		}

		String newMaxLogFiles = environmentInfo.getProperty(PROP_LOG_FILE_MAX);
		if (newMaxLogFiles != null) {
			maxLogFiles = Integer.parseInt(newMaxLogFiles);
			if (maxLogFiles < 1) {
				// Make sure no invalid assigned value. (at least >= 1)
				maxLogFiles = DEFAULT_LOG_FILES;
			}
		}

		String newLogLevel = environmentInfo.getProperty(PROP_LOG_LEVEL);
		if (newLogLevel != null) {
			if (newLogLevel.equals("MODULAR_REFRESH_ERROR")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.ERROR;
			else if (newLogLevel.equals("WARNING")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.ERROR | FrameworkLogEntry.WARNING;
			else if (newLogLevel.equals("INFO")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.INFO | FrameworkLogEntry.ERROR | FrameworkLogEntry.WARNING | FrameworkLogEntry.CANCEL;
			else
				logLevel = FrameworkLogEntry.OK; // OK (0) means log everything
		}

		includeCommandLine = "true".equals(environmentInfo.getProperty(PROP_LOG_INCLUDE_COMMAND_LINE));
	}

	/**
	 * Determines if the log entry should be logged based on log level.
	 */
	private boolean isLoggable(int fwkEntrySeverity) {
		if (logLevel == 0)
			return true;
		return (fwkEntrySeverity & logLevel) != 0;
	}

	public boolean isLoggable(Bundle bundle, String loggableName, int loggableLevel) {
		if (!enabled) {
			return false;
		}
		return loggerName.equals(loggableName);
		//return Activator.BUNDLE_LOGGER_NAME.equals(loggerName);
	}

	@Override
	public void logged(LogEntry entry) {
		if (!(entry instanceof ExtendedLogEntry))
			return;
		ExtendedLogEntry extended = (ExtendedLogEntry) entry;
		Object context = extended.getContext();
		if (context instanceof IBundleStatus) {
			IBundleStatus s = (IBundleStatus) context;
			s.setHighestStatusCode();
			convertServerity(s);
			log(new BundleLogEntryImpl((IBundleStatus) context));
			return;
		}
		if (context instanceof BundleLogEntryImpl) {
			log((BundleLogEntryImpl) context);
			return;
		}
	}
	
	public void convertServerity(IBundleStatus status) {
		
		status.convertSeverity();
		for (IStatus childStatus : status.getChildren()) {
			if (childStatus instanceof IBundleStatus) {
				IBundleStatus bundleStatus = (IBundleStatus) childStatus;
				convertServerity(bundleStatus);
			}
		}		
	}
	
	private static int convertSeverity(int entryLevel) {
		switch (entryLevel) {
			case LogService.LOG_ERROR :
				return FrameworkLogEntry.ERROR;
			case LogService.LOG_WARNING :
				return FrameworkLogEntry.WARNING;
			case LogService.LOG_INFO :
				return FrameworkLogEntry.INFO;
			case LogService.LOG_DEBUG :
				return FrameworkLogEntry.OK;
			default :
				return 32; // unknown
		}
	}

	public static int getLevel(IBundleStatus status) {
		switch (status.getSeverity()) {
			case IStatus.ERROR :
				return LogService.LOG_ERROR;
			case IStatus.WARNING :
				return LogService.LOG_WARNING;
			case IStatus.INFO :
				return LogService.LOG_INFO;
			case IStatus.OK :
				return LogService.LOG_DEBUG;
			case IStatus.CANCEL :
			default :
				return 32; // unknown
		}
	}
	

	public String getLoggerName() {
		return loggerName;
	}
}
