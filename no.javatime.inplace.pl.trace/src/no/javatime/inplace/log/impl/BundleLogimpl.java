package no.javatime.inplace.log.impl;

import java.text.MessageFormat;

import no.javatime.inplace.dl.preferences.intface.MessageOptions;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.Activator;
import no.javatime.inplace.log.dl.LogWriter;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.log.msg.Messages;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.equinox.log.Logger;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

public class BundleLogimpl implements BundleLog {

	public BundleLogimpl() {
	}

	IBundleStatus multiStatus = null;
	
	@Override
	public String log(IBundleStatus status) {

		return logStatus(status);
	}

	@Override
	public String logMsg(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String logExp(Bundle bundle, Exception e, String pattern, Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.EXCEPTION, bundle.getSymbolicName(), bundle, msg, e));
		} catch (IllegalArgumentException exp) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, exp);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String logWarn(Bundle bundle, String pattern, Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.WARNING, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String logErr(Bundle bundle, String pattern, Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.ERROR, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String logInfo(Bundle bundle, String pattern, Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			logStatus(new BundleStatus(StatusCode.INFO, bundle.getSymbolicName(), bundle, msg, null));
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String add(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			if (null == multiStatus) {
				multiStatus = new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null);
			} else {
				multiStatus.add(new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null));
			}
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public String addParent(StatusCode statusCode, Bundle bundle, String pattern,
			Object... substitutions) {

		String msg = null;
		try {
			msg = MessageFormat.format(pattern, substitutions);
			IBundleStatus status = new BundleStatus(statusCode, bundle.getSymbolicName(), bundle, msg, null);
			if (null == multiStatus) {
				multiStatus = status;
			} else {
				status.add(multiStatus);
				multiStatus = status;
			}
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(Messages.FORMAT_MSG_EXP, pattern);
			Bundle srcBundle = Activator.getContext().getBundle();
			IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, srcBundle.getSymbolicName(), srcBundle, errMsg, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
		return msg;
	}

	@Override
	public boolean clear() {
		if (null != multiStatus) {
			multiStatus = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean log() {
		if (null != multiStatus) {
			log(multiStatus);
			multiStatus = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean enableLogging(boolean logging) throws ExtenderException {

		Extender<MessageOptions> msgOptions = Extenders.getExtender(MessageOptions.class.getName());
		if (null == msgOptions) {
			throw new ExtenderException(NLS.bind(Messages.GET_SERVICE_EXP, MessageOptions.class.getName()));
		}
		MessageOptions msgOptionsService = msgOptions.getService();
		if (null == msgOptionsService) {
			throw new ExtenderException(NLS.bind(Messages.GET_SERVICE_EXP, MessageOptions.class.getName()));
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
				throw new ExtenderException(NLS.bind(Messages.GET_SERVICE_EXP, BundleLogView.class.getName()));
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
	 * Logs the status object.
	 * 
	 * @param status status object to log
	 * @return the message held by the status object
	 */
	private String logStatus(IBundleStatus status) {

		Bundle bundle = status.getBundle();
		if (null == bundle) {
			bundle = Activator.getContext().getBundle();
		}
		String msg = status.getMessage();
		// Do not use the ExtendedLogReaderService while the workbench is closing
		// It logs the root message (in the log listener) to the error log in addition
		// to the log status object to be logged
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
