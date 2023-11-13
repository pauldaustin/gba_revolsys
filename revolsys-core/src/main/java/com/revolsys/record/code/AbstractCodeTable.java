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
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.record.schema.FieldDefinition;

public abstract class AbstractCodeTable extends BaseObjectWithPropertiesAndChange
  implements BaseCloseable, CodeTable, Cloneable {

  protected CodeTableData data = new CodeTableData(this);

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

  public CodeTableEntry getEntry(final Consumer<CodeTableEntry> callback, final Object idOrValue) {
    return getData().getEntry(idOrValue);
  }

  @Override
  public Identifier getIdentifier(final Consumer<CodeTableEntry> callback, final Object value) {
    final CodeTableEntry entry = getEntry(callback, value);
    return CodeTableEntry.getIdentifier(entry);
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
  public JsonObject getMap(final Consumer<JsonObject> callback, final Identifier id) {
    final CodeTableEntry entry = getEntry(e -> {
      final var map = getMap(e);
      callback.accept(map);
    }, id);
    return getMap(entry);
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
  public <V> V getValue(final Consumer<CodeTableEntry> callback, final Identifier id) {
    final CodeTableEntry entry = getEntry(callback, id);
    return CodeTableEntry.getValue(entry);
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
  public List<Object> getValues(final Consumer<CodeTableEntry> callback, final Identifier id) {
    final CodeTableEntry entry = getEntry(callback, id);
    return CodeTableEntry.getValues(entry);
  }

  public boolean hasIdentifier(final Identifier id) {
    return getData().hasIdentifier(id);
  }

  @Override
  public boolean hasValue(final Object value) {
    final CodeTableEntry entry = getEntry(null, value);
    return entry != null;
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
    if (idOrValue == null) {
      callback.accept(null);
    } else {
      final var entry = getEntry(callback, idOrValue);
      if (entry != null) {
        callback.accept(entry);
      }
    }
  }

}
