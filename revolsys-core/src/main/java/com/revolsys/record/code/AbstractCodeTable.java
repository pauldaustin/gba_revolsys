package com.revolsys.record.code;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.swing.JComponent;

import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.common.data.type.DataTypes;

import com.revolsys.io.BaseCloseable;
import com.revolsys.properties.BaseObjectWithPropertiesAndChange;
import com.revolsys.record.schema.FieldDefinition;

public class AbstractCodeTable extends BaseObjectWithPropertiesAndChange
  implements BaseCloseable, CodeTable, Cloneable {

  protected CodeTableData data = newData();

  protected CodeTableData newData() {
    return new CodeTableData(this);
  }

  private String name;

  protected final ReentrantLock lock = new ReentrantLock();

  private JComponent swingEditor;

  private List<FieldDefinition> valueFieldDefinitions = Arrays
    .asList(new FieldDefinition("value", DataTypes.STRING, true));

  private boolean caseSensitive;

  public AbstractCodeTable() {
  }

  protected CodeTableEntry addEntry(final Identifier id, final Object value) {
    return getData().addEntry(id, value);
  }

  @Override
  public AbstractCodeTable clone() {
    final AbstractCodeTable clone = (AbstractCodeTable)super.clone();
    clone.data = this.data.clone();
    return clone;
  }

  @Override
  public void close() {
    getData().close();
    this.swingEditor = null;
  }

  protected CodeTableData getData() {
    return this.data;
  }

  @Override
  public CodeTableEntry getEntry(final Object idOrValue) {
    return getData().getEntry(idOrValue);
  }

  public Identifier getIdentifier(final int index) {
    return getData().getIdentifier(index);
  }

  @Override
  public Identifier getIdentifierByIndex(final int index) {
    return getData().getIdentifier(index);
  }

  @Override
  public List<Identifier> getIdentifiers() {
    return getData().getIdentifiers();
  }

  @Override
  public String getIdFieldName() {
    return getName();
  }

  @Override
  public String getName() {
    return this.name;
  }

  protected long getNextId() {
    return getData().getNextId();
  }

  @Override
  public JComponent getSwingEditor() {
    return this.swingEditor;
  }

  @Override
  public <V> V getValue(final Identifier id) {
    return getEntry(id).getValue();
  }

  @Override
  public FieldDefinition getValueFieldDefinition() {
    return this.valueFieldDefinitions.get(0);
  }

  public List<FieldDefinition> getValueFieldDefinitions() {
    return this.valueFieldDefinitions;
  }

  @Override
  public int getValueFieldLength() {
    return getData().getValueFieldLength();
  }

  @Override
  public List<Object> getValues(final Identifier id) {
    return getEntry(id).getValues();
  }

  public boolean hasIdentifier(final Identifier id) {
    return getData().hasIdentifier(id);
  }

  @Override
  public boolean hasValue(final Object value) {
    return !getEntry(value).isEmpty();
  }

  public boolean isCaseSensitive() {
    return this.caseSensitive;
  }

  @Override
  public boolean isEmpty() {
    return getData().isEmpty();
  }

  protected boolean isFindByValue(final Identifier identifier) {
    return true;
  }

  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  protected void setData(final CodeTableData data) {
    this.data = data;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setSwingEditor(final JComponent swingEditor) {
    this.swingEditor = swingEditor;
  }

  public AbstractCodeTable setValueFieldDefinitions(
    final List<FieldDefinition> valueFieldDefinitions) {
    this.valueFieldDefinitions = valueFieldDefinitions;
    return this;
  }

  @Override
  public int size() {
    return getData().size();
  }

  @Override
  public void withEntry(Consumer<CodeTableEntry> callback, Object idOrValue) {
    final var entry = getEntry(callback, idOrValue);
    if (entry.isEmpty()) {
      callback.accept(entry);
    }
  }

}
