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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to open the definition of an identifier or include file via "F3".
 * 
 * @author Peter Dell
 * 
 * @since 1.7.0
 */
public final class LanguageEditorOpenDeclarationCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var editor = HandlerUtil.getActiveEditorChecked(event);
		if (!(editor instanceof ILanguageEditor)) {
			return null;
		}

		var languageEditor = (ILanguageEditor) editor;
		ITextSelection textSelection = (ITextSelection) languageEditor.getSite().getSelectionProvider().getSelection();
		if (textSelection != null) {
			var document = languageEditor.getDocument();
			var offset = textSelection.getOffset();
			List<LanguageHyperlink> hyperlinks = new ArrayList<LanguageHyperlink>();
			LanguageHyperlinkDetector.detectHyperlinks(languageEditor, document, offset, false, hyperlinks);
			if (!hyperlinks.isEmpty()) {
				LanguageHyperlink hyperlink = hyperlinks.get(0);
				hyperlink.open();
			}
		}
		return null;
	}

}
