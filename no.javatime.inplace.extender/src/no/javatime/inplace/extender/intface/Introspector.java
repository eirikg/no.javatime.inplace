package no.javatime.inplace.extender.intface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.osgi.framework.Bundle;

public class Introspector {

	/**
	 * When {@code Type} initialized with a value of an object, its fully qualified class name 
	 * will be prefixed with this.
	 * 
	 * @see {@link ReflectionUtil#getClassName(Type)}
	 */
	private static final String TYPE_CLASS_NAME_PREFIX = "class ";
	private static final String TYPE_INTERFACE_NAME_PREFIX = "interface ";

	public static <T> T createObject(Class<T> cls) throws ExtenderException {

		try {
			if(!hasDefaultConstructor(cls)) {
				throw new ExtenderException("Missing default constructor in class {0}", cls.getSimpleName());
			}
			return cls.newInstance();			
		} catch (SecurityException e) {
			throw new ExtenderException(e, "Failed to instantiate class {0} due to security reasons", cls.getSimpleName());
		} catch (InstantiationException e) {
			throw new ExtenderException(e, "Failed to instantiate Class {0}", cls.getSimpleName());
		} catch (IllegalAccessException e) {
			throw new ExtenderException(e, "Failed to access Class {0}. Is the class or its nullary constructor accessible?", cls.getSimpleName());
		} catch (ExceptionInInitializerError e) {
			throw new ExtenderException(e, "Exception in a static initializer creating an instance of class {0}", cls.getSimpleName());
		}
	}

	/**
	 * OSGi load class wrapper
	 *  
	 * @param bundle Use the class loader of this bundle
	 * @param classname the name of the class to load using the class loader of the specified bundle
	 * @return the loaded class object
	 * @throws ExtenderException if the bundle context is not valid or it is a fragment bundle, class
	 * is not found in the specified bundle, the bundle is in an illegal state (uninstalled, installed or
	 * resolved) or if the caller does not have the appropriate AdminPermission[this,CLASS], and the
	 * Java Runtime Environment supports permissions.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> loadClass(Bundle bundle, String classname) throws ExtenderException {

		try {
			return (Class<T>) bundle.loadClass(classname);					
		} catch (ClassNotFoundException e) {
			throw new ExtenderException(e, "Failed to load class {0} from bundle: {1}", classname, bundle);
		} catch (IllegalStateException e) {
			throw new ExtenderException(e, "{1} is in an illegal state (is bundle resolved?). Failed loading class {0}", classname, bundle);
		} catch (NullPointerException e) {
			if (null != classname) {
				throw new ExtenderException(e, "Invalid bundle (null) loading class {0}", classname);
			} else {
				throw new ExtenderException(e, "Invalid or null bundle and null class name loading class");					
			}
		}					
	}

	/**
	 * Loads extension class, creates extension object and executes an arbitrary class member method given 
	 * its class, object, formal and actual parameters
	 * 
	 * @param T type of class
	 * @param methodName method name to invoke
	 * @param cls class in which the method is the member method to invoke
	 * @param paramDef an array defining the formal parameter types of the method
	 * @param obj the object from which underlying method is invoked from
	 * @param paramVal the actual parameter values used in the method call
	 * @return the return value of the invoked method or null if the signature of the method return value is void
	 * @exception ExtenderException exception bounded to the underlying reflection exceptions. Adds some limited 
	 * additional information about the cause in context of this method
	 */
	public static <T> Object invoke(String methodName, Class<T> cls, Class<T>[] paramDef, Object obj,
			Object[] paramVal) throws ExtenderException {

		/* The method to invoke */
		Method method = null;

		try {
			method = getMethod(methodName, cls, paramDef); 
			return method.invoke(obj, paramVal);
		} catch (IllegalArgumentException e) {
			throw new ExtenderException(e, "Encountered an illegal argument while trying to execute method {0} in Class {1}", methodName, cls.getSimpleName());
		} catch (IllegalAccessException e) {
			throw new ExtenderException(e, "Failed to access method {0} in class {1}", methodName, cls.getSimpleName());
		} catch (InvocationTargetException e) {
			throw new ExtenderException(e, "Execution failed in Class: {0} and Method: {1}", cls.getSimpleName(), methodName);
		} catch (ExceptionInInitializerError e) {
			throw new ExtenderException(e, "Exception in a static initializer provoked by method {1} in class {0}", cls.getSimpleName(), methodName);
		} catch (NullPointerException e) {
			throw new ExtenderException(e);
		}
	}
	
	public static Class<?> getFirstInterface(Class<?> cls) throws ExtenderException {
		Class<?>[] interfaces = cls.getInterfaces();
		if (interfaces.length > 0) {
			return interfaces[0];
		}
		return null;
	}

	public static <T> Class<T> getInterface(Class<T> cls, String interfaceName) throws ExtenderException {
		
		@SuppressWarnings("unchecked")
		Class<T>[] interfaces = (Class<T>[]) cls.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			String name =  interfaces[i].getName();
			if (name.equals(interfaceName)) {
				return interfaces[i];
			}
		}
		return null;
	}
	
	public static <T> Class<T> getTypeInterface(Class<T> cls, String interfaceName) throws ExtenderException {
		
		@SuppressWarnings("unchecked")
		Class<T>[] interfaces = (Class<T>[]) cls.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			String name =  interfaces[i].getName();
			if (name.equals(interfaceName)) {
				return interfaces[i];
			}
		}
		return null;
	}

	public static <T> Method getMethod(String methodName, Class<T> cls, Class<T>[] paramDef) throws ExtenderException {
		
		try {
			return cls.getMethod(methodName, paramDef);
		} catch (SecurityException e) {
	    throw new ExtenderException(e, "Security violation while executing method {0} in class {1}", methodName, cls.getSimpleName());
		} catch (NoSuchMethodException e) {
	    throw new ExtenderException(e, "Method {0} in class {1} could not be found", methodName, cls.getSimpleName());
		}
	}
	/**
	 * Returns the {@code Class} object associated with the given {@link Type}
	 * depending on its fully qualified name. 
	 * 
	 * @param type the {@code Type} whose {@code Class} is needed.
	 * @return the {@code Class} object for the class with the specified name.
	 * 
	 * @throws ClassNotFoundException if the class cannot be located.
	 * 
	 * @see {@link ReflectionUtil#getClassName(Type)}
	 */
	public static Class<?> getClass(Type type) 
			throws ClassNotFoundException {
		String className = getClassName(type);
		if (className==null || className.isEmpty()) {
			return null;
		}
		return Class.forName(className);
	}

	/**
	 * {@link Type#toString()} value is the fully qualified class name prefixed
	 * with {@link ReflectionUtil#TYPE_NAME_PREFIX}. This method will substring it, for it to be eligible
	 * for {@link Class#forName(String)}.
	 * 
	 * @param type the {@code Type} value whose class name is needed.  
	 * @return {@code String} class name of the invoked {@code type}.
	 * 
	 * @see {@link ReflectionUtil#getClass()}
	 */
	public static String getClassName(Type type) {
		if (type==null) {
			return "";
		}
		String className = type.toString();
		if (className.startsWith(TYPE_CLASS_NAME_PREFIX)) {
	    	className = className.substring(TYPE_CLASS_NAME_PREFIX.length());
	    } else if (className.startsWith(TYPE_INTERFACE_NAME_PREFIX)) {
	    	className = className.substring(TYPE_INTERFACE_NAME_PREFIX.length());
	    }
	    return className;
	}

	/**
	 * Returns an array of {@code Type} objects representing the actual type
	 * arguments to this object.
	 * If the returned value is null, then this object represents a non-parameterized 
	 * object.
	 * 
	 * @param object the {@code object} whose type arguments are needed. 
	 * @return an array of {@code Type} objects representing the actual type 
	 * 		arguments to this object.
	 * 
	 * @see {@link Class#getGenericSuperclass()}
	 * @see {@link ParameterizedType#getActualTypeArguments()}
	 */
	public static Type[] getParameterizedTypes(Object object) {
		Type superclassType = object.getClass().getGenericSuperclass();
		if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
			return null;
		}	
		return ((ParameterizedType)superclassType).getActualTypeArguments();
	}

	/**
	 * Checks whether a {@code Constructor} object with no parameter types is specified
	 * by the invoked {@code Class} object or not.
	 * 
	 * @param clazz the {@code Class} object whose constructors are checked.
	 * @return {@code true} if a {@code Constructor} object with no parameter types is specified.
	 * @throws SecurityException If a security manager, <i>s</i> is present and any of the
	 *         following conditions is met:
	 *			<ul>
	 *             <li> invocation of
	 *             {@link SecurityManager#checkMemberAccess
	 *             s.checkMemberAccess(this, Member.PUBLIC)} denies
	 *             access to the constructor
	 *
	 *             <li> the caller's class loader is not the same as or an
	 *             ancestor of the class loader for the current class and
	 *             invocation of {@link SecurityManager#checkPackageAccess
	 *             s.checkPackageAccess()} denies access to the package
	 *             of this class
	 *         </ul>
	 *         
	 * @see {@link Class#getConstructor(Class...)}
	 */
	public static boolean hasDefaultConstructor(Class<?> clazz) throws SecurityException {
		Class<?>[] empty = {};
		try {
			clazz.getConstructor(empty);
		} catch (NoSuchMethodException e) {
			return false;
		}
		return true;
	}

	private Introspector() {}
}
