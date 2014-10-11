package no.javatime.inplace.bundlejobs;

import java.util.Collection;

import org.eclipse.core.resources.IProject;

public class BundleJobFactoryImpl extends NatureJob {
	/**
	 * Construct a transaction job with a given job name
	 * 
	 * @param name job name
	 */
	public BundleJobFactoryImpl(String name) {
		super(name);
	}

	/**
	 * Constructs a transaction job with a given job name and pending bundle projects
	 * 
	 * @param name job name
	 * @param projects pending projects to process
	 */
	public BundleJobFactoryImpl(String name, Collection<IProject> projects) {
		super(name, projects);
	}

	/**
	 * Constructs a transaction job with a given job name and a pending bundle project
	 * 
	 * @param name job name
	 * @param project pending project to process
	 */
	public BundleJobFactoryImpl(String name, IProject project) {
		super(name, project);
	}
	
//	public ActivateBundle createActivateBundle(String name) {	
//		ActivateBundle abj = new ActivateBundleJob(name);
//		return abj;
//	}
}
