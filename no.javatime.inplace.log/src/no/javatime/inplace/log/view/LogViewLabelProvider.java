/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 202583, 207344, 207101
 *******************************************************************************/
package no.javatime.inplace.log.view;

import java.util.ArrayList;

import no.javatime.inplace.log.dl.LogSession;
import no.javatime.inplace.log.impl.BundleLogEntryImpl;
import no.javatime.inplace.log.msg.Messages;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.ibm.icu.text.DateFormat;

public class LogViewLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider {

	private static int MAX_LABEL_LENGTH = 200;

	private Image infoImage;
	private Image okImage;
	private Image errorImage;
	private Image warningImage;
	private Image errorWithStackImage;
	private Image hierarchicalImage;
	ArrayList<Object> consumers = new ArrayList<>();
	private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	private LogView logView;

	public LogViewLabelProvider(LogView logView) {
		errorImage = SharedImages.getImage(SharedImages.DESC_ERROR_ST_OBJ);
		warningImage = SharedImages.getImage(SharedImages.DESC_WARNING_ST_OBJ);
		infoImage = SharedImages.getImage(SharedImages.DESC_INFO_ST_OBJ);
		okImage = SharedImages.getImage(SharedImages.DESC_OK_ST_OBJ);
		errorWithStackImage = SharedImages.getImage(SharedImages.DESC_ERROR_STACK_OBJ);
		hierarchicalImage = SharedImages.getImage(SharedImages.DESC_HIERARCHICAL_LAYOUT_OBJ);

		this.logView = logView;
	}

	public void dispose() {
		if (consumers.size() == 0) {
			super.dispose();
		}
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (element instanceof Group) {
			return (columnIndex == 0) ? hierarchicalImage : null;
		}

		BundleLogEntryImpl entry = (BundleLogEntryImpl) element;
		if (columnIndex == 0) {
			switch (entry.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.OK :
					return okImage;
				case IStatus.WARNING :
					return warningImage;
				default :
					return (entry.getStack() == null ? errorImage : errorWithStackImage);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if ((element instanceof LogSession) && (columnIndex == 2)) {
			LogSession session = (LogSession) element;
			if (session.getDate() == null)
				return ""; //$NON-NLS-1$

			return dateFormat.format(session.getDate());
		}

		if ((element instanceof Group) && (columnIndex == 0)) {
			return element.toString();
		}

		if (element instanceof BundleLogEntryImpl) {
			BundleLogEntryImpl entry = (BundleLogEntryImpl) element;
			switch (columnIndex) {
				case 0 :
					if (entry.getMessage() != null) {
						String message = entry.getMessage();
						if (message.length() > MAX_LABEL_LENGTH) {
							String warning = Messages.LogViewLabelProvider_truncatedMessage;
							StringBuffer sb = new StringBuffer(message.substring(0, MAX_LABEL_LENGTH - warning.length()));
							sb.append(warning);
							return sb.toString();
						}
						return entry.getMessage();
					}
				case 1 :
					if (null != entry.getPluginId() /* && !entry.hasChildren() */) {
						return entry.getPluginId();
					}
				case 2 :
					if (null != entry.getBundleState() /* && !entry.hasChildren() */) {
						return entry.getBundleState();						
					}
				case 3 :
					return dateFormat.format(entry.getDate());
			}
		}
		return ""; //$NON-NLS-1$
	}


	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}

	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.size() == 0) {
			dispose();
		}
	}

	public Font getFont(Object element, int columnIndex) {
		if ((element instanceof LogSession) && (logView.isCurrentLogSession((LogSession) element))) {
			return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}

		return null;
	}
}
