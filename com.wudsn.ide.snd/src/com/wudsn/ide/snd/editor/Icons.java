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

package com.wudsn.ide.snd.editor;

import org.eclipse.swt.graphics.Image;

import com.wudsn.ide.base.common.AbstractIDEPlugin;
import com.wudsn.ide.snd.SoundPlugin;

final class Icons {

	public static final Image PLAY;
	public static final Image PAUSE;
	public static final Image STOP;
	public static final Image EXPORT;

	static {
		AbstractIDEPlugin plugin = SoundPlugin.getInstance();
		PLAY = plugin.getImage("player-play-16x16.png");
		PAUSE = plugin.getImage("player-pause-16x16.png");
		STOP = plugin.getImage("player-stop-16x16.png");
		EXPORT = plugin.getImage("player-export-16x16.png");
	}
}
