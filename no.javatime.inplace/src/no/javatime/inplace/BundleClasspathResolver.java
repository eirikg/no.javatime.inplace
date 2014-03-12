package no.javatime.inplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.IBundleClasspathResolver;

import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.bundleproject.BundleProject;
import no.javatime.inplace.bundleproject.ProjectProperties;

/**
 * Adds default output location to source folders when dynamically generated bundles 
 * added to BundleManager runtime launch and when looking up sources from the bundle
 */
public class BundleClasspathResolver implements IBundleClasspathResolver {

	/**
	 * Default constructor
	 */
	public BundleClasspathResolver() {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map getAdditionalClasspathEntries(IJavaProject javaProject) {
		Map<IPath, Collection<IPath>> additionalEntries = new HashMap<IPath, Collection<IPath>>(); 		
		IPath defaultOutputlocation = BundleProject.getDefaultOutputLocation(javaProject.getProject());
		Collection<IPath> srcPath = null;
		try {
			srcPath = ProjectProperties.getJavaProjectSourceFolders(javaProject.getProject());
			for (IPath path : srcPath) {
				additionalEntries.put(path, Collections.singletonList(defaultOutputlocation)); 
			}
		} catch (JavaModelException e) {
		} catch (InPlaceException e) {
		}
		return (Map) additionalEntries;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection getAdditionalSourceEntries(IJavaProject javaProject) {
		return new ArrayList();
	}
}
