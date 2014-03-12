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
package no.javatime.util.deltatime;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import no.javatime.util.messages.ErrorMessage;
import no.javatime.util.messages.Message;

/**
 *
 */
public class Dates {

	public static String ID = Dates.class.getName();

	/*
	 * Substituting the declaration of instance and the method #getInstance()
	 * with:
	 * 
	 * public final static Dates INSTANCE = new Dates();
	 * 
	 * gives better performance but is rigorous to changes
	 */
	private static Dates instance = null;

	/**
	 * Prevent outside, not inherited classes from instantiation.
	 * Initialize which default output device to use and if prefix should be used.
	 */
	protected Dates() {
	}

	/**
	 * This access the singleton
	 * @return the instance of the <code>Message</code>
	 */
	public synchronized static Dates getInstance() {
		if (instance == null) {
			instance = new Dates();
		}
		return instance;
	}
	
/**
	 * Compose a string representing a XMLGregorianCalendar based on a 
	 * default format specified in the Message resource bundle
	 * @param XMLdate is the date to convert to a string
	 * @return the string representation of the date
	 */
	public String dateFormat(XMLGregorianCalendar XMLdate) {

		// DatatypeFactory.newInstance().newXMLGregorianCalendar();
		GregorianCalendar date = new GregorianCalendar(Locale.getDefault());
    // Prepare for conversion
		date.clear();
    date.setGregorianChange(new Date(Long.MIN_VALUE));
    date = XMLdate.toGregorianCalendar();
    return Message.getInstance().getString("date_format", date.getTime());
	}

	public String dateFormat(Date date) {

    return Message.getInstance().getString("date_format", date.getTime());
	}
	
	public static GregorianCalendar dateFormat(String timestamp)
	throws Exception {
		/*
		 ** we specify Locale.US since months are in english
		 */
		// SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault());
		//Date date = sdf.parse(timestamp);
		DateFormat df = DateFormat.getTimeInstance(DateFormat.FULL, Locale.getDefault());
		Date date = df.parse(timestamp);
		GregorianCalendar cal = new GregorianCalendar(Locale.getDefault());
		cal.setTime(date);
		return cal;
	}

	/**
	 * Helper member creating and initializing XMLGregorianCalender with the
	 * current date and time.
	 * 
	 * @return XMLGregorianCalendar initialized with the current date and time or null
	 */
	public XMLGregorianCalendar createXMLGregorianCalendar() {
		XMLGregorianCalendar cal = null;
		try {
			DatatypeFactory xmlCal = DatatypeFactory.newInstance();
			cal = xmlCal.newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			ErrorMessage.getInstance().getString("create_current_date", new GregorianCalendar());
		}
		return cal;
	}
}
