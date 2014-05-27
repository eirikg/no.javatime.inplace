package no.javatime.inplace.pl.trace.impl;

import no.javatime.inplace.pl.trace.Activator;
import no.javatime.inplace.pl.trace.intface.Trace;
import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.Message.Output;
import no.javatime.util.messages.TraceMessage;
import no.javatime.util.messages.UserMessage;
import no.javatime.util.messages.WarnMessage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

public class TraceImpl implements Trace {
	
	public TraceImpl() {
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

	private String setInput(IStatus status) {
		String bundleName = status.getPlugin();
		Bundle b = Platform.getBundle(bundleName);

		Bundle[] bundles = Platform.getBundles(bundleName, null);
		if (null != bundles && bundles.length > 0) {
//			Bundle bundle =Activator.getContext().getBundle();
			Logger logger = Activator.getDefault().getLogger(bundles[0]);
			logger.log(LogService.LOG_INFO, status.getMessage());
		}
		return status.getMessage();
	}
	
	@Override
	public String trace(IStatus status) {
		return setInput(status);
	}

	public String message(IStatus status) {
		return setInput(status);
	}

	public String user(IStatus status) {
		return setInput(status);
	}
	
	public String exception(IStatus status) {
		return setInput(status);
	}

	public String warning(IStatus status) {
		return setInput(status);
	}

	public String error(IStatus status) {
		return setInput(status);
	}
}
