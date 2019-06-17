package component;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.util.Vector;

public class SnippetJTable extends JTable {
    private static final String uiClassID = "SnippetTableUI";

    public SnippetJTable() {
    }

    public SnippetJTable(TableModel dm) {
        super(dm);
    }

    public SnippetJTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public SnippetJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    public SnippetJTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    public SnippetJTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }

    public SnippetJTable(@NotNull Object[][] rowData, @NotNull Object[] columnNames) {
        super(rowData, columnNames);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

}
