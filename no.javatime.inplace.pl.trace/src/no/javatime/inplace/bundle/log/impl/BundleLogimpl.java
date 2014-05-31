package no.javatime.inplace.bundle.log.impl;

import no.javatime.inplace.bundle.log.Activator;
import no.javatime.inplace.bundle.log.dl.LogWriter;
import no.javatime.inplace.bundle.log.intface.BundleLog;
import no.javatime.inplace.bundle.log.status.IBundleStatus;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.Message.Output;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;

public class BundleLogimpl implements BundleLog {
	
	public BundleLogimpl() {
	}
	
	private Output convertOutput(Device device) {
		
		Output out; 
		
		switch (device) {
		case CONSOLE: {
			out = Output.console;
			break;
		}
		case LOG: {
			out = Output.log;
			break;
		}
		case VIEW: {
			out = Output.view;
			break;
		}
		case VIEW_AND_CONSOLE: {
			out = Output.viewAndConsole;
			break;
		}
		case VIEW_AND_LOG: {
			out = Output.viewAndLog;
			break;
		}
		case CONSOLE_AND_LOG: {
			out = Output.consoleAndLog;
			break;
		}
		case VIEW_AND_LOG_AND_CONSOLE: {
			out = Output.viewAndLogAndConsole;
			break;
		}
		case NIL:			
		default:
			out = Output.nil;
		}
		return out;
	}
	
	public void setOut(MessageType messageType, Device device) {
		Output output = convertOutput(device);
		switch (messageType) {
		case MESSAGE: {
			Message.getInstance().setOutput(output);
			break;
		}
		case TRACE: {
			TraceMessage.getInstance().setOutput(output);
			break;
		}
		case USER: {
			UserMessage.getInstance().setOutput(output);
			break;
		}
		case EXCEPTION: {
			ExceptionMessage.getInstance().setOutput(output);
			break;
		}
		case ERROR: {
			ErrorMessage.getInstance().setOutput(output);
			break;
		}
		case WARNING: {
			WarnMessage.getInstance().setOutput(output);
			break;
		}
		default:
			Message.getInstance().setOutput(output);
		}
	}

	private String setInput(IBundleStatus status) {
		Bundle bundle = status.getBundle();
		if (null == bundle) {
			bundle = Activator.getContext().getBundle();
		}
		Logger logger = Activator.getDefault().getLogger(bundle);
		String msg = status.getMessage();
		logger.log(status, LogWriter.getLevel(status), msg, status.getException());
		return msg;
	}
	
	@Override
	public String trace(IBundleStatus status) {
		return setInput(status);
	}

	public String message(IBundleStatus status) {
		return setInput(status);
	}

	public String user(IBundleStatus status) {
		return setInput(status);
	}
	
	public String exception(IBundleStatus status) {
		return setInput(status);
	}

	public String warning(IBundleStatus status) {
		return setInput(status);
	}

	public String error(IBundleStatus status) {
		return setInput(status);
	}
}
