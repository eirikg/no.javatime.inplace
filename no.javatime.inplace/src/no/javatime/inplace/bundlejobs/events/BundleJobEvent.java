package no.javatime.inplace.bundlejobs.events;

import org.eclipse.core.resources.WorkspaceJob;

public interface BundleJobEvent {

	public WorkspaceJob getBundleJob();

	public long getDelay();

}