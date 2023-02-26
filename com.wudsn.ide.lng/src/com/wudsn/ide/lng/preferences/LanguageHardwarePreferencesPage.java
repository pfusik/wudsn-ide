/**
 * Copyright (C) 2009 - 2021 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of WUDSN IDE.
 * 
 * WUDSN IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * WUDSN IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with WUDSN IDE.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wudsn.ide.lng.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.wudsn.ide.base.common.EnumUtility;
import com.wudsn.ide.base.common.ProcessWithLogs;
import com.wudsn.ide.base.common.StringUtility;
import com.wudsn.ide.base.common.TextUtility;
import com.wudsn.ide.base.gui.SWTFactory;
import com.wudsn.ide.base.hardware.Hardware;
import com.wudsn.ide.lng.Language;
import com.wudsn.ide.lng.LanguagePlugin;
import com.wudsn.ide.lng.LanguageUtility;
import com.wudsn.ide.lng.Texts;
import com.wudsn.ide.lng.compiler.CompilerDefinition;
import com.wudsn.ide.lng.compiler.CompilerOutputFolderMode;
import com.wudsn.ide.lng.editor.LanguageEditor;
import com.wudsn.ide.lng.runner.RunnerId;

/**
 * Visual editor page for the language preferences regarding compilers. There is
 * a separate page per {@link Hardware}. Subclasses only implement the
 * constructor.
 * 
 * @author Peter Dell
 */
public abstract class LanguageHardwarePreferencesPage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private static final class Tab {

		public final CompilerDefinition compilerDefinition;
		public final int tabIndex;
		public final TabItem tabItem;
		public final Control enabledControl;
		public final Control disabledControl;
		public final List<ControlDecoration> controlDecorations;
		public boolean initialized;
		public boolean enabled;

		public Tab(CompilerDefinition compilerDefinition, int tabIndex, TabItem tabItem, Control enabledControl,
				Control disabledControl, List<ControlDecoration> controlDecorations) {
			this.compilerDefinition = compilerDefinition;
			this.tabIndex = tabIndex;
			this.tabItem = tabItem;
			this.enabledControl = enabledControl;
			this.disabledControl = disabledControl;
			this.controlDecorations = controlDecorations;
			initialized = false;
			enabled = false;
		}
	}

	/**
	 * Local workaround class to react on changes of a radio group field editor. By
	 * default the selection listener is set to the containing page, so we need a
	 * second listener mechanism.
	 */
	private final class RadioGroupFieldEditorWithAction extends RadioGroupFieldEditor {

		private IPropertyChangeListener propertyChangeListener;

		public RadioGroupFieldEditorWithAction(String name, String labelText, int numColumns, String[][] labelAndValues,
				Composite parent) {
			super(name, labelText, numColumns, labelAndValues, parent);
		}

		@Override
		protected void fireValueChanged(String property, Object oldValue, Object newValue) {
			super.fireValueChanged(property, oldValue, newValue);
			if (propertyChangeListener != null) {
				propertyChangeListener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
			}

		}

		public void setAdditionalPropertyChangeListener(IPropertyChangeListener propertyChangeListener) {
			this.propertyChangeListener = propertyChangeListener;

		}
	}

	/**
	 * Property change listener to set the enabled state of the output folder path
	 * field based on the output folder mode field.
	 */
	private final class OutputFolderModeChangeListener implements IPropertyChangeListener {

		private Composite outputFolderPathFieldEditorParent;
		private StringFieldEditor outputFolderPathFieldEditor;

		public OutputFolderModeChangeListener(Composite outputFolderPathFieldEditorParent,
				StringFieldEditor outputFolderPathFieldEditor) {
			if (outputFolderPathFieldEditorParent == null) {
				throw new IllegalArgumentException("Parameter 'outputFolderPathFieldEditorParent' must not be null.");
			}
			if (outputFolderPathFieldEditor == null) {
				throw new IllegalArgumentException("Parameter 'outputFolderPathFieldEditor' must not be null.");
			}
			this.outputFolderPathFieldEditorParent = outputFolderPathFieldEditorParent;
			this.outputFolderPathFieldEditor = outputFolderPathFieldEditor;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event == null) {
				throw new IllegalArgumentException("Parameter 'event' must not be null.");
			}
			setOutputFolderMode((String) event.getNewValue());
		}

		public void setOutputFolderMode(String newValue) {
			var enabled = CompilerOutputFolderMode.FIXED_FOLDER.equals(newValue);
			outputFolderPathFieldEditor.setEnabled(enabled, outputFolderPathFieldEditorParent);
		}

	}

	/**
	 * The language.
	 */
	final Language language;

	/**
	 * The type of hardware used to filter the compilers and emulators.
	 */
	final Hardware hardware;

	/**
	 * The owning plugin.
	 */
	private final LanguagePlugin plugin;

	/**
	 * The tab folder and all visible tab items.
	 */
	private TabFolder tabFolder;
	private final Map<String, Tab> tabs;

	/**
	 * The id of the compiler and runner to be used as default.
	 */
	private String activeCompilerId;
	private String activeRunnerId;

	/**
	 * Creation is protected for sub-classes.
	 * 
	 * @param hardware The type of hardware used to filter the compilers and
	 *                 emulators, not <code>null</code>.
	 */
	protected LanguageHardwarePreferencesPage(Language language, Hardware hardware) {
		super(GRID);
		if (language == null) {
			throw new IllegalArgumentException("Parameter 'language' must not be null.");
		}
		if (hardware == null) {
			throw new IllegalArgumentException("Parameter 'hardware' must not be null.");
		}
		this.language = language;
		this.hardware = hardware;
		plugin = LanguagePlugin.getInstance();
		IPreferenceStore preferencesStore = plugin.getPreferenceStore();
		setPreferenceStore(preferencesStore);

		tabs = new TreeMap<String, Tab>();
	}

	@Override
	public final void init(IWorkbench workbench) {
		if (workbench == null) {
			throw new IllegalArgumentException("Parameter 'workbench' must not be null.");
		}
		var editor = workbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof LanguageEditor) {
			LanguageEditor languageEditor;
			languageEditor = (LanguageEditor) editor;
			activeCompilerId = languageEditor.getCompilerDefinition().getId();
			activeRunnerId = languageEditor.getLanguageHardwareCompilerPreferences().getRunnerId();
		} else {
			activeCompilerId = "";
			activeRunnerId = "";
		}
	}

	@Override
	public final void createFieldEditors() {

		var parent = getFieldEditorParent();
		var gridData = new GridData();
		gridData.verticalIndent = 0;
		gridData.horizontalIndent = 0;
		parent.setLayoutData(gridData);

		createCompilerFieldEditors(parent);
		setTabsStatus();
	}

	private void createCompilerFieldEditors(Composite parent) {
		if (parent == null) {
			throw new IllegalArgumentException("Parameter 'parent' must not be null.");
		}

		// Create the editors for all compilers of the hardware.
		var compilerRegistry = plugin.getCompilerRegistry();
		var compilerDefinitions = compilerRegistry.getCompilerDefinitions(language);

		tabFolder = new TabFolder(parent, SWT.FLAT);
		for (var compilerDefinition : compilerDefinitions) {

			createTabItem(tabFolder, compilerDefinition);
		}

		// Default to tab item for active compiler or to first.
		TabItem selectedTabItem = null;
		if (activeCompilerId != null) {
			var selectedTab = tabs.get(activeCompilerId);
			if (selectedTab != null) {
				selectedTabItem = selectedTab.tabItem;
			}
		}
		if (selectedTabItem == null && tabFolder.getItemCount() > 0) {
			selectedTabItem = tabFolder.getItem(0);
		}
		if (selectedTabItem != null) {
			tabFolder.setSelection(selectedTabItem);
		}

		// Make sure the control decorations are updated as required
		tabFolder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setTabsStatus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	private void createTabItem(TabFolder tabFolder, CompilerDefinition compilerDefinition) {
		if (tabFolder == null) {
			throw new IllegalArgumentException("Parameter 'tabFolder' must not be null.");
		}
		if (compilerDefinition == null) {
			throw new IllegalArgumentException("Parameter 'compilerDefinition' must not be null.");
		}

		var labelsAndValues = new String[][] {
				{ Texts.PREFERENCES_COMPILER_OUTPUT_FOLDER_MODE_SOURCE_FOLDER_TEXT,
						CompilerOutputFolderMode.SOURCE_FOLDER },
				{ Texts.PREFERENCES_COMPILER_OUTPUT_FOLDER_MODE_TEMP_FOLDER_TEXT,
						CompilerOutputFolderMode.TEMP_FOLDER },
				{ Texts.PREFERENCES_COMPILER_OUTPUT_FOLDER_MODE_FIXED_FOLDER_TEXT,
						CompilerOutputFolderMode.FIXED_FOLDER }

		};

		var compilerId = compilerDefinition.getId();
		var tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(compilerDefinition.getName());

		var tabContent = SWTFactory.createComposite(tabFolder, 1, 1, GridData.FILL_BOTH);

		List<ControlDecoration> controlDecorations;
		controlDecorations = new ArrayList<ControlDecoration>();

		// Field: target
		var composite = SWTFactory.createComposite(tabContent, 2, 3, GridData.FILL_HORIZONTAL);

		// Filtering of Target based on hardware is currently not implemented
		// because expansion boards like a W65816 board might be there/added
		// for a hardware.
		var targets = compilerDefinition.getSupportedTargets();
		String[][] entryNamesAndValues = new String[targets.size()][];
		int i = 0;
		for (var target : targets) {
			entryNamesAndValues[i] = new String[2];
			entryNamesAndValues[i][1] = target.name();
			entryNamesAndValues[i][0] = EnumUtility.getText(target);
			i++;
		}

		var comboFieldEditor = new ComboFieldEditor(
				LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerTargetName(language, hardware, compilerDefinition),
				Texts.PREFERENCES_COMPILER_TARGET_LABEL, entryNamesAndValues, composite);
		comboFieldEditor.setEnabled(entryNamesAndValues.length > 1, composite);
		addField(comboFieldEditor);

		// Field: defaultParameters
		composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);

		var label = new Label(composite, SWT.LEFT);
		label.setText(Texts.PREFERENCES_COMPILER_DEFAULT_PARAMETERS_LABEL);
		var textField = new Text(composite, SWT.SINGLE | SWT.BORDER);
		textField.setEditable(false);
		textField.setText(compilerDefinition.getDefaultParameters());
		var gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		textField.setLayoutData(gd);

		// Field: parameters
		composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);
		var name = LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerParametersName(language, hardware, compilerDefinition);
		var parametersFieldEditor = new StringFieldEditor(name, Texts.PREFERENCES_COMPILER_PARAMETERS_LABEL,
				tabContent);

		var compilerParametersHelp = Texts.PREFERENCES_COMPILER_PARAMETERS_HELP + "\n"
				+ Texts.PREFERENCES_COMPILER_PARAMETERS_VARIABLES;
		controlDecorations.add(createHelpDecoration(parametersFieldEditor, tabContent, compilerParametersHelp));

		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalIndent = 14;
		parametersFieldEditor.getTextControl(tabContent).setLayoutData(gd);

		addField(parametersFieldEditor);

		// Field: outputFolderMode
		composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);
		var outputFolderModeChoiceEditor = new RadioGroupFieldEditorWithAction(
				LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerOutputFolderModeName(language, hardware, compilerDefinition),
				Texts.PREFERENCES_COMPILER_OUTPUT_FOLDER_MODE_LABEL, 3, labelsAndValues, composite);
		addField(outputFolderModeChoiceEditor);

		// Field: outputFolderPath
		composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);
		var outputFolderPathFieldEditor = new DirectoryFieldDownloadEditor(
				LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerOutputFolderPathName(language, hardware, compilerDefinition),
				Texts.PREFERENCES_COMPILER_OUTPUT_FOLDER_PATH_LABEL, composite);
		addField(outputFolderPathFieldEditor);

		// Create a connection between the output mode field and the output
		// path field.
		var outputFolderModeChangeListener = new OutputFolderModeChangeListener(composite, outputFolderPathFieldEditor);
		// Set initial status based on current output folder mode.
		outputFolderModeChangeListener
				.setOutputFolderMode(getPreferenceStore().getString(outputFolderModeChoiceEditor.getPreferenceName()));
		// Register for changes.
		outputFolderModeChoiceEditor.setAdditionalPropertyChangeListener(outputFolderModeChangeListener);

		// Field: outputFileExtension
		composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);
		var outputFileExtensionFieldEditor = new StringFieldEditor(
				LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerOutputFileExtensionName(language, hardware, compilerDefinition),
				Texts.PREFERENCES_COMPILER_OUTPUT_FILE_EXTENSION_LABEL, composite);

		gd = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
		gd.widthHint = convertWidthInCharsToPixels(5);
		outputFileExtensionFieldEditor.getTextControl(composite).setLayoutData(gd);
		outputFileExtensionFieldEditor.getTextControl(composite);
		addField(outputFileExtensionFieldEditor);

		composite = SWTFactory.createComposite(tabContent, 1, 2, GridData.FILL_HORIZONTAL);

		var runnerRegistry = plugin.getRunnerRegistry();
		var runnerDefinitions = runnerRegistry.getDefinitions(hardware);
		entryNamesAndValues = new String[runnerDefinitions.size()][];
		i = 0;
		for (var runnerDefinition : runnerDefinitions) {
			entryNamesAndValues[i] = new String[2];
			entryNamesAndValues[i][1] = runnerDefinition.getId();
			entryNamesAndValues[i][0] = runnerDefinition.getName();
			i++;
		}
		comboFieldEditor = new ComboFieldEditor(
				LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerRunnerIdName(language, hardware, compilerDefinition),
				Texts.PREFERENCES_COMPILER_RUNNER_ID_LABEL, entryNamesAndValues, composite);
		addField(comboFieldEditor);
		createRunnerFieldEdiors(compilerDefinition, composite, controlDecorations);

		var disabledControl = SWTFactory.createComposite(tabFolder, 1, 1, GridData.FILL_BOTH);
		label = new Label(disabledControl, SWT.NONE);

		// ERROR: Path to {0} '{1}' executable is not set in the '{2}' preferences.
		var compilerPreferencesText = LanguageUtility.getCompilerPreferencesText(compilerDefinition.getLanguage());
		label.setText(TextUtility.format(Texts.MESSAGE_E100, compilerDefinition.getText(), compilerDefinition.getName(),
				compilerPreferencesText));
		var tab = new Tab(compilerDefinition, tabs.size(), tabItem, tabContent, disabledControl, controlDecorations);
		tabs.put(compilerId, tab);

	}

	void setTabsStatus() {
		for (Tab tab : tabs.values()) {
			setTabStatus(tab);

		}
	}

	private void setTabStatus(Tab tab) {
		if (tab == null) {
			throw new IllegalArgumentException("Parameter 'tab' must not be null.");
		}

		var languagePreferences = plugin.getLanguagePreferences(language);
		var enabled = StringUtility
				.isSpecified(languagePreferences.getCompilerExecutablePathOrDefault(tab.compilerDefinition));

		if (!tab.initialized || enabled != tab.enabled) {
			tab.initialized = true;
			tab.enabled = enabled;
			if (enabled) {
				tab.tabItem.setControl(tab.enabledControl);
			} else {
				tab.tabItem.setControl(tab.disabledControl);
			}
			tab.disabledControl.setVisible(!enabled);
			tab.enabledControl.setVisible(enabled);
		}
		var tabActive = tab.tabIndex == tabFolder.getSelectionIndex();
		for (var controlDecoration : tab.controlDecorations) {
			if (enabled && tabActive) {
				controlDecoration.show();
			} else {
				controlDecoration.hide();
			}
		}

	}

	private void createRunnerFieldEdiors(CompilerDefinition compilerDefinition, Composite parent,
			List<ControlDecoration> controlDecorations) {
		if (compilerDefinition == null) {
			throw new IllegalArgumentException("Parameter 'compilerDefinition' must not be null.");
		}
		if (parent == null) {
			throw new IllegalArgumentException("Parameter 'parent' must not be null.");
		}
		if (controlDecorations == null) {
			throw new IllegalArgumentException("Parameter 'controlDecorations' must not be null.");
		}

		var tabFolder = new TabFolder(parent, SWT.NONE);
		TabItem selectedTabItem = null;

		var gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		tabFolder.setLayoutData(gd);

		var runnerRegistry = plugin.getRunnerRegistry();
		var runnerDefinitions = runnerRegistry.getDefinitions(hardware);
		var runnerPaths = plugin.getRunnerPaths();

		var runnerCommandLineHelp = Texts.PREFERENCES_COMPILER_RUNNER_COMMAND_LINE_HELP + "\n"
				+ Texts.PREFERENCES_COMPILER_RUNNER_COMMAND_LINE_VARIABLES;

		for (var runnerDefinition : runnerDefinitions) {

			var runnerId = runnerDefinition.getId();

			if (runnerId.equals(RunnerId.DEFAULT_APPLICATION)) {
				continue;
			}

			var tabItem = new TabItem(tabFolder, SWT.NONE);
			tabItem.setText(runnerDefinition.getName());

			var tabContent = SWTFactory.createComposite(tabFolder, 2, 1, GridData.FILL_BOTH);

			var name = LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerRunnerExecutablePathName(language, hardware, compilerDefinition,
					runnerId);

			var composite = SWTFactory.createComposite(tabContent, 4, 2, GridData.FILL_HORIZONTAL);
			var fileFieldEditor = new FileFieldDownloadEditor(name,
					Texts.PREFERENCES_COMPILER_RUNNER_EXECUTABLE_PATH_LABEL, composite);
			var runnerPath = runnerPaths.getDefaultRunnerPath(hardware, runnerId);
			if (runnerPath != null) {
				var file = runnerPath.getAbsoluteFile();
				if (file != null) {
					fileFieldEditor.setFilterPath(file.getParentFile());
				}
			}
			fileFieldEditor.setFileExtensions(ProcessWithLogs.getExecutableExtensions());
			fileFieldEditor.setEnabled(runnerDefinition.isRunnerExecutablePathPossible(), composite);
			addField(fileFieldEditor);

			// Field: defaultEmulatorParameters
			composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);

			var label = new Label(composite, SWT.LEFT);
			label.setText(Texts.PREFERENCES_COMPILER_RUNNER_DEFAULT_COMMAND_LINE_LABEL);
			var textField = new Text(composite, SWT.SINGLE | SWT.BORDER);
			textField.setEditable(false);
			textField.setText(runnerDefinition.getDefaultCommandLine());
			gd = new GridData();
			gd.horizontalSpan = 2;
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
			textField.setLayoutData(gd);

			// Field: parameters
			composite = SWTFactory.createComposite(tabContent, 2, 2, GridData.FILL_HORIZONTAL);
			name = LanguageHardwareCompilerDefinitionPreferencesConstants.getCompilerRunnerCommandLineName(language, hardware, compilerDefinition,
					runnerId);
			var commandLineFieldEditor = new StringFieldEditor(name,
					Texts.PREFERENCES_COMPILER_RUNNER_COMMAND_LINE_LABEL, tabContent);

			gd = new GridData();
			gd.horizontalSpan = 1;
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalIndent = 14;
			commandLineFieldEditor.getTextControl(tabContent).setLayoutData(gd);
			addField(commandLineFieldEditor);

			controlDecorations.add(createHelpDecoration(commandLineFieldEditor, tabContent, runnerCommandLineHelp));
			var url = runnerDefinition.getHomePageURL();
			fileFieldEditor.setLinkURL(url);

			// Field: illegalOpcodesVisible
			composite = SWTFactory.createComposite(tabContent, 2, 3, GridData.FILL_HORIZONTAL);
			FieldEditor booleanFieldEditor = new BooleanFieldEditor(LanguageHardwareCompilerDefinitionPreferencesConstants
					.getCompilerRunnerWaitForCompletionName(language, hardware, compilerDefinition, runnerId),
					Texts.PREFERENCES_COMPILER_RUNNER_WAIT_FOR_COMPLETION_LABEL, composite);

			addField(booleanFieldEditor);

			tabItem.setControl(tabContent);

			if (runnerId.equals(activeRunnerId)) {
				selectedTabItem = tabItem;
			}

		}

		// Default to selected tab item.
		if (selectedTabItem != null) {
			tabFolder.setSelection(selectedTabItem);
		}
	}

	private ControlDecoration createHelpDecoration(StringFieldEditor parametersFieldEditor, Composite tabContent,
			String text) {
		if (parametersFieldEditor == null) {
			throw new IllegalArgumentException("Parameter 'parametersFieldEditor' must not be null.");
		}
		if (tabContent == null) {
			throw new IllegalArgumentException("Parameter 'tabContent' must not be null.");
		}
		if (text == null) {
			throw new IllegalArgumentException("Parameter 'text' must not be null.");
		}
		var textControl = parametersFieldEditor.getTextControl(tabContent);
		var controlDecoration = new ControlDecoration(textControl, SWT.LEFT | SWT.CENTER);
		controlDecoration.hide();
		controlDecoration.setShowHover(true);
		controlDecoration.setDescriptionText(text);

		controlDecoration.setImage(LanguagePlugin.getInstance().getImage("help-16x16.png"));

		return controlDecoration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean performOk() {
		if (super.performOk()) {
			saveChanges();
			return true;
		}
		return false;
	}

	/**
	 * The field editor preference page implementation of a
	 * <code>PreferencePage</code> method loads all the field editors with their
	 * default values except for the executable paths.
	 */
	@Override
	protected final void performDefaults() {

		super.performDefaults();

	}

	@Override
	public final void dispose() {
		super.dispose();

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			setTabsStatus();
		}

	}

	/**
	 * Saves all changes to the {@link IPreferenceStore}.
	 */
	private void saveChanges() {

		plugin.savePreferences();

	}
}
