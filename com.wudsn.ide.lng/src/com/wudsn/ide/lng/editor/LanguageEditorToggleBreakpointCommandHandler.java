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
 * Event handler for command "org.eclipse.debug.ui.commands.ToggleBreakpoint". That command has no handler
 * of its own in the Eclipse Platform: it used to be served solely by the "class" of the deprecated
 * "org.eclipse.ui.popupMenus" ruler action, which was implicitly wired to the command via its
 * "definitionId" attribute. Now that the ruler action has been migrated to "org.eclipse.ui.menus", an
 * explicit handler is required, or the command stops working everywhere it is used: the ruler context
 * menu, the text editor context menu and the keyboard shortcut.
 *
 * This handler drives {@link RulerToggleBreakpointActionDelegate} directly, which is the same class the
 * former ruler action used. It determines the target line from the ruler's last mouse position, falling
 * back to the caret line when the command is invoked from the text editor context menu or the keyboard
 * shortcut, i.e. without any prior ruler click.
 *
 * @author Peter Dell
 */
public final class LanguageEditorToggleBreakpointCommandHandler extends AbstractHandler {

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
