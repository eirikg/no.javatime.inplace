package no.javatime.inplace.bundlemanager;

import no.javatime.inplace.InPlace;
import no.javatime.inplace.extender.provider.Introspector;

import org.osgi.framework.Bundle;

/**
 * Utility accessing and manipulating threads belonging to a bundle. There are also a few general purpose members for
 * finding a thread.
 * <p>
 * The main purpose is to obtain information about threads owing active OSGi bundle lifecycle operations. An ongoing
 * operation (state changing) means that a bundle is in the process of executing a bundle operation and has not finished
 * the operation yet.
 * <p>
 * Note that information obtained about the state of an operation (in process or ended) may change after returning from
 * one of the bundle specific members in this class.
 */
public class BundleThread {
	
	//Luna (OSGi R6)
	private final static String lunaBundleImplClassName = "org.eclipse.osgi.container.Module";
	private final static String getStateChangeownerMethodName = "getStateChangeOwner";
	//Juno and Kepler (OSGi R5)
	private final static String keplerBundleImplClassName = "org.eclipse.osgi.framework.internal.core.BundleHost";
	private final static String getStateChangingMethodName = "getStateChanging";

	/**
	 * Obtain the thread running the current bundle operation.
	 * <p>
	 * Note that the return value may not be the same in the time frame after this method returns the thread and the
	 * receiver tries to access the thread.
	 * <p>
	 * Equinox was refactored after the implementation of OSGi R6. Thus, accessing the current thread running a bundle
	 * operation is different in Luna compared to Juno & Kepler (OSGi R5)
	 * <p>
	 * There is to my knowledge no official way to obtain or ask a bundle if it is in a state changing process. 
	 * Interrogating the bundle dynamically for its thread (the life cycle API is single threaded) breaks the life cycle API 
	 * even if it is not recognized by the compiler.
	 * 
	 * @param bundle the bundle running the operation
	 * @return null or the thread
	 */
	static public Thread getThread(Bundle bundle) {
		
		/* The thread running the bundle operation */
		Thread thread = null;
		/* Class of the system bundle to load */
		Class<?> systemClass = null;
		/* Method name in system bundle class used to obtain the thread running the bundle operation */
		String methodName = null;

		try {
			methodName = getStateChangeownerMethodName; 
			systemClass = loadSystemBundleClass(lunaBundleImplClassName);
			if (null == systemClass) {
				systemClass = loadSystemBundleClass(keplerBundleImplClassName);
				methodName = getStateChangingMethodName; 
			}
			if (null != systemClass && null != bundle) {
				Object systemObject = bundle.adapt(systemClass);
				if (null != systemObject) {
					thread = (Thread) Introspector.invoke(methodName, systemClass, (Class[]) null, systemObject, (Object[]) null);					
					//thread = (Thread) invoke(methodName, systemClass, (Class[]) null, systemObject, (Object[]) null);
				}
			}
			// Just return null when failing
		} catch (InPlaceException e) {
		} catch (SecurityException e) {
		} catch (IllegalStateException e) {
		}
		return thread;
	}

	/**
	 * Check if this bundle is busy running an operation.
	 * <p>
	 * Note this may not be true in the time frame after this method returns true and the receiver checks the state
	 * changing status
	 * 
	 * @param bundle the bundle running the operation
	 * @return true if the bundle has an ongoing operation. Otherwise false
	 * @see #getThread(Bundle)
	 */
	static public boolean isStateChanging(Bundle bundle) {
		Thread thread = getThread(bundle);
		return (thread != null) ? true : false;
	}

	/**
	 * Stop the active thread running a bundle operation on the specified bundle
	 * 
	 * @param bundle with an active thread running a bundle operation
	 * @return true if the thread is stopped. False is returned if no thread exist or the current thread can't modify his
	 *         thread
	 * @see #stopThread(Thread)
	 */
	static public boolean stopThread(Bundle bundle) {
		Thread thread = getThread(bundle);
		return stopThread(thread);
	}

	/**
	 * Returns the thread with the specified name. Search among all thread groups.
	 * <p>
	 * Note this may not be true in the time frame after this method returns the thread and the receiver tries to access
	 * the thread.
	 * 
	 * @param threadName name of the thread to find
	 * @return the thread if there is an active thread with the specified name. Otherwise null.
	 */
	static public Thread getThread(String threadName) {

		Thread t = null;
		if (null == threadName) {
			return t;
		}
		ThreadGroup tg = getTopThreadGroup();
		Thread[] threads = new Thread[tg.activeCount()];
		tg.enumerate(threads, true);
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getName().equals(threadName)) {
				t = threads[i];
				break;
			}
		}
		return t;
	}

	/**
	 * Stop the specified thread.
	 * <p>
	 * If this is a thread belonging to a bundle the implication of stopping the thread has the effect that the OSGi
	 * operation is terminated and the bundle as a whole may be in an unstable state. It is recommended to unresolve or
	 * unistall the bundle and its requiring bundles after terminating an ongoing operation by stopping the thread that
	 * owns the operation.
	 * <p>
	 * Note that this method use the deprecated Thread.stop() method
	 * 
	 * @param thread the thread to stop
	 * @return true if the tread was stopped. False is also returned if the thread is null or the current thread can't
	 *         modify his thread
	 */
	@SuppressWarnings("deprecation")
	static public boolean stopThread(Thread thread) {

//		if (null != thread) {
//			thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//
//				@Override
//				public void uncaughtException(Thread thread, Throwable ex) {
//					new Thread() {
//						@Override
//						public void run() {
//							System.out.println("Uncought exception handler for thread" + thread.getName());
//						}
//					}.start();
//				}
//			});
//		}

		try {
			if (null != thread) {
				thread.stop();
				return true;
			}
		} catch (SecurityException e) {
			// Ignore. No permission to modify this thread
		}
		return false;
	}

	/**
	 * Load a class from the system bundle with the specified qualified name
	 * 
	 * @param className fully qualified class name to load. Must be a class owned by the system bundle
	 * @return null or the loaded class
	 */
	static private Class<?> loadSystemBundleClass(String className) {

		Class<?> systemClass = null;
		try {
			Bundle systemBundle = InPlace.getContext().getBundle(0);
			if (null != systemBundle) {
				systemClass = systemBundle.loadClass(className);
			}
		} catch (IllegalStateException e1) {
			// Impossible that the system bundle is uninstalled as long as we are running
		} catch (ClassNotFoundException e1) {
			// The class loading is trial and error
		}
		return systemClass;
	}

	/**
	 * Answers all thread groups in the system.
	 * 
	 * @return An array of all thread groups.
	 */
	public static ThreadGroup[] getThreadGroups() {
		ThreadGroup tg = getTopThreadGroup();
		ThreadGroup[] groups = new ThreadGroup[tg.activeGroupCount()];
		int count = tg.enumerate(groups, true);
		if (count == groups.length) {
			return groups;
		}
		// get rid of null entries
		ThreadGroup[] ngroups = new ThreadGroup[count];
		System.arraycopy(groups, 0, ngroups, 0, count);
		return ngroups;
	}

	/**
	 * Answers the top level group of the current thread.
	 * <p>
	 * It is the 'system' or 'main' thread group under which all 'user' thread groups are allocated.
	 * 
	 * @return The parent of all user thread groups.
	 */
	public static ThreadGroup getTopThreadGroup() {
		ThreadGroup topGroup = Thread.currentThread().getThreadGroup();
		if (topGroup != null) {
			while (topGroup.getParent() != null) {
				topGroup = topGroup.getParent();
			}
		}
		return topGroup;
	}

	private BundleThread() {
	}
}
