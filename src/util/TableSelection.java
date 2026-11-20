package util;
import javax.swing.JTable;
public final class TableSelection {
    private TableSelection(){}
    public static int modelRow(JTable table){int row=table.getSelectedRow();return row<0?-1:table.convertRowIndexToModel(row);}
}
