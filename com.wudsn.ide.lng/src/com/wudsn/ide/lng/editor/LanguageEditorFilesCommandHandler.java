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

package com.wudsn.ide.lng.editor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.wudsn.ide.lng.LanguagePlugin;
import com.wudsn.ide.lng.compiler.CompilerFiles;

/**
 * Base class for commands which operate on the current file of an language
 * editor, in case the file is within the work space. The base class ensures
 * that the corresponding command is disabled, if there is no active language
 * editor or the editor contains a file from outside of the work space.
 * 
 * @author Peter Dell
 */
public abstract class LanguageEditorFilesCommandHandler extends AbstractHandler {

	public LanguageEditorFilesCommandHandler() {
		super();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var editor = HandlerUtil.getActiveEditorChecked(event);
		if (!(editor instanceof ILanguageEditor)) {
			if (LanguageEditorPropertyTester.isPascalEditor(editor)) {

				editor = new LanguageEditorWrapper((ITextEditor) editor,
						"com.wudsn.ide.lng.pas.compiler.mp.MadPascalEditor");
			} else {
				return null;
			}

		}

		var languageEditor = (ILanguageEditor) editor;
		var files = LanguageEditorFilesLogic.createInstance(languageEditor).createCompilerFiles();

		if (files != null) {
			execute(event, languageEditor, files);
		} else {
			try {
				LanguagePlugin.getInstance().showError(languageEditor.getSite().getShell(),
						"Operation '" + event.getCommand().getName()
								+ "' is not possible because the file in the editor is not located in the workspace.",
						new Exception("Cannot resolve compiler files of " + languageEditor.getEditorInput()));
			} catch (NotDefinedException ignore) {
				// Ignore
			}

		}
		return null;

	}

	/**
	 * Perform the action on the current editor and file.
	 * 
	 * @param event          The event, not <code>null</code>.
	 * @param languageEditor The language editor, not <code>null</code> and with
	 *                       current files which are not <code>null</code>.
	 * @param files          The current compiler files of the editor, not
	 *                       <code>null</code> .
	 * @throws ExecutionException if an exception occurred during execution.
	 */
	protected abstract void execute(ExecutionEvent event, ILanguageEditor languageEditor, CompilerFiles files)
			throws ExecutionException;

}