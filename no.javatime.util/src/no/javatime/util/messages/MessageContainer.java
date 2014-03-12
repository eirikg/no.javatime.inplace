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
package no.javatime.util.messages;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.javatime.util.messages.views.MessageView;

/**
 * Simple container for messages displayed in the {@link MessageView}. The
 * messages are all high level user messages reporting different events.
 */
public class MessageContainer {

	/*
	 * By substituting the declaration of instance and the method #getInstance()
	 * with:
	 * 
	 * public final static Message INSTANCE = new Message();
	 * 
	 * gives better performance but is rigorous to changes
	 */
	private static MessageContainer instance = null;

	/**
	 * Limit the in memory number of messages
	 */
	private int maxNoOfMessages = 1024;

	/**
	 * Prevent outside not inherited classes from instantiation.
	 */
	protected MessageContainer() {
	}

	/**
	 * This access the singleton
	 * 
	 * @return the instance of the <code>Message</class>
	 */
	public synchronized static MessageContainer getInstance() {
		if (instance == null) {
			instance = new MessageContainer();
		}
		return instance;
	}

	protected ConcurrentLinkedQueue<MessageItem> msgItems = new ConcurrentLinkedQueue<MessageItem>();

	static protected class MessageItem {

		public String msgContext = null;
		public String msgText = null;

		public MessageItem(String msgKey, String msgContext, String msgText) {

			this.msgContext = msgContext;
			this.msgText = msgText;
		}
	}

	/**
	 * Returns the message texts as an array
	 * 
	 * @return the list of messages.
	 */
	public synchronized String[] getMessages() {

		int size = msgItems.size();
		String[] messages = new String[size];
		int i = size;
		Iterator<MessageItem> iterator = msgItems.iterator();
		for (;iterator.hasNext() && --i >= 0;) {
			MessageItem msgItem = iterator.next();
			if (msgItem.msgContext != null) {
				messages[i] = msgItem.msgContext + msgItem.msgText;
			} else if (msgItem.msgText != null) {
				messages[i] = msgItem.msgText;
			}
		}
		return messages;
	}

	@Override
	public synchronized String toString() {
		return Arrays.toString(getMessages()); 
	}

	/**
	 * Adds a message to the message list.
	 * 
	 * @param message the message to add to the list
	 */
	public synchronized void addMessage(final String message) {
		if (msgItems.size() >= maxNoOfMessages) {
			//msgItems.clear();
			msgItems.poll();
		}
		msgItems.add(new MessageItem("key", "context", message));
	}

	public synchronized void addMessage(String key, String context, String message) {
		if (msgItems.size() >= maxNoOfMessages) {
			//msgItems.clear();
			msgItems.poll();
		}
		msgItems.add(new MessageItem(key, context, message));
	}
	
	/**
	 * Finds a message in the container
	 * 
	 * @param msg the message to search for
	 * @return true if message exist in container
	 */
	public synchronized boolean isMessage(String msg) {
		if (msg == null)
			return false;
		for (Iterator<MessageItem> iterator = msgItems.iterator(); iterator.hasNext();) {
			MessageItem msgItem = iterator.next();
			if (msgItem.msgText != null && msgItem.msgText.equals(msg)) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized int size() {
		return msgItems.size();
	}
	
	public synchronized void clearMessages() {
		this.msgItems.clear();
	}
}
