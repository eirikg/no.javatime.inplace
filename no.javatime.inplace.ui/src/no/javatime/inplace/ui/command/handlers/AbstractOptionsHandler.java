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

public abstract class AbstractOptionsHandler extends AbstractHandler implements IElementUpdater {

	public static String stateId = "org.eclipse.ui.commands.toggleState";

	
	/**
	 * Store the specified value in options store
	 * @param value saved in options store  
	 * @throws InPlaceException if the options store service could not be obtained
	 */
	abstract protected void storeValue(Boolean value) throws InPlaceException;

	/**
	 * Retrieves the stored value from the options store
	 * @return the stored value
	 * @throws InPlaceException if the options store service could not be obtained
	 */
	
	abstract protected boolean getStoredValue() throws InPlaceException;

	
	/**
	 * The command id of the menu element
	 * @return the command id
	 */
	abstract protected String getCommandId();
	
	/**
	 * The state id of the menu element
	 * @return the state id
	 */
	protected String getStateId() {
		return stateId;
	}
		
	/**
	 * The options command service
	 * @return the options command service
	 * @throws InPlaceException if the options store service could not be obtained
	 */
	protected CommandOptions getOptionsService() throws InPlaceException{
		return Activator.getDefault().getOptionsService();
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
		State state = event.getCommand().getState(getStateId());
		// Flip state value, sync state with store and update state
		Boolean stateVal = !(Boolean) state.getValue();
		storeValue(stateVal);
		CommandOptions cmdStore = getOptionsService();
		cmdStore.flush();
		state.setValue(stateVal); // calls updateElement
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
			boolean storedValue = getStoredValue();
			element.setChecked(storedValue);
		} catch (InPlaceException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.EXCEPTION, Activator.PLUGIN_ID, e.getMessage(), e),
					StatusManager.LOG);			
		}
	}
	
	/**
	 * The is enabled method is called before the menu is shown. If the stored state is different from the
	 * state of this command, update the command state and broadcast the change to update the checked state
	 * of the UI element. This may also happen if the stored value has been updated elsewhere. 
	 */
	@Override
	public boolean isEnabled() {

		ICommandService service = (ICommandService) Activator.getDefault().getWorkbench().getService(ICommandService.class);
			if (null != service) {
				// Get stored value and synch with state. 
				Command command = service.getCommand(getCommandId());
				State state = command.getState(getStateId());
				Boolean stateVal = (Boolean) state.getValue();
				Boolean storeVal = getStoredValue();
				// Values may be different if stored  value has been changed elsewhere (e.g. preference page)
				// If different update checked menu element before the menu becomes visible by broadcasting the change 
				if (!stateVal.equals(storeVal)) {
					state.setValue(storeVal);
					service.refreshElements(command.getId(), null);
				}
			}
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
}
