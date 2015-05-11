package no.javatime.inplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.intface.BundleProjectMeta;
import no.javatime.inplace.region.intface.InPlaceException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.IBundleClasspathResolver;

/**
 * Adds default output location to source folders when dynamically generated bundles 
 * added to BundleExecutorEventManagerImpl runtime launch and when looking up sources from the bundle
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
		BundleProjectMeta bundleProjectMeta = Activator.getbundlePrrojectMetaService();
		IPath defaultOutputlocation = bundleProjectMeta.getDefaultOutputFolder(javaProject.getProject());
		Collection<IPath> srcPath = null;
		try {
			srcPath = bundleProjectMeta.getSourceFolders(javaProject.getProject());
			for (IPath path : srcPath) {
				additionalEntries.put(path, Collections.<IPath>singletonList(defaultOutputlocation)); 
			}
		} catch (InPlaceException | JavaModelException | ExtenderException e) {
		}
		return (Map) additionalEntries;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection getAdditionalSourceEntries(IJavaProject javaProject) {
		return new ArrayList();
	}
}
