package no.javatime.inplace.pl.preferences.page;

import java.util.ArrayList;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Class is intended to create a Group Widgets, inside of the {@link FieldEditorPreferencePage} objects.
 * 
 * @author alf
 * http://sourceforge.net/p/texlipse/net.sourceforge.texlipse/ci/f4c4fbbb09afc24292786c84d7718f0e90ba341f/tree/source/net/sourceforge/texlipse/properties/TexEditorPreferencePage.java#l93
 * http://www.eclipse.org/forums/index.php/t/5159/
 */
public class GroupFieldEditor extends FieldEditor {
	private String name;
	private ArrayList<FieldEditor> members = new ArrayList<FieldEditor>();
	private int numcolumns;
	private Group group;
	private Composite parent;
	Composite fieldEditorParent;
	private boolean initialized = false;

	private static final int GROUP_PADDING = 5;
	private static final int VERTICAL_SPACE = 5;

	/**
	 * Creates a Group of {@link FieldEditor} objects
	 * 
	 * @param name - name
	 * @param groupFieldEditorParent - parent
	 */
	public GroupFieldEditor(String name, Composite groupFieldEditorParent) {

		this.name = name;
		this.parent = groupFieldEditorParent;


//	this.group = new Group(parent, SWT.DEFAULT);
//	this.group.setText(this.name);
//
		this.group = new Group(groupFieldEditorParent, SWT.NONE);
		this.group.setText(this.name);

//		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
//		data.horizontalIndent = 2;
//		data.verticalIndent = VERTICAL_SPACE;
//		this.group.setLayoutData(data);
//
		GridData gridDataGrabX = new GridData(GridData.FILL, GridData.FILL, true, false);		
		this.group.setLayoutData(gridDataGrabX);

//		GridLayout groupLayout = new GridLayout();
//		groupLayout.marginLeft = GROUP_PADDING;
//		groupLayout.marginRight = GROUP_PADDING;
//		groupLayout.marginTop = GROUP_PADDING;
//		groupLayout.marginBottom = GROUP_PADDING;
//		this.group.setLayout(groupLayout);
//
		// TODO Generalize (with additional constructors taking grid layout and grid data)
		GridLayout gridLayoutSingleColumn = new GridLayout(1, false);
		this.group.setLayout(gridLayoutSingleColumn);

		fieldEditorParent = new Composite(group, SWT.NONE);
		GridData gridDataGrabXGrabY = new GridData(GridData.FILL, GridData.FILL, true, true);
		fieldEditorParent.setLayout(gridLayoutSingleColumn);
		fieldEditorParent.setLayoutData(gridDataGrabXGrabY);

	}

	/**
	 * Encapsulate group members to own components.
	 * This is necessary to get the layout right.
	 * @param group field editor group
	 * @return parent component for a field editor
	 * Get the parent composite of the field editors used by members of this group
	 * <p>
	 * Note that this group editor only support the grid style (not the flat style)
	 * 
	 * @return composite of group member field editors
	 */
	public Composite getMemberFieldEditorParent() {
		// Need to recalculate max number of columns and create new parent composite
		// to support the flat style
		return fieldEditorParent;
	} 

	/**
	 * The parent group for all the FieldEditors that are of members this Group.
	 * 
	 * @return - the parent group for field editor members
	 */
	public Group getFieldEditorGroup() {
		return group;
	}

	/**
	 * Only works when init() hasn't been called yet.
	 */
	public void add(FieldEditor fieldEditor) {
		if (initialized)
			throw new RuntimeException(
					"The GroupFieldEditor is initialized already, new field editors can't be added anymore after that. ");

		members.add(fieldEditor);
	}

	/**
	 * Initializes using the currently added field editors.
	 */
	public void init() {
		if (!initialized) {
			doFillIntoGrid(getFieldEditorGroup(), numcolumns);
			initialized = true;
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		this.numcolumns = numColumns;
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
	protected void doFillIntoGrid(Composite parentParam, int numColumns) {
		if (members != null) {
			int maxControlls = 1;
			for (FieldEditor editor : members) {
				maxControlls = Math.max(maxControlls, editor.getNumberOfControls());
			}
			for (FieldEditor editor : members) {
				editor.fillIntoGrid(parent, maxControlls);
			}
		}

		this.parent.layout();
		this.parent.redraw();
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor. Loads the value from the preference store and sets it to the check
	 * box.
	 */
	@Override
	protected void doLoad() {
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.load();
			}
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor. Loads the default value from the preference store and sets it to the
	 * check box.
	 */
	@Override
	protected void doLoadDefault() {
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.loadDefault();
			}
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
	protected void doStore() {
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.store();
			}
		}
	}

	@Override
	public void store() {
		doStore();
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
	public int getNumberOfControls() {
		return 1;
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
	public void setFocus() {
		if (members != null && !members.isEmpty()) {
			members.iterator().next().setFocus();
		}
	}

	/*
	 * @see FieldEditor.setEnabled
	 */
	@Override
	public void setEnabled(boolean enabled, Composite parentParam) {
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.setEnabled(enabled, parentParam);
			}
		}
	}

	@Override
	public void setPreferenceStore(IPreferenceStore store) {
		super.setPreferenceStore(store);
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.setPreferenceStore(store);
			}
		}
	}

	@Override
	public void setPage(DialogPage dialogPage) {
		if (members != null) {
			for (FieldEditor editor : members) {
				editor.setPage(dialogPage);
			}
		}
	}

	@Override
	public boolean isValid() {
		if (members != null) {
			for (FieldEditor editor : members) {
				if (!editor.isValid()) {
					return false;
				}
			}
		}
		return true;
	}
}