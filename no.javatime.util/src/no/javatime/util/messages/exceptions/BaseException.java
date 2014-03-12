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
package no.javatime.util.messages.exceptions;

/**
 * Utility and convenience methods for use by exception classes sub classing
 * this class.  
 *
 */
public abstract class BaseException extends RuntimeException {

	public BaseException() {
  	super();
  }

  public BaseException(String message) {
  	super(message);
  }
  
  public BaseException(String message, Throwable tex) {
  	super(message, tex);
  }

  public BaseException(Throwable tex) {
  	super(tex);
  }

  /**
	 * Unique ID of the class
	 */
	public static String ID = BaseException.class.getName();

	/**
	 * 
	 */
	private static final long serialVersionUID = -2366610141240698788L;

	/**
	 * Gets the stack frame of the caller. This is by best effort.
	 * 
	 * @return the relevant stack frame or null if no frame was found
	 */
	protected StackTraceElement getCallerMetaInfo() {

		// Get the stack trace.
		StackTraceElement stack[] = (new Throwable()).getStackTrace();
		// First, search back to a method in the one of the exception classes.
		int ix = 0;
		while (ix < stack.length) {
			StackTraceElement frame = stack[ix];
			String frameName = frame.getClassName();

			if (frameName.equals(ID) || frameName.equals(BaseException.ID)
					|| frameName.equals(LogException.ID)
					|| frameName.equals(ViewException.ID)) {
				break;
			}
			ix++;
		}
		// Search for the first frame before any of the exception classes.
		while (ix < stack.length) {
			StackTraceElement frame = stack[ix];
			String frameName = frame.getClassName();

			if (!frameName.equals(ID) && !frameName.equals(BaseException.ID)
					&& !frameName.equals(LogException.ID)
					&& !frameName.equals(ViewException.ID)) {
				// Found a relevant frame.
				return frame;
			}
			ix++;
		}
		// Did not find a relevant frame, so just continue. This is
		// OK as we are only committed to making a "best effort" here.
		return null;
	}
	
	/**
	 * Returns the cause of this exception or {@code null} if no cause was
	 * specified when this exception was created.
	 * 
	 * <p>
	 * This method predates the general purpose exception chaining mechanism.
	 * The {@code getCause()} method is now the preferred means of
	 * obtaining this information.
	 * 
	 * @return The result of calling {@code getCause()}.
	 */
	public Throwable getNestedException() {
		return getCause();
	}

	/**
	 * Returns the cause of this exception or {@code null} if no cause was
	 * set.
	 * 
	 * @return The cause of this exception or {@code null} if no cause was
	 *         set.
	 */
	public Throwable getCause() {
		return super.getCause();
	}

	/**
	 * Initializes the cause of this exception to the specified value.
	 * 
	 * @param cause The cause of this exception.
	 * @return This exception.
	 * @throws IllegalArgumentException If the specified cause is this
	 *         exception.
	 * @throws IllegalStateException If the cause of this exception has already
	 *         been set.
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}
