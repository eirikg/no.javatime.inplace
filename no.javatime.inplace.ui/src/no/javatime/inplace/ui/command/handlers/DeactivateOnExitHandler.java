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

import no.javatime.inplace.bundlemanager.InPlaceException;
import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.statushandler.BundleStatus;
import no.javatime.inplace.statushandler.IBundleStatus.StatusCode;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.msg.Msg;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

public class DeactivateOnExitHandler extends AbstractHandler implements IElementUpdater {

	public static String commandId = "no.javatime.inplace.command.deactivate";
	public static String stateId = "org.eclipse.ui.commands.toggleState";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		try {
			State state = event.getCommand().getState(stateId);
			CommandOptions cmdStore = Activator.getDefault().getOptionsService();
			// Flip state value, update state and sync state with store
			Boolean stateVal = !(Boolean) state.getValue();
			state.setValue(stateVal);
			cmdStore.setIsDeactivateOnExit(stateVal);
			cmdStore.flush();
		} catch (BackingStoreException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, Msg.PREFERENCE_FLUSH_EXCEPTION, e),
					StatusManager.LOG);
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}

		return null;
	}

	/**
	 * Update the checked menu element with the last updated value from the command options preference store
	 * <p>
	 * Invoked by framework on first contribution (first time menu entry is shown) and after each refresh in (See
	 * {@linkplain #isEnabled()}
	 */
	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		try {
			CommandOptions cmdStore = Activator.getDefault().getOptionsService();
			element.setChecked(cmdStore.isDeactivateOnExit());
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
	}
	
	/**
	 * The is enabled method is called before the menu is shown. If the stored state is different from the
	 * state of this command update the command state and broadcast the change to update the checked state
	 * of the UI element. This may happen if the stored value has been updated elsewhere. 
	 */
	@Override
	public boolean isEnabled() {

		try {
			ICommandService service = (ICommandService) Activator.getDefault().getWorkbench().getService(ICommandService.class);
			if (null != service) {
				// Get stored value and synch with state. 
				Command command = service.getCommand(commandId);
				CommandOptions cmdStore = Activator.getDefault().getOptionsService();
				State state = command.getState(stateId);
				Boolean stateVal = (Boolean) state.getValue();
				Boolean storeVal = cmdStore.isDeactivateOnExit();
				// Values may be different if stored  value has been changed elsewhere (e.g. preference page)
				// If different update checked menu element before the menu becomes visible by broadcasting the change 
				if (!stateVal.equals(storeVal)) {
					state.setValue(storeVal);
					service.refreshElements(command.getId(), null);
				}
			}
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
}
