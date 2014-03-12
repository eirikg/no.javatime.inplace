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

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import no.javatime.inplace.bundlemanager.BundleManager;
import no.javatime.inplace.bundlemanager.BundleTransition;
import no.javatime.inplace.bundleproject.ProjectProperties;
import no.javatime.util.messages.Category;

public class AutoUpdateHandler extends AbstractHandler implements IElementUpdater {

	public static String commandId = "no.javatime.inplace.command.autoupdate";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Boolean prevState = HandlerUtil.toggleCommandState(event.getCommand());
		Category.setState(Category.autoUpdate, !prevState);
		if (!prevState) {
			Collection<IProject> projects = ProjectProperties.getActivatedProjects();
			Collection<IProject> projectsToUpdate = 
					BundleManager.getTransition().getPendingProjects(projects, BundleTransition.Transition.UPDATE);			
			BundleMainActivationHandler.updateHandler(projectsToUpdate);
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		Command command = BundleMenuActivationHandler.setCheckedMenuEntry(Category.autoUpdate, commandId);
		if (null != command) {
			element.setChecked(Category.getState(Category.autoUpdate));
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
}
