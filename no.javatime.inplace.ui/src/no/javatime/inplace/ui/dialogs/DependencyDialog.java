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
package no.javatime.inplace.ui.dialogs;

import no.javatime.inplace.ui.Activator;
import no.javatime.util.messages.Category;
import no.javatime.util.messages.UserMessage;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

/**
 * Non-modal dialog for setting dependency rules on bundles to activate, deactivate,
 * start and stop.
 */
public class DependencyDialog extends TitleAreaDialog {

	static int DEFAULT_ID = 101;
	static String DEAFAULT_LABEL = "Default";
	Image captionDialogImage = null;
	private Composite container;
	// Groups and buttons
	Group grpActivate = null;
	Button btnActivateProvidingBundles = null;
	Button btnActivateRequiringBundles = null;
	Button btnActivatePartialGraph = null;
	Group grpDeactivate = null;
	Button btnDeactivateRequiringBundles = null;
	Button btnDeactivateProvidingBundles = null;
	Button btnDeactivatePartialGraph = null;
	Group grpStart = null;
	Button btnStartSingleBundles = null;
	Button btnStartRequiringBundles = null;
	Button btnStartProvidingBundles = null;
	Button btnStartRequiringProvidingBundles = null;
	Button btnStartPartialGraph = null;
	Group grpStop = null;
	Button btnStopSingleBundles = null;
	Button btnStopRequiringBundles = null;
	Button btnStopProvidingBundles = null;
	Button btnStopProvidingRequiringBundles = null;
	Button btnStopPartialGraph = null;
	// Initial states
	// Activate
	private Boolean partialOnActivate = false;
	private Boolean requiringOnActivate = false;
	// Deactivate
	private Boolean partialOnDeactivate = false;
	private Boolean providingOnDeactivate = true;
	// Start
	private Boolean partialOnStart = false;	
	private Boolean requiringOnStart = false;
	private Boolean providingOnStart = true;
	// Stop
	private Boolean partialOnStop = false;
	private Boolean requiringOnStop = false;
	private Boolean providingOnStop = true;
	private UserMessage msg = UserMessage.getInstance();
	// Textual name of operation
	final String activateOp = msg.formatString("dep_operation_activate");
	final String deactivateOp = msg.formatString("dep_operation_deactivate");
	final String startOp = msg.formatString("dep_operation_start");
	final String stopOp = msg.formatString("dep_operation_stop");
	// Size of radio groups
	private int grpHeightHint = 40;
	private int	grWidthHint = 550;

	/**
	 * Create the dialog.
	 * @param parentShell the workspace shell
	 */
	public DependencyDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
		setBlockOnOpen(false);
		setInitialDependencies();
	}

	@Override
	public boolean close() {
		if (null != captionDialogImage) {
			captionDialogImage.dispose();
		}
		return super.close();
	}
 
	/**
	 * Create the dialog with radio groups for activate, deactivate, start and stop.
	 * @param parent the dialog
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		captionDialogImage = Activator.getImageDescriptor("icons/dependencies.gif").createImage();
		parent.getShell().setImage(captionDialogImage);
		parent.getShell().setText(msg.formatString("dep_dialog_caption_text"));
		setMessage(msg.formatString("dep_main_message"));
		setTitle(msg.formatString("dep_title_message"));
		container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		// Activate, deactivate, start and stop groups		
		activate();		
		deactivate();
		start();
		stop();		
		return container;
	}

	/**
	 * Initialize and manage the activate radio group
	 */
	private void activate() {
		grpActivate = new Group(container, SWT.SHADOW_ETCHED_OUT);
		grpActivate.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_activation", activateOp), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_activation", activateOp), IMessageProvider.INFORMATION);
			}
		});
		grpActivate.setToolTipText(msg.formatString("dep_operation_group_activation", activateOp));
		GridData gd_grpActivate = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpActivate.heightHint = grpHeightHint;
		gd_grpActivate.widthHint = grWidthHint;
		grpActivate.setLayoutData(gd_grpActivate);
		grpActivate.setText(msg.formatString("dep_bundles_name", msg.formatString("dep_operation_activate")));
		// Providing
		btnActivateProvidingBundles = new Button(grpActivate, SWT.RADIO);
		btnActivateProvidingBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateProvidingBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_providing", activateOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateProvidingBundles.setToolTipText(msg.formatString("dep_operation_providing", activateOp));
		btnActivateProvidingBundles.setBounds(101, 20, 72, 16);
		btnActivateProvidingBundles.setText("Providing");
		
		if (!requiringOnActivate && !partialOnActivate) {
			btnActivateProvidingBundles.setSelection(true);
		}
		btnActivateRequiringBundles = new Button(grpActivate, SWT.RADIO);
		btnActivateRequiringBundles.setToolTipText(msg.formatString("dep_operation_requiring_providing", activateOp));
		btnActivateRequiringBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.requiringOnActivate, btnActivateRequiringBundles.getSelection());
				if (btnActivateRequiringBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_requiring_providing", activateOp), IMessageProvider.INFORMATION);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				btnActivateRequiringBundles.setSelection(Category.getInstance().resetState(Category.requiringOnActivate));
			}
		});
		btnActivateRequiringBundles.setBounds(290, 20, 149, 16);
		btnActivateRequiringBundles.setText("Requiring and Providing");
		btnActivateRequiringBundles.setSelection(Category.getState(Category.requiringOnActivate));
	
		btnActivatePartialGraph = new Button(grpActivate, SWT.RADIO);
		btnActivatePartialGraph.setToolTipText(msg.formatString("dep_operation_partial", activateOp));
		btnActivatePartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.partialGraphOnActivate, btnActivatePartialGraph.getSelection());
				if (btnActivatePartialGraph.getSelection()) {
					setMessage(msg.formatString("dep_operation_partial", activateOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivatePartialGraph.setBounds(463, 20, 89, 16);
		btnActivatePartialGraph.setText("Partial Graph");
		btnActivatePartialGraph.setSelection(Category.getState(Category.partialGraphOnActivate));	
	}
	
	/**
	 * Initialize and manage the deactivate radio group
	 */
	private void deactivate() {
		grpDeactivate = new Group(container, SWT.SHADOW_ETCHED_IN);
		grpDeactivate.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_activation", deactivateOp), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_activation", deactivateOp), IMessageProvider.INFORMATION);
			}
		});
		grpDeactivate.setToolTipText(msg.formatString("dep_operation_group_activation", deactivateOp));
		GridData gd_grpDeactivate = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpDeactivate.widthHint = grWidthHint;
		gd_grpDeactivate.heightHint = grpHeightHint;
		grpDeactivate.setLayoutData(gd_grpDeactivate);
		grpDeactivate.setText(msg.formatString("dep_bundles_name", msg.formatString("dep_operation_deactivate")));

		btnDeactivateRequiringBundles = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivateRequiringBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateRequiringBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_requiring", deactivateOp), IMessageProvider.INFORMATION);
				}
		}
		});
		btnDeactivateRequiringBundles.setToolTipText(msg.formatString("dep_operation_requiring", deactivateOp));
		btnDeactivateRequiringBundles.setBounds(194, 20, 72, 16);
		btnDeactivateRequiringBundles.setText("Requiring");
		if (!providingOnDeactivate && !partialOnDeactivate) {
			btnDeactivateRequiringBundles.setSelection(true);
		}
		
		btnDeactivateProvidingBundles = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivateProvidingBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.providingOnDeactivate, btnDeactivateProvidingBundles.getSelection());
				if (btnDeactivateProvidingBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_providing_requiring", deactivateOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateProvidingBundles.setToolTipText(msg.formatString("dep_operation_providing_requiring", deactivateOp));
		btnDeactivateProvidingBundles.setText("Providing and Requiring");
		btnDeactivateProvidingBundles.setBounds(290, 20, 149, 16);
		btnDeactivateProvidingBundles.setSelection(Category.getState(Category.providingOnDeactivate));
		
		btnDeactivatePartialGraph = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivatePartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.partialGraphOnDeactivate, btnDeactivatePartialGraph.getSelection());
				if (btnDeactivatePartialGraph.getSelection()) {
					setMessage(msg.formatString("dep_operation_partial", deactivateOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivatePartialGraph.setToolTipText(msg.formatString("dep_operation_partial", deactivateOp));
		btnDeactivatePartialGraph.setText("Partial Graph");
		btnDeactivatePartialGraph.setBounds(463, 20, 89, 16);
		btnDeactivatePartialGraph.setSelection(Category.getState(Category.partialGraphOnDeactivate));
	}

	/**
	 * Initialize and manage the start radio group
	 */
	private void start() {
		grpStart = new Group(container, SWT.NONE);
		grpStart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_startstop", startOp), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_startstop", startOp), IMessageProvider.INFORMATION);
			}
		});
		grpStart.setToolTipText(msg.formatString("dep_operation_group_startstop", startOp));
		GridData gd_grpStart = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpStart.heightHint = grpHeightHint;
		gd_grpStart.widthHint = grWidthHint;
		grpStart.setLayoutData(gd_grpStart);
		grpStart.setText(msg.formatString("dep_bundles_name", msg.formatString("dep_operation_start")));

		btnStartSingleBundles = new Button(grpStart, SWT.RADIO);
		btnStartSingleBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnStartSingleBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_single", startOp), IMessageProvider.INFORMATION);
				}
				if (!btnStartSingleBundles.getSelection()){
					setMessage("Excluding providing bundles on start may result in stale references", IMessageProvider.WARNING);
				}
			}
		});
		btnStartSingleBundles.setToolTipText(msg.formatString("dep_operation_single", startOp));
		btnStartSingleBundles.setBounds(30, 20, 53, 16);
		btnStartSingleBundles.setText("Single");
		if (!requiringOnStart && !providingOnStart && !partialOnStart) {
			btnStartSingleBundles.setSelection(true);
		}
		btnStartRequiringBundles = new Button(grpStart, SWT.RADIO);
		btnStartRequiringBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnStartRequiringProvidingBundles.getSelection()) {
					Category.setState(Category.requiringOnStart, btnStartRequiringBundles.getSelection());
				}			
				if (btnStartRequiringBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_requiring", startOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStartRequiringBundles.setToolTipText(msg.formatString("dep_operation_requiring", startOp));
		btnStartRequiringBundles.setBounds(194, 20, 72, 16);
		btnStartRequiringBundles.setText("Requiring");
		if (!providingOnStart) {
			btnStartRequiringBundles.setSelection(Category.getState(Category.requiringOnStart));
		} else {
			btnStartRequiringBundles.setSelection(false);			
		}
		btnStartProvidingBundles = new Button(grpStart, SWT.RADIO);
		btnStartProvidingBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnStartRequiringProvidingBundles.getSelection()) {
					Category.setState(Category.providingOnStart, btnStartProvidingBundles.getSelection());
				} 
				if (btnStartProvidingBundles.getSelection()){
					setMessage(msg.formatString("dep_operation_providing", startOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStartProvidingBundles.setToolTipText(msg.formatString("dep_operation_providing", startOp));
		btnStartProvidingBundles.setBounds(101, 20, 72, 16);
		btnStartProvidingBundles.setText("Providing");
		if (!requiringOnStart) {
			btnStartProvidingBundles.setSelection(Category.getState(Category.providingOnStart));
		} else {
			btnStartProvidingBundles.setSelection(false);			
		}
		btnStartRequiringProvidingBundles = new Button(grpStart, SWT.RADIO);
		btnStartRequiringProvidingBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.requiringOnStart, btnStartRequiringProvidingBundles.getSelection());
				Category.setState(Category.providingOnStart, btnStartRequiringProvidingBundles.getSelection());
				if (btnStartRequiringProvidingBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_requiring_providing", startOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStartRequiringProvidingBundles.setToolTipText(msg.formatString("dep_operation_requiring_providing", startOp));
		btnStartRequiringProvidingBundles.setBounds(290, 20, 144, 16);
		btnStartRequiringProvidingBundles.setText("Requiring and Providing");
		if (Category.getState(Category.requiringOnStart) && Category.getState(Category.providingOnStart)) {
			btnStartRequiringProvidingBundles.setSelection(true);
		}
		btnStartPartialGraph = new Button(grpStart, SWT.RADIO);
		btnStartPartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.partialGraphOnStart, btnStartPartialGraph.getSelection());
				if (btnStartPartialGraph.getSelection()) {
					setMessage(msg.formatString("dep_operation_partial", startOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStartPartialGraph.setToolTipText(msg.formatString("dep_operation_partial", startOp));
		btnStartPartialGraph.setBounds(463, 20, 89, 16);
		btnStartPartialGraph.setText("Partial Graph");
		btnStartPartialGraph.setSelection(Category.getState(Category.partialGraphOnStart));	
	}

	/**
	 * Initialize and manage the stop radio group
	 */
	private void stop() {
		grpStop = new Group(container, SWT.NONE);
		grpStop.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_startstop", stopOp), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(msg.formatString("dep_operation_group_startstop", stopOp), IMessageProvider.INFORMATION);
			}
		});
		grpStop.setToolTipText(msg.formatString("dep_operation_group_startstop", stopOp));
		GridData gd_grpStop = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_grpStop.heightHint = grpHeightHint;
		gd_grpStop.widthHint = grWidthHint;
		grpStop.setLayoutData(gd_grpStop);
		grpStop.setText(msg.formatString("dep_bundles_name", msg.formatString("dep_operation_stop")));

		
		btnStopSingleBundles = new Button(grpStop, SWT.RADIO);
		btnStopSingleBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnStopSingleBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_single", stopOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStopSingleBundles.setToolTipText(msg.formatString("dep_operation_single", stopOp));
		btnStopSingleBundles.setBounds(30, 20, 53, 16);
		btnStopSingleBundles.setText("Single");
		if (!requiringOnStop && !providingOnStop && !partialOnStop) {
			btnStopSingleBundles.setSelection(true);
		}

		btnStopRequiringBundles = new Button(grpStop, SWT.RADIO);
		btnStopRequiringBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnStopProvidingRequiringBundles.getSelection()) {
					Category.setState(Category.requiringOnStop, btnStopRequiringBundles.getSelection());
				}
				if (btnStopRequiringBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_requiring", stopOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStopRequiringBundles.setToolTipText(msg.formatString("dep_operation_requiring", stopOp));
		btnStopRequiringBundles.setText("Requiring");
		btnStopRequiringBundles.setBounds(194, 20, 72, 16);
		if (!providingOnStop) {
			btnStopRequiringBundles.setSelection(Category.getState(Category.requiringOnStop));
		} else {
			btnStopRequiringBundles.setSelection(false);			
		}

		btnStopProvidingBundles = new Button(grpStop, SWT.RADIO);
		btnStopProvidingBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnStopProvidingRequiringBundles.getSelection()) {
					Category.setState(Category.providingOnStop, btnStopProvidingBundles.getSelection());
				}
				if (btnStopProvidingBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_providing", stopOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStopProvidingBundles.setToolTipText(msg.formatString("dep_operation_providing", stopOp));
		btnStopProvidingBundles.setText("Providing");
		btnStopProvidingBundles.setBounds(101, 20, 72, 16);
		if (!requiringOnStop) {
			btnStopProvidingBundles.setSelection(Category.getState(Category.providingOnStop));
		} else {
			btnStopProvidingBundles.setSelection(false);			
		}
		btnStopProvidingRequiringBundles = new Button(grpStop, SWT.RADIO);
		btnStopProvidingRequiringBundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.requiringOnStop, btnStopProvidingRequiringBundles.getSelection());
				Category.setState(Category.providingOnStop, btnStopProvidingRequiringBundles.getSelection());
				if (btnStopProvidingRequiringBundles.getSelection()) {
					setMessage(msg.formatString("dep_operation_providing_requiring", stopOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStopProvidingRequiringBundles.setToolTipText(msg.formatString("dep_operation_providing_requiring", stopOp));
		btnStopProvidingRequiringBundles.setBounds(290, 20, 149, 16);
		btnStopProvidingRequiringBundles.setText("Providing and Requiring");
		if (Category.getState(Category.requiringOnStop) && Category.getState(Category.providingOnStop)) {
			btnStopProvidingRequiringBundles.setSelection(true);
		}		
		btnStopPartialGraph = new Button(grpStop, SWT.RADIO);
		btnStopPartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Category.setState(Category.partialGraphOnStop, btnStopPartialGraph.getSelection());
				if (btnStopPartialGraph.getSelection()) {
					setMessage(msg.formatString("dep_operation_partial", stopOp), IMessageProvider.INFORMATION);
				}
			}
		});
		btnStopPartialGraph.setToolTipText(msg.formatString("dep_operation_partial", stopOp));
		btnStopPartialGraph.setText("Partial Graph");
		btnStopPartialGraph.setBounds(463, 20, 89, 16);
		btnStopPartialGraph.setSelection(Category.getState(Category.partialGraphOnStop));
	}

	/**
	 * Create a default, cancel and ok button in the button bar.
	 * @param parent the title area dialog
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		Button btnDefault = createButton(parent, DEFAULT_ID, DEAFAULT_LABEL, false);
		btnDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				defaultDependencies();
			}
		});
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancel();
			}
		});
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(609, 441);
	}	
	
	/**
	 * Assign existing dependency settings 
	 */
	private void setInitialDependencies() {
		// Activate
		this.partialOnActivate = Category.getState(Category.partialGraphOnActivate);
		this.requiringOnActivate = Category.getState(Category.requiringOnActivate);
		// Start
		this.partialOnStart = Category.getState(Category.partialGraphOnStart);
		this.requiringOnStart = Category.getState(Category.requiringOnStart);
		this.providingOnStart = Category.getState(Category.providingOnStart);
		// Deactivate
		this.partialOnDeactivate = Category.getState(Category.partialGraphOnDeactivate);
		this.providingOnDeactivate = Category.getState(Category.providingOnDeactivate);
		// Stop		
		this.partialOnStop = Category.getState(Category.partialGraphOnStop);
		this.requiringOnStop = Category.getState(Category.requiringOnStop);
		this.providingOnStop = Category.getState(Category.providingOnStop);
	}
	
	/**
	 * Assign dependency setting from before the dependency dialog was created
	 */
	private void cancel() {
		// Activate
		Category.setState(Category.partialGraphOnActivate, this.partialOnActivate);
		Category.setState(Category.requiringOnActivate, this.requiringOnActivate);
		// Start
		Category.setState(Category.partialGraphOnStart, this.partialOnStart);
		Category.setState(Category.requiringOnStart, this.requiringOnStart);
		Category.setState(Category.providingOnStart, this.providingOnStart);
		// Deactivate
		Category.setState(Category.partialGraphOnDeactivate, this.partialOnDeactivate);
		Category.setState(Category.providingOnDeactivate, this.providingOnDeactivate);
		// Stop
		Category.setState(Category.partialGraphOnStop, this.partialOnStop);
		Category.setState(Category.requiringOnStop, this.requiringOnStop);
		Category.setState(Category.providingOnStop, this.providingOnStop);
	}

	/**
	 * Assign default dependency settings 
	 */
	private void defaultDependencies() {
		// Activate
		Boolean requiringOnActiavate = Category.getInstance().resetState(Category.requiringOnActivate);
		Boolean partialOnActiavte = Category.getInstance().resetState(Category.partialGraphOnActivate);
		if (!requiringOnActiavate && !partialOnActiavte) {
			btnActivateProvidingBundles.setSelection(true);
		} else {
			btnActivateProvidingBundles.setSelection(false);			
		}
		btnActivateRequiringBundles.setSelection(requiringOnActiavate);
		btnActivatePartialGraph.setSelection(partialOnActiavte);
		// Deactivate
		Boolean providingOnDeactivate = Category.getInstance().resetState(Category.providingOnDeactivate);
		Boolean partialOnDectiavate = Category.getInstance().resetState(Category.partialGraphOnDeactivate);
		if (!providingOnDeactivate && !partialOnDectiavate) {
			btnDeactivateRequiringBundles.setSelection(true);
		} else {
			btnDeactivateRequiringBundles.setSelection(false);			
		}
		btnDeactivateProvidingBundles.setSelection(providingOnDeactivate);
		btnDeactivatePartialGraph.setSelection(partialOnDectiavate);
		// Start
		Boolean requiringOnStart = Category.getInstance().resetState(Category.requiringOnStart);
		Boolean providingOnStart = Category.getInstance().resetState(Category.providingOnStart);
		Boolean partialOnStart = Category.getInstance().resetState(Category.partialGraphOnStart);		
		if (!partialOnStart && !requiringOnStart && !providingOnStart) {
			btnStartSingleBundles.setSelection(true);	
		} else {
			btnStartSingleBundles.setSelection(false);					
			if (requiringOnStart && providingOnStart) {
				btnStartRequiringProvidingBundles.setSelection(true);
			} else { 
				btnStartRequiringProvidingBundles.setSelection(false);					
				btnStartRequiringBundles.setSelection(requiringOnStart);			
				btnStartProvidingBundles.setSelection(providingOnStart);			
				btnStartPartialGraph.setSelection(partialOnStart);
			}
		}
		// Stop		
		Boolean requiringOnStop = Category.getInstance().resetState(Category.requiringOnStop);
		Boolean providingOnStop = Category.getInstance().resetState(Category.providingOnStop);
		Boolean partialOnStop = Category.getInstance().resetState(Category.partialGraphOnStop);;
		if (!partialOnStop && !requiringOnStop && !providingOnStop) {
			btnStopSingleBundles.setSelection(true);	
		} else {
			btnStopSingleBundles.setSelection(false);					
			if (requiringOnStop && providingOnStop) {
				btnStopProvidingRequiringBundles.setSelection(true);			
			} else {
				btnStopProvidingRequiringBundles.setSelection(false);			 
				btnStopRequiringBundles.setSelection(requiringOnStop);
				btnStopProvidingBundles.setSelection(providingOnStop);
				btnStopPartialGraph.setSelection(partialOnStop);
			}
		}
	}	
}
