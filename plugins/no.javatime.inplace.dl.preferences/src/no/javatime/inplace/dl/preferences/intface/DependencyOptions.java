package no.javatime.inplace.dl.preferences.intface;

import java.util.EnumSet;

import org.osgi.service.prefs.BackingStoreException;

/**
 * A set of dependency options or closures that may be applied in project and
 * bundle activate and deactivate operations. When applied, additional bundle 
 * projects are added to the operation based on the closure and the initial set
 * of bundle projects to activate or deactivate.
 */
public interface DependencyOptions {
	
	public final static String DEPENDENCY_OPTIONS_SERVICE = "Dependency-Options-Service";

	/**
	 * An enumeration of all dependency options that may be applied to bundles that are associated with a bundle operation.
	 */
	public enum Closure {
		
		/** 
		 * The providing closure adds all bundles that directly or indirectly (transitive) provides capabilities to an initial 
		 * set of bundle projects to activate or deactivate 
		*/
		PROVIDING,

		/** 
		 * The requiring closure adds all bundles that directly or indirectly (transitive) requires capabilities from an initial 
		 * set of bundle projects to activate or deactivate 
		*/
		REQUIRING,
		
		/** 
		 * The requiring and providing closure first applies the requiring closure then adds the requiring set of bundles to
		 * the initial set bundle projects before applying the providing closure to this new set if initial bundle projects to 
		 * activate or deactivate 
		*/
		REQUIRING_AND_PROVIDING,

		/** 
		 * The providing and requiring closure first applies the providing closure then adds the providing set of bundles to
		 * the initial set bundle projects before applying the requiring closure to this new set if initial bundle projects to 
		 * activate or deactivate 
		*/
		PROVIDING_AND_REQUIRING,
		
		/** 
		 * Dependent bundles are collectively members of an acyclic directed graph. This closure calculates the partial graphs 
		 * from the initial set of bundle projects to activate or deactivate.   
		*/
		PARTIAL_GRAPH,
		
		/** 
		 * Ignores any dependencies and does not add any new bundle projects to the operation. The initial set of bundle projects 
		 * are sorted in dependency order according default closure (providing or requiring) of the present operation. 
		 */
		SINGLE;
	}
	
	/**
	 * Bundle operations or set of bundle operations where closures may be applied.
	 */
	public enum Operation {
		
		/**
		 * Used when bundle projects are activated, installed, resolved and started. Closures are providing (default),
		 * requiring and providing and partial graph
		*/
		ACTIVATE_PROJECT,
		/**
		 * Used when bundle projects are deactivated and uninstalled from any valid state including state ACTIVE. Closures are requiring (default),
		 * providing and requiring and partial graph 
		 */
		DEACTIVATE_PROJECT,

		/**
		 * Used when bundles are started with resolve as the initial state. If start is part of a project activation operation 
		 * (e.g. with uninstalled as the initial state) it is the current closure of the activate project operation that is used. 
		 */
		ACTIVATE_BUNDLE,

		/**
		 * Used when bundles are stopped with resolved as the target state. If stop is part of a project deactivation operation 
		 * (e.g. with uninstalled as the target state) it is the current closure of the deactivate project operation that is used. 
		 */
		DEACTIVATE_BUNDLE,
	}
	
	/**
	 * Get the present dependency option for the specified operation. 
	 * Only one closure of the set of closures bound to an operation 
	 * may be the current closure at a specific point in time
	 * 
	 * @param operation to obtain the current dependency option for
	 * @return the current dependency option.If failed to get the option the default
	 * option for the specified operation is returned
	 */	
	public Closure get(Operation operation);

	/**
	 * Get the state of the specified dependency option bound to the specified operation
	 * 
	 * @param operation associated with a closure to obtain the state for
	 * @param closure to get state of. Only one closure of the set of closures bound to an 
	 * operation may be true at a specific point in time 
	 * @return the state of current dependency option. True if this is the current dependency option and
	 * false if not
	 * @throws IllegalStateException if not a valid closure/operation combination
	 */	
	public boolean get(Operation operation, Closure closure)  throws IllegalStateException ;

	/**
	 * Get the default dependency option (requiring or providing) for the specified operation
	 * 
	 * @param operation to obtain the default dependency option for
	 * @return the closure representing the default dependency option.If failed to get the option the 
	 * {@code Closure.SINGLE} is returned.
	 */
	public Closure getDefault(Operation operation);
	/**
	 * Check if the specified closure is the default option for the
	 * specified operation. Default closure is requiring or providing.
	 * 
	 * @param operation to check the default closure for
	 * @param closure to check if it is the default closure for the specified operation
	 * @return true if the closure is the default for the operation. Otherwise false.
	 */
	public boolean isDefault(Operation operation, Closure closure);

	/**
	 * Check if the specified closure is a valid closure for the specified operation.
	 *
	 * @param operation to check for a valid closure
	 * @param closure to be checked for validity
	 * @return true if the closure is valid for the specified operation. Otherwise false
	 */
	public boolean isAllowed(Operation operation, Closure closure);
	
	/**
	 * Get the set of possible closures that can be applied to the specified operation
	 * 
	 * @param operation to get the valid closures for
	 * @return a set of valid closures or an empty set
	 */
	public EnumSet<Closure> getvalidClosures(Operation operation);
	
	/**
	 * Set the current dependency option for the specified operation.
	 * There is only one closure that can be the current
	 * closure at one point in time
	 * @param operation on of {@link Operation}
	 * @param closure on of the allowed {@link Closure} for the specified operation 
	 * @return true if the closure was set for the operation. False if setting the closure fails
	 * @see #isAllowed(Operation, Closure)
	 * @throws IllegalStateException if not a valid closure/operation combination
	 */
	public boolean set(Operation operation, Closure closure)  throws IllegalStateException ;
		
	/**
	 * Flush all changes to OSGi preference store
	 * @throws BackingStoreException thrown when the flush operation could not complete
	 */
	public void flush() throws BackingStoreException;

}
