package no.javatime.inplace.pl.preferences.page;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.pl.preferences.Activator;
import no.javatime.inplace.pl.preferences.msg.Msg;
import no.javatime.inplace.region.status.BundleStatus;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Standard preference page of command and manifest bundle options Maintains preferences stored in the OSGi preference
 * store with a copy in the the default plug-in preference store for preference pages.
 * <p>
 * On initialization of the preference page values are loaded from OSGi preferences and stored in the plug-in
 * preferences before they are displayed.
 * <p>
 * On ok and apply values are first stored in the plug-in preference store before stored in the OSGi preference store.
 * <p>
 * Other bundles relies on the shared data in the OSGi preference store.
 */

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	// The group field editor for timeout settings
	GroupFieldEditor groupTimeoutEditor;
	// Editor for the number of seconds before timeout
	IntegerFieldEditor timoutSecEditor;

	/**
	 * Sets the grid layout and initializes the preference page with the standard preference store
	 */
	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Msg.PAGE_DESCRIPTION_LABEL);
	}

	/**
	 * Get the command and manifest options from OSGi preferences store
	 * 
	 * @return the command and manifest options
	 * @throws IllegalStateException when the options service is unavailable
	 */
	public CommandOptions getPrefService() throws IllegalStateException {
		return Activator.getDefault().getOptionsService(); // tracker
		// return OptionsService.getCommandOptions(); // DS
	}

	/**
	 * Create group and field editors
	 */
	public void createFieldEditors() {

		// Timeout options group
		groupTimeoutEditor = new GroupFieldEditor(Msg.TIMEOUT_GROUP_LABEL, getFieldEditorParent());
		addField(groupTimeoutEditor);

		// Stop bundle operations manually from menu or automatic after timeout
		RadioGroupFieldEditor timeoutRadioGroupEditor = new RadioGroupFieldEditor(Msg.TIMEOUT_RADIOGROUP_NAME_LABEL,
				Msg.TERMINATE_ON_TIMEOUT_LABEL, 1, new String[][] {
						{ Msg.IS_MANUAL_TERMINATE_LABEL, CommandOptions.IS_MANUAL_TERMINATE },
						{ Msg.IS_TIMEOUT_LABEL, CommandOptions.IS_TIMEOUT } },
				groupTimeoutEditor.getMemberFieldEditorParent(), true);
		addField(timeoutRadioGroupEditor);
		groupTimeoutEditor.add(timeoutRadioGroupEditor);

		// Refresh when terminating bundle task
		BooleanFieldEditor booleanEditor = new BooleanFieldEditor(CommandOptions.IS_DEACTIVATE_ON_TERMINATE,
				Msg.IS_DEACTIVATE_ON_TERMINATE_LABEL, groupTimeoutEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupTimeoutEditor.add(booleanEditor);

		// If timeout is enabled specify the duration in seconds before timeout
		timoutSecEditor = new IntegerFieldEditor(CommandOptions.TIMEOUT_SECONDS, Msg.TIMEOUT_SECONDS_LABEL,
				groupTimeoutEditor.getMemberFieldEditorParent());
		timoutSecEditor.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
		timoutSecEditor.setValidRange(1, 60);
		addField(timoutSecEditor);
		groupTimeoutEditor.add(timoutSecEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		// OSGi Command options group
		GroupFieldEditor groupCmdEditor = new GroupFieldEditor(Msg.COMMAND_GROUP_LABEL, getFieldEditorParent());
		addField(groupCmdEditor);

		// Enable/Disable to run update job after build
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_UPDATE_ON_BUILD,
				Msg.IS_UPDATE_ON_BUILD_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable to activate and update projects with compile time errors
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_ACTIVATE_ON_COMPILE_ERROR,
				Msg.IS_ACTIVATE_ON_COMPILE_ERROR_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable to run refresh after update
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_REFRESH_ON_UPDATE,
				Msg.IS_REFRESH_ON_UPDATE_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable deactivation of the workspace at shut down
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_DEACTIVATE_ON_EXIT,
				Msg.IS_DEACTIVATE_ON_EXIT_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable to handle external bundle commands automatically
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS,
				Msg.IS_AUTO_HANDLE_EXTERNAL_COMMANDS_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		// Manifest options group
		GroupFieldEditor groupManifestEditor = new GroupFieldEditor(Msg.MANIFEST_GROUP_LABEL,
				getFieldEditorParent());
		addField(groupManifestEditor);

		// Explanatory label for default output folder
		LabelFieldEditor defOutputFolderlabelFieldEditor = new LabelFieldEditor(Msg.DEAFULT_OUTPUT_FOLDER_LABEL,
				groupManifestEditor.getMemberFieldEditorParent());
		addField(defOutputFolderlabelFieldEditor);
		groupManifestEditor.add(defOutputFolderlabelFieldEditor);

		// Enable/Disable to update default output folder on Activation/Deactivation
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER,
				Msg.IS_UPDATE_DEFAULT_OUTPUT_FOLDER_LABEL, groupManifestEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupManifestEditor.add(booleanEditor);

		// Enable/Disable to set eager activation policy on bundles when activated
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_EAGER_ON_ACTIVATE,
				Msg.IS_EAGER_ON_ACTIVATE_LABEL, groupManifestEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupManifestEditor.add(booleanEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		// Save options
		GroupFieldEditor groupSaveEditor = new GroupFieldEditor(Msg.SAVE_GROUP_LABEL,
				getFieldEditorParent());
		addField(groupSaveEditor);
		// Save dirty editors
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION,
				Msg.IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION_LABEL, groupSaveEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupSaveEditor.add(booleanEditor);
		// Save a snapshot of the workspace
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION,
				Msg.IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION_LABEL, groupSaveEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupSaveEditor.add(booleanEditor);

		// addField(new SpacerFieldEditor(getFieldEditorParent()));

		// Enable/Disable to include bundles contributing to the UI using extension
		addField(new LabelFieldEditor(Msg.ALLOW_EXTENSIONS_TEXT_LABEL, getFieldEditorParent()));
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS,
				Msg.IS_ALLOW_UI_CONTRIBUTIONS_LABEL, getFieldEditorParent());
		addField(booleanEditor);

		// Enable/Disable to activate and update projects with compile time errors
//		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_ACTIVATE_ON_COMPILE_ERROR,
//				Msg.IS_ACTIVATE_ON_COMPILE_ERROR_LABEL, getFieldEditorParent());
//		addField(booleanEditor);

		// Footnote
		addField(new SeparatorFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor(Msg.AVAILABLE_FROM_MAIN_MENU_LABEL, getFieldEditorParent()));

	}

	@Override
	protected void addField(FieldEditor editor) {
		super.addField(editor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void initialize() {
		loadOptions();
		super.initialize();
	}

	/**
	 * Set values for all editors when preference page is opened
	 */
	private void loadOptions() {

		try {
			CommandOptions cmdStore = getPrefService();
			IPreferenceStore prefStore = getPreferenceStore();
			prefStore.setValue(CommandOptions.TIMEOUT_SECONDS, cmdStore.getTimeout());
			Boolean isTimeout = cmdStore.isTimeOut();
			prefStore.setValue(CommandOptions.IS_TIMEOUT, isTimeout);
			if (isTimeout) {
				prefStore.setValue(Msg.TIMEOUT_RADIOGROUP_NAME_LABEL, CommandOptions.IS_TIMEOUT);
			} else {
				prefStore.setValue(Msg.TIMEOUT_RADIOGROUP_NAME_LABEL, CommandOptions.IS_MANUAL_TERMINATE);
			}
			timoutSecEditor.setEnabled(isTimeout, groupTimeoutEditor.getMemberFieldEditorParent());
			prefStore.setValue(CommandOptions.IS_DEACTIVATE_ON_EXIT, cmdStore.isDeactivateOnExit());
			prefStore.setValue(CommandOptions.IS_DEACTIVATE_ON_TERMINATE, cmdStore.isDeactivateOnTerminate());
			prefStore.setValue(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER,
					cmdStore.isUpdateDefaultOutPutFolder());
			prefStore.setValue(CommandOptions.IS_UPDATE_ON_BUILD, cmdStore.isUpdateOnBuild());
			prefStore.setValue(CommandOptions.IS_ACTIVATE_ON_COMPILE_ERROR, cmdStore.isActivateOnCompileError());
			prefStore.setValue(CommandOptions.IS_REFRESH_ON_UPDATE, cmdStore.isRefreshOnUpdate());
			prefStore.setValue(CommandOptions.IS_EAGER_ON_ACTIVATE, cmdStore.isEagerOnActivate());
			prefStore.setValue(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS,
					cmdStore.isAutoHandleExternalCommands());
			prefStore.setValue(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS, cmdStore.isAllowUIContributions());
			prefStore.setValue(CommandOptions.IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION, cmdStore.isSaveFilesBeforeBundleOperation());
			prefStore.setValue(CommandOptions.IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION, cmdStore.isSaveSnapshotBeforeBundleOperation());
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, Msg.INIT_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		}
	}

	/**
	 * Disable timeout seconds editor if timeout is disabled
	 */
	protected void performDefaults() {

		super.performDefaults();
		try {
			CommandOptions cmdStore = getPrefService();
			Boolean isTimeout = cmdStore.getDefaultIsTimeOut();
			timoutSecEditor.setEnabled(isTimeout, groupTimeoutEditor.getMemberFieldEditorParent());
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, Msg.DEFAULT_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		}
	}

	/**
	 * Save all editor values to options store and the standard preference store when "ok" or "apply" is pressed
	 */
	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		storeOptions();
		return ok;
	}

	/**
	 * Save all editor values to options store when "ok" or "apply" is pressed
	 */
	private void storeOptions() {

		try {
			CommandOptions cmdStore = getPrefService();
			IPreferenceStore prefStore = getPreferenceStore();
			cmdStore.setTimeOut(prefStore.getInt(CommandOptions.TIMEOUT_SECONDS));
			String rgv = prefStore.getString(Msg.TIMEOUT_RADIOGROUP_NAME_LABEL);
			if (rgv.equals(CommandOptions.IS_TIMEOUT)) {
				cmdStore.setIsTimeOut(true);
				cmdStore.setIsManualTerminate(false);
			} else {
				cmdStore.setIsTimeOut(false);
				cmdStore.setIsManualTerminate(true);
			}
			cmdStore.setIsDeactivateOnExit(prefStore.getBoolean(CommandOptions.IS_DEACTIVATE_ON_EXIT));
			cmdStore.setIsDeactivateOnTerminate(prefStore.getBoolean(CommandOptions.IS_DEACTIVATE_ON_TERMINATE));
			cmdStore.setIsUpdateDefaultOutPutFolder(prefStore
					.getBoolean(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER));
			cmdStore.setIsUpdateOnBuild(prefStore.getBoolean(CommandOptions.IS_UPDATE_ON_BUILD));
			cmdStore.setIsActivateOnCompileError(prefStore.getBoolean(CommandOptions.IS_ACTIVATE_ON_COMPILE_ERROR));
			cmdStore.setIsRefreshOnUpdate(prefStore.getBoolean(CommandOptions.IS_REFRESH_ON_UPDATE));
			cmdStore.setIsEagerOnActivate(prefStore.getBoolean(CommandOptions.IS_EAGER_ON_ACTIVATE));
			cmdStore.setIsAutoHandleExternalCommands(prefStore
					.getBoolean(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS));
			cmdStore.setIsAllowUIContributions(prefStore.getBoolean(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS));
			cmdStore.setIsSaveFilesBeforeBundleOperation(prefStore.getBoolean(CommandOptions.IS_SAVE_FILES_BEFORE_BUNDLE_OPERATION));
			cmdStore.setIsSaveSnapshotBeforeBundleOperation(prefStore.getBoolean(CommandOptions.IS_SAVE_SNAPSHOT_BEFORE_BUNDLE_OPERATION));
			cmdStore.flush();
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, Msg.SAVE_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		} catch (BackingStoreException bse) {
			StatusManager.getManager().handle(
					new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, Msg.PREFERENCE_FLUSH_EXCEPTION, bse),
					StatusManager.LOG);
		}
	}

	/**
	 * Disable timeout seconds editor if timeout is disabled
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {

		if (((FieldEditor) event.getSource()).getPreferenceName().equals(Msg.TIMEOUT_RADIOGROUP_NAME_LABEL)) {
			try {
				RadioGroupFieldEditor rge = (RadioGroupFieldEditor) event.getSource();
				timoutSecEditor.setEnabled(getStopOperationValue(rge, CommandOptions.IS_TIMEOUT),
						groupTimeoutEditor.getMemberFieldEditorParent());
			} catch (Exception ex) {
				StatusManager.getManager().handle(
						new BundleStatus(StatusCode.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex),
						StatusManager.LOG);
			}
		}
	}

	/**
	 * Find and return the state of the button specified by the command option id in the specified radio group. The
	 * command option id should be a member in the specified radio group editor.
	 * 
	 * @param rge the radio group editor
	 * @param commandOptionId identifier representing a button in the radio group
	 * @return state of the identified radio button
	 * @throws Exception if the command id not identifies a radio button in the group
	 */
	private boolean getStopOperationValue(RadioGroupFieldEditor rge, String commandOptionId) throws Exception {

		Composite rgc = rge.getRadioBoxControl(groupTimeoutEditor.getMemberFieldEditorParent());
		Control[] radioControls = rgc.getChildren();
		for (int i = 0; i < radioControls.length; i++) {
			Control radio = radioControls[i];
			if (radio instanceof Button) {
				Button btn = (Button) radio;
				if (btn.getData().equals(commandOptionId)) {
					return btn.getSelection();
				}
			}
		}
		throw new Exception(Msg.STOP_OPERATION_OPTON_SELECTION_EXCEPTION);
	}

	@SuppressWarnings("unused")
	private boolean setStopOperationValue(RadioGroupFieldEditor rge, String commandOption, Boolean value)
			throws Exception {

		Composite rgc = rge.getRadioBoxControl(groupTimeoutEditor.getMemberFieldEditorParent());
		Control[] radioControls = rgc.getChildren();
		for (int i = 0; i < radioControls.length; i++) {
			Control radio = radioControls[i];
			if (radio instanceof Button) {
				Button btn = (Button) radio;
				if (btn.getData().equals(commandOption)) {
					btn.setSelection(value);
				}
			}
		}
		throw new Exception(Msg.STOP_OPERATION_OPTON_SELECTION_EXCEPTION);
	}

}