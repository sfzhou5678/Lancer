package component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TextAreaEditor extends DefaultCellEditor {
    protected RTextScrollPane scrollpane;
    protected RSyntaxTextArea textarea;

    public TextAreaEditor() {
        super(new JCheckBox());
//        scrollpane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollpane = new RTextScrollPane();
        textarea = new RSyntaxTextArea();
        textarea.setEditable(false);
        textarea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
//        textarea.setCodeFoldingEnabled(true); // code folding failed in JTable

//        textarea.setLineWrap(true);
//        textarea.setWrapStyleWord(true);
//        textarea.setBorder(new TitledBorder("Title Border"));
        scrollpane.getViewport().add(textarea);

    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textarea.setText((String) value);

        return scrollpane;
    }

    public Object getCellEditorValue() {
        return textarea.getText();
    }
}