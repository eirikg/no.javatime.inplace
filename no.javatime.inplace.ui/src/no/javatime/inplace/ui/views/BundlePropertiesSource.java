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

import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;
import no.javatime.util.messages.ErrorMessage;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
/**
 * Standard property source providing properties for projects and bundles  
 */
public class BundlePropertiesSource implements IPropertySource {
	
	private PropertyDescriptor bundleIdDescriptor = new PropertyDescriptor(BundleProperties.bundleIdLabelName, BundleProperties.bundleIdLabelName);
	private PropertyDescriptor bundleSymbolicNameDescriptor = new PropertyDescriptor(BundleProperties.bundleSymbolicNameLabelName, BundleProperties.bundleSymbolicNameLabelName);
	private PropertyDescriptor bundleVersionDescriptor = new PropertyDescriptor(BundleProperties.bundleVersionLabelName, BundleProperties.bundleVersionLabelName);
	private PropertyDescriptor projectNameDescriptor = new PropertyDescriptor(BundleProperties.projectLabelName, BundleProperties.projectLabelName);
	private PropertyDescriptor locationDescriptor = new PropertyDescriptor(BundleProperties.locationLabelName, BundleProperties.locationLabelName);
	
	private PropertyDescriptor activationStatusDescriptor = new PropertyDescriptor(BundleProperties.activationStatusLabelName, BundleProperties.activationStatusLabelName);
	
	private PropertyDescriptor bundleStateDescriptor = new PropertyDescriptor(BundleProperties.bundleStateLabelName, BundleProperties.bundleStateLabelName);
	private PropertyDescriptor bundleLastCmdDescriptor = new PropertyDescriptor(BundleProperties.lastTransitionLabelName, BundleProperties.lastTransitionLabelName);
	
	private PropertyDescriptor servicesInUseDescriptor = new PropertyDescriptor(BundleProperties.servicesInUseLabelName, BundleProperties.servicesInUseLabelName);

	private PropertyDescriptor numberOfRevisionsDescriptor = new PropertyDescriptor(BundleProperties.numberOfRevisionsLabelName, BundleProperties.numberOfRevisionsLabelName);
	private PropertyDescriptor activationPolicyDescriptor = new PropertyDescriptor(BundleProperties.activationPolicyLabelName, BundleProperties.activationPolicyLabelName);
	private PropertyDescriptor buildStatusDescriptor = new PropertyDescriptor(BundleProperties.bundleStatusLabelName, BundleProperties.bundleStatusLabelName);
	private PropertyDescriptor UIExtensionDescriptor = new PropertyDescriptor(BundleProperties.UIExtensionsLabelName, BundleProperties.UIExtensionsLabelName);
	private PropertyDescriptor lastInstalledOrUpdatedDescriptor = new PropertyDescriptor(BundleProperties.lastInstalledOrUpdatedLabelName, BundleProperties.lastInstalledOrUpdatedLabelName);

	private final BundleProperties bundleproperties;
	
	public BundlePropertiesSource(BundleProperties adaptableObject) {
		this.bundleproperties = adaptableObject;
		bundleIdDescriptor.setCategory(Msg.IDENTIFIERS_PROP_CATEGORY_LABEL);
		bundleSymbolicNameDescriptor.setCategory(Msg.IDENTIFIERS_PROP_CATEGORY_LABEL);
		bundleVersionDescriptor.setCategory(Msg.IDENTIFIERS_PROP_CATEGORY_LABEL);
		projectNameDescriptor.setCategory(Msg.IDENTIFIERS_PROP_CATEGORY_LABEL);
		locationDescriptor.setCategory(Msg.IDENTIFIERS_PROP_CATEGORY_LABEL);
		activationStatusDescriptor.setCategory(Msg.STATUS_PROP_CATEGORY_LABEL);
		bundleStateDescriptor.setCategory(Msg.STATUS_PROP_CATEGORY_LABEL);
		bundleLastCmdDescriptor.setCategory(Msg.STATUS_PROP_CATEGORY_LABEL);
		buildStatusDescriptor.setCategory(Msg.STATUS_PROP_CATEGORY_LABEL);
		servicesInUseDescriptor.setCategory(Msg.RESOLVED_DEPENDENCIES_PROP_CATEGORY_LABEL);		
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
//		PropertyDescriptor bundleNameDescriptor = new PropertyDescriptor(BundleProperties.bundleLabelName, bundleproperties.getBundleLabelName());
//		bundleNameDescriptor.setCategory(identifiersCategory);
		PropertyDescriptor requiringAllBundlesDescriptor = new PropertyDescriptor(BundleProperties.requiringDeclaredBundlesLabelName, bundleproperties.getAllRequiringLabelName());
		requiringAllBundlesDescriptor.setCategory(Msg.ALL_DEPENDENCIES_PROP_CATEGORY_LABEL);
		PropertyDescriptor providingAllBundlesDescriptor = new PropertyDescriptor(BundleProperties.providingDeclaredBundlesLabelName, bundleproperties.getAllProvidingLabelName());
		providingAllBundlesDescriptor.setCategory(Msg.ALL_DEPENDENCIES_PROP_CATEGORY_LABEL);
		
		PropertyDescriptor requiringResolvedBundlesDescriptor = new PropertyDescriptor(BundleProperties.requiringResolvedBundlesLabelName, bundleproperties.getResolvedRequiringLabelName());
		requiringResolvedBundlesDescriptor.setCategory(Msg.RESOLVED_DEPENDENCIES_PROP_CATEGORY_LABEL);
		PropertyDescriptor providingResolvedBundlesDescriptor = new PropertyDescriptor(BundleProperties.providingResolvedBundlesLabelName, bundleproperties.getResolvedProvidingLabelName());
		providingResolvedBundlesDescriptor.setCategory(Msg.RESOLVED_DEPENDENCIES_PROP_CATEGORY_LABEL);

		return new IPropertyDescriptor[] {
				// Bundle Status Category
				activationStatusDescriptor, 
				bundleStateDescriptor, 
				bundleLastCmdDescriptor, 
				buildStatusDescriptor,
				// All Dependencies Category
				requiringAllBundlesDescriptor, 
				providingAllBundlesDescriptor,
				// Resolved Dependencies Category
				requiringResolvedBundlesDescriptor,
				providingResolvedBundlesDescriptor,
				servicesInUseDescriptor,
				// Identifiers Category
				bundleIdDescriptor,
				bundleSymbolicNameDescriptor,
				bundleVersionDescriptor,
				locationDescriptor,
				projectNameDescriptor, 
				// Misc. Category
				numberOfRevisionsDescriptor,
				activationPolicyDescriptor,
				UIExtensionDescriptor,								
				lastInstalledOrUpdatedDescriptor			
		};
	}

	@Override
	public Object getPropertyValue(Object id) {

//		if (id.equals(BundleProperties.bundleLabelName)) {
//			return bundleproperties.getBundleName();
//		}
		if (id.equals(BundleProperties.bundleSymbolicNameLabelName)) {
			return bundleproperties.getSymbolicName();
		}
		if (id.equals(BundleProperties.bundleVersionLabelName)) {
			return bundleproperties.getBundleVersion();
		}
		if (id.equals(BundleProperties.projectLabelName)) {
			return bundleproperties.getProjectName();
		}
		if (id.equals(BundleProperties.bundleIdLabelName)) {
			return bundleproperties.getBundleId();
		}
		if (id.equals(BundleProperties.activationStatusLabelName)) {
			return bundleproperties.getActivationMode();
		}
		if (id.equals(BundleProperties.bundleStateLabelName)) {
			return bundleproperties.getBundleState();
		}
		try {
			if (id.equals(BundleProperties.lastTransitionLabelName)) {
				return bundleproperties.getLastTransition();
			}
			if (id.equals(BundleProperties.requiringDeclaredBundlesLabelName)) {
				return bundleproperties.getDeclaredRequiringBundleProjects();
			}
			if (id.equals(BundleProperties.providingDeclaredBundlesLabelName)) {
				return bundleproperties.getAllProvidingBundleProjects();
			}
			if (id.equals(BundleProperties.requiringResolvedBundlesLabelName)) {
				return bundleproperties.getResolvedRequiringBundleProjects();
			}
			if (id.equals(BundleProperties.providingResolvedBundlesLabelName)) {
				return bundleproperties.getResolvedProvidingBundleProjects();
			}
			if (id.equals(BundleProperties.servicesInUseLabelName)) {
				return bundleproperties.getServicesInUse();
			}
			if (id.equals(BundleProperties.numberOfRevisionsLabelName)) {
				return bundleproperties.getBundleRevisions();
			}
			if (id.equals(BundleProperties.activationPolicyLabelName)) {
				return bundleproperties.getActivationPolicy();
			}
			if (id.equals(BundleProperties.bundleStatusLabelName)) {
				return bundleproperties.getBundleStatus();
			}
			if (id.equals(BundleProperties.UIExtensionsLabelName)) {
				return bundleproperties.getUIExtension();
			}
			if (id.equals(BundleProperties.locationLabelName)) {
				try {
					return bundleproperties.getBundleLocationIdentifier();
				} catch (ProjectLocationException e) {
					String msg = ErrorMessage.getInstance().formatString("project_location", bundleproperties.getProject().getName());			
					IBundleStatus status = new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg,e);
					msg  = NLS.bind(Msg.REFRESH_HINT_INFO, bundleproperties.getProject().getName()); 
					status.add(new BundleStatus(StatusCode.INFO, Activator.PLUGIN_ID, msg));
					StatusManager.getManager().handle(status, StatusManager.LOG);
					return e.getLocalizedMessage();
				} catch (SecurityException e) {
					String msg = ErrorMessage.getInstance().formatString("project_location", bundleproperties.getProject().getName());			
					StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, msg, e),
							StatusManager.LOG);
					StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, null, e),
							StatusManager.LOG);
					return e.getLocalizedMessage();
				}
			}
			if (id.equals(BundleProperties.lastInstalledOrUpdatedLabelName)) {
				return bundleproperties.getLastInstalledOrUpdated();
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, null, e),
					StatusManager.LOG);
		}
		return null;	
	}

	@Override
	public boolean isPropertySet(Object id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	}
}
