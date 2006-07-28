/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.editor.GrammarDocument;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Assert;

public class FileProzessor {
	private Map documents;

	public static File getFileForName(Document parentDocument, String fileName) {
		Assert.isNotNull(fileName);
		fileName = StringUtils.trimQuotes(fileName);
		File file = null;
		String[] includeDirs = parentDocument.getOptions().getIncludeDirs();
		if (includeDirs != null) {
			for (int i = 0; i < includeDirs.length; i++) {
				String fullName = includeDirs[i] + "/" + fileName;
				file = new File(fullName);
				if (file.exists() && file.isFile())
					break;
			}
		}
		if (file == null || !file.exists())
			file = new File(fileName);
		return file;
	}

	public Document process(Document parentDocument, String fileName)
			throws Exception {
		Assert.isNotNull(parentDocument);
		File file = getFileForName(parentDocument, fileName);
		if (!file.exists() || !file.isFile())
			throw new Exception("File " + fileName + " could not be found.");

		if (GrammarEditorPlugin.DEBUG)
			System.out.println("Processing file: " + file.getAbsolutePath());
		String content = getFileContent(file);
		String path = file.getAbsolutePath();
		if (documents == null)
			documents = new HashMap();
		IDocument document = (IDocument) documents.get(path);
		if (document == null || !document.get().equals(content)
				|| document instanceof GrammarDocument && ((GrammarDocument) document).getParentDocument() != parentDocument)
			documents.put(path, document = new GrammarDocument(content, parentDocument));
		Document model = GrammarEditorPlugin.getDocumentModel(document, null, false);
		model.label = path;

		parentDocument.addInclude(model);
		return model;
	}

	private String getFileContent(File file) {
		char[] buf = new char[(int) file.length()];
		FileReader in = null;
		try {
			in = new FileReader(file);
			int count = in.read(buf);
			for (int c1 = 0; c1 != -1 && (count += c1) < buf.length; c1 = in.read(buf, count, buf.length - count)) ;
				
		} catch (IOException e) {
			GrammarEditorPlugin.logError("Cannot process included file: " + file, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}
		return new String(buf);
	}

}
