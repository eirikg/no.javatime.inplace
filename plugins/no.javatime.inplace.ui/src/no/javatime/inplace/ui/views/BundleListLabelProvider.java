/*******************************************************************************
 * Copyright (c) 2011, 2017 JavaTime project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	JavaTime project, Eirik Gronsund - initial implementation
 *******************************************************************************/
package no.javatime.inplace.ui.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.framework.Bundle;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.region.closure.BundleSorter;
import no.javatime.inplace.region.intface.BundleCommand;
import no.javatime.inplace.region.intface.BundleTransition;
import no.javatime.inplace.region.intface.BundleTransition.Transition;
import no.javatime.inplace.region.intface.BundleTransition.TransitionError;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.intface.ProjectLocationException;
import no.javatime.inplace.ui.Activator;

/**
 * Maintains bundle symbolic name, bundle status, bundle state and bundle transition columns.
 * <p>
 * Except for some logic to display images based on the status, state, transition and transition
 * error of bundles the values of the rest of the columns are just accessed and returned for
 * display. The state column displays the current bundle state and the transition displays the last
 * transition executed on a bundle.
 * <p>
 * Supports column based sorting, reordering of columns and preservation of column widths between
 * sessions and closing and opening of the bundle view hosting this bundle label provider
 * 
 */
public class BundleListLabelProvider extends LabelProvider implements ITableLabelProvider {

	private final static ImageDescriptor activatedImageDesc = Activator
			.getImageDescriptor("icons/gear_activated.png"); //$NON-NLS-1$
	private final static ImageDescriptor errorImageDesc = Activator
			.getImageDescriptor("icons/gear_error.png"); //$NON-NLS-1$
	private final static ImageDescriptor pendingImageDesc = Activator
			.getImageDescriptor("icons/gear_pending.png"); //$NON-NLS-1$
	private final static ImageDescriptor deactivatedImageDesc = Activator
			.getImageDescriptor("icons/gear_deactivated.png"); //$NON-NLS-1$
	private final static ImageDescriptor warningImageDesc = Activator
			.getImageDescriptor("icons/gear_warning.png"); //$NON-NLS-1$

	private final Image activatedImage = activatedImageDesc.createImage(true);
	private final Image errorImage = errorImageDesc.createImage(true);
	private final Image warningImage = warningImageDesc.createImage(true);
	private final Image pendingImage = pendingImageDesc.createImage(true);
	private final Image deactivatedImage = deactivatedImageDesc.createImage(true);

	/* Section name and keys for persisted column widths */
	private final static String bundleViewColumnWidthSection = "BundleViewColumnWidhtSection";
	private final static String bundlNameColumnWidth = "BundlNameColumnWidht";
	private final static String statusColumnWidth = "StatusColumnWidht";
	private final static String stateColumnWidth = "StateColumnWidht";
	private final static String transitionColumnWidth = "TransitionColumnWidht";

	/* Columns and column widths. New widths are preserved after resizing */
	private int bundleNameColWidh = 200;
	private TableViewerColumn bundleNameColumn;

	private int statusColWidth = 110;
	private TableViewerColumn statusColumn;

	private int stateColWidth = 89;
	private TableViewerColumn stateColumn;

	private int transitionColWidth = 89;
	private TableViewerColumn transitionColumn;

	/* Section names and keys for persisted sort column */
	private final static String bundleViewSortSection = "BundleViewSortSection";
	private final static String sortColumn = "SortColumn";
	private final static String sortDirection = "SortDirection";

	/* Column sort direction */
	private int BUNDLE_ORDER = 1;
	private int STATE_ORDER = 1;
	private int STATUS_ORDER = 1;
	private int TRANSITION_ORDER = 1;

	/* Column identifiers of columns to sort */
	public final static int BUNDLE = 0;
	public final static int STATUS = 1;
	public final static int STATE = 2;
	public final static int TRANSITION = 3;

	public final static int ASCENDING = 1;
	public final static int DESCENDING = -1;

	/**
	 * Persisted sort column
	 */
	private int sortColumnSetting;

	/**
	 * Persisted sort column direction
	 */
	private int sortOrderSetting;

	/**
	 * Reference to the view hosting this label provider
	 */
	private TableViewer viewer;

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

	/**
	 * Should be called after construction and before supplying column data to this bundle provider
	 * 
	 * @param viewer The bundle viewer hosting this bundle label provider
	 */
	public void createColumns(final TableViewer viewer) {
		this.viewer = viewer;
		final Table table = viewer.getTable();
		table.removeAll();
		restoreColumnsWidths();
		createBundleNameColumn();
		createStatusColumn();
		createStateColumn();
		createTransitionColumn();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private int getBundleNameColWidth() {
		return bundleNameColWidh;
	}

	private void setBundleNameColWidth(int bUndleNameColWidh) {
		this.bundleNameColWidh = bUndleNameColWidh;
	}

	/**
	 * Crate a bundle symbolic name column, enable sorting, record width and sort order and return
	 * image and column values through a column label provider
	 * <p>
	 * Displays image according to the combination of the last executed bundle transition, bundle
	 * state, transition error and bundle status
	 * 
	 * @see BundleProperties#getSymbolicName()
	 */

	private void createBundleNameColumn() {

		bundleNameColumn = createTableViewerColumn("Bundle", bundleNameColWidh, 0); //$NON-NLS-1$
		// Persisted sort identifier
		bundleNameColumn.getColumn().setData(BUNDLE);
		bundleNameColumn.setLabelProvider(new ColumnLabelProvider() {

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
				BundleProperties bundleProperties = (BundleProperties) element;
				Bundle bundle = bundleProperties.getBundle();
				IProject project = bundleProperties.getProject();
				try {
					boolean isProjectActivated = Activator.getBundleRegionService()
							.isBundleActivated(project);
					BundleCommand bundleCommand = Activator.getBundleCommandService();
					BundleTransition bundleTransition = Activator.getBundleTransitionService();
					TransitionError error = bundleTransition.getTransitionError(project);
					if (error != TransitionError.NOERROR) {
						switch (error) {
						case BUILD_CYCLE:
						case BUILD_STATE:
						case BUILD_DESCRIPTION_FILE:
						case BUILD_MANIFEST:
						case BUILD_MODULAR_EXTERNAL_DUPLICATE:
						case BUILD_MODULAR_WORKSPACE_DUPLICATE:
						case MODULAR_REFRESH_ERROR:
						case MODULAR_EXCEPTION:
							return errorImage;
						case MODULAR_EXTERNAL_UNINSTALL:
							return warningImage;
						case SERVICE_EXCEPTION:
						case SERVICE_INCOMPLETE_TRANSITION:
						case SERVICE_STATECHANGE:
							return errorImage;
						case BUILD:
							return Activator.getCommandOptionsService().isActivateOnCompileError() ? warningImage
									: errorImage;
						default:
							return errorImage;
						}
					} else if (bundleTransition.containsPending(project, Transition.BUILD, false)) {
						return pendingImage;
					} else if (isProjectActivated
							&& bundleTransition.containsPending(project, Transition.UPDATE, false)) {
						return pendingImage;
					} else if (bundleTransition.getTransition(project) == Transition.EXTERNAL) {
						return pendingImage;
					} else if (null != bundle && bundleCommand.getBundleRevisions(bundle).size() > 1) {
						return pendingImage;
					} else if (isProjectActivated && (bundleCommand.getState(bundle) & (Bundle.RESOLVED)) != 0
							&& !BundleSorter.isFragment(bundle)) {
						return pendingImage;
					} else if (isProjectActivated
							&& (bundleCommand.getState(bundle) & (Bundle.STARTING)) != 0) {
						return pendingImage;
					} else {
						if (isProjectActivated) {
							if ((bundleCommand.getState(bundle) & (Bundle.UNINSTALLED | Bundle.INSTALLED
									| Bundle.RESOLVED | Bundle.STOPPING)) != 0) {
								return pendingImage;
							} else {
								return activatedImage;
							}
						} else {
							return deactivatedImage;
						}
					}
				} catch (InPlaceException e) {
					return errorImage;
				} catch (ProjectLocationException | ExtenderException e) {
					return errorImage;
				}
			}
		});
		final TableColumn tableColumn = bundleNameColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				BUNDLE_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(BUNDLE);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, BUNDLE_ORDER);
			}
		});
		tableColumn.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				setBundleNameColWidth(tableColumn.getWidth());
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});
	}

	private int getStatusColWidth() {
		return statusColWidth;
	}

	private void setStatusColWidth(int statusColWidh) {
		this.statusColWidth = statusColWidh;
	}

	/**
	 * Crate a bundle status table viewer column, enable sorting, record width and sort order and
	 * return image and column values through a column label provider
	 * 
	 * @see BundleProperties#getBundleStatus()
	 */
	private void createStatusColumn() {

		statusColumn = createTableViewerColumn("Status", statusColWidth, 2); //$NON-NLS-1$
		// Persisted sort identifier
		statusColumn.getColumn().setData(STATUS);
		statusColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getBundleStatus();
				}
				return null;
			}
		});
		final TableColumn tableColumn = statusColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				STATUS_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(STATUS);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, STATUS_ORDER);
			}
		});
		tableColumn.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				setStatusColWidth(tableColumn.getWidth());
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});
	}

	private int getStateColWidth() {
		return stateColWidth;
	}

	private void setStateColWidth(int stateColWidht) {
		this.stateColWidth = stateColWidht;
	}

	/**
	 * Crate a bundle state table viewer column, enable sorting, record width and sort order and
	 * return image and column values through a column label provider
	 * 
	 * @see BundleProperties#getBundleState()
	 */
	private void createStateColumn() {

		stateColumn = createTableViewerColumn("State", stateColWidth, 3); //$NON-NLS-1$
		// Persisted sort identifier
		stateColumn.getColumn().setData(STATE);
		stateColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getBundleState();
				}
				return null;
			}
		});
		final TableColumn tableColumn = stateColumn.getColumn();
		tableColumn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				STATE_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(STATE);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, STATE_ORDER);
			}
		});
		tableColumn.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				setStateColWidth(tableColumn.getWidth());
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});
	}

	private int getTransitionColWidth() {
		return transitionColWidth;
	}

	private void setTransitionColWidth(int transitionColWidh) {
		this.transitionColWidth = transitionColWidh;
	}

	/**
	 * Crate a bundle transition table viewer column, enable sorting, record width and sort order and
	 * return image and column values through a column label provider
	 * 
	 * @see BundleProperties#getLastTransition()
	 */

	private void createTransitionColumn() {

		transitionColumn = createTableViewerColumn("Transition", transitionColWidth, 4); //$NON-NLS-1$
		// Persisted sort identifier
		transitionColumn.getColumn().setData(TRANSITION);
		transitionColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof BundleProperties) {
					return ((BundleProperties) element).getLastTransition();
				}
				return null;
			}
		});
		final TableColumn tableColumn = transitionColumn.getColumn();

		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TRANSITION_ORDER *= -1;
				ViewerComparator comparator = getViewerComparator(TRANSITION);
				viewer.setComparator(comparator);
				setColumnSorting(tableColumn, TRANSITION_ORDER);
			}
		});
		tableColumn.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				setTransitionColWidth(tableColumn.getWidth());
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});
	}

	/**
	 * Helper for creating table viewer columns with the specified attributes
	 * 
	 * @param title Descriptive name of the column
	 * @param width Column width
	 * @param colNumber not in use
	 * @return A column object with the specified attributes
	 */
	private TableViewerColumn createTableViewerColumn(String title, int width, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.LEFT);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(width);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	/**
	 * Return a comparator for a specific column based on a predefined sort type
	 * 
	 * @param sortType One type for each of the columns that are supported
	 * <p>
	 * For an unknown sort type the comparator for the bundle symbolic name is returned
	 * 
	 * @return A comparator for a specific column
	 */
	private ViewerComparator getViewerComparator(int sortType) {

		if (sortType == STATE) {
			return new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getBundleState(),
								((BundleProperties) e2).getBundleState()) * STATE_ORDER;
					}
					return 0;
				}
			};
		} else if (sortType == TRANSITION) {
			return new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getLastTransition(),
								((BundleProperties) e2).getLastTransition()) * TRANSITION_ORDER;
					}
					return 0;
				}
			};
		} else if (sortType == STATUS) {
			return new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getBundleStatus(),
								((BundleProperties) e2).getBundleStatus()) * STATUS_ORDER;
					}
					return 0;
				}
			};
		} else { // sort type is BUNDLE
			return new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					if ((e1 instanceof BundleProperties) && (e2 instanceof BundleProperties)) {
						return getComparator().compare(((BundleProperties) e1).getSymbolicName(),
								((BundleProperties) e2).getSymbolicName()) * BUNDLE_ORDER;
					}
					return 0;
				}
			};
		}
	}

	/**
	 * Set current sort column and sort direction of the specified sort column
	 * 
	 * @param sortColumn current sort column
	 * @param sortDirection sort direction of current sort column
	 */
	private void setColumnSorting(TableColumn sortColumn, int sortDirection) {
		Table table = viewer.getTable();
		table.setSortColumn(sortColumn);
		table.setSortDirection(sortDirection == ASCENDING ? SWT.UP : SWT.DOWN);
		sortColumnSetting = (int) sortColumn.getData();
		sortOrderSetting = sortDirection;
	}

	@Override
	public void dispose() {
		saveColumnsWidths();
		saveSortColumn();
		activatedImage.dispose();
		errorImage.dispose();
		pendingImage.dispose();
		deactivatedImage.dispose();
		warningImage.dispose();
		super.dispose();
	}

	/**
	 * Get stored away column widths for all columns in this bundle label provider
	 * <p>
	 * Always returns false at first use of the bundle view
	 * 
	 * @return true if column widths was restored, otherwise false
	 */
	private boolean restoreColumnsWidths() {

		IDialogSettings dlgSettings = Activator.getDefault().getDialogSettings();
		if (null != dlgSettings) {
			IDialogSettings widthSect = dlgSettings.getSection(bundleViewColumnWidthSection);
			if (null != widthSect) {
				try {
					setBundleNameColWidth(widthSect.getInt(bundlNameColumnWidth));
					setStatusColWidth(widthSect.getInt(statusColumnWidth));
					setStateColWidth(widthSect.getInt(stateColumnWidth));
					setTransitionColWidth(widthSect.getInt(transitionColumnWidth));
					return true;
				} catch (NumberFormatException e) {
					// First use
				}
			}
		}
		return false;
	}

	/**
	 * Restore any sort column and sort the column
	 * 
	 * @return true if a sort column was restored and sorted, otherwise false
	 */
	public boolean restoreSortColumn() {

		IDialogSettings dlgSettings = Activator.getDefault().getDialogSettings();
		if (null != dlgSettings) {
			IDialogSettings sortSect = dlgSettings.getSection(bundleViewSortSection);
			if (null != sortSect) {
				try {
					sortOrderSetting = sortSect.getInt(sortDirection);
					int sortCol = sortSect.getInt(sortColumn);
					switch (sortCol) {
					case BUNDLE:
						BUNDLE_ORDER = sortOrderSetting * -1;
						bundleNameColumn.getColumn().notifyListeners(SWT.Selection, new Event());
						break;
					case STATUS:
						STATUS_ORDER = sortOrderSetting * -1;
						statusColumn.getColumn().notifyListeners(SWT.Selection, new Event());
						break;
					case STATE:
						STATE_ORDER = sortOrderSetting * -1;
						stateColumn.getColumn().notifyListeners(SWT.Selection, new Event());
						break;
					case TRANSITION:
						TRANSITION_ORDER = sortOrderSetting * -1;
						transitionColumn.getColumn().notifyListeners(SWT.Selection, new Event());
						break;
					default:
						return false;
					}
					return true;
				} catch (NumberFormatException e) {
					// First use
				}
			}
		}
		return false;
	}

	/**
	 * Store any sort column to be restored
	 * 
	 * @return true if a sort column has been stored, otherwise false
	 */
	private boolean saveSortColumn() {

		if (sortOrderSetting != 0) {
			IDialogSettings dlgSettings = Activator.getDefault().getDialogSettings();
			if (null != dlgSettings) {
				IDialogSettings sortSect = dlgSettings.getSection(bundleViewSortSection);
				if (null == sortSect) {
					sortSect = dlgSettings.addNewSection(bundleViewSortSection);
				}
				sortSect.put(sortColumn, sortColumnSetting);
				sortSect.put(sortDirection, sortOrderSetting);
				return true;
			}
		}
		return false;
	}

	/**
	 * Store column widths for all columns in this bundle label provider
	 * <p>
	 * Column widths are stored by the dialog settings provided by this plug-in and persisted at shut
	 * down
	 * 
	 * @return true if column widths was stored, otherwise false
	 */
	private boolean saveColumnsWidths() {

		IDialogSettings dlgSettings = Activator.getDefault().getDialogSettings();
		if (null != dlgSettings) {
			IDialogSettings widthSect = dlgSettings.getSection(bundleViewColumnWidthSection);
			if (null == widthSect) {
				widthSect = dlgSettings.addNewSection(bundleViewColumnWidthSection);
			}
			widthSect.put(bundlNameColumnWidth, getBundleNameColWidth());
			widthSect.put(statusColumnWidth, getStatusColWidth());
			widthSect.put(stateColumnWidth, getStateColWidth());
			widthSect.put(transitionColumnWidth, getTransitionColWidth());
			return true;
		}
		return false;
	}
}
