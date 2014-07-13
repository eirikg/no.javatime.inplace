package no.javatime.inplace.log.impl;

import no.javatime.inplace.log.Activator;
import no.javatime.inplace.log.dl.LogWriter;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.equinox.log.Logger;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;

public class BundleLogimpl implements BundleLog {
	
	public BundleLogimpl() {
	}
	
//	private Output convertOutput(Device device) {
//		
//		Output out; 
//		
//		switch (device) {
//		case CONSOLE: {
//			out = Output.console;
//			break;
//		}
//		case LOG: {
//			out = Output.log;
//			break;
//		}
//		case VIEW: {
//			out = Output.view;
//			break;
//		}
//		case VIEW_AND_CONSOLE: {
//			out = Output.viewAndConsole;
//			break;
//		}
//		case VIEW_AND_LOG: {
//			out = Output.viewAndLog;
//			break;
//		}
//		case CONSOLE_AND_LOG: {
//			out = Output.consoleAndLog;
//			break;
//		}
//		case VIEW_AND_LOG_AND_CONSOLE: {
//			out = Output.viewAndLogAndConsole;
//			break;
//		}
//		case NIL:			
//		default:
//			out = Output.nil;
//		}
//		return out;
//	}
//	
//	public void setOut(MessageType messageType, Device device) {
//		Output output = convertOutput(device);
//		switch (messageType) {
//		case MESSAGE: {
//			Message.getInstance().setOutput(output);
//			break;
//		}
//		case TRACE: {
//			TraceMessage.getInstance().setOutput(output);
//			break;
//		}
//		case USER: {
//			UserMessage.getInstance().setOutput(output);
//			break;
//		}
//		case EXCEPTION: {
//			ExceptionMessage.getInstance().setOutput(output);
//			break;
//		}
//		case ERROR: {
//			ErrorMessage.getInstance().setOutput(output);
//			break;
//		}
//		case WARNING: {
//			WarnMessage.getInstance().setOutput(output);
//			break;
//		}
//		default:
//			Message.getInstance().setOutput(output);
//		}
//	}

	private String log(IBundleStatus status) {
		Bundle bundle = status.getBundle();
		if (null == bundle) {
			bundle = Activator.getContext().getBundle();
		}
		String msg = status.getMessage();
		// Do not use the ExtendedLogReaderService while the workbench is closing
		// It logs the root message (in the log listener) to the error log in addition
		// to the trace status object to be logged
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
	
	@Override
	public String trace(IBundleStatus status) {
		return log(status);
	}

	public String message(IBundleStatus status) {
		return log(status);
	}

	public String user(IBundleStatus status) {
		return log(status);
	}
	
	public String exception(IBundleStatus status) {
		return log(status);
	}

	public String warning(IBundleStatus status) {
		return log(status);
	}

	public String error(IBundleStatus status) {
		return log(status);
	}
}
