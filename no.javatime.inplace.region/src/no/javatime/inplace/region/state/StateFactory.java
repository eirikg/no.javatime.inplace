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
	public final LazyState lazyState = new LazyState();
	public final ActiveState activeState = new ActiveState();
	
	private HashMap<Integer, BundleState> states = new HashMap<Integer, BundleState>();
	
	private StateFactory() {
		states.put(STATELESS, stateLess);
		states.put(Bundle.UNINSTALLED, uninstalledState);
		states.put(Bundle.INSTALLED, installedState);
		states.put(Bundle.RESOLVED, resolvedState);
		states.put(Bundle.STARTING, lazyState);
		states.put(Bundle.ACTIVE, activeState);		
	}

	public BundleState get(Integer stateConstant) {
		return states.get(stateConstant);
	}
}
