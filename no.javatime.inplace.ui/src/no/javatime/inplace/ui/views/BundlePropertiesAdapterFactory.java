/*******************************************************************************
 * Copyright (c) 2011, 2012 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.ui.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.views.properties.IPropertySource;

public class BundlePropertiesAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if (adapterType == IPropertySource.class && adaptableObject instanceof BundleProperties){
			return new BundlePropertiesSource((BundleProperties) adaptableObject);
		}
		if (adapterType == IProject.class && adaptableObject instanceof BundleProperties){
			return  ((BundleProperties) adaptableObject).getProject();
		}
		if (adapterType == IJavaProject.class && adaptableObject instanceof BundleProperties){
			return  ((BundleProperties) adaptableObject).getJavaProject();
		}
		if (adapterType == IResource.class && adaptableObject instanceof BundleProperties){
			return  ((BundleProperties) adaptableObject).getJavaProject().getResource();
		}		
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class, IProject.class, IJavaProject.class, IResource.class };
	}

}
