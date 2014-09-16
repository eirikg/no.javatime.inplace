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
package no.javatime.inplace.pl.console.view;

import java.io.Console;
import java.io.PrintStream;

import no.javatime.inplace.pl.console.Activator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Outputs streams to the bundle console. {@code System.out} and
 * {@code Systerm.err}. May also be redirected by user in the console view to the
 * bundle console or to the IDE default.
 */
public class BundleConsole extends MessageConsole {

	final public static ImageDescriptor consoleViewImage = Activator.getImageDescriptor("icons/console_view.gif"); //$NON-NLS-1$

	// Misc. stream for directing output to the bundle console.
	private MessageConsoleStream messageOut = null;
	// Bundle console, always same as where the bundles run
	private PrintStream systemOut = null;
	private PrintStream systemErr = null;
	// The IDE default for system out and err, shell or development platform
	private PrintStream defaultOut = System.out;
	private PrintStream defaultErr = System.err;

	private boolean initialized = false;

	public BundleConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
	}

	/**
	 * Used to notify this console of life cycle changes; - <code>init()</code> and
	 * - <code>dispose()</code>.
	 */
	public class ConsoleLifecycle implements IConsoleListener {

		/**
		 * Initialize the different streams
		 */
		public void consolesAdded(IConsole[] consoles) {
			for (int i = 0; i < consoles.length; i++) {
				IConsole console = consoles[i];
				if (console == BundleConsole.this) {
					init();
				}
			}
		}

		/**
		 * Removes the listener and calls {@code dispose} which does nothing. See
		 * comments in {@code dispose}.
		 */
		public void consolesRemoved(IConsole[] consoles) {
			for (int i = 0; i < consoles.length; i++) {
				IConsole console = consoles[i];
				if (console == BundleConsole.this) {
					ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
					dispose();
				}
			}
		}
	}

	/**
	 * Initialize the bundle console streams
	 */
	protected void init() {
		// Called when console is added to the console view
		super.init();
		// Ensure that initialization occurs in the UI thread
		Activator.getDisplay().asyncExec(new Runnable() {
			public void run() { 
				initializeStreams(); 
			} 
		});
	}

	/**
	 * Initialize the bundle console in/out streams and set output
	 * to bundle console or IDE default according to user selection. 
	 * Must be called from the UI thread.
	 */
	public void initializeStreams() {
		if (!initialized) {
			systemOut = new PrintStream(newMessageStream(), true);
			systemErr = new PrintStream(newMessageStream(), true);
			initialized = true;
			if (Activator.getDefault().getMsgOpt().isSystemOutBundleConsole()) {
				setSystemOutToBundleConsole();
			} else {
				setSystemOutToIDEDefault();
			}
		}
	}

	protected void dispose() {
		// Can't call super.dispose() because the document should be open when
		// the console is removed. New messages added to a removed console are added to
		// the document and shown when the console becomes visible
	}

	/**
	 * A separate stream, that always are directed
	 * to the bundle console when using {@code System.out} or the stream
	 * directly. 
	 * @return a stream directed to the bundle console
	 */
	public MessageConsoleStream getBundleConsoleStream() {
		if (null == messageOut) {
			messageOut = newMessageStream();
		}
		return messageOut;
	}

	/**
	 * Redirect {@code System.out} and {@code Systerm.err} to the bundle console.
	 * This is the console in the current IDE, which in this case is both the
	 * development and the target IDE.
	 * 
	 * @return the console before redirected to the bundle console
	 */
	public Console setSystemOutToBundleConsole() {
		Console console = System.console();
		if (initialized) {
			System.setOut(systemOut);
			System.setErr(systemErr);
		}
		return console;
	}

	/**
	 * Set the console used by the IDE at startup. This is usually the console
	 * used by the development IDE (not the target IDE).
	 */
	public void setSystemOutToIDEDefault() {
		if (initialized) {
			System.setErr(defaultErr);
			System.setOut(defaultOut);
		}
	}
	
	/**
	 * When the console is initialized the output console is defined and set.
	 * @return true if initialized and false if not 
	 */
	public boolean isInitialized() {
		return initialized;
	}
}
