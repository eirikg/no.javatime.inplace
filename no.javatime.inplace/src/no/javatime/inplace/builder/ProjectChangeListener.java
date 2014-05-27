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
package no.javatime.inplace.builder;

import no.javatime.inplace.InPlace;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.Message.Output;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * Debug listener. Indents resource change events to CONSOLE.
 * <p>
 * The calss is not documented.
 * @see no.javatime.util.messages.Category#listeners
 */
public class ProjectChangeListener implements IResourceChangeListener {


	/** Indicate that a full build should be carried out */
	private void setFullBuildRequired(){
	}

	/** Process an IResourceChangeEvent */
	public void resourceChanged(IResourceChangeEvent event){
		//Event(event);
		IResourceDelta delta = event.getDelta();
		printEvent(event);
		resourceChanged(delta);
	}

	/** Process an IResourceDelta */
	public boolean resourceChanged(IResourceDelta delta){
		if (delta == null) return false;

		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) throws CoreException {
				switch (delta.getResource().getType()){
				case IResource.ROOT:
					// do nothing
					break;
				case IResource.PROJECT:
					processProjectChange(delta);
					// do nothing
					break;
				case IResource.FILE:
					// do nothing
					break;
				case IResource.FOLDER:
					// do nothing
					break;
				default:
					// do nothing
					break;
				}
				return true; // visit children
			}	
		};
		try{
			delta.accept(visitor);
		} catch (CoreException ex){
			// Ignore
		}
		return true;
	}

	/** Handle a change against the Project itself */
	private void processProjectChange(IResourceDelta delta){
		// Do nothing for now.
		switch (delta.getFlags()) {
		case IResourceDelta.DESCRIPTION:
			break;
		case IResourceDelta.OPEN:
			setFullBuildRequired();
			break;
		case IResourceDelta.TYPE:
			break;
		case IResourceDelta.SYNC:
			setFullBuildRequired();
			break;
		case IResourceDelta.MARKERS:
			break;
		case IResourceDelta.REPLACED:
			setFullBuildRequired();
			break;
		case IResourceDelta.MOVED_TO:
			setFullBuildRequired();
			break;
		case IResourceDelta.MOVED_FROM:
			break;
		default:
			break;
		}
		StringBuffer buf = new StringBuffer();
		printResourcesChanged(delta, buf, 2);
		Message.getInstance().getRawString(Message.defKey, Output.console, buf.toString());
	}

	/** Handle a resource change against a folder */
	@SuppressWarnings("unused")
	private void processFolderChange(IResourceDelta delta){
		switch(delta.getKind()){
		case IResourceDelta.ADDED:
			break;
		case IResourceDelta.REMOVED:
			if (delta.getFlags() == IResourceDelta.MOVED_TO)
				processInterestingFolderMove(delta);
			else
				processInterestingFolderRemoval(delta);
			break; 
		case IResourceDelta.CHANGED:
			processInterestingFolderChange(delta);
			break;
		default: 
			break;
		}
	}

	/** Handle the change of a folder in the project */
	private void processInterestingFolderChange(IResourceDelta delta){
		// we don't really care that the folder changed at the moment

		//		switch(delta.getFlags()){
		//		case IResourceDelta.CONTENT:
		//		case IResourceDelta.DESCRIPTION:
		//		case IResourceDelta.OPEN:
		//		case IResourceDelta.TYPE:
		//		case IResourceDelta.SYNC:
		//		case IResourceDelta.MARKERS:
		//		case IResourceDelta.REPLACED:
		//		case IResourceDelta.MOVED_TO:
		//		case IResourceDelta.MOVED_FROM:
		//		default:
		//			break;
		//		}
	}

	/** Handle the removal of a folder in the project  */
	private void processInterestingFolderRemoval(IResourceDelta delta){
		setFullBuildRequired();
	}

	/** Handle the move of a folder in the project */
	private void processInterestingFolderMove(IResourceDelta delta){
		setFullBuildRequired();
	}

	/** Handle the change of a file in the project */
	@SuppressWarnings("unused")
	private void processFileChange(IResourceDelta delta) {
		setFullBuildRequired();
	}

	@SuppressWarnings("unused")
	private static void printChange(IResourceDelta delta, StringBuffer buffer) {
		buffer.append("Flag: ");
		switch (delta.getFlags()) {
		case IResourceDelta.CONTENT:
			buffer.append(" (CONTENT)");
			break;
		case IResourceDelta.ENCODING:
			buffer.append(" (ENCODING)");
			break;
		case IResourceDelta.DESCRIPTION:
			buffer.append(" (DESCRIPTION)");
			break;
		case IResourceDelta.OPEN:
			buffer.append(" (OPEN)");
			break;
		case IResourceDelta.TYPE:
			buffer.append(" (TYPE)");
			break;
		case IResourceDelta.SYNC:
			buffer.append(" (SYNC)");
			break;
		case IResourceDelta.MARKERS:
			buffer.append(" (MARKERS)");
			break;
		case IResourceDelta.REPLACED:
			buffer.append(" (REPLACED)");
			break;
		case IResourceDelta.MOVED_TO:
			buffer.append(" (MOVED_TO)");
			break;
		case IResourceDelta.MOVED_FROM:
			buffer.append(" (MOVED_FROM)");
			break;
		default:
			buffer.append(" (<unknown>)");
		}
	}

	/** Debug code */
	private static void printOneResourceChanged(IResourceDelta delta, StringBuffer buffer, int indent) {
		for (int i = 0; i < indent; i++) {
			buffer.append(' ');
		}
		buffer.append("Kind: ");
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			buffer.append("ADDED");
			break;
		case IResourceDelta.REMOVED:
			buffer.append("REMOVED");
			break;
		case IResourceDelta.CHANGED:
			buffer.append("CHANGED");
			break;
		default:
			buffer.append('[');
			buffer.append(delta.getKind());
			buffer.append(']');
		}
		buffer.append(' ');
		buffer.append(printFlags(delta));
		// printChange(delta, buffer);
		buffer.append(' ');
		buffer.append("Resource delta is: ");
		buffer.append(delta.getResource());
		buffer.append('\n');
	}

	private static void printResourcesChanged(IResourceDelta delta, StringBuffer buffer, int indent) {
		printOneResourceChanged(delta, buffer, indent);
		IResourceDelta[] children = delta.getAffectedChildren();
		for (int i = 0; i < children.length; i++) {
			printResourcesChanged(children[i], buffer, indent + 1);
		}
	}

	public static void printEvent(IResourceChangeEvent event) {
		StringBuffer buffer = new StringBuffer(80);
		buffer.append("Event type: ");
		switch (event.getType()) {
		case IResourceChangeEvent.POST_BUILD:
			buffer.append("[POST_BUILD]");
			break;
		case IResourceChangeEvent.POST_CHANGE:
			buffer.append("[POST_CHANGE]");
			break;
		case IResourceChangeEvent.PRE_BUILD:
			buffer.append("[PRE_BUILD]");
			break;
		case IResourceChangeEvent.PRE_CLOSE:
			buffer.append("[PRE_CLOSE]");
			break;
		case IResourceChangeEvent.PRE_DELETE:
			buffer.append("[PRE_DELETE]");
			break;
		default:
		}
		buffer.append(".\n");
		Message.getInstance().getRawString(Message.defKey, Output.console, buffer.toString());
	}

	public static void printResourceChanges(IResourceDelta delta) {
		StringBuffer buffer = new StringBuffer(80);
		if (delta != null)
			printResourcesChanged(delta, buffer, 0);
		Message.getInstance().getRawString(Message.defKey, Output.console, buffer.toString());
	}
	
	public static void printEvent(IResourceChangeEvent event, IProject project) {
		StringBuffer buffer = new StringBuffer(80);
		buffer.append("Project: ");
		buffer.append(project.getName());
		buffer.append(' ');
		buffer.append("Event type: ");
		switch (event.getType()) {
		case IResourceChangeEvent.POST_BUILD:
			buffer.append("[POST_BUILD]");
			break;
		case IResourceChangeEvent.POST_CHANGE:
			buffer.append("[POST_CHANGE]");
			break;
		case IResourceChangeEvent.PRE_BUILD:
			buffer.append("[PRE_BUILD]");
			break;
		case IResourceChangeEvent.PRE_CLOSE:
			buffer.append("[PRE_CLOSE]");
			break;
		case IResourceChangeEvent.PRE_DELETE:
			buffer.append("[PRE_DELETE]");
			break;
		default:
		}
		InPlace.get().trace("{0}", buffer.toString());
	}

	public static void printKind(int kind) {

		if (kind == IResourceDelta.ADDED) {

			InPlace.get().trace("{0}", "Kind --> Added");
		}
		if (kind == IResourceDelta.CHANGED) {
			InPlace.get().trace("{0}", "Kind --> Changed");
		}
		if (kind == IResourceDelta.REMOVED) {
			InPlace.get().trace("{0}", "Kind --> Removed");
		}
		if (kind == IResourceDelta.REMOVED_PHANTOM) {
			InPlace.get().trace("{0}", "Kind --> Removed phantom");
		}
		if (kind == IResourceDelta.ADDED_PHANTOM) {
			InPlace.get().trace("{0}", "Kind --> Added phantom");
		}
	}

	public static String  printFlags(IResourceDelta resourceDelta) {
		int flags = resourceDelta.getFlags(); 
		StringBuffer buf = new StringBuffer();
		
		if ((flags & IResourceDelta.CONTENT) != 0) {		
			buf.append(" Flag: Content Change");
		}
		if ((flags & IResourceDelta.DERIVED_CHANGED) != 0) {
			buf.append(" Flag: Derived changed");
		}
		if ((flags & IResourceDelta.ENCODING) != 0) {
			buf.append(" Flag: Encoding");
		}
		if ((flags & IResourceDelta.DESCRIPTION) != 0) {
			buf.append(" Flag: Description");
		}
		if ((flags & IResourceDelta.OPEN) != 0) {
			buf.append(" Flag: Project open");
		}
		if ((flags & IResourceDelta.TYPE) != 0) {
			buf.append(" Flag: Type changed");
		}
		if ((flags & IResourceDelta.SYNC) != 0) {
			buf.append(" Flag: Sync");
		}
		if ((flags & IResourceDelta.MARKERS) != 0) {
			buf.append(" Flag: Markers");
		}
		if ((flags & IResourceDelta.REPLACED) != 0) {
			buf.append(" Flag: Content Replaced");
		}
		if ((flags & IResourceDelta.LOCAL_CHANGED) != 0) {
			buf.append(" Flag: Local canged");
		}
		if ((flags & IResourceDelta.MOVED_FROM) != 0) {
			buf.append(" Flag: Moved from ");
			buf.append(resourceDelta.getMovedFromPath());
		}
		if ((flags & IResourceDelta.MOVED_TO) != 0) {
			buf.append(" Flag: Moved to ");
			buf.append(resourceDelta.getMovedToPath());
		}
		if ((flags & IResourceDelta.COPIED_FROM) != 0) {
			buf.append(" Flag: Copied from ");
			buf.append(resourceDelta.getMovedFromPath());
		}
		if (buf.length() == 0) {
			buf.append(" Flag: No flag or unknown");
		}
			
		return buf.toString();
	}

	static public void traceDeltaKind(IResourceChangeEvent event, IResourceDelta resourceDelta, IProject project) {
		if (Category.DEBUG && Category.getState(Category.listeners)) {
			ProjectChangeListener.printEvent(event, project);
			ProjectChangeListener.printKind(resourceDelta.getKind());
			ProjectChangeListener.printFlags(resourceDelta);
		}		
	}

}
