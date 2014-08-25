package com.revolsys.swing.map.layer.record.table.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

import com.revolsys.data.equals.EqualsRegistry;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.swing.map.form.LayerRecordForm;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.table.record.model.AbstractSingleRecordTableModel;
import com.revolsys.util.Property;

public class RecordLayerAttributesTableModel extends
  AbstractSingleRecordTableModel implements PropertyChangeListener {

  private static final long serialVersionUID = 1L;

  private LayerRecord record;

  private final AbstractRecordLayer layer;

  private final Reference<LayerRecordForm> form;

  public RecordLayerAttributesTableModel(final LayerRecordForm form) {
    super(form.getRecordDefinition(), true);
    this.form = new WeakReference<>(form);
    this.layer = form.getLayer();
    this.record = form.getRecord();
    Property.addListener(this.layer, this);
  }

  @Override
  public int getColumnCount() {
    return 4;
  }

  @Override
  public String getColumnName(final int column) {
    if (column == 3) {
      return "Original Value";
    } else {
      return super.getColumnName(column);
    }
  }

  @Override
  public String getFieldTitle(final String fieldName) {
    return this.layer.getFieldTitle(fieldName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Map<String, Object>> V getMap(final int columnIndex) {
    if (columnIndex == 2) {
      return (V)this.record;
    } else {
      return null;
    }
  }

  @Override
  public Object getObjectValue(final int rowIndex, final int columnIndex) {
    if (this.record == null) {
      return null;
    } else {
      return this.record.getValue(rowIndex);
    }
  }

  public LayerRecord getRecord() {
    return this.record;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    if (this.record == null) {
      return null;
    } else if (columnIndex == 3) {
      final String attributeName = getFieldName(rowIndex);
      return this.record.getOriginalValue(attributeName);
    } else {
      return super.getValueAt(rowIndex, columnIndex);
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    if (columnIndex == 2) {
      if (this.form.get().isEditable()) {
        final String attributeName = getFieldName(rowIndex);
        final RecordDefinition recordDefinition = getRecordDefinition();
        final Attribute idAttribute = recordDefinition.getIdAttribute();
        if (idAttribute != null) {
          final String idAttributeName = idAttribute.getName();
          if (attributeName.equals(idAttributeName)) {
            if (this.record.getState() == RecordState.New) {
              if (!Number.class.isAssignableFrom(idAttribute.getTypeClass())) {
                return true;
              }
            }
            return false;
          }
        }
        if (recordDefinition.getGeometryAttributeNames()
            .contains(attributeName)) {
          return false;
        } else {
          return this.form.get().isEditable(attributeName);
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public boolean isModified(final int rowIndex) {
    final String attributeName = getFieldName(rowIndex);
    final Object originalValue = this.record.getOriginalValue(attributeName);
    final Object value = this.record.getValue(attributeName);
    return !EqualsRegistry.equal(originalValue, value);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final Object source = event.getSource();
    if (source == this.record) {
      final String propertyName = event.getPropertyName();
      final RecordDefinition recordDefinition = getRecordDefinition();
      final int index = recordDefinition.getAttributeIndex(propertyName);
      if (index > -1) {
        try {
          fireTableRowsUpdated(index, index);
        } catch (final Throwable t) {
        }
      }
    }
  }

  public void removeListener() {
    Property.removeListener(this.layer, this);
  }

  @Override
  protected Object setObjectValue(final int rowIndex, final Object value) {
    final Object oldValue = this.record.getValue(rowIndex);
    this.record.setValue(rowIndex, value);
    return oldValue;
  }

  public void setRecord(final LayerRecord record) {
    this.record = record;
  }
}
