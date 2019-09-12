package com.revolsys.swing.map.layer.record.component.recordmerge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.WindowConstants;

import org.jeometry.common.awt.WebColors;
import org.jeometry.common.data.type.DataTypes;

import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.TabbedPane;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.ProgressMonitor;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.table.RecordLayerTableCellEditor;
import com.revolsys.swing.menu.BaseJPopupMenu;
import com.revolsys.swing.table.BaseJTable;
import com.revolsys.swing.table.TablePanel;
import com.revolsys.swing.table.lambda.column.ColumnBasedTableModel;
import com.revolsys.swing.table.lambda.column.LayerRecordTableModelColumn;
import com.revolsys.swing.table.lambda.column.RecordTableModelColumn;
import com.revolsys.swing.table.record.model.RecordListTableModel;
import com.revolsys.swing.toolbar.ToolBar;
import com.revolsys.swing.undo.CreateRecordUndo;
import com.revolsys.swing.undo.DeleteLayerRecordUndo;
import com.revolsys.swing.undo.MultipleUndo;
import com.revolsys.swing.undo.UndoManager;
import com.revolsys.util.Property;

public class MergeRecordsDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  public static void showDialog(final AbstractRecordLayer layer) {
    final RecordMerger recordMerger = new RecordMerger(layer);
    final Window window = SwingUtil.getWindowAncestor(layer.getMapPanel());
    ProgressMonitor.background(window, "Merge " + layer.getName() + " Records", null,
      recordMerger::run, 100, () -> {
        new MergeRecordsDialog(window, layer, recordMerger) //
          .setVisible(true);
      });
  }

  private final AbstractRecordLayer layer;

  private TabbedPane mergedRecordsPanel;

  private final UndoManager undoManager;

  private final RecordMerger recordMerger;

  private MergeRecordsDialog(final Window window, final AbstractRecordLayer layer,
    final RecordMerger recordMerger) {
    super(window, "Merge " + layer.getName() + " Records", ModalityType.APPLICATION_MODAL);
    this.undoManager = layer.getMapPanel().getUndoManager();
    this.layer = layer;
    this.recordMerger = recordMerger;
    recordMerger.dialog = this;
    initDialog();
  }

  private void addHighlighter(final BaseJTable table, final MergeableRecord mergeableRecord,
    final MergeFieldMatchType matchType, final Color color, final Color colorSelected) {
    if (matchType != MergeFieldMatchType.EQUAL) {
      addOriginalRecordFieldHighlighter(table, mergeableRecord, matchType, color, colorSelected);
    }
    addMessageHighlighter(table, mergeableRecord, matchType, color, colorSelected);
  }

  private void addMessageHighlighter(final BaseJTable table, final MergeableRecord mergeableRecord,
    final MergeFieldMatchType matchType, final Color color1, final Color color2) {
    table.addColorHighlighter((rowIndex, columnIndex) -> {
      final MergeFieldMatchType rowMatchType = mergeableRecord.getMatchType(rowIndex);
      if (rowMatchType == matchType && columnIndex == 3) {
        return true;
      }
      return false;
    }, color1, color2);
  }

  private void addOriginalRecordFieldHighlighter(final BaseJTable table,
    final MergeableRecord mergeableRecord, final MergeFieldMatchType messageType, final Color c1,
    final Color c2) {
    table.addColorHighlighter((renderer, rowIndex, columnIndex) -> {
      if (columnIndex > 3) {
        final int recordIndex = columnIndex - 4;
        final MergeOriginalRecord originalRecord = mergeableRecord.getOriginalRecord(recordIndex);
        final int fieldIndex = rowIndex;
        final MergeFieldOriginalFieldState fieldState = originalRecord.getFieldState(fieldIndex);
        if (fieldState.isMatchType(messageType)) {
          ((JComponent)renderer)
            .setToolTipText("<html><b color='red'>" + fieldState.getMessage() + "</b></html>");
          return true;
        }
      }
      return false;
    }, c1, c2);
  }

  protected void initDialog() {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(600, 100));

    final BasePanel panel = new BasePanel(new BorderLayout());
    add(new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

    this.mergedRecordsPanel = new TabbedPane();
    panel.add(this.mergedRecordsPanel, BorderLayout.CENTER);

    pack();
    SwingUtil.autoAdjustPosition(this);
    setMergedRecords(this.recordMerger.errorMessage);
  }

  private TablePanel newTable(final MergeableRecord mergeableRecord) {
    final ColumnBasedTableModel model = newTableModel(mergeableRecord);

    final TablePanel tablePanel = model.newTablePanel();

    final String tabTitle = mergeableRecord.originalRecords.size() + " Mergeable";
    this.mergedRecordsPanel.addClosableTab(tabTitle, null, tablePanel, () -> {
      if (this.mergedRecordsPanel.getTabCount() == 0) {
        setVisible(false);
      }
    });

    final ToolBar toolBar = tablePanel.getToolBar();

    toolBar.addButton("default", "Merge Records", "table_row_merge",
      mergeableRecord.canMergeEnableCheck, () -> {
        final MultipleUndo multipleUndo = new MultipleUndo();
        final CreateRecordUndo createRecordUndo = new CreateRecordUndo(this.layer, mergeableRecord);
        multipleUndo.addEdit(createRecordUndo);
        for (final MergeOriginalRecord originalRecord : mergeableRecord.originalRecords) {
          final DeleteLayerRecordUndo deleteRecordUndo = new DeleteLayerRecordUndo(
            originalRecord.originalRecord);
          multipleUndo.addEdit(deleteRecordUndo);
        }
        if (this.undoManager == null) {
          multipleUndo.redo();
        } else {
          this.undoManager.addEdit(multipleUndo);
        }
        this.mergedRecordsPanel.remove(tablePanel);
        if (this.mergedRecordsPanel.getTabCount() == 0) {
          setVisible(false);
        }
      });
    final BaseJTable table = tablePanel.getTable();

    addHighlighter(table, mergeableRecord, MergeFieldMatchType.ERROR, WebColors.Pink,
      WebColors.Red);
    addHighlighter(table, mergeableRecord, MergeFieldMatchType.OVERRIDDEN, WebColors.Moccasin,
      WebColors.DarkOrange);
    addHighlighter(table, mergeableRecord, MergeFieldMatchType.ALLOWED_NOT_EQUAL,
      WebColors.PaleTurquoise, WebColors.DarkTurquoise);
    addHighlighter(table, mergeableRecord, MergeFieldMatchType.EQUAL, WebColors.LightGreen,
      WebColors.Green);

    table.setSortOrder(2, SortOrder.ASCENDING);

    table//
      .setColumnWidth(0, 40) //
      .setColumnPreferredWidth(1, 150) //
      .setColumnPreferredWidth(2, 110) //
    ;
    for (int i = 0; i <= mergeableRecord.originalRecords.size(); i++) {
      table.setColumnPreferredWidth(3 + i, 120);
    }
    return tablePanel;
  }

  private ColumnBasedTableModel newTableModel(final MergeableRecord mergeableRecord) {
    mergeableRecord.mergeValidateFields();
    final RecordDefinition recordDefinition = this.layer.getRecordDefinition();
    final int fieldCount = recordDefinition.getFieldCount();

    final List<String> fieldTitles = recordDefinition.getFieldTitles();

    final List<MergeFieldMatchType> matchTypes = mergeableRecord.getMatchTypes();

    final ColumnBasedTableModel model = new ColumnBasedTableModel()//
      .setRowCount(fieldCount) //
      .addColumnRowIndex() //
      .addColumnValues("Name", String.class, fieldTitles) //
      .addColumnValues("Match Type", MergeFieldMatchType.class, matchTypes) //
    ;
    model.addColumn( //
      new LayerRecordTableModelColumn("Merged Record", this.layer, mergeableRecord, true) {
        @Override
        public BaseJPopupMenu getMenu(final int fieldIndex) {
          final MergeFieldMatchType matchType = matchTypes.get(fieldIndex);
          if (matchType == MergeFieldMatchType.ERROR
            || matchType == MergeFieldMatchType.OVERRIDDEN) {
            final BaseJPopupMenu menu = new BaseJPopupMenu();
            final Set<Object> values = new HashSet<>();
            {
              final Object value = mergeableRecord.getCodeValue(fieldIndex);
              values.add(value);
            }
            for (final MergeOriginalRecord originalRecord : mergeableRecord.originalRecords) {
              final Object originalValue = originalRecord.getCodeValue(fieldIndex);
              values.add(originalValue);
            }
            for (final Object value : values) {
              final String menuTitle = "<html><b>Use:</b> <b color='red'>"
                + DataTypes.toString(value) + "</b></html>";
              menu.add(new RunnableAction(menuTitle, () -> {
                mergeableRecord.setValue(fieldIndex, value);
                mergeableRecord.mergeValidateField(fieldIndex);
                for (final MergeOriginalRecord originalRecord : mergeableRecord.originalRecords) {
                  final MergeFieldOriginalFieldState fieldState = originalRecord
                    .getFieldState(fieldIndex);
                  fieldState.setOverrideError(true);
                }
                mergeableRecord.mergeValidateField(fieldIndex);
              }));
            }
            if (matchType == MergeFieldMatchType.OVERRIDDEN) {
              final String menuTitle = "<html><b color='red'>Remove Overrides</b></html>";
              menu.add(new RunnableAction(menuTitle, () -> {
                for (final MergeOriginalRecord originalRecord : mergeableRecord.originalRecords) {
                  final MergeFieldOriginalFieldState fieldState = originalRecord
                    .getFieldState(fieldIndex);
                  fieldState.setOverrideError(false);
                }
                mergeableRecord.mergeValidateField(fieldIndex);
              }));
            }
            return menu;
          }
          return null;
        }
      } //
        .setCellEditor(table -> new RecordLayerTableCellEditor(table, this.layer) {
          @Override
          protected String getColumnFieldName(final int rowIndex, final int columnIndex) {
            return recordDefinition.getFieldName(rowIndex);
          }
        }) //
    );

    for (int i = 0; i < mergeableRecord.originalRecords.size(); i++) {
      final MergeOriginalRecord originalRecord = mergeableRecord.getOriginalRecord(i);
      model.addColumn(new RecordTableModelColumn("Record " + (i + 1), originalRecord, false) {
        @Override
        public BaseJPopupMenu getMenu(final int fieldIndex) {
          final MergeFieldOriginalFieldState fieldState = originalRecord.getFieldStates()
            .get(fieldIndex);
          if (fieldState.isMatchType(MergeFieldMatchType.ERROR)) {
            final BaseJPopupMenu menu = new BaseJPopupMenu();
            final String menuTitle = "<html><b>Ignore:</b> <b color='red'>"
              + fieldState.getMessage() + "</b></html>";
            menu.add(new RunnableAction(menuTitle, () -> {
              fieldState.setOverrideError(true);
              mergeableRecord.mergeValidateField(fieldIndex);
            }));
            return menu;
          } else if (fieldState.isMatchType(MergeFieldMatchType.OVERRIDDEN)) {
            final BaseJPopupMenu menu = new BaseJPopupMenu();
            final String menuTitle = "<html><b color='red'>Remove Override</b></html>";
            menu.add(new RunnableAction(menuTitle, () -> {
              fieldState.setOverrideError(false);
              mergeableRecord.mergeValidateField(fieldIndex);
            }));
            return menu;
          }
          return null;
        }
      });
    }

    mergeableRecord.tableModel = model;
    return model;
  }

  private void setMergedRecords(String errorMessage) {
    final List<LayerRecord> nonMergeableRecords = new ArrayList<>();

    for (final MergeableRecord mergeableRecord : this.recordMerger.records) {
      final List<MergeOriginalRecord> originalRecords = mergeableRecord.originalRecords;
      if (originalRecords.size() == 1) {
        for (final MergeOriginalRecord originalRecord : originalRecords) {
          nonMergeableRecords.add(originalRecord.originalRecord);
        }
      } else {
        newTable(mergeableRecord);
      }
    }
    if (!nonMergeableRecords.isEmpty() || Property.hasValue(errorMessage)) {
      final TablePanel tablePanel = RecordListTableModel.newPanel(this.layer, nonMergeableRecords);
      final RecordListTableModel tableModel = tablePanel.getTableModel();
      tableModel.setEditable(false);
      tablePanel.setPreferredSize(new Dimension(100, 50 + nonMergeableRecords.size() * 22));

      final JPanel panel = new JPanel(new BorderLayout());
      if (!Property.hasValue(errorMessage)) {
        errorMessage = "The following records could not be merged and will not be modified.";
      }
      final JLabel unMergeLabel = new JLabel(
        "<html><p style=\"color:red\">" + errorMessage + "</p></html>");
      panel.add(unMergeLabel, BorderLayout.NORTH);
      panel.add(tablePanel, BorderLayout.CENTER);

      this.mergedRecordsPanel.addClosableTab(nonMergeableRecords.size() + " Non-Mergeable", null,
        panel, () -> {
          if (this.mergedRecordsPanel.getTabCount() == 0) {
            setVisible(false);
          }
        });
    }
    SwingUtil.autoAdjustPosition(this);
  }

  @Override
  public String toString() {
    return getTitle();
  }

}