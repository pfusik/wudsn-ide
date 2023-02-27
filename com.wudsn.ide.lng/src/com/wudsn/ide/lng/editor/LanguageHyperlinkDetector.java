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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;

import com.wudsn.ide.base.common.NumberUtility;
import com.wudsn.ide.base.common.TextUtility;
import com.wudsn.ide.gfx.editor.GraphicsConversionEditor;
import com.wudsn.ide.hex.HexEditor;
import com.wudsn.ide.lng.LanguageUtility;
import com.wudsn.ide.lng.Texts;
import com.wudsn.ide.lng.compiler.parser.CompilerSourceParser;
import com.wudsn.ide.lng.compiler.parser.CompilerSourceParserFileReference;
import com.wudsn.ide.lng.compiler.parser.CompilerSourceParserFileReferenceType;
import com.wudsn.ide.lng.compiler.parser.CompilerSourceParserTreeObject;
import com.wudsn.ide.lng.compiler.parser.CompilerSourceParserTreeObjectType;
import com.wudsn.ide.lng.compiler.syntax.CompilerSyntax;

/**
 * Hyperlink detector implementation for opening source or binary include files.
 * 
 * @author Peter Dell
 */
public final class LanguageHyperlinkDetector extends AbstractHyperlinkDetector {

	/**
	 * Hyperlink detector target as defined in the extension
	 * "org.eclipse.ui.workbench.texteditor.hyperlinkDetectorTargets".
	 */
	public static final String TARGET = "com.wudsn.ide.lng.editor.LanguageHyperlinkDetectorEditorTarget";

	/**
	 * Creates a new instance. Called by extension point
	 * "org.eclipse.ui.workbench.texteditor.hyperlinkDetectors".
	 * 
	 */
	public LanguageHyperlinkDetector() {
	}

	/**
	 * Tries to detect hyperlinks for the given region in the given text viewer and
	 * returns them.
	 * <p>
	 * In most of the cases only one hyperlink should be returned.
	 * </p>
	 * 
	 * @param textViewer                the text viewer on which the hover popup
	 *                                  should be shown
	 * @param region                    the text range in the text viewer which is
	 *                                  used to detect the hyperlinks
	 * @param canShowMultipleHyperlinks tells whether the caller is able to show
	 *                                  multiple links to the user. If
	 *                                  <code>true</code> {@link IHyperlink#open()}
	 *                                  should directly open the link and not show
	 *                                  any additional UI to select from a list. If
	 *                                  <code>false</code> this method should only
	 *                                  return one hyperlink which upon
	 *                                  {@link IHyperlink#open()} may allow to
	 *                                  select from a list.
	 * @return The hyperlinks or <code>null</code> if no hyperlink was detected
	 */
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		List<LanguageHyperlink> hyperlinks;
		hyperlinks = new ArrayList<LanguageHyperlink>(2);

		if (region == null || textViewer == null)
			return null;

		LanguageEditor languageEditor;

		languageEditor = getAdapter(LanguageEditor.class);
		if (languageEditor == null) {
			return null;
		}

		IDocument document = textViewer.getDocument();

		if (document == null)
			return null;

		int offset = region.getOffset();

		detectHyperlinks(languageEditor, document, offset, canShowMultipleHyperlinks, hyperlinks);

		if (!hyperlinks.isEmpty()) {
			return hyperlinks.toArray(new IHyperlink[hyperlinks.size()]);
		}
		return null;

	}

	final static void detectHyperlinks(ILanguageEditor languageEditor, IDocument document, int offset,
			boolean canShowMultipleHyperlinks, List<LanguageHyperlink> hyperlinks) {
		IRegion lineInfo;
		int lineNumber;
		String line;
		try {
			lineInfo = document.getLineInformationOfOffset(offset);
			lineNumber = document.getLineOfOffset(offset) + 1;
			line = document.get(lineInfo.getOffset(), lineInfo.getLength());
		} catch (BadLocationException ex) {
			throw new RuntimeException("Region with offset " + offset + " no valid", ex);
		}

		int offsetInLine = offset - lineInfo.getOffset();

		if (offsetInLine >= line.length()) {
			return;
		}
		detectInclude(languageEditor, lineInfo, lineNumber, line, offsetInLine, canShowMultipleHyperlinks, hyperlinks);
		if (hyperlinks.isEmpty()) {
			detectIdentifier(languageEditor, lineInfo, lineNumber, line, offsetInLine, canShowMultipleHyperlinks,
					hyperlinks);
		}
	}

	private static void detectInclude(ILanguageEditor languageEditor, IRegion lineInfo, int lineNumber, String line,
			int offsetInLine, boolean canShowMultipleHyperlinks, List<LanguageHyperlink> hyperlinks) {
		// Try to detect binary or source includes
		CompilerSourceParser compilerSourceParser = languageEditor.createCompilerSourceParser();
		CompilerSourceParserFileReference fileReference;
		fileReference = new CompilerSourceParserFileReference();
		compilerSourceParser.detectFileReference(line, fileReference);
		if (fileReference.getType() != CompilerSourceParserFileReferenceType.NONE) {

			CompilerSyntax syntax = compilerSourceParser.getCompilerSyntax();
			int startQuoteOffset = 0;
			String filePath = null;
			Iterator<String> i = syntax.getStringDelimiters().iterator();
			while (i.hasNext() && filePath == null) {
				String quote = i.next();
				startQuoteOffset = line.indexOf(quote, fileReference.getDirectiveEndOffset());
				if (startQuoteOffset == -1) {
					continue;
				}
				int endQuoteOffset = line.indexOf(quote, startQuoteOffset + 1);
				if (endQuoteOffset == -1) {
					continue;
				}

				if (startQuoteOffset < offsetInLine && offsetInLine < endQuoteOffset) {
					filePath = line.substring(startQuoteOffset + 1, endQuoteOffset);
				} else {
					continue;
				}
			}
			if (filePath == null) {
				return;
			}

			// Perform resolution of relative paths and compiler specific
			// default extension.
			var currentDirectory = languageEditor.getCurrentDirectory();
			String absoluteFilePath = compilerSourceParser.getIncludeAbsoluteFilePath(fileReference.getType(),
					currentDirectory, filePath);
			if (absoluteFilePath == null) {
				return;
			}

			var uri = new File(absoluteFilePath).toURI();

			IRegion linkRegion = new Region(lineInfo.getOffset() + startQuoteOffset + 1, filePath.length());

			IEditorSite site = languageEditor.getEditorSite();
			if (site == null) {
				return;
			}
			IWorkbenchPage workbenchPage = site.getWorkbenchWindow().getActivePage();

			switch (fileReference.getType()) {
			case CompilerSourceParserFileReferenceType.SOURCE:
				hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri,
						languageEditor.getClass().getName(), 0, TextUtility
								// INFO: Open with {0} Editor
								.format(Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_SOURCE_WITH_LANGUAGE_EDITOR,
										LanguageUtility.getText(languageEditor.getLanguage()))));
				break;
			case CompilerSourceParserFileReferenceType.BINARY:
				hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri, HexEditor.ID, 0,
						Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_BINARY_WITH_HEX_EDITOR));
				if (canShowMultipleHyperlinks) {
					hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri,
							GraphicsConversionEditor.ID, 0,
							Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_BINARY_WITH_GRAPHICS_EDITOR));
					hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri,
							LanguageHyperlink.DEFAULT_EDITOR, 0,
							Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_BINARY_WITH_DEFAULT_EDITOR));
					hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri,
							LanguageHyperlink.SYSTEM_EDITOR, 0,
							Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_BINARY_WITH_SYSTEM_EDITOR));
				}
				break;
			default:
				throw new IllegalStateException("Unknown include type " + fileReference.getType());

			}
		}
	}

	private static void detectIdentifier(ILanguageEditor languageEditor, IRegion lineInfo, int lineNumber, String line,
			int offsetInLine, boolean canShowMultipleHyperlinks, List<LanguageHyperlink> hyperlinks) {

		var compilerSyntax = languageEditor.createCompilerSourceParser().getCompilerSyntax();

		int startIdentifierOffset = offsetInLine;
		int endIdentifierOffset = offsetInLine;

		if (!compilerSyntax.isIdentifierCharacter(line.charAt(startIdentifierOffset))) {
			return;
		}

		while (startIdentifierOffset > 0
				&& compilerSyntax.isIdentifierCharacter(line.charAt(startIdentifierOffset - 1))) {
			startIdentifierOffset--;
		}
		while (endIdentifierOffset < line.length()
				&& compilerSyntax.isIdentifierCharacter(line.charAt(endIdentifierOffset))) {
			// If we find an identifier separator character when moving right,
			// we stop if we pass the cursor position.
			if (endIdentifierOffset > offsetInLine
					&& compilerSyntax.isIdentifierSeparatorCharacter(line.charAt(endIdentifierOffset))) {
				break;
			}
			endIdentifierOffset++;
		}
		String identifier = line.substring(startIdentifierOffset, endIdentifierOffset);
		// TODO: Hard cast to LanguageEditor
		var compilerSourceFile = ((LanguageEditor) languageEditor).getCompilerSourceFile();
		if (compilerSourceFile == null) {
			return;
		}

		List<CompilerSourceParserTreeObject> foundElements;
		foundElements = compilerSourceFile.getIdentifierDefinitionElements(identifier);
		if (foundElements.isEmpty()) {
			return;
		}

		IEditorSite site = languageEditor.getEditorSite();
		if (site == null) {
			return;
		}
		IWorkbenchPage workbenchPage = site.getWorkbenchWindow().getActivePage();

		int size;
		if (canShowMultipleHyperlinks) {
			size = foundElements.size();
		} else {
			size = 1;
		}
		for (int i = 0; i < size; i++) {
			CompilerSourceParserTreeObject element = foundElements.get(i);

			String absoluteFilePath;
			String fileName;
			File documentFile = element.getCompilerSourceFile().getDocumentFile();
			if (documentFile != null) {
				absoluteFilePath = documentFile.getPath();
				fileName = documentFile.getName();
			} else {
				absoluteFilePath = "";
				fileName = "";
			}
			int elementLineNumber;
			try {
				elementLineNumber = element.getCompilerSourceFile().getDocument()
						.getLineOfOffset(element.getStartOffset()) + 1;
			} catch (BadLocationException ex) {
				continue;
			}

			// Ignore if the found element is the start element only.
			File currentFile = languageEditor.getCurrentFile();
			boolean inSameFile = foundElements.size() == 1 && currentFile != null
					&& currentFile.getPath().equals(absoluteFilePath);

			if (inSameFile && lineNumber == elementLineNumber) {
				continue;
			}

			URI uri;
			uri = new File(absoluteFilePath).toURI();

			IRegion linkRegion = new Region(lineInfo.getOffset() + startIdentifierOffset, identifier.length());

			String hyperlinkText;
			if (inSameFile) {
				hyperlinkText = TextUtility.format(Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_IDENTIFIER,
						CompilerSourceParserTreeObjectType.getText(element.getType()), element.getCompoundName(),
						NumberUtility.getLongValueDecimalString(elementLineNumber));
			} else {
				hyperlinkText = TextUtility.format(Texts.COMPILER_HYPERLINK_DETECTOR_OPEN_IDENTIFIER_IN_INCLUDE,
						CompilerSourceParserTreeObjectType.getText(element.getType()), element.getCompoundName(),
						NumberUtility.getLongValueDecimalString(elementLineNumber), fileName);
			}
			hyperlinks.add(new LanguageHyperlink(linkRegion, workbenchPage, absoluteFilePath, uri,
					languageEditor.getClass().getName(), elementLineNumber, hyperlinkText));
		}
	}

}