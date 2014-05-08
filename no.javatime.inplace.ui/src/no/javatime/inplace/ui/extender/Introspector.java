package no.javatime.inplace.ui.extender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import no.javatime.inplace.bundlemanager.ExtenderException;

import org.osgi.framework.Bundle;

public class Introspector {

	public static Object createExtensionObject(Class<?> cls) throws ExtenderException {

		try {
			return cls.newInstance();			
		} catch (SecurityException e) {
			throw new ExtenderException(e, "security_instantiation", cls.getSimpleName());
		} catch (InstantiationException e) {
			throw new ExtenderException(e, "instantiation", cls.getSimpleName());
		} catch (IllegalAccessException e) {
			throw new ExtenderException(e, "illegal_access_class", cls.getSimpleName());
		} catch (ExceptionInInitializerError e) {
			throw new ExtenderException(e, "initializer_instantiation", cls.getSimpleName());
		}
	}

	public static Class<?> loadExtensionClass(Bundle bundle, String classname) throws ExtenderException {

		try {
			return bundle.loadClass(classname);					
		} catch (ClassNotFoundException e) {
			throw new ExtenderException(e, "load_class_not_found", classname, bundle);
		} catch (IllegalStateException e) {
			throw new ExtenderException(e, "load_class_illegal_state", classname, bundle);
		} catch (NullPointerException e) {
			if (null != classname) {
				throw new ExtenderException("null_load_class", classname);
			} else {
				throw new ExtenderException("null_load_class_name");					
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
	 * @exception ExtenderException exception bounded to the underlying reflection exceptions. Adds some limited 
	 * additional information about the cause in context of this method
	 */
	public static Object invoke(String methodName, Class<?> cls, Class<?>[] paramDef, Object obj,
			Object[] paramVal) throws ExtenderException {
		
		// Class<?>[] doubleParDef = new Class<?>[] {Double.class};
		// Object[] doubleParVal = new Object[1];

		// invoke(setMethodName, elemClass, doubleParDef, elemInstance, new Object[] {startValue});

		/* The method to invoke */
		Method method = null;

		try {			
			try {
				method = getMethod(methodName, cls, paramDef); 
			} catch (ExtenderException e) {
				throw e;
			}
			try {
				return method.invoke(obj, paramVal);
			} catch (IllegalArgumentException e) {
		    throw new ExtenderException(e, "illegal_argument_method", methodName, cls.getSimpleName());
			} catch (IllegalAccessException e) {
				throw new ExtenderException(e, "illegal_access_method", methodName, cls.getSimpleName());
			} catch (InvocationTargetException e) {
				throw new ExtenderException(e, "method_invocation_target", cls.getSimpleName(), methodName);
			} catch (ExceptionInInitializerError e) {
				throw new ExtenderException(e, "initializer_error", cls.getSimpleName(), methodName);
			}
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

	public static Class<?> getInterface(Class<?> cls, String interfaceName) throws ExtenderException {
		Class<?>[] interfaces = cls.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			String name =  interfaces[i].getName();
			if (name.equals(interfaceName)) {
				return interfaces[i];
			}
		}
		return null;
	}

	public static Method getMethod(String methodName, Class<?> cls, Class<?>[] paramDef) throws ExtenderException {
		try {
			return cls.getMethod(methodName, paramDef);
		} catch (SecurityException e) {
	    throw new ExtenderException(e, "security_violation", methodName, cls.getSimpleName());
		} catch (NoSuchMethodException e) {
	    throw new ExtenderException(e, "no_such_method", methodName, cls.getSimpleName());
		}
	}

}
