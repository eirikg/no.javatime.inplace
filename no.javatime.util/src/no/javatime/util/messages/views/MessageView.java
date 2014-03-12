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
package no.javatime.util.messages.views;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import no.javatime.util.Activator;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.ExceptionMessage;
import no.javatime.util.messages.MessageContainer;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A message view that displays data obtained from the {@link MessageContainer} model. The view is connected to
 * the model using a minimal content provider and the view part of the model is a simple list of queued messages.
 * <p>
 * The primary purpose of this view is tracing of bundle commands and events and filtering out and displaying
 * log messages from the bind manager due to bug 295662. The view should probably be disabled when the
 * InPlace Activator get mature.
 * <p>
 * Not using properties. UI messages are hard coded.
 */
public class MessageView extends ViewPart {

	public static String ID = "no.javatime.util.messages.views.MessageView"; //$NON-NLS-1$
	final public static ImageDescriptor messageViewImage = Activator.getImageDescriptor("icons/message_view.gif"); //$NON-NLS-1$

	private TableViewer viewer;
	private Action clearAction;
	private Action copyAction;
	private Action doubleClickAction;
	private Action selectAllAction;
	private Action traceOperationsAction;
	private Action traceEventsAction;
	private Action infoMessageAction;
	private ViewContentProvider contentProvider;
	private IMemento memento;
	
	/*
	 * Provides a list of messages to the view.
	 */
	class ViewContentProvider implements IStructuredContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}
		
		public int size() {	
			return MessageContainer.getInstance().size();
		}
		
		public Object[] getElements(Object parent) {

			return MessageContainer.getInstance().getMessages();
		}
	}

	/**
	 * Displays messages as a simple list
	 * 
	 */
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider, IFontDecorator {

		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}

		public Image getImage(Object obj) {
			return null;
		}

		@Override
		public Font decorateFont(Object element) {
			return JFaceResources.getFontRegistry().getBold(JFaceResources.TEXT_FONT);
		}
	}

	
	public MessageView() {
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}
	
	/**
	 * Save state of trace tool bar buttons and  checked menu for receiving binding
	 * messages. 
	 * Overrides settings at bundle level
	 * @param memento interface to persist state
	 */
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (null != memento) {
			IMemento mem = memento.createChild("ToolbarButtons"); //$NON-NLS-1$
			mem.putBoolean(traceEventsAction.getId(), traceEventsAction.isChecked());
			mem = memento.createChild("ToolbarButtons"); //$NON-NLS-1$
			mem.putBoolean(traceOperationsAction.getId(), traceOperationsAction.isChecked());
			mem = memento.createChild("ToolbarButtons"); //$NON-NLS-1$
			mem.putBoolean(infoMessageAction.getId(), infoMessageAction.isChecked());
		}
	}

	/**
	 * Set state of trace tool bar buttons and menu for receiving binding messages from previous session
	 * Overrides settings at bundle level if memento exist
	 * @param memento interface to access persisted state
	 */
	public void restoreState(IMemento memento) {
		if (null != memento) {
			IMemento[] 	mem = memento.getChildren("ToolbarButtons");  //$NON-NLS-1$
			if (null != mem && mem.length == 3) {		
				Boolean traceEventsValue = mem[0].getBoolean(traceEventsAction.getId());
				Boolean traceOperationsValue = mem[1].getBoolean(traceOperationsAction.getId());
				Boolean traceBindingValue = mem[2].getBoolean(infoMessageAction.getId());
				if (null != traceOperationsValue && null != traceEventsValue && null != traceBindingValue) {
					setTraceState(traceEventsValue, traceOperationsValue, traceBindingValue);
				}
			}
		} else {
			// Use settings at bundle level
			setTraceState(Category.getState(Category.bundleEvents), 
					Category.getState(Category.bundleOperations), 
					Category.getState(Category.infoMessages));
		}
	}
	
	/**
	 * Create and initialize the viewer with selection provider and listener, actions,
	 * tool bar, pop-up and pull-down menus.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		contentProvider = new ViewContentProvider(); 
		viewer.setContentProvider(contentProvider);
		ViewLabelProvider labelProvider = new ViewLabelProvider();
		viewer.setLabelProvider(labelProvider);
		viewer.getTable().setLinesVisible(true);
		viewer.setInput(MessageContainer.getInstance());
		getSite().setSelectionProvider(viewer);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
			}
		});
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "no.javatime.util.messages.viewer"); //$NON-NLS-1$
		makeActions();
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		restoreState(memento);

		viewer.getTable().addFocusListener(new FocusListener() {
			
			@Override
			public void focusGained(FocusEvent e) {
				
				toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
				setTraceState(traceEventsAction.isChecked(), traceOperationsAction.isChecked(), 
						infoMessageAction.isChecked());
				try {
					MessageContainer mc = MessageContainer.getInstance();
					setInput(mc);
				} catch (ConcurrentModificationException ex) {
					StatusManager.getManager().handle(new Status(Status.ERROR, Activator.PLUGIN_ID, ex.getLocalizedMessage(), ex),
							StatusManager.LOG);
					ExceptionMessage.getInstance().handleMessage(ex, "ConcurrentModificationException");						 //$NON-NLS-1$
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
			}
		});
		toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
	}
	
	/**
	 * Set trace buttons and category trace variables based on the value of trace actions.
	 * @param eventState event state to apply to trace event button and category
	 * @param operationState operation state to apply to trace operation button and category
	 * @param infoMessageState to receive binding messages
	 */
	private void setTraceState(Boolean eventState, Boolean operationState, Boolean infoMessageState) {
		traceEventsAction.setChecked(eventState);
		Category.setState(Category.bundleEvents, eventState);
		if (eventState) {
			traceEventsAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/enable_trace_events.png")); //$NON-NLS-1$
		} else {
			traceEventsAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/disable_trace_events.png")); //$NON-NLS-1$
		}
		traceOperationsAction.setChecked(operationState);			
		Category.setState(Category.bundleOperations, operationState);
		if (operationState) {
			traceOperationsAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/enable_trace_op.png")); //$NON-NLS-1$
		} else {
			traceOperationsAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/disable_trace_op.png")); //$NON-NLS-1$
		}		
		infoMessageAction.setChecked(infoMessageState);			
		Category.setState(Category.infoMessages, infoMessageState);
		if (infoMessageState) {
			infoMessageAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/process_info.png")); //$NON-NLS-1$
		} else {
			infoMessageAction.setImageDescriptor(Activator
					.getImageDescriptor("icons/no_process_info.png")); //$NON-NLS-1$
		}		
	}
	
	/**
	 * Invoke filling tool bar and pull-down menu
	 */
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	/**
	 * Fill local pull-down menu
	 * @param manager the menu manager for the pull-down menu
	 */
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(traceEventsAction);
		manager.add(traceOperationsAction);
		manager.add(infoMessageAction);
		manager.add(new Separator());
		manager.add(clearAction);
		manager.add(selectAllAction);
		manager.add(copyAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * Fill local tool bar menu
	 * @param manager the menu manager for the tool bar
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(clearAction);
		manager.add(selectAllAction);
		manager.add(new Separator());
		manager.add(traceEventsAction);
		manager.add(traceOperationsAction);
		manager.add(infoMessageAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Fill local pop-up menu
	 * @param manager the menu manager for the pop-up menu
	 */
	private void fillContextMenu(IMenuManager manager) {
		manager.add(traceEventsAction);
		manager.add(traceOperationsAction);
		manager.add(infoMessageAction);
		manager.add(new Separator());
		manager.add(clearAction);
		manager.add(selectAllAction);
		manager.add(copyAction);
		// Other plug-ins can contribute their actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * Hook for the context pop-up menu	
	 */
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				MessageView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Create all actions and assign text, tool tip and images
	 */
	private void makeActions() {

		traceEventsAction = new Action("Trace Bundle Events", Action.AS_CHECK_BOX) { //$NON-NLS-1$

			public void run() {
				Boolean traceEvents = traceEventsAction.isChecked();
				Category.setState(Category.bundleEvents, traceEvents);
				if (traceEvents) {
					traceEventsAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/enable_trace_events.png")); //$NON-NLS-1$
				} else {
					traceEventsAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/disable_trace_events.png")); //$NON-NLS-1$
				}
			}
		};
		traceEventsAction.setId("traceEventsAction"); //$NON-NLS-1$
		traceEventsAction.setToolTipText("Trace Bundle and Framework Events");		 //$NON-NLS-1$
		
		traceOperationsAction = new Action("Trace Bundle Operations", Action.AS_CHECK_BOX) { //$NON-NLS-1$

			public void run() {
				Boolean traceOp = traceOperationsAction.isChecked();
				Category.setState(Category.bundleOperations, traceOp);
				if (traceOp) {
					traceOperationsAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/enable_trace_op.png")); //$NON-NLS-1$
				} else {
					traceOperationsAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/disable_trace_op.png")); //$NON-NLS-1$
				}
			}
		};
		traceOperationsAction.setId("traceOperationsAction"); //$NON-NLS-1$
		traceOperationsAction.setToolTipText("Trace Bundle Life Cycle Operations"); //$NON-NLS-1$

		infoMessageAction = new Action("Information Messages", Action.AS_CHECK_BOX) { //$NON-NLS-1$

			public void run() {
				Boolean infoEvents = infoMessageAction.isChecked();
				Category.setState(Category.infoMessages, infoEvents);
				if (infoEvents) {
					infoMessageAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/process_info.png")); //$NON-NLS-1$
				} else {
					infoMessageAction.setImageDescriptor(Activator
							.getImageDescriptor("icons/no_process_info.png")); //$NON-NLS-1$
				}
			}
		};
		infoMessageAction.setId("infoMessageAction"); //$NON-NLS-1$
		infoMessageAction.setToolTipText("Information Messages");		 //$NON-NLS-1$

		clearAction = new Action("C&lear") { //$NON-NLS-1$
			public void run() {
				MessageContainer.getInstance().clearMessages();
				viewer.refresh();
				toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
			}
		};
		clearAction.setToolTipText("Clear the message view"); //$NON-NLS-1$
		clearAction.setImageDescriptor(Activator.getImageDescriptor("icons/clear.gif")); //$NON-NLS-1$
		clearAction.setEnabled(true);

		copyAction = new Action("&Copy \tCtrl+C") { //$NON-NLS-1$
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (null != selection) {
					Collection<IStructuredSelection> values = selection.toList();
					copyValues(values);
				}
			}
		};
		copyAction.setToolTipText("Copy selected message(s)"); //$NON-NLS-1$
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));

		selectAllAction = new Action("Select &All \tCtrl+A") { //$NON-NLS-1$
			public void run() {
				viewer.getTable().selectAll();
				toggleClipboardActions(true);
			}
		};
		selectAllAction.setToolTipText("Select all message(s)"); //$NON-NLS-1$
		selectAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/select_all.png")); //$NON-NLS-1$

		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				showMessage(obj.toString());
			}
		};
	}

	/**
	 * Toggle clip board actions. If empty disable clear, selection of rows and the copy UI element
	 * @param notEmpty if empty disable clip board UI elements, otherwise enable clip board UI elements
	 */
	private void toggleClipboardActions(Boolean notEmpty) {
		if (copyAction.isEnabled() != notEmpty) {
			copyAction.setEnabled(notEmpty);
		}
		if (selectAllAction.isEnabled() != notEmpty) {
			selectAllAction.setEnabled(notEmpty);
		}
		if (clearAction.isEnabled() != notEmpty) {
			clearAction.setEnabled(notEmpty);
		}
	}

	private void copyValues(Collection<IStructuredSelection> values) {
		StringWriter buf = new StringWriter();
		PrintWriter writer = new PrintWriter(buf);
		for (Iterator<IStructuredSelection> i = values.iterator(); i.hasNext();) {
			writer.print(i.next());
			if (i.hasNext())
				writer.println();
		}
		writer.close();
		Clipboard clipboard = new Clipboard(Activator.getDisplay());
		Object[] o = new Object[] { buf.toString() };
		Transfer[] t = new Transfer[] { TextTransfer.getInstance() };
		clipboard.setContents(o, t);
		clipboard.dispose();
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Message View", message); //$NON-NLS-1$
	}

	/**
	 * Sets the input to the {@link MessageContainer}
	 * @param instance the container providing content
	 */
	public void setInput(MessageContainer instance) {
		viewer.setInput(instance);
		// Always enabled when there are messages to clear
		// Content provider initialized in create part
		if (null != contentProvider) {
			toggleClipboardActions((contentProvider.size()) > 0 ? true : false);
		}
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}