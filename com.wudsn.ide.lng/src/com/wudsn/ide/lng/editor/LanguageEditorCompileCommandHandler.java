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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.wudsn.ide.lng.compiler.CompilerFiles;

/**
 * Event handler for the "Compile" command.
 * 
 * @author Peter Dell
 */
public final class LanguageEditorCompileCommandHandler extends LanguageEditorFilesCommandHandler {

	/**
	 * Creates a new instance. Called by the extension point
	 * "org.eclipse.ui.handlers".
	 */
	public LanguageEditorCompileCommandHandler() {

	}

	@Override
	protected void execute(ExecutionEvent event, ILanguageEditor languageEditor, CompilerFiles files)
			throws ExecutionException {
		if (event == null) {
			throw new IllegalArgumentException("Parameter 'event' must not be null.");
		}
		if (languageEditor == null) {
			throw new IllegalArgumentException("Parameter 'languageEditor' must not be null.");
		}
		if (files == null) {
			throw new IllegalArgumentException("Parameter 'files' must not be null.");
		}
		try {
			LanguageEditorCompileCommand.execute(languageEditor, files, event.getCommand().getId(), null);
		} catch (RuntimeException ex) {
			throw new ExecutionException("Cannot execute event " + event, ex);
		}
	}
}