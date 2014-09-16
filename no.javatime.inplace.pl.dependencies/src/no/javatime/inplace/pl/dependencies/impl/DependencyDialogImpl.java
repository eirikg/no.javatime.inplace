package no.javatime.inplace.pl.dependencies.impl;

import java.util.EnumSet;

import no.javatime.inplace.dl.preferences.intface.DependencyOptions;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Closure;
import no.javatime.inplace.dl.preferences.intface.DependencyOptions.Operation;
import no.javatime.inplace.pl.dependencies.Activator;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;
import no.javatime.inplace.pl.dependencies.msg.Msg;
import no.javatime.inplace.region.manager.InPlaceException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
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
 * Non-modal dialog for setting dependency closures on bundle project to activate, deactivate,
 * start and stop.
 * <p>
 * To maintain the design view in Windows Builder, do not generalize the code 
 */
public class DependencyDialogImpl extends TitleAreaDialog implements DependencyDialog {

	static int DEFAULT_ID = 101;
	static String DEAFAULT_LABEL = "Default";
	Image captionDialogImage = null;
	private Composite container;

	// Groups and buttons
	Group grpActivateProject = null;
	Button btnActivateProjectProviding = null;
	Button btnActivateProjectRequiringAndProviding = null;
	Button btnActivateProjectPartialGraph = null;
	Group grpDeactivate = null;
	Button btnDeactivateProjectRequiring = null;
	Button btnDeactivateProjectProvidingAndRequiring = null;
	Button btnDeactivateProjectPartialGraph = null;
	Group grpStart = null;
	Button btnActivateBundleSingle = null;
	Button btnActivateBundleRequiring = null;
	Button btnActivateBundleProviding = null;
	Button btnActivateBundleRequiringProviding = null;
	Button btnActivateBundlePartialGraph = null;
	Group grpStop = null;
	Button btnDeactivateBundleSingle = null;
	Button btnDeactivateBundleRequiring = null;
	Button btnDeactivateBundleProviding = null;
	Button btnDeactivateBundleProvidingRequiring = null;
	Button btnDeactivateBundlePartialGraph = null;
	
	// Set on start up and reverted to start up values on cancel 
	// Activate
	private Boolean activateProjectProvidingClosure = true;
	private Boolean activateProjectRequiringAndProvidingClosure = false;
	private Boolean activateProjectPartialGraphClosure = false;
	// Deactivate
	private Boolean deactivateProjectRequiringClosure = true;
	private Boolean deactivateProjectPartialGraphClosure = false;
	private Boolean deactivateProjectProvidingAndRequiringClosure = true;
	// Start
	private Boolean activateBundlePartialGraphClosure = false;	
	private Boolean activateBundleRequiringAndProvidingClosure = false;
	private Boolean activateBundleProvidingClosure = true;
	private Boolean activateBundleRequiringClosure = true;
	private Boolean activateBundleSingleClosure = true;
	// Stop
	private Boolean deactivateBundlePartialGraphClosure = false;
	private Boolean deactivateBundleRequiringClosure = false;
	private Boolean deactivateBundleProvidingClosure = true;
	private Boolean deactivateBundleProvidingAndRequiringClosure = false;
	private Boolean deactivateBundleSingleClosure = false;

	
	// Size of radio groups
	private int grpHeightHint = 40;
	private int	grWidthHint = 550;

	public DependencyDialogImpl() {
		super(Activator.getDefault().getShell());
		setHelpAvailable(false);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
		setBlockOnOpen(false);
		setInitialDependencies();
	}
	
	/**
	 * Create the dialog.
	 * @param parentShell the workspace shell
	 * @wbp.parser.constructor
	 */
	public DependencyDialogImpl(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
		setBlockOnOpen(false);
		setInitialDependencies();
	}
	
	/**
	 * Standard contract for extenders
	 */
	@Override
	public int open() {
		return super.open();
	}

	/**
	 * Standard contract for extenders
	 */
	@Override
	public boolean close() {
		if (null != captionDialogImage) {
			captionDialogImage.dispose();
		}
		return super.close();
	}
	
	DependencyOptions getOpt() {
		return Activator.getDefault().getDependencyOptionsService();			
	}
	
	/**
	 * Create the dialog with radio groups for activate, deactivate, start and stop.
	 * @param parent the dialog
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		captionDialogImage = Activator.getImageDescriptor("icons/dependencies.gif").createImage();
		parent.getShell().setImage(captionDialogImage);
		parent.getShell().setText(Msg.DIALOG_CAPTION_TEXT);
		setMessage(Msg.MAIN_MESSAGE);
		setTitle(Msg.TITLE_MESSAGE);
		container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		// Activate, deactivate, start and stop groups		
		activateProject();		
		deactivateProject();
		activateBundle();
		deactivateBundle();		
		return container;
	}

	/**
	 * Initialize and manage the activate radio group
	 */
	private void activateProject() {
		
//		grpActivateProject = createGroup(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.ACTIVATE_OPERATION),
//				NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.ACTIVATE_OPERATION));

		grpActivateProject = new Group(container, SWT.SHADOW_ETCHED_OUT);
		grpActivateProject.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.ACTIVATE_OPERATION), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.ACTIVATE_OPERATION), IMessageProvider.INFORMATION);
			}
		});
		grpActivateProject.setToolTipText(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.ACTIVATE_OPERATION));
		GridData gd_grpActivate = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpActivate.heightHint = grpHeightHint;
		gd_grpActivate.widthHint = grWidthHint;
		grpActivateProject.setLayoutData(gd_grpActivate);
		grpActivateProject.setText(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.ACTIVATE_OPERATION));

		// Closure: Providing
		btnActivateProjectProviding = new Button(grpActivateProject, SWT.RADIO);
		btnActivateProjectProviding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateProjectProviding.getSelection()) {
					set(Operation.ACTIVATE_PROJECT, Closure.PROVIDING, true);
					setMessage(NLS.bind(Msg.PROVIDING_OPERATION_DESC, Msg.ACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateProjectProviding.setToolTipText(NLS.bind(Msg.PROVIDING_OPERATION_DESC, Msg.ACTIVATE_OPERATION));
		btnActivateProjectProviding.setBounds(101, 20, 72, 16);
		btnActivateProjectProviding.setText(Msg.PROVIDING_LABEL);
		btnActivateProjectProviding.setSelection(
					get(Operation.ACTIVATE_PROJECT, Closure.PROVIDING));			
		
		btnActivateProjectRequiringAndProviding = new Button(grpActivateProject, SWT.RADIO);
		btnActivateProjectRequiringAndProviding.setToolTipText(NLS.bind(Msg.REQUIRING_PROVIDING_OPERATION_DESC, Msg.ACTIVATE_OPERATION));
		btnActivateProjectRequiringAndProviding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateProjectRequiringAndProviding.getSelection()) {
					set(Operation.ACTIVATE_PROJECT, Closure.REQUIRING_AND_PROVIDING, true);
					setMessage(NLS.bind(Msg.REQUIRING_PROVIDING_OPERATION_DESC, Msg.ACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});

		btnActivateProjectRequiringAndProviding.setBounds(290, 20, 149, 16);
		btnActivateProjectRequiringAndProviding.setText(Msg.REQURING_AND_PROVIDING_LABEL);
		btnActivateProjectRequiringAndProviding.setSelection(
				get(Operation.ACTIVATE_PROJECT, Closure.REQUIRING_AND_PROVIDING));
	
		btnActivateProjectPartialGraph = new Button(grpActivateProject, SWT.RADIO);
		btnActivateProjectPartialGraph.setToolTipText(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.ACTIVATE_OPERATION));
		btnActivateProjectPartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateProjectPartialGraph.getSelection()) {
					set(Operation.ACTIVATE_PROJECT, Closure.PARTIAL_GRAPH, true);
					setMessage(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.ACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateProjectPartialGraph.setBounds(463, 20, 89, 16);
		btnActivateProjectPartialGraph.setText(Msg.PARTIAL_GRAPH_LABEL);
		btnActivateProjectPartialGraph.setSelection(	
				get(Operation.ACTIVATE_PROJECT, Closure.PARTIAL_GRAPH));
	}
	
	/**
	 * Initialize and manage the deactivate radio group
	 */
	private void deactivateProject() {
		
//		grpDeactivate = createGroup(NLS.bind(Msg.BUNDLES_OPERATION_NAME, 
//				Msg.DEACTIVATE_OPERATION), NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.DEACTIVATE_OPERATION));
	
		grpDeactivate = new Group(container, SWT.SHADOW_ETCHED_IN);
		grpDeactivate.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.DEACTIVATE_OPERATION), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.DEACTIVATE_OPERATION), IMessageProvider.INFORMATION);
			}
		});
		grpDeactivate.setToolTipText(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.DEACTIVATE_OPERATION));
		GridData gd_grpDeactivate = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpDeactivate.widthHint = grWidthHint;
		gd_grpDeactivate.heightHint = grpHeightHint;
		grpDeactivate.setLayoutData(gd_grpDeactivate);
		grpDeactivate.setText(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.DEACTIVATE_OPERATION));

		btnDeactivateProjectRequiring = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivateProjectRequiring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateProjectRequiring.getSelection()) {
					set(Operation.DEACTIVATE_PROJECT, Closure.REQUIRING, true);
					setMessage(NLS.bind(Msg.REQUIRING_OPERATION_DESC, Msg.DEACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
		}
		});
		btnDeactivateProjectRequiring.setToolTipText(NLS.bind(Msg.REQUIRING_OPERATION_DESC, Msg.DEACTIVATE_OPERATION));
		btnDeactivateProjectRequiring.setBounds(194, 20, 72, 16);
		btnDeactivateProjectRequiring.setText(Msg.REQUIRING_LABEL);		
			btnDeactivateProjectRequiring.setSelection(
					get(Operation.DEACTIVATE_PROJECT, Closure.REQUIRING));
		
		btnDeactivateProjectProvidingAndRequiring = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivateProjectProvidingAndRequiring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateProjectProvidingAndRequiring.getSelection()) {
					set(Operation.DEACTIVATE_PROJECT, Closure.PROVIDING_AND_REQURING, true);
					setMessage(NLS.bind(Msg.PROVIDING_REQUIRING_OPERATION_DESC, Msg.DEACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateProjectProvidingAndRequiring.setToolTipText(NLS.bind(Msg.PROVIDING_REQUIRING_OPERATION_DESC, Msg.DEACTIVATE_OPERATION));
		btnDeactivateProjectProvidingAndRequiring.setText(Msg.PROVIDING_AND_REQUIRING_LABEL);
		btnDeactivateProjectProvidingAndRequiring.setBounds(290, 20, 149, 16);
		btnDeactivateProjectProvidingAndRequiring.setSelection(
				get(Operation.DEACTIVATE_PROJECT, Closure.PROVIDING_AND_REQURING));		
		btnDeactivateProjectPartialGraph = new Button(grpDeactivate, SWT.RADIO);
		btnDeactivateProjectPartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateProjectPartialGraph.getSelection()) {
					set(Operation.DEACTIVATE_PROJECT, Closure.PARTIAL_GRAPH, true);		
					setMessage(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.DEACTIVATE_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateProjectPartialGraph.setToolTipText(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.DEACTIVATE_OPERATION));
		btnDeactivateProjectPartialGraph.setText(Msg.PARTIAL_GRAPH_LABEL);
		btnDeactivateProjectPartialGraph.setBounds(463, 20, 89, 16);
		btnDeactivateProjectPartialGraph.setSelection(
				get(Operation.DEACTIVATE_PROJECT, Closure.PARTIAL_GRAPH));		
	}
	
	/**
	 * Initialize and manage the start radio group
	 */
	private void activateBundle() {

//		grpStart = createGroup(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.START_OPERATION), 
//				NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.START_OPERATION));

		grpStart = new Group(container, SWT.NONE);
		grpStart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
			}
		});
		grpStart.setToolTipText(NLS.bind(Msg.ACTIVATE_GROUP_DESC, Msg.START_OPERATION));
		GridData gd_grpStart = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_grpStart.heightHint = grpHeightHint;
		gd_grpStart.widthHint = grWidthHint;
		grpStart.setLayoutData(gd_grpStart);
		grpStart.setText(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.START_OPERATION));

		btnActivateBundleSingle = new Button(grpStart, SWT.RADIO);
		btnActivateBundleSingle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateBundleSingle.getSelection()) {
					set(Operation.ACTIVATE_BUNDLE, Closure.SINGLE, true);		
					setMessage(NLS.bind(Msg.SINGLE_START_OPERATION_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateBundleSingle.setToolTipText(NLS.bind(Msg.SINGLE_START_OPERATION_DESC, Msg.START_OPERATION));
		btnActivateBundleSingle.setBounds(30, 20, 53, 16);
		btnActivateBundleSingle.setText(Msg.SINGLE_LABEL);
		btnActivateBundleSingle.setSelection(
				get(Operation.ACTIVATE_BUNDLE, Closure.SINGLE));		
		
		btnActivateBundleRequiring = new Button(grpStart, SWT.RADIO);
		btnActivateBundleRequiring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateBundleRequiring.getSelection()) {
					set(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING, true);		
					setMessage(NLS.bind(Msg.REQUIRING_START_OPERATION_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});

		btnActivateBundleRequiring.setToolTipText(NLS.bind(Msg.REQUIRING_OPERATION_DESC, Msg.START_OPERATION));
		btnActivateBundleRequiring.setBounds(194, 20, 72, 16);
		btnActivateBundleRequiring.setText(Msg.REQUIRING_LABEL);
		btnActivateBundleRequiring.setSelection(
					get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING));		
		
		btnActivateBundleProviding = new Button(grpStart, SWT.RADIO);
		btnActivateBundleProviding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateBundleProviding.getSelection()){
					set(Operation.ACTIVATE_BUNDLE, Closure.PROVIDING, true);		
					setMessage(NLS.bind(Msg.PROVIDING_OPERATION_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateBundleProviding.setToolTipText(NLS.bind(Msg.PROVIDING_OPERATION_DESC, Msg.START_OPERATION));
		btnActivateBundleProviding.setBounds(101, 20, 72, 16);
		btnActivateBundleProviding.setText(Msg.PROVIDING_LABEL);
		btnActivateBundleProviding.setSelection(
				get(Operation.ACTIVATE_BUNDLE, Closure.PROVIDING));		
		btnActivateBundleRequiringProviding = new Button(grpStart, SWT.RADIO);
		btnActivateBundleRequiringProviding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateBundleRequiringProviding.getSelection()) {
					set(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING_AND_PROVIDING, true);		
					setMessage(NLS.bind(Msg.REQUIRING_PROVIDING_OPERATION_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateBundleRequiringProviding.setToolTipText(NLS.bind(Msg.REQUIRING_PROVIDING_OPERATION_DESC, Msg.START_OPERATION));
		btnActivateBundleRequiringProviding.setBounds(290, 20, 144, 16);
		btnActivateBundleRequiringProviding.setText(Msg.REQURING_AND_PROVIDING_LABEL);
			btnActivateBundleRequiringProviding.setSelection(
					get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING_AND_PROVIDING));		

			btnActivateBundlePartialGraph = new Button(grpStart, SWT.RADIO);
		btnActivateBundlePartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnActivateBundlePartialGraph.getSelection()) {
					set(Operation.ACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH, true);		
					setMessage(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.START_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnActivateBundlePartialGraph.setToolTipText(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.START_OPERATION));
		btnActivateBundlePartialGraph.setBounds(463, 20, 89, 16);
		btnActivateBundlePartialGraph.setText(Msg.PARTIAL_GRAPH_LABEL);
		btnActivateBundlePartialGraph.setSelection(
				get(Operation.ACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH));		
	}

	/**
	 * Initialize and manage the stop radio group
	 */
	private void deactivateBundle() {

//		grpStop = createGroup(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.STOP_OPERATION), 
//				NLS.bind(Msg.DEACTIVATE_GROUP_DESC, Msg.STOP_OPERATION));
		grpStop = new Group(container, SWT.NONE);
		grpStop.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setMessage(NLS.bind(Msg.DEACTIVATE_GROUP_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				setMessage(NLS.bind(Msg.DEACTIVATE_GROUP_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
			}
		});
		grpStop.setToolTipText(NLS.bind(Msg.DEACTIVATE_GROUP_DESC, Msg.STOP_OPERATION));
		GridData gd_grpStop = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_grpStop.heightHint = grpHeightHint;
		gd_grpStop.widthHint = grWidthHint;
		grpStop.setLayoutData(gd_grpStop);
		grpStop.setText(NLS.bind(Msg.BUNDLES_OPERATION_NAME, Msg.STOP_OPERATION));

		
		btnDeactivateBundleSingle = new Button(grpStop, SWT.RADIO);
		btnDeactivateBundleSingle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateBundleSingle.getSelection()) {
					set(Operation.DEACTIVATE_BUNDLE, Closure.SINGLE, true);		
					setMessage(NLS.bind(Msg.SINGLE_STOP_OPERATION_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateBundleSingle.setToolTipText(NLS.bind(Msg.SINGLE_START_OPERATION_DESC, Msg.STOP_OPERATION));
		btnDeactivateBundleSingle.setBounds(30, 20, 53, 16);
		btnDeactivateBundleSingle.setText(Msg.SINGLE_LABEL);
			btnDeactivateBundleSingle.setSelection(
					get(Operation.DEACTIVATE_BUNDLE, Closure.SINGLE));		
		btnDeactivateBundleRequiring = new Button(grpStop, SWT.RADIO);
		btnDeactivateBundleRequiring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateBundleRequiring.getSelection()) {
					set(Operation.DEACTIVATE_BUNDLE, Closure.REQUIRING, true);		
					setMessage(NLS.bind(Msg.REQUIRING_OPERATION_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateBundleRequiring.setToolTipText(NLS.bind(Msg.REQUIRING_OPERATION_DESC, Msg.STOP_OPERATION));
		btnDeactivateBundleRequiring.setText(Msg.REQUIRING_LABEL);
		btnDeactivateBundleRequiring.setBounds(194, 20, 72, 16);
			btnDeactivateBundleRequiring.setSelection(
					get(Operation.DEACTIVATE_BUNDLE, Closure.REQUIRING));		

		btnDeactivateBundleProviding = new Button(grpStop, SWT.RADIO);
		btnDeactivateBundleProviding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateBundleProviding.getSelection()) {
					set(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING, true);		
					setMessage(NLS.bind(Msg.PROVIDING_STOP_OPERATION_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateBundleProviding.setToolTipText(NLS.bind(Msg.PROVIDING_OPERATION_DESC, Msg.STOP_OPERATION));
		btnDeactivateBundleProviding.setText(Msg.PROVIDING_LABEL);
		btnDeactivateBundleProviding.setBounds(101, 20, 72, 16);
		btnDeactivateBundleProviding.setSelection(
				get(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING));		

		btnDeactivateBundleProvidingRequiring = new Button(grpStop, SWT.RADIO);
		btnDeactivateBundleProvidingRequiring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateBundleProvidingRequiring.getSelection()) {
					set(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING_AND_REQURING, true);		
					setMessage(NLS.bind(Msg.PROVIDING_REQUIRING_OPERATION_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateBundleProvidingRequiring.setToolTipText(NLS.bind(Msg.PROVIDING_REQUIRING_OPERATION_DESC, Msg.STOP_OPERATION));
		btnDeactivateBundleProvidingRequiring.setBounds(290, 20, 149, 16);
		btnDeactivateBundleProvidingRequiring.setText(Msg.PROVIDING_AND_REQUIRING_LABEL);
		btnDeactivateBundleProvidingRequiring.setSelection(
				get(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING_AND_REQURING));		
		btnDeactivateBundlePartialGraph = new Button(grpStop, SWT.RADIO);
		btnDeactivateBundlePartialGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnDeactivateBundlePartialGraph.getSelection()) {
					set(Operation.DEACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH, true);		
					setMessage(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.STOP_OPERATION), IMessageProvider.INFORMATION);
				}
			}
		});
		btnDeactivateBundlePartialGraph.setToolTipText(NLS.bind(Msg.PARTIAL_GRAPH_OPERATION_DESC, Msg.STOP_OPERATION));
		btnDeactivateBundlePartialGraph.setText(Msg.PARTIAL_GRAPH_LABEL);
		btnDeactivateBundlePartialGraph.setBounds(463, 20, 89, 16);
		btnDeactivateBundlePartialGraph.setSelection(
				get(Operation.DEACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH));		
	}
	
	@SuppressWarnings("unused")
	private Button createButton(final Operation op, final Closure cls,
			Group grp, final String opMsg, String opLabel) {

		final Button btn = new Button(grp, SWT.RADIO);
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btn.getSelection()) {
					set(op, cls, true);		
					setMessage(opMsg, IMessageProvider.INFORMATION);
				}
			}
		});
		btn.setToolTipText(opMsg);
		btn.setText(opLabel);
		btn.setBounds(463, 20, 89, 16);
		btn.setSelection(
				get(op, cls));		
		return btn;
	}

@SuppressWarnings("unused")
private Group createGroup(String groupName, final String groupText) {

	Group group = new Group(container, SWT.NONE);
	group.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			setMessage(groupText, IMessageProvider.INFORMATION);
		}
		@Override
		public void mouseDown(MouseEvent e) {
			setMessage(groupText, IMessageProvider.INFORMATION);
		}
	});
	group.setToolTipText(groupText);
	GridData gd_groupp = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
	gd_groupp.heightHint = grpHeightHint;
	gd_groupp.widthHint = grWidthHint;
	group.setLayoutData(gd_groupp);
	group.setText(groupName);
	return group;
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

		activateProjectProvidingClosure =
				get(Operation.ACTIVATE_PROJECT, Closure.PROVIDING);			
		activateProjectRequiringAndProvidingClosure =
				get(Operation.ACTIVATE_PROJECT, Closure.REQUIRING_AND_PROVIDING);			
		activateProjectPartialGraphClosure =
				get(Operation.ACTIVATE_PROJECT, Closure.PARTIAL_GRAPH);			
		// Deactivate
		deactivateProjectRequiringClosure =
				get(Operation.DEACTIVATE_PROJECT, Closure.REQUIRING);			
		deactivateProjectProvidingAndRequiringClosure =
				get(Operation.DEACTIVATE_PROJECT, Closure.PROVIDING_AND_REQURING);			
		deactivateProjectPartialGraphClosure = 
				get(Operation.DEACTIVATE_PROJECT, Closure.PARTIAL_GRAPH);			
		// Start
		activateBundleProvidingClosure =
				get(Operation.ACTIVATE_BUNDLE, Closure.PROVIDING);			
		activateBundleRequiringClosure = 
				get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING);			
		activateBundleRequiringAndProvidingClosure =
				get(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING_AND_PROVIDING);			
		activateBundlePartialGraphClosure = 
		get(Operation.ACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH);			
		activateBundleSingleClosure =
				get(Operation.ACTIVATE_BUNDLE, Closure.SINGLE);			
		// Stop		
		deactivateBundleProvidingClosure =
				get(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING);			
		deactivateBundleRequiringClosure =
				get(Operation.DEACTIVATE_BUNDLE, Closure.REQUIRING);			
		deactivateBundleProvidingAndRequiringClosure =
				get(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING_AND_REQURING);			
		deactivateBundlePartialGraphClosure =
				get(Operation.DEACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH);			
		deactivateBundleSingleClosure =
				get(Operation.DEACTIVATE_BUNDLE, Closure.SINGLE);			
	}
	
	
	/**
	 * Assign dependency setting from before the dependency dialog was created
	 */
	private void cancel() {
		// Activate
		set(Operation.ACTIVATE_PROJECT, Closure.PROVIDING, activateProjectProvidingClosure);			
		set(Operation.ACTIVATE_PROJECT, Closure.REQUIRING_AND_PROVIDING, activateProjectRequiringAndProvidingClosure);			
		set(Operation.ACTIVATE_PROJECT, Closure.PARTIAL_GRAPH, activateProjectPartialGraphClosure);

		set(Operation.DEACTIVATE_PROJECT, Closure.REQUIRING, deactivateProjectRequiringClosure);			
		set(Operation.DEACTIVATE_PROJECT, Closure.PROVIDING_AND_REQURING, deactivateProjectProvidingAndRequiringClosure);			
		set(Operation.DEACTIVATE_PROJECT, Closure.PARTIAL_GRAPH, deactivateProjectPartialGraphClosure);

		set(Operation.ACTIVATE_BUNDLE, Closure.PROVIDING, activateBundleProvidingClosure);			
		set(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING, activateBundleRequiringClosure);			
		set(Operation.ACTIVATE_BUNDLE, Closure.REQUIRING_AND_PROVIDING, activateBundleRequiringAndProvidingClosure);			
		set(Operation.ACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH, activateBundlePartialGraphClosure);
		set(Operation.ACTIVATE_BUNDLE, Closure.SINGLE, activateBundleSingleClosure);			
		
		set(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING, deactivateBundleProvidingClosure);			
		set(Operation.DEACTIVATE_BUNDLE, Closure.REQUIRING, deactivateBundleRequiringClosure);			
		set(Operation.DEACTIVATE_BUNDLE, Closure.PROVIDING_AND_REQURING, deactivateBundleProvidingAndRequiringClosure);			
		set(Operation.DEACTIVATE_BUNDLE, Closure.PARTIAL_GRAPH, deactivateBundlePartialGraphClosure);
		set(Operation.DEACTIVATE_BUNDLE, Closure.SINGLE, deactivateBundleSingleClosure);			
	}

	/**
	 * Assign default dependency settings 
	 */
	private void defaultDependencies() {
		// Activate
		setDeaultOption(Operation.ACTIVATE_PROJECT);
		setDeaultOption(Operation.DEACTIVATE_PROJECT);
		setDeaultOption(Operation.ACTIVATE_BUNDLE);
		setDeaultOption(Operation.DEACTIVATE_BUNDLE);		
	}	

	private void setDeaultOption(Operation op) {
		
		try {
			getOpt().set(op, getOpt().getDefault(op));								
			EnumSet<Closure> cls = getOpt().getvalidClosures(op);
			for (Closure cl : cls) {
				boolean state = getOpt().isDefault(op, cl);
				Button btn = getBind(op,cl);
				if (null != btn) {
					btn.setSelection(state);
				}
			}		
		} catch (IllegalStateException e) {
			setMessage(Msg.ILLEGAL_STATE_ON_SAVE_EXCEPTION, IMessageProvider.ERROR);
		} catch (InPlaceException e) {
			setMessage(Msg.INVALID_OPTIONS_SERVICE_EXCEPTION, IMessageProvider.ERROR);
		}
	}

	private void set(Operation op, Closure cl, boolean value) {
		try {
			if (value) {
				getOpt().set(op, cl);			
			}
		} catch (IllegalStateException e) {
			setMessage(Msg.ILLEGAL_STATE_ON_SAVE_EXCEPTION, IMessageProvider.ERROR);
		} catch (InPlaceException e) {
			setMessage(Msg.INVALID_OPTIONS_SERVICE_EXCEPTION, IMessageProvider.ERROR);
		}
	}
	
	private boolean get(Operation op, Closure cl) {
		try {
			return getOpt().get(op, cl);			
		} catch (IllegalStateException e) {
			setMessage(Msg.ILLEGAL_STATE_ON_READ_EXCEPTION, IMessageProvider.ERROR);
		} catch (InPlaceException e) {
			setMessage(Msg.INVALID_OPTIONS_SERVICE_EXCEPTION, IMessageProvider.ERROR);
		}
		return false;
	}
	

	private Button getBind(Operation operation, Closure closure) {
		switch (operation) {
		case ACTIVATE_PROJECT:
			switch (closure) {
			case PROVIDING:
				return btnActivateProjectProviding;
			case REQUIRING_AND_PROVIDING:
				return btnActivateProjectRequiringAndProviding;
			case PARTIAL_GRAPH:
				return btnActivateProjectPartialGraph;
			default:
				return null;
			}
		case DEACTIVATE_PROJECT:
			switch (closure) {
			case REQUIRING:
				return btnDeactivateProjectRequiring;
			case PROVIDING_AND_REQURING:
				return btnDeactivateProjectProvidingAndRequiring;
			case PARTIAL_GRAPH:
				return btnDeactivateProjectPartialGraph;
			default:
			}
		case ACTIVATE_BUNDLE:
			switch (closure) {
			case PROVIDING:
				return btnActivateBundleProviding;
			case REQUIRING:
				return btnActivateBundleRequiring;
			case REQUIRING_AND_PROVIDING:
				return btnActivateBundleRequiringProviding;
			case PARTIAL_GRAPH:
				return btnActivateBundlePartialGraph;
			case SINGLE:
				return btnActivateBundleSingle;
			default:
				return null;
			}
		default: {
			return null;
		}
		case DEACTIVATE_BUNDLE:
			switch (closure) {
			case PROVIDING:
				return btnDeactivateBundleProviding;
			case REQUIRING:
				return btnDeactivateBundleRequiring;
			case PROVIDING_AND_REQURING:
				return btnDeactivateBundleProvidingRequiring;
			case PARTIAL_GRAPH:
				return btnDeactivateBundlePartialGraph;
			case SINGLE:
				return btnDeactivateBundleSingle;
			default:
				return null;
			}
		}
	}
}
