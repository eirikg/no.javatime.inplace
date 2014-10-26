package no.javatime.inplace.bundlejobs.intface;

import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public interface Nature extends Bundles {

	/**
	 * Toggles JavaTime nature on a project. If the project has the JavaTime nature, the nature is removed and
	 * if the the project is not nature enabled, the JavaTime nature is added.
	 * 
	 * @param project the project to add or remove JavaTime nature on
	 * @param monitor the progress monitor to use for reporting progress.
	 * @throws InPlaceException Fails to remove or add the nature from/to the project.
	 */
	public void toggleNatureActivation(IProject project, IProgressMonitor monitor)
			throws InPlaceException;

}