package no.javatime.inplace.bundlejobs.intface;

import java.util.Collection;

import no.javatime.inplace.region.status.IBundleStatus;

import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public interface Status {

	/**
	 * Get all trace status objects added by this job
	 * 
	 * @return a list of status trace objects or an empty list
	 * 
	 * @see #addTrace(String, Bundle, IProject)
	 * @see #addTrace(String, Object[], Object)
	 */
	public Collection<IBundleStatus> getTraceList();

	/**
	 * Get all status information added by this job
	 * 
	 * @return a list of status objects where each status object describes the nature of the status or an empty list
	 */
	public Collection<IBundleStatus> getStatusList();

	/**
	 * Number of status elements registered
	 * 
	 * @return number of status elements
	 */
	public int statusList();

	/**
	 * Check if any bundle status objects have been added to the status list
	 * 
	 * @return true if bundle status objects exists in the status list, otherwise false
	 */
	public boolean hasStatus();

}