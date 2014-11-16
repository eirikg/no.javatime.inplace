package no.javatime.inplace.region.state;

import java.util.HashMap;

import org.osgi.framework.Bundle;

public class StateFactory {
	
	public static final int	STATELESS				= 0x00000000;
	
	public final static StateFactory INSTANCE = new StateFactory();
	
	public final StateLess stateLess = new StateLess();
	public final UninstalledState uninstalledState = new UninstalledState();
	public final InstalledState installedState = new InstalledState();
	public final ResolvedState resolvedState = new ResolvedState();
	public final StartingState startingState = new StartingState();
	public final ActiveState activeState = new ActiveState();
	public final StoppingState stoppingState = new StoppingState();
	
	private HashMap<Integer, BundleState> states = new HashMap<>();
	
	private StateFactory() {
		states.put(STATELESS, stateLess);
		states.put(Bundle.UNINSTALLED, uninstalledState);
		states.put(Bundle.INSTALLED, installedState);
		states.put(Bundle.RESOLVED, resolvedState);
		states.put(Bundle.STARTING, startingState);
		states.put(Bundle.ACTIVE, activeState);		
		states.put(Bundle.STOPPING, stoppingState);				
	}
	
	/**
	 * Returns the state class representing one of the state constants
	 * specified in {@link Bundle}
	 * 
	 * @param stateConstant one of the {@link Bundle} state constants 
	 * @return the state class corresponding to the specified {@link Bundle} constant
	 */
	public BundleState get(Integer stateConstant) {
		return states.get(stateConstant);
	}
}
