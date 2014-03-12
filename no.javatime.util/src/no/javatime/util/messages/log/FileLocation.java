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
package no.javatime.util.messages.log;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.Message;
import no.javatime.util.messages.exceptions.ViewException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

/**
 * Storage locations for all program and data files produced by JavaTime. There is
 * one data folder and one model folder, both sub folders of the working 
 * folder which is the root folder of the project.  
 */
public class FileLocation {

	/**
	 * Unique ID of the class
	 */
	public static String ID = FileLocation.class.getName();
	private static FileLocation instance = null;

	protected FileLocation() {
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>FileLocation</class>
	 */
	public synchronized static FileLocation getInstance() {
		if (instance == null) {
			instance = new FileLocation();
		}
		return instance;
	}

	/** The current working directory will be the root folder of the project */
	public static final String WORKING_FOLDER;
	static {
		String pathName = ".";
		try {
			IPath projectRoot = Platform.getLocation();
			pathName = MessageFormat.format("{0}\\", projectRoot.toOSString());			
		} catch (IllegalStateException e) {
			// This will raise an init exception of a static class and never happen
			throw new ViewException(e, "io_exception", 
					ExceptionMessage.getInstance().getString("working_folder"));
		}
		WORKING_FOLDER = pathName;
	}

	/** The data sub folder name of the working folder */
	public static final String DATA_SUBFOLDER_NAME = 
			Message.getInstance().formatString("data_subfolder_name") + "\\";
	
	/** XML and log files instances are stored in the "data" sub folder of the project */
	public static final String DATA_SUBFOLDER = WORKING_FOLDER + DATA_SUBFOLDER_NAME;

	/** The model sub folder name of the working folder. */
	public static final String MODEL_SUBFOLDER_NAME = 
			Message.getInstance().formatString("model_subfolder_name") + "/";

	/** Design and run time meta files are stored in "model" sub folder of the project */
	public static final String MODEL_SUBFOLDER = WORKING_FOLDER + MODEL_SUBFOLDER_NAME;

	/**
	 * Location of all java class files
	 * @param clazz to find the location for
	 * @param dotNotation if true
	 * @return the location in dot or canonical path representation or null
	 */
	public String getClassFileName(Class<?> clazz, boolean dotNotation) {
	
		URL location;
 
		String classLocation = clazz.getName().replace('.', '/') + ".class";

		/*
		if (dotNotation)
			return URI.createFileURI(clazz.getName().replace('.', '\\') + ".class").toFileString();
    */
    final ClassLoader loader = clazz.getClassLoader();
    if (loader == null) {
      location = ClassLoader.getSystemResource(classLocation);
    } else {
    	location = loader.getResource(classLocation);
    }
    if (location == null) {
			try {
				location = new URL(ErrorMessage.getInstance().getString("can_not_load_class", classLocation));
			} catch (MalformedURLException e) {
				ErrorMessage.getInstance().getString("invalid_url", classLocation);
				return null;
			}
    }
    classLocation = location.getFile();
    if (dotNotation) {
    	classLocation = classLocation.replace('/', '.');
  		return classLocation.substring(classLocation.indexOf('.')+1, classLocation.length());
    } else {
    	return FileLocation.WORKING_FOLDER.substring(0, FileLocation.WORKING_FOLDER.lastIndexOf('\\')) +
    			location.getFile();
		}
	}

	/**
	 * Location of all java source files
	 * @param clazz to find the location for
	 * @param dotNotation if true
	 * @return the location in dot or canonical path representation
	 */
	public String getSourceFileName(Class<?> clazz, boolean dotNotation) {
		
		String sourceLocation = clazz.getName()+ ".java";
    if (!dotNotation) {
    	sourceLocation = FileLocation.WORKING_FOLDER + sourceLocation.replace('.', '\\');
    }
    return sourceLocation;
	}
}
