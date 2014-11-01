package no.javatime.inplace.bundlejobs.intface;


/**
 * 		
 * // TODO Use this as a starting point when creating service for jobs Remove after testing
		Extension<Extender<?>> e;
		e = Extenders.getExtension(Extender.class.getName());
		Extender<?> ex = e.getService(context.getBundle());
		BundleScopeServiceFactory<ActivateBundle> bundleScopeFactory = 
				new BundleScopeServiceFactory<>(ActivateBundleJob.class.getName());
		Extender<ActivateBundle> activateExtender = 
				Extenders.register(context.getBundle(), context.getBundle(), ActivateBundle.class.getName(), 
						bundleScopeFactory);
						// Specific factory for activate job
//						new BundleJobServiceFactory());
		ActivateBundle activate = activateExtender.getService();
		activate.addPendingProjects(BundleProjectImpl.INSTANCE.getCandidateProjects());
		activateExtender.unregisterService();
		activateExtender.registerService();
		activate = activateExtender.getService();

 *
 */
public interface ActivateBundle extends Bundles {

	/**
	 * Set preference for activating bundles according to bundle state in preference store
	 * 
	 * @param useStoredState true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public void setUseStoredState(Boolean useStoredState);

	/**
	 * Check if to activate bundles according to bundle state in preference store
	 * 
	 * @return true if bundle state from preference store is to be used. Otherwise false
	 * @see no.javatime.inplace.InPlace#savePluginSettings(Boolean, Boolean)
	 */
	public Boolean getUseStoredState();

}