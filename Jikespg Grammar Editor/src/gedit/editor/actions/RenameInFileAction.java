/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import gedit.GrammarEditorPlugin;
import gedit.editor.GrammarEditor;
import gedit.model.ModelBase;
import gedit.model.Node;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

public class RenameInFileAction extends Action {
	private GrammarEditor fEditor;
	
	public RenameInFileAction(GrammarEditor editor) {
		super("Rena&me in File");
		fEditor = editor;
		setDescription("Rename the element in the current file");
		setToolTipText("Rename the element in the current file");
	}
	
	public void run() {
		if (fEditor == null)
			return;
		ModelBase element = fEditor.getSelectedElement();
		if (element == null)
			return;
		
		ISourceViewer viewer = fEditor.getViewer();
		IDocument document = viewer.getDocument();
		int offset = ((ITextSelection) viewer.getSelectionProvider().getSelection()).getOffset();
		LinkedPositionGroup group = new LinkedPositionGroup();
		Node[] occurrences = fEditor.findOccurrences(element.getLabel(element));
	
		addPositionsToGroup(offset, occurrences, document, group);
		if (group.isEmpty()) {
		    return;         
        }
		try {
			LinkedModeModel model = new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();
			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
			ui.enter();
			viewer.setSelectedRange(offset, 0);
		} catch (BadLocationException e) {
			GrammarEditorPlugin.logError("Cannot rename in file", e);
		}
	}
	
    
    private void addPositionsToGroup(int offset, Node[] occurrences, IDocument document, LinkedPositionGroup group) {
		int i = 0;
		int j = 0;
        int firstPosition = -1;
        try {
        	for (int k = 0; k < occurrences.length; k++) {
                Node node = occurrences[k];
                if (firstPosition == -1) {
                    if (new Position(node.getOffset(), node.getLength()).overlapsWith(offset, 0)) {
                        firstPosition = i;
                        group.addPosition(new LinkedPosition(document, node.getOffset(), node.getLength(), j++));
                    }
                } else {
                    group.addPosition(new LinkedPosition(document, node.getOffset(), node.getLength(), j++));
                }
                i++;
            }
            
            for (i = 0; i < firstPosition; i++) {
                Node node = occurrences[i];
                group.addPosition(new LinkedPosition(document, node.getOffset(), node.getLength(), j++));
            }
        } catch (BadLocationException be) {
            GrammarEditorPlugin.logError("Cannot add the position", be);
        }
    }

	public void setEditor(GrammarEditor editor) {
		fEditor = editor;
	}
}