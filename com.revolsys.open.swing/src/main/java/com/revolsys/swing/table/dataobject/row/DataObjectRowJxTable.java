package com.revolsys.swing.table.dataobject.row;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXTable;

import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.swing.table.SortableTableCellHeaderRenderer;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectRowJxTable extends JXTable implements MouseListener {
  private static final long serialVersionUID = 1L;

  public DataObjectRowJxTable(final DataObjectRowTableModel model) {
    super(model);
    setModel(model);
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    setAutoCreateColumnsFromModel(false);

    final DataObjectMetaData metaData = model.getMetaData();

    final DataObjectRowTableCellRenderer cellRenderer = new DataObjectRowTableCellRenderer();

    final TableCellRenderer headerRenderer = new SortableTableCellHeaderRenderer();
    final JTableHeader tableHeader = getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setDefaultRenderer(headerRenderer);

    final List<TableColumn> removeColumns = new ArrayList<TableColumn>();
    final TableColumnModel columnModel = getColumnModel();
    for (int i = 0; i < model.getColumnCount(); i++) {
      final TableColumn column = columnModel.getColumn(i);
      final Class<?> attributeClass = metaData.getAttributeClass(i);
      if (Geometry.class.isAssignableFrom(attributeClass)) {
        removeColumns.add(column);
      } else {
        final String columnName = model.getColumnName(i) + "XX";

        final Attribute attribute = model.getColumnAttribute(i);
        final int attributeLength = Math.min(
          Math.max(columnName.length(), attribute.getMaxStringLength()), 40) + 2;
        final StringBuffer text = new StringBuffer(attributeLength);
        for (int j = 0; j < attributeLength + 2; j++) {
          text.append('X');
        }

        final Component c = headerRenderer.getTableCellRendererComponent(null,
          text.toString(), false, false, 0, 0);
        column.setMinWidth(c.getMinimumSize().width);
        column.setMaxWidth(c.getMaximumSize().width);
        column.setPreferredWidth(c.getPreferredSize().width);
        column.setWidth(column.getPreferredWidth());

        column.setCellRenderer(cellRenderer);
      }
    }
    for (final TableColumn column : removeColumns) {
      removeColumn(column);
    }
    tableHeader.addMouseListener(this);
    model.addTableModelListener(this);
  }

  public DataObjectMetaData getMetaData() {
    final DataObjectRowTableModel model = getModel();
    return model.getMetaData();
  }

  @Override
  public DataObjectRowTableModel getModel() {
    return (DataObjectRowTableModel)super.getModel();
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    if (e.getSource() == getTableHeader()) {
      final DataObjectRowTableModel model = getModel();
      final DataObjectMetaData metaData = model.getMetaData();
      final int column = columnAtPoint(e.getPoint());
      if (SwingUtilities.isLeftMouseButton(e)) {
        final Class<?> attributeClass = metaData.getAttributeClass(column);
        if (!Geometry.class.isAssignableFrom(attributeClass)) {
          model.setSortOrder(column);
        }
      }
    }
  }

  @Override
  public void mouseEntered(final MouseEvent e) {
  }

  @Override
  public void mouseExited(final MouseEvent e) {
  }

  @Override
  public void mousePressed(final MouseEvent e) {
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
  }

  @Override
  public void tableChanged(final TableModelEvent e) {
    super.tableChanged(e);
    if (tableHeader != null) {
      tableHeader.resizeAndRepaint();
    }
  }
}
