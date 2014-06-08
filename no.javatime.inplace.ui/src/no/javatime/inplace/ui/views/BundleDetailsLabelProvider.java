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
package no.javatime.inplace.ui.views;

import no.javatime.inplace.region.manager.InPlaceException;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

class BundleDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {
	TableViewer viewer = null;

	public String getColumnText(Object obj, int index) {

		if (null == viewer || null == obj || !(obj instanceof BundleProperties))
			return "";
		
		BundleProperties bundleProp = (BundleProperties) obj;
		String value = null;
		// Get the column for this index
		TableColumn column = viewer.getTable().getColumn(index);
		// Repeat this
		column.setAlignment(SWT.LEFT);
		try {
			if (index == 0) {
				value = bundleProp.getName();
			}
			if (index == 1) {
				value = bundleProp.getValue();
			}
		} catch (InPlaceException e) {
			// Workaround forcing auto fit of column width, when adding the last row
			for (int i = 0, n = viewer.getTable().getColumnCount(); i < n; i++) {
				viewer.getTable().getColumn(i).pack();
	    }
			return "?";
		}
		// Workaround forcing auto fit of column width, when adding the last row
		for (int i = 0, n = viewer.getTable().getColumnCount(); i < n; i++) {
			viewer.getTable().getColumn(i).pack();
    }
		return value;
	}

	public Image getColumnImage(Object obj, int index) {
		return null;
	}

	public Image getImage(Object obj) {
		return null;
	}

	public void createColumns(TableViewer viewer) {			
		this.viewer = viewer;
		Table table = viewer.getTable();
		table.removeAll(); // rows		
		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("Property");
		column.setAlignment(SWT.LEFT);
		column.setWidth(1);
		column.setResizable(true);
		
		TableColumn value = new TableColumn(table, SWT.LEFT, 1);
		value.setText("Value");
		value.setAlignment(SWT.LEFT);
		value.setWidth(1);
		value.setResizable(true);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
}
