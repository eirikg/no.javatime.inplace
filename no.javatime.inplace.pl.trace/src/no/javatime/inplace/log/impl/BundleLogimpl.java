package no.javatime.inplace.log.impl;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.Activator;
import no.javatime.inplace.log.dl.LogWriter;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.log.intface.BundleLogException;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.log.msg.Messages;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.log.Logger;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleLogimpl implements BundleLog {


	// The root of the bundle status tree
	private IBundleStatus rootStatus = null;
	// The current position in the root bundle status tree
	private IBundleStatus currStatus = null;
	/* internal object to use for synchronization */
	private final Object statusLock = new Object();

	public BundleLogimpl() {
	}

	@Override
	public String log(IBundleStatus status) throws BundleLogException {

		return logStatus(status);
	}

	@Override
	public String log(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, exception));
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public void log(StatusCode statusCode, Bundle bundle, Exception exception, String msg)
			throws BundleLogException {
		try {

			logStatus(new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, exception));
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
	}

	@Override
	public void log() throws BundleLogException {
		
		synchronized (statusLock) {
			if (null != rootStatus) {
				logStatus(rootStatus);
				rootStatus = currStatus = null;
			} else {
				throw new BundleLogException(Messages.NULL_ROOT_STATUS_EXP);
			}
		}
	}

	@Override
	public String add(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			add(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public void add(StatusCode statusCode, Bundle bundle, Exception exception, String msg) {

		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// This is not the first status object added
			if (!currStatus.equals(nextStatus)) {
				currStatus.add(nextStatus);
				currStatus = nextStatus;
			}
		}
	}

	@Override
	public String addParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addParent(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public void addParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) throws BundleLogException {

		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// Current status is root but not the first status object added
			if (currStatus.equals(rootStatus) && !currStatus.equals(nextStatus)) {
				// Adding a status object as parent to root adds this status object as the new root
				nextStatus.add(rootStatus);
				rootStatus = nextStatus;
			} else if (!currStatus.equals(nextStatus)) {
				IBundleStatus prevStatus = currStatus;
				setParent(rootStatus, null);
				if (currStatus.equals(rootStatus)) {
					currStatus = prevStatus;
					throw new BundleLogException(Messages.ADD_PARENT_EXP);
				}
				setParent(rootStatus, null);
				currStatus.add(nextStatus);
				currStatus = nextStatus;
			}
		}
	}

	public String addToParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addToParent(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public void addToParent(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) throws BundleLogException {

		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// This is not the first status object added
			if (!currStatus.equals(nextStatus)) {
				currStatus.add(nextStatus);
			}
		}
	}

	@Override
	public String addSibling(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addSibling(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public void addSibling(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) {
		
		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// Current status is root but not the first status object added
			if (currStatus.equals(rootStatus) && !currStatus.equals(nextStatus)) {
				throw new BundleLogException(Messages.ADD_SIBLING_EXP);
			} else if (!currStatus.equals(nextStatus)) {
				setParent(rootStatus, null);
				currStatus.add(nextStatus);
				currStatus = nextStatus;
			}
		}
	}

	@Override
	public String addRoot(StatusCode statusCode, Bundle bundle, Exception exception, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addRoot(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public void addRoot(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) {
		
		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// Current status is not root
			if (!currStatus.equals(nextStatus)) {
				nextStatus.add(rootStatus);
				rootStatus = currStatus = nextStatus;
			}
		}
	}

	@Override
	public boolean isRoot() {

		synchronized (statusLock) {
			if (null != rootStatus) {
				return rootStatus.equals(currStatus) ? true : false;
			} else {
				return false;
			}
		}
	}
		

	@Override
	public String addToRoot(StatusCode statusCode, Bundle bundle, Exception exception,
			String pattern, Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addToRoot(statusCode, bundle, exception, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		} catch (IllegalStateException e) {
			String errMsg = NLS.bind(Messages.INVALID_CONTEXT_EXP, Activator.PLUGIN_ID);
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public void addToRoot(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) {

		synchronized (statusLock) {
			IBundleStatus nextStatus = createStatus(statusCode, bundle, exception, msg);
			// Current status is not root
			if (!currStatus.equals(nextStatus)) {
				rootStatus.add(nextStatus);
				currStatus = nextStatus;
			}
		}
	}

	@Override
	public boolean clear() {
		
		synchronized (statusLock) {
			if (null != rootStatus) {
				rootStatus = currStatus = null;
				return true;
			}
			return false;
		}
	}

	@Override
	public int size() {

		synchronized (statusLock) {
			return null == rootStatus ? 0 : size(rootStatus, new AtomicInteger(0)).intValue();
		}
	}

	/**
	 * Counts the number of status objects contained in the specified bundle status object
	 * 
	 * @param status a status object in the status tree. Must not be null
	 * @param count The initial number of status objects before counting. Typically zero.
	 * @return number of status objects in tree the including the specified status object.
	 */
	private AtomicInteger size(IBundleStatus status, AtomicInteger count) {

		count.incrementAndGet();
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			size((IBundleStatus) children[i], count);
		}
		return count;
	}

	/**
	 * Creates a new status object and returns it. If this is the first status object, the root,
	 * current and the returned status object are set equal to the created status object.
	 * 
	 * This private method should only be called from within a synchronized block/method
	 * 
	 * @param statusCode the status code to log
	 * @param bundle logs the bundle symbolic name and bundle state
	 * @param exception the exception to log
	 * @param msg the message to log
	 * @return the newly created status object
	 */
	private IBundleStatus createStatus(StatusCode statusCode, Bundle bundle, Exception exception,
			String msg) {

		String symbolicname = null == bundle ? Activator.PLUGIN_ID : bundle.getSymbolicName();
		IBundleStatus nextStatus = new BundleStatus(statusCode, symbolicname, bundle, msg, exception);
		if (null == rootStatus) {
			rootStatus = currStatus = nextStatus;
		}
		return nextStatus;
	}

	/**
	 * Change the current status object to the parent status object
	 * <p>
	 * Search the status object tree for the current status object beginning with the specified status
	 * parameter and assign the the parent status object to the current status object
	 * <p>
	 * To find a match the specified status parameter must not be null and a current status object
	 * must exist as a child to the specified status object.
	 * <p>
	 * This private method should only be called from within a synchronized block/method
	 * 
	 * @param status a potential current status object
	 * @param parent the parent status object to the specified status object parameter
	 */
	private void setParent(IBundleStatus status, IBundleStatus parent) {

		if (currStatus.equals(status)) {
			currStatus = parent;
			return;
		}
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			setParent((IBundleStatus) children[i], status);
		}
	}

	@Override
	public boolean enableLogging(boolean logging) throws ExtenderException {

		Extender<MessageOptions> msgOptions = Extenders.getExtender(MessageOptions.class.getName());
		if (null == msgOptions) {
			throw new ExtenderException(
					NLS.bind(Messages.GET_SERVICE_EXP, MessageOptions.class.getName()));
		}
		MessageOptions msgOptionsService = msgOptions.getService();
		if (null == msgOptionsService) {
			throw new ExtenderException(
					NLS.bind(Messages.GET_SERVICE_EXP, MessageOptions.class.getName()));
		}
		boolean isLogging = msgOptionsService.isBundleEvents();
		msgOptionsService.setIsBundleEvents(logging);
		return isLogging;
	}

	@Override
	public BundleLogView getBundleLogViewService(Bundle bundle) throws ExtenderException {

		Extender<BundleLogView> extender = Extenders.getExtender(BundleLogView.class.getName());
		if (null == extender) {
			Bundle thisBundle = Activator.getContext().getBundle();
			String bundleLogViewSvcName = thisBundle.getHeaders().get(BundleLogView.BUNDLE_LOG_VIEW_SERVICE);
			if (null == bundleLogViewSvcName) {
				throw new ExtenderException(NLS.bind(Messages.GET_SERVICE_EXP,
						BundleLogView.class.getName()));
			}
			extender = Extenders.register(thisBundle, bundle, BundleLogView.class.getName(),
					bundleLogViewSvcName, null);
		}
		BundleLogView blv = extender.getService(bundle);
		if (null == blv) {
			throw new ExtenderException(NLS.bind(Messages.GET_SERVICE_EXP, BundleLogView.class.getName()));
		}
		return blv;
	}

	/**
	 * Logs the next status object.
	 * 
	 * @param status status object to log
	 * @return the message held by the status object
	 */
	private String logStatus(IBundleStatus status) throws IllegalStateException {

		Bundle bundle = status.getBundle();
		if (null == bundle) {
			BundleContext context = Activator.getContext();
			if (null != context) {
				bundle = context.getBundle();
			} else {
				throw new IllegalStateException(NLS.bind(Messages.NULL_CONTEXT_EXP, Activator.PLUGIN_ID));
			}
		}
		String msg = status.getMessage();
		// Do not use the ExtendedLogReaderService while the workbench is closing
		// It logs the root message (in the log listener) to the error log in addition
		// to the log status object to be logged
		Activator activator = Activator.getDefault();
		IWorkbench workbench = activator.getWorkbench();
		if (null != workbench && workbench.isClosing()) {
			// Write directly to the log file
			LogWriter logWriter = activator.getLogWriter();
			if (null != logWriter) {
				logWriter.log(new BundleLogEntryImpl(status));
			}
		} else {
			Logger logger = activator.getLogger(bundle);
			logger.log(status, LogWriter.getLevel(status), msg, status.getException());
		}
		return msg;
	}
}
