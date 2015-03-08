package no.javatime.inplace.dl.preferences.impl;

import java.util.EnumSet;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.msg.Msg;
import no.javatime.inplace.dl.preferences.service.PreferencesServiceStore;

import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Load/save dependency options to and from preference store and maintains the integrity
 * of closures bound to operations. An operation can only have one current closure 
 * among the set of closures defined for the operation.
 * <p>
 * The project activation and deactivation operations have three closures each. Only partial
 * graph and requiring and providing are stored for the activate project operation whereas
 * the providing closure is deducted. For the deactivate project operation only partial
 * graph and providing and requiring are stored for the deactivate project operation whereas
 * the requiring closure is derived from the stored closures.  
 * <p>
 * For bundle activation and deactivation which have five closures each, providing, requiring,
 * and partial graph are stored for each of the two operations while the requiring and providing closure
 * (activate bundle), providing and requiring closure (deactivate bundle) and the single closure 
 * (both activate and deactivate bundle) are derived. 
 */
public class DependencyOptionsImpl implements DependencyOptions {


	private Preferences wrapper;
	
	final private EnumSet<Closure> activateProjectClosure = 
			EnumSet.of(Closure.PROVIDING, Closure.REQUIRING_AND_PROVIDING, Closure.PARTIAL_GRAPH);

	final private EnumSet<Closure> deactivateProjectClosure = 
			EnumSet.of(Closure.REQUIRING, Closure.PROVIDING_AND_REQURING, Closure.PARTIAL_GRAPH);
	
	final private EnumSet<Closure> activateBundleClosure = 
			EnumSet.of(Closure.PROVIDING, Closure.REQUIRING, Closure.REQUIRING_AND_PROVIDING, Closure.PARTIAL_GRAPH, Closure.SINGLE);

	final private EnumSet<Closure> deactivateBundleClosure = 
			EnumSet.of(Closure.REQUIRING, Closure.PROVIDING, Closure.PROVIDING_AND_REQURING, Closure.PARTIAL_GRAPH, Closure.SINGLE);


	public DependencyOptionsImpl() {
		getPrefs();
	}

	protected Preferences getPrefs() {
		if (null == wrapper) {
			wrapper = PreferencesServiceStore.getPreferences();
		}
		return wrapper;
	}

	@Override
	public boolean isAllowed(Operation operation, Closure closure) {
			switch (operation) {
				case ACTIVATE_PROJECT: {
					return activateProjectClosure.contains(closure);
				}
				case DEACTIVATE_PROJECT: {
					return deactivateProjectClosure.contains(closure);
				}
				case ACTIVATE_BUNDLE: {
					return activateBundleClosure.contains(closure);
				}
				case DEACTIVATE_BUNDLE: {
					return deactivateBundleClosure.contains(closure);
				}
				default: {
					return false;
				}
		}
	}
	
	@Override
	public EnumSet<Closure> getvalidClosures(Operation operation) {
		switch (operation) {
		case ACTIVATE_PROJECT:
			return activateProjectClosure;
		case ACTIVATE_BUNDLE:
			return activateBundleClosure;
		case DEACTIVATE_PROJECT:
			return deactivateProjectClosure;
		case DEACTIVATE_BUNDLE:
			return deactivateBundleClosure;
		default:
			return EnumSet.noneOf(Closure.class);
		}
	}

	@Override
	public boolean isDefault(Operation operation, Closure closure) {
		switch (operation) {
		case ACTIVATE_PROJECT:
		case ACTIVATE_BUNDLE:
			if (closure.equals(Closure.PROVIDING)) {
				return true;
			}
			return false;
		case DEACTIVATE_PROJECT:
		case DEACTIVATE_BUNDLE:
			if (closure.equals(Closure.REQUIRING)) {
				return true;
			}
		default:
			return false;
		}
	}

	@Override
	public Closure getDefault(Operation operation) {
		switch (operation) {
		case ACTIVATE_PROJECT:
		case ACTIVATE_BUNDLE:
			return Closure.PROVIDING;
		case DEACTIVATE_PROJECT:
		case DEACTIVATE_BUNDLE:
			return Closure.REQUIRING;
		default:
			return Closure.SINGLE;
		}
	}

	@Override
	public Closure get(Operation operation) {
		switch (operation) {
		case ACTIVATE_PROJECT:
			for (Closure closure : activateProjectClosure) {
				if (!closure.equals(Closure.PROVIDING) && load(operation, closure)) {
					return closure;
				}
			}
			return Closure.PROVIDING;
		case DEACTIVATE_PROJECT:
			for (Closure closure : deactivateProjectClosure) {
				if (!closure.equals(Closure.REQUIRING) && load(operation, closure)) {
					return closure;
				}
			}
			return Closure.REQUIRING;
		case ACTIVATE_BUNDLE:
		case DEACTIVATE_BUNDLE:
			boolean providing = load(operation, Closure.PROVIDING);
			boolean requiring = load(operation, Closure.REQUIRING);
			if (providing && requiring) {
				if (operation.equals(Operation.ACTIVATE_BUNDLE)) {
					return Closure.REQUIRING_AND_PROVIDING;
				} else {
					return Closure.PROVIDING_AND_REQURING;
				}
			} else if (providing) {
				return Closure.PROVIDING;
			} else if (requiring) {
				return Closure.REQUIRING;
			} else if (load(operation, Closure.PARTIAL_GRAPH)) {
				return Closure.PARTIAL_GRAPH;
			} else  {
				return Closure.SINGLE;				
			}			
		default:
			return getDefault(operation);				
		}
	}

	@Override
	public boolean get(Operation operation, Closure closure) throws IllegalStateException {
		if (!isAllowed(operation, closure)) {
			throw new IllegalStateException(NLS.bind(Msg.ILLEGAL_CLOSURE_EXCEPTION, closure.name(), operation.name()));
		}
		switch (operation) {
			case ACTIVATE_PROJECT: 
			case DEACTIVATE_PROJECT: 
				switch (closure) {
				case PROVIDING:
					if (!load(operation, Closure.REQUIRING_AND_PROVIDING)
							&& !load(operation, Closure.PARTIAL_GRAPH)) {
						return true;
					}
					return false;
				case REQUIRING:
					if (!load(operation, Closure.PROVIDING_AND_REQURING)
							&& !load(operation, Closure.PARTIAL_GRAPH)) {
						return true;
					}
					return false;
				case REQUIRING_AND_PROVIDING:
				case PROVIDING_AND_REQURING:
				case PARTIAL_GRAPH:
					return load(operation, closure);
				default:
					return false;
				}
			case ACTIVATE_BUNDLE:
			case DEACTIVATE_BUNDLE:
				switch (closure) {
				case PROVIDING:
					if (load(operation, Closure.PROVIDING)
							&& !load(operation, Closure.REQUIRING)) {
						return true;
					}
					return false;
				case REQUIRING:
					if (!load(operation, Closure.PROVIDING)
							&& load(operation, Closure.REQUIRING)) {
						return true;
					}
					return false;
				case PARTIAL_GRAPH:
					return load(operation, closure);
				case REQUIRING_AND_PROVIDING:
				case PROVIDING_AND_REQURING:
					if (load(operation, Closure.PROVIDING)
							&& load(operation, Closure.REQUIRING)) {
						return true;
					}
					return false;
				case SINGLE:
					if (!load(operation, Closure.PROVIDING)
							&& !load(operation, Closure.REQUIRING)
							&& !load(operation, Closure.PARTIAL_GRAPH)) {
						return true;
					}	
					return false;
				default:
					return false;
				}
			default: {
				throw new IllegalStateException(NLS.bind(Msg.ILLEGAL_CLOSURE_EXCEPTION, closure.name(), operation.name()));
			}
		}
	}

	@Override
	public boolean set(Operation operation, Closure closure)  throws IllegalStateException {
		if (!isAllowed(operation, closure)) {
			throw new IllegalStateException(NLS.bind(Msg.ILLEGAL_CLOSURE_EXCEPTION, closure.name(), operation.name()));
		}
		switch (operation) {
			case ACTIVATE_PROJECT: 
			case DEACTIVATE_PROJECT:
				switch (closure) {
				case PROVIDING:
				case REQUIRING:
					store(operation, Closure.PARTIAL_GRAPH, false);						
					if (operation.equals(Operation.ACTIVATE_PROJECT)) {
						store(operation, Closure.REQUIRING_AND_PROVIDING, false);
					} else {
						store(operation, Closure.PROVIDING_AND_REQURING, false);													
					}
					return true;
				case REQUIRING_AND_PROVIDING:
				case PROVIDING_AND_REQURING:
					store(operation, closure, true);
					store(operation, Closure.PARTIAL_GRAPH, false);						
					return true;
				case PARTIAL_GRAPH:
					store(operation, closure, true);
					if (operation.equals(Operation.ACTIVATE_PROJECT)) {
						store(operation, Closure.REQUIRING_AND_PROVIDING, false);
					} else {
						store(operation, Closure.PROVIDING_AND_REQURING, false);							
					}
					return true;
				default:
					return false;
				}
			case ACTIVATE_BUNDLE:
			case DEACTIVATE_BUNDLE:
				switch (closure) {
				case PROVIDING:
					store(operation, closure, true);
					store(operation, Closure.REQUIRING, false);						
					store(operation, Closure.PARTIAL_GRAPH, false);						
					return true;
				case REQUIRING:
					store(operation, closure, true);
					store(operation, Closure.PROVIDING, false);						
					store(operation, Closure.PARTIAL_GRAPH, false);						
					return true;
				case REQUIRING_AND_PROVIDING:
				case PROVIDING_AND_REQURING:
					store(operation, Closure.REQUIRING, true);						
					store(operation, Closure.PROVIDING, true);						
					store(operation, Closure.PARTIAL_GRAPH, false);						
					return true;
				case PARTIAL_GRAPH:
					store(operation, closure, true);
					store(operation, Closure.REQUIRING, false);						
					store(operation, Closure.PROVIDING, false);						
					return true;
				case SINGLE:
					store(operation, Closure.REQUIRING, false);						
					store(operation, Closure.PROVIDING, false);						
					store(operation, Closure.PARTIAL_GRAPH, false);						
					return true;
				default:
					return false;
				}
			default: {
				return false;
			}
		}
	}

	private void store(Operation operation, Closure closure, boolean value) {
		getPrefs().putBoolean(operation.name() + closure.name(), value);		
	}

	private boolean load(Operation operation, Closure closure) {
		return getPrefs().getBoolean(operation.name() + closure.name(), isDefault(operation, closure));		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see no.javatime.inplace.dl.preferences.impl.PreferencesStore#flush()
	 */
	@Override
	public void flush() throws BackingStoreException {
		getPrefs().flush();
	}
}
