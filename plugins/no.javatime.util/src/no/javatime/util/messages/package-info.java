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
/**
This is a minimal wrapper implementation of resource bundles for 
access of key/value pairs in categorized properties files.
<p>
There is one specialized class, of the base or general 
{@link no.javatime.util.messages.Message Message} 
class, for each category where each specialized class use it's own property file. 
An example is the <code>UserMessage</code> class who store it's key/value 
pairs in a usermessages.properties file.
<p>
The following categories represented by their class names exists:
<ul>
	<li>{@link no.javatime.util.messages.Message Message} General uncategorized messages
 	<li>{@link no.javatime.util.messages.UserMessage UserMessage} UI related information
 	<li>{@link no.javatime.util.messages.TraceMessage TraceMessage} Code tracing
 	<li>{@link no.javatime.util.messages.ErrorMessage ErrorMessage} General user related error messages
 	<li>{@link no.javatime.util.messages.ExceptionMessage ExceptionMessage} Used by Exception handlers
</ul>
 
Categorized key/value pairs is accessed through different
{@link no.javatime.util.messages.Message#getString(String, Object...) getString(key, ...)} members providing
support for plain text retrieval, substitution, translation, context information about callers and
directing of accessed messages to different output devices.
<p>

 <!-- <h2>Package Specification</h2> -->

 <h2>Related Documentation</h2>

 For overviews, tutorials, examples, guides, and tool documentation, please see:
 <ul>
 	 <li><a href="http://en.wikipedia.org/wiki/.properties">About properties files</a>
 	 <li><a href="http://java.sun.com/developer/technicalArticles/Intl/ResourceBundles/">Localization with ResourceBundles (Sun)</a>
 	 <li><a href="http://www.javaworld.com/javaworld/jw-04-2003/jw-0425-designpatterns.html?page=1">Singleton pattern</a>
 </ul>

 <!-- Put @see and @since tags down here. -->
 @see java.text.MessageFormat Message format API specification

 @since JTR 0.1.0
 */
package no.javatime.util.messages;


