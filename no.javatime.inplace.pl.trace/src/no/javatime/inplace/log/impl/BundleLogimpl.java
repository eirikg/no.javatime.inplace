package no.javatime.inplace.log.impl;

import java.text.MessageFormat;

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

public class BundleLogimpl implements BundleLog {

	public BundleLogimpl() {
	}

	// The current root of the bundle status tree
	private IBundleStatus rootStatus = null;
	// The current position in the bundle status tree
	private IBundleStatus currStatus = null;
	// The next status position. Becomes the current status after it has been added
	private IBundleStatus nextStatus = null;

	@Override
	public String log(IBundleStatus status) {

		return logStatus(status);
	}

	@Override
	public String logMsg(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public String logExp(Bundle bundle, Exception e, String pattern, Object... substitutions)
			throws BundleLogException {

		String msg = null;
		try {
			msg = null == pattern ? "" : MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.EXCEPTION, bundle.getSymbolicName(), bundle, msg, e));
		} catch (IllegalArgumentException exp) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public String logWarn(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.WARNING, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public String logErr(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.ERROR, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public String logInfo(Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.INFO, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	@Override
	public String add(StatusCode statusCode, Bundle bundle, String pattern, Object... substitutions)
			throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			add(statusCode, bundle, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public String add(StatusCode statusCode, Bundle bundle, String msg) {

		if (null != msg) {
			createStatus(statusCode, bundle, msg);
			if (currStatus != nextStatus) {
				currStatus.add(nextStatus);
				currStatus = nextStatus;
			}
		}
		return msg;
	}

	@Override
	public String addParent(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addParent(statusCode, bundle, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public String addParent(StatusCode statusCode, Bundle bundle, String msg) {

		createStatus(statusCode, bundle, msg);
		if (currStatus.equals(rootStatus) && !currStatus.equals(nextStatus)) {
			// Adding a nextStatus object as parent to root adds this nextStatus object as the new root
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
		return msg;
	}

	@Override
	public String addSibling(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addSibling(statusCode, bundle, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public String addSibling(StatusCode statusCode, Bundle bundle, String msg) {

		createStatus(statusCode, bundle, msg);
		// Current nextStatus is root and not first nextStatus object added
		if (currStatus.equals(rootStatus) && !currStatus.equals(nextStatus)) {
			throw new BundleLogException(Messages.ADD_SIBLING_EXP);
		} else if (!currStatus.equals(nextStatus)) {
			setParent(rootStatus, null);
			currStatus.add(nextStatus);
			currStatus = nextStatus;
		}
		return msg;
	}
	@Override
	public String addRoot(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addRoot(statusCode, bundle, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public String addRoot(StatusCode statusCode, Bundle bundle, String msg) {

		createStatus(statusCode, bundle, msg);
		// Current nextStatus is root and not first nextStatus object added
		if (!currStatus.equals(nextStatus)) {
			nextStatus.add(rootStatus);
			rootStatus = nextStatus;
		}
		return msg;
	}

	@Override
	public String addToRoot(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) throws BundleLogException {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			addToRoot(statusCode, bundle, msg);
		} catch (IllegalArgumentException e) {
			String errMsg = pattern != null ? NLS.bind(Messages.FORMAT_MSG_EXP, pattern)
					: Messages.FORMAT_ARG_EXP;
			throw new BundleLogException(e, errMsg);
		}
		return msg;
	}

	public String addToRoot(StatusCode statusCode, Bundle bundle, String msg) {

		createStatus(statusCode, bundle, msg);
		if (!currStatus.equals(nextStatus)) {
			rootStatus.add(nextStatus);
			currStatus = nextStatus;
		}
		return msg;
	}

	@Override
	public boolean clear() {
		if (null != rootStatus) {
			rootStatus = currStatus = nextStatus = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean log() {
		if (null != rootStatus) {
			log(rootStatus);
			rootStatus = currStatus = nextStatus = null;
			return true;
		}
		return false;
	}

	private String createStatus(StatusCode statusCode, Bundle bundle, String msg) {

		nextStatus = new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null);
		if (null == rootStatus) {
			rootStatus = currStatus = nextStatus;
		}
		return msg;
	}

	/**
	 * Change the current nextStatus object to the parent nextStatus object
	 * <p>
	 * Search the nextStatus object tree beginning with the specified nextStatus parameter for the
	 * current nextStatus in the tree and assign the current nextStatus to the parent nextStatus
	 * object
	 * <p>
	 * The specified nextStatus parameter must not be null and a current nextStatus object must exist.
	 * 
	 * @param nextStatus a potential current nextStatus object
	 * @param parent the parent nextStatus object to the specified nextStatus object parameter
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
			String bundleLogViewSvcName = thisBundle.getHeaders().get(BundleLogView.BUNDLE_LOG_VIEW_IMPL);
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
	 * Logs the nextStatus object.
	 * 
	 * @param nextStatus nextStatus object to log
	 * @return the message held by the nextStatus object
	 */
	private String logStatus(IBundleStatus status) {

		Bundle bundle = status.getBundle();
		if (null == bundle) {
			bundle = Activator.getContext().getBundle();
		}
		String msg = status.getMessage();
		// Do not use the ExtendedLogReaderService while the workbench is closing
		// It logs the root message (in the log listener) to the error log in addition
		// to the log nextStatus object to be logged
		IWorkbench workbench = Activator.getDefault().getWorkbench();
		if (null != workbench && workbench.isClosing()) {
			// Write directly to the log file
			LogWriter logWriter = Activator.getDefault().getLogWriter();
			if (null != logWriter) {
				logWriter.log(new BundleLogEntryImpl(status));
			}
		} else {
			Logger logger = Activator.getDefault().getLogger(bundle);
			logger.log(status, LogWriter.getLevel(status), msg, status.getException());
		}
		return msg;
	}
}
