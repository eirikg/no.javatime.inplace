package no.javatime.inplace.bundlejobs.intface;

/**
 * Uninstalls pending bundle projects and optionally all requiring bundles of the pending bundle
 * projects from any state except UNINSTALLED.
 * 
 * @see Install
 * @see Deactivate
 */
public interface Uninstall {

	/**
	 * Determines if to include requiring bundles projects to pending projects added to this bundle
	 * executor when uninstalling
	 * <p>
	 * 
	 * @return if true include requiring bundle projects to uninstall and ignore requiring bundle
	 * projects if false
	 * @see #setAddRequiring(boolean)
	 */
	public boolean isAddRequiring();

	/**
	 * Option to include requiring bundle projects to pending projects added to this bundle executor
	 * when uninstalling.
	 * <p>
	 * Default is to include requiring bundle projects.
	 * 
	 * @param includeRequiring if true include requiring bundle projects to uninstall. Otherwise
	 * ignore any requiring bundle projects.
	 * @see #isAddRequiring()
	 */
	public void setAddRequiring(boolean includeRequiring);

	/**
	 * Check if the bundle project is set to be unregistered from the workspace region after uninstall
	 * 
	 * @return true if the bundle project is going to be unregistered from the workspace region after
	 * uninstalling and false if not.
	 * @see #setUnregister(boolean)
	 */
	public boolean isUnregister();

	/**
	 * Unregister all pending bundle projects from the workspace region after they are uninstalled.
	 * <p>
	 * Projects are automatically registered in the workspace region when installed.
	 * <p>
	 * Default is to not unregister projects after they are uninstalled.
	 * <p>
	 * Projects are unregistered unconditionally when the workspace region is deactivated, at shut
	 * down of the IDE and when projects are deleted from the workspace.
	 * 
	 * @param unregister true to unregister all pending bundle projects from the workspace region and
	 * false to keep the projects registered in the workspace region after uninstalling.
	 * @see #isUnregister()
	 */
	public void setUnregister(boolean unregister);

}