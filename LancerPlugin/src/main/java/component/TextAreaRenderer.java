package component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TextAreaRenderer extends JScrollPane implements TableCellRenderer {
    RSyntaxTextArea textarea;

    public TextAreaRenderer() {
        textarea = new RSyntaxTextArea();
        textarea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
//        textarea.setCodeFoldingEnabled(true);   // code folding failed in JTable

//        textarea.setLineWrap(true);
//        textarea.setWrapStyleWord(true);
//        textarea.setBorder(new TitledBorder("Title Border"));
        getViewport().add(textarea);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        String lineCode = (String) value;
        int lineCnt = 1;
        for (int i = 0; i < lineCode.length(); i++) {
            char c = lineCode.charAt(i);
            if (c == '\n')
                lineCnt++;
        }
        this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        try {
            int fontsize = 16;
            table.setRowHeight(row, fontsize * lineCnt + 10);
        } catch (Exception e) {
            table.setRowHeight(row, 150);
        }
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
            textarea.setForeground(table.getSelectionForeground());
            textarea.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
            textarea.setForeground(table.getForeground());
            textarea.setBackground(table.getBackground());
        }

        textarea.setText(lineCode);
        textarea.setCaretPosition(0);
        return this;
    }
}