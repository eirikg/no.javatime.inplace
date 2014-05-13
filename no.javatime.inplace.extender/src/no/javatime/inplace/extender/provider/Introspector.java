package no.javatime.inplace.extender.provider;

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

	public static Object createExtensionObject(Class<?> cls) throws InPlaceException {

		try {
			return cls.newInstance();			
		} catch (SecurityException e) {
			throw new InPlaceException(e, "security_instantiation", cls.getSimpleName());
		} catch (InstantiationException e) {
			throw new InPlaceException(e, "instantiation", cls.getSimpleName());
		} catch (IllegalAccessException e) {
			throw new InPlaceException(e, "illegal_access_class", cls.getSimpleName());
		} catch (ExceptionInInitializerError e) {
			throw new InPlaceException(e, "initializer_instantiation", cls.getSimpleName());
		}
	}

	public static Class<?> loadExtensionClass(Bundle bundle, String classname) throws InPlaceException {

		try {
			return bundle.loadClass(classname);					
		} catch (ClassNotFoundException e) {
			throw new InPlaceException(e, "load_class_not_found", classname, bundle);
		} catch (IllegalStateException e) {
			throw new InPlaceException(e, "load_class_illegal_state", classname, bundle);
		} catch (NullPointerException e) {
			if (null != classname) {
				throw new InPlaceException("null_load_class", classname);
			} else {
				throw new InPlaceException("null_load_class_name");					
			}
		}					
	}

	/**
	 * Loads extension class, creates extension object and executes an arbitrary class member method given 
	 * its class, object, formal and actual parameters
	 * 
	 * @param methodName method name to invoke
	 * @param cls class in which the method is the member method to invoke
	 * @param paramDef an array defining the formal parameter types of the method
	 * @param obj the object from which underlying method is invoked from
	 * @param paramVal the actual parameter values used in the method call
	 * @return the return value of the invoked method or null if the signature of the method return value is void
	 * @exception InPlaceException exception bounded to the underlying reflection exceptions. Adds some limited 
	 * additional information about the cause in context of this method
	 */
	public static Object invoke(String methodName, Class<?> cls, Class<?>[] paramDef, Object obj,
			Object[] paramVal) throws InPlaceException {
		
		// Class<?>[] doubleParDef = new Class<?>[] {Double.class};
		// Object[] doubleParVal = new Object[1];

		// invoke(setMethodName, elemClass, doubleParDef, elemInstance, new Object[] {startValue});

		/* The method to invoke */
		Method method = null;

		try {			
			try {
				method = getMethod(methodName, cls, paramDef); 
			} catch (InPlaceException e) {
				throw e;
			}
			try {
				return method.invoke(obj, paramVal);
			} catch (IllegalArgumentException e) {
		    throw new InPlaceException(e, "illegal_argument_method", methodName, cls.getSimpleName());
			} catch (IllegalAccessException e) {
				throw new InPlaceException(e, "illegal_access_method", methodName, cls.getSimpleName());
			} catch (InvocationTargetException e) {
				throw new InPlaceException(e, "method_invocation_target", cls.getSimpleName(), methodName);
			} catch (ExceptionInInitializerError e) {
				throw new InPlaceException(e, "initializer_error", cls.getSimpleName(), methodName);
			}
		} catch (NullPointerException e) {
			throw new InPlaceException(e);
		}
	}
	
	public static Class<?> getFirstInterface(Class<?> cls) throws InPlaceException {
		Class<?>[] interfaces = cls.getInterfaces();
		if (interfaces.length > 0) {
			return interfaces[0];
		}
		return null;
	}

	public static Class<?> getInterface(Class<?> cls, String interfaceName) throws InPlaceException {
		Class<?>[] interfaces = cls.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			String name =  interfaces[i].getName();
			if (name.equals(interfaceName)) {
				return interfaces[i];
			}
		}
		return null;
	}
	public static <T> Class<T> getTypeInterface(Class<?> cls, String interfaceName) throws InPlaceException {
		Class<T>[] interfaces = (Class<T>[]) cls.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			String name =  interfaces[i].getName();
			if (name.equals(interfaceName)) {
				return interfaces[i];
			}
		}
		return null;
	}

	public static Method getMethod(String methodName, Class<?> cls, Class<?>[] paramDef) throws InPlaceException {
		try {
			return cls.getMethod(methodName, paramDef);
		} catch (SecurityException e) {
	    throw new InPlaceException(e, "security_violation", methodName, cls.getSimpleName());
		} catch (NoSuchMethodException e) {
	    throw new InPlaceException(e, "no_such_method", methodName, cls.getSimpleName());
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
