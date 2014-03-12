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
package no.javatime.inplace;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;

/**
 * Access previous saved state so change events will be created for all
 * changes that have occurred since the last save
 */
public class WorkspaceSaveParticipant implements ISaveParticipant {

	@Override
	public void doneSaving(ISaveContext context) {	
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
	}

	@Override
	public void rollback(ISaveContext context) {
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
    // No state to be saved by the plug-in, but request a
    // resource delta to be used on next activation.
    context.needDelta();
	}

}
