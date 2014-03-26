package no.javatime.inplace.pl.preferences.page;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import no.javatime.inplace.dl.preferences.intface.CommandOptions;
import no.javatime.inplace.pl.preferences.PreferencePlActivator;
import no.javatime.inplace.pl.preferences.msg.Msg;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Standard preference page of command and manifest bundle options
 * Maintains preferences stored in the OSGi preference store with a copy in the the default plug-in preference store for
 * preference pages.
 * <p>
 * On initialization of the preference page values are loaded from OSGi preferences and stored in the plug-in
 * preferences before they are displayed.
 * <p>
 * On ok and apply values are first stored in the plug-in preference store before stored in the OSGi preference store.
 * <p>
 * Other bundles relies on the shared data in the OSGi preference store.
 */

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Maintain own list of field editors to be used in overriding methods to traverse field editors.
	 * <p>
	 * Field editors that do not represent a data type (e.g. space, separator and group field editors) are ignored 
	 * in own list if added.
	 */
	private List<FieldEditor> fields = new LinkedList<FieldEditor>();

	// Index of the IsTimout field editor in the editor fields list
	int timeOutSecIndex = 0;

	// The group field editor for timeout settings
	GroupFieldEditor groupTimeoutEditor;

	/**
	 * Sets the grid layout and initializes the preference page with the standard preference store 
	 */
	public PreferencePage() {
		super(GRID);
		setPreferenceStore(PreferencePlActivator.getDefault().getPreferenceStore());
		setDescription("Bundle Options");
	}

	/**
	 * Get the command and manifest options from OSGi preferences store
	 * @return the command and manifest options
	 * @throws IllegalStateException when the options service is unavailable
	 */
	public CommandOptions getPrefService() throws IllegalStateException {
		return PreferencePlActivator.getDefault().getOptionsService(); // tracker	
		// return OptionsService.getCommandOptions(); // DS
	}

	/**
	 * Create group and field editors
	 */
	public void createFieldEditors() {

		// Timeout preferences group
		groupTimeoutEditor = new GroupFieldEditor(Msg.TIMEOUT_GROUP_LABEL, getFieldEditorParent());
		addField(groupTimeoutEditor);

		// Enable/Disable timeout in start and stop methods
		// Problems getting the field editor property listener to work. Using listener at class level
		BooleanFieldEditor booleanEditor = new BooleanFieldEditor(CommandOptions.IS_TIMEOUT, Msg.IS_TIMEOUT_LABEL,
				groupTimeoutEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupTimeoutEditor.add(booleanEditor);

		// If timeout is enabled specify the duration in seconds before timeout
		IntegerFieldEditor intEditor = new IntegerFieldEditor(CommandOptions.TIMEOUT_SECONDS, Msg.TIMEOUT_SECONDS_LABEL,
				groupTimeoutEditor.getMemberFieldEditorParent());
		intEditor.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
		intEditor.setValidRange(1, 60);
		addField(intEditor);
		groupTimeoutEditor.add(intEditor);
		timeOutSecIndex = fields.lastIndexOf(intEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		// addField(new SeparatorFieldEditor(getFieldEditorParent()));

		// OSGi Command preferences group
		GroupFieldEditor groupCmdEditor = new GroupFieldEditor(Msg.COMMAND_GROUP_LABEL, getFieldEditorParent());
		addField(groupCmdEditor);

		// Enable/Disable to run update job after build
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_UPDATE_ON_BUILD, Msg.IS_UPDATE_ON_BUILD_LABEL,
				groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable to run refresh after update
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_REFRESH_ON_UPDATE, Msg.IS_REFRESH_ON_UPDATE_LABEL,
				groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable deactivation of the workspace at shut down
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_DEACTIVATE_ON_EXIT, Msg.IS_DEACTIVATE_ON_EXIT_LABEL,
				groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		// Enable/Disable to handle external bundle automatically
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS,
				Msg.IS_AUTO_HANDLE_EXTERNAL_COMMANDS_LABEL, groupCmdEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupCmdEditor.add(booleanEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		// Manifest preferences group
		GroupFieldEditor groupManifestEditor = new GroupFieldEditor(Msg.MANIFEST_GROUP_LABEL, getFieldEditorParent());
		addField(groupManifestEditor);

		// Enable/Disable to update default output folder on Activation/Deactivation
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER,
				Msg.IS_UPDATE_DEFAULT_OUTPUT_FOLDER_LABEL, groupManifestEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupManifestEditor.add(booleanEditor);

		// Enable/Disable to set eager activation policy on bundles when activated
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_EAGER_ON_ACTIVATE, Msg.IS_EAGER_ON_ACTIVATE_LABEL,
				groupManifestEditor.getMemberFieldEditorParent());
		addField(booleanEditor);
		groupManifestEditor.add(booleanEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		// Enable/Disable to include bundles contributing to the UI using extension
		LabelFieldEditor labelEditor = new LabelFieldEditor(Msg.ALLOW_EXTIONS_TEXT_LABEL, getFieldEditorParent());
		addField(labelEditor);
		booleanEditor = new BooleanFieldEditor(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS, Msg.IS_ALLOW_UI_CONTRIBUTIONS_LABEL,
				getFieldEditorParent());
		addField(booleanEditor);
	}
	
	/**
	 * Adds the field editor to the preference page list of field editors and to own maintained list of editors
	 */
	@Override
	protected void addField(FieldEditor editor) {
		super.addField(editor);
		// Skip known editors that not represent a data type
		// Anyway, if added they will be skipped when traversing the fields list
		if (!(editor instanceof SpacerFieldEditor) && !(editor instanceof SeparatorFieldEditor)
				&& (!(editor instanceof GroupFieldEditor) && (!(editor instanceof LabelFieldEditor)))) {
			fields.add(editor);
		}
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
		initializeValues();
		super.initialize();
	}

	/**
	 * Set values for all editors when preference page is opened
	 */
	private void initializeValues() {

		try {
			CommandOptions cmdStore = getPrefService();			
			IPreferenceStore prefStore = getPreferenceStore();

			if (fields != null) {
				Iterator<FieldEditor> e = fields.iterator();
				while (e.hasNext()) {
					FieldEditor fe = (FieldEditor) e.next();
					if (fe.getPreferenceName().equals(CommandOptions.TIMEOUT_SECONDS)) {
						prefStore.setValue(CommandOptions.TIMEOUT_SECONDS, cmdStore.getTimeout());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_TIMEOUT)) {
						Boolean isTimeout = cmdStore.isTimeOut();
						prefStore.setValue(CommandOptions.IS_TIMEOUT, isTimeout);
						setEnabledOnTimeoutSeconds(isTimeout.toString());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_DEACTIVATE_ON_EXIT)) {
						prefStore.setValue(CommandOptions.IS_DEACTIVATE_ON_EXIT, cmdStore.isDeactivateOnExit());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER)) {
						prefStore.setValue(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER, cmdStore.isUpdateDefaultOutPutFolder());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_UPDATE_ON_BUILD)) {
						prefStore.setValue(CommandOptions.IS_UPDATE_ON_BUILD, cmdStore.isUpdateOnBuild());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_REFRESH_ON_UPDATE)) {
						prefStore.setValue(CommandOptions.IS_REFRESH_ON_UPDATE, cmdStore.isRefreshOnUpdate());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_EAGER_ON_ACTIVATE)) {
						prefStore.setValue(CommandOptions.IS_EAGER_ON_ACTIVATE, cmdStore.isEagerOnActivate());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS)) {
						prefStore.setValue(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS, cmdStore.isAutoHandleExternalCommands());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS)) {
						prefStore.setValue(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS, cmdStore.isAllowUIContributions());
					}
				}
			}
			super.initialize();
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, PreferencePlActivator.PLUGIN_ID, Msg.INIT_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		}
	}
	
	/**
	 * Disable timeout seconds editor if timeout is disabled
	 */
	protected void performDefaults() {

		super.performDefaults();
		try {
			CommandOptions  cmdStore = getPrefService();			
			Boolean timeout = cmdStore.getDefaultIsTimeOut();
			setEnabledOnTimeoutSeconds(timeout.toString());
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, PreferencePlActivator.PLUGIN_ID, Msg.DEFAULT_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		}
	}

	/**
	 * Save all editor values to options store and the standard preference store when "ok" or "apply" is pressed
	 */
	@Override
	public boolean performOk() {
		storeValues();
		return super.performOk();
	}

	/**
	 * Save all editor values to options store when "ok" or "apply" is pressed
	 */
	private void storeValues() {

		try {
			CommandOptions  cmdStore = getPrefService();			
			if (fields != null) {
				Iterator<FieldEditor> e = fields.iterator();
				while (e.hasNext()) {
					FieldEditor fe = (FieldEditor) e.next();
					if (fe.getPreferenceName().equals(CommandOptions.TIMEOUT_SECONDS)) {
						cmdStore.setTimeOut(((IntegerFieldEditor) fe).getIntValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_TIMEOUT)) {
						cmdStore.setIsTimeOut(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_DEACTIVATE_ON_EXIT)) {
						cmdStore.setIsDeactivateOnExit(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_UPDATE_DEFAULT_OUTPUT_FOLDER)) {
						cmdStore.setIsUpdateDefaultOutPutFolder(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_UPDATE_ON_BUILD)) {
						cmdStore.setIsUpdateOnBuild(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_REFRESH_ON_UPDATE)) {
						cmdStore.setIsRefreshOnUpdate(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_EAGER_ON_ACTIVATE)) {
						cmdStore.setIsEagerOnActivate(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_AUTO_HANDLE_EXTERNAL_COMMANDS)) {
						cmdStore.setIsAutoHandleExternalCommands(((BooleanFieldEditor) fe).getBooleanValue());
					} else if (fe.getPreferenceName().equals(CommandOptions.IS_ALLOW_UI_CONTRIBUTIONS)) {
						cmdStore.setIsAllowUIContributions(((BooleanFieldEditor) fe).getBooleanValue());
					}
				}
				cmdStore.flush();
			}
		} catch (IllegalStateException e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, PreferencePlActivator.PLUGIN_ID, Msg.SAVE_PREF_PAGE_ERROR, e),
					StatusManager.LOG);
		} catch (BackingStoreException bse) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, PreferencePlActivator.PLUGIN_ID, Msg.PREFERENCE_FLUSH_EXCEPTION, bse),
					StatusManager.LOG);
		}
	}

	/**
	 * Disable timeout seconds editor if timeout is disabled
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {

		if (((FieldEditor) event.getSource()).getPreferenceName().equals(CommandOptions.IS_TIMEOUT)) {
			setEnabledOnTimeoutSeconds(event.getNewValue().toString());
		}
	}
	/**
	 * Disable the editor to specify timeout in seconds if the timeout is disabled 
	 * @param isTimeOut value "true" if timeout functionality is enabled, otherwise value "false" 
	 */
	void setEnabledOnTimeoutSeconds(String isTimeOut) {
		if (timeOutSecIndex >= 0) {
			FieldEditor timeoutEditor = fields.get(timeOutSecIndex);
			timeoutEditor.setEnabled(Boolean.parseBoolean(isTimeOut), groupTimeoutEditor.getMemberFieldEditorParent());
		}
	}

}