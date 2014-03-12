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
package no.javatime.inplace.ui.command.handlers;

import java.util.Map;

import no.javatime.inplace.ui.Activator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

/**
 * Checked menu item allowing/disallowing activating bundles with ui contributions
 */
@SuppressWarnings("restriction")
public class NewPluginProjectHandler extends AbstractHandler implements IElementUpdater, IPageChangedListener {

	WizardDialog wizard = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event); 
		wizard = new WizardDialog(shell, new NewPluginProjectWizard());  
		wizard.setBlockOnOpen(false);
		wizard.open();   
		wizard.getShell().setImage(Activator.getImageDescriptor("icons/new_prj_bundle_wizard.gif").createImage());
		wizard.getShell().setText("New Bundle Project");
		wizard.setTitle("Bundle Project");
		wizard.setMessage("Specify a project name for the new bundle project");
		wizard.addPageChangedListener(this);	
		return null;    
	}	
	
	@Override
	public void dispose() {
		if (null != wizard) {
			wizard.removePageChangedListener(this);
		}
		super.dispose();
	}
	@Override
	public void updateElement(UIElement element, Map parameters) {
		// TODO Auto-generated method stub
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		IDialogPage idp = (IDialogPage) event.getSelectedPage();
		if (null != wizard) {
			wizard.getShell().setText("New Bundle Project");
		}
		if (idp.getTitle().equals("Plug-in Project")) {
			idp.setTitle("Bundle Project");
		} else if (idp.getTitle().equals("Content")) {
			idp.setTitle("Bundle Content");			
		} else if (idp.getTitle().equals("Templates")) {
			idp.setTitle("Bundle Templates");						
		} 
		return;
	}

}
