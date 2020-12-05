package com.revolsys.swing.table.counts;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jeometry.common.awt.WebColors;
import org.jeometry.common.io.PathNameProxy;

import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.table.AbstractTableModel;
import com.revolsys.swing.table.BaseJTable;
import com.revolsys.swing.table.BaseTableColumnModelListener;
import com.revolsys.util.Counter;
import com.revolsys.util.count.CategoryLabelCountMap;
import com.revolsys.util.count.LabelCounters;
import com.revolsys.util.count.TotalLabelCounters;

public class LabelCountMapTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 1L;

  private String selectedCountName;

  private String selectedLabel;

  private final CategoryLabelCountMap categoryLabelCountMap = new CategoryLabelCountMap();

  private final List<String> countNames = new ArrayList<>();

  private final List<String> labels = new ArrayList<>();

  private int columnCount = 1;

  public LabelCountMapTableModel() {
  }

  public LabelCountMapTableModel(final String labelTitle, final String... countNames) {
    if (labelTitle != null) {
      this.categoryLabelCountMap.setLabelTitle(labelTitle);
    }
    for (final String countName : countNames) {
      this.countNames.add(countName);
    }
    this.columnCount += this.countNames.size();
  }

  public void addColumns(final String... columnNames) {
    if (columnNames != null && columnNames.length > 0) {
      for (final String columnName : columnNames) {
        addCountNameColumn(columnName);
      }
      fireTableStructureChanged();
    }
  }

  public LabelCountMapTableModel addCount(final CharSequence rowLabel,
    final CharSequence columnLabel) {
    return addCount(rowLabel, columnLabel, 1);
  }

  public LabelCountMapTableModel addCount(final CharSequence rowLabel,
    final CharSequence columnLabel, final long count) {
    if (rowLabel != null && columnLabel != null && count != 0) {
      final LabelCounters labelCountMap = getLabelCounters(rowLabel, columnLabel);
      labelCountMap.addCount(rowLabel, count);
    }
    return this;
  }

  public LabelCountMapTableModel addCount(final PathNameProxy pathNameProxy,
    final CharSequence countName) {
    return addCount(pathNameProxy, countName, 1);
  }

  public LabelCountMapTableModel addCount(final PathNameProxy pathNameProxy,
    final CharSequence countName, final long count) {
    if (pathNameProxy != null) {
      final CharSequence label = pathNameProxy.getPathName();
      addCount(label, countName, count);
    }
    return this;
  }

  public void addCountNameColumn(final CharSequence countName) {
    if (!this.countNames.contains(countName)) {
      Invoke.andWait(() -> {
        if (!this.countNames.contains(countName)) {
          this.countNames.add(countName.toString());
          final int columnIndex = this.columnCount;
          this.columnCount++;
          fireTableStructureChanged();
          final BaseJTable table = getTable();
          if (table != null) {
            final TableColumn column = new TableColumnExt(columnIndex);
            setColumnWidth(columnIndex, column);
            table.addColumn(column);
          }
        }
      });
    }

  }

  public LabelCountMapTableModel addRowAndColumn(final CharSequence countName,
    final CharSequence label) {
    addCountNameColumn(countName);
    addRowLabel(label);
    return this;
  }

  public void addRowLabel(final CharSequence label) {
    final String labelString = label.toString();
    if (SwingUtilities.isEventDispatchThread()) {
      if (!this.labels.contains(labelString)) {
        this.labels.add(labelString);
        fireTableDataChanged();
      }
    } else {
      if (!this.labels.contains(labelString)) {
        Invoke.andWait(() -> {
          addRowLabel(labelString);
        });
      }
    }
  }

  public TotalLabelCounters addTotalColumn(final String totalCountName) {
    final TotalLabelCounters total = new TotalLabelCounters(totalCountName);
    this.categoryLabelCountMap.setLabelCounters(totalCountName, total);
    addCountNameColumn(totalCountName);
    return total;
  }

  public TotalLabelCounters addTotalColumn(final String totalCountName,
    final String... countNames) {
    final TotalLabelCounters total = addTotalColumn(totalCountName);
    for (final String countName : countNames) {
      final LabelCounters labelCounters = getLabelCounters(countName);
      total.addCounters(labelCounters);
    }
    return total;
  }

  public void clearCounts() {
    this.categoryLabelCountMap.clear();
  }

  public void clearCounts(final CharSequence countName) {
    this.categoryLabelCountMap.clearCounts(countName);
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == 0) {
      return String.class;
    } else {
      return Long.class;
    }
  }

  @Override
  public int getColumnCount() {
    return this.columnCount;
  }

  @Override
  public String getColumnName(final int columnIndex) {
    if (columnIndex < 1) {
      return getLabelTitle();
    } else {
      final int index = columnIndex - 1;
      if (index < this.countNames.size()) {
        return this.countNames.get(index);
      }
      return null;
    }
  }

  public Long getCount(final CharSequence label, final CharSequence countName) {
    return this.categoryLabelCountMap.getCount(countName, label);
  }

  public Long getCount(final CharSequence label, final CharSequence countName,
    final long defaultValue) {
    final Long count = this.categoryLabelCountMap.getCount(countName, label);
    if (count == null) {
      return defaultValue;
    } else {
      return count;
    }
  }

  public Counter getCounter(final CharSequence rowLabel, final CharSequence columnLabel) {
    final LabelCounters labelCounters = getLabelCounters(rowLabel, columnLabel);
    return labelCounters.getCounter(rowLabel);
  }

  public LabelCounters getLabelCounters(final CharSequence columnLabel) {
    final LabelCounters labelCountMap = this.categoryLabelCountMap.getLabelCountMap(columnLabel);
    addCountNameColumn(columnLabel);
    return labelCountMap;
  }

  public LabelCounters getLabelCounters(final CharSequence rowLabel,
    final CharSequence columnLabel) {
    final LabelCounters labelCounters = getLabelCounters(columnLabel);
    addRowLabel(rowLabel);
    return labelCounters;
  }

  public String getLabelTitle() {
    return this.categoryLabelCountMap.getLabelTitle();
  }

  @Override
  public int getRowCount() {
    return this.labels.size();
  }

  public CategoryLabelCountMap getStatistics() {
    return this.categoryLabelCountMap;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final String label = this.labels.get(rowIndex);
    switch (columnIndex) {
      case 0:
        return label;
      default:
        final String countName = this.countNames.get(columnIndex - 1);
        final Long value = getCount(label, countName);
        return value;
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  @Override
  public BaseJTable newTable() {
    final BaseJTable table = new BaseJTable(this);
    setTable(table);
    final TableColumnModel columnModel = table.getColumnModel();
    columnModel.addColumnModelListener(new BaseTableColumnModelListener() {

      @Override
      public void columnAdded(final TableColumnModelEvent e) {
        // e.get
        // final TableColumn column = new TableColumnExt(columnIndex);
        // setColumnWidth(columnIndex, column);
        // table.addColumn(column);
      }
    });
    for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
      final TableColumn column = columnModel.getColumn(columnIndex);
      setColumnWidth(columnIndex, column);
    }
    table.setAutoCreateColumnsFromModel(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
    final SortKey sortKey = new SortKey(0, SortOrder.ASCENDING);
    rowSorter.setSortKeys(Arrays.asList(sortKey));

    table.addHighlighter(
      new ColorHighlighter((final Component renderer, final ComponentAdapter adapter) -> {
        final int row = adapter.convertRowIndexToModel(adapter.row);
        if (getValueAt(row, 0).equals(LabelCountMapTableModel.this.selectedLabel)) {
          return true;
        }
        return false;
      }, WebColors.ForestGreen, WebColors.Yellow, WebColors.DarkGreen, WebColors.Yellow));

    table.addHighlighter(
      new ColorHighlighter((final Component renderer, final ComponentAdapter adapter) -> {
        final int column = adapter.convertColumnIndexToModel(adapter.column);
        final int row = adapter.convertRowIndexToModel(adapter.row);
        if (getValueAt(row, 0).equals(LabelCountMapTableModel.this.selectedLabel)) {
          if (getColumnName(column).equals(LabelCountMapTableModel.this.selectedCountName)) {
            return true;
          }
        }
        return false;
      }, WebColors.Yellow, WebColors.DarkGreen, WebColors.Gold, WebColors.DarkGreen));
    return table;
  }

  public void refresh() {

    final int rowCount = getRowCount();
    if (rowCount > 0) {
      try {
        fireTableRowsUpdated(0, rowCount - 1);
        if (this.selectedCountName != null && this.selectedLabel != null) {
          selectLabelCountCell(this.selectedLabel, this.selectedCountName);
        }
      } catch (final Throwable t) {
      }
    }
  }

  @Override
  public void removeTableModelListener(final TableModelListener l) {
  }

  public void selectLabelCountCell(final CharSequence label, final CharSequence countName) {
    this.selectedLabel = label.toString();
    this.selectedCountName = countName.toString();
    final BaseJTable table = getTable();
    table.repaint();
  }

  public void selectLabelCountCell(final PathNameProxy pathNameProxy, final String countName) {
    final CharSequence label = pathNameProxy.getPathName();
    selectLabelCountCell(label, countName);
  }

  private void setColumnWidth(final int columnIndex, final TableColumn column) {
    int minWidth = 70;
    if (columnIndex == 0) {
      minWidth = 240;
    }
    final String columnName = getColumnName(columnIndex);
    final int width = Math.max(minWidth, columnName.length() * 7);
    column.setMinWidth(width);
    column.setPreferredWidth(width);
  }

  public void setCounter(final CharSequence rowLabel, final CharSequence columnLabel,
    final Counter counter) {
    final LabelCounters labelCountMap = getLabelCounters(rowLabel, columnLabel);
    labelCountMap.setCounter(rowLabel, counter);
  }

  public void setStatistics(final String statisticName, final LabelCounters labelCountMap) {
    addCountNameColumn(statisticName);
    this.categoryLabelCountMap.setLabelCounters(statisticName, labelCountMap);
  }

  @Override
  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
  }

  public void writeCounts(final Object target) {
    if (getRowCount() > 0) {
      final String labelTitle = getLabelTitle();
      this.categoryLabelCountMap.writeCounts(target, labelTitle, this.countNames);
    }
  }

}
