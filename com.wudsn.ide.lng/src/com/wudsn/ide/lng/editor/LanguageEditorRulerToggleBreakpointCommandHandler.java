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
import org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Event handler for the ruler context menu "Toggle Breakpoint" command. Unlike
 * {@link LanguageEditorToggleBreakpointCommandHandler} (used for the text editor context menu and the
 * keyboard shortcut, both of which act on the current caret line), this handler drives
 * {@link RulerToggleBreakpointActionDelegate} directly, which determines the target line from the ruler's
 * last mouse position, so toggling a breakpoint from the ruler context menu affects the line that was
 * actually clicked, regardless of where the caret currently is.
 *
 * @author Peter Dell
 */
public final class LanguageEditorRulerToggleBreakpointCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor == null) {
			return null;
		}

		IEditorActionDelegate delegate = new RulerToggleBreakpointActionDelegate();
		Action action = new Action() {
		};
		delegate.setActiveEditor(action, editor);
		delegate.run(action);
		return null;
	}
}
