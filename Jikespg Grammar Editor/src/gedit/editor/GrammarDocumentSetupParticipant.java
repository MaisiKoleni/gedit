/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

public class GrammarDocumentSetupParticipant implements IDocumentSetupParticipant {

	public static final String GRAMMAR_PARTITION = "___grammar_partitioning";

	public void setup(IDocument document) {
		setupDocument(document);
	}

	static IDocumentPartitioner createDocumentPartitioner() {
		return new FastPartitioner(new GrammarPartitionScanner(), new String[] {
				GrammarPartitionScanner.GRAMMAR_COMMENT,
				GrammarPartitionScanner.GRAMMAR_OPTION,
				GrammarPartitionScanner.GRAMMAR_MACRO,
				GrammarPartitionScanner.GRAMMAR_STRING,
				GrammarPartitionScanner.GRAMMAR_OPERATOR,
		});
	}

	public static void setupDocument(IDocument document) {
		IDocumentPartitioner partitioner = createDocumentPartitioner();
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			extension3.setDocumentPartitioner(GRAMMAR_PARTITION, partitioner);
		} else {
			document.setDocumentPartitioner(partitioner);
		}
		partitioner.connect(document);
	}
}
