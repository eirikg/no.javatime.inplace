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

import no.javatime.inplace.bundlemanager.BundleJobManager;
import no.javatime.inplace.region.closure.BuildErrorClosure;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.manager.BundleCommand;
import no.javatime.inplace.region.manager.BundleTransition;
import no.javatime.inplace.region.manager.ProjectLocationException;
import no.javatime.inplace.region.manager.BundleTransition.Transition;
import no.javatime.inplace.region.manager.BundleTransition.TransitionError;
import no.javatime.inplace.region.project.BundleProjectState;
import no.javatime.inplace.ui.Activator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.framework.Bundle;

public class BundleListLabelProvider extends LabelProvider implements ITableLabelProvider {

	final private static ImageDescriptor activatedImageDesc = Activator
			.getImageDescriptor("icons/gear_activated.png"); //$NON-NLS-1$
	final private static ImageDescriptor errorImageDesc = Activator.getImageDescriptor("icons/gear_error.png"); //$NON-NLS-1$
	final private static ImageDescriptor pendingImageDesc = Activator
			.getImageDescriptor("icons/gear_pending.png"); //$NON-NLS-1$
	final private static ImageDescriptor deactivatedImageDesc = Activator
			.getImageDescriptor("icons/gear_deactivated.png"); //$NON-NLS-1$
	final private static ImageDescriptor warningImageDesc = Activator
			.getImageDescriptor("icons/gear_warning.png"); //$NON-NLS-1$

	private Image activatedImage = activatedImageDesc.createImage(true);
	private Image errorImage = errorImageDesc.createImage(true);
	private Image warningImage = warningImageDesc.createImage(true);
	private Image pendingImage = pendingImageDesc.createImage(true);
	private Image deactivatedImage = deactivatedImageDesc.createImage(true);

	TableViewer viewer;

	public BundleListLabelProvider() {
	}

	/**
	 * @see #createColumns(TableViewer)
	 */
	@Override
	public String getColumnText(Object obj, int index) {
		return null;
	}

	/**
	 * @see #createColumns(TableViewer)
	 */
	@Override
	public Image getColumnImage(Object obj, int index) {
		return null;
	}

	private int BUNDLE_ORDER = 1;
	private int MODE_ORDER = 1;
	private int STATE_ORDER = 1;
	private int STATUS_ORDER = 1;
	private int TRANSITION_ORDER = 1;

	public final static byte BUNDLE = 0x0;
	public final static byte MODE = 0x1;
	public final static byte STATE = 0x2;
	public final static byte STATUS = 0x3;
	public final static byte TRANSITION = 0x4;

	public static int ASCENDING = 1;
	public static int DESCENDING = -1;

	public void createColumns(final TableViewer viewer) {
		this.viewer = viewer;
		final Table table = viewer.getTable();
		table.removeAll();
		createBundleColumn();
		// Uncomment to show in list page
		// createModeColumn();
		createStatusColumn();
		createStateColumn();
		// Uncomment to show in list page
		createTransitionColumn();
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private void createBundleColumn() {

		TableViewerColumn viewerColumn = createTableViewerColumn("Bundle", 100, 0); //$NON-NLS-1$
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getSymbolicName();
				}
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof BundleProperties)) {
					return null;
				}
				Bundle bundle = ((BundleProperties) element).getBundle();
				IProject project = ((BundleProperties) element).getProject();
				boolean isProjectActivated = BundleProjectState.isProjectActivated(project);
				BundleCommand bundleCommand = BundleJobManager.getCommand();
				BundleTransition bundleTransition = BundleJobManager.getTransition();
				try {
					if (!BuildErrorClosure.hasBuildState(project)) {
						return warningImage;
					} else if (BuildErrorClosure.hasBuildErrors(project)) {
						return warningImage;
					} else if (bundleTransition.containsPending(project, Transition.BUILD, false)) {
						return pendingImage;
					} else if (isProjectActivated
							&& bundleTransition.containsPending(project, Transition.UPDATE, false)) {
						return pendingImage;
					} else if (bundleTransition.hasTransitionError(project)) {
						TransitionError error = bundleTransition.getError(project);
						if (error == TransitionError.DEPENDENCY) {
							return warningImage;
						} else if (error == TransitionError.INCOMPLETE) {
							return errorImage;							
						} else if (error == TransitionError.BUILD) {
							return warningImage; 
						} else {
							return errorImage;
						}
					} else if (bundleTransition.getTransition(project) == Transition.EXTERNAL) {
						return pendingImage;
					} else if (null != bundle && bundleCommand.getBundleRevisions(bundle).size() > 1) {
						return pendingImage;
					} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.RESOLVED)) != 0
							&& !BundleSorter.isFragment(bundle)) {
						return pendingImage;
					} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.STARTING)) != 0) {
						return pendingImage;
					} else {
						if (isProjectActivated) {
							if ((bundleCommand.getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
								return pendingImage;
							}	else {
								return activatedImage;
							}
						} else {
							return deactivatedImage;
						}
					}
				} catch (ProjectLocationException e) {
					return errorImage;
				}
			}
		});
		final TableColumn tableColumn = viewerColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BUNDLE_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(BUNDLE);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, BUNDLE_ORDER);
			}
		});
	}

	/**
	 * Try without the mode column. To many columns in list view
	 */
	private void createModeColumn() {

		TableViewerColumn viewerColumn = createTableViewerColumn("Mode", 0, 1); //$NON-NLS-1$
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getActivationMode();
				}
				return null;
			}
		});
		final TableColumn tableColumn = viewerColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MODE_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(MODE);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, MODE_ORDER);
			}
		});
	}

	/**
	 * Crate a bundle status table viewer column, enable sorting and return image and column values through a
	 * column label provider
	 * 
	 * @see BundleProperties#getBundleStatus()
	 */
	private void createStatusColumn() {

		TableViewerColumn viewerColumn = createTableViewerColumn("Status", 100, 2); //$NON-NLS-1$
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getBundleStatus();
				}
				return null;
			}
		});
		final TableColumn tableColumn = viewerColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				STATUS_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(STATUS);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, STATUS_ORDER);
			}
		});
	}


	private void createStateColumn() {

		TableViewerColumn viewerColumn = createTableViewerColumn("State", 100, 3); //$NON-NLS-1$
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getBundleState();
				}
				return null;
			}
		});
		final TableColumn tableColumn = viewerColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				STATE_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(STATE);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, STATE_ORDER);
			}
		});
	}

	private void createTransitionColumn() {

		TableViewerColumn viewerColumn = createTableViewerColumn("Transition", 100, 4); //$NON-NLS-1$
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getLastTransition();
				}
				return null;
			}
		});
		final TableColumn tableColumn = viewerColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TRANSITION_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(TRANSITION);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, TRANSITION_ORDER);
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.LEFT);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	private ViewerComparator getViewerComparator(byte sortType) {

		if (sortType == MODE) {
			return new ViewerComparator() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getActivationMode(),
								((BundleProperties) e2).getActivationMode())
								* MODE_ORDER;
					}
					return 0;
				}
			};
		} else if (sortType == STATE) {
			return new ViewerComparator() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getBundleState(),
								((BundleProperties) e2).getBundleState())
								* STATE_ORDER;
					}
					return 0;
				}
			};
		} else if (sortType == TRANSITION) {
			return new ViewerComparator() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getLastTransition(),
								((BundleProperties) e2).getLastTransition())
								* TRANSITION_ORDER;
					}
					return 0;
				}
			};
		} else if (sortType == STATUS) {
			return new ViewerComparator() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getBundleStatus(),
								((BundleProperties) e2).getBundleStatus())
								* STATUS_ORDER;
					}
					return 0;
				}
			};
		} else { // sort type is BUNDLE
			return new ViewerComparator() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getSymbolicName(),
								((BundleProperties) e2).getSymbolicName())
								* BUNDLE_ORDER;
					}
					return 0;
				}
			};
		}
	}

	private void setColumnSorting(TableColumn column, int order) {
		Table table = viewer.getTable();
		table.setSortColumn(column);
		table.setSortDirection(order == ASCENDING ? SWT.UP : SWT.DOWN);
	}

	@Override
	public void dispose() {
		activatedImage.dispose();
		errorImage.dispose();
		pendingImage.dispose();
		deactivatedImage.dispose();
		warningImage.dispose();
		super.dispose();
	}
}
